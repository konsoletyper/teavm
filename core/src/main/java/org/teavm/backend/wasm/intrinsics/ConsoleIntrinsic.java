/*
 *  Copyright 2019 Alexey Andreev.
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
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Console;

public class ConsoleIntrinsic implements WasmIntrinsic {
    private static final MethodReference PRINT_STRING = new MethodReference(WasmRuntime.class,
            "printString", String.class, void.class);
    private static final MethodReference PRINT_INT = new MethodReference(WasmRuntime.class,
            "printInt", int.class, void.class);

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Console.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "printString":
            case "printInt":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "printString": {
                String name = manager.getNames().forMethod(PRINT_STRING);
                WasmCall call = new WasmCall(name, true);
                call.getArguments().add(manager.generate(invocation.getArguments().get(0)));
                return call;
            }
            case "printInt": {
                String name = manager.getNames().forMethod(PRINT_INT);
                WasmCall call = new WasmCall(name, true);
                call.getArguments().add(manager.generate(invocation.getArguments().get(0)));
                return call;
            }
            default:
                return new WasmUnreachable();
        }
    }
}
