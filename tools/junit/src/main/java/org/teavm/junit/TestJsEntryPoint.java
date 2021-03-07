/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.junit;

import org.teavm.jso.JSBody;

final class TestJsEntryPoint {
    private TestJsEntryPoint() {
    }

    public static void main(String[] args) throws Throwable {
        try {
            TestEntryPoint.run(args.length > 0 ? args[0] : null);
        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder();
            printStackTrace(e, sb);
            saveJavaException(sb.toString());
            throw e;
        }
    }

    private static void printStackTrace(Throwable e, StringBuilder stream) {
        stream.append(e.getClass().getName());
        String message = e.getLocalizedMessage();
        if (message != null) {
            stream.append(": " + message);
        }
        stream.append("\n");
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                stream.append("\tat ");
                stream.append(element).append("\n");
            }
        }
        if (e.getCause() != null && e.getCause() != e) {
            stream.append("Caused by: ");
            printStackTrace(e.getCause(), stream);
        }
    }

    @JSBody(params = "e", script = "window.teavmException = e")
    private static native void saveJavaException(String e);
}
