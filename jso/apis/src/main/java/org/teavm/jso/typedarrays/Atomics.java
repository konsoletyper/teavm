/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.jso.typedarrays;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;

@JSClass
public class Atomics {
    public static final String WAIT_OK = "ok";
    public static final String WAIT_NOT_EQUAL = "not-equal";
    public static final String WAIT_TIMED_OUT = "timed-out";

    private Atomics() {
    }

    public static native byte add(Int8Array typedArray, int index, byte value);

    public static native short add(Int16Array typedArray, int index, short value);

    public static native int add(Int32Array typedArray, int index, int value);

    public static native long add(BigInt64Array typedArray, int index, long value);

    public static native byte and(Int8Array typedArray, int index, byte value);

    public static native short and(Int16Array typedArray, int index, short value);

    public static native int and(Int32Array typedArray, int index, int value);

    public static native long and(BigInt64Array typedArray, int index, long value);

    public static native byte compareExchange(Int8Array typedArray, int index, byte expectedValue,
            byte replacementValue);

    public static native short compareExchange(Int16Array typedArray, int index, short expectedValue,
            short replacementValue);

    public static native int compareExchange(Int32Array typedArray, int index, int expectedValue,
            int replacementValue);

    public static native int compareExchange(BigInt64Array typedArray, int index, long expectedValue,
            long replacementValue);

    public static native byte exchange(Int8Array typedArray, int index, byte value);

    public static native short exchange(Int16Array typedArray, int index, short value);

    public static native int exchange(Int32Array typedArray, int index, int value);

    public static native long exchange(BigInt64Array typedArray, int index, long value);

    public static native boolean isLockFree(int size);

    public static native byte load(Int8Array typedArray, int index);

    public static native short load(Int16Array typedArray, int index);

    public static native int load(Int32Array typedArray, int index);

    public static native long load(BigInt64Array typedArray, int index);

    public static native int notify(Int32Array typedArray, int index, int count);

    public static native int notify(BigInt64Array typedArray, int index, int count);

    public static native byte or(Int8Array typedArray, int index, byte value);

    public static native short or(Int16Array typedArray, int index, short value);

    public static native int or(Int32Array typedArray, int index, int value);

    public static native long or(BigInt64Array typedArray, int index, long value);

    public static native void pause();

    public static native byte store(Int8Array typedArray, int index, byte value);

    public static native short store(Int16Array typedArray, int index, short value);

    public static native int store(Int32Array typedArray, int index, int value);

    public static native long store(BigInt64Array typedArray, int index, long value);

    public static native byte sub(Int8Array typedArray, int index, byte value);

    public static native short sub(Int16Array typedArray, int index, short value);

    public static native int sub(Int32Array typedArray, int index, int value);

    public static native long sub(BigInt64Array typedArray, int index, long value);

    public static native String wait(Int32Array typedArray, int index, int value);

    public static native String wait(Int32Array typedArray, int index, int value, int timeout);

    public static native String wait(BigInt64Array typedArray, int index, long value);

    public static native String wait(BigInt64Array typedArray, int index, long value, int timeout);

    public static native AsyncWaitResult waitAsync(Int32Array typedArray, int index, int value);

    public static native AsyncWaitResult waitAsync(Int32Array typedArray, int index, int value, int timeout);

    public static native AsyncWaitResult waitAsync(BigInt64Array typedArray, int index, long value);

    public static native AsyncWaitResult waitAsync(BigInt64Array typedArray, int index, long value, int timeout);

    public static native byte xor(Int8Array typedArray, int index, byte value);

    public static native short xor(Int16Array typedArray, int index, short value);

    public static native int xor(Int32Array typedArray, int index, int value);

    public static native long xor(BigInt64Array typedArray, int index, long value);

    public interface AsyncWaitResult extends JSObject {
        @JSProperty
        boolean isAsync();

        @JSProperty
        JSObject getValue();

        @JSProperty("value")
        String getStringValue();

        @JSProperty("value")
        JSPromise<String> getPromiseValue();
    }
}
