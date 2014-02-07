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
import org.teavm.dependency.PluggableDependency;

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

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject getTypeName(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject getGlobal();

    @GeneratedBy(JSNativeGenerator.class)
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

    @GeneratedBy(JSNativeGenerator.class)
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

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeGenerator.class)
    public static native void set(JSObject instance, JSObject index, JSObject obj);
}
