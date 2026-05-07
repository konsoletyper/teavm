/*
 *  Copyright 2026 Alexey Andreev.
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

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class AddressIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "toInt":
            case "toStructure":
                context.generate(builder, invocation.getArguments().get(0));
                break;
            case "toLong":
                context.generate(builder, invocation.getArguments().get(0));
                builder.convert(WasmNumType.INT32, WasmNumType.INT64, false);
                break;
            case "fromInt":
                context.generate(builder, invocation.getArguments().get(0));
                break;
            case "fromLong":
                context.generate(builder, invocation.getArguments().get(0));
                builder.convert(WasmNumType.INT64, WasmNumType.INT32, false);
                break;
            case "add": {
                context.generate(builder, invocation.getArguments().get(0));
                if (invocation.getMethod().parameterCount() == 1) {
                    context.generate(builder, invocation.getArguments().get(1));
                    if (invocation.getMethod().parameterType(0) == ValueType.LONG) {
                        builder.convert(WasmNumType.INT64, WasmNumType.INT32, false);
                    }
                } else {
                    var type = ((ConstantExpr) invocation.getArguments().get(1)).getValue();
                    var className = ((ValueType.Object) type).getClassName();
                    int size = context.classInfoProvider().getHeapSize(className);
                    int alignment = context.classInfoProvider().getHeapAlignment(className);
                    size = WasmGeneratorUtil.align(size, alignment);
                    context.generate(builder, invocation.getArguments().get(2));
                    builder.i32Const(size).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL);
                }
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
                break;
            }
            case "getByte":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI32(1, 0, WasmInt32Subtype.INT8);
                break;
            case "getShort":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI32(2, 0, WasmInt32Subtype.INT16);
                break;
            case "getChar":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI32(2, 0, WasmInt32Subtype.UINT16);
                break;
            case "getAddress":
            case "getInt":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI32(4, 0, WasmInt32Subtype.INT32);
                break;
            case "getLong":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI64(8, 0, WasmInt64Subtype.INT64);
                break;
            case "getFloat":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadF32(4, 0);
                break;
            case "getDouble":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadF64(8, 0);
                break;
            case "putByte":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeI32(1, 0, WasmInt32Subtype.INT8);
                break;
            case "putShort":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeI32(2, 0, WasmInt32Subtype.INT16);
                break;
            case "putChar":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeI32(2, 0, WasmInt32Subtype.UINT16);
                break;
            case "putAddress":
            case "putInt":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeI32(4, 0, WasmInt32Subtype.INT32);
                break;
            case "putLong":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeI64(8, 0, WasmInt64Subtype.INT64);
                break;
            case "putFloat":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeF32(4, 0);
                break;
            case "putDouble":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.storeF64(8, 0);
                break;
            case "sizeOf":
                builder.i32Const(4);
                break;
            case "align": {
                var delegate = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                for (var arg : invocation.getArguments()) {
                    context.generate(builder, arg);
                }
                builder.call(context.functions().forStaticMethod(delegate));
                break;
            }
            case "isLessThan":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED);
                break;
            case "diff":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB)
                        .convert(WasmNumType.INT32, WasmNumType.INT64, true);
                break;
            case "fill":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                context.generate(builder, invocation.getArguments().get(2));
                builder.fill();
                break;
            case "fillZero":
                context.generate(builder, invocation.getArguments().get(0));
                builder.i32Const(0);
                context.generate(builder, invocation.getArguments().get(1));
                builder.fill();
                break;
            case "moveMemoryBlock":
                context.generate(builder, invocation.getArguments().get(1));
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(2));
                builder.copy();
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
