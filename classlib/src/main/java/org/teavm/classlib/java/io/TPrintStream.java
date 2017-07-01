/*
 *  Copyright 2013 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.*;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

public class TPrintStream extends TFilterOutputStream {
    private boolean autoFlush;
    private boolean errorState;
    private TStringBuilder sb = new TStringBuilder();
    private char[] buffer = new char[32];
    private TCharset charset;

    public TPrintStream(TOutputStream out, boolean autoFlush, TString encoding) throws TUnsupportedEncodingException {
        super(out);
        this.autoFlush = autoFlush;
        try {
            charset = TCharset.forName(encoding.toString());
        } catch (TUnsupportedCharsetException | TIllegalCharsetNameException e) {
            throw new TUnsupportedEncodingException(encoding);
        }
    }

    public TPrintStream(TOutputStream out, boolean autoFlush) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = new TUTF8Charset();
    }

    public TPrintStream(TOutputStream out) {
        this(out, false);
    }

    public boolean checkError() {
        flush();
        return errorState;
    }

    protected void setError() {
        errorState = true;
    }

    protected void clearError() {
        errorState = false;
    }

    @Override
    public void write(int b) {
        if (!check()) {
            return;
        }
        try {
            out.write(b);
        } catch (TIOException e) {
            errorState = true;
        }
        if (autoFlush && !errorState) {
            flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (!check()) {
            return;
        }
        try {
            out.write(b, off, len);
        } catch (TIOException e) {
            errorState = true;
        }
    }

    @Override
    public void close() throws TIOException {
        if (!checkError()) {
            return;
        }
        try {
            out.close();
        } catch (TIOException e) {
            errorState = true;
        } finally {
            out = null;
        }
    }

    @Override
    public void flush() throws TIOException {
        if (!check()) {
            return;
        }
        try {
            out.flush();
        } catch (TIOException e) {
            errorState = true;
        }
    }

    private boolean check() {
        if (out == null) {
            errorState = true;
        }
        return !errorState;
    }

    public void print(char[] s) {
        print(s, 0, s.length);
    }

    private void print(char[] s, int begin, int end) {
        TCharBuffer src = TCharBuffer.wrap(s, begin, end - begin);
        byte[] destBytes = new byte[TMath.max(16, TMath.min(s.length, 1024))];
        TByteBuffer dest = TByteBuffer.wrap(destBytes);
        TCharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(TCodingErrorAction.REPLACE)
                .onUnmappableCharacter(TCodingErrorAction.REPLACE);
        while (true) {
            boolean overflow = encoder.encode(src, dest, true).isOverflow();
            write(destBytes, 0, dest.position());
            dest.clear();
            if (!overflow) {
                break;
            }
        }
        while (true) {
            boolean overflow = encoder.flush(dest).isOverflow();
            write(destBytes, 0, dest.position());
            dest.clear();
            if (!overflow) {
                break;
            }
        }
    }

    public void print(char c) {
        buffer[0] = c;
        print(buffer, 0, 1);
    }

    public void print(int i) {
        sb.append(i);
        printSB();
    }

    public void print(long l) {
        sb.append(l);
        printSB();
    }

    public void print(double d) {
        sb.append(d);
        printSB();
    }

    public void print(TString s) {
        sb.append(s);
        printSB();
    }

    public void print(TObject s) {
        sb.append(s);
        printSB();
    }

    public void println(int i) {
        sb.append(i).append('\n');
        printSB();
    }

    public void println(long l) {
        sb.append(l).append('\n');
        printSB();
    }

    public void println(double d) {
        sb.append(d).append('\n');
        printSB();
    }

    public void println(TString s) {
        sb.append(s).append('\n');
        printSB();
    }

    public void println(TObject s) {
        sb.append(s).append('\n');
        printSB();
    }

    public void println() {
        print('\n');
    }

    private void printSB() {
        char[] buffer = sb.length() > this.buffer.length ? new char[sb.length()] : this.buffer;
        sb.getChars(0, sb.length(), buffer, 0);
        print(buffer, 0, sb.length());
        sb.setLength(0);
    }
}
