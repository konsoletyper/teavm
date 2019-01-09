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
import org.teavm.dependency.DependencyAgent;
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

class UsageGenerator {
    private int suffix;
    private int ownSuffix;
    private DependencyAgent agent;
    private MethodModel model;
    private MethodDependency methodDep;
    private CallLocation location;
    private Diagnostics diagnostics;
    private Method proxyMethod;
    private MetaprogrammingClassLoader classLoader;
    private boolean annotationErrorReported;
    private MethodDependency nameDependency;

    UsageGenerator(DependencyAgent agent, MethodModel model, MethodDependency methodDep,
            MetaprogrammingClassLoader classLoader, int suffix) {
        this.agent = agent;
        this.diagnostics = agent.getDiagnostics();
        this.model = model;
        this.methodDep = methodDep;
        this.location = new CallLocation(methodDep.getReference());
        this.classLoader = classLoader;
        this.suffix = suffix;
    }

    void installProxyEmitter() {
        Diagnostics diagnostics = agent.getDiagnostics();

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

        nameDependency = installAdditionalDependencies();

        if (model.getClassParameterIndex() >= 0) {
            int index = 1 + model.getClassParameterIndex();
            methodDep.getVariable(index).getClassValueNode().addConsumer(
                    type -> emitPermutation(findClass(type.getName())));
        } else {
            emitPermutation(null);
        }
    }

    private MethodDependency installAdditionalDependencies() {
        MethodDependency nameDep = agent.linkMethod(new MethodReference(Class.class, "getName", String.class));
        nameDep.addLocation(location);
        nameDep.getVariable(0).propagate(agent.getType(Class.class.getName()));
        nameDep.getThrown().connect(methodDep.getThrown());
        nameDep.use();

        MethodDependency equalsDep = agent.linkMethod(new MethodReference(String.class, "equals", Object.class,
                boolean.class));
        equalsDep.addLocation(location);
        nameDep.getResult().connect(equalsDep.getVariable(0));
        equalsDep.getVariable(1).propagate(agent.getType("java.lang.String"));
        equalsDep.getThrown().connect(methodDep.getThrown());
        equalsDep.use();

        MethodDependency hashCodeDep = agent.linkMethod(new MethodReference(String.class, "hashCode", int.class));
        hashCodeDep.addLocation(location);
        hashCodeDep.getVariable(0).propagate(agent.getType("java.lang.String"));
        nameDep.getResult().connect(hashCodeDep.getVariable(0));
        hashCodeDep.getThrown().connect(methodDep.getThrown());
        hashCodeDep.use();

        agent.linkMethod(new MethodReference(Object.class, "hashCode", int.class));
        agent.linkMethod(new MethodReference(Object.class, "equals", Object.class, boolean.class));

        return nameDep;
    }

    private void emitPermutation(ValueType type) {
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

        String suffix = getSuffix();
        implRef = buildMethodReference(suffix);
        MetaprogrammingImpl.templateMethod = model.getMetaMethod();
        VariableContext varContext = new TopLevelVariableContext(diagnostics);
        MetaprogrammingImpl.generator = new CompositeMethodGenerator(varContext);
        MetaprogrammingImpl.varContext = varContext;
        MetaprogrammingImpl.returnType = model.getMethod().getReturnType();
        MetaprogrammingImpl.generator.location = location != null ? location.getSourceLocation() : null;
        MetaprogrammingImpl.proxySuffixGenerators.clear();
        MetaprogrammingImpl.suffix = suffix;

        for (int i = 0; i <= model.getMetaParameterCount(); ++i) {
            MetaprogrammingImpl.generator.getProgram().createVariable();
        }

        Object[] proxyArgs = new Object[model.getMetaParameterCount()];
        for (int i = 0; i < proxyArgs.length; ++i) {
            if (i == model.getMetaClassParameterIndex()) {
                proxyArgs[i] = MetaprogrammingImpl.findClass(type);
            } else {
                proxyArgs[i] = new ValueImpl<>(getParameterVar(i), MetaprogrammingImpl.varContext,
                        model.getMetaParameterType(i));
            }
        }

        MetaprogrammingImpl.unsupportedCase = false;

        try {
            proxyMethod.invoke(null, proxyArgs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            diagnostics.error(location, "Error calling proxy method {{m0}}: " + writer.toString(),
                    model.getMetaMethod());
        }

        MetaprogrammingImpl.close();
        if (MetaprogrammingImpl.unsupportedCase) {
            return;
        }

        model.getUsages().put(type, implRef);
        Program program = MetaprogrammingImpl.generator.getProgram();
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
        agent.getIncrementalCache().setNoCache(cls.getName());

        MethodDependency implMethod = agent.linkMethod(implRef);
        implMethod.addLocation(location);
        for (int i = 0; i < implRef.parameterCount(); ++i) {
            methodDep.getVariable(i + 1).connect(implMethod.getVariable(i + 1));
        }

        if (model.getClassParameterIndex() >= 0) {
            implMethod.getVariable(model.getClassParameterIndex() + 1).getClassValueNode()
                    .connect(nameDependency.getVariable(0));
        }

        if (implMethod.getResult() != null) {
            implMethod.getResult().connect(methodDep.getResult());
        }
        implMethod.getThrown().connect(methodDep.getThrown());
        implMethod.use();

        agent.linkClass(implRef.getClassName());
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

    private MethodReference buildMethodReference(String suffix) {
        if (model.getClassParameterIndex() < 0) {
            return new MethodReference(model.getMethod().getClassName() + "$PROXY$" + suffix,
                    model.getMethod().getDescriptor());
        }

        int i;
        ValueType[] signature = new ValueType[model.getMetaParameterCount() + 1];
        for (i = 0; i < signature.length - 1; ++i) {
            signature[i] = model.getMetaParameterType(i);
        }
        signature[i] = model.getMethod().getReturnType();

        return new MethodReference(model.getMethod().getClassName() + "$PROXY$" + suffix,
                model.getMethod().getName(), signature);
    }

    private String getSuffix() {
        return suffix + "_" + ownSuffix++;
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

    private Variable getParameterVar(int index) {
        Program program = MetaprogrammingImpl.generator.getProgram();
        Variable var = program.variableAt(index + 1);
        ValueType type = model.getMethod().parameterType(index);
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    var = box(var, Boolean.class, boolean.class);
                    break;
                case BYTE:
                    var = box(var, Byte.class, byte.class);
                    break;
                case SHORT:
                    var = box(var, Short.class, short.class);
                    break;
                case CHARACTER:
                    var = box(var, Character.class, char.class);
                    break;
                case INTEGER:
                    var = box(var, Integer.class, int.class);
                    break;
                case LONG:
                    var = box(var, Long.class, long.class);
                    break;
                case FLOAT:
                    var = box(var, Float.class, float.class);
                    break;
                case DOUBLE:
                    var = box(var, Double.class, double.class);
                    break;
            }
        }
        return var;
    }

    private Variable box(Variable var, Class<?> boxed, Class<?> primitive) {
        Program program = MetaprogrammingImpl.generator.getProgram();
        BasicBlock block = program.basicBlockAt(0);

        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(boxed, "valueOf", primitive, boxed));
        insn.setArguments(var);
        var = program.createVariable();
        insn.setReceiver(var);

        block.add(insn);
        return var;
    }
}
