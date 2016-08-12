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
package org.teavm.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.model.MethodReference;
import org.teavm.runtime.RuntimeClass;
import org.teavm.wasm.generate.WasmClassGenerator;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmInt32Constant;

public class WasmRuntimeClassIntrinsic implements WasmIntrinsic {
    private WasmClassGenerator classGenerator;

    public WasmRuntimeClassIntrinsic(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(RuntimeClass.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "getArrayClass":
                return true;
        }

        return false;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getArrayClass":
                return new WasmInt32Constant(classGenerator.getArrayClassPointer());
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
