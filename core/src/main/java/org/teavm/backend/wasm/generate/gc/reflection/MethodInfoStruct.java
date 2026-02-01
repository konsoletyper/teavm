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
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.DerivedClassInfo;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.runtime.reflect.MethodReflectionInfo;

public class MethodInfoStruct {
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final DependencyInfo dependencies;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;

    private int nameIndex = -1;
    private int modifiersIndex = -1;
    private int returnTypeIndex = -1;
    private int parameterTypesIndex = -1;
    private int callerIndex = -1;
    private int reflectionIndex = -1;

    private WasmFunctionType callerType;

    private WasmArray array;

    MethodInfoStruct(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            DependencyInfo dependencies, WasmGCClassInfoProvider classInfoProvider) {
        this.names = names;
        this.module = module;
        this.functionTypes = functionTypes;
        this.dependencies = dependencies;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(MethodInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "name", StringInfo.class)) != null) {
            nameIndex = fields.size();
            var strType = classInfoProvider.getClassInfo("java.lang.String").getType();
            fields.add(new WasmField(strType, "name"));
        }
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "modifiers", int.class)) != null) {
            modifiersIndex = fields.size();
            fields.add(new WasmField(WasmType.INT32, "modifiers"));
        }
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "returnType",
                DerivedClassInfo.class)) != null) {
            returnTypeIndex = fields.size();
            var infoType = classInfoProvider.reflectionTypes().derivedClassInfo().structure().getReference();
            fields.add(new WasmField(infoType, "returnType"));
        }
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "parameterCount", int.class)) != null) {
            parameterTypesIndex = fields.size();
            var infoType = classInfoProvider.reflectionTypes().derivedClassInfo().array().getReference();
            fields.add(new WasmField(infoType, "parameterTypes"));
        }
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "call", Object.class, Object[].class,
                Object.class)) != null) {
            var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
            var objArrayType = classInfoProvider.getClassInfo(ValueType.parse(Object[].class)).getType();
            callerType = functionTypes.of(objType, objType, objArrayType);
            callerIndex = fields.size();
            fields.add(new WasmField(callerType, "caller"));
        }
        if (dependencies.getMethod(new MethodReference(MethodInfo.class, "reflection",
                MethodReflectionInfo.class)) != null) {
            reflectionIndex = fields.size();
            var reflectionStruct = classInfoProvider.reflectionTypes().methodReflectionInfo().structure();
            fields.add(new WasmField(reflectionStruct, "reflection"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.topLevel(names.suggestForArray(names.suggestForClass(MethodInfo.class.getName())));
            array = new WasmArray(name, structure.getReference().asStorage());
            module.types.add(array);
        }
        return array;
    }

    public int nameIndex() {
        init();
        return nameIndex;
    }

    public int modifiersIndex() {
        init();
        return modifiersIndex;
    }

    public int returnTypeIndex() {
        init();
        return returnTypeIndex;
    }

    public int parameterTypesIndex() {
        init();
        return parameterTypesIndex;
    }

    public int callerIndex() {
        init();
        return callerIndex;
    }

    public int reflectionIndex() {
        init();
        return reflectionIndex;
    }

    public WasmFunctionType callerType() {
        init();
        return callerType;
    }

    private void init() {
        structure.init();
    }
}
