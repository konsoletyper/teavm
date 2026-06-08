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

import java.lang.reflect.Proxy;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.MethodReference;

public class ProxyIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(Proxy.class.getName())) {
            return false;
        }
        switch (method.getName()) {
            case "registerProxyClasses":
            case "wrapDependency":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "registerProxyClasses":
                context.includes().includePath("proxy.h");
                context.writer().print("teavm_proxy_registerClasses()");
                break;
            case "wrapDependency":
                context.emit(invocation.getArguments().get(0));
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
