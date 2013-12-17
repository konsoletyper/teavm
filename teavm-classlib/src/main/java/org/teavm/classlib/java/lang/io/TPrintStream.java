package org.teavm.classlib.java.lang.io;

import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TStringBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class TPrintStream extends TFilterOutputStream {
    private boolean autoFlush;
    private boolean errorState;
    private TStringBuilder sb = new TStringBuilder();
    private char[] buffer = new char[32];

    public TPrintStream(TOutputStream out, boolean autoFlush) {
        super(out);
        this.autoFlush = autoFlush;
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
        int[] codePoints = new int[TMath.min(s.length, 4096)];
    }

    public void print(char c) {
        buffer[0] = c;
        print(buffer, 0, 1);
    }

    public void print(int i) {
        sb.append(i);
        printSB();
    }

    private void printSB() {
        char[] buffer = sb.length() > this.buffer.length ? new char[sb.length()] : this.buffer;
        sb.getChars(0, sb.length(), buffer, 0);
        print(buffer);
        sb.setLength(0);
    }
}
