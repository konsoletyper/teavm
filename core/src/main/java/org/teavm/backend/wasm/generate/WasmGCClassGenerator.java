/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.WasmFunctionRepository;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.Characteristics;

public class WasmGCClassGenerator {
    private ClassReaderSource processedClassSource;
    private ClassReaderSource classSource;
    private Characteristics characteristics;
    private Map<ValueType, WasmGCClassInfo> classInfoMap = new HashMap<>();
    private WasmGCClassInfo classClassInfo;
    public final WasmFunctionRepository functions;
    public final WasmModule module;
    public final WasmGCStringPool strings;
    private final NameProvider names;

    public WasmGCClassGenerator(WasmFunctionRepository functions, WasmModule module, NameProvider names) {
        this.functions = functions;
        this.module = module;
        this.names = names;
        strings = new WasmGCStringPool(this, module);
    }

    public WasmGCClassInfo getClassInfo(ValueType type) {
        var classInfo = classInfoMap.get(type);
        if (classInfo == null) {
            classInfo = new WasmGCClassInfo();
            classInfoMap.put(type, classInfo);
            var structure = new WasmStructure(null, () -> {
                var fields = new ArrayList<WasmStorageType>();
                fields.add(getClassClassInfo().getType().asStorage());
                fillFields(fields, type);
                return fields;
            });
            classInfo.structure = structure;
            var pointerName = names.forClassInstance(type);
            classInfo.pointer = new WasmGlobal(pointerName, structure.getReference(),
                    WasmExpression.defaultValueOfType(structure.getReference()));
        }
        return classInfo;
    }

    private void fillFields(List<WasmStorageType> fields, ValueType type) {
        fields.add(getClassClassInfo().getType().asStorage());
        if (type instanceof ValueType.Object) {

        } else if (type instanceof ValueType.Array) {

        }
    }

    private void fillClassFields(List<WasmStorageType> fields, String className) {
        var classReader = processedClassSource.get(className);
        if (classReader.getParent() != null) {
            fillClassFields(className);
        }
    }

    public WasmGCClassInfo getClassInfo(String name) {
        return null;
    }

    private WasmGCClassInfo getClassClassInfo() {
        if (classClassInfo == null) {
            classClassInfo = getClassInfo("java.lang.Class");
        }
        return classClassInfo;
    }
}
