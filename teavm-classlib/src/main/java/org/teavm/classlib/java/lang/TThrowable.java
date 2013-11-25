package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.Remove;
import org.teavm.javascript.ni.Rename;
import org.teavm.javascript.ni.Superclass;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@Superclass("java.lang.Object")
public class TThrowable extends RuntimeException {
    private static final long serialVersionUID = 2026791432677149320L;
    private TString message;
    private TThrowable cause;

    public TThrowable() {
        fillInStackTrace();
    }

    public TThrowable(TString message) {
        fillInStackTrace();
        this.message = message;
    }

    public TThrowable(TString message, TThrowable cause) {
        fillInStackTrace();
        this.message = message;
        this.cause = cause;
    }

    public TThrowable(TThrowable cause) {
        this.cause = cause;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Rename("getMessage")
    public TString getMessage0() {
        return message;
    }

    @Rename("getLocalizedMessage")
    public TString getLocalizedMessage0() {
        return getMessage0();
    }

    @Override
    public synchronized TThrowable getCause() {
        return cause != this ? cause : null;
    }

    @Remove
    public native TClass<?> getClass0();

    @Remove
    public native TString toString0();

    public synchronized TThrowable initCause(TThrowable cause) {
        if (this.cause != this && this.cause != null) {
            throw new TIllegalStateException(TString.wrap("Cause already set"));
        }
        if (cause == this) {
            throw new TIllegalArgumentException(TString.wrap("Circular causation relation"));
        }
        this.cause = cause;
        return this;
    }
}
