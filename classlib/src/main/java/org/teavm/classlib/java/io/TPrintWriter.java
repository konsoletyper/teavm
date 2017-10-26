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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;

public class TPrintWriter extends Writer {
    protected Writer out;
    private boolean ioError;
    private boolean autoflush;

    public TPrintWriter(OutputStream out) {
        this(new OutputStreamWriter(out), false);
    }

    public TPrintWriter(OutputStream out, boolean autoflush) {
        this(new OutputStreamWriter(out), autoflush);
    }

    public TPrintWriter(Writer wr) {
        this(wr, false);
    }

    public TPrintWriter(Writer wr, boolean autoflush) {
        super(wr);
        this.autoflush = autoflush;
        out = wr;
    }

    public boolean checkError() {
        Writer delegate = out;
        if (delegate == null) {
            return ioError;
        }

        flush();
        return ioError;
    }

    @Override
    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                setError();
            }
            out = null;
        }
    }

    @Override
    public void flush() {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                setError();
            }
        } else {
            setError();
        }
    }

    public TPrintWriter format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public TPrintWriter format(Locale l, String format, Object... args) {
        Objects.requireNonNull(format);
        new Formatter(this, l).format(format, args);
        if (autoflush) {
            flush();
        }
        return this;
    }

    public TPrintWriter printf(String format, Object... args) {
        return format(format, args);
    }

    public TPrintWriter printf(Locale l, String format, Object... args) {
        return format(l, format, args);
    }

    public void print(char[] charArray) {
        print(new String(charArray, 0, charArray.length));
    }

    public void print(char ch) {
        print(String.valueOf(ch));
    }

    public void print(double dnum) {
        print(String.valueOf(dnum));
    }

    public void print(float fnum) {
        print(String.valueOf(fnum));
    }

    public void print(int inum) {
        print(String.valueOf(inum));
    }

    public void print(long lnum) {
        print(String.valueOf(lnum));
    }

    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    public void print(String str) {
        write(str != null ? str : String.valueOf((Object) null));
    }

    public void print(boolean bool) {
        print(String.valueOf(bool));
    }

    public void println() {
        print("\n");
        if (autoflush) {
            flush();
        }
    }

    public void println(char[] charArray) {
        println(new String(charArray, 0, charArray.length));
    }

    public void println(char ch) {
        println(String.valueOf(ch));
    }

    public void println(double dnum) {
        println(String.valueOf(dnum));
    }

    public void println(float fnum) {
        println(String.valueOf(fnum));
    }

    public void println(int inum) {
        println(String.valueOf(inum));
    }

    public void println(long lnum) {
        println(String.valueOf(lnum));
    }

    public void println(Object obj) {
        println(String.valueOf(obj));
    }

    public void println(String str) {
        print(str);
        println();
    }

    public void println(boolean bool) {
        println(String.valueOf(bool));
    }

    protected void setError() {
        ioError = true;
    }

    @Override
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(char[] buf, int offset, int count) {
        doWrite(buf, offset, count);
    }

    @Override
    public void write(int oneChar) {
        doWrite(new char[] { (char) oneChar }, 0, 1);
    }

    private void doWrite(char[] buf, int offset, int count) {
        if (out != null) {
            try {
                out.write(buf, offset, count);
            } catch (IOException e) {
                setError();
            }
        } else {
            setError();
        }
    }
    @Override
    public void write(String str) {
        write(str.toCharArray());
    }

    @Override
    public void write(String str, int offset, int count) {
        write(str.substring(offset, offset + count).toCharArray());
    }

    @Override
    public TPrintWriter append(char c) {
        write(c);
        return this;
    }

    @Override
    public TPrintWriter append(CharSequence csq) {
        if (null == csq) {
            append("null", 0, 4);
        } else {
            append(csq, 0, csq.length());
        }
        return this;
    }

    @Override
    public TPrintWriter append(CharSequence csq, int start, int end) {
        if (null == csq) {
            csq = "null";
        }
        String output = csq.subSequence(start, end).toString();
        write(output, 0, output.length());
        return this;
    }
}
