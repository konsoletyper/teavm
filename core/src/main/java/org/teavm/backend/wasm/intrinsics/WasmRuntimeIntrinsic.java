/*
 *  Copyright 2016 Alexey Andreev.
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
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.model.MethodReference;

public class WasmRuntimeIntrinsic implements WasmIntrinsic {
    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(WasmRuntime.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "gt":
            case "lt":
            case "initStack":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "lt":
                return comparison(WasmIntBinaryOperation.LT_SIGNED, WasmFloatBinaryOperation.LT,
                        invocation, manager);
            case "gt":
                return comparison(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.GT,
                        invocation, manager);
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private static WasmExpression comparison(WasmIntBinaryOperation intOp, WasmFloatBinaryOperation floatOp,
            InvocationExpr invocation, WasmIntrinsicManager manager) {
        WasmType type = WasmGeneratorUtil.mapType(invocation.getMethod().parameterType(0));

        WasmExpression first = manager.generate(invocation.getArguments().get(0));
        WasmExpression second = manager.generate(invocation.getArguments().get(1));

        switch (type) {
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
