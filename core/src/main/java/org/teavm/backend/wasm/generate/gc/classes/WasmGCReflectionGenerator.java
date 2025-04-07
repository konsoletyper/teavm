/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc.classes;

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ValueType;

class WasmGCReflectionGenerator implements WasmGCReflectionProvider {
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmGCNameProvider names;

    private WasmStructure reflectionFieldType;
    private WasmArray reflectionFieldArrayType;

    private WasmStructure reflectionMethodType;
    private WasmArray reflectionMethodArrayType;

    private WasmArray classArrayType;

    WasmGCReflectionGenerator(WasmModule module, WasmFunctionTypes functionTypes,
            WasmGCClassInfoProvider classInfoProvider, WasmGCNameProvider names) {
        this.module = module;
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        this.names = names;
    }

    @Override
    public WasmStructure getReflectionFieldType() {
        if (reflectionFieldType == null) {
            reflectionFieldType = new WasmStructure(names.topLevel("@teavm.Field"), fields -> {
                var stringClass = classInfoProvider.getClassInfo("java.lang.String");
                var classClass = classInfoProvider.getClassInfo("java.lang.Class");
                var objectClass = classInfoProvider.getClassInfo("java.lang.Object");

                var getterType = functionTypes.of(objectClass.getType(), objectClass.getType()).getReference();
                var setterType = functionTypes.of(null, objectClass.getType(), objectClass.getType()).getReference();

                fields.add(new WasmField(stringClass.getType().asStorage(),
                        names.structureField("@name")));
                fields.add(new WasmField(WasmType.INT32.asStorage(),
                        names.structureField("@flags")));
                fields.add(new WasmField(WasmType.INT32.asStorage(),
                        names.structureField("@accessLevel")));
                fields.add(new WasmField(classClass.getType().asStorage(),
                        names.structureField("@type")));
                fields.add(new WasmField(getterType.asStorage(), names.structureField("@reader")));
                fields.add(new WasmField(setterType.asStorage(), names.structureField("@writer")));
            });
            module.types.add(reflectionFieldType);
        }
        return reflectionFieldType;
    }

    @Override
    public WasmArray getReflectionFieldArrayType() {
        if (reflectionFieldArrayType == null) {
            reflectionFieldArrayType = new WasmArray("Array<@teavm.Field>", getReflectionFieldType().getReference()
                    .asStorage());
            module.types.add(reflectionFieldArrayType);
        }
        return reflectionFieldArrayType;
    }

    @Override
    public WasmStructure getReflectionMethodType() {
        if (reflectionMethodType == null) {
            reflectionMethodType = new WasmStructure(names.topLevel("@teavm.Method"), fields -> {
                var stringClass = classInfoProvider.getClassInfo("java.lang.String");
                var classClass = classInfoProvider.getClassInfo("java.lang.Class");
                var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
                var objectArrayClass = classInfoProvider.getClassInfo(ValueType.arrayOf(
                        ValueType.object("java.lang.Object")));

                var callerType = functionTypes.of(objectClass.getType(), objectClass.getType(),
                        objectArrayClass.getType()).getReference();

                fields.add(new WasmField(stringClass.getType().asStorage(),
                        names.structureField("@name")));
                fields.add(new WasmField(WasmType.INT32.asStorage(),
                        names.structureField("@flags")));
                fields.add(new WasmField(WasmType.INT32.asStorage(),
                        names.structureField("@accessLevel")));
                fields.add(new WasmField(classClass.getType().asStorage(),
                        names.structureField("@returnType")));
                fields.add(new WasmField(getClassArrayType().getReference().asStorage(),
                        names.structureField("@parameterTypes")));
                fields.add(new WasmField(callerType.asStorage(), names.structureField("@caller")));
            });
            module.types.add(reflectionMethodType);
        }
        return reflectionMethodType;
    }

    @Override
    public WasmArray getReflectionMethodArrayType() {
        if (reflectionMethodArrayType == null) {
            reflectionMethodArrayType = new WasmArray("Array<@teavm.Method>", getReflectionMethodType().getReference()
                    .asStorage());
            module.types.add(reflectionMethodArrayType);
        }
        return reflectionMethodArrayType;
    }

    @Override
    public WasmArray getClassArrayType() {
        if (classArrayType == null) {
            classArrayType = new WasmArray("Array<java.lang.Class>", () ->
                    classInfoProvider.getClassInfo("java.lang.Class").getType().asStorage());
            module.types.add(classArrayType);
        }
        return classArrayType;
    }
}
