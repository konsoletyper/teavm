/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics;

import java.util.stream.Collectors;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmExpression;
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
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.interop.Address;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeArray;

public class AddressIntrinsic implements WasmIntrinsic {
    private WasmClassGenerator classGenerator;

    public AddressIntrinsic(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.getClassName().equals(Address.class.getName());
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "toInt":
            case "toStructure":
                return manager.generate(invocation.getArguments().get(0));
            case "toLong": {
                WasmExpression value = manager.generate(invocation.getArguments().get(0));
                return new WasmConversion(WasmType.INT32, WasmType.INT64, false, value);
            }
            case "fromInt":
            case "ofObject":
                return manager.generate(invocation.getArguments().get(0));
            case "fromLong": {
                WasmExpression value = manager.generate(invocation.getArguments().get(0));
                return new WasmConversion(WasmType.INT64, WasmType.INT32, false, value);
            }
            case "add": {
                WasmExpression base = manager.generate(invocation.getArguments().get(0));
                if (invocation.getMethod().parameterCount() == 1) {
                    WasmExpression offset = manager.generate(invocation.getArguments().get(1));
                    if (invocation.getMethod().parameterType(0) == ValueType.LONG) {
                        offset = new WasmConversion(WasmType.INT64, WasmType.INT32, false, offset);
                    }
                    return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
                } else {
                    WasmExpression offset = manager.generate(invocation.getArguments().get(2));
                    Object type = ((ConstantExpr) invocation.getArguments().get(1)).getValue();
                    String className = ((ValueType.Object) type).getClassName();
                    int size = classGenerator.getClassSize(className);
                    int alignment = classGenerator.getClassAlignment(className);
                    size = WasmClassGenerator.align(size, alignment);

                    offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL, offset,
                            new WasmInt32Constant(size));
                    return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
                }
            }
            case "getByte":
                return new WasmLoadInt32(1, manager.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT8);
            case "getShort":
                return new WasmLoadInt32(2, manager.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT16);
            case "getChar":
                return new WasmLoadInt32(2, manager.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.UINT16);
            case "getAddress":
            case "getInt":
                return new WasmLoadInt32(4, manager.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT32);
            case "getLong":
                return new WasmLoadInt64(8, manager.generate(invocation.getArguments().get(0)),
                        WasmInt64Subtype.INT64);
            case "getFloat":
                return new WasmLoadFloat32(4, manager.generate(invocation.getArguments().get(0)));
            case "getDouble":
                return new WasmLoadFloat64(8, manager.generate(invocation.getArguments().get(0)));
            case "putByte": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(1, address, value, WasmInt32Subtype.INT8);
            }
            case "putShort": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(2, address, value, WasmInt32Subtype.INT16);
            }
            case "putChar": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(2, address, value, WasmInt32Subtype.UINT16);
            }
            case "putAddress":
            case "putInt": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(4, address, value, WasmInt32Subtype.INT32);
            }
            case "putLong": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreInt64(8, address, value, WasmInt64Subtype.INT64);
            }
            case "putFloat": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreFloat32(4, address, value);
            }
            case "putDouble": {
                WasmExpression address = manager.generate(invocation.getArguments().get(0));
                WasmExpression value = manager.generate(invocation.getArguments().get(1));
                return new WasmStoreFloat64(8, address, value);
            }
            case "sizeOf":
                return new WasmInt32Constant(4);
            case "align": {
                MethodReference delegate = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                WasmCall call = new WasmCall(manager.getNames().forMethod(delegate));
                call.getArguments().addAll(invocation.getArguments().stream()
                        .map(arg -> manager.generate(arg))
                        .collect(Collectors.toList()));
                return call;
            }
            case "isLessThan":
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED,
                        manager.generate(invocation.getArguments().get(0)),
                        manager.generate(invocation.getArguments().get(1)));
            case "ofData": {
                ValueType.Array type = (ValueType.Array) invocation.getMethod().parameterType(0);
                int alignment = getAlignment(type.getItemType());
                int start = WasmClassGenerator.align(classGenerator.getClassSize(RuntimeArray.class.getName()),
                        alignment);
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                        manager.generate(invocation.getArguments().get(0)), new WasmInt32Constant(start));
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }

    private static int getAlignment(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return 1;
                case SHORT:
                case CHARACTER:
                    return 2;
                case INTEGER:
                case FLOAT:
                    return 4;
                case LONG:
                case DOUBLE:
                    return 8;
            }
        }
        return 4;
    }
}
