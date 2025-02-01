/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics.gc;

import java.util.stream.Collectors;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.WasmNumType;
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
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class AddressIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "toInt":
            case "toStructure":
                return context.generate(invocation.getArguments().get(0));
            case "toLong": {
                var value = context.generate(invocation.getArguments().get(0));
                return new WasmConversion(WasmNumType.INT32, WasmNumType.INT64, false, value);
            }
            case "fromInt":
                return context.generate(invocation.getArguments().get(0));
            case "fromLong": {
                var value = context.generate(invocation.getArguments().get(0));
                return new WasmConversion(WasmNumType.INT64, WasmNumType.INT32, false, value);
            }
            case "add": {
                var base = context.generate(invocation.getArguments().get(0));
                if (invocation.getMethod().parameterCount() == 1) {
                    var offset = context.generate(invocation.getArguments().get(1));
                    if (invocation.getMethod().parameterType(0) == ValueType.LONG) {
                        offset = new WasmConversion(WasmNumType.INT64, WasmNumType.INT32, false, offset);
                    }
                    return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
                } else {
                    var offset = context.generate(invocation.getArguments().get(2));
                    var type = ((ConstantExpr) invocation.getArguments().get(1)).getValue();
                    var className = ((ValueType.Object) type).getClassName();
                    int size = context.classInfoProvider().getHeapSize(className);
                    int alignment = context.classInfoProvider().getHeapAlignment(className);
                    size = WasmClassGenerator.align(size, alignment);

                    offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL, offset,
                            new WasmInt32Constant(size));
                    return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
                }
            }
            case "getByte":
                return new WasmLoadInt32(1, context.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT8);
            case "getShort":
                return new WasmLoadInt32(2, context.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT16);
            case "getChar":
                return new WasmLoadInt32(2, context.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.UINT16);
            case "getAddress":
            case "getInt":
                return new WasmLoadInt32(4, context.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT32);
            case "getLong":
                return new WasmLoadInt64(8, context.generate(invocation.getArguments().get(0)),
                        WasmInt64Subtype.INT64);
            case "getFloat":
                return new WasmLoadFloat32(4, context.generate(invocation.getArguments().get(0)));
            case "getDouble":
                return new WasmLoadFloat64(8, context.generate(invocation.getArguments().get(0)));
            case "putByte": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(1, address, value, WasmInt32Subtype.INT8);
            }
            case "putShort": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(2, address, value, WasmInt32Subtype.INT16);
            }
            case "putChar": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(2, address, value, WasmInt32Subtype.UINT16);
            }
            case "putAddress":
            case "putInt": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreInt32(4, address, value, WasmInt32Subtype.INT32);
            }
            case "putLong": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreInt64(8, address, value, WasmInt64Subtype.INT64);
            }
            case "putFloat": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreFloat32(4, address, value);
            }
            case "putDouble": {
                var address = context.generate(invocation.getArguments().get(0));
                var value = context.generate(invocation.getArguments().get(1));
                return new WasmStoreFloat64(8, address, value);
            }
            case "sizeOf":
                return new WasmInt32Constant(4);
            case "align": {
                var delegate = new MethodReference(WasmRuntime.class.getName(), invocation.getMethod().getDescriptor());
                var call = new WasmCall(context.functions().forStaticMethod(delegate));
                call.getArguments().addAll(invocation.getArguments().stream()
                        .map(context::generate)
                        .collect(Collectors.toList()));
                return call;
            }
            case "isLessThan":
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED,
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)));
            case "diff": {
                WasmExpression result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1))
                );
                result = new WasmConversion(WasmNumType.INT32, WasmNumType.INT64, true, result);
                result.setLocation(invocation.getLocation());
                return result;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }

    private static int getAlignment(ValueType type) {
        return WasmClassGenerator.getTypeSize(type);
    }
}
