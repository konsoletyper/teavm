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

import java.lang.reflect.Array;
import java.util.Iterator;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.jso.plugin.JSNativeGenerator;

/**
 * <p>Container of static methods to manipulate over {@link JSObject}s.</p>
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

    @JSExpression(params = "obj", expr = "typeof(obj)")
    private static native JSObject getTypeName(JSObject obj);

    /**
     * Gets global JavaScript object, that is similar to the <code>window</code> object in the browser.
     * @return global object.
     */
    @JSExpression(params = "obj", expr = "window")
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

    public static JSArray<JSIntArray> wrap(short[][] array) {
        JSArray<JSIntArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSIntArray>> wrap(short[][][] array) {
        JSArray<JSArray<JSIntArray>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSIntArray wrap(char[] array) {
        JSIntArray result = createIntArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSIntArray> wrap(char[][] array) {
        JSArray<JSIntArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSIntArray>> wrap(char[][][] array) {
        JSArray<JSArray<JSIntArray>> result = JSArray.create(array.length);
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
        JSArray<JSIntArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSIntArray>> wrap(int[][][] array) {
        JSArray<JSArray<JSIntArray>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSStringArray wrap(String[] array) {
        JSStringArray result = JSStringArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSStringArray> wrap(String[][] array) {
        JSArray<JSStringArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSStringArray>> wrap(String[][][] array) {
        JSArray<JSArray<JSStringArray>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSDoubleArray wrap(float[] array) {
        JSDoubleArray result = createDoubleArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSDoubleArray> wrap(float[][] array) {
        JSArray<JSDoubleArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSDoubleArray>> wrap(float[][][] array) {
        JSArray<JSArray<JSDoubleArray>> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSDoubleArray wrap(double[] array) {
        JSDoubleArray result = createDoubleArray(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, array[i]);
        }
        return result;
    }

    public static JSArray<JSDoubleArray> wrap(double[][] array) {
        JSArray<JSDoubleArray> result = JSArray.create(array.length);
        for (int i = 0; i < array.length; ++i) {
            result.set(i, wrap(array[i]));
        }
        return result;
    }

    public static JSArray<JSArray<JSDoubleArray>> wrap(double[][][] array) {
        JSArray<JSArray<JSDoubleArray>> result = JSArray.create(array.length);
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

    @JSExpression(params = "obj", expr = "typeof(obj) === 'undefined'")
    public static native boolean isUndefined(JSObject obj);

    @JSExpression(params = { "instance", "method" }, expr = "instance[method]()")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method);

    @JSExpression(params = { "instance", "method", "a" }, expr = "instance[method](a)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a);

    @JSExpression(params = { "instance", "method", "a", "b" }, expr = "instance[method](a, b)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b);

    @JSExpression(params = { "instance", "method", "a", "b", "c" }, expr = "instance[method](a, b, c)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d" }, expr = "instance[method](a, b, c, d)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e" }, expr = "instance[method](a, b, c, d, e)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f" },
            expr = "instance[method](a, b, c, d, e, f)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g" },
            expr = "instance[method](a, b, c, d, e, f, g)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h" },
            expr = "instance[method](a, b, c, d, e, f, g, h)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h", "i" },
            expr = "instance[method](a, b, c, d, e, f, g, h, i)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" },
            expr = "instance[method](a, b, c, d, e, f, g, h, i, j)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k" },
            expr = "instance[method](a, b, c, d, e, f, g, h, i, j, k)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l" },
            expr = "instance[method](a, b, c, d, e, f, g, h, i, j, k, l)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l);

    @JSExpression(params = { "instance", "method", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m" },
            expr = "instance[method](a, b, c, d, e, f, g, h, i, j, k, l, m)")
    @InjectedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject invoke(JSObject instance, JSObject method, JSObject a, JSObject b, JSObject c,
            JSObject d, JSObject e, JSObject f, JSObject g, JSObject h, JSObject i, JSObject j, JSObject k,
            JSObject l, JSObject m);

    @JSExpression(params = { "instance", "method" },
            expr = "new instance[method]()")
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

    @JSExpression(params = { "instance", "index" }, expr = "instance[index]")
    public static native JSObject get(JSObject instance, JSObject index);

    @InjectedBy(JSNativeGenerator.class)
    @JSExpression(params = { "instance", "index", "obj" }, expr = "instance[index] = obj")
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @GeneratedBy(JSNativeGenerator.class)
    @PluggableDependency(JSNativeGenerator.class)
    public static native JSObject function(JSObject instance, JSObject property);
}
