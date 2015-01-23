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
package org.teavm.jso;

import java.util.Iterator;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.InjectedBy;

/**
 *
 * @author Alexey Andreev
 */
public final class JS {
    private JS() {
    }

    public static JSType getType(JSObject obj) {
        switch (unwrapString(getTypeName(obj))) {
            case "boolean":
                return JSType.OBJECT;
            case "number":
                return JSType.NUMBER;
            case "string":
                return JSType.STRING;
            case "function":
                return JSType.FUNCTION;
            case "object":
                return JSType.OBJECT;
            case "undefined":
                return JSType.UNDEFINED;
        }
        throw new AssertionError("Unexpected type");
    }

    public static native <T extends JSObject> JSArray<T> createArray(int size);

    public static native JSIntArray createIntArray(int size);

    public static native JSStringArray createStringArray(int size);

    public static native JSFloatArray createFloatArray(int size);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject getTypeName(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject getGlobal();

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(String str);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(char c);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(int num);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(float num);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(double num);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(boolean num);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(byte num);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(short num);

    public static <T extends JSObject> JSArray<T> wrap(T[] array) {
        JSArray<T> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static <T extends JSObject> JSArray<JSArray<T>> wrap(T[][] array) {
        JSArray<JSArray<T>> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static <T extends JSObject> JSArray<JSArray<JSArray<T>>> wrap(T[][][] array) {
        JSArray<JSArray<JSArray<T>>> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSIntArray wrap(int[] array) {
        JSIntArray result = createIntArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSIntArray> wrap(int[][] array) {
        JSArray<JSIntArray> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSIntArray>> wrap(int[][][] array) {
        JSArray<JSArray<JSIntArray>> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSStringArray wrap(String[] array) {
        JSStringArray result = createStringArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSStringArray> wrap(String[][] array) {
        JSArray<JSStringArray> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSStringArray>> wrap(String[][][] array) {
        JSArray<JSArray<JSStringArray>> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSFloatArray wrap(float[] array) {
        JSFloatArray result = createFloatArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSFloatArray> wrap(float[][] array) {
        JSArray<JSFloatArray> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSFloatArray>> wrap(float[][][] array) {
        JSArray<JSArray<JSFloatArray>> result = createArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    @InjectedBy(JSNativeGenerator.class)
    public static native boolean unwrapBoolean(JSObject obj);

    public static byte unwrapByte(JSObject obj) {
        return (byte)unwrapInt(obj);
    }

    public static short unwrapShort(JSObject obj) {
        return (short)unwrapInt(obj);
    }

    @InjectedBy(JSNativeGenerator.class)
    public static native int unwrapInt(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native float unwrapFloat(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native double unwrapDouble(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native String unwrapString(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native char unwrapCharacter(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native boolean isUndefined(JSObject obj);

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
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c, JSObject d);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c, JSObject d, JSObject e);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c, JSObject d, JSObject e, JSObject f);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c, JSObject d, JSObject e, JSObject f, JSObject g);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject instantiate(JSObject instance, JSObject constructor, JSObject a, JSObject b,
            JSObject c, JSObject d, JSObject e, JSObject f, JSObject g, JSObject h);

    public static <T extends JSObject> Iterable<T> iterate(final JSArrayReader<T> array) {
        return new Iterable<T>() {
            @Override public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int index;
                    @Override public boolean hasNext() {
                        return index < array.getLength();
                    }
                    @Override public T next() {
                        return array.get(index++);
                    }
                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static <T extends JSObject> Iterable<String> iterate(final JSStringArrayReader array) {
        return new Iterable<String>() {
            @Override public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int index;
                    @Override public boolean hasNext() {
                        return index < array.getLength();
                    }
                    @Override public String next() {
                        return array.get(index++);
                    }
                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeGenerator.class)
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject function(JSObject instance, JSObject property);
}
