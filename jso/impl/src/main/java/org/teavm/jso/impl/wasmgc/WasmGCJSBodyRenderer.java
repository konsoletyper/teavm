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

import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicFactory;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicFactoryContext;
import org.teavm.jso.impl.JSBodyRepository;
import org.teavm.model.MethodReference;

class WasmGCJSBodyRenderer implements WasmGCIntrinsicFactory {
    private JSBodyRepository repository;
    private WasmGCJSFunctions jsFunctions;
    private WasmGCBodyGenerator bodyGenerator;

    WasmGCJSBodyRenderer(JSBodyRepository repository) {
        this.repository = repository;
        jsFunctions = new WasmGCJSFunctions();
        bodyGenerator = new WasmGCBodyGenerator(jsFunctions);
    }

    @Override
    public WasmGCIntrinsic createIntrinsic(MethodReference methodRef, WasmGCIntrinsicFactoryContext context) {
        var emitter = repository.emitters.get(methodRef);
        if (emitter == null) {
            return null;
        }
        var inlined = repository.inlineMethods.contains(emitter.method());
        return new WasmGCBodyIntrinsic(emitter, inlined, bodyGenerator, jsFunctions);
    }
}
