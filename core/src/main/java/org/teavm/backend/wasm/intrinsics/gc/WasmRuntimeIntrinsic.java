/*
 *  Copyright 2024 Alexey Andreev.
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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;

public class WasmRuntimeIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "lt":
                return comparison(WasmIntBinaryOperation.LT_SIGNED, WasmFloatBinaryOperation.LT,
                        invocation, context);
            case "gt":
                return comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.GT,
                        invocation, context);
            case "ltu":
                return comparison(WasmIntBinaryOperation.LT_UNSIGNED, WasmFloatBinaryOperation.LT,
                        invocation, context);
            case "gtu":
                return comparison(WasmIntBinaryOperation.GT_UNSIGNED, WasmFloatBinaryOperation.GT,
                        invocation, context);
            case "min":
                return comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.MIN,
                        invocation, context);
            case "max":
                return comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.MAX,
                        invocation, context);
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private static WasmExpression comparison(WasmIntBinaryOperation intOp, WasmFloatBinaryOperation floatOp,
            InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var type = (WasmType.Number) WasmGeneratorUtil.mapType(invocation.getMethod().parameterType(0));

        WasmExpression first = context.generate(invocation.getArguments().get(0));
        WasmExpression second = context.generate(invocation.getArguments().get(1));

        switch (type.number) {
            case INT32:
                return new WasmIntBinary(WasmIntType.INT32, intOp, first, second);
            case INT64:
                return new WasmIntBinary(WasmIntType.INT64, intOp, first, second);
            case FLOAT32:
                return new WasmFloatBinary(WasmFloatType.FLOAT32, floatOp, first, second);
            case FLOAT64:
                return new WasmFloatBinary(WasmFloatType.FLOAT64, floatOp, first, second);
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }
}
