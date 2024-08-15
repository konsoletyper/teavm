/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.backend.wasm.generate;

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.interop.Address;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;

public class WasmInteropFunctionGenerator {
    private WasmClassGenerator classGenerator;
    private WasmFunctionTypes functionTypes;

    public WasmInteropFunctionGenerator(WasmClassGenerator classGenerator, WasmFunctionTypes functionTypes) {
        this.classGenerator = classGenerator;
        this.functionTypes = functionTypes;
    }

    public void generateFunctions(WasmModule module) {
        module.functions.add(allocateString());
        module.functions.add(stringData());

        module.functions.add(allocateArray("teavm_allocateObjectArray", ValueType.parse(Object.class)));
        module.functions.add(allocateArray("teavm_allocateStringArray", ValueType.parse(String.class)));
        module.functions.add(allocateArray("teavm_allocateByteArray", ValueType.parse(byte.class)));
        module.functions.add(allocateArray("teavm_allocateShortArray", ValueType.parse(short.class)));
        module.functions.add(allocateArray("teavm_allocateCharArray", ValueType.parse(char.class)));
        module.functions.add(allocateArray("teavm_allocateIntArray", ValueType.parse(int.class)));
        module.functions.add(allocateArray("teavm_allocateLongArray", ValueType.parse(long.class)));
        module.functions.add(allocateArray("teavm_allocateFloatArray", ValueType.parse(float.class)));
        module.functions.add(allocateArray("teavm_allocateDoubleArray", ValueType.parse(double.class)));
        module.functions.add(arrayData("teavm_objectArrayData", 4));
        module.functions.add(arrayData("teavm_byteArrayData", 1));
        module.functions.add(arrayData("teavm_shortArrayData", 2));
        module.functions.add(arrayData("teavm_charArrayData", 2));
        module.functions.add(arrayData("teavm_intArrayData", 4));
        module.functions.add(arrayData("teavm_longArrayData", 8));
        module.functions.add(arrayData("teavm_floatArrayData", 4));
        module.functions.add(arrayData("teavm_doubleArrayData", 8));
        module.functions.add(arrayLength());
    }


    private WasmFunction allocateString() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
        function.setName("teavm_allocateString");
        function.setExportName(function.getName());

        WasmLocal sizeLocal = new WasmLocal(WasmType.INT32, "size");
        function.add(sizeLocal);

        var constructor = classGenerator.functions.forStaticMethod(new MethodReference(String.class, "allocate",
                int.class, String.class));
        WasmCall constructorCall = new WasmCall(constructor);
        constructorCall.getArguments().add(new WasmGetLocal(sizeLocal));
        function.getBody().add(constructorCall);

        function.getBody().add(new WasmReturn(constructorCall));

        return function;
    }

    private WasmFunction allocateArray(String name, ValueType type) {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
        function.setName(name);
        function.setExportName(name);

        WasmLocal sizeLocal = new WasmLocal(WasmType.INT32, "size");
        function.add(sizeLocal);

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        var allocFunction = classGenerator.functions.forStaticMethod(new MethodReference(Allocator.class,
                "allocateArray", RuntimeClass.class, int.class, Address.class));
        WasmCall call = new WasmCall(allocFunction);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(new WasmGetLocal(sizeLocal));

        function.getBody().add(new WasmReturn(call));

        return function;
    }

    private WasmFunction stringData() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
        function.setName("teavm_stringData");
        function.setExportName(function.getName());

        WasmLocal stringLocal = new WasmLocal(WasmType.INT32, "string");
        function.add(stringLocal);

        int offset = classGenerator.getFieldOffset(new FieldReference("java.lang.String", "characters"));
        WasmExpression chars = new WasmLoadInt32(4, new WasmGetLocal(stringLocal), WasmInt32Subtype.INT32, offset);

        function.getBody().add(new WasmReturn(chars));

        return function;
    }

    private WasmFunction arrayData(String name, int alignment) {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
        function.setName(name);
        function.setExportName(function.getName());

        WasmLocal arrayLocal = new WasmLocal(WasmType.INT32, "array");
        function.add(arrayLocal);

        int start = WasmClassGenerator.align(classGenerator.getClassSize(RuntimeArray.class.getName()),
                alignment);
        WasmExpression data =  new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                new WasmGetLocal(arrayLocal), new WasmInt32Constant(start));

        function.getBody().add(new WasmReturn(data));

        return function;
    }

    private WasmFunction arrayLength() {
        WasmFunction function = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
        function.setName("teavm_arrayLength");
        function.setExportName(function.getName());

        WasmLocal arrayLocal = new WasmLocal(WasmType.INT32, "array");
        function.add(arrayLocal);

        int sizeOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeArray.class.getName(), "size"));

        WasmExpression ptr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                new WasmGetLocal(arrayLocal), new WasmInt32Constant(sizeOffset));
        WasmExpression length = new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);

        function.getBody().add(new WasmReturn(length));

        return function;
    }
}
