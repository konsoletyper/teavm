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
package org.teavm.jso.impl.wasmgc;

import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCMarshallMethodGenerator implements WasmGCCustomGenerator {
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCMarshallMethodGenerator(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var jsoContext = WasmGCJsoContext.wrap(context);

        var thisLocal = new WasmLocal(context.typeMapper().mapType(ValueType.object(method.getClassName())), "this");
        function.add(thisLocal);

        var jsClassGlobal = commonGen.getDefinedClass(jsoContext, method.getClassName());
        var wrapperFunction = commonGen.javaObjectToJSFunction(jsoContext);
        function.getBody().add(new WasmCall(wrapperFunction, new WasmGetLocal(thisLocal),
                new WasmGetGlobal(jsClassGlobal)));
    }
}
