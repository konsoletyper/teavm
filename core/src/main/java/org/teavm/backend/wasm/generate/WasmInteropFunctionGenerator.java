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

    public WasmInteropFunctionGenerator(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    public void generateFunctions(WasmModule module) {
        module.add(allocateString());
        module.add(stringData());

        module.add(allocateArray("teavm_allocateObjectArray", ValueType.parse(Object.class)));
        module.add(allocateArray("teavm_allocateStringArray", ValueType.parse(String.class)));
        module.add(allocateArray("teavm_allocateByteArray", ValueType.parse(byte.class)));
        module.add(allocateArray("teavm_allocateShortArray", ValueType.parse(short.class)));
        module.add(allocateArray("teavm_allocateCharArray", ValueType.parse(char.class)));
        module.add(allocateArray("teavm_allocateIntArray", ValueType.parse(int.class)));
        module.add(allocateArray("teavm_allocateLongArray", ValueType.parse(long.class)));
        module.add(allocateArray("teavm_allocateFloatArray", ValueType.parse(float.class)));
        module.add(allocateArray("teavm_allocateDoubleArray", ValueType.parse(double.class)));
        module.add(arrayData("teavm_objectArrayData", 4));
        module.add(arrayData("teavm_byteArrayData", 1));
        module.add(arrayData("teavm_shortArrayData", 2));
        module.add(arrayData("teavm_charArrayData", 2));
        module.add(arrayData("teavm_intArrayData", 4));
        module.add(arrayData("teavm_longArrayData", 8));
        module.add(arrayData("teavm_floatArrayData", 4));
        module.add(arrayData("teavm_doubleArrayData", 8));
        module.add(arrayLength());
    }


    private WasmFunction allocateString() {
        WasmFunction function = new WasmFunction("teavm_allocateString");
        function.setExportName(function.getName());
        function.setResult(WasmType.INT32);
        function.getParameters().add(WasmType.INT32);

        WasmLocal sizeLocal = new WasmLocal(WasmType.INT32, "size");
        function.add(sizeLocal);

        String constructorName = classGenerator.names.forMethod(new MethodReference(String.class, "allocate",
                int.class, String.class));
        WasmCall constructorCall = new WasmCall(constructorName);
        constructorCall.getArguments().add(new WasmGetLocal(sizeLocal));
        function.getBody().add(constructorCall);

        function.getBody().add(new WasmReturn(constructorCall));

        return function;
    }

    private WasmFunction allocateArray(String name, ValueType type) {
        WasmFunction function = new WasmFunction(name);
        function.setExportName(name);
        function.setResult(WasmType.INT32);
        function.getParameters().add(WasmType.INT32);

        WasmLocal sizeLocal = new WasmLocal(WasmType.INT32, "size");
        function.add(sizeLocal);

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        String allocName = classGenerator.names.forMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(new WasmGetLocal(sizeLocal));

        function.getBody().add(new WasmReturn(call));

        return function;
    }

    private WasmFunction stringData() {
        WasmFunction function = new WasmFunction("teavm_stringData");
        function.setExportName(function.getName());
        function.setResult(WasmType.INT32);
        function.getParameters().add(WasmType.INT32);

        WasmLocal stringLocal = new WasmLocal(WasmType.INT32, "string");
        function.add(stringLocal);

        int offset = classGenerator.getFieldOffset(new FieldReference("java.lang.String", "characters"));
        WasmExpression chars = new WasmLoadInt32(4, new WasmGetLocal(stringLocal), WasmInt32Subtype.INT32, offset);

        function.getBody().add(new WasmReturn(chars));

        return function;
    }

    private WasmFunction arrayData(String name, int alignment) {
        WasmFunction function = new WasmFunction(name);
        function.setExportName(function.getName());
        function.setResult(WasmType.INT32);
        function.getParameters().add(WasmType.INT32);

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
        WasmFunction function = new WasmFunction("teavm_arrayLength");
        function.setExportName(function.getName());
        function.setResult(WasmType.INT32);
        function.getParameters().add(WasmType.INT32);

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
