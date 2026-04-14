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
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.runtime.reflect.DerivedClassInfo;

public class DerivedClassInfoStruct {
    private WasmGCNameProvider names;
    private WasmModule module;
    private ReflectionTypes reflectionTypes;

    private WasmStructure structure;
    private WasmArray array;
    private int classInfoIndex;
    private int arrayDegreeIndex;

    DerivedClassInfoStruct(WasmGCNameProvider names, WasmModule module, ReflectionTypes reflectionTypes) {
        this.names = names;
        this.module = module;
        this.reflectionTypes = reflectionTypes;

        var name = names.topLevel(names.suggestForClass(DerivedClassInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        classInfoIndex = fields.size();
        fields.add(new WasmField(reflectionTypes.classInfo().structure(), "classInfo"));

        arrayDegreeIndex = fields.size();
        fields.add(new WasmField(WasmStorageType.INT8, "arrayDegree"));
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.topLevel(names.suggestForArray(names.suggestForClass(DerivedClassInfo.class.getName())));
            array = new WasmArray(name, structure.getReference().asStorage());
            module.types.add(array);
        }
        return array;
    }

    public int classInfoIndex() {
        init();
        return classInfoIndex;
    }

    public int arrayDegreeIndex() {
        init();
        return arrayDegreeIndex;
    }

    private void init() {
        structure.init();
    }
}
