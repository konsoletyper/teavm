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

public class TCharArrayWriter extends Writer {
    protected char[] buf;
    protected int count;

    public TCharArrayWriter() {
        buf = new char[32];
        lock = buf;
    }

    public TCharArrayWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException();
        }
        buf = new char[initialSize];
        lock = buf;
    }

    @Override
    public void close() {
        /* empty */
    }

    private void expand(int i) {
        /* Can the buffer handle @i more chars, if not expand it */
        if (count + i <= buf.length) {
            return;
        }

        int newLen = Math.max(2 * buf.length, count + i);
        char[] newbuf = new char[newLen];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
    }

    @Override
    public void flush() {
        /* empty */
    }

    public void reset() {
        count = 0;
    }

    public int size() {
        return count;
    }

    public char[] toCharArray() {
        char[] result = new char[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    @Override
    public void write(char[] c, int offset, int len) {
        // avoid int overflow
        if (offset < 0 || offset > c.length || len < 0 || len > c.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        expand(len);
        System.arraycopy(c, offset, this.buf, this.count, len);
        this.count += len;
    }

    @Override
    public void write(int oneChar) {
        expand(1);
        buf[count++] = (char) oneChar;
    }

    @Override
    public void write(String str, int offset, int len) {
        if (str == null) {
            throw new NullPointerException();
        }
        // avoid int overflow
        if (offset < 0 || offset > str.length() || len < 0 || len > str.length() - offset) {
            throw new StringIndexOutOfBoundsException();
        }
        expand(len);
        str.getChars(offset, offset + len, buf, this.count);
        this.count += len;
    }

    public void writeTo(Writer out) throws IOException {
        out.write(buf, 0, count);
    }

    @Override
    public TCharArrayWriter append(char c) {
        write(c);
        return this;
    }

    @Override
    public TCharArrayWriter append(CharSequence csq) {
        if (null == csq) {
            append("NULL", 0, 4);
        } else {
            append(csq, 0, csq.length());
        }
        return this;
    }

    @Override
    public TCharArrayWriter append(CharSequence csq, int start, int end) {
        if (null == csq) {
            csq = "NULL";
        }
        String output = csq.subSequence(start, end).toString();
        write(output, 0, output.length());
        return this;
    }
}
