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
package org.teavm.backend.wasm;

import org.teavm.backend.wasm.generate.gc.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCModuleGenerator {
    private WasmGCDeclarationsGenerator declarationsGenerator;

    public WasmGCModuleGenerator(WasmGCDeclarationsGenerator declarationsGenerator) {
        this.declarationsGenerator = declarationsGenerator;
    }

    public void generate() {
        declarationsGenerator.generate();
        createInitializer();
    }


    public WasmFunction generateMainFunction(String entryPoint) {
        return declarationsGenerator.functions().forStaticMethod(new MethodReference(entryPoint,
                "main", ValueType.parse(String[].class), ValueType.VOID));
    }

    public WasmFunction generateCreateStringBuilderFunction() {
        return declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "createStringBuilder", StringBuilder.class));
    }

    public WasmFunction generateCreateStringArrayFunction() {
        return declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "createStringArray", int.class, String[].class));
    }

    public WasmFunction generateAppendCharFunction() {
        return declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                StringBuilder.class, "append", char.class, StringBuilder.class));
    }

    public WasmFunction generateBuildStringFunction() {
        return declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                StringBuilder.class, "toString", String.class));
    }

    public WasmFunction generateSetToStringArrayFunction() {
        return declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "setToStringArray", String[].class, int.class, String.class, void.class));
    }

    public WasmFunction generateStringLengthFunction() {
        return declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                String.class, "length", int.class));
    }

    public WasmFunction generateCharAtFunction() {
        return declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                String.class, "charAt", int.class, char.class));
    }

    public WasmFunction generateReportGarbageCollectedStringFunction() {
        var entryType = ValueType.object(StringInternPool.class.getName() + "$Entry");
        return declarationsGenerator.functions().forStaticMethod(new MethodReference(
                StringInternPool.class.getName(),
                "remove",
                entryType,
                ValueType.VOID
        ));
    }

    private void createInitializer() {
        var functionType = declarationsGenerator.functionTypes.of(null);
        var initializer = new WasmFunction(functionType);
        initializer.setName("teavm@initializer");
        declarationsGenerator.module.functions.add(initializer);
        declarationsGenerator.module.setStartFunction(initializer);
        declarationsGenerator.contributeToInitializer(initializer);
    }
}
