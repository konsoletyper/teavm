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
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.model.ValueType;

class WasmGCBodyIntrinsic implements WasmGCIntrinsic {
    private JSBodyEmitter emitter;
    private boolean inlined;
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGlobal global;
    private WasmGCJSFunctions jsFunctions;

    WasmGCBodyIntrinsic(JSBodyEmitter emitter, boolean inlined, WasmGCJsoCommonGenerator commonGen,
            WasmGCJSFunctions jsFunctions) {
        this.emitter = emitter;
        this.inlined = inlined;
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        var jsoContext = WasmGCJsoContext.wrap(context);
        if (global == null) {
            global = commonGen.addJSBody(jsoContext, emitter, inlined);
        }
        var caller = jsFunctions.getFunctionCaller(jsoContext, invocation.getArguments().size());
        builder.getGlobal(global);
        for (var arg : invocation.getArguments()) {
            context.generate(builder, arg);
        }
        builder.call(caller);
        if (invocation.getMethod().getReturnType() == ValueType.VOID) {
            builder.drop();
        }
    }
}
