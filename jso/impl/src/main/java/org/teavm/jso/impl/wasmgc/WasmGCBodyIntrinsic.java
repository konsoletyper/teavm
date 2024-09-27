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
package org.teavm.jso.impl.wasmgc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.jso.impl.JSBodyEmitter;

class WasmGCBodyIntrinsic implements WasmGCIntrinsic {
    private JSBodyEmitter emitter;
    private boolean inlined;
    private WasmGCBodyGenerator bodyGenerator;
    private WasmGlobal global;
    private WasmGCJSFunctions jsFunctions;

    WasmGCBodyIntrinsic(JSBodyEmitter emitter, boolean inlined, WasmGCBodyGenerator bodyGenerator,
            WasmGCJSFunctions jsFunctions) {
        this.emitter = emitter;
        this.inlined = inlined;
        this.bodyGenerator = bodyGenerator;
        this.jsFunctions = jsFunctions;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        if (global == null) {
            global = bodyGenerator.addBody(context, emitter, inlined);
        }
        var call = new WasmCall(jsFunctions.getFunctionCaller(context, invocation.getArguments().size()));
        call.getArguments().add(new WasmGetGlobal(global));
        for (var arg : invocation.getArguments()) {
            call.getArguments().add(context.generate(arg));
        }
        return call;
    }
}
