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
package org.teavm.model.instructions;

import java.util.ArrayList;
import java.util.List;
import org.teavm.ast.*;
import org.teavm.ast.InvocationType;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Mutator;

public class MutatorIntrinsic implements WasmIntrinsic {
    private List<WasmInt32Constant> staticGcRootsExpressions = new ArrayList<>();

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Mutator.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "getStaticGcRoots":
            case "getStackGcRoots":
            case "getNextStackRoots":
            case "getStackRootCount":
            case "getStackRootPointer":
                return true;
            default:
                return false;
        }
    }

    public void setStaticGcRootsAddress(int address) {
        for (WasmInt32Constant constant : staticGcRootsExpressions) {
            constant.setValue(address);
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getStaticGcRoots": {
                WasmInt32Constant constant = new WasmInt32Constant(0);
                staticGcRootsExpressions.add(constant);
                return constant;
            }
            default: {
                InvocationExpr expr = new InvocationExpr();
                MethodReference method = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                expr.setMethod(method);
                expr.setType(InvocationType.SPECIAL);
                expr.getArguments().addAll(invocation.getArguments());
                return manager.generate(expr);
            }
        }
    }
}
