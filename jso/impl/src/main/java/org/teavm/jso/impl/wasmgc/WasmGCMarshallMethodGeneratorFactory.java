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
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorFactory;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorFactoryContext;
import org.teavm.jso.impl.JSMethods;
import org.teavm.model.MethodReference;

class WasmGCMarshallMethodGeneratorFactory implements WasmGCCustomGeneratorFactory {
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCMarshallMethodGeneratorFactory(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public WasmGCCustomGenerator createGenerator(MethodReference methodRef,
            WasmGCCustomGeneratorFactoryContext context) {
        if (!methodRef.getName().equals(JSMethods.MARSHALL_TO_JS.getName())) {
            return null;
        }
        var cls = context.classes().get(methodRef.getClassName());
        return cls != null && cls.getInterfaces().contains(JSMethods.JS_MARSHALLABLE)
                ? new WasmGCMarshallMethodGenerator(commonGen)
                : null;
    }
}
