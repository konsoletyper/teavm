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

import static org.teavm.jso.impl.JSMethods.GET;
import static org.teavm.jso.impl.JSMethods.GET_PURE;
import static org.teavm.jso.impl.JSMethods.GLOBAL;
import static org.teavm.jso.impl.JSMethods.IMPORT_MODULE;
import static org.teavm.jso.impl.JSMethods.JS_CLASS;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import static org.teavm.jso.impl.JSMethods.STRING;
import static org.teavm.jso.impl.JSMethods.THROW_CCE_IF_FALSE;
import static org.teavm.jso.impl.JSMethods.WASM_GC_JS_RUNTIME_CLASS;
import static org.teavm.jso.impl.JSMethods.WRAP;
import static org.teavm.jso.impl.JSMethods.WRAP_STRING;
import org.teavm.backend.wasm.gc.TeaVMWasmGCHost;
import org.teavm.jso.impl.JSBodyRepository;
import org.teavm.jso.impl.JSClassObjectToExpose;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
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
        wasmGCHost.addIntrinsic(WRAP_STRING, jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS_CLASS, "unwrapString", JS_OBJECT, STRING), jsIntrinsic);
        wasmGCHost.addIntrinsic(GLOBAL, jsIntrinsic);
        wasmGCHost.addIntrinsic(THROW_CCE_IF_FALSE, jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS_CLASS, "isNull", JS_OBJECT, ValueType.BOOLEAN), jsIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS_CLASS, "jsArrayItem", OBJECT, ValueType.INTEGER, OBJECT),
                jsIntrinsic);
        wasmGCHost.addIntrinsic(GET, jsIntrinsic);
        wasmGCHost.addIntrinsic(GET_PURE, jsIntrinsic);
        wasmGCHost.addIntrinsic(IMPORT_MODULE, jsIntrinsic);

        var wrapperIntrinsic = new WasmGCJSWrapperIntrinsic();
        wasmGCHost.addIntrinsic(WRAP, wrapperIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(JS_WRAPPER_CLASS, "isJava", JS_OBJECT, ValueType.BOOLEAN),
                wrapperIntrinsic);

        var runtimeInstrinsic = new WasmGCJSRuntimeIntrinsic(commonGen);
        wasmGCHost.addIntrinsic(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "wrapObject", OBJECT,
                JS_OBJECT), runtimeInstrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(WasmGCJSRuntime.CharArrayData.class, "of", String.class,
                WasmGCJSRuntime.CharArrayData.class), runtimeInstrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(WasmGCJSRuntime.CharArrayData.class, "asString", String.class),
                runtimeInstrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(WasmGCJSRuntime.CharArrayData.class, "create", int.class,
                WasmGCJSRuntime.CharArrayData.class), runtimeInstrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(WasmGCJSRuntime.CharArrayData.class, "put", int.class,
                char.class, void.class), runtimeInstrinsic);
        var nonNullExternal = "org.teavm.jso.impl.wasmgc.WasmGCJSRuntime$NonNullExternal";
        wasmGCHost.addIntrinsic(new MethodReference(nonNullExternal, "toNullable", JS_OBJECT), runtimeInstrinsic);
    }
}
