/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.io;

import java.io.IOException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.stream.TStream;
import org.teavm.classlib.java.util.stream.impl.TBufferedReaderLinesStream;

public class TBufferedReader extends TReader {
    private TReader innerReader;
    private char[] buffer;
    private int index;
    private int count;
    private boolean eof;
    private int mark = -1;

    public TBufferedReader(TReader innerReader, int size) {
        if (size < 0) {
            throw new TIllegalArgumentException();
        }
        this.innerReader = innerReader;
        this.buffer = new char[TMath.max(64, size)];
    }

    public TBufferedReader(TReader innerReader) {
        this(innerReader, 1024);
    }

    @Override
    public int read() throws IOException {
        requireOpened();
        if (index >= count) {
            if (!fillBuffer(0, 1)) {
                return -1;
            }
        }
        return buffer[index++];
    }

    @Override
    public void close() throws IOException {
        requireOpened();
        innerReader.close();
        innerReader = null;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        requireOpened();
        if (index == count && eof) {
            return -1;
        }
        int charsRead = 0;
        while (charsRead < len) {
            int n = TMath.min(count - index, len - charsRead);
            System.arraycopy(buffer, index, cbuf, off, n);
            off += n;
            index += n;
            charsRead += n;
            if (charsRead > 0 && !innerReader.ready() || !fillBuffer(0, len - charsRead)) {
                break;
            }
        }
        return charsRead;
    }

    public String readLine() throws IOException {
        requireOpened();
        if (eof && index >= count) {
            return null;
        }
        TStringBuilder line = new TStringBuilder();
        while (true) {
            if (index >= count) {
                if (!fillBuffer(0, count - index)) {
                    if (line.isEmpty()) {
                        return null;
                    }
                    break;
                }
            }
            char ch = buffer[index++];
            if (ch == '\n') {
                break;
            } else if (ch == '\r') {
                if (index >= count) {
                    if (!fillBuffer(0, count - index)) {
                        break;
                    }
                }
                if (buffer[index] == '\n') {
                    ++index;
                }
                break;
            } else {
                line.append(ch);
            }
        }
        return line.toString();
    }

    public TStream<String> lines() {
        return new TBufferedReaderLinesStream(this);
    }

    @Override
    public long skip(long n) throws IOException {
        requireOpened();
        if (n < count - index) {
            index += n;
            return n;
        } else {
            n -= count - index;
            long skipped = innerReader.skip(n);
            if (skipped == n) {
                fillBuffer(0, (int) n);
            } else {
                eof = true;
            }
            return skipped;
        }
    }

    @Override
    public boolean ready() {
        return index < count;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit > buffer.length) {
            buffer = TArrays.copyOf(buffer, readAheadLimit);
        }
        if (count - index < readAheadLimit) {
            for (int i = index; i < count; ++i) {
                buffer[i - index] = buffer[i];
            }
            fillBuffer(count - index, count - index);
        }
        mark = index;
    }

    @Override
    public void reset() throws IOException {
        if (mark == -1) {
            throw new IOException();
        }
        index = mark;
    }

    private boolean fillBuffer(int offset, int readLimit) throws IOException {
        if (eof) {
            return false;
        }
        readLimit = Math.min(readLimit, buffer.length - index);
        var totalRead = 0;
        while (true) {
            int charsRead = innerReader.read(buffer, offset, buffer.length - offset);
            if (charsRead == -1) {
                eof = true;
                break;
            } else {
                offset += charsRead;
                totalRead += charsRead;
                if ((offset >= readLimit || !innerReader.ready()) && totalRead > 0) {
                    break;
                }
            }
        }
        count = offset;
        index = 0;
        mark = -1;
        return totalRead > 0;
    }

    private void requireOpened() throws IOException {
        if (innerReader == null) {
            throw new IOException();
        }
    }
}
