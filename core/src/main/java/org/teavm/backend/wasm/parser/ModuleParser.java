/*
 *  Copyright 2022 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.backend.wasm.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.common.AsyncInputStream;
import org.teavm.common.Promise;

public class ModuleParser {
    private static final int WASM_HEADER_SIZE = 8;
    private AsyncInputStream reader;
    private int pos;
    private int posBefore;
    private byte[] buffer = new byte[256];
    private int posInBuffer;
    private int bufferLimit;
    private int currentLEB;
    private int currentLEBShift;
    private boolean eof;
    private int bytesInLEB;
    private int sectionCode;
    private List<byte[]> chunks = new ArrayList<>();

    public ModuleParser(AsyncInputStream inputStream) {
        this.reader = inputStream;
    }

    public Promise<Void> parse() {
        return parseHeader().thenAsync(v -> parseSections());
    }

    private Promise<Void> parseHeader() {
        return fillAtLeast(16).thenVoid(v -> {
            if (remaining() < WASM_HEADER_SIZE) {
                error("Invalid WebAssembly header");
            }
            if (readInt32() != 0x6d736100) {
                error("Invalid WebAssembly magic number");
            }
            if (readInt32() != 1) {
                error("Unsupported WebAssembly version");
            }
        });
    }

    private Promise<Void> parseSections() {
        return readLEB().thenAsync(n -> {
            if (n == null) {
                return Promise.VOID;
            }
            sectionCode = n;
            return continueParsingSection().thenAsync(v -> parseSections());
        });
    }

    private Promise<Void> continueParsingSection() {
        return requireLEB("Unexpected end of file reading section length").thenAsync(sectionSize -> {
            if (sectionCode == 0) {
                return parseSection(pos + sectionSize);
            } else {
                var consumer = getSectionConsumer(sectionCode, pos, null);
                if (consumer == null) {
                    return skip(sectionSize, "Error skipping section " + sectionCode + " of size " + sectionSize);
                } else {
                    return readBytes(sectionSize, "Error reading section " + sectionCode + " content")
                            .thenVoid(consumer);
                }
            }
        });
    }

    private Promise<Void> parseSection(int limit) {
        return readString("Error reading custom section name").thenAsync(name -> {
            var consumer = getSectionConsumer(0, pos, name);
            if (consumer != null) {
                return readBytes(limit - pos, "Error reading section '" + name + "' content").thenVoid(consumer);
            } else {
                return skip(limit - pos, "Error skipping section '" + name + "'");
            }
        });
    }

    protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
        return null;
    }

    private Promise<String> readString(String error) {
        return readBytes(error).then(b -> new String(b, StandardCharsets.UTF_8));
    }

    private Promise<byte[]> readBytes(String error) {
        return requireLEB(error + ": error parsing size")
                .thenAsync(size -> readBytes(size, error + ": error reading content"));
    }

    private Promise<byte[]> readBytes(int count, String error) {
        return readBytesImpl(count, error).then(v -> {
            var result = new byte[count];
            var i = 0;
            for (var chunk : chunks) {
                System.arraycopy(chunk, 0, result, i, chunk.length);
                i += chunk.length;
            }
            chunks.clear();
            return result;
        });
    }

    private Promise<Void> readBytesImpl(int count, String error) {
        posBefore = pos;
        var min = Math.min(count, remaining());
        var chunk = new byte[min];
        System.arraycopy(buffer, posInBuffer, chunk, 0, min);
        chunks.add(chunk);
        pos += min;
        posInBuffer += min;
        if (count > min) {
            if (eof) {
                error(error);
            }
            return fill().thenAsync(x -> readBytesImpl(count - min, error));
        } else {
            return Promise.VOID;
        }
    }

    private Promise<Integer> requireLEB(String errorMessage) {
        return readLEB().then(n -> {
            if (n == null) {
                error(errorMessage);
            }
            return n;
        });
    }

    private Promise<Integer> readLEB() {
        posBefore = pos;
        currentLEB = 0;
        bytesInLEB = 0;
        currentLEBShift = 0;
        return continueLEB();
    }

    private Promise<Integer> continueLEB() {
        while (posInBuffer < bufferLimit) {
            var b = buffer[posInBuffer++];
            ++pos;
            ++bytesInLEB;
            var digit = b & 0x7F;
            if (((digit << currentLEBShift) >> currentLEBShift) != digit) {
                error("LEB represents too big number");
            }
            currentLEB |= digit << currentLEBShift;
            if ((b & 0x80) == 0) {
                return Promise.of(currentLEB);
            }
            currentLEBShift += 7;
        }
        return fill().thenAsync(bytesRead -> {
            if (eof) {
                if (bytesInLEB > 0) {
                    error("Unexpected end of file reached reading LEB");
                }
                return Promise.of(null);
            } else {
                return continueLEB();
            }
        });
    }

    private int readInt32() {
        posBefore = pos;
        var result = (buffer[posInBuffer] & 255)
                | ((buffer[posInBuffer + 1] & 255) << 8)
                | ((buffer[posInBuffer + 2] & 255) << 16)
                | ((buffer[posInBuffer + 3] & 255) << 24);
        posInBuffer += 4;
        pos += 4;
        return result;
    }

    private int remaining() {
        return bufferLimit - posInBuffer;
    }

    private Promise<Void> skip(int bytes, String error) {
        if (bytes <= remaining()) {
            posInBuffer += bytes;
            pos += bytes;
            return Promise.VOID;
        } else {
            posBefore = pos;
            pos += remaining();
            bytes -= remaining();
            posInBuffer = 0;
            bufferLimit = 0;
            return skipImpl(bytes, error);
        }
    }

    private Promise<Void> skipImpl(int bytes, String error) {
        return reader.skip(bytes).thenAsync(bytesSkipped -> {
            if (bytesSkipped < 0) {
                error(error);
            }
            pos += bytesSkipped;
            return bytes > bytesSkipped ? skipImpl(bytes - bytesSkipped, error) : Promise.VOID;
        });
    }

    private Promise<Void> fillAtLeast(int least) {
        return fill().thenAsync(x -> fillAtLeastImpl(least));
    }

    private Promise<Void> fillAtLeastImpl(int least) {
        if (eof || least <= remaining()) {
            return Promise.VOID;
        }
        return reader.read(buffer, bufferLimit, Math.min(least, buffer.length - bufferLimit))
                .thenAsync(bytesRead -> {
                    if (bytesRead < 0) {
                        eof = true;
                    } else {
                        bufferLimit += bytesRead;
                    }
                    return fillAtLeastImpl(least);
                });
    }

    private Promise<Void> fill() {
        return readNext(buffer.length - remaining());
    }

    private Promise<Void> readNext(int bytes) {
        if (eof) {
            return Promise.VOID;
        }
        if (posInBuffer > 0 && posInBuffer < bufferLimit) {
            System.arraycopy(buffer, posInBuffer, buffer, 0, bufferLimit - posInBuffer);
        }
        bufferLimit -= posInBuffer;
        posInBuffer = 0;
        return reader.read(buffer, bufferLimit, bytes).thenVoid(bytesRead -> {
            if (bytesRead < 0) {
                eof = true;
            } else {
                bufferLimit += bytesRead;
            }
        });
    }

    private void error(String message) {
        throw new ParseException(message, posBefore);
    }

}
