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
package org.teavm.backend.wasm;

import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@StaticInit
public final class WasmRuntime {
    private WasmRuntime() {
    }

    @Unmanaged
    public static int compare(int a, int b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compareUnsigned(int a, int b) {
        return gtu(a, b) ? 1 : ltu(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compareUnsigned(long a, long b) {
        return gtu(a, b) ? 1 : ltu(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compare(long a, long b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compare(float a, float b) {
        return a == b ? 0 : lt(a, b) ? -1 : 1;
    }

    @Unmanaged
    public static int compareLess(float a, float b) {
        return a == b ? 0 : gt(a, b) ? 1 : -1;
    }

    @Unmanaged
    public static int compare(double a, double b) {
        return a == b ? 0 : lt(a, b) ? -1 : 1;
    }

    @Unmanaged
    public static int compareLess(double a, double b) {
        return a == b ? 0 : gt(a, b) ? 1 : -1;
    }

    @Unmanaged
    public static native float min(float a, float b);

    @Unmanaged
    public static native double min(double a, double b);

    @Unmanaged
    public static native float max(float a, float b);

    @Unmanaged
    public static native double max(double a, double b);

    @Unmanaged
    public static float remainder(float a, float b) {
        return a - (float) (int) (a / b) * b;
    }

    @Unmanaged
    public static double remainder(double a, double b) {
        return a - (double) (long) (a / b) * b;
    }

    @Unmanaged
    private static native boolean lt(int a, int b);

    @Unmanaged
    private static native boolean gt(int a, int b);

    @Unmanaged
    private static native boolean ltu(int a, int b);

    @Unmanaged
    private static native boolean gtu(int a, int b);

    @Unmanaged
    private static native boolean lt(long a, long b);

    @Unmanaged
    private static native boolean gt(long a, long b);

    @Unmanaged
    private static native boolean ltu(long a, long b);

    @Unmanaged
    private static native boolean gtu(long a, long b);

    @Unmanaged
    private static native boolean lt(float a, float b);

    @Unmanaged
    private static native boolean gt(float a, float b);

    @Unmanaged
    private static native boolean lt(double a, double b);

    @Unmanaged
    private static native boolean gt(double a, double b);

}
