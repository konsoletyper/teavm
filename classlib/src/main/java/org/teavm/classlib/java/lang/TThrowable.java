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

import java.io.PrintStream;
import java.io.PrintWriter;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.interop.Import;
import org.teavm.interop.Remove;
import org.teavm.interop.Rename;
import org.teavm.interop.Superclass;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.runtime.ExceptionHandling;

@Superclass("java.lang.Object")
public class TThrowable extends RuntimeException {
    private static final long serialVersionUID = 2026791432677149320L;
    private String message;
    private TThrowable cause;
    private boolean suppressionEnabled;
    private boolean writableStackTrace;
    private TThrowable[] suppressed = new TThrowable[0];
    private TStackTraceElement[] stackTrace;
    private LazyStackSupplier lazyStackTrace;

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    protected TThrowable(String message, TThrowable cause, boolean enableSuppression, boolean writableStackTrace) {
    }

    @Rename("<init>")
    public void init(String message, TThrowable cause, boolean enableSuppression, boolean writableStackTrace) {
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
    public TThrowable(@SuppressWarnings("unused") String message) {
    }

    @Rename("<init>")
    private void init(String message) {
        this.suppressionEnabled = true;
        this.writableStackTrace = true;
        fillInStackTrace();
        this.message = message;
    }

    @SuppressWarnings("unused")
    @Rename("fakeInit")
    public TThrowable(String message, TThrowable cause) {
    }

    @Rename("<init>")
    private void init(String message, TThrowable cause) {
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
        if (PlatformDetector.isLowLevel()) {
            stackTrace = (TStackTraceElement[]) (Object) ExceptionHandling.fillStackTrace();
        } else if (PlatformDetector.isWebAssemblyGC()) {
            lazyStackTrace = takeWasmGCStack();
            decorateException(this);
        }
        return this;
    }

    @Import(name = "decorateException")
    private static native void decorateException(Object obj);

    private void ensureStackTrace() {
        if (PlatformDetector.isWebAssemblyGC()) {
            if (lazyStackTrace != null) {
                var supplier = lazyStackTrace;
                lazyStackTrace = null;
                var nativeStack = supplier.getStack();
                if (nativeStack == null) {
                    return;
                }
                var stack = new TStackTraceElement[nativeStack.getLength()];
                for (var i = 0; i < nativeStack.getLength(); ++i) {
                    var frame = nativeStack.get(i);
                    stack[i] = new TStackTraceElement(frame.getClassName(), frame.getMethod(), frame.getFile(),
                            frame.getLine());
                }
                stackTrace = stack;
            }
        }
    }

    @Import(name = "takeStackTrace")
    private native LazyStackSupplier takeWasmGCStack();

    @Rename("getMessage")
    public String getMessage0() {
        return message;
    }

    @Rename("getLocalizedMessage")
    public String getLocalizedMessage0() {
        return getMessage();
    }

    @Remove
    public native TThrowable getCause();

    @Rename("getCause")
    public TThrowable getCause0() {
        return cause != this ? cause : null;
    }

    @Remove
    public native Class<?> getClass0();

    @Rename("toString")
    public String toString0() {
        String message = getLocalizedMessage();
        return getClass().getName() + (message != null ? ": " + message : "");
    }

    public TThrowable initCause(TThrowable cause) {
        if (this.cause != this && this.cause != null) {
            throw new IllegalStateException("Cause already set");
        }
        if (cause == this) {
            throw new IllegalArgumentException("Circular causation relation");
        }
        this.cause = cause;
        return this;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream stream) {
        stream.print(getClass().getName());
        String message = getLocalizedMessage();
        if (message != null) {
            stream.print(": " + message);
        }
        stream.println();
        ensureStackTrace();
        if (stackTrace != null) {
            for (TStackTraceElement element : stackTrace) {
                stream.print("\tat ");
                stream.println(element);
            }
        }
        if (cause != null && cause != this) {
            stream.print("Caused by: ");
            cause.printStackTrace(stream);
        }
    }

    public void printStackTrace(PrintWriter stream) {
        stream.print(getClass().getName());
        String message = getLocalizedMessage();
        if (message != null) {
            stream.print(": " + message);
        }
        stream.println();
        ensureStackTrace();
        if (stackTrace != null) {
            for (TStackTraceElement element : stackTrace) {
                stream.print("\tat ");
                stream.println(element);
            }
        }
        if (cause != null && cause != this) {
            stream.print("Caused by: ");
            cause.printStackTrace(stream);
        }
    }

    @Rename("getStackTrace")
    public TStackTraceElement[] getStackTrace0() {
        ensureStackTrace();
        return stackTrace != null ? stackTrace.clone() : new TStackTraceElement[0];
    }

    public void setStackTrace(@SuppressWarnings("unused") TStackTraceElement[] stackTrace) {
        if (PlatformDetector.isWebAssemblyGC()) {
            lazyStackTrace = null;
        }
        this.stackTrace = stackTrace.clone();
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

    interface LazyStackSupplier extends JSObject {
        StackFrames getStack();
    }

    interface StackFrames extends JSObject {
        @JSProperty
        int getLength();

        @JSIndexer
        StackFrame get(int index);
    }

    interface StackFrame extends JSObject {
        @JSProperty
        String getClassName();

        @JSProperty
        String getMethod();

        @JSProperty
        String getFile();

        @JSProperty
        int getLine();
    }
}
