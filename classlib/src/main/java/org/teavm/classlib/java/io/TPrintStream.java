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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetEncoder;
import org.teavm.classlib.java.nio.charset.TCodingErrorAction;
import org.teavm.classlib.java.nio.charset.TIllegalCharsetNameException;
import org.teavm.classlib.java.nio.charset.TUnsupportedCharsetException;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;
import org.teavm.classlib.java.util.TFormatter;

public class TPrintStream extends TFilterOutputStream implements Appendable {
    private boolean autoFlush;
    private boolean errorState;
    private TStringBuilder sb = new TStringBuilder();
    private char[] buffer = new char[32];
    private TCharset charset;

    public TPrintStream(OutputStream out, boolean autoFlush, String encoding) throws TUnsupportedEncodingException {
        super(out);
        this.autoFlush = autoFlush;
        try {
            charset = TCharset.forName(encoding.toString());
        } catch (TUnsupportedCharsetException | TIllegalCharsetNameException e) {
            throw new TUnsupportedEncodingException(encoding);
        }
    }

    public TPrintStream(OutputStream out, boolean autoFlush) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = TUTF8Charset.INSTANCE;
    }

    public TPrintStream(OutputStream out, boolean autoFlush, TCharset charset) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = charset;
    }

    public TPrintStream(OutputStream out, TCharset charset) {
        this(out, false, charset);
    }

    public TPrintStream(OutputStream out) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
            errorState = true;
        }
    }

    @Override
    public void close() {
        if (!checkError()) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            errorState = true;
        } finally {
            out = null;
        }
    }

    @Override
    public void flush() {
        if (!check()) {
            return;
        }
        try {
            out.flush();
        } catch (IOException e) {
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

    private void print(CharSequence s, int begin, int end) {
        printCharBuffer(TCharBuffer.wrap(s, begin, end), begin, end);
    }

    private void print(char[] s, int begin, int end) {
        printCharBuffer(TCharBuffer.wrap(s, begin, end - begin), begin, end);
    }

    private void printCharBuffer(TCharBuffer src, int begin, int end) {
        byte[] destBytes = new byte[TMath.max(16, TMath.min(end - begin, 1024))];
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

    public void print(String s) {
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

    public void println(float d) {
        sb.append(d).append('\n');
        printSB();
    }

    public void println(char c) {
        sb.append(c).append('\n');
        printSB();
    }

    public void println(boolean b) {
        sb.append(b).append('\n');
        printSB();
    }

    public void println(String s) {
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

    public TPrintStream printf(String format, Object... args) {
        return format(format, args);
    }

    public TPrintStream printf(Locale locale, String format, Object... args) {
        return format(locale, format, args);
    }

    public TPrintStream format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public TPrintStream format(Locale locale, String format, Object... args) {
        if (args == null) {
            args = new Object[1];
        }
        try (var formatter = new TFormatter(this, locale)) {
            formatter.format(format, args);
            if (formatter.ioException() != null) {
                errorState = true;
            }
        }
        return this;
    }

    private void printSB() {
        char[] buffer = sb.length() > this.buffer.length ? new char[sb.length()] : this.buffer;
        sb.getChars(0, sb.length(), buffer, 0);
        print(buffer, 0, sb.length());
        sb.setLength(0);
    }

    @Override
    public TPrintStream append(CharSequence csq) {
        if (csq != null) {
            print(csq, 0, csq.length());
        } else {
            print("null");
        }
        return this;
    }

    @Override
    public TPrintStream append(CharSequence csq, int start, int end) {
        print(csq == null ? "null" : csq, start, end);
        return this;
    }

    @Override
    public TPrintStream append(char c) {
        print(c);
        return this;
    }
}
