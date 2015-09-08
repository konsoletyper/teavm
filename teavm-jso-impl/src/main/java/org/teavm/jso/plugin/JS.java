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
package org.teavm.jso.plugin;

import java.lang.reflect.Array;
import java.util.Iterator;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSType;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

/**
 * <p>Container of static methods to manipulate over {@link JSObject}s.</p>
 *
 * @author Alexey Andreev
 */
final class JS {
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

    @JSBody(params = "obj", script = "return typeof(obj);")
    private static native JSObject getTypeName(JSObject obj);

    /**
     * Gets global JavaScript object, that is similar to the <code>window</code> object in the browser.
     * @return global object.
     */
    @JSBody(params = {}, script = "return window;")
    public static native JSObject getGlobal();

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(String str);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject wrap(char c);

    @InjectedBy(JSNativeGenerator.class)
    public static native JSObject marshall(Object obj);

    public static <T extends JSObject> JSArray<T> wrap(T[] array) {
        JSArray<T> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static <T extends JSObject> JSArray<JSArray<T>> wrap(T[][] array) {
        JSArray<JSArray<T>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static <T extends JSObject> JSArray<JSArray<JSArray<T>>> wrap(T[][][] array) {
        JSArray<JSArray<JSArray<T>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSBoolean> wrap(boolean[] array) {
        JSArray<JSBoolean> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSBoolean.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSBoolean>> wrap(boolean[][] array) {
        JSArray<JSArray<JSBoolean>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSBoolean>>> wrap(boolean[][][] array) {
        JSArray<JSArray<JSArray<JSBoolean>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(byte[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(byte[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(byte[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(short[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(short[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(short[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(char[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(char[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(char[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(int[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(int[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(int[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSString> wrap(String[] array) {
        JSArray<JSString> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSString.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSString>> wrap(String[][] array) {
        JSArray<JSArray<JSString>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSString>>> wrap(String[][][] array) {
        JSArray<JSArray<JSArray<JSString>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(float[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(float[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(float[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSNumber> wrap(double[] array) {
        JSArray<JSNumber> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, JSNumber.valueOf(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSNumber>> wrap(double[][] array) {
        JSArray<JSArray<JSNumber>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSArray<JSNumber>>> wrap(double[][][] array) {
        JSArray<JSArray<JSArray<JSNumber>>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native String unwrapString(JSObject obj);

    @InjectedBy(JSNativeGenerator.class)
    public static native char unwrapCharacter(JSObject obj);

    public static <T extends JSObject> T[] unwrapArray(Class<T> type, JSArray<T> array) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, array.getLength());
        for (int i = 0; i < result.length; ++i) {
            result[i] = array.get(i);
        }
        return result;
    }

    public static <T extends JSObject> T[][] unwrapArray2(Class<T> type, JSArray<JSArray<T>> array) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][]) Array.newInstance(Array.newInstance(type, 0).getClass(), array.getLength());
        for (int i = 0; i < result.length; ++i) {
            result[i] = unwrapArray(type, array.get(i));
        }
        return result;
    }

    public static <T extends JSObject> T[][][] unwrapArray3(Class<T> type, JSArray<JSArray<JSArray<T>>> array) {
        Class<?> baseType = Array.newInstance(type, 0).getClass();
        @SuppressWarnings("unchecked")
        T[][][] result = (T[][][]) Array.newInstance(Array.newInstance(baseType, 0).getClass(), array.getLength());
        for (int i = 0; i < result.length; ++i) {
            result[i] = unwrapArray2(type, array.get(i));
        }
        return result;
    }

    @JSBody(params = "obj", script = "return typeof(obj) === 'undefined';")
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
        return () -> new Iterator<T>() {
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

    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeGenerator.class)
    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject function(JSObject instance, JSObject property);
}
