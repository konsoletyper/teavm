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

import org.teavm.backend.wasm.gc.TeaVMWasmGCHost;
import org.teavm.jso.JSObject;
import org.teavm.jso.impl.JS;
import org.teavm.jso.impl.JSBodyRepository;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.TeaVMHost;

public final class WasmGCJso {
    private WasmGCJso() {
    }

    public static void install(TeaVMHost host, TeaVMWasmGCHost wasmGCHost, JSBodyRepository jsBodyRepository) {
        host.add(new WasmGCJSDependencies());
        wasmGCHost.addCustomTypeMapperFactory(new WasmGCJSTypeMapper());
        wasmGCHost.addIntrinsicFactory(new WasmGCJSBodyRenderer(jsBodyRepository));

        var jsIntrinsic = new WasmGCJSIntrinsic();
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "wrap", String.class, JSObject.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "unwrapString", JSObject.class, String.class),
                jsIntrinsic);
    }
}
