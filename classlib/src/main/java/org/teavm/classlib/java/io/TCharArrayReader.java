/*
 *  Copyright 2017 Alexey Andreev.
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
import java.io.Reader;

public class TCharArrayReader extends Reader {
    protected char[] buf;
    protected int pos;
    protected int markedPos = -1;
    protected int count;

    public TCharArrayReader(char[] buf) {
        this.buf = buf;
        this.count = buf.length;
    }

    public TCharArrayReader(char[] buf, int offset, int length) {
        /*
         * The spec of this constructor is broken. In defining the legal values
         * of offset and length, it doesn't consider buffer's length. And to be
         * compatible with the broken spec, we must also test whether
         * (offset + length) overflows.
         */
        if (offset < 0 || offset > buf.length || length < 0 || offset + length < 0) {
            throw new IllegalArgumentException();
        }
        this.buf = buf;
        this.pos = offset;
        this.markedPos = offset;

        /* This is according to spec */
        int bufferLength = buf.length;
        this.count = offset + length < bufferLength ? length : bufferLength;
    }

    @Override
    public void close() {
        if (isOpen()) {
            buf = null;
        }
    }

    private boolean isOpen() {
        return buf != null;
    }

    private boolean isClosed() {
        return buf == null;
    }

    @Override
    public void mark(int readLimit) throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        markedPos = pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        if (pos == count) {
            return -1;
        }
        return buf[pos++];
    }

    @Override
    public int read(char[] buffer, int offset, int len) throws IOException {
        if (offset < 0 || offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("Offset out of bounds:" + offset);
        }
        if (len < 0 || len > buffer.length - offset) {
            throw new ArrayIndexOutOfBoundsException("Length out of bounds: " + len);
        }
        if (isClosed()) {
            throw new IOException();
        }
        if (pos < this.count) {
            int bytesRead = pos + len > this.count ? this.count - pos : len;
            System.arraycopy(this.buf, pos, buffer, offset, bytesRead);
            pos += bytesRead;
            return bytesRead;
        }
        return -1;
    }

    @Override
    public boolean ready() throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        return pos != count;
    }

    @Override
    public void reset() throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        pos = markedPos != -1 ? markedPos : 0;
    }

    @Override
    public long skip(long n) throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        if (n <= 0) {
            return 0;
        }
        long skipped;
        if (n < this.count - pos) {
            pos = pos + (int) n;
            skipped = n;
        } else {
            skipped = this.count - pos;
            pos = this.count;
        }
        return skipped;
    }
}
