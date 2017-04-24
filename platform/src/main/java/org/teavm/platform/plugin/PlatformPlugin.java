/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.wasm.TeaVMWasmHost;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class PlatformPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        if (host.getExtension(TeaVMJavaScriptHost.class) != null) {
            host.add(new MetadataProviderTransformer());
            host.add(new ResourceTransformer());
            host.add(new ResourceAccessorTransformer(host));
            host.add(new ResourceAccessorDependencyListener());
        } else {
            host.add(new StringAmplifierTransformer());
        }

        TeaVMWasmHost wasmHost = host.getExtension(TeaVMWasmHost.class);
        if (wasmHost != null) {
            wasmHost.add(ctx -> new MetadataIntrinsic(ctx.getClassSource(), ctx.getClassLoader(), ctx.getServices(),
                    ctx.getProperties()));
            wasmHost.add(ctx -> new ResourceReadIntrinsic(ctx.getClassSource(), ctx.getClassLoader()));

            wasmHost.add(ctx -> new WasmIntrinsic() {
                @Override
                public boolean isApplicable(MethodReference methodReference) {
                    return methodReference.getClassName().equals(StringAmplifier.class.getName());
                }

                @Override
                public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
                    return manager.generate(invocation.getArguments().get(0));
                }
            });
        }

        host.add(new AsyncMethodProcessor());
        host.add(new NewInstanceDependencySupport());
        host.add(new ClassLookupDependencySupport());
        host.add(new EnumDependencySupport());
        host.add(new AnnotationDependencySupport());
        host.add(new PlatformDependencyListener());
    }
}
