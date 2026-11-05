/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.teavm.classlib.impl.Base64Impl;

public final class TBase64 {
    private TBase64() {
    }

    public static Encoder getEncoder() {
        return new Encoder(Base64Impl.alphabet, true);
    }

    public static Encoder getUrlEncoder() {
        return new Encoder(Base64Impl.urlAlphabet, true);
    }

    public static class Encoder {
        private final byte[] mapping;
        private final boolean padding;

        private Encoder(byte[] mapping, boolean padding) {
            this.mapping = mapping;
            this.padding = padding;
        }

        public byte[] encode(byte[] src) {
            return Base64Impl.encode(src, mapping, padding);
        }

        public String encodeToString(byte[] src) {
            var bytes = encode(src);
            var chars = new char[bytes.length];
            for (var i = 0; i < bytes.length; ++i) {
                chars[i] = (char) (bytes[i] & 0xFF);
            }
            return new String(chars);
        }

        public Encoder withoutPadding() {
            return new Encoder(mapping, false);
        }

        public OutputStream wrap(OutputStream output) {
            return new EncodingStream(Objects.requireNonNull(output), mapping, padding);
        }
    }

    public static Decoder getDecoder() {
        return new Decoder(Base64Impl.reverse);
    }

    public static Decoder getUrlDecoder() {
        return new Decoder(Base64Impl.urlReverse);
    }

    public static class Decoder {
        private final int[] mapping;

        private Decoder(int[] mapping) {
            this.mapping = mapping;
        }

        public byte[] decode(byte[] src) {
            return Base64Impl.decode(src, mapping);
        }

        public byte[] decode(String src) {
            var bytes = new byte[src.length()];
            for (var i = 0; i < bytes.length; ++i) {
                bytes[i] = (byte) src.charAt(i);
            }
            return decode(bytes);
        }

        public InputStream wrap(InputStream input) {
            return new DecodingStream(Objects.requireNonNull(input), mapping);
        }
    }

    private static final class EncodingStream extends OutputStream {
        private final OutputStream output;
        private final byte[] mapping;
        private final boolean padding;
        private int bits;
        private int count;
        private boolean closed;

        EncodingStream(OutputStream output, byte[] mapping, boolean padding) {
            this.output = output;
            this.mapping = mapping;
            this.padding = padding;
        }

        @Override
        public void write(int value) throws IOException {
            ensureOpen();
            bits = (bits << 8) | (value & 255);
            if (++count == 3) {
                output.write(mapping[(bits >>> 18) & 63]);
                output.write(mapping[(bits >>> 12) & 63]);
                output.write(mapping[(bits >>> 6) & 63]);
                output.write(mapping[bits & 63]);
                bits = 0;
                count = 0;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ensureOpen();
            Objects.checkFromIndexSize(offset, length, bytes.length);
            for (int i = 0; i < length; ++i) {
                write(bytes[offset + i]);
            }
        }

        @Override
        public void flush() throws IOException {
            ensureOpen();
            output.flush();
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                if (count == 1) {
                    output.write(mapping[(bits >>> 2) & 63]);
                    output.write(mapping[(bits << 4) & 63]);
                    if (padding) {
                        output.write('=');
                        output.write('=');
                    }
                } else if (count == 2) {
                    output.write(mapping[(bits >>> 10) & 63]);
                    output.write(mapping[(bits >>> 4) & 63]);
                    output.write(mapping[(bits << 2) & 63]);
                    if (padding) {
                        output.write('=');
                    }
                }
            } finally {
                output.close();
            }
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
        }
    }

    private static final class DecodingStream extends InputStream {
        private final InputStream input;
        private final int[] mapping;
        private int bits;
        private int remaining;
        private boolean ended;
        private boolean closed;

        DecodingStream(InputStream input, int[] mapping) {
            this.input = input;
            this.mapping = mapping;
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            if (remaining == 0 && !readQuantum()) {
                return -1;
            }
            return (bits >>> (--remaining * 8)) & 255;
        }

        private boolean readQuantum() throws IOException {
            if (ended) {
                return false;
            }
            bits = 0;
            int count = 0;
            while (count < 4) {
                int value = input.read();
                if (value < 0) {
                    ended = true;
                    if (count == 1) {
                        throw new IOException("Incomplete Base64 quantum");
                    }
                    break;
                }
                if (value == '=') {
                    if (count < 2 || count == 2 && input.read() != '=') {
                        throw new IOException("Invalid Base64 padding");
                    }
                    ended = true;
                    break;
                }
                int decoded = mapping[value];
                if (decoded < 0) {
                    throw new IOException("Invalid Base64 character");
                }
                bits = (bits << 6) | decoded;
                ++count;
            }
            if (count < 2) {
                return false;
            }
            remaining = count - 1;
            bits >>>= (4 - count) * 2;
            return true;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            ensureOpen();
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            int count = 0;
            while (count < length) {
                int value = read();
                if (value < 0) {
                    break;
                }
                bytes[offset + count++] = (byte) value;
            }
            return count == 0 ? -1 : count;
        }

        @Override
        public int available() throws IOException {
            ensureOpen();
            return input.available();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                input.close();
            }
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
        }
    }
}
