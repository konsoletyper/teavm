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

import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInt32Subtype;
import org.teavm.backend.wasm.model.instruction.WasmInt64Subtype;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;

public class AddressIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;
    private final BaseWasmFunctionRepository functions;

    public AddressIntrinsic(WasmGCClassInfoProvider classInfoProvider, BaseWasmFunctionRepository functions) {
        this.classInfoProvider = classInfoProvider;
        this.functions = functions;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "toInt":
            case "toStructure":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                break;
            case "fromInt":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                break;
            case "toLong":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.convert(WasmNumType.INT32, WasmNumType.INT64, false);
                break;
            case "fromLong":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.convert(WasmNumType.INT64, WasmNumType.INT32, false);
                break;
            case "add": {
                if (invocation.getMethod().parameterCount() == 1) {
                    builder.getLocal(context.mapToLocal(invocation.getInstance()));
                    builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                    if (invocation.getMethod().parameterType(0) == ValueType.LONG) {
                        builder.convert(WasmNumType.INT64, WasmNumType.INT32, false);
                    }
                } else {
                    builder.getLocal(context.mapToLocal(invocation.getInstance()));
                    var type = context.classConstant(invocation.getArguments().get(0));
                    var className = ((ValueType.Object) type).getClassName();
                    int size = classInfoProvider.getHeapSize(className);
                    int alignment = classInfoProvider.getHeapAlignment(className);
                    size = WasmGeneratorUtil.align(size, alignment);
                    builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
                    builder.i32Const(size).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL);
                }
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
                break;
            }
            case "getByte":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadI32(1, 0, WasmInt32Subtype.INT8);
                break;
            case "getShort":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadI32(2, 0, WasmInt32Subtype.INT16);
                break;
            case "getChar":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadI32(2, 0, WasmInt32Subtype.UINT16);
                break;
            case "getAddress":
            case "getInt":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadI32(4, 0, WasmInt32Subtype.INT32);
                break;
            case "getLong":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadI64(8, 0, WasmInt64Subtype.INT64);
                break;
            case "getFloat":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadF32(4, 0);
                break;
            case "getDouble":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.loadF64(8, 0);
                break;
            case "putByte":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeI32(1, 0, WasmInt32Subtype.INT8);
                break;
            case "putShort":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeI32(2, 0, WasmInt32Subtype.INT16);
                break;
            case "putChar":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeI32(2, 0, WasmInt32Subtype.UINT16);
                break;
            case "putAddress":
            case "putInt":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeI32(4, 0, WasmInt32Subtype.INT32);
                break;
            case "putLong":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeI64(8, 0, WasmInt64Subtype.INT64);
                break;
            case "putFloat":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeF32(4, 0);
                break;
            case "putDouble":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.storeF64(8, 0);
                break;
            case "sizeOf":
                builder.i32Const(4);
                break;
            case "align": {
                var delegate = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                for (var arg : invocation.getArguments()) {
                    builder.getLocal(context.mapToLocal(arg));
                }
                builder.call(functions.forStaticMethod(delegate));
                break;
            }
            case "isLessThan":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED);
                break;
            case "diff":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB)
                        .convert(WasmNumType.INT32, WasmNumType.INT64, true);
                break;
            case "fill":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(2)));
                builder.fill();
                break;
            case "fillZero":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.i32Const(0);
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
                builder.fill();
                break;
            case "moveMemoryBlock":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(2)));
                builder.copy();
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
