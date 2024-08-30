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
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;

public class FloatIntrinsic implements WasmGCIntrinsic {
    private static final int EXPONENT_BITS = 0x7F800000;
    private static final int FRACTION_BITS = 0x007FFFFF;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext manager) {
        switch (invocation.getMethod().getName()) {
            case "getNaN":
                return new WasmFloat32Constant(Float.NaN);
            case "isNaN":
                return testNaN(manager.generate(invocation.getArguments().get(0)), manager);
            case "isInfinite":
                return testIsInfinite(manager.generate(invocation.getArguments().get(0)));
            case "isFinite":
                return testIsFinite(manager.generate(invocation.getArguments().get(0)));
            case "floatToRawIntBits": {
                var conversion = new WasmConversion(WasmNumType.FLOAT32, WasmNumType.INT32, false,
                        manager.generate(invocation.getArguments().get(0)));
                conversion.setReinterpret(true);
                return conversion;
            }
            case "intBitsToFloat": {
                var conversion = new WasmConversion(WasmNumType.INT32, WasmNumType.FLOAT32, false,
                        manager.generate(invocation.getArguments().get(0)));
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
        var cache = context.exprCache().create(expression, WasmType.FLOAT32, expression.getLocation(),
                block.getBody());
        block.getBody().add(new WasmFloatBinary(WasmFloatType.FLOAT32, WasmFloatBinaryOperation.NE,
                cache.expr(), cache.expr()));
        cache.release();
        return block;
    }

    private WasmExpression testIsInfinite(WasmExpression expression) {
        var conversion = new WasmConversion(WasmNumType.FLOAT32, WasmNumType.INT32, false, expression);
        conversion.setReinterpret(true);

        var result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND,
                conversion, new WasmInt32Constant(EXPONENT_BITS));
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, result,
                new WasmInt32Constant(EXPONENT_BITS));
    }

    private WasmExpression testIsFinite(WasmExpression expression) {
        var conversion = new WasmConversion(WasmNumType.FLOAT32, WasmNumType.INT32, false, expression);
        conversion.setReinterpret(true);

        var result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND,
                conversion, new WasmInt32Constant(EXPONENT_BITS));
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.NE, result,
                new WasmInt32Constant(EXPONENT_BITS));
    }
}
