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
package org.teavm.backend.wasm.transformation;

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmMemoryAccess;
import org.teavm.backend.wasm.model.expression.WasmReplacingExpressionVisitor;

public class MemoryAccessTraceTransformation {
    private WasmModule module;

    public MemoryAccessTraceTransformation(WasmModule module) {
        this.module = module;
    }

    public void apply() {
        WasmFunction traceFunction = new WasmFunction("traceMemoryAccess");
        traceFunction.setImportModule("debug");
        traceFunction.setImportName("traceMemoryAccess");
        traceFunction.getParameters().add(WasmType.INT32);
        traceFunction.getParameters().add(WasmType.INT32);
        traceFunction.setResult(WasmType.INT32);
        module.add(traceFunction);

        int[] positionHolder = { 0 };
        WasmReplacingExpressionVisitor visitor = new WasmReplacingExpressionVisitor(expression -> {
            if (expression instanceof WasmMemoryAccess) {
                WasmMemoryAccess memoryAccess = (WasmMemoryAccess) expression;
                WasmCall call = new WasmCall(traceFunction.getName());
                call.setImported(true);
                call.getArguments().add(new WasmInt32Constant(positionHolder[0]++));
                call.getArguments().add(memoryAccess.getIndex());
                memoryAccess.setIndex(call);
            }
            return expression;
        });
        for (WasmFunction function : module.getFunctions().values()) {
            visitor.replace(function);
        }
    }
}
