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
package org.teavm.backend.wasm.disasm;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmCompositeType;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.parser.WasmEntityIndexer;

/**
 * <p>Takes names directly from entities of Wasm model, unlike {@link NameAccumulatingSectionListener},
 * which takes names from name section of a binary module.</p>
 *
 * <p>Since entities may not have indices yet (for example, when function is being generated),
 * indices are assigned in order in which entities are first encountered.</p>
 */
public class ModelNameProvider implements NameProvider, WasmEntityIndexer {
    private final Indexes<WasmFunction> functions = new Indexes<>();
    private final Indexes<WasmCompositeType> types = new Indexes<>();
    private final Indexes<WasmGlobal> globals = new Indexes<>();

    @Override
    public int function(WasmFunction function) {
        return functions.index(function);
    }

    @Override
    public int type(WasmCompositeType type) {
        return types.index(type);
    }

    @Override
    public int global(WasmGlobal global) {
        return globals.index(global);
    }

    @Override
    public String function(int index) {
        var function = functions.get(index);
        return function != null ? function.getName() : null;
    }

    @Override
    public String local(int functionIndex, int index) {
        var function = functions.get(functionIndex);
        if (function == null) {
            return null;
        }
        var locals = function.getLocalVariables();
        return index >= 0 && index < locals.size() ? locals.get(index).getName() : null;
    }

    @Override
    public String type(int index) {
        var type = types.get(index);
        return type != null ? type.getName() : null;
    }

    @Override
    public String global(int index) {
        var global = globals.get(index);
        return global != null ? global.getName() : null;
    }

    @Override
    public String field(int typeIndex, int index) {
        var type = types.get(typeIndex);
        if (!(type instanceof WasmStructure)) {
            return null;
        }
        var fields = ((WasmStructure) type).getFields();
        return index >= 0 && index < fields.size() ? fields.get(index).getName() : null;
    }

    private static class Indexes<T> {
        private final Map<T, Integer> indexes = new IdentityHashMap<>();
        private final List<T> entities = new ArrayList<>();

        int index(T entity) {
            return indexes.computeIfAbsent(entity, e -> {
                entities.add(e);
                return entities.size() - 1;
            });
        }

        T get(int index) {
            return index >= 0 && index < entities.size() ? entities.get(index) : null;
        }
    }
}
