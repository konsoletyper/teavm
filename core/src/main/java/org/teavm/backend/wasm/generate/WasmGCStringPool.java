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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.render.WasmBinaryWriter;

public class WasmGCStringPool {
    private WasmGCClassGenerator classGenerator;
    private WasmModule module;
    private WasmBinaryWriter binaryWriter = new WasmBinaryWriter();
    private Map<String, WasmGlobal> stringMap = new HashMap<>();
    private WasmType stringType;

    WasmGCStringPool(WasmGCClassGenerator classGenerator, WasmModule module) {
        this.classGenerator = classGenerator;
        this.module = module;
    }

    public WasmGlobal getStringPointer(String string) {
        return stringMap.computeIfAbsent(string, s -> {
            binaryWriter.writeInt32(string.length());
            binaryWriter.writeBytes(string.getBytes(StandardCharsets.UTF_8));
            var globalName = "java_string_" + stringMap.size();
            var globalType = getStringType();
            var global = new WasmGlobal(globalName, globalType, WasmExpression.defaultValueOfType(globalType));
            module.globals.add(global);
            return global;
        });
    }

    private WasmType getStringType() {
        if (stringType == null) {
            stringType = classGenerator.getClassInfo("java.lang.Class").getType();
        }
        return stringType;
    }
}
