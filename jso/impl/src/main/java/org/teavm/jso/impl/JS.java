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
import java.util.function.Function;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.dependency.PluggableDependency;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

final class JS {
    private JS() {
    }

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject arrayData(Object array);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(byte value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(short value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(int value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(char value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(float value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(double value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(boolean value);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(String value);

    @InjectedBy(JSNativeGenerator.class)
    public static native byte unwrapByte(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native char unwrapCharacter(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native short unwrapShort(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native int unwrapInt(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native float unwrapFloat(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native double unwrapDouble(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    public static native boolean unwrapBoolean(JSObject value);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native String unwrapString(JSObject value);

    public static <T extends JSObject> JSArray<T> wrap(T[] array) {
        if (array == null) {
            return null;
        }
        JSArray<T> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static <T extends JSObject> Function<T[], JSArray<T>> arrayWrapper() {
        return JS::wrap;
    }

    public static <T extends JSObject, S> JSArray<T> map(S[] array, Function<S, T> f) {
        if (array == null) {
            return null;
        }
        JSArray<T> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, f.apply(array[i]));
        }
        return result;
    }

    public static <T extends JSObject, S> Function<S[], JSArray<T>> arrayMapper(Function<S, T> f) {
        return array -> map(array, f);
    }

    public static JSArray<JSBoolean> wrap(boolean[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSBoolean> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSBoolean.valueOf(array[i]));
        }
        return result;
    }

    public static Function<boolean[], JSArray<JSBoolean>> booleanArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(byte[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<byte[], JSArray<JSNumber>> byteArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(short[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<short[], JSArray<JSNumber>> shortArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(char[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<char[], JSArray<JSNumber>> charArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(int[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<int[], JSArray<JSNumber>> intArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSString> wrap(String[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSString> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSString.valueOf(array[i]));
        }
        return result;
    }

    public static Function<String[], JSArray<JSString>> stringArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(float[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<float[], JSArray<JSNumber>> floatArrayWrapper() {
        return JS::wrap;
    }

    public static JSArray<JSNumber> wrap(double[] array) {
        if (array == null) {
            return null;
        }
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static Function<double[], JSArray<JSNumber>> doubleArrayWrapper() {
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

    public static <T extends JSObject> Function<JSArrayReader<T>, T[]> arrayUnwrapper(Class<T> type) {
        return array -> unwrapArray(type, array);
    }

    public static <S extends JSObject, T> T[] unmapArray(Class<T> type, JSArrayReader<S> array, Function<S, T> f) {
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

    public static <T, S extends JSObject> Function<JSArray<S>, T[]> arrayUnmapper(Class<T> type, Function<S, T> f) {
        return array -> unmapArray(type, array, f);
    }

    public static boolean[] unwrapBooleanArray(JSArrayReader<JSBoolean> array) {
        if (array == null) {
            return null;
        }
        boolean[] result = new boolean[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).booleanValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSBoolean>, boolean[]> booleanArrayUnwrapper() {
        return JS::unwrapBooleanArray;
    }

    public static byte[] unwrapByteArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        byte[] result = new byte[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).byteValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, byte[]> byteArrayUnwrapper() {
        return JS::unwrapByteArray;
    }

    public static short[] unwrapShortArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        short[] result = new short[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).shortValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, short[]> shortArrayUnwrapper() {
        return JS::unwrapShortArray;
    }

    public static int[] unwrapIntArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        int[] result = new int[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).intValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, int[]> intArrayUnwrapper() {
        return JS::unwrapIntArray;
    }

    public static char[] unwrapCharArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        char[] result = new char[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).charValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, char[]> charArrayUnwrapper() {
        return JS::unwrapCharArray;
    }

    public static float[] unwrapFloatArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        float[] result = new float[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).floatValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, float[]> floatArrayUnwrapper() {
        return JS::unwrapFloatArray;
    }

    public static double[] unwrapDoubleArray(JSArrayReader<JSNumber> array) {
        if (array == null) {
            return null;
        }
        double[] result = new double[array.getLength()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i).doubleValue();
        }
        return result;
    }

    public static Function<JSArrayReader<JSNumber>, double[]> doubleArrayUnwrapper() {
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

    public static Function<JSArrayReader<JSString>, String[]> stringArrayUnwrapper() {
        return JS::unwrapStringArray;
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l, JSObject m);

    @InjectedBy(JSNativeGenerator.class)
    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeGenerator.class)
    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject function(JSObject instance, JSObject property);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject functionAsObject(JSObject instance, JSObject property);
}
