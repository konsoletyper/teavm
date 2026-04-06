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
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.runtime.reflect.ClassReflectionInfo;

public class ClassReflectionInfoStruct {
    private final DependencyInfo dependencies;
    private final ClassMetadataRequirements metadataRequirements;
    private final ReflectionTypes reflectionTypes;

    private WasmStructure structure;

    private int annotationsIndex = -1;
    private int fieldsIndex = -1;
    private int methodsIndex = -1;
    private int typeParametersIndex = -1;
    private int innerClassesIndex = -1;

    ClassReflectionInfoStruct(WasmGCNameProvider names, WasmModule module, DependencyInfo dependencies,
            ClassMetadataRequirements metadataRequirements, ReflectionTypes reflectionTypes) {
        this.dependencies = dependencies;
        this.metadataRequirements = metadataRequirements;
        this.reflectionTypes = reflectionTypes;
        var structName = names.topLevel(names.suggestForClass(ClassReflectionInfo.class.getName()));
        structure = new WasmStructure(structName, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (metadataRequirements.hasGetAnnotations()) {
            annotationsIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.annotationInfo().array(), "annotations"));
        }
        if (metadataRequirements.hasGetFields()) {
            fieldsIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.fieldInfo().array(), "fields"));
        }
        if (metadataRequirements.hasGetMethods()) {
            methodsIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.methodInfo().array(), "methods"));
        }
        if (dependencies.getMethod(new MethodReference(ClassReflectionInfo.class, "typeParameterCount",
                int.class)) != null) {
            typeParametersIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.typeVariableInfo().array(), "typeParameters"));
        }
        if (dependencies.getMethod(new MethodReference(ClassReflectionInfo.class, "innerClassCount",
                int.class)) != null) {
            innerClassesIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.classInfo().array(), "innerClasses"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public int annotationsIndex() {
        init();
        return annotationsIndex;
    }

    public int fieldsIndex() {
        init();
        return fieldsIndex;
    }

    public int methodsIndex() {
        init();
        return methodsIndex;
    }

    public int typeParametersIndex() {
        init();
        return typeParametersIndex;
    }

    public int innerClassesIndex() {
        init();
        return innerClassesIndex;
    }

    private void init() {
        structure.init();
    }
}
