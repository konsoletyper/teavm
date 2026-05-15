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
package org.teavm.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.runtime.reflect.ProxyImplementor;

class ProxyDependencySupport {
    private ReflectionDependencyListener reflection;
    private DependencyAgent agent;
    private Map<MethodReference, MethodReference> implementedMethods = new HashMap<>();
    private Map<MethodReference, MethodReference> methodProviders = new HashMap<>();
    private Set<MethodReference> usedMethods = new LinkedHashSet<>();
    private Set<String> generatedClasses = new HashSet<>();
    private MethodDependency getDeclaredMethod;

    ProxyDependencySupport(ReflectionDependencyListener reflection, DependencyAgent agent) {
        this.reflection = reflection;
        this.agent = agent;
        getDeclaredMethod = agent.linkMethod(new MethodReference(Class.class, "getDeclaredMethod",
                String.class, Class[].class, Method.class));
        getDeclaredMethod.getVariable(0).propagate(agent.getType(ValueType.parse(Class.class)));
        getDeclaredMethod.getVariable(1).propagate(agent.getType(ValueType.parse(String.class)));
        getDeclaredMethod.getVariable(2).propagate(agent.getType(ValueType.parse(Class[].class)));
        getDeclaredMethod.getVariable(2).getArrayItem().propagate(agent.getType(ValueType.parse(Class.class)));
        getDeclaredMethod.use();
    }

    boolean isGeneratedClass(String className) {
        return generatedClasses.contains(className);
    }

    Set<MethodReference> getUsedMethods() {
        return usedMethods;
    }

    MethodReference getWorkerAcquireMethod(MethodReference method) {
        return methodProviders.get(method);
    }

    void onMethodAdded(MethodDependency method) {
        var implementedMethod = implementedMethods.get(method.getReference());
        if (implementedMethod != null) {
            agent.linkMethod(implementedMethod);
            reflection.addReflectableMethod(agent, agent.getClassSource().getMethod(implementedMethod));
            reflection.addAccessibleMethod(implementedMethod);
            for (var param : implementedMethod.getParameterTypes()) {
                getDeclaredMethod.getVariable(2).getArrayItem().getClassValueNode()
                        .propagate(agent.getType(param));
            }
        }
        if (generatedClasses.contains(method.getReference().getClassName())) {
            if (method.getMethod().hasModifier(ElementModifier.STATIC)
                    && method.getMethod().getName().startsWith(ReflectionMethods.PROXY_ACQUIRE_METHOD)) {
                method.getResult().propagate(agent.getType(ValueType.parse(Method.class)));
            }
        }
    }

    void generateProxyClass(List<? extends String> interfaces, String className) {
        generatedClasses.add(className);
        var cls = new ClassHolder(className);
        cls.setParent(Proxy.class.getName());
        cls.getInterfaces().addAll(interfaces);
        cls.setLevel(AccessLevel.PUBLIC);
        cls.getModifiers().add(ElementModifier.FINAL);
        cls.getAnnotations().add(new AnnotationHolder(ProxyImplementor.class.getName()));

        generateConstructor(cls);

        var generatedMethods = new HashSet<Signature>();
        generatedMethods.add(new Signature("equals", ValueType.parse(Object.class)));
        generatedMethods.add(new Signature("hashCode"));
        generatedMethods.add(new Signature("toString"));
        generateWorkerMethod(new MethodReference(Object.class, "equals", Object.class, boolean.class), cls, 0);
        generateWorkerMethod(new MethodReference(Object.class, "hashCode", int.class), cls, 1);
        generateWorkerMethod(new MethodReference(Object.class, "toString", String.class), cls, 2);
        getDeclaredMethod.getVariable(0).getClassValueNode()
                .propagate(agent.getType(ValueType.object("java.lang.Object")));
        var methodIdHolder = new MethodIdHolder();
        methodIdHolder.id = 3;
        var visited = new HashSet<String>();
        for (var itfName : interfaces) {
            generateWorkersForInterface(itfName, cls, generatedMethods, methodIdHolder, visited);
        }

        agent.submitClass(cls);
        agent.linkClass(className);
        agent.linkMethod(new MethodReference(className, "<init>", ValueType.VOID))
                .propagate(0, agent.getType(ValueType.object(className)))
                .use();

        agent.linkMethod(new MethodReference(Proxy.class, "wrapDependency", Object.class, Object.class))
                .getResult().propagate(agent.getType(ValueType.object(className)));
    }

