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
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.FieldReflectionInfo;
import org.teavm.runtime.reflect.GenericTypeInfo;

public class FieldReflectionInfoStruct {
    private final DependencyInfo dependency;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;
    private int annotationsIndex = -1;
    private int genericTypeIndex = -1;

    public FieldReflectionInfoStruct(WasmGCNameProvider names, WasmModule module, DependencyInfo dependency,
            WasmGCClassInfoProvider classInfoProvider) {
        this.dependency = dependency;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(FieldReflectionInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependency.getMethod(new MethodReference(FieldReflectionInfo.class, "annotationCount",
                int.class)) != null) {
            annotationsIndex = fields.size();
            var annotStruct = classInfoProvider.reflectionTypes().annotationInfo();
            fields.add(new WasmField(annotStruct.array(), "annotations"));
        }
        if (dependency.getMethod(new MethodReference(FieldReflectionInfo.class, "genericType",
                GenericTypeInfo.class)) != null) {
            genericTypeIndex = fields.size();
            fields.add(new WasmField(WasmType.STRUCT, "genericType"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public int annotationsIndex() {
        init();
        return annotationsIndex;
    }

    public int genericTypeIndex() {
        init();
        return genericTypeIndex;
    }

    private void init() {
        structure.init();
    }
}
