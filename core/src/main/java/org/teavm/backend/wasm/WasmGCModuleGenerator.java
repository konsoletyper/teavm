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
