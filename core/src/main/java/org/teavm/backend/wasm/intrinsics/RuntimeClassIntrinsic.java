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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.model.MethodReference;
import org.teavm.runtime.RuntimeClass;

public class RuntimeClassIntrinsic implements WasmIntrinsic {
    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(RuntimeClass.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "pack":
            case "unpack":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "pack":
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED,
                        manager.generate(invocation.getArguments().get(0)), new WasmInt32Constant(3));
            case "unpack":
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        manager.generate(invocation.getArguments().get(0)), new WasmInt32Constant(3));
            default:
                throw new AssertionError();
        }
    }
}
