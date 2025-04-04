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
package org.teavm.backend.wasm.generators.gc;

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.MethodReference;

public class ArrayGenerator implements WasmGCCustomGenerator {
    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
        var classLocal = new WasmLocal(clsStruct.getReference());
        var sizeLocal = new WasmLocal(WasmType.INT32);
        function.add(classLocal);
        function.add(sizeLocal);
        var constructorRef = new WasmStructGet(clsStruct, new WasmGetLocal(classLocal),
                context.classInfoProvider().getNewArrayFunctionOffset());
        var functionType = context.functionTypes().of(
                context.classInfoProvider().getClassInfo("java.lang.Object").getType(),
                context.classInfoProvider().getClassInfo("java.lang.Class").getType(),
                WasmType.INT32
        );
        var result = new WasmCallReference(constructorRef, functionType);
        result.getArguments().add(new WasmGetLocal(classLocal));
        result.getArguments().add(new WasmGetLocal(sizeLocal));
        function.getBody().add(result);
    }
}
