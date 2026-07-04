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

import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;

class WasmGCJSBodyIntrinsic implements WasmGCInlineIntrinsic {
    private JSBodyEmitter emitter;
    private boolean inlined;
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGlobal global;
    private WasmGCJSFunctions jsFunctions;

    WasmGCJSBodyIntrinsic(JSBodyEmitter emitter, boolean inlined, WasmGCJsoCommonGenerator commonGen,
            WasmGCJSFunctions jsFunctions) {
        this.emitter = emitter;
        this.inlined = inlined;
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (global == null) {
            global = commonGen.addJSBody(emitter, inlined);
        }
        var argCount = invocation.getArguments().size();
        if (invocation.getInstance() != null) {
            argCount++;
        }
        var caller = jsFunctions.getFunctionCaller(argCount);
        builder.getGlobal(global);
        if (invocation.getInstance() != null) {
            builder.getLocal(context.mapToLocal(invocation.getInstance()));
        }
        for (var arg : invocation.getArguments()) {
            builder.getLocal(context.mapToLocal(arg));
        }
        builder.call(caller);
        if (invocation.getMethod().getReturnType() == ValueType.VOID) {
            builder.drop();
        }
    }
}
