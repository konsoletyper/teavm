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
import org.teavm.ast.InvocationType;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.MethodReference;
import org.teavm.runtime.ShadowStack;

public class ShadowStackIntrinsic implements WasmIntrinsic {
    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(ShadowStack.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "getStackTop":
            case "getNextStackFrame":
            case "getStackRootCount":
            case "getStackRootPointer":
            case "getCallSiteId":
            case "setExceptionHandlerId":
            case "setExceptionHandlerSkip":
            case "setExceptionHandlerRestore":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        InvocationExpr expr = new InvocationExpr();
        MethodReference method = new MethodReference(WasmRuntime.class.getName(),
                invocation.getMethod().getDescriptor());
        expr.setMethod(method);
        expr.setType(InvocationType.SPECIAL);
        expr.getArguments().addAll(invocation.getArguments());
        return manager.generate(expr);
    }
}
