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
package org.teavm.backend.wasm.generators;

import java.lang.reflect.Array;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;

public class ArrayGenerator implements WasmMethodGenerator {
    private static FieldReference componentField = new FieldReference(RuntimeClass.class.getName(), "itemType");
    private static FieldReference flagsField =  new FieldReference(RuntimeClass.class.getName(), "flags");
    private static final String[] primitiveWrappers = { "Byte", "Short", "Character", "Integer", "Long",
            "Float", "Double", "Boolean" };
    private static final ValueType.Primitive[] primitiveTypes = { ValueType.BYTE, ValueType.SHORT, ValueType.CHARACTER,
            ValueType.INTEGER, ValueType.LONG, ValueType.FLOAT, ValueType.DOUBLE, ValueType.BOOLEAN };
    private static final int[] shift = new int[] { 0, 1, 1, 2, 3, 2, 3, 0 };

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Array.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "getImpl":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmMethodGeneratorContext context) {
        WasmLocal arrayVar = new WasmLocal(WasmType.INT32, "_array");
        WasmLocal indexVar = new WasmLocal(WasmType.INT32, "_index");
        WasmLocal flagsVar = new WasmLocal(WasmType.INT32, "_flags");
        function.add(arrayVar);
        function.add(indexVar);
        function.add(flagsVar);

        WasmExpression arrayClass = new WasmLoadInt32(4, new WasmGetLocal(arrayVar), WasmInt32Subtype.INT32);
        arrayClass = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, arrayClass,
                new WasmInt32Constant(3));

        int componentOffset = context.getClassGenerator().getFieldOffset(componentField);
        WasmExpression componentClass = new WasmLoadInt32(4, arrayClass, WasmInt32Subtype.INT32, componentOffset);

        int flagsOffset = context.getClassGenerator().getFieldOffset(flagsField);
        WasmExpression flags = new WasmLoadInt32(4, componentClass, WasmInt32Subtype.INT32, flagsOffset);
        function.getBody().add(new WasmSetLocal(flagsVar, flags));

        WasmExpression isPrimitive = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND,
                new WasmGetLocal(flagsVar), new WasmInt32Constant(RuntimeClass.PRIMITIVE));
        WasmBlock objectBlock = new WasmBlock(false);
        objectBlock.getBody().add(new WasmBranch(isPrimitive, objectBlock));
        function.getBody().add(objectBlock);
        int base = context.getClassGenerator().getClassSize(RuntimeArray.class.getName());

        WasmBlock currentBlock = new WasmBlock(false);
        function.getBody().add(currentBlock);
        function.getBody().add(new WasmReturn(new WasmInt32Constant(0)));

        WasmSwitch primitiveSwitch = new WasmSwitch(new WasmGetLocal(flagsVar), currentBlock);
        for (int i = 0; i <= 8; ++i) {
            primitiveSwitch.getTargets().add(currentBlock);
        }

        for (int i = 0; i < primitiveWrappers.length; ++i) {
            String wrapper = "java.lang." + primitiveWrappers[i];
            MethodReference methodRef = new MethodReference(wrapper, "valueOf",
                    primitiveTypes[i], ValueType.object(wrapper));
            ClassReader cls = context.getClassSource().get(methodRef.getClassName());
            if (cls == null || cls.getMethod(methodRef.getDescriptor()) == null) {
                continue;
            }

            WasmBlock nextBlock = new WasmBlock(false);
            primitiveSwitch.getTargets().set(i, nextBlock);

            WasmExpression offset = new WasmGetLocal(indexVar);
            if (shift[i] != 0) {
                offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        new WasmGetLocal(indexVar), new WasmInt32Constant(shift[i]));
            }
            offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, offset,
                    new WasmGetLocal(arrayVar));
            int baseAddr = BinaryWriter.align(base, 1 << shift[i]);

            WasmCall call = new WasmCall(context.getNames().forMethod(methodRef));

            switch (primitiveTypes[i].getKind()) {
                case BOOLEAN:
                case BYTE:
                    call.getArguments().add(new WasmLoadInt32(0, offset, WasmInt32Subtype.INT8, baseAddr));
                    break;
                case SHORT:
                    call.getArguments().add(new WasmLoadInt32(1, offset, WasmInt32Subtype.INT16, baseAddr));
                    break;
                case CHARACTER:
                    call.getArguments().add(new WasmLoadInt32(1, offset, WasmInt32Subtype.UINT16, baseAddr));
                    break;
                case INTEGER:
                    call.getArguments().add(new WasmLoadInt32(2, offset, WasmInt32Subtype.INT16, baseAddr));
                    break;
                case LONG:
                    call.getArguments().add(new WasmLoadInt64(3, offset, WasmInt64Subtype.INT64, baseAddr));
                    break;
                case FLOAT:
                    call.getArguments().add(new WasmLoadFloat32(2, offset, baseAddr));
                    break;
                case DOUBLE:
                    call.getArguments().add(new WasmLoadFloat64(3, offset, baseAddr));
                    break;
            }

            currentBlock.getBody().add(nextBlock);
            currentBlock.getBody().add(new WasmReturn(call));
            currentBlock = nextBlock;
        }

        currentBlock.getBody().add(primitiveSwitch);

        WasmExpression objectOffset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                new WasmGetLocal(indexVar), new WasmInt32Constant(2));
        objectOffset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, objectOffset,
                new WasmGetLocal(arrayVar));
        objectBlock.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, objectOffset, WasmInt32Subtype.INT32, BinaryWriter.align(base, 4))));
    }
}
