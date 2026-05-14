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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContext;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.reflect.ClassInfo;

public class ProxyIntrinsic implements WasmGCInlineIntrinsic {
    private ReflectionDependencyListener reflection;
    private WasmGCCodeGenContext codeGenContext;
    private ProxyIntrinsicContext intrinsicContext;

    public ProxyIntrinsic(ReflectionDependencyListener reflection, WasmGCCodeGenContext codeGenContext,
            ProxyIntrinsicContext intrinsicContext) {
        this.reflection = reflection;
        this.codeGenContext = codeGenContext;
        this.intrinsicContext = intrinsicContext;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "registerProxyClasses":
                registerProxyClasses(builder);
                break;
            case "wrapDependency":
                context.generate(builder, invocation.getArguments().get(0));
                break;
        }
    }

    private void registerProxyClasses(WasmInstructionBuilder builder) {
        var initializedMethods = new HashSet<MethodReference>();
        for (var method : reflection.getProxyReflectionMethods()) {
            var methodDep = codeGenContext.dependency().getMethod(method);
            if (methodDep != null && methodDep.isUsed()) {
                var methodToAcquire = reflection.getProxyWorkerAcquireMethod(method);
                if (!initializedMethods.add(methodToAcquire)) {
                    continue;
                }
                WasmGCGenerationUtil.emitClassLiteral(codeGenContext.classInfoProvider(), builder,
                        ValueType.object(methodToAcquire.getClassName()));
                builder.getGlobal(codeGenContext.strings().getStringConstant(methodToAcquire.getName()).global);
                WasmGCGenerationUtil.allocateArray(codeGenContext.classInfoProvider(),
                        ValueType.parse(Class.class), builder, (array, b) -> {
                            for (var param : methodToAcquire.getParameterTypes()) {
                                WasmGCGenerationUtil.emitClassLiteral(codeGenContext.classInfoProvider(), b, param);
                            }
                            b.arrayNewFixed(array, methodToAcquire.parameterCount());
                        });
                var getDeclaredMethod = codeGenContext.functions().forInstanceMethod(new MethodReference(Class.class,
                        "getDeclaredMethod", String.class, Class[].class, Method.class));
                builder.call(getDeclaredMethod);
                builder.setGlobal(intrinsicContext.getMethodGlobal(methodToAcquire));
            }
        }
        for (var entry : reflection.getProxyClasses().entrySet()) {
            var itfList = entry.getKey();
            var cls = entry.getValue();
            var appendItf = codeGenContext.functions().forStaticMethod(new MethodReference(Proxy.class,
                    "appendInterface", ClassInfo.class, void.class));
            for (var itf : itfList) {
                builder.getGlobal(codeGenContext.classInfoProvider().getClassInfo(itf).getPointer());
                builder.call(appendItf);
            }
            var registerCls = codeGenContext.functions().forStaticMethod(new MethodReference(Proxy.class,
                    "registerClass", ClassInfo.class, void.class));
            builder.getGlobal(codeGenContext.classInfoProvider().getClassInfo(cls).getPointer());
            builder.call(registerCls);
        }
    }
}
