/*
 *  Copyright 2018 Alexey Andreev.
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

import java.util.Arrays;
import java.util.function.Function;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

final class JSMethods {
    public static final MethodReference GET = new MethodReference(JS.class, "get", JSObject.class,
            JSObject.class, JSObject.class);
    public static final MethodReference GET_PURE = new MethodReference(JS.class, "getPure", JSObject.class,
            JSObject.class, JSObject.class);
    public static final MethodReference SET = new MethodReference(JS.class, "set", JSObject.class, JSObject.class,
            JSObject.class, void.class);
    public static final MethodReference SET_PURE = new MethodReference(JS.class, "setPure", JSObject.class,
            JSObject.class, JSObject.class, void.class);
    public static final MethodReference FUNCTION = new MethodReference(JS.class, "function", JSObject.class,
            JSObject.class, JSObject.class);
    public static final MethodReference ARRAY_DATA = new MethodReference(JS.class, "arrayData",
            Object.class, JSObject.class);
    public static final MethodReference ARRAY_MAPPER = new MethodReference(JS.class, "arrayMapper",
            Function.class, Function.class);
    public static final MethodReference BOOLEAN_ARRAY_WRAPPER = new MethodReference(JS.class, "booleanArrayWrapper",
            Function.class);
    public static final MethodReference BYTE_ARRAY_WRAPPER = new MethodReference(JS.class, "byteArrayWrapper",
            Function.class);
    public static final MethodReference SHORT_ARRAY_WRAPPER = new MethodReference(JS.class, "shortArrayWrapper",
            Function.class);
    public static final MethodReference CHAR_ARRAY_WRAPPER = new MethodReference(JS.class, "charArrayWrapper",
            Function.class);
    public static final MethodReference INT_ARRAY_WRAPPER = new MethodReference(JS.class, "intArrayWrapper",
            Function.class);
    public static final MethodReference FLOAT_ARRAY_WRAPPER = new MethodReference(JS.class, "floatArrayWrapper",
            Function.class);
    public static final MethodReference DOUBLE_ARRAY_WRAPPER = new MethodReference(JS.class, "doubleArrayWrapper",
            Function.class);
    public static final MethodReference STRING_ARRAY_WRAPPER = new MethodReference(JS.class, "stringArrayWrapper",
            Function.class);
    public static final MethodReference ARRAY_WRAPPER = new MethodReference(JS.class, "arrayWrapper",
            Function.class);
    public static final MethodReference ARRAY_UNMAPPER = new MethodReference(JS.class, "arrayUnmapper",
            Class.class, Function.class, Function.class);
    public static final MethodReference UNMAP_ARRAY = new MethodReference(JS.class, "unmapArray", Class.class,
            JSArrayReader.class, Function.class, Object[].class);
    public static final MethodReference UNWRAP_BOOLEAN_ARRAY = new MethodReference(JS.class, "unwrapBooleanArray",
            JSArrayReader.class, boolean[].class);
    public static final MethodReference UNWRAP_BYTE_ARRAY = new MethodReference(JS.class, "unwrapByteArray",
            JSArrayReader.class, byte[].class);
    public static final MethodReference UNWRAP_SHORT_ARRAY = new MethodReference(JS.class, "unwrapShortArray",
            JSArrayReader.class, short[].class);
    public static final MethodReference UNWRAP_CHAR_ARRAY = new MethodReference(JS.class, "unwrapCharArray",
            JSArrayReader.class, char[].class);
    public static final MethodReference UNWRAP_INT_ARRAY = new MethodReference(JS.class, "unwrapIntArray",
            JSArrayReader.class, int[].class);
    public static final MethodReference UNWRAP_FLOAT_ARRAY = new MethodReference(JS.class, "unwrapFloatArray",
            JSArrayReader.class, float[].class);
    public static final MethodReference UNWRAP_DOUBLE_ARRAY = new MethodReference(JS.class, "unwrapDoubleArray",
            JSArrayReader.class, double[].class);
    public static final MethodReference UNWRAP_STRING_ARRAY = new MethodReference(JS.class, "unwrapStringArray",
            JSArrayReader.class, String[].class);
    public static final MethodReference UNWRAP_ARRAY = new MethodReference(JS.class, "unwrapArray", Class.class,
            JSArrayReader.class, JSObject[].class);
    public static final MethodReference BOOLEAN_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "booleanArrayUnwrapper", Function.class);
    public static final MethodReference BYTE_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "byteArrayUnwrapper", Function.class);
    public static final MethodReference SHORT_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "shortArrayUnwrapper", Function.class);
    public static final MethodReference CHAR_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "charArrayUnwrapper", Function.class);
    public static final MethodReference INT_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "intArrayUnwrapper", Function.class);
    public static final MethodReference FLOAT_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "floatArrayUnwrapper", Function.class);
    public static final MethodReference DOUBLE_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "doubleArrayUnwrapper", Function.class);
    public static final MethodReference STRING_ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "stringArrayUnwrapper", Function.class);
    public static final MethodReference ARRAY_UNWRAPPER = new MethodReference(JS.class,
            "arrayUnwrapper", Class.class, Function.class);

    public static final MethodReference DATA_TO_BYTE_ARRAY = new MethodReference(JS.class,
            "dataToByteArray", JSObject.class, byte[].class);
    public static final MethodReference DATA_TO_SHORT_ARRAY = new MethodReference(JS.class,
            "dataToShortArray", JSObject.class, short[].class);
    public static final MethodReference DATA_TO_CHAR_ARRAY = new MethodReference(JS.class,
            "dataToCharArray", JSObject.class, char[].class);
    public static final MethodReference DATA_TO_INT_ARRAY = new MethodReference(JS.class,
            "dataToIntArray", JSObject.class, int[].class);
    public static final MethodReference DATA_TO_FLOAT_ARRAY = new MethodReference(JS.class,
            "dataToFloatArray", JSObject.class, float[].class);
    public static final MethodReference DATA_TO_DOUBLE_ARRAY = new MethodReference(JS.class,
            "dataToDoubleArray", JSObject.class, double[].class);
    public static final MethodReference DATA_TO_ARRAY = new MethodReference(JS.class,
            "dataToArray", JSObject.class, JSObject[].class);

    public static final MethodReference FUNCTION_AS_OBJECT = new MethodReference(JS.class, "functionAsObject",
            JSObject.class, JSObject.class, JSObject.class);

    public static final ValueType JS_OBJECT = ValueType.object(JSObject.class.getName());
    public static final ValueType JS_ARRAY = ValueType.object(JSArray.class.getName());
    private static final MethodReference[] INVOKE_METHODS = new MethodReference[13];

    static {
        for (int i = 0; i < INVOKE_METHODS.length; ++i) {
            ValueType[] signature = new ValueType[i + 3];
            Arrays.fill(signature, JS_OBJECT);
            INVOKE_METHODS[i] = new MethodReference(JS.class.getName(), "invoke", signature);
        }
    }

    private JSMethods() {
    }

    public static MethodReference invoke(int parameterCount) {
        return INVOKE_METHODS[parameterCount];
    }
}
