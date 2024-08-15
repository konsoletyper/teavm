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
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.interop.Function;
import org.teavm.model.MethodReference;

public class FunctionIntrinsic implements WasmIntrinsic {
    private WasmClassGenerator classGenerator;

    public FunctionIntrinsic(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (methodReference.getClassName().equals(Function.class.getName())
                && methodReference.getName().equals("isNull")) {
            return false;
        }
        return classGenerator.isFunctionClass(methodReference.getClassName());
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        var parameterTypes = new WasmType[invocation.getMethod().parameterCount()];
        for (var i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = WasmGeneratorUtil.mapType(invocation.getMethod().parameterType(i));
        }
        var returnType = WasmGeneratorUtil.mapType(invocation.getMethod().getReturnType());
        var functionType = manager.getFunctionTypes().of(returnType, parameterTypes);

        var selector = manager.generate(invocation.getArguments().get(0));
        var call = new WasmIndirectCall(selector, functionType);

        for (int i = 1; i < invocation.getArguments().size(); ++i) {
            call.getArguments().add(manager.generate(invocation.getArguments().get(i)));
        }

        return call;
    }
}