    private void generateWorkersForInterface(String itfName, ClassHolder cls, Set<Signature> generatedMethods,
            MethodIdHolder methodIdHolder, Set<String> visited) {
        if (!visited.add(itfName)) {
            return;
        }
        var itf = agent.getClassSource().get(itfName);
        for (var method : itf.getMethods()) {
            if (method.hasModifier(ElementModifier.BRIDGE)) {
                continue;
            }
            var signature = new Signature(method.getName(), method.getDescriptor().getParameterTypes());
            if (generatedMethods.add(signature)) {
                generateWorkerMethod(method, cls, methodIdHolder.id++);
                getDeclaredMethod.getVariable(0).getClassValueNode()
                        .propagate(agent.getType(ValueType.object(itfName)));
            }
        }
        for (var parent : itf.getInterfaces()) {
            generateWorkersForInterface(parent, cls, generatedMethods, methodIdHolder, visited);
        }
    }

    private static class MethodIdHolder {
        int id;
    }

    private void generateConstructor(ClassHolder cls) {
        var ctor = new MethodHolder("<init>", ValueType.VOID);
        ctor.setLevel(AccessLevel.PUBLIC);
        var pe = ProgramEmitter.create(ctor, agent.getClassHierarchy());
        var thisVar = pe.var(0, ValueType.object(cls.getName()));
        thisVar.invokeSpecial(Proxy.class, "<init>", thisVar.cast(Proxy.class));
        pe.exit();
    }

    private void generateWorkerMethod(MethodReference originalMethod, ClassHolder cls, int id) {
        generateWorkerMethod(agent.getClassSource().getMethod(originalMethod), cls, id);
    }

    private void generateWorkerMethod(MethodReader originalMethod, ClassHolder cls, int id) {
        var acquireMethod = new MethodHolder(new MethodDescriptor(ReflectionMethods.PROXY_ACQUIRE_METHOD + id,
                Method.class));
        acquireMethod.setLevel(AccessLevel.PRIVATE);
        acquireMethod.getModifiers().addAll(List.of(ElementModifier.NATIVE, ElementModifier.STATIC));
        cls.addMethod(acquireMethod);
        methodProviders.put(acquireMethod.getReference(), originalMethod.getReference());
        usedMethods.add(acquireMethod.getReference());

        var method = new MethodHolder(originalMethod.getDescriptor());
        implementedMethods.put(new MethodReference(cls.getName(), originalMethod.getDescriptor()),
                originalMethod.getReference());
        var pe = ProgramEmitter.create(method, agent.getClassHierarchy());
        var thisVar = pe.var(0, ValueType.object(cls.getName()));
        var methodVar = pe.invoke(acquireMethod.getReference());
        ValueEmitter argsVar;
        if (originalMethod.parameterCount() == 0) {
            argsVar = pe.constantNull(Object[].class);
        } else {
            argsVar = pe.constructArray(Object.class, originalMethod.parameterCount());
            for (var i = 0; i < originalMethod.parameterCount(); ++i) {
                var arg = pe.var(i + 1, originalMethod.parameterType(i));
                argsVar.setElement(i, boxIfNecessary(pe, arg));
            }
        }
        var result = thisVar.getField("h", InvocationHandler.class).invokeVirtual("invoke", Object.class,
                thisVar.cast(Object.class), methodVar, argsVar);
        if (method.getResultType() == ValueType.VOID) {
            pe.exit();
        } else {
            unboxIfNecessary(result, method.getResultType()).returnValue();
        }

        var mainBlock = pe.getBlock();
        var passThoughBlock = pe.prepareBlock();
        var exVar = pe.newVar(Throwable.class);
        for (var exception : originalMethod.getThrownTypes()) {
            var tryCatch = new TryCatchBlock();
            tryCatch.setExceptionType(exception);
            tryCatch.setHandler(passThoughBlock);
            mainBlock.getTryCatchBlocks().add(tryCatch);
        }

        var tryCatch = new TryCatchBlock();
        tryCatch.setHandler(passThoughBlock);
        tryCatch.setExceptionType(RuntimeException.class.getName());
        mainBlock.getTryCatchBlocks().add(tryCatch);

        passThoughBlock.setExceptionVariable(exVar.getVariable());
        pe.enter(passThoughBlock);
        var throwInsn = new RaiseInstruction();
        throwInsn.setException(exVar.getVariable());
        pe.addInstruction(throwInsn);

        var wrapBlock = pe.prepareBlock();
        var wrapTryCatch = new TryCatchBlock();
        wrapTryCatch.setHandler(wrapBlock);
        wrapTryCatch.setExceptionType(Exception.class.getName());
        mainBlock.getTryCatchBlocks().add(wrapTryCatch);
        exVar = pe.newVar(Exception.class);
        wrapBlock.setExceptionVariable(exVar.getVariable());
        pe.enter(wrapBlock);
        var wrapperVar = pe.construct(UndeclaredThrowableException.class, exVar.cast(Throwable.class));
        throwInsn = new RaiseInstruction();
        throwInsn.setException(wrapperVar.getVariable());
        pe.addInstruction(throwInsn);

        cls.addMethod(method);
    }

