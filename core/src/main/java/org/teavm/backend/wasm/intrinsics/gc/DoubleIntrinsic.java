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
package org.teavm.backend.wasm.intrinsics.gc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;

public class DoubleIntrinsic implements WasmGCIntrinsic {
    private static final long EXPONENT_BITS = 0x7FF0000000000000L;
    private static final long FRACTION_BITS = 0x000FFFFFFFFFFFFFL;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "getNaN":
                return new WasmFloat64Constant(Double.NaN);
            case "isNaN":
                return testNaN(context.generate(invocation.getArguments().get(0)), context);
            case "isInfinite":
                return testIsInfinite(context.generate(invocation.getArguments().get(0)));
            case "isFinite":
                return testIsFinite(context.generate(invocation.getArguments().get(0)));
            case "doubleToRawLongBits": {
                WasmConversion conversion = new WasmConversion(WasmNumType.FLOAT64, WasmNumType.INT64, false,
                        context.generate(invocation.getArguments().get(0)));
                conversion.setReinterpret(true);
                return conversion;
            }
            case "longBitsToDouble": {
                WasmConversion conversion = new WasmConversion(WasmNumType.INT64, WasmNumType.FLOAT64, false,
                        context.generate(invocation.getArguments().get(0)));
                conversion.setReinterpret(true);
                return conversion;
            }
            default:
                throw new AssertionError();
        }
    }

    private WasmExpression testNaN(WasmExpression expression, WasmGCIntrinsicContext context) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        var cache = context.exprCache().create(expression, WasmType.FLOAT64, expression.getLocation(),
                block.getBody());
        block.getBody().add(new WasmFloatBinary(WasmFloatType.FLOAT64, WasmFloatBinaryOperation.NE,
                cache.expr(), cache.expr()));
        cache.release();
        return block;
    }

    private WasmExpression testIsInfinite(WasmExpression expression) {
        var conversion = new WasmConversion(WasmNumType.FLOAT64, WasmNumType.INT64, false, expression);
        conversion.setReinterpret(true);

        var result = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.AND,
                conversion, new WasmInt64Constant(EXPONENT_BITS));
        return new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.EQ, result,
                new WasmInt64Constant(EXPONENT_BITS));
    }

    private WasmExpression testIsFinite(WasmExpression expression) {
        var conversion = new WasmConversion(WasmNumType.FLOAT64, WasmNumType.INT64, false, expression);
        conversion.setReinterpret(true);

        var result = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.AND,
                conversion, new WasmInt64Constant(EXPONENT_BITS));
        return new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.NE, result,
                new WasmInt64Constant(EXPONENT_BITS));
    }
}
