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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.classlib.java.lang.TInteger;
import org.teavm.classlib.java.lang.TObject;

public abstract class TInputStream extends TObject implements TCloseable {
    private static final int BUFFER_SIZE = 2048;

    public TInputStream() {
    }

    public abstract int read() throws IOException;

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            int bt = read();
            if (bt < 0) {
                return i == 0 ? -1 : i;
            }
            b[off++] = (byte) bt;
        }
        return len > 0 ? len : -1;
    }

    public long skip(long n) throws IOException {
        if (n < TInteger.MAX_VALUE) {
            return skip((int) n);
        } else {
            for (long i = 0; i < n; ++i) {
                if (read() < 0) {
                    return i;
                }
            }
            return n;
        }
    }

    private int skip(int n) throws IOException {
        for (int i = 0; i < n; ++i) {
            if (read() < 0) {
                return i;
            }
        }
        return n;
    }

    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }

    public void mark(@SuppressWarnings("unused") int readlimit) {
    }

    public void reset() throws IOException {
        throw new IOException();
    }

    public boolean markSupported() {
        return false;
    }

    public byte[] readAllBytes() throws IOException {
        return readNBytes(Integer.MAX_VALUE);
    }

    public byte[] readNBytes(int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return new byte[0];
        }

        List<byte[]> buffers = null;
        var buffer = new byte[BUFFER_SIZE];
        var positionInBuffer = 0;
        var totalBytesRead = 0;
        while (true) {
            var bytesToRead = Math.min(buffer.length - positionInBuffer, len - totalBytesRead);
            var bytesRead = read(buffer, positionInBuffer, bytesToRead);
            if (bytesRead < 0) {
                break;
            }
            positionInBuffer += bytesRead;
            totalBytesRead += bytesRead;
            if (totalBytesRead == len) {
                break;
            }
            if (positionInBuffer * 2 > buffer.length) {
                if (buffers == null) {
                    buffers = new ArrayList<>();
                }
                buffers.add(Arrays.copyOf(buffer, positionInBuffer));
                positionInBuffer = 0;
            }
        }
        if (buffers == null) {
            return positionInBuffer == buffer.length ? buffer : Arrays.copyOf(buffer, positionInBuffer);
        }

        var result = new byte[totalBytesRead];
        var ptr = 0;
        for (var part : buffers) {
            System.arraycopy(part, 0, result, ptr, part.length);
            ptr += part.length;
        }
        if (positionInBuffer > 0) {
            System.arraycopy(buffer, 0, result, ptr, positionInBuffer);
        }
        return result;
    }

    public int readNBytes(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len >= b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        var initialLen = len;

        while (true) {
            var bytesRead = read(b, off, len);
            if (bytesRead < 0) {
                break;
            }
            off += bytesRead;
            len -= bytesRead;
        }

        return initialLen - len;
    }

    public void skipNBytes(long n) throws IOException {
        if (n < 0) {
            throw new IndexOutOfBoundsException();
        }
        while (n > 0) {
            var bytesSkipped = skip(n);
            if (bytesSkipped < 0) {
                throw new IOException();
            }
            n -= bytesSkipped;
            if (n == 0) {
                break;
            }
            if (n < 0) {
                throw new IOException();
            }
        }
    }

    public long transferTo(OutputStream out) throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        var bytesTransferred = 0L;
        while (true) {
            var bytesRead = read(buffer);
            if (bytesRead < 0) {
                break;
            }
            out.write(buffer, 0, bytesRead);
            bytesTransferred += bytesRead;
        }
        return bytesTransferred;
    }

    public static TInputStream nullInputStream() {
        return new TInputStream() {
            private boolean closed;

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public int read() throws IOException {
                checkOpen();
                return -1;
            }

            @Override
            public int read(byte[] b) throws IOException {
                checkOpen();
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                checkOpen();
                return -1;
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                checkOpen();
                return new byte[0];
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                checkOpen();
                return new byte[0];
            }

            @Override
            public long skip(long n) throws IOException {
                checkOpen();
                if (n > 0) {
                    throw new IOException();
                } else if (n < 0) {
                    throw new IndexOutOfBoundsException();
                }
                return 0;
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                if (n > 0) {
                    throw new IOException();
                }
            }

            private void checkOpen() throws IOException {
                if (closed) {
                    throw new IOException();
                }
            }
        };
    }
}
