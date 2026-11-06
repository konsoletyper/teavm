/*
 *  Copyright 2026 Alexey Andreev.
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

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.instructions.InvokeInstruction;

public class SystemIntrinsic implements WasmGCInlineIntrinsic {
    private WasmFunctionTypes functionTypes;
    private WasmModule module;
    private WasmFunction currentTimeFunction;
    private WasmFunction killFunction;

    public SystemIntrinsic(WasmFunctionTypes functionTypes, WasmModule module) {
        this.functionTypes = functionTypes;
        this.module = module;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "currentTimeMillis":
                if (currentTimeFunction == null) {
                    currentTimeFunction = new WasmFunction(functionTypes.of(WasmType.FLOAT64));
                    currentTimeFunction.setName("teavm@currentTimeMillis");
                    currentTimeFunction.setImportName("currentTimeMillis");
                    currentTimeFunction.setImportModule("teavmDate");
                    module.functions.add(currentTimeFunction);
                }
                builder.call(currentTimeFunction).convert(WasmNumType.FLOAT64, WasmNumType.INT64, true);
                break;
            case "exit":
                if (killFunction == null) {
                    killFunction = new WasmFunction(functionTypes.of(null, WasmType.INT32));
                    killFunction.setName("teavm@kill");
                    killFunction.setImportName("kill");
                    killFunction.setImportModule("teavm");
                    module.functions.add(killFunction);
                }
                context.emitLocal(builder, invocation.getArguments().get(0))
                        .call(killFunction)
                        .unreachable();
                break;
        }
    }
}
