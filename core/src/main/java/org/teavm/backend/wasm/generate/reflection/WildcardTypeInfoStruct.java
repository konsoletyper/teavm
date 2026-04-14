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
import org.teavm.runtime.reflect.WildcardTypeInfo;

public class WildcardTypeInfoStruct {
    private WasmStructure structure;

    private int kindIndex;
    private int boundIndex;

    WildcardTypeInfoStruct(WasmGCNameProvider names, WasmModule module) {
        var name = names.topLevel(names.suggestForClass(WildcardTypeInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        kindIndex = fields.size();
        fields.add(new WasmField(WasmType.INT32, "kind"));

        boundIndex = fields.size();
        fields.add(new WasmField(WasmType.STRUCT, "bound"));
    }

    public WasmStructure structure() {
        return structure;
    }

    public int kindIndex() {
        init();
        return kindIndex;
    }

    public int boundIndex() {
        init();
        return boundIndex;
    }

    private void init() {
        structure.init();
    }
}
