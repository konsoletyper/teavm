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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;

class TemporaryVariablePool {
    private WasmFunction function;
    private List<Deque<WasmLocal>> temporaryVariablesByType = new ArrayList<>();

    TemporaryVariablePool(WasmFunction function) {
        this.function = function;
        int typeCount = WasmType.values().length;
        for (int i = 0; i < typeCount; ++i) {
            temporaryVariablesByType.add(new ArrayDeque<>());
        }
    }

    WasmLocal acquire(WasmType type) {
        var stack = temporaryVariablesByType.get(type.ordinal());
        WasmLocal variable = stack.pollFirst();
        if (variable == null) {
            variable = new WasmLocal(type);
            function.add(variable);
        }
        return variable;
    }

    void release(WasmLocal variable) {
        var stack = temporaryVariablesByType.get(variable.getType().ordinal());
        stack.push(variable);
    }
}
