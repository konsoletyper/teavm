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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.model.MethodReference;

public class DoubleIntrinsic implements WasmIntrinsic {
    private static final long EXPONENT_BITS = 0x7FF0000000000000L;
    private static final long FRACTION_BITS = 0x000FFFFFFFFFFFFFL;

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Double.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "getNaN":
            case "isNaN":
            case "isInfinite":
            case "doubleToLongBits":
            case "longBitsToDouble":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getNaN":
                return new WasmFloat64Constant(Double.NaN);
            case "isNaN":
                return testSpecialIEEE(manager.generate(invocation.getArguments().get(0)), manager,
                        WasmIntBinaryOperation.NE);
            case "isInfinite":
                return testSpecialIEEE(manager.generate(invocation.getArguments().get(0)), manager,
                    WasmIntBinaryOperation.EQ);
            case "doubleToLongBits": {
                WasmConversion conversion = new WasmConversion(WasmType.FLOAT64, WasmType.INT64, false,
                        manager.generate(invocation.getArguments().get(0)));
                conversion.setReinterpret(true);
                return conversion;
            }
            case "longBitsToDouble": {
                WasmConversion conversion = new WasmConversion(WasmType.INT64, WasmType.FLOAT64, false,
                        manager.generate(invocation.getArguments().get(0)));
                conversion.setReinterpret(true);
                return conversion;
            }
            default:
                throw new AssertionError();
        }
    }

    private WasmExpression testSpecialIEEE(WasmExpression expression, WasmIntrinsicManager manager,
            WasmIntBinaryOperation fractionOp) {
        WasmLocal bitsVar = manager.getTemporary(WasmType.INT64);
        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        WasmConversion conversion = new WasmConversion(WasmType.FLOAT64, WasmType.INT64, false, expression);
        conversion.setReinterpret(true);
        block.getBody().add(new WasmSetLocal(bitsVar, conversion));

        WasmExpression exponentBits = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.AND,
                new WasmGetLocal(bitsVar), new WasmInt64Constant(EXPONENT_BITS));
        WasmExpression fractionBits = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.AND,
                new WasmGetLocal(bitsVar), new WasmInt64Constant(FRACTION_BITS));
        WasmExpression testExponent = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.NE,
                exponentBits, new WasmInt64Constant(EXPONENT_BITS));
        WasmExpression testFraction = new WasmIntBinary(WasmIntType.INT64, fractionOp,
                fractionBits, new WasmInt64Constant(0));

        WasmBranch breakIfWrongExponent = new WasmBranch(testExponent, block);
        breakIfWrongExponent.setResult(new WasmInt32Constant(0));
        block.getBody().add(new WasmDrop(breakIfWrongExponent));

        block.getBody().add(testFraction);
        return block;
    }
}
