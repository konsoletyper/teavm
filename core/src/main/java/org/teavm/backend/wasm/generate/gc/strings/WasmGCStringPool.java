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
package org.teavm.backend.wasm.generate.gc.strings;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.MethodReference;

public class WasmGCStringPool implements WasmGCStringProvider, WasmGCInitializerContributor {
    private WasmGCStandardClasses standardClasses;
    private WasmModule module;
    private WasmBinaryWriter binaryWriter = new WasmBinaryWriter();
    private Map<String, WasmGCStringConstant> stringMap = new LinkedHashMap<>();
    private BaseWasmFunctionRepository functionProvider;
    private WasmFunction nextCharArrayFunction;
    private WasmGCNameProvider names;

    public WasmGCStringPool(WasmGCStandardClasses standardClasses, WasmModule module,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names) {
        this.standardClasses = standardClasses;
        this.module = module;
        this.functionProvider = functionProvider;
        this.names = names;
    }

    @Override
    public void contributeToInitializerDefinitions(WasmFunction function) {
        var segment = new WasmMemorySegment();
        module.getSegments().add(segment);
        segment.setData(binaryWriter.getData());
        for (var str : stringMap.values()) {
            var newStruct = new WasmStructNewDefault(standardClasses.stringClass().getStructure());
            function.getBody().add(new WasmSetGlobal(str.global, newStruct));
        }
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        if (nextCharArrayFunction == null) {
            return;
        }
        var stringStruct = standardClasses.stringClass().getStructure();
        for (var str : stringMap.values()) {
            var value = new WasmCall(nextCharArrayFunction);
            function.getBody().add(new WasmStructSet(stringStruct, new WasmGetGlobal(str.global),
                    WasmGCClassInfoProvider.CUSTOM_FIELD_OFFSETS, value));
            function.getBody().add(new WasmStructSet(stringStruct, new WasmGetGlobal(str.global),
                    WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                    new WasmGetGlobal(standardClasses.stringClass().getPointer())));
        }
    }

    @Override
    public WasmGCStringConstant getStringConstant(String string) {
        return stringMap.computeIfAbsent(string, s -> {
            if (nextCharArrayFunction == null) {
                initNextCharArrayFunction();
            }
            binaryWriter.writeLEB(string.length());
            binaryWriter.writeBytes(string.getBytes(StandardCharsets.UTF_8));
            var brief = string.length() > 16 ? string.substring(0, 16) : string;
            var globalName = names.topLevel("teavm@string<" + stringMap.size() + ">"
                    + WasmGCNameProvider.sanitize(brief));
            var globalType = standardClasses.stringClass().getType();
            var global = new WasmGlobal(globalName, globalType, WasmExpression.defaultValueOfType(globalType));
            module.globals.add(global);
            return new WasmGCStringConstant(stringMap.size(), global);
        });
    }

    private void initNextCharArrayFunction() {
        nextCharArrayFunction = functionProvider.forStaticMethod(new MethodReference(WasmGCSupport.class,
                "nextCharArray", char[].class));
    }
}
