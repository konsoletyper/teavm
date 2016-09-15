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
package org.teavm.backend.wasm.optimization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;

public class UnusedFunctionElimination {
    private WasmModule module;
    private Set<WasmFunction> usedFunctions = new HashSet<>();

    public UnusedFunctionElimination(WasmModule module) {
        this.module = module;
    }

    public void apply() {
        List<WasmFunction> exported = module.getFunctions().values().stream()
                .filter(function -> function.getExportName() != null)
                .collect(Collectors.toList());
        for (WasmFunction function : exported) {
            use(function);
        }
        for (WasmFunction function : module.getFunctionTable()) {
            use(function);
        }
        if (module.getStartFunction() != null) {
            use(module.getStartFunction());
        }

        for (WasmFunction function : module.getFunctions().values().toArray(new WasmFunction[0])) {
            if (!usedFunctions.contains(function)) {
                module.remove(function);
            }
        }
    }

    private void use(WasmFunction function) {
        if (!usedFunctions.add(function)) {
            return;
        }
        for (WasmExpression part : function.getBody()) {
            part.acceptVisitor(visitor);
        }
    }

    private WasmExpressionVisitor visitor = new WasmDefaultExpressionVisitor() {
        @Override
        public void visit(WasmCall expression) {
            super.visit(expression);
            WasmFunction function = module.getFunctions().get(expression.getFunctionName());
            if (function != null) {
                use(function);
            }
        }
    };
}
