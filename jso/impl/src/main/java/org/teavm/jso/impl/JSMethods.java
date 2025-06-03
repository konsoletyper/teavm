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
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public final class JSMethods {
    public static final String JS_CLASS = "org.teavm.jso.impl.JS";
    public static final String JS_OBJECT_CLASS = "org.teavm.jso.JSObject";
    public static final String JS_WRAPPER_CLASS = "org.teavm.jso.impl.JSWrapper";
    public static final String JS_EXCEPTIONS_CLASS = "org.teavm.jso.JSExceptions";
    public static final String WASM_GC_JS_RUNTIME_CLASS = "org.teavm.jso.impl.wasmgc.WasmGCJSRuntime";
    public static final ValueType JS_OBJECT = ValueType.object(JS_OBJECT_CLASS);
    public static final ValueType JS_WRAPPER = ValueType.object(JS_WRAPPER_CLASS);
    public static final ValueType JS_ARRAY = ValueType.object("org.teavm.jso.core.JSArray");
    public static final ValueType JS_ARRAY_READER = ValueType.object("org.teavm.jso.core.JSArrayReader");
    public static final ValueType OBJECT = ValueType.object("java.lang.Object");
    public static final ValueType CLASS = ValueType.object("java.lang.Class");
    public static final ValueType STRING = ValueType.object("java.lang.String");
    private static final ValueType JS_WRAP_FUNCTION = ValueType.object("org.teavm.jso.impl.JS$WrapFunction");
    private static final ValueType JS_UNWRAP_FUNCTION = ValueType.object("org.teavm.jso.impl.JS$UnwrapFunction");

    public static final MethodReference GET = new MethodReference(JS_CLASS, "get", JS_OBJECT, JS_OBJECT, JS_OBJECT);
    public static final MethodReference GET_PURE = new MethodReference(JS_CLASS, "getPure", JS_OBJECT,
            JS_OBJECT, JS_OBJECT);
    public static final MethodReference SET = new MethodReference(JS_CLASS, "set", JS_OBJECT, JS_OBJECT,
            JS_OBJECT, ValueType.VOID);
    public static final MethodReference SET_PURE = new MethodReference(JS_CLASS, "setPure", JS_OBJECT,
            JS_OBJECT, JS_OBJECT, ValueType.VOID);
    public static final MethodReference APPLY = new MethodReference(JS_CLASS, "apply", JS_OBJECT, JS_OBJECT,
            JS_ARRAY, JS_OBJECT);
    public static final MethodReference FUNCTION = new MethodReference(JS_CLASS, "function", JS_OBJECT,
            JS_OBJECT, JS_OBJECT);
    public static final MethodReference ARRAY_DATA = new MethodReference(JS_CLASS, "arrayData",
            OBJECT, JS_OBJECT);
    public static final MethodReference CONCAT_ARRAY = new MethodReference(JS_CLASS, "concatArray",
            JS_OBJECT, JS_OBJECT, JS_OBJECT);
    public static final MethodReference ARRAY_MAPPER = new MethodReference(JS_CLASS, "arrayMapper",
            JS_WRAP_FUNCTION, JS_WRAP_FUNCTION);
    public static final MethodReference BOOLEAN_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "booleanArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference BYTE_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "byteArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference SHORT_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "shortArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference CHAR_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "charArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference INT_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "intArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference LONG_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "longArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference FLOAT_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "floatArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference DOUBLE_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "doubleArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference STRING_ARRAY_WRAPPER = new MethodReference(JS_CLASS, "stringArrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference ARRAY_WRAPPER = new MethodReference(JS_CLASS, "arrayWrapper",
            JS_WRAP_FUNCTION);
    public static final MethodReference ARRAY_UNMAPPER = new MethodReference(JS_CLASS, "arrayUnmapper",
            CLASS, JS_UNWRAP_FUNCTION, JS_UNWRAP_FUNCTION);
    public static final MethodReference UNMAP_ARRAY = new MethodReference(JS_CLASS, "unmapArray", CLASS,
            JS_ARRAY_READER, JS_UNWRAP_FUNCTION, ValueType.arrayOf(OBJECT));
    public static final MethodReference UNWRAP_BOOLEAN_ARRAY = new MethodReference(JS_CLASS, "unwrapBooleanArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.BOOLEAN));
    public static final MethodReference UNWRAP_BYTE_ARRAY = new MethodReference(JS_CLASS, "unwrapByteArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.BYTE));
    public static final MethodReference UNWRAP_SHORT_ARRAY = new MethodReference(JS_CLASS, "unwrapShortArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.SHORT));
    public static final MethodReference UNWRAP_CHAR_ARRAY = new MethodReference(JS_CLASS, "unwrapCharArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.CHARACTER));
    public static final MethodReference UNWRAP_INT_ARRAY = new MethodReference(JS_CLASS, "unwrapIntArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.INTEGER));
    public static final MethodReference UNWRAP_LONG_ARRAY = new MethodReference(JS_CLASS, "unwrapLongArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.LONG));
    public static final MethodReference UNWRAP_FLOAT_ARRAY = new MethodReference(JS_CLASS, "unwrapFloatArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.FLOAT));
    public static final MethodReference UNWRAP_DOUBLE_ARRAY = new MethodReference(JS_CLASS, "unwrapDoubleArray",
            JS_ARRAY_READER, ValueType.arrayOf(ValueType.DOUBLE));
    public static final MethodReference UNWRAP_STRING_ARRAY = new MethodReference(JS_CLASS, "unwrapStringArray",
            JS_ARRAY_READER, ValueType.arrayOf(STRING));
    public static final MethodReference UNWRAP_ARRAY = new MethodReference(JS_CLASS, "unwrapArray", CLASS,
            JS_ARRAY_READER, ValueType.arrayOf(JS_OBJECT));
    public static final MethodReference BOOLEAN_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "booleanArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference BYTE_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "byteArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference SHORT_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "shortArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference CHAR_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "charArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference INT_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "intArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference LONG_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "longArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference FLOAT_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "floatArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference DOUBLE_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "doubleArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference STRING_ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "stringArrayUnwrapper", JS_UNWRAP_FUNCTION);
    public static final MethodReference ARRAY_UNWRAPPER = new MethodReference(JS_CLASS,
            "arrayUnwrapper", CLASS, JS_UNWRAP_FUNCTION);

    public static final MethodReference DATA_TO_BYTE_ARRAY = new MethodReference(JS_CLASS,
            "dataToByteArray", JS_OBJECT, ValueType.arrayOf(ValueType.BYTE));
    public static final MethodReference DATA_TO_SHORT_ARRAY = new MethodReference(JS_CLASS,
            "dataToShortArray", JS_OBJECT, ValueType.arrayOf(ValueType.SHORT));
    public static final MethodReference DATA_TO_CHAR_ARRAY = new MethodReference(JS_CLASS,
            "dataToCharArray", JS_OBJECT, ValueType.arrayOf(ValueType.CHARACTER));
    public static final MethodReference DATA_TO_INT_ARRAY = new MethodReference(JS_CLASS,
            "dataToIntArray", JS_OBJECT, ValueType.arrayOf(ValueType.INTEGER));
    public static final MethodReference DATA_TO_LONG_ARRAY = new MethodReference(JS_CLASS,
            "dataToLongArray", JS_OBJECT, ValueType.arrayOf(ValueType.LONG));
    public static final MethodReference DATA_TO_FLOAT_ARRAY = new MethodReference(JS_CLASS,
            "dataToFloatArray", JS_OBJECT, ValueType.arrayOf(ValueType.FLOAT));
    public static final MethodReference DATA_TO_DOUBLE_ARRAY = new MethodReference(JS_CLASS,
            "dataToDoubleArray", JS_OBJECT, ValueType.arrayOf(ValueType.DOUBLE));
    public static final MethodReference DATA_TO_ARRAY = new MethodReference(JS_CLASS,
            "dataToArray", JS_OBJECT, ValueType.arrayOf(JS_OBJECT));

    public static final MethodReference WRAP_STRING = new MethodReference(JS_CLASS, "wrap",
            STRING, JS_OBJECT);

    public static final MethodReference FUNCTION_AS_OBJECT = new MethodReference(JS_CLASS, "functionAsObject",
            JS_OBJECT, JS_OBJECT, JS_OBJECT);

    public static final MethodReference GLOBAL = new MethodReference(JS_CLASS, "global", STRING, JS_OBJECT);
    public static final MethodReference IMPORT_MODULE = new MethodReference(JS_CLASS, "importModule",
            STRING, JS_OBJECT);

    public static final MethodReference INSTANCE_OF = new MethodReference(JS_CLASS, "instanceOf", JS_OBJECT,
            JS_OBJECT, ValueType.BOOLEAN);
    public static final MethodReference INSTANCE_OF_OR_NULL = new MethodReference(JS_CLASS, "instanceOfOrNull",
            JS_OBJECT, JS_OBJECT, ValueType.BOOLEAN);
    public static final MethodReference IS_PRIMITIVE = new MethodReference(JS_CLASS, "isPrimitive", JS_OBJECT,
            JS_OBJECT, ValueType.BOOLEAN);
    public static final MethodReference THROW_CCE_IF_FALSE = new MethodReference(JS_CLASS, "throwCCEIfFalse",
            ValueType.BOOLEAN, JS_OBJECT, JS_OBJECT);
    public static final MethodReference ARGUMENTS_BEGINNING_AT = new MethodReference(JS_CLASS,
            "argumentsBeginningAt", ValueType.INTEGER, JS_OBJECT);

    private static final MethodReference[] INVOKE_METHODS = new MethodReference[13];
    private static final MethodReference[] CONSTRUCT_METHODS = new MethodReference[13];
    private static final MethodReference[] ARRAY_OF_METHODS = new MethodReference[13];

    public static final MethodReference WRAP = new MethodReference(JS_WRAPPER_CLASS, "wrap", JS_OBJECT,
            OBJECT);
    public static final MethodReference MAYBE_WRAP = new MethodReference(JS_WRAPPER_CLASS, "maybeWrap", OBJECT,
            OBJECT);
    public static final MethodReference UNWRAP = new MethodReference(JS_WRAPPER_CLASS, "unwrap", OBJECT,
            JS_OBJECT);
    public static final MethodReference MAYBE_UNWRAP = new MethodReference(JS_WRAPPER_CLASS, "maybeUnwrap",
            OBJECT, JS_OBJECT);
    public static final MethodReference IS_JS = new MethodReference(JS_WRAPPER_CLASS, "isJs",
            OBJECT, ValueType.BOOLEAN);
    public static final MethodReference WRAPPER_IS_PRIMITIVE = new MethodReference(JS_WRAPPER_CLASS, "isPrimitive",
            OBJECT, JS_OBJECT, ValueType.BOOLEAN);
    public static final MethodReference WRAPPER_INSTANCE_OF = new MethodReference(JS_WRAPPER_CLASS, "instanceOf",
            OBJECT, JS_OBJECT, ValueType.BOOLEAN);

    public static final String JS_MARSHALLABLE = JSMarshallable.class.getName();
    public static final MethodDescriptor MARSHALL_TO_JS = new MethodDescriptor("marshallToJs", JS_OBJECT);

    static {
        for (int i = 0; i < INVOKE_METHODS.length; ++i) {
            var signature = new ValueType[i + 3];
            Arrays.fill(signature, JS_OBJECT);
            INVOKE_METHODS[i] = new MethodReference(JS_CLASS, "invoke", signature);

            var constructSignature = new ValueType[i + 2];
            Arrays.fill(constructSignature, JS_OBJECT);
            CONSTRUCT_METHODS[i] = new MethodReference(JS_CLASS, "construct", constructSignature);

            var arrayOfSignature = new ValueType[i + 1];
            Arrays.fill(arrayOfSignature, JS_OBJECT);
            ARRAY_OF_METHODS[i] = new MethodReference(JS_CLASS, "arrayOf", arrayOfSignature);
        }
    }

    private JSMethods() {
    }

    public static MethodReference invoke(int parameterCount) {
        return INVOKE_METHODS[parameterCount];
    }

    public static MethodReference construct(int parameterCount) {
        return CONSTRUCT_METHODS[parameterCount];
    }

    public static MethodReference arrayOf(int parameterCount) {
        return ARRAY_OF_METHODS[parameterCount];
    }
}
