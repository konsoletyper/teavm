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
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.DerivedClassInfo;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.FieldReflectionInfo;

public class FieldInfoStruct {
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final DependencyInfo dependencies;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;

    private int nameIndex = -1;
    private int modifiersIndex = -1;
    private int typeIndex = -1;
    private int readerIndex = -1;
    private int writerIndex = -1;
    private int reflectionIndex = -1;

    private WasmFunctionType readerType;
    private WasmFunctionType writerType;

    private WasmArray array;

    FieldInfoStruct(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            DependencyInfo dependencies, WasmGCClassInfoProvider classInfoProvider) {
        this.names = names;
        this.module = module;
        this.functionTypes = functionTypes;
        this.dependencies = dependencies;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(FieldInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "name", StringInfo.class)) != null) {
            nameIndex = fields.size();
            var strType = classInfoProvider.getClassInfo("java.lang.String").getType();
            fields.add(new WasmField(strType, "name"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "modifiers", int.class)) != null) {
            modifiersIndex = fields.size();
            fields.add(new WasmField(WasmType.INT32, "modifiers"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "type", DerivedClassInfo.class)) != null) {
            typeIndex = fields.size();
            var infoType = classInfoProvider.reflectionTypes().derivedClassInfo().structure().getReference();
            fields.add(new WasmField(infoType, "type"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "read", Object.class, Object.class)) != null) {
            var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
            readerType = functionTypes.of(objType, objType);
            readerIndex = fields.size();
            fields.add(new WasmField(readerType, "reader"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "write", Object.class, Object.class,
                void.class)) != null) {
            var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
            writerType = functionTypes.of(null, objType, objType);
            writerIndex = fields.size();
            fields.add(new WasmField(writerType, "writer"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "reflection",
                FieldReflectionInfo.class)) != null) {
            reflectionIndex = fields.size();
            var reflectionStruct = classInfoProvider.reflectionTypes().fieldReflectionInfo().structure();
            fields.add(new WasmField(reflectionStruct, "reflection"));
        }
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.topLevel(names.suggestForArray(names.suggestForClass(FieldInfo.class.getName())));
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

    public int typeIndex() {
        init();
        return typeIndex;
    }

    public int readerIndex() {
        init();
        return readerIndex;
    }

    public int writerIndex() {
        init();
        return writerIndex;
    }

    public int reflectionIndex() {
        init();
        return reflectionIndex;
    }

    public WasmFunctionType readerType() {
        init();
        return readerType;
    }

    public WasmFunctionType writerType() {
        init();
        return writerType;
    }

    private void init() {
        structure.init();
    }
}
