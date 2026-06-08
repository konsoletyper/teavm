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
package org.teavm.backend.c.intrinsic.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.MethodReference;
import org.teavm.reflection.ReflectionDependencyListener;

public class ProxyMethodIntrinsic implements Intrinsic {
    private ReflectionDependencyListener reflection;
    private ProxyIntrinsicContext intrinsicContext;

    public ProxyMethodIntrinsic(ReflectionDependencyListener reflection, ProxyIntrinsicContext intrinsicContext) {
        this.reflection = reflection;
        this.intrinsicContext = intrinsicContext;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        if (!reflection.isGeneratedProxyClass(method.getClassName())) {
            return false;
        }
        return reflection.getProxyWorkerAcquireMethod(method) != null;
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        var methodToAcquire = reflection.getProxyWorkerAcquireMethod(invocation.getMethod());
        int index = intrinsicContext.getMethodIndex(methodToAcquire);
        context.includes().includePath("proxy.h");
        context.writer().print("teavm_getProxyMethod(" + index + ")");
    }
}
