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
package org.teavm.common;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author Alexey Andreev
 */
public class CommutatedWriter extends Writer {
    private ThreadLocal<StringBuilder> buffer = new ThreadLocal<>();
    private Writer innerWriter;

    public CommutatedWriter(Writer innerWriter) {
        this.innerWriter = innerWriter;
    }

    private StringBuilder getBuffer() {
        if (buffer.get() == null) {
            buffer.set(new StringBuilder());
        }
        return buffer.get();
    }

    @Override
    public void write(int c) throws IOException {
        getBuffer().append(c);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        getBuffer().append(cbuf);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        getBuffer().append(cbuf, off, len);
    }

    @Override
    public void write(String str) throws IOException {
        getBuffer().append(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        getBuffer().append(str, off, len + off);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        getBuffer().append(csq);
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        getBuffer().append(csq, start, end);
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        getBuffer().append(c);
        return this;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        StringBuilder sb = getBuffer();
        innerWriter.write(sb.toString());
        buffer.set(null);
    }
}
