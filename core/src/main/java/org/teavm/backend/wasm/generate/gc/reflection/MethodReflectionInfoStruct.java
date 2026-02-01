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
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.MethodReflectionInfo;

public class MethodReflectionInfoStruct {
    private final DependencyInfo dependency;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;
    private int genericReturnTypeIndex = -1;
    private int genericParameterTypesIndex = -1;
    private int annotationsIndex = -1;
    private int typeParametersIndex = -1;

    MethodReflectionInfoStruct(WasmGCNameProvider names, WasmModule module, DependencyInfo dependency,
            WasmGCClassInfoProvider classInfoProvider) {
        this.dependency = dependency;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(MethodReflectionInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependency.getMethod(new MethodReference(MethodReflectionInfo.class, "genericReturnType",
                GenericTypeInfo.class)) != null) {
            genericReturnTypeIndex = fields.size();
            fields.add(new WasmField(WasmType.STRUCT, "genericReturnType"));
        }
        if (dependency.getMethod(new MethodReference(MethodReflectionInfo.class, "genericParameterTypeCount",
                int.class)) != null) {
            genericParameterTypesIndex = fields.size();
            fields.add(new WasmField(classInfoProvider.reflectionTypes().genericTypeArray(), "genericParameterType"));
        }
        if (dependency.getMethod(new MethodReference(MethodReflectionInfo.class, "annotationCount",
                int.class)) != null) {
            annotationsIndex = fields.size();
            var annotStruct = classInfoProvider.reflectionTypes().annotationInfo();
            fields.add(new WasmField(annotStruct.array(), "annotations"));
        }
        if (dependency.getMethod(new MethodReference(MethodReflectionInfo.class, "typeParameterCount",
                int.class)) != null) {
            typeParametersIndex = fields.size();
            var typeParamStruct = classInfoProvider.reflectionTypes().typeVariableInfo();
            fields.add(new WasmField(typeParamStruct.array(), "typeParameters"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public int genericReturnTypeIndex() {
        init();
        return genericReturnTypeIndex;
    }

    public int genericParameterTypesIndex() {
        init();
        return genericParameterTypesIndex;
    }

    public int annotationsIndex() {
        init();
        return annotationsIndex;
    }

    public int typeParametersIndex() {
        init();
        return typeParametersIndex;
    }

    private void init() {
        structure.init();
    }
}