    private ValueEmitter boxIfNecessary(ProgramEmitter pe, ValueEmitter value) {
        if (value.getType() instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) value.getType()).getKind()) {
                case BOOLEAN:
                    return pe.invoke(Boolean.class, "valueOf", Boolean.class, value);
                case CHARACTER:
                    return pe.invoke(Character.class, "valueOf", Character.class, value);
                case BYTE:
                    return pe.invoke(Byte.class, "valueOf", Byte.class, value);
                case SHORT:
                    return pe.invoke(Short.class, "valueOf", Short.class, value);
                case INTEGER:
                    return pe.invoke(Integer.class, "valueOf", Integer.class, value);
                case LONG:
                    return pe.invoke(Long.class, "valueOf", Long.class, value);
                case FLOAT:
                    return pe.invoke(Float.class, "valueOf", Float.class, value);
                case DOUBLE:
                    return pe.invoke(Double.class, "valueOf", Double.class, value);
            }
        }
        return value.cast(Object.class);
    }

    private ValueEmitter unboxIfNecessary(ValueEmitter value, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return value.cast(Boolean.class).invokeSpecial("booleanValue", boolean.class);
                case CHARACTER:
                    return value.cast(Character.class).invokeSpecial("charValue", char.class);
                case BYTE:
                    return value.cast(Byte.class).invokeSpecial("byteValue", byte.class);
                case SHORT:
                    return value.cast(Short.class).invokeSpecial("shortValue", short.class);
                case INTEGER:
                    return value.cast(Integer.class).invokeSpecial("intValue", int.class);
                case LONG:
                    return value.cast(Long.class).invokeSpecial("longValue", long.class);
                case FLOAT:
                    return value.cast(Float.class).invokeSpecial("floatValue", float.class);
                case DOUBLE:
                    return value.cast(Double.class).invokeSpecial("doubleValue", double.class);
            }
        }
        return value.cast(type);
    }

    private static final class Signature {
        String name;
        ValueType[] args;

        Signature(String name, ValueType... args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Signature)) {
                return false;
            }
            var signature = (Signature) object;
            return Objects.equals(name, signature.name) && Objects.deepEquals(args, signature.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(args));
        }
    }
}
