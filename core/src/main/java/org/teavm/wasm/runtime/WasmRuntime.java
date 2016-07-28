/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.wasm.runtime;

import org.teavm.interop.Import;

public final class WasmRuntime {
    private WasmRuntime() {
    }

    public static int compare(int a, int b) {
        return a > b ? 1 : a < b ? -1 : 0;
    }

    public static int compare(long a, long b) {
        return a > b ? 1 : a < b ? -1 : 0;
    }

    public static int compare(float a, float b) {
        return a > b ? 1 : a < b ? -1 : 0;
    }

    public static int compare(double a, double b) {
        return a > b ? 1 : a < b ? -1 : 0;
    }

    public static float remainder(float a, float b) {
        return a - (float) (int) (a / b) * b;
    }

    public static double remainder(double a, double b) {
        return a - (double) (long) (a / b) * b;
    }

    @Import(name = "print.i32")
    public static native void print(int a);
}
