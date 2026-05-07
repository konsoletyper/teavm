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
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

public class WasmRuntimeIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "lt":
                comparison(WasmIntBinaryOperation.LT_SIGNED, WasmFloatBinaryOperation.LT,
                        invocation, context, builder);
                break;
            case "gt":
                comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.GT,
                        invocation, context, builder);
                break;
            case "ltu":
                comparison(WasmIntBinaryOperation.LT_UNSIGNED, WasmFloatBinaryOperation.LT,
                        invocation, context, builder);
                break;
            case "gtu":
                comparison(WasmIntBinaryOperation.GT_UNSIGNED, WasmFloatBinaryOperation.GT,
                        invocation, context, builder);
                break;
            case "min":
                comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.MIN,
                        invocation, context, builder);
                break;
            case "max":
                comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.MAX,
                        invocation, context, builder);
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private static void comparison(WasmIntBinaryOperation intOp, WasmFloatBinaryOperation floatOp,
            InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        var type = (WasmType.Number) WasmGeneratorUtil.mapType(invocation.getMethod().parameterType(0));

        context.generate(builder, invocation.getArguments().get(0));
        context.generate(builder, invocation.getArguments().get(1));

        switch (type.number) {
            case INT32:
                builder.intBinary(WasmIntType.INT32, intOp);
                break;
            case INT64:
                builder.intBinary(WasmIntType.INT64, intOp);
                break;
            case FLOAT32:
                builder.floatBinary(WasmFloatType.FLOAT32, floatOp);
                break;
            case FLOAT64:
                builder.floatBinary(WasmFloatType.FLOAT64, floatOp);
                break;
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }
}
