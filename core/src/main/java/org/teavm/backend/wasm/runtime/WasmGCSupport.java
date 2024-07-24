/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.runtime;

import org.teavm.interop.Import;

public class WasmGCSupport {
    private WasmGCSupport() {
    }

    public static NullPointerException npe() {
        return new NullPointerException();
    }

    public static ArrayIndexOutOfBoundsException aiiobe() {
        return new ArrayIndexOutOfBoundsException();
    }

    public static ClassCastException cce() {
        return new ClassCastException();
    }

    @Import(name = "putcharStdout")
    public static native void putCharStdout(char c);

    @Import(name = "putcharStderr")
    public static native void putCharStderr(char c);
}
