/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.lang.reflect;

import org.teavm.classlib.java.lang.TRuntimeException;

/**
 * Thrown by a method invocation on a proxy instance if its invocation handler's
 * {@link TInvocationHandler#invoke} method throws a checked exception that is
 * not assignable to any of the exception types declared in the throws clause
 * of the method that was invoked on the proxy instance and dispatched to the
 * invocation handler.
 */
public class TUndeclaredThrowableException extends TRuntimeException {
    private Throwable undeclaredThrowable;

    public TUndeclaredThrowableException(Throwable cause) {
        super(cause);
        this.undeclaredThrowable = cause;
    }

    public TUndeclaredThrowableException(Throwable cause, String message) {
        super(message, cause);
        this.undeclaredThrowable = cause;
    }

    public Throwable getUndeclaredThrowable() {
        return undeclaredThrowable;
    }

    @Override
    public Throwable getCause() {
        return undeclaredThrowable;
    }
}
