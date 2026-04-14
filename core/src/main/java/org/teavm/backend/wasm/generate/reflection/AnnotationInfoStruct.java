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
import org.teavm.runtime.reflect.AnnotationInfo;

public class AnnotationInfoStruct {
    private final WasmFunctionTypes functionTypes;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;
    private WasmArray array;
    private int dataIndex;
    private int constructorIndex;

    private WasmFunctionType constructorType;

    AnnotationInfoStruct(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            WasmGCClassInfoProvider classInfoProvider) {
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;

        var structName = names.topLevel(names.suggestForClass(AnnotationInfo.class.getName()));
        structure = new WasmStructure(structName, this::initFields);
        module.types.add(structure);

        var arrayName = names.topLevel(names.suggestForArray(names.suggestForClass(AnnotationInfo.class.getName())));
        array = new WasmArray(arrayName, structure.getReference().asStorage());
        module.types.add(array);
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        return array;
    }

    public int dataIndex() {
        init();
        return dataIndex;
    }

    public int constructorIndex() {
        init();
        return constructorIndex;
    }

    public WasmFunctionType constructorType() {
        init();
        return constructorType;
    }

    private void init() {
        structure.init();
    }

    private void initFields(List<WasmField> fields) {
        dataIndex = fields.size();
        fields.add(new WasmField(WasmType.STRUCT, "data"));

        constructorType = functionTypes.of(classInfoProvider.getClassInfo("java.lang.Object").getType(),
                WasmType.STRUCT);
        constructorIndex = fields.size();
        fields.add(new WasmField(constructorType, "constructor"));
    }
}
