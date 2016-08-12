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

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.interop.Address;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.wasm.generate.WasmClassGenerator;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmInt32Constant;

public class WasmStructureIntrinsic implements WasmIntrinsic {
    private WasmClassGenerator classGenerator;

    public WasmStructureIntrinsic(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return !methodReference.getClassName().equals(Address.class.getName())
                && classGenerator.getClassPointer(methodReference.getClassName()) < 0;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "toAddress":
            case "cast":
                return manager.generate(invocation.getArguments().get(0));
            case "sizeOf": {
                ValueType.Object type = (ValueType.Object) ((ConstantExpr) invocation.getArguments().get(0)).getValue();
                return new WasmInt32Constant(classGenerator.getClassSize(type.getClassName()));
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
