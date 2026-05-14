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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.reflect.ClassInfo;

public class ProxyGenerator implements Injector {
    private ReflectionDependencyListener reflection;
    private ProxyGeneratorContext proxyContext;

    public ProxyGenerator(ReflectionDependencyListener reflection, ProxyGeneratorContext proxyContext) {
        this.reflection = reflection;
        this.proxyContext = proxyContext;
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "registerProxyClasses":
                registerProxyClasses(context);
                break;
            case "wrapDependency":
                context.writeExpr(context.getArgument(0));
                break;
        }
    }

    private void registerProxyClasses(InjectorContext context) {
        var writer = context.getWriter();
        writer.append("Object.assign(").appendFunction("$rt_proxyMethods").append(",").ws().append("{")
                .softNewLine().indent();
        var first = true;
        var initializedMethods = new HashSet<MethodReference>();
        for (var method : reflection.getProxyReflectionMethods()) {
            var methodDep = context.getDependencies().getMethod(method);
            if (methodDep != null && methodDep.isUsed()) {
                var methodToAcquire = reflection.getProxyWorkerAcquireMethod(method);
                if (!initializedMethods.add(methodToAcquire)) {
                    continue;
                }
                if (!first) {
                    writer.append(",").softNewLine();
                }
                first = false;
                writer.append(proxyContext.getPropertyName(methodToAcquire)).append(":").ws();
                writer.appendMethod(Class.class, "getDeclaredMethod", String.class, Class[].class, Method.class);
                writer.append("(").appendFunction("$rt_cls").append("(");
                RenderingUtil.typeToClsString(writer, ValueType.object(methodToAcquire.getClassName()));
                writer.append("),").ws().appendFunction("$rt_str").append("(");
                RenderingUtil.writeString(writer, methodToAcquire.getName());
                writer.append("),").ws();
                writer.appendFunction("$rt_wrapArray").append("(").appendClass("java.lang.Class").append(",")
                        .ws().append("[");
                var firstParam = true;
                for (var param : methodToAcquire.getParameterTypes()) {
                    if (!firstParam) {
                        writer.append(",").ws();
                    }
                    firstParam = false;
                    writer.appendFunction("$rt_cls").append("(");
                    RenderingUtil.typeToClsString(writer, param);
                    writer.append(")");
                }
                writer.append("]))");
            }
        }
        writer.softNewLine().outdent().append("});").softNewLine();
        for (var entry : reflection.getProxyClasses().entrySet()) {
            var itfList = entry.getKey();
            var cls = entry.getValue();
            for (var itf : itfList) {
                writer.appendMethod(Proxy.class, "appendInterface", ClassInfo.class, void.class).append("(");
                RenderingUtil.typeToClsString(writer, ValueType.object(itf));
                writer.append(");").softNewLine();
            }
            writer.appendMethod(Proxy.class, "registerClass", ClassInfo.class, void.class).append("(");
            RenderingUtil.typeToClsString(writer, ValueType.object(cls));
            writer.append(");").softNewLine();
        }
    }
}
