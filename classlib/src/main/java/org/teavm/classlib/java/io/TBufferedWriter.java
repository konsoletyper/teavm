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
import java.io.Writer;
import java.util.Objects;

public class TBufferedWriter extends Writer {
    private Writer out;
    private char[] buf;
    private int pos;
    private final String lineSeparator = "\n";

    public TBufferedWriter(Writer out) {
        super(out);
        this.out = out;
        buf = new char[8192];
    }

    public TBufferedWriter(Writer out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        this.out = out;
        this.buf = new char[size];
    }

    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }

        Throwable thrown = null;
        try {
            flushInternal();
        } catch (Throwable e) {
            thrown = e;
        }
        buf = null;

        try {
            out.close();
        } catch (Throwable e) {
            if (thrown == null) {
                thrown = e;
            }
        }
        out = null;

        if (thrown != null) {
            if (thrown instanceof IOException) {
                throw (IOException) thrown;
            } else if (thrown instanceof RuntimeException) {
                throw (RuntimeException) thrown;
            } else {
                throw new IOException(thrown);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        flushInternal();
        out.flush();
    }

    private void flushInternal() throws IOException {
        if (pos > 0) {
            out.write(buf, 0, pos);
        }
        pos = 0;
    }

    private boolean isClosed() {
        return out == null;
    }

    public void newLine() throws IOException {
        write(lineSeparator, 0, lineSeparator.length());
    }

    @Override
    public void write(char[] cbuf, int offset, int count) throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        if (offset < 0 || offset > Objects.requireNonNull(cbuf).length - count || count < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (pos == 0 && count >= this.buf.length) {
            out.write(cbuf, offset, count);
            return;
        }
        int available = this.buf.length - pos;
        if (count < available) {
            available = count;
        }
        if (available > 0) {
            System.arraycopy(cbuf, offset, this.buf, pos, available);
            pos += available;
        }
        if (pos == this.buf.length) {
            out.write(this.buf, 0, this.buf.length);
            pos = 0;
            if (count > available) {
                offset += available;
                available = count - available;
                if (available >= this.buf.length) {
                    out.write(cbuf, offset, available);
                    return;
                }

                System.arraycopy(cbuf, offset, this.buf, pos, available);
                pos += available;
            }
        }
    }

    @Override
    public void write(int oneChar) throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        if (pos >= buf.length) {
            out.write(buf, 0, buf.length);
            pos = 0;
        }
        buf[pos++] = (char) oneChar;
    }

    @Override
    public void write(String str, int offset, int count) throws IOException {
        if (isClosed()) {
            throw new IOException();
        }
        if (count <= 0) {
            return;
        }
        if (offset > Objects.requireNonNull(str).length() - count || offset < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        if (pos == 0 && count >= buf.length) {
            char[] chars = new char[count];
            str.getChars(offset, offset + count, chars, 0);
            out.write(chars, 0, count);
            return;
        }
        int available = buf.length - pos;
        if (count < available) {
            available = count;
        }
        if (available > 0) {
            str.getChars(offset, offset + available, buf, pos);
            pos += available;
        }
        if (pos == buf.length) {
            out.write(this.buf, 0, this.buf.length);
            pos = 0;
            if (count > available) {
                offset += available;
                available = count - available;
                if (available >= buf.length) {
                    char[] chars = new char[count];
                    str.getChars(offset, offset + available, chars, 0);
                    out.write(chars, 0, available);
                    return;
                }
                str.getChars(offset, offset + available, buf, pos);
                pos += available;
            }
        }
    }
}
