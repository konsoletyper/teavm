/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.vm;

import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import java.lang.reflect.Modifier;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.PluggableDependency;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.interop.PlatformMarker;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Value;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.spi.TeaVMHost;

@CompileTime
public final class TeaVMPluginUtil {
    private TeaVMPluginUtil() {
    }

    public static void handleNatives(TeaVMHost host, Class<?> cls) {
        if (!isBootstrap()) {
            return;
        }

        handleDependenciesImpl(host, cls);
        /*
        var jsHost = host.getExtension(TeaVMJavaScriptHost.class);
        if (jsHost != null) {
            handleNativesImpl(host, jsHost, cls);
        }
         */
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    @Meta
    private static native void handleNativesImpl(TeaVMHost host, TeaVMJavaScriptHost jsHost, Class<?> cls);

    private static void handleNativesImpl(Value<TeaVMHost> host, Value<TeaVMJavaScriptHost> jsHost,
            IntrospectClass<?> cls) {
        for (var method : cls.declaredMethods()) {
            if (!Modifier.isNative(method.modifiers())) {
                continue;
            }

            var generatedBy = method.annotation(GeneratedBy.class);
            if (generatedBy != null) {
                var generatorClass = (IntrospectClass<?>) generatedBy.value("value");
                var generatorConstructor = generatorClass.method("<init>");
                var generatorConstructorCaller = caller(generatorConstructor);

                Value<MethodReference> methodRef = methodToReference(method);
                emit(() -> jsHost.get().add(methodRef.get(), (Generator) generatorConstructorCaller.construct()));
            }

            var injectedBy = method.annotation(InjectedBy.class);
            if (injectedBy != null) {
                var generatorClass = (IntrospectClass<?>) injectedBy.value("value");
                var generatorConstructor = generatorClass.method("<init>");
                var generatorConstructorCaller = caller(generatorConstructor);

                Value<MethodReference> methodRef = methodToReference(method);
                emit(() -> jsHost.get().add(methodRef.get(), (Injector) generatorConstructorCaller.construct()));
            }
        }
    }

    @Meta
    private static native void handleDependenciesImpl(TeaVMHost host, Class<?> cls);

    private static void handleDependenciesImpl(Value<TeaVMHost> host, IntrospectClass<?> cls) {
        for (var method : cls.declaredMethods()) {
            var dependency = method.annotation(PluggableDependency.class);
            if (dependency != null) {
                var generatorClass = (IntrospectClass<?>) dependency.value("value");
                var generatorConstructor = generatorClass.method("<init>");

                Value<MethodReference> methodRef = methodToReference(method);
                var generatorConstructorCaller = caller(generatorConstructor);
                emit(() -> host.get().add(methodRef.get(), (DependencyPlugin) generatorConstructorCaller.construct()));
            }
        }
    }

    private static Value<MethodReference> methodToReference(IntrospectMethod method) {
        int signatureSize = method.parameters().size() + 1;
        Value<ValueType[]> signature = emit(() -> new ValueType[signatureSize]);
        for (int i = 0; i < method.parameters().size(); ++i) {
            Value<ValueType> paramType = classToValueType(method.parameters().get(i).type());
            int index = i;
            emit(() -> signature.get()[index] = paramType.get());
        }
        Value<ValueType> returnType = classToValueType(method.returnType());
        emit(() -> signature.get()[signatureSize - 1] = returnType.get());

        String className = method.declaringClass().name();
        String name = method.name();
        return emit(() -> new MethodReference(className, new MethodDescriptor(name, signature.get())));
    }

    private static Value<ValueType> classToValueType(IntrospectClass<?> cls) {
        if (cls.isArray()) {
            Value<ValueType> itemType = classToValueType(cls.componentType());
            return emit(() -> ValueType.arrayOf(itemType.get()));
        } else if (cls.isPrimitive()) {
            switch (cls.name()) {
                case "boolean":
                    return emit(() -> ValueType.BOOLEAN);
                case "byte":
                    return emit(() -> ValueType.BYTE);
                case "short":
                    return emit(() -> ValueType.SHORT);
                case "char":
                    return emit(() -> ValueType.CHARACTER);
                case "int":
                    return emit(() -> ValueType.INTEGER);
                case "long":
                    return emit(() -> ValueType.LONG);
                case "float":
                    return emit(() -> ValueType.FLOAT);
                case "double":
                    return emit(() -> ValueType.DOUBLE);
                case "void":
                    return emit(() -> ValueType.VOID);
                default:
                    throw new IllegalArgumentException("Unexpected primitive type: " + cls.name());
            }
        } else {
            String name = cls.name();
            return emit(() -> ValueType.object(name));
        }
    }
}
