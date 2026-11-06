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
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.model.MethodReference;
import org.teavm.model.instructions.InvokeInstruction;

public class IntNumIntrinsic implements WasmGCInlineIntrinsic {
    private final MethodReference compareUnsigned;
    private final WasmIntType wasmType;
    private final BaseWasmFunctionRepository functions;

    public IntNumIntrinsic(Class<?> javaType, WasmIntType wasmType, BaseWasmFunctionRepository functions) {
        compareUnsigned = new MethodReference(WasmRuntime.class, "compareUnsigned", javaType, javaType, int.class);
        this.wasmType = wasmType;
        this.functions = functions;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "divideUnsigned":
                context.emitLocals(builder, invocation.getArguments().get(0), invocation.getArguments().get(1))
                        .intBinary(wasmType, WasmIntBinaryOperation.DIV_UNSIGNED);
                break;
            case "remainderUnsigned":
                context.emitLocals(builder, invocation.getArguments().get(0), invocation.getArguments().get(1))
                        .intBinary(wasmType, WasmIntBinaryOperation.REM_UNSIGNED);
                break;
            case "compareUnsigned":
                context.emitLocals(builder, invocation.getArguments().get(0), invocation.getArguments().get(1))
                        .call(functions.forStaticMethod(compareUnsigned));
                break;
            case "numberOfLeadingZeros":
                context.emitLocal(builder, invocation.getArguments().get(0));
                builder.intUnary(wasmType, WasmIntUnaryOperation.CLZ);
                castToInt(builder);
                break;
            case "numberOfTrailingZeros":
                context.emitLocal(builder, invocation.getArguments().get(0));
                builder.intUnary(wasmType, WasmIntUnaryOperation.CTZ);
                castToInt(builder);
                break;
            case "bitCount":
                context.emitLocal(builder, invocation.getArguments().get(0));
                builder.intUnary(wasmType, WasmIntUnaryOperation.POPCNT);
                castToInt(builder);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void castToInt(WasmInstructionBuilder builder) {
        if (wasmType == WasmIntType.INT64) {
            builder.convert(WasmNumType.INT64, WasmNumType.INT32, false);
        }
    }
}
