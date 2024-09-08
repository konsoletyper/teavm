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
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
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
    private WasmFunction initNextStringFunction;
    private WasmGCNameProvider names;
    private WasmFunctionTypes functionTypes;

    public WasmGCStringPool(WasmGCStandardClasses standardClasses, WasmModule module,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names,
            WasmFunctionTypes functionTypes) {
        this.standardClasses = standardClasses;
        this.module = module;
        this.functionProvider = functionProvider;
        this.names = names;
        this.functionTypes = functionTypes;
    }

    @Override
    public void contributeToInitializerDefinitions(WasmFunction function) {
        var segment = new WasmMemorySegment();
        module.getSegments().add(segment);
        segment.setData(binaryWriter.getData());
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        if (initNextStringFunction == null) {
            return;
        }
        var stringStruct = standardClasses.stringClass().getStructure();
        for (var str : stringMap.values()) {
            function.getBody().add(new WasmCall(initNextStringFunction, new WasmGetGlobal(str.global)));
        }
    }

    @Override
    public WasmGCStringConstant getStringConstant(String string) {
        return stringMap.computeIfAbsent(string, s -> {
            if (initNextStringFunction == null) {
                createInitNextStringFunction();
            }
            binaryWriter.writeLEB(string.length());
            binaryWriter.writeBytes(string.getBytes(StandardCharsets.UTF_8));
            var brief = string.length() > 16 ? string.substring(0, 16) : string;
            var globalName = names.topLevel("teavm@string<" + stringMap.size() + ">"
                    + WasmGCNameProvider.sanitize(brief));
            var globalType = standardClasses.stringClass().getStructure().getNonNullReference();
            var global = new WasmGlobal(globalName, globalType,
                    new WasmStructNewDefault(standardClasses.stringClass().getStructure()));
            global.setImmutable(true);
            module.globals.add(global);
            return new WasmGCStringConstant(stringMap.size(), global);
        });
    }

    private void createInitNextStringFunction() {
        var stringTypeInfo = standardClasses.stringClass();
        var nextCharArrayFunction = functionProvider.forStaticMethod(new MethodReference(WasmGCSupport.class,
                "nextCharArray", char[].class));
        var function = new WasmFunction(functionTypes.of(null, stringTypeInfo.getType()));
        function.setName(names.topLevel("teavm@initNextString"));

        var stringLocal = new WasmLocal(stringTypeInfo.getType());
        function.add(stringLocal);

        var value = new WasmCall(nextCharArrayFunction);
        function.getBody().add(new WasmStructSet(stringTypeInfo.getStructure(),
                new WasmGetLocal(stringLocal), WasmGCClassInfoProvider.CUSTOM_FIELD_OFFSETS, value));
        function.getBody().add(new WasmStructSet(stringTypeInfo.getStructure(), new WasmGetLocal(stringLocal),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                new WasmGetGlobal(stringTypeInfo.getPointer())));
        module.functions.add(function);
        initNextStringFunction = function;
    }
}
