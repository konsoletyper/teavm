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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.model.MethodReference;

public class IntNumIntrinsic implements WasmGCIntrinsic {
    private final MethodReference compareUnsigned;
    private final WasmIntType wasmType;

    public IntNumIntrinsic(Class<?> javaType, WasmIntType wasmType) {
        compareUnsigned = new MethodReference(WasmRuntime.class, "compareUnsigned", javaType, javaType, int.class);
        this.wasmType = wasmType;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "divideUnsigned":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.intBinary(wasmType, WasmIntBinaryOperation.DIV_UNSIGNED);
                break;
            case "remainderUnsigned":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.intBinary(wasmType, WasmIntBinaryOperation.REM_UNSIGNED);
                break;
            case "compareUnsigned":
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                builder.call(context.functions().forStaticMethod(compareUnsigned));
                break;
            case "numberOfLeadingZeros":
                context.generate(builder, invocation.getArguments().get(0));
                builder.intUnary(wasmType, WasmIntUnaryOperation.CLZ);
                castToInt(builder);
                break;
            case "numberOfTrailingZeros":
                context.generate(builder, invocation.getArguments().get(0));
                builder.intUnary(wasmType, WasmIntUnaryOperation.CTZ);
                castToInt(builder);
                break;
            case "bitCount":
                context.generate(builder, invocation.getArguments().get(0));
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
