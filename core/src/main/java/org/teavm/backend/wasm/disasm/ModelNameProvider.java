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
import java.util.HashMap;
import java.util.HashSet;
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
 *
 * <p>Since locals of a function may have equal names (for example, during transformation passes),
 * duplicate names get unique suffixes {@code _1}, {@code _2}, etc.</p>
 */
public class ModelNameProvider implements NameProvider, WasmEntityIndexer {
    private final Indexes<WasmFunction> functions = new Indexes<>();
    private final Indexes<WasmCompositeType> types = new Indexes<>();
    private final Indexes<WasmGlobal> globals = new Indexes<>();
    private final Map<WasmFunction, String[]> localNames = new IdentityHashMap<>();
    private boolean withLocalNames;

    public ModelNameProvider(boolean withLocalNames) {
        this.withLocalNames = withLocalNames;
    }

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
        if (!withLocalNames) {
            return null;
        }
        var function = functions.get(functionIndex);
        if (function == null) {
            return null;
        }
        var names = localNames.computeIfAbsent(function, ModelNameProvider::uniqueLocalNames);
        return index >= 0 && index < names.length ? names[index] : null;
    }

    private static String[] uniqueLocalNames(WasmFunction function) {
        var locals = function.getLocalVariables();
        var usedNames = new HashSet<String>();
        for (var local : locals) {
            if (local.getName() != null) {
                usedNames.add(local.getName());
            }
        }

        var result = new String[locals.size()];
        var encountered = new HashSet<String>();
        var suffixes = new HashMap<String, Integer>();
        for (var i = 0; i < locals.size(); ++i) {
            var name = locals.get(i).getName();
            if (name != null && !encountered.add(name)) {
                var suffix = suffixes.getOrDefault(name, 0);
                String candidate;
                do {
                    candidate = name + "_" + ++suffix;
                } while (!usedNames.add(candidate));
                suffixes.put(name, suffix);
                name = candidate;
            }
            result[i] = name;
        }
        return result;
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
