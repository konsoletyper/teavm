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
import org.teavm.jso.impl.JSClassObjectToExpose;
import org.teavm.jso.impl.JSWrapper;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.TeaVMHost;

public final class WasmGCJso {
    private WasmGCJso() {
    }

    public static void install(TeaVMHost host, TeaVMWasmGCHost wasmGCHost, JSBodyRepository jsBodyRepository) {
        host.add(new WasmGCJSDependencies());
        host.add(new WasmGCJSWrapperTransformer());
        var jsFunctions = new WasmGCJSFunctions();
        var commonGen = new WasmGCJsoCommonGenerator(jsFunctions);
        wasmGCHost.addCustomTypeMapperFactory(new WasmGCJSTypeMapper());
        wasmGCHost.addIntrinsicFactory(new WasmGCJSBodyRenderer(jsBodyRepository, jsFunctions, commonGen));
        wasmGCHost.addGeneratorFactory(new WasmGCMarshallMethodGeneratorFactory(commonGen));
        wasmGCHost.addClassConsumer((context, className) -> {
            var cls = context.classes().get(className);
            if (cls != null && cls.getAnnotations().get(JSClassObjectToExpose.class.getName()) != null) {
                commonGen.getDefinedClass(WasmGCJsoContext.wrap(context), className);
            }
        });

        var jsIntrinsic = new WasmGCJSIntrinsic(commonGen, jsFunctions);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "wrap", String.class, JSObject.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "unwrapString", JSObject.class, String.class),
                jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "global", String.class, JSObject.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "throwCCEIfFalse", boolean.class, JSObject.class,
                JSObject.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "isNull", JSObject.class, boolean.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "jsArrayItem", Object.class, int.class, Object.class),
                jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "get", JSObject.class, JSObject.class, JSObject.class),
                jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "getPure", JSObject.class, JSObject.class,
                JSObject.class), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS.class, "importModule", String.class, JSObject.class),
                jsIntrinsic);

        var wrapperIntrinsic = new WasmGCJSWrapperIntrinsic();
        wasmGCHost.addIntrinsic(new MethodReference(JSWrapper.class, "wrap", JSObject.class, Object.class),
                wrapperIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JSWrapper.class, "isJava", JSObject.class, boolean.class),
                wrapperIntrinsic);

        wasmGCHost.addIntrinsic(new MethodReference(WasmGCJSRuntime.class, "wrapObject", Object.class,
                JSObject.class), new WasmGCJSRuntimeIntrinsic(commonGen));
    }
}
