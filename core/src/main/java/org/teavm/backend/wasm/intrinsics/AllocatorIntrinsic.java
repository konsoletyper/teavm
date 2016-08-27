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

import java.util.stream.Collectors;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmMangling;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Allocator;

public class AllocatorIntrinsic implements WasmIntrinsic {
    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.getClassName().equals(Allocator.class.getName())
                && methodReference.getName().equals("fillZero");
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "fillZero":
            case "moveMemoryBlock": {
                MethodReference delegateMetod = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                WasmCall call = new WasmCall(WasmMangling.mangleMethod(delegateMetod));
                call.getArguments().addAll(invocation.getArguments().stream().map(manager::generate)
                        .collect(Collectors.toList()));
                return call;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
