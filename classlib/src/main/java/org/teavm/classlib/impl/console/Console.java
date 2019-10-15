/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.impl.console;

import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSBody;

public final class Console {
    private Console() {
    }

    public static void writeStderr(int b) {
        if (PlatformDetector.isC()) {
            writeC(b);
        } else if (PlatformDetector.isWebAssembly()) {
            writeWasm(b);
        } else {
            writeJs(b);
        }
    }

    public static void writeStdout(int b) {
        if (PlatformDetector.isC()) {
            writeC(b);
        } else if (PlatformDetector.isWebAssembly()) {
            writeWasm(b);
        } else {
            writeJsStdout(b);
        }
    }

    public static void writeStdout(String s) {
        for (int i = 0; i < s.length(); ++i) {
            writeStderr(s.charAt(i));
        }
    }

    @JSBody(params = "b", script = "$rt_putStderr(b);")
    private static native void writeJs(int b);

    @JSBody(params = "b", script = "$rt_putStdout(b);")
    private static native void writeJsStdout(int b);

    @Import(name = "putwchar", module = "teavm")
    private static native void writeWasm(int b);

    @Unmanaged
    @Import(name = "teavm_logchar")
    @RuntimeInclude("log.h")
    private static native void writeC(int b);
}
