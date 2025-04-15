/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.impl;

import java.lang.reflect.Array;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.PlatformDetector;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSBigInt;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.BigInt64Array;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;

public final class JS {
    private JS() {
    }

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject arrayData(Object array);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "concatArray", module = "teavmJso")
    public static native JSObject concatArray(JSObject a, JSObject b);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native byte[] dataToByteArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native char[] dataToCharArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native short[] dataToShortArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native int[] dataToIntArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native long[] dataToLongArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native float[] dataToFloatArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native double[] dataToDoubleArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject[] dataToArray(JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapByte", module = "teavmJso")
    public static native JSObject wrap(byte value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapShort", module = "teavmJso")
    public static native JSObject wrap(short value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapInt", module = "teavmJso")
    public static native JSObject wrap(int value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapLong", module = "teavmJso")
    public static native JSObject wrap(long value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapChar", module = "teavmJso")
    public static native JSObject wrap(char value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapFloat", module = "teavmJso")
    public static native JSObject wrap(float value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapDouble", module = "teavmJso")
    public static native JSObject wrap(double value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "wrapBoolean", module = "teavmJso")
    public static native JSObject wrap(boolean value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject wrap(String value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapByte", module = "teavmJso")
    public static native byte unwrapByte(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapChar", module = "teavmJso")
    public static native char unwrapCharacter(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapShort", module = "teavmJso")
    public static native short unwrapShort(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapInt", module = "teavmJso")
    public static native int unwrapInt(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapLong", module = "teavmJso")
    public static native long unwrapLong(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapFloat", module = "teavmJso")
    public static native float unwrapFloat(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapDouble", module = "teavmJso")
    public static native double unwrapDouble(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "unwrapBoolean", module = "teavmJso")
    public static native boolean unwrapBoolean(JSObject value);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    public static native String unwrapString(JSObject value);

    public static <T extends JSObject> JSObject wrap(T[] array) {
        if (array == null) {
            return null;
        }
        var result = new JSArray<T>(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static <T> JSObject wrap(T[] array) {
        if (array == null) {
            return null;
        }
        var result = new JSArray<T>(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static <T extends JSObject> WrapFunction<T[], JSObject> arrayWrapper() {
        return JS::wrap;
    }

    public static <T extends JSObject, S> JSObject map(S[] array, WrapFunction<S, T> f) {
        if (array == null) {
            return null;
        }
        var result = new JSArray<T>(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, f.apply(array[i]));
        }
        return result;
    }

    public interface WrapFunction<S, T extends JSObject> {
        T apply(S obj);
    }

    public interface UnwrapFunction<S extends JSObject, T> {
        T apply(S obj);
    }

    public static <T extends JSObject, S> WrapFunction<S[], JSObject> arrayMapper(WrapFunction<S, T> f) {
        return array -> map(array, f);
    }

    public static JSObject wrap(boolean[] array) {
        if (array == null) {
            return null;
        }
        var result = new JSArray<JSBoolean>(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSBoolean.valueOf(array[i]));
        }
        return result;
    }

    public static WrapFunction<boolean[], JSObject> booleanArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(byte[] array) {
        if (array == null) {
            return null;
        }
        var result = new Int8Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            for (var i = 0; i < array.length; i += WasmBufferUtil.BUFFER_SIZE) {
                var upper = Math.min(array.length, i + WasmBufferUtil.BUFFER_SIZE);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putByte(array[i + j]);
                    ptr = ptr.add(1);
                }
                var dest = new Int8Array(result.getBuffer(), result.getByteOffset() + i, sz);
                var src = new Int8Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<byte[], JSObject> byteArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(short[] array) {
        if (array == null) {
            return null;
        }
        var result = new Int16Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 2;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putShort(array[i + j]);
                    ptr = ptr.add(2);
                }
                var dest = new Int16Array(result.getBuffer(), result.getByteOffset() + i * 2, sz);
                var src = new Int16Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<short[], JSObject> shortArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(char[] array) {
        if (array == null) {
            return null;
        }
        var result = new Uint16Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 2;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putChar(array[i + j]);
                    ptr = ptr.add(2);
                }
                var dest = new Uint16Array(result.getBuffer(), result.getByteOffset() + i * 2, sz);
                var src = new Uint16Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<char[], JSObject> charArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(int[] array) {
        if (array == null) {
            return null;
        }
        var result = new Int32Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 4;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putInt(array[i + j]);
                    ptr = ptr.add(4);
                }
                var dest = new Int32Array(result.getBuffer(), result.getByteOffset() + i * 4, sz);
                var src = new Int32Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<int[], JSObject> intArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(long[] array) {
        if (array == null) {
            return null;
        }
        var result = new BigInt64Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 8;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putLong(array[i + j]);
                    ptr = ptr.add(8);
                }
                var dest = new BigInt64Array(result.getBuffer(), result.getByteOffset() + i * 8, sz);
                var src = new BigInt64Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<long[], JSObject> longArrayWrapper() {
        return JS::wrap;
    }

    @NoSideEffects
    public static JSObject wrap(String[] array) {
        if (array == null) {
            return null;
        }
        var result = new JSArray<JSString>(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSString.valueOf(array[i]));
        }
        return result;
    }

    public static WrapFunction<String[], JSObject> stringArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(float[] array) {
        if (array == null) {
            return null;
        }
        var result = new Float32Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 4;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putFloat(array[i + j]);
                    ptr = ptr.add(4);
                }
                var dest = new Float32Array(result.getBuffer(), result.getByteOffset() + i * 4, sz);
                var src = new Float32Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<float[], JSObject> floatArrayWrapper() {
        return JS::wrap;
    }

    public static JSObject wrap(double[] array) {
        if (array == null) {
            return null;
        }
        var result = new Float64Array(array.length);
        if (PlatformDetector.isWebAssemblyGC()) {
            var count = WasmBufferUtil.BUFFER_SIZE / 8;
            for (var i = 0; i < array.length; i += count) {
                var upper = Math.min(array.length, i + count);
                var sz = upper - i;
                var ptr = WasmBufferUtil.buffer;
                for (var j = 0; j < sz; ++j) {
                    ptr.putDouble(array[i + j]);
                    ptr = ptr.add(8);
                }
                var dest = new Float64Array(result.getBuffer(), result.getByteOffset() + i * 8, sz);
                var src = new Float64Array(WasmBufferUtil.getLinearMemory(), WasmBufferUtil.buffer.toInt(), sz);
                dest.set(src);
            }
        } else {
            for (int i = 0; i < array.length; ++i) {
                result.set(i, array[i]);
            }
        }
        return result;
    }

    public static WrapFunction<double[], JSObject> doubleArrayWrapper() {
        return JS::wrap;
    }

    public static <T extends JSObject> T[] unwrapArray(Class<T> type, JSArrayReader<T> array) {
        if (array == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, array.getLength());
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i);
        }
        return result;
    }

    public static <T extends JSObject> UnwrapFunction<JSArrayReader<T>, T[]> arrayUnwrapper(Class<T> type) {
        return array -> unwrapArray(type, array);
    }

    public static <S extends JSObject, T> T[] unmapArray(Class<T> type, JSArrayReader<S> array,
            UnwrapFunction<S, T> f) {
        if (array == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, array.getLength());
        for (int i = 0; i < result.length; ++i) {
            result[i] = f.apply(array.get(i));
        }
        return result;
    }

    public static <T, S extends JSObject> UnwrapFunction<JSArray<S>, T[]> arrayUnmapper(Class<T> type,
            UnwrapFunction<S, T> f) {
        return array -> unmapArray(type, array, f);
    }

    public static boolean[] unwrapBooleanArray(JSArrayReader<JSBoolean> array) {
        if (array == null) {
            return null;
        }
        boolean[] result = new boolean[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Int8Array) {
            var typedArray = (Int8Array) array;
            for (int i = 0; i < result.length; i += WasmBufferUtil.BUFFER_SIZE) {
                var upper = Math.min(i + WasmBufferUtil.BUFFER_SIZE, result.length);
                var sz = upper - i;
                var part = new Int8Array(typedArray.getBuffer(), typedArray.getByteOffset() + i, sz);
                var ptr = WasmBufferUtil.buffer;
                new Int8Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getByte() != 0;
                    ptr = ptr.add(1);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).booleanValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSBoolean>, boolean[]> booleanArrayUnwrapper() {
        return JS::unwrapBooleanArray;
    }

    public static byte[] unwrapByteArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        byte[] result = new byte[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Int8Array) {
            var typedArray = (Int8Array) array;
            for (int i = 0; i < result.length; i += WasmBufferUtil.BUFFER_SIZE) {
                var upper = Math.min(i + WasmBufferUtil.BUFFER_SIZE, result.length);
                var sz = upper - i;
                var part = new Int8Array(typedArray.getBuffer(), typedArray.getByteOffset() + i, sz);
                var ptr = WasmBufferUtil.buffer;
                new Int8Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getByte();
                    ptr = ptr.add(1);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).byteValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, byte[]> byteArrayUnwrapper() {
        return JS::unwrapByteArray;
    }

    public static short[] unwrapShortArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        short[] result = new short[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Int16Array) {
            var typedArray = (Int16Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 2;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new Int16Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 2, sz);
                var ptr = WasmBufferUtil.buffer;
                new Int16Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getShort();
                    ptr = ptr.add(2);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).shortValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, short[]> shortArrayUnwrapper() {
        return JS::unwrapShortArray;
    }

    public static int[] unwrapIntArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        int[] result = new int[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Int32Array) {
            var typedArray = (Int32Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 4;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new Int32Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 4, sz);
                var ptr = WasmBufferUtil.buffer;
                new Int32Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getInt();
                    ptr = ptr.add(4);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).intValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, int[]> intArrayUnwrapper() {
        return JS::unwrapIntArray;
    }

    public static long[] unwrapLongArray(JSArrayReader<JSBigInt> array) {
        if (array == null) {
            return null;
        }
        var result = new long[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof BigInt64Array) {
            var typedArray = (BigInt64Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 8;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new BigInt64Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 8, sz);
                var ptr = WasmBufferUtil.buffer;
                new BigInt64Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getLong();
                    ptr = ptr.add(8);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).longValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSBigInt>, long[]> longArrayUnwrapper() {
        return JS::unwrapLongArray;
    }

    public static char[] unwrapCharArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        char[] result = new char[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Uint16Array) {
            var typedArray = (Uint16Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 2;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new Uint16Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 2, sz);
                var ptr = WasmBufferUtil.buffer;
                new Uint16Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getChar();
                    ptr = ptr.add(2);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).charValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, char[]> charArrayUnwrapper() {
        return JS::unwrapCharArray;
    }

    public static float[] unwrapFloatArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        float[] result = new float[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Float32Array) {
            var typedArray = (Float32Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 4;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new Float32Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 4, sz);
                var ptr = WasmBufferUtil.buffer;
                new Float32Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getFloat();
                    ptr = ptr.add(4);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).floatValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, float[]> floatArrayUnwrapper() {
        return JS::unwrapFloatArray;
    }

    public static double[] unwrapDoubleArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        double[] result = new double[array.getLength()];
        if (PlatformDetector.isWebAssemblyGC() && array instanceof Float64Array) {
            var typedArray = (Float64Array) array;
            var elemCount = WasmBufferUtil.BUFFER_SIZE / 8;
            for (int i = 0; i < result.length; i += elemCount) {
                var upper = Math.min(i + elemCount, result.length);
                var sz = upper - i;
                var part = new Float64Array(typedArray.getBuffer(), typedArray.getByteOffset() + i * 8, sz);
                var ptr = WasmBufferUtil.buffer;
                new Float64Array(WasmBufferUtil.getLinearMemory(), ptr.toInt(), upper - i).set(part);
                for (var j = 0; j < sz; ++j) {
                    result[i + j] = ptr.getDouble();
                    ptr = ptr.add(8);
                }
            }
            return result;
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).doubleValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSNumber>, double[]> doubleArrayUnwrapper() {
        return JS::unwrapDoubleArray;
    }

    public static String[] unwrapStringArray(JSArrayReader<JSString> array) {
        if (array == null) {
            return null;
        }
        String[] result = new String[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).stringValue();
        }
        return result;
    }

    public static UnwrapFunction<JSArrayReader<JSString>, String[]> stringArrayUnwrapper() {
        return JS::unwrapStringArray;
    }

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod0", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod1", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod2", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod3", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod4", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod5", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod6", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod7", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod8", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod9", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod10", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod11", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod12", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "callMethod13", module = "teavmJso")
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l, JSObject m);

    @InjectedBy(JSNativeInjector.class)
    @Import(name = "apply", module = "teavmJso")
    public static native JSObject apply(JSObject instance, JSObject method, JSArray<JSObject> v);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf1", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf2", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf3", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf4", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf5", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf6", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf7", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf8", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "arrayOf9", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h, JSObject i);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf10", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h, JSObject i, JSObject j);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf11", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h, JSObject i, JSObject j, JSObject k);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf12", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h, JSObject i, JSObject j, JSObject k, JSObject l);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "arrayOf13", module = "teavmJso")
    public static native JSObject arrayOf(JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h, JSObject i, JSObject j, JSObject k, JSObject l, JSObject m);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct0", module = "teavmJso")
    public static native JSObject construct(JSObject cls);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct1", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct2", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct3", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct4", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct5", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct6", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct7", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct8", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct9", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h, JSObject i);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct10", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h, JSObject i, JSObject j);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct11", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct12", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k, JSObject l);

    @InjectedBy(JSNativeInjector.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "construct13", module = "teavmJso")
    public static native JSObject construct(JSObject cls, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e,
            JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k, JSObject l, JSObject m);


    @InjectedBy(JSNativeInjector.class)
    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeInjector.class)
    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    @NoSideEffects
    public static native JSObject getPure(JSObject instance, JSObject index);

    @InjectedBy(JSNativeInjector.class)
    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    @Import(name = "setProperty", module = "teavmJso")
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @InjectedBy(JSNativeInjector.class)
    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    @NoSideEffects
    @Import(name = "setPropertyPure", module = "teavmJso")
    public static native void setPure(JSObject instance, JSObject index, JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "asFunction", module = "teavmJso")
    public static native JSObject function(JSObject instance, JSObject property);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeInjector.class)
    @Import(name = "functionAsObject", module = "teavmJso")
    public static native JSObject functionAsObject(JSObject instance, JSObject property);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject global(String name);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject importModule(String name);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "instanceOf", module = "teavmJso")
    public static native boolean instanceOf(JSObject obj, JSObject cls);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "instanceOfOrNull", module = "teavmJso")
    public static native boolean instanceOfOrNull(JSObject obj, JSObject cls);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "isPrimitive", module = "teavmJso")
    public static native boolean isPrimitive(JSObject obj, JSObject primitive);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject throwCCEIfFalse(boolean value, JSObject o);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native JSObject argumentsBeginningAt(int index);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    @Import(name = "sameRef", module = "teavmJso")
    public static native boolean sameRef(JSObject a, JSObject b);

    @InjectedBy(JSNativeInjector.class)
    @NoSideEffects
    public static native boolean isNull(JSObject o);

    @NoSideEffects
    public static native Object jsArrayItem(Object array, int index);
}
