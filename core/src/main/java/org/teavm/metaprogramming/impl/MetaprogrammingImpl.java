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

import org.teavm.dependency.DependencyAgent;
import org.teavm.metaprogramming.Action;
import org.teavm.metaprogramming.Computation;
import org.teavm.metaprogramming.Diagnostics;
import org.teavm.metaprogramming.InvocationHandler;
import org.teavm.metaprogramming.LazyComputation;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.SourceLocation;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.impl.reflect.ReflectClassImpl;
import org.teavm.metaprogramming.impl.reflect.ReflectContext;
import org.teavm.metaprogramming.impl.reflect.ReflectFieldImpl;
import org.teavm.metaprogramming.impl.reflect.ReflectMethodImpl;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.InstructionLocation;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public final class MetaprogrammingImpl {
    static ClassLoader classLoader;
    static ClassReaderSource classSource;
    static ReflectContext reflectContext;
    static DependencyAgent agent;
    static VariableContext varContext;
    static MethodReference templateMethod;
    static CompositeMethodGenerator generator;
    static ValueType returnType;

    private MetaprogrammingImpl() {
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> Value<T> emit(Computation<T> computation) {
        if (computation instanceof ValueImpl<?>) {
            @SuppressWarnings("unchecked")
            ValueImpl<T> valueImpl = (ValueImpl<T>) computation;
            Variable var = varContext.emitVariable(valueImpl, new CallLocation(templateMethod, generator.location));
            return new ValueImpl<>(var, varContext, valueImpl.type);
        } else if (computation instanceof LazyValueImpl<?>) {
            LazyValueImpl<?> valueImpl = (LazyValueImpl<?>) computation;
            Variable var = generator.lazy(valueImpl);
            return var != null ? new ValueImpl<>(var, varContext, valueImpl.type) : null;
        } else {
            Fragment fragment = (Fragment) computation;
            MethodReader method = classSource.resolve(fragment.method);
            generator.addProgram(method.getProgram(), fragment.capturedValues);
            return new ValueImpl<>(generator.getResultVar(), varContext, fragment.method.getReturnType());
        }
    }

    public static void emit(Action action) {
        Fragment fragment = (Fragment) action;
        MethodReader method = classSource.resolve(fragment.method);
        generator.addProgram(method.getProgram(), fragment.capturedValues);
    }

    public static <T> Value<T> lazyFragment(LazyComputation<T> computation) {
        return lazyFragment(ValueType.object("java.lang.Object"), computation);
    }

    public static <T> Value<T> lazy(Computation<T> computation) {
        Fragment fragment = (Fragment) computation;
        return lazyFragment(fragment.method.getReturnType(), () -> emit(computation));
    }

    private static <T> Value<T> lazyFragment(ValueType type, LazyComputation<T> computation) {
        return new LazyValueImpl<>(varContext, computation, type, generator.forcedLocation);
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void exit(Computation<?> value) {
        if (value == null) {
            returnValue(null);
            return;
        }

        if (value instanceof Fragment) {
            Fragment fragment = (Fragment) value;
            MethodReader method = classSource.resolve(fragment.method);
            generator.addProgram(method.getProgram(), fragment.capturedValues);
            generator.blockIndex = generator.returnBlockIndex;

            returnValue(unbox(generator.getResultVar()));
        } else {
            throw new IllegalStateException("Unexpected computation type: " + value.getClass().getName());
        }
    }

    private static Variable unbox(Variable var) {
        if (returnType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) returnType).getKind()) {
                case BOOLEAN:
                    var = unbox(var, Boolean.class, boolean.class);
                    break;
                case BYTE:
                    var = unbox(var, Byte.class, byte.class);
                    break;
                case SHORT:
                    var = unbox(var, Short.class, short.class);
                    break;
                case CHARACTER:
                    var = unbox(var, Character.class, char.class);
                    break;
                case INTEGER:
                    var = unbox(var, Integer.class, int.class);
                    break;
                case LONG:
                    var = unbox(var, Long.class, long.class);
                    break;
                case FLOAT:
                    var = unbox(var, Float.class, float.class);
                    break;
                case DOUBLE:
                    var = unbox(var, Double.class, double.class);
                    break;
            }
        }
        return var;
    }

    private static Variable unbox(Variable var, Class<?> boxed, Class<?> primitive) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setInstance(var);
        insn.setType(InvocationType.VIRTUAL);
        insn.setMethod(new MethodReference(boxed, primitive.getName() + "Value", primitive));
        var = generator.program.createVariable();
        insn.setReceiver(var);
        generator.add(insn);
        return var;
    }

    public static void exit() {
        exit(null);
    }

    public static void location(String fileName, int lineNumber) {
        unsupported();
    }

    public static void defaultLocation() {
        unsupported();
    }

    @SuppressWarnings("WeakerAccess")
    public static ReflectClass<?> findClass(String name) {
        return reflectContext.findClass(name);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> ReflectClass<T> findClass(Class<T> cls) {
        return reflectContext.findClass(cls);
    }

    static ReflectClass<?> findClass(ValueType type) {
        return reflectContext.getClass(type);
    }

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReflectClass<T[]> arrayClass(ReflectClass<T> componentType) {
        ReflectClassImpl<T> componentTypeImpl = (ReflectClassImpl<T>) componentType;
        return (ReflectClassImpl<T[]>) reflectContext.getClass(ValueType.arrayOf(componentTypeImpl.type));
    }

    public static ReflectClass<?> createClass(byte[] bytecode) {
        return findClass(agent.submitClassFile(bytecode).replace('/', '.'));
    }

    public static <T> Value<T> proxy(Class<T> type, InvocationHandler<T> handler)  {
        return proxy(findClass(type), handler);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> Value<T> proxy(ReflectClass<T> type, InvocationHandler<T> handler) {
        unsupported();
        return null;
    }

    private static void returnValue(Variable var) {
        ExitInstruction insn = new ExitInstruction();
        insn.setValueToReturn(var);
        generator.add(insn);
    }

    public static Diagnostics getDiagnostics() {
        return diagnostics;
    }

    public void submitClass(ClassHolder cls) {
        agent.submitClass(cls);
    }

    private static void unsupported() {
        throw new UnsupportedOperationException("This operation is only supported from TeaVM compile-time "
                + "environment");
    }

    private static Diagnostics diagnostics = new Diagnostics() {
        @Override
        public void error(SourceLocation location, String error, Object... params) {
            convertParams(params);
            agent.getDiagnostics().error(convertLocation(location), error, params);
        }

        @Override
        public void warning(SourceLocation location, String error, Object... params) {
            convertParams(params);
            agent.getDiagnostics().warning(convertLocation(location), error, params);
        }

        private void convertParams(Object[] params) {
            for (int i = 0; i < params.length; ++i) {
                if (params[i] instanceof ReflectMethodImpl) {
                    params[i] = ((ReflectMethodImpl) params[i]).method.getReference();
                } else if (params[i] instanceof ReflectClassImpl) {
                    params[i] = ((ReflectClassImpl<?>) params[i]).type;
                } else if (params[i] instanceof ReflectFieldImpl) {
                    params[i] = ((ReflectFieldImpl) params[i]).field.getReference();
                } else if (params[i] instanceof Class<?>) {
                    params[i] = ValueType.parse((Class<?>) params[i]);
                }
            }
        }

        private CallLocation convertLocation(SourceLocation location) {
            MethodReader method = ((ReflectMethodImpl) location.getMethod()).method;
            return location.getFileName() != null
                    ? new CallLocation(method.getReference(),
                            new InstructionLocation(location.getFileName(), location.getLineNumber()))
                    : new CallLocation(method.getReference());
        }
    };
}
