/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics.reflection;

import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.model.MethodReference;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.vm.intrinsic.IntrinsicProvider;

public class ProxyMethodIntrinsicProvider implements IntrinsicProvider<WasmGCInlineIntrinsic> {
    private ReflectionDependencyListener reflection;
    private ProxyIntrinsicContext intrinsicContext;

    public ProxyMethodIntrinsicProvider(ReflectionDependencyListener reflection, 
            ProxyIntrinsicContext intrinsicContext) {
        this.reflection = reflection;
        this.intrinsicContext = intrinsicContext;
    }

    @Override
    public WasmGCInlineIntrinsic getIntrinsic(MethodReference method) {
        if (reflection.isGeneratedProxyClass(method.getClassName())) {
            var methodToAcquire = reflection.getProxyWorkerAcquireMethod(method);
            if (methodToAcquire != null) {
                return (invocation, context, builder) -> {
                    builder.getGlobal(intrinsicContext.getMethodGlobal(methodToAcquire));
                };
            }
        }
        return null;
    }
}
