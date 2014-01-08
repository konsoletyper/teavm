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

    @Rename("fakeInit")
    public TThrowable() {
    }

    @Rename("<init>")
    private void init() {
        fillInStackTrace();
    }

    @Rename("fakeInit")
    public TThrowable(@SuppressWarnings("unused") TString message) {
    }

    @Rename("<init>")
    private void init(TString message) {
        fillInStackTrace();
        this.message = message;
    }

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    public TThrowable(TString message, TThrowable cause) {
    }

    @Rename("<init>")
    private void init(TString message, TThrowable cause) {
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
        fillInStackTrace();
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
        return TString.wrap(getMessage());
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
