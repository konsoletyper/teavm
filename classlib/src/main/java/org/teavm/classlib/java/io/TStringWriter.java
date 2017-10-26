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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

public class TStringWriter extends Writer {
    private StringBuffer buf;

    public TStringWriter() {
        buf = new StringBuffer(16);
        lock = buf;
    }

    public TStringWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException();
        }
        buf = new StringBuffer(initialSize);
        lock = buf;
    }

    @Override
    public void close() throws IOException {
        /* empty */
    }

    @Override
    public void flush() {
        /* empty */
    }

    public StringBuffer getBuffer() {
        return buf;
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    @Override
    public void write(char[] cbuf, int offset, int count) {
        if (offset < 0 || offset > cbuf.length || count < 0 || count > cbuf.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        if (count == 0) {
            return;
        }
        buf.append(cbuf, offset, count);
    }

    @Override
    public void write(int oneChar) {
        buf.append((char) oneChar);
    }

    @Override
    public void write(String str) {
        buf.append(str);
    }

    @Override
    public void write(String str, int offset, int count) {
        String sub = str.substring(offset, offset + count);
        buf.append(sub);
    }

    @Override
    public TStringWriter append(char c) {
        write(c);
        return this;
    }

    @Override
    public TStringWriter append(CharSequence csq) {
        if (null == csq) {
            write("null");
        } else {
            write(csq.toString());
        }
        return this;
    }

    @Override
    public TStringWriter append(CharSequence csq, int start, int end) {
        if (null == csq) {
            csq = "null";
        }
        String output = csq.subSequence(start, end).toString();
        write(output, 0, output.length());
        return this;
    }
}
