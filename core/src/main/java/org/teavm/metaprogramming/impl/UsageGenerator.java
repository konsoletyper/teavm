/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.metaprogramming.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.MethodDependency;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.impl.model.MethodModel;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class UsageGenerator {
    private static Map<DependencyAgent, Integer> suffixGenerator = new WeakHashMap<>();
    DependencyAgent agent;
    MethodModel model;
    MethodDependency methodDep;
    CallLocation location;
    Diagnostics diagnostics;
    Method proxyMethod;
    private MetaprogrammingClassLoader classLoader;
    private boolean annotationErrorReported;

    UsageGenerator(DependencyAgent agent, MethodModel model, MethodDependency methodDep, CallLocation location,
            MetaprogrammingClassLoader classLoader) {
        this.agent = agent;
        this.diagnostics = agent.getDiagnostics();
        this.model = model;
        this.methodDep = methodDep;
        this.location = location;
        this.classLoader = classLoader;
    }

    public void installProxyEmitter() {
        Diagnostics diagnostics = agent.getDiagnostics();

        MethodDependency getClassDep = agent.linkMethod(new MethodReference(Object.class, "getClass", Class.class),
                location);
        getClassDep.getThrown().connect(methodDep.getThrown());

        try {
            proxyMethod = getJavaMethod(classLoader, model.getMetaMethod());
            proxyMethod.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            StringWriter stackTraceWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTraceWriter));
            diagnostics.error(location, "Error accessing proxy method {{m0}}: " + stackTraceWriter.getBuffer(),
                    model.getMetaMethod());
            return;
        }

        if (model.getClassParameterIndex() >= 0) {
            int index = (model.isStatic() ? 0 : 1) + model.getClassParameterIndex();
            methodDep.getVariable(index).addConsumer(type -> consumeType(type, getClassDep));
        } else {
            emitPermutation(null, getClassDep);
        }

        installAdditionalDependencies(getClassDep);
    }

    private void installAdditionalDependencies(MethodDependency getClassDep) {
        MethodDependency nameDep = agent.linkMethod(new MethodReference(Class.class, "getName", String.class),
                location);
        getClassDep.getResult().connect(nameDep.getVariable(0));
        nameDep.getThrown().connect(methodDep.getThrown());
        nameDep.use();

        MethodDependency equalsDep = agent.linkMethod(new MethodReference(String.class, "equals", Object.class,
                boolean.class), location);
        nameDep.getResult().connect(equalsDep.getVariable(0));
        equalsDep.getVariable(1).propagate(agent.getType("java.lang.String"));
        equalsDep.getThrown().connect(methodDep.getThrown());
        equalsDep.use();

        MethodDependency hashCodeDep = agent.linkMethod(new MethodReference(String.class, "hashCode", int.class),
                location);
        nameDep.getResult().connect(hashCodeDep.getVariable(0));
        hashCodeDep.getThrown().connect(methodDep.getThrown());
        hashCodeDep.use();
    }

    private void consumeType(DependencyType type, MethodDependency getClassDep) {
        emitPermutation(findClass(type.getName()), getClassDep);
    }

    private void emitPermutation(ValueType type, MethodDependency getClassDep) {
        if (!classLoader.isCompileTimeClass(model.getMetaMethod().getClassName()) && !annotationErrorReported) {
            annotationErrorReported = true;
            diagnostics.error(location, "Metaprogramming method should be within class marked with "
                    + "{{c0}} annotation", CompileTime.class.getName());
            return;
        }

        MethodReference implRef = model.getUsages().get(type);
        if (implRef != null) {
            return;
        }

        implRef = buildMethodReference(type);
        model.getUsages().put(type, implRef);
        //emitter = new EmitterImpl<>(emitterContext, model.getProxyMethod(), model.getMethod().getReturnType());

        /*for (int i = 0; i <= model.getParameters().size(); ++i) {
            emitter.generator.getProgram().createVariable();
        }
        */

        Object[] proxyArgs = new Object[model.getMetaParameterCount()];
        for (int i = 0; i < proxyArgs.length; ++i) {
            if (i == model.getMetaClassParameterIndex()) {
                proxyArgs[i] = MetaprogrammingImpl.findClass(type);
            } else {
                proxyArgs[i] = new ValueImpl<>(null, null, model.getMetaParameterType(i));
            }
        }

        try {
            proxyMethod.invoke(null, proxyArgs);
            //emitter.close();
            Program program = null; //emitter.generator.getProgram();
            //new BoxingEliminator().optimize(program);

            ClassHolder cls = new ClassHolder(implRef.getClassName());
            cls.setLevel(AccessLevel.PUBLIC);
            cls.setParent("java.lang.Object");

            MethodHolder method = new MethodHolder(implRef.getDescriptor());
            method.setLevel(AccessLevel.PUBLIC);
            method.getModifiers().add(ElementModifier.STATIC);
            method.setProgram(program);
            cls.addMethod(method);

            agent.submitClass(cls);
        } catch (IllegalAccessException | InvocationTargetException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            diagnostics.error(location, "Error calling proxy method {{m0}}: " + writer.toString(),
                    model.getMetaMethod());
        }

        MethodDependency implMethod = agent.linkMethod(implRef, location);
        for (int i = 0; i < implRef.parameterCount(); ++i) {
            methodDep.getVariable(i + 1).connect(implMethod.getVariable(i + 1));
        }

        if (model.getClassParameterIndex() >= 0) {
            implMethod.getVariable(model.getClassParameterIndex() + 1).connect(getClassDep.getVariable(0));
        }

        if (implMethod.getResult() != null) {
            implMethod.getResult().connect(methodDep.getResult());
        }
        implMethod.getThrown().connect(methodDep.getThrown());
        implMethod.use();

        agent.linkClass(implRef.getClassName(), location);
    }

    private ValueType findClass(String name) {
        // TODO: dirty hack due to bugs somewhere in TeaVM
        if (name.startsWith("[")) {
            ValueType type = ValueType.parseIfPossible(name);
            if (type != null) {
                return type;
            }

            int degree = 0;
            while (name.charAt(degree) == '[') {
                ++degree;
            }
            type = ValueType.object(name.substring(degree));

            while (degree-- > 0) {
                type = ValueType.arrayOf(type);
            }
            return type;
        } else {
            return ValueType.object(name);
        }
    }

    private MethodReference buildMethodReference(ValueType type) {
        if (model.getClassParameterIndex() < 0) {
            return new MethodReference(model.getMethod().getClassName() + "$PROXY$" + getSuffix(),
                    model.getMethod().getDescriptor());
        }

        int i = 0;
        ValueType[] signature = new ValueType[model.getMetaParameterCount()];
        for (i = 0; i < signature.length; ++i) {
            signature[i] = model.getMetaParameterType(i);
        }
        signature[i] = model.getMethod().getReturnType();

        return new MethodReference(model.getMethod().getClassName() + "$PROXY$" + getSuffix(),
                model.getMethod().getName(), signature);
    }

    private int getSuffix() {
        int suffix = suffixGenerator.getOrDefault(agent, 0);
        suffixGenerator.put(agent, suffix + 1);
        return suffix;
    }

    private Method getJavaMethod(ClassLoader classLoader, MethodReference ref) throws ReflectiveOperationException {
        Class<?> cls = Class.forName(ref.getClassName(), true, classLoader);
        Class<?>[] parameterTypes = new Class<?>[ref.parameterCount()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = getJavaType(classLoader, ref.parameterType(i));
        }
        return cls.getDeclaredMethod(ref.getName(), parameterTypes);
    }

    private Class<?> getJavaType(ClassLoader classLoader, ValueType type) throws ReflectiveOperationException {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return boolean.class;
                case BYTE:
                    return byte.class;
                case SHORT:
                    return short.class;
                case CHARACTER:
                    return char.class;
                case INTEGER:
                    return int.class;
                case LONG:
                    return long.class;
                case FLOAT:
                    return float.class;
                case DOUBLE:
                    return double.class;
            }
        } else if (type instanceof ValueType.Array) {
            Class<?> componentType = getJavaType(classLoader, ((ValueType.Array) type).getItemType());
            return Array.newInstance(componentType, 0).getClass();
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return Class.forName(className, true, classLoader);
        } else if (type instanceof ValueType.Void) {
            return void.class;
        }
        throw new AssertionError("Don't know how to map type: " + type);
    }


    private Variable box(Variable var, Class<?> boxed, Class<?> primitive) {
        Program program = null; //emitter.generator.getProgram();
        BasicBlock block = program.basicBlockAt(0);

        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(boxed, "valueOf", primitive, boxed));
        insn.getArguments().add(var);
        var = program.createVariable();
        insn.setReceiver(var);

        block.getInstructions().add(insn);
        return var;
    }
}
