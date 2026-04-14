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
package org.teavm.backend.wasm.generate.reflection;

import java.util.List;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.ParameterizedTypeInfo;

public class ParameterizedTypeInfoStruct {
    private final DependencyInfo dependencies;
    private final ReflectionTypes reflectionTypes;

    private WasmStructure structure;
    private int rawTypeIndex = -1;
    private int actualTypeArgumentsIndex = -1;
    private int ownerTypeIndex = -1;

    ParameterizedTypeInfoStruct(WasmGCNameProvider names, WasmModule module, DependencyInfo dependencies,
            ReflectionTypes reflectionTypes) {
        this.dependencies = dependencies;
        this.reflectionTypes = reflectionTypes;
        var name = names.topLevel(names.suggestForClass(ParameterizedTypeInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependencies.getMethod(new MethodReference(ParameterizedTypeInfo.class, "rawType",
                ClassInfo.class)) != null) {
            rawTypeIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.classInfo().structure(), "rawType"));
        }
        if (dependencies.getMethod(new MethodReference(ParameterizedTypeInfo.class, "actualTypeArgumentCount",
                int.class)) != null) {
            actualTypeArgumentsIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.genericTypeArray(), "actualTypeArguments"));
        }
        if (dependencies.getMethod(new MethodReference(ParameterizedTypeInfo.class, "ownerType",
                GenericTypeInfo.class)) != null) {
            ownerTypeIndex = fields.size();
            fields.add(new WasmField(WasmType.STRUCT, "ownerType"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public int rawTypeIndex() {
        init();
        return rawTypeIndex;
    }

    public int actualTypeArgumentsIndex() {
        init();
        return actualTypeArgumentsIndex;
    }

    public int ownerTypeIndex() {
        init();
        return ownerTypeIndex;
    }

    private void init() {
        structure.init();
    }
}
