/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import org.teavm.jso.JSBody;

public final class ThrowableContract {
    private ThrowableContract() {
    }

    public static void main(String[] args) {
        captureContract();
        writableContract();
        System.out.println("Throwable contract passed");
    }

    private static void captureContract() {
        var exception = new Exception("fresh");
        var stack = exception.getStackTrace();
        check(stack.length > 0, "fresh exception has a captured stack");
        check(stack[0] != null, "captured frame is non-null");
        if (isJavaScript()) {
            check("<JS>".equals(stack[0].getClassName()), "native frames are labeled as JavaScript");
            check(stack[0].getMethodName() != null, "native frame has an honest function label");
            check(parseV8AndFirefoxFrames("Error: contains 123\n"
                    + "    at sampleV8 (https://example.test/app.js:21:5)\n"
                    + "sampleFirefox@https://example.test/app.js:33:2"),
                    "V8 and Firefox stack lines are parsed with source positions");
            check(ignoresErrorHeader(), "error message headers are not treated as frames");
        }

        var first = new StackTraceElement("Fixture", "first", "Fixture.java", 10);
        var second = new StackTraceElement("Fixture", "second", "Fixture.java", 20);
        StackTraceElement[] supplied = { first, second };
        exception.setStackTrace(supplied);
        supplied[0] = second;
        check("first".equals(exception.getStackTrace()[0].getMethodName()), "setStackTrace copies input");
        StackTraceElement[] returned = exception.getStackTrace();
        returned[0] = second;
        check("first".equals(exception.getStackTrace()[0].getMethodName()), "getStackTrace returns a copy");

        try {
            exception.setStackTrace(null);
            throw new AssertionError("setStackTrace(null) must fail");
        } catch (NullPointerException expected) {
            // Expected.
        }
        try {
            exception.setStackTrace(new StackTraceElement[] { first, null });
            throw new AssertionError("setStackTrace with a null frame must fail");
        } catch (NullPointerException expected) {
            // Expected.
        }

        check(exception.fillInStackTrace() == exception, "fillInStackTrace returns the same throwable");
        check(exception.getStackTrace().length > 0, "fillInStackTrace refreshes the captured stack");
    }

    private static void writableContract() {
        var exception = new DisabledStackException();
        check(exception.getStackTrace().length == 0, "disabled writable stack starts empty");
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("Fixture", "ignored", null, -1)
        });
        exception.fillInStackTrace();
        check(exception.getStackTrace().length == 0, "disabled writable stack ignores updates");
        try {
            exception.setStackTrace(new StackTraceElement[] { null });
            throw new AssertionError("disabled stack still validates null frames");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static boolean isJavaScript() {
        return "TeaVM".equals(System.getProperty("os.name"));
    }

    @JSBody(params = "stack", script = "var f=$rt_parseNativeStack(stack);"
            + "return f.length===2&&f[0].methodName==='sampleV8'&&f[0].lineNumber===21"
            + "&&f[1].methodName==='sampleFirefox'&&f[1].lineNumber===33;")
    private static native boolean parseV8AndFirefoxFrames(String stack);

    @JSBody(script = "return $rt_parseNativeStack('Error: 123').length===0;")
    private static native boolean ignoresErrorHeader();

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class DisabledStackException extends Exception {
        private static final long serialVersionUID = 1L;

        DisabledStackException() {
            super("disabled", null, true, false);
        }
    }
}
