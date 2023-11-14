/*
 *  Copyright 2022 Alexey Andreev.
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

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.runtime.Fiber;

public class WasmSupport {
    private WasmSupport() {
    }

    @Import(name = "putwcharsErr", module = "teavm")
    public static native void putCharsStderr(Address address, int count);

    @Import(name = "putwcharsOut", module = "teavm")
    public static native void putCharsStdout(Address address, int count);

    public static long currentTimeMillis() {
        return (long) currentTimeMillisImpl();
    }

    @Import(name = "currentTimeMillis", module = "teavm")
    private static native double currentTimeMillisImpl();

    @Import(name = "print", module = "spectest")
    public static native void print(int a);

    @Import(name = "logString", module = "teavm")
    public static native void printString(String s);

    @Import(name = "logInt", module = "teavm")
    public static native void printInt(int i);

    @Import(name = "logOutOfMemory", module = "teavm")
    public static native void printOutOfMemory();

    @Import(name = "init", module = "teavmHeapTrace")
    public static native void initHeapTrace(int maxHeap);

    public static String[] getArgs() {
        return new String[0];
    }

    @Import(module = "teavmMath", name = "random")
    public static native double random();

    @Import(module = "teavmMath", name = "pow")
    public static native double pow(double x, double y);

    private static void initClasses() {
    }

    public static void runWithoutArgs() {
        initClasses();
        Fiber.start(() -> Fiber.runMain(getArgs()), false);
    }

    public static void runWithArgs(String[] args) {
        initClasses();
        Fiber.start(() -> Fiber.runMain(args), false);
    }
}
