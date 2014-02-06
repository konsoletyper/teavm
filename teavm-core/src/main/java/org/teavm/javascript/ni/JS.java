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
package org.teavm.javascript.ni;

import java.util.Iterator;

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

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject getTypeName(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject getGlobal();

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject wrap(String str);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject wrap(char c);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject wrap(int num);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject wrap(float num);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject wrap(double num);

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

    @GeneratedBy(JSNativeGenerator.class)
    public static native boolean unwrapBoolean(JSObject obj);

    public static byte unwrapByte(JSObject obj) {
        return (byte)unwrapInt(obj);
    }

    public static short unwrapShort(JSObject obj) {
        return (short)unwrapInt(obj);
    }

    @GeneratedBy(JSNativeGenerator.class)
    public static native int unwrapInt(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native float unwrapFloat(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native double unwrapDouble(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native String unwrapString(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native char unwrapCharacter(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native boolean isUndefined(JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g);

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h);

    public static <T extends JSObject> Iterable<T> iterate(final JSArray<T> array) {
        return new Iterable<T>() {
            @Override public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int index = 0;
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

    @GeneratedBy(JSNativeGenerator.class)
    public static native JSObject get(JSObject instance, JSObject index);

    @GeneratedBy(JSNativeGenerator.class)
    public static native void set(JSObject instance, JSObject index, JSObject obj);
}
