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
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
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

        var thisType = context.typeMapper().mapType(ValueType.object(method.getClassName()));
        var receiverType = context.isCompactMode()
                ? WasmType.Reference.ANY
                : thisType;
        var thisLocal = new WasmLocal(receiverType, "this");
        function.add(thisLocal);

        var jsClassGlobal = commonGen.getDefinedClass(jsoContext, method.getClassName());
        var wrapperFunction = commonGen.javaObjectToJSFunction(jsoContext);
        WasmExpression thisRef = new WasmGetLocal(thisLocal);
        if (context.isCompactMode()) {
            thisRef = new WasmCast(thisRef, (WasmType.Reference) thisType);
        }
        function.getBody().add(new WasmCall(wrapperFunction, thisRef, new WasmGetGlobal(jsClassGlobal)));
    }
}
