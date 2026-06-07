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
package org.teavm.junit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.teavm.classlib.impl.console.StderrOutputStream;
import org.teavm.interop.Import;
import org.teavm.interop.c.Include;

final class TestNativeEntryPoint {
    private TestNativeEntryPoint() {
    }

    public static void main(String[] args) {
        try {
            TestEntryPoint.run(args.length > 0 ? args[0] : null);
            exitProcess(0);
        } catch (Throwable e) {
            String exceptionFilePath = getenv("TEAVM_TEST_EXCEPTION_FILE");
            if (exceptionFilePath != null) {
                try (var os = new FileOutputStream(exceptionFilePath);
                     var ps = new PrintStream(os)) {
                    printStructuredException(ps, e);
                } catch (IOException ignored) {
                    // best-effort; CRunStrategy falls back to RuntimeException
                }
            } else {
                PrintStream err = new PrintStream(StderrOutputStream.INSTANCE);
                e.printStackTrace(err);
            }
            exitProcess(1);
        }
    }

    private static void printStructuredException(PrintStream out, Throwable e) {
        out.println("TEAVM_EXCEPTION_START");
        Throwable current = e;
        int depth = 0;
        while (current != null && depth < 20) {
            out.println("TEAVM_CLASS:" + current.getClass().getName());
            String message = current.getMessage();
            out.println("TEAVM_MESSAGE:" + (message == null ? "" : escapeMessage(message)));
            for (StackTraceElement frame : current.getStackTrace()) {
                out.println("TEAVM_AT:" + frame);
            }
            current = current.getCause();
            if (current != null) {
                out.println("TEAVM_CAUSE");
            }
            depth++;
        }
        out.println("TEAVM_EXCEPTION_END");
        out.flush();
    }

    private static String escapeMessage(String s) {
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Include("stdlib.h")
    @Import(name = "exit")
    private static native void exitProcess(int code);

    @Include("stdlib.h")
    @Import(name = "getenv")
    private static native String getenv(String name);
}
