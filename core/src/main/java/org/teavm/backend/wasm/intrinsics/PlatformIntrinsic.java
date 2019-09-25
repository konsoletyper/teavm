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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class PlatformIntrinsic implements WasmIntrinsic {
    private static final String PLATFORM = "org.teavm.platform.Platform";

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.getClassName().equals(PLATFORM)
                && isApplicableToMethod(methodReference.getDescriptor());
    }

    private boolean isApplicableToMethod(MethodDescriptor methodDescriptor) {
        switch (methodDescriptor.getName()) {
            case "getPlatformObject":
            case "asJavaClass":
            case "createQueue":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getPlatformObject":
            case "asJavaClass":
                return manager.generate(invocation.getArguments().get(0));
            case "createQueue":
                return new WasmInt32Constant(0);
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
