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
package org.teavm.backend.javascript.intrinsics.reflection;

import java.util.function.Function;
import org.teavm.backend.javascript.ProviderContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.reflection.ReflectionDependencyListener;

public class ProxyMethodInjectorProvider implements Function<ProviderContext, Injector> {
    private ReflectionDependencyListener reflection;
    private ProxyGeneratorContext proxyContext;

    public ProxyMethodInjectorProvider(ReflectionDependencyListener reflection, ProxyGeneratorContext proxyContext) {
        this.reflection = reflection;
        this.proxyContext = proxyContext;
    }

    @Override
    public Injector apply(ProviderContext context) {
        if (reflection.isGeneratedProxyClass(context.getMethod().getClassName())) {
            var methodToAcquire = reflection.getProxyWorkerAcquireMethod(context.getMethod());
            if (methodToAcquire != null) {
                return (ctx, method) -> {
                    ctx.getWriter().appendFunction("$rt_proxyMethods").append(".")
                            .append(proxyContext.getPropertyName(methodToAcquire));
                };
            }
        }
        return null;
    }
}
