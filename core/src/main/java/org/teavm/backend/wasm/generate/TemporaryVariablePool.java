/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;

public class TemporaryVariablePool {
    private WasmFunction function;
    private Map<WasmType, Deque<WasmLocal>> temporaryVariablesByType = new HashMap<>();

    public TemporaryVariablePool(WasmFunction function) {
        this.function = function;
    }

    public WasmLocal acquire(WasmType type) {
        var stack = temporaryVariablesByType.computeIfAbsent(type, k -> new ArrayDeque<>());
        WasmLocal variable = stack.pollFirst();
        if (variable == null) {
            variable = new WasmLocal(type);
            function.add(variable);
        }
        return variable;
    }

    public void release(WasmLocal variable) {
        var stack = temporaryVariablesByType.get(variable.getType());
        stack.push(variable);
    }
}
