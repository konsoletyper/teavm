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

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.findClass;
import java.lang.reflect.Modifier;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.PlatformMarker;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectMethod;
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

        handleNativesImpl(host, host.getExtension(TeaVMJavaScriptHost.class), cls);
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    @Meta
    private static native void handleNativesImpl(TeaVMHost host, TeaVMJavaScriptHost jsHost, Class<?> cls);

    private static void handleNativesImpl(Value<TeaVMHost> host, Value<TeaVMJavaScriptHost> jsHost,
            ReflectClass<?> cls) {
        for (ReflectMethod method : cls.getDeclaredMethods()) {
            if (!Modifier.isNative(method.getModifiers())) {
                continue;
            }

            GeneratedBy generatedBy = method.getAnnotation(GeneratedBy.class);
            if (generatedBy != null) {
                ReflectClass<?> generatorClass = findClass(generatedBy.value().getName());
                ReflectMethod generatorConstructor = generatorClass.getMethod("<init>");

                Value<MethodReference> methodRef = methodToReference(method);
                emit(() -> jsHost.get().add(methodRef.get(), (Generator) generatorConstructor.construct()));
            }

            InjectedBy injectedBy = method.getAnnotation(InjectedBy.class);
            if (injectedBy != null) {
                ReflectClass<?> generatorClass = findClass(injectedBy.value().getName());
                ReflectMethod generatorConstructor = generatorClass.getMethod("<init>");

                Value<MethodReference> methodRef = methodToReference(method);
                emit(() -> jsHost.get().add(methodRef.get(), (Injector) generatorConstructor.construct()));
            }

            PluggableDependency dependency = method.getAnnotation(PluggableDependency.class);
            if (dependency != null) {
                ReflectClass<?> generatorClass = findClass(dependency.value().getName());
                ReflectMethod generatorConstructor = generatorClass.getMethod("<init>");

                Value<MethodReference> methodRef = methodToReference(method);
                emit(() -> host.get().add(methodRef.get(), (DependencyPlugin) generatorConstructor.construct()));
            }
        }
    }

    private static Value<MethodReference> methodToReference(ReflectMethod method) {
        int signatureSize = method.getParameterCount() + 1;
        Value<ValueType[]> signature = emit(() -> new ValueType[signatureSize]);
        for (int i = 0; i < method.getParameterCount(); ++i) {
            Value<ValueType> paramType = classToValueType(method.getParameterType(i));
            int index = i;
            emit(() -> signature.get()[index] = paramType.get());
        }
        Value<ValueType> returnType = classToValueType(method.getReturnType());
        emit(() -> signature.get()[signatureSize - 1] = returnType.get());

        String className = method.getDeclaringClass().getName();
        String name = method.getName();
        return emit(() -> new MethodReference(className, new MethodDescriptor(name, signature.get())));
    }

    private static Value<ValueType> classToValueType(ReflectClass<?> cls) {
        if (cls.isArray()) {
            Value<ValueType> itemType = classToValueType(cls.getComponentType());
            return emit(() -> ValueType.arrayOf(itemType.get()));
        } else if (cls.isPrimitive()) {
            switch (cls.getName()) {
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
                    throw new IllegalArgumentException("Unexpected primitive type: " + cls.getName());
            }
        } else {
            String name = cls.getName();
            return emit(() -> ValueType.object(name));
        }
    }
}
