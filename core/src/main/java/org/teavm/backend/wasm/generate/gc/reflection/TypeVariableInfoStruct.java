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
package org.teavm.backend.wasm.generate.gc.reflection;

import java.util.List;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.TypeVariableInfo;

public class TypeVariableInfoStruct {
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private final DependencyInfo dependencies;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;
    private WasmArray array;

    private int nameIndex = -1;
    private int boundsIndex = -1;

    TypeVariableInfoStruct(WasmGCNameProvider names, WasmModule module, DependencyInfo dependencies,
            WasmGCClassInfoProvider classInfoProvider) {
        this.names = names;
        this.module = module;
        this.dependencies = dependencies;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(TypeVariableInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependencies.getMethod(new MethodReference(TypeVariableInfo.class, "name", StringInfo.class)) != null) {
            nameIndex = fields.size();
            var stringClass = classInfoProvider.getClassInfo("java.lang.String");
            fields.add(new WasmField(stringClass.getType(), "name"));
        }
        if (dependencies.getMethod(new MethodReference(TypeVariableInfo.class, "boundCount", int.class)) != null) {
            boundsIndex = fields.size();
            fields.add(new WasmField(classInfoProvider.reflectionTypes().genericTypeArray(), "bounds"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.topLevel(names.suggestForArray(names.suggestForClass(TypeVariableInfo.class.getName())));
            array = new WasmArray(name, structure.getReference().asStorage());
            module.types.add(array);
        }
        return array;
    }

    public int nameIndex() {
        init();
        return nameIndex;
    }

    public int boundsIndex() {
        init();
        return boundsIndex;
    }

    private void init() {
        structure.init();
    }
}
