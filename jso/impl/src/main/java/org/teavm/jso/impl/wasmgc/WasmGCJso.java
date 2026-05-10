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

import static org.teavm.jso.impl.JSMethods.JS_CLASS;
import org.teavm.backend.wasm.TeaVMWasmGCHost;
import org.teavm.jso.impl.JSBodyRepository;
import org.teavm.jso.impl.JSClassObjectToExpose;
import org.teavm.jso.impl.JSWrapper;
import org.teavm.vm.spi.TeaVMHost;

public final class WasmGCJso {
    private WasmGCJso() {
    }

    public static void install(TeaVMHost host, TeaVMWasmGCHost wasmGCHost, JSBodyRepository jsBodyRepository) {
        host.add(new WasmGCJSDependencies());
        host.add(new WasmGCJSWrapperTransformer());
        wasmGCHost.contributeToCodeGen((ctx, reg) -> {
            var jsFunctions = new WasmGCJSFunctions(ctx.functionTypes(), ctx.names(), ctx.module());
            var commonGen = new WasmGCJsoCommonGenerator(jsFunctions, ctx.functionTypes(), ctx.classes(),
                    ctx.functions(), ctx.typeMapper(), ctx.names(), ctx.strings(), ctx.module(),
                    ctx.exceptionTag(), ctx.initializerRegistry(), ctx.entryPoint());
            for (var className : ctx.classes().getClassNames()) {
                var cls = ctx.classes().get(className);
                if (cls.getAnnotations().get(JSClassObjectToExpose.class.getName()) != null) {
                    commonGen.getDefinedClass(cls.getName());
                }
            }
            reg.inlineIntrinsics().registerIntrinsic(new WasmGCJSBodyRenderer(jsBodyRepository, jsFunctions,
                    commonGen));
            reg.bodyIntrinsics().registerIntrinsic(new WasmGCMarshallMethodGeneratorFactory(commonGen, ctx.classes(),
                    ctx.typeMapper()));
            reg.inlineIntrinsics().registerIntrinsic(JS_CLASS, new WasmGCJSIntrinsic(commonGen, jsFunctions,
                    ctx.diagnostics(), ctx.classInfoProvider(), ctx.functions(), ctx.functionTypes(), ctx.names(),
                    ctx.module(), ctx.exceptionTag()));
            var runtimeIntrinsic = new WasmGCJSRuntimeIntrinsic(commonGen, ctx.classInfoProvider(), ctx.functions());
            reg.inlineIntrinsics().registerIntrinsic(WasmGCJSRuntime.class, runtimeIntrinsic);
            reg.inlineIntrinsics().registerIntrinsic(WasmGCJSRuntime.CharArrayData.class, runtimeIntrinsic);
            reg.inlineIntrinsics().registerIntrinsic(WasmGCJSRuntime.NonNullExternal.class, runtimeIntrinsic);
            reg.inlineIntrinsics().registerIntrinsic(JSWrapper.class, new WasmGCJSWrapperIntrinsic(ctx.typeMapper(),
                    ctx.functions(), ctx.functionTypes(), ctx.module()));
        });

        wasmGCHost.addCustomTypeMapperFactory(new WasmGCJSTypeMapper());
    }
}
