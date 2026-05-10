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

import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.intrinsics.WasmGCBodyIntrinsic;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCMarshallMethodGenerator implements WasmGCBodyIntrinsic {
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGCTypeMapper typeMapper;

    WasmGCMarshallMethodGenerator(WasmGCJsoCommonGenerator commonGen, WasmGCTypeMapper typeMapper) {
        this.commonGen = commonGen;
        this.typeMapper = typeMapper;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        var thisType = typeMapper.mapType(ValueType.object(method.getClassName()));
        var thisLocal = new WasmLocal(thisType, "this");
        function.add(thisLocal);

        var jsClassGlobal = commonGen.getDefinedClass(method.getClassName());
        var wrapperFunction = commonGen.javaObjectToJSFunction();
        var body = function.getBody().builder();
        body.getLocal(thisLocal);
        body.getGlobal(jsClassGlobal);
        body.call(wrapperFunction);
    }
}
