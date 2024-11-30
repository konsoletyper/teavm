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
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.model.MethodReference;

public class IntNumIntrinsic implements WasmGCIntrinsic {
    private final MethodReference compareUnsigned;
    private final WasmIntType wasmType;

    public IntNumIntrinsic(Class<?> javaType, WasmIntType wasmType) {
        compareUnsigned = new MethodReference(WasmRuntime.class, "compareUnsigned", javaType, javaType, int.class);
        this.wasmType = wasmType;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "divideUnsigned":
                return new WasmIntBinary(wasmType, WasmIntBinaryOperation.DIV_UNSIGNED,
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)));
            case "remainderUnsigned":
                return new WasmIntBinary(wasmType, WasmIntBinaryOperation.REM_UNSIGNED,
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)));
            case "compareUnsigned":
                return new WasmCall(context.functions().forStaticMethod(compareUnsigned),
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)));
            case "numberOfLeadingZeros":
                return new WasmIntUnary(wasmType, WasmIntUnaryOperation.CLZ,
                        context.generate(invocation.getArguments().get(0)));
            case "numberOfTrailingZeros":
                return new WasmIntUnary(wasmType, WasmIntUnaryOperation.CTZ,
                        context.generate(invocation.getArguments().get(0)));
            case "bitCount":
                return new WasmIntUnary(wasmType, WasmIntUnaryOperation.POPCNT,
                        context.generate(invocation.getArguments().get(0)));
            default:
                throw new AssertionError();
        }
    }
}
