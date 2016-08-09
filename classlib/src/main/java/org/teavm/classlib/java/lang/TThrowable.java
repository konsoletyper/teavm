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
package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.io.TPrintStream;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.interop.Remove;
import org.teavm.interop.Rename;
import org.teavm.interop.Superclass;

/**
 *
 * @author Alexey Andreev
 */
@Superclass("java.lang.Object")
public class TThrowable extends RuntimeException {
    private static final long serialVersionUID = 2026791432677149320L;
    private TString message;
    private TThrowable cause;
    private boolean suppressionEnabled;
    private boolean writableStackTrace;
    private TThrowable[] suppressed = new TThrowable[0];

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    protected TThrowable(TString message, TThrowable cause, boolean enableSuppression, boolean writableStackTrace) {
    }

    @Rename("<init>")
    public void init(TString message, TThrowable cause, boolean enableSuppression, boolean writableStackTrace) {
        if (writableStackTrace) {
            fillInStackTrace();
        }
        this.suppressionEnabled = enableSuppression;
        this.writableStackTrace = writableStackTrace;
        this.message = message;
        this.cause = cause;
    }

    @Rename("fakeInit")
    public TThrowable() {
    }

    @Rename("<init>")
    private void init() {
        this.suppressionEnabled = true;
        this.writableStackTrace = true;
        fillInStackTrace();
    }

    @Rename("fakeInit")
    public TThrowable(@SuppressWarnings("unused") TString message) {
    }

    @Rename("<init>")
    private void init(TString message) {
        this.suppressionEnabled = true;
        this.writableStackTrace = true;
        fillInStackTrace();
        this.message = message;
    }

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    public TThrowable(TString message, TThrowable cause) {
    }

    @Rename("<init>")
    private void init(TString message, TThrowable cause) {
        this.suppressionEnabled = true;
        this.writableStackTrace = true;
        fillInStackTrace();
        this.message = message;
        this.cause = cause;
    }

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    public TThrowable(TThrowable cause) {
    }

    @Rename("<init>")
    private void init(TThrowable cause) {
        this.suppressionEnabled = true;
        this.writableStackTrace = true;
        fillInStackTrace();
        this.cause = cause;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Rename("getMessage")
    public TString getMessage0() {
        return message;
    }

    @Rename("getLocalizedMessage")
    public TString getLocalizedMessage0() {
        return TString.wrap(getMessage());
    }

    @Override
    public TThrowable getCause() {
        return cause != this ? cause : null;
    }

    @Remove
    public native TClass<?> getClass0();

    @Remove
    public native TString toString0();

    public TThrowable initCause(TThrowable cause) {
        if (this.cause != this && this.cause != null) {
            throw new TIllegalStateException(TString.wrap("Cause already set"));
        }
        if (cause == this) {
            throw new TIllegalArgumentException(TString.wrap("Circular causation relation"));
        }
        this.cause = cause;
        return this;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(TSystem.err);
    }

    public void printStackTrace(TPrintStream stream) {
        stream.println(TString.wrap(getClass().getName() + ": " + getMessage()));
    }

    @Rename("getStackTrace")
    public TStackTraceElement[] getStackTrace0() {
        return new TStackTraceElement[0];
    }

    public void setStackTrace(@SuppressWarnings("unused") TStackTraceElement[] stackTrace) {
        // do nothing
    }

    @Rename("getSuppressed")
    public final TThrowable[] getSuppressed0() {
        return TArrays.copyOf(suppressed, suppressed.length);
    }

    public final void addSuppressed(TThrowable exception) {
        if (!suppressionEnabled) {
            return;
        }
        suppressed = TArrays.copyOf(suppressed, suppressed.length + 1);
        suppressed[suppressed.length - 1] = exception;
    }
}
