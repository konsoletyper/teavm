/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.jso.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExceptions;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSError;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ExceptionsTest {
    @Test
    public void throwExceptionThroughJSCode() {
        JSRunnable[] actions = new JSRunnable[] {
                () -> {
                    throw new CustomException1();
                },
                () -> {
                    throw new CustomException2();
                }
        };

        StringBuilder sb = new StringBuilder();
        for (JSRunnable action : actions) {
            try {
                runJsCode(action);
            } catch (RuntimeException e) {
                sb.append(e.getMessage());
            }
        }

        assertEquals("foobar", sb.toString());
    }

    @Test
    public void catchNativeException() {
        StringBuilder sb = new StringBuilder();
        JSError.catchNative(() -> {
            throwNativeException();
            return null;
        }, e -> {
            sb.append("caught");
            assertTrue("Should catch Error", JSError.isError(e));

            JSError error = (JSError) e;
            assertEquals("foo", error.getMessage());
            return null;
        });
        assertEquals("caught", sb.toString());
    }

    @Test
    public void catchThrowableAsNativeException() {
        JSRunnable[] actions = new JSRunnable[] {
                () -> {
                    throw new CustomException1();
                },
                () -> {
                    throw new CustomException2();
                }
        };

        StringBuilder sb = new StringBuilder();
        for (JSRunnable action : actions) {
            JSError.catchNative(() -> {
                runJsCode(action);
                return null;
            }, e -> {
                Throwable t = JSExceptions.getJavaException(e);
                sb.append(t.getMessage());
                return null;
            });
        }

        assertEquals("foobar", sb.toString());
    }

    @Test
    public void catchNativeExceptionAsRuntimeException() {
        StringBuilder sb = new StringBuilder();
        try {
            throwNativeException();
        } catch (RuntimeException e) {
            sb.append(e.getMessage());
        }

        assertEquals("(JavaScript) Error: foo", sb.toString());
    }

    @JSBody(params = "runnable", script = "runnable();")
    private static native void runJsCode(JSRunnable runnable);

    @JSBody(script = "throw new Error('foo');")
    private static native void throwNativeException();

    @JSFunctor
    interface JSRunnable extends JSObject {
        void run();
    }

    static class CustomException1 extends RuntimeException {
        @Override
        public String getMessage() {
            return "foo";
        }
    }

    static class CustomException2 extends RuntimeException {
        @Override
        public String getMessage() {
            return "bar";
        }
    }
}
