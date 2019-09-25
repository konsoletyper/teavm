/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.model.AnnotationReader;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.runtime.Fiber;

public class AsyncLowLevelDependencyListener extends AbstractDependencyListener {
    private Set<String> generatedClassNames = new HashSet<>();

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod() != null && method.getMethod().getAnnotations().get(Async.class.getName()) != null) {
            ClassHolder cls = generateCall(method.getMethod());
            if (cls != null) {
                agent.submitClass(cls);
            }
        }
    }

    private ClassHolder generateCall(MethodReader method) {
        ClassHolder cls = generateClassDecl(method);
        if (cls == null) {
            return null;
        }
        cls.addMethod(generateConstructor(method, cls.getName()));
        cls.addMethod(generateRun(method, cls.getName()));
        return cls;
    }

    private ClassHolder generateClassDecl(MethodReader method) {
        AnnotationReader annot = method.getAnnotations().get(AsyncCallClass.class.getName());
        String className = annot.getValue("value").getString();
        if (!generatedClassNames.add(className)) {
            return null;
        }
        ClassHolder cls = new ClassHolder(className);

        cls.getInterfaces().add(Fiber.class.getName() + "$AsyncCall");

        List<ValueType> types = new ArrayList<>();
        if (!method.hasModifier(ElementModifier.STATIC)) {
            types.add(ValueType.object(method.getOwnerName()));
            FieldHolder field = new FieldHolder("instance");
            field.setType(ValueType.object(method.getOwnerName()));
            cls.addField(field);
        }
        ValueType[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; ++i) {
            types.add(parameterTypes[i]);
            FieldHolder field = new FieldHolder("param" + i);
            field.setType(parameterTypes[i]);
            cls.addField(field);
        }
        types.add(ValueType.VOID);
        MethodHolder constructor = new MethodHolder("<init>", types.toArray(new ValueType[0]));
        cls.addMethod(constructor);
        return cls;
    }

    private MethodHolder generateConstructor(MethodReader method, String className) {
        List<ValueType> types = new ArrayList<>();
        if (!method.hasModifier(ElementModifier.STATIC)) {
            types.add(ValueType.object(method.getOwnerName()));
        }
        Collections.addAll(types, method.getParameterTypes());
        types.add(ValueType.VOID);
        MethodHolder constructor = new MethodHolder("<init>", types.toArray(new ValueType[0]));

        Program program = new Program();
        constructor.setProgram(program);
        BasicBlock block = program.createBasicBlock();
        Variable instance = program.createVariable();

        if (!method.hasModifier(ElementModifier.STATIC)) {
            PutFieldInstruction putField = new PutFieldInstruction();
            putField.setValue(program.createVariable());
            putField.setField(new FieldReference(className, "instance"));
            putField.setFieldType(ValueType.object(method.getOwnerName()));
            putField.setInstance(instance);
            block.add(putField);
        }

        for (int i = 0; i < method.parameterCount(); ++i) {
            PutFieldInstruction putField = new PutFieldInstruction();
            putField.setValue(program.createVariable());
            putField.setField(new FieldReference(className, "param" + i));
            putField.setFieldType(method.parameterType(i));
            putField.setInstance(instance);
            block.add(putField);
        }

        block.add(new ExitInstruction());
        return constructor;
    }

    private MethodHolder generateRun(MethodReader method, String className) {
        MethodHolder runMethod = new MethodHolder("run", ValueType.parse(AsyncCallback.class), ValueType.VOID);
        Program program = new Program();
        runMethod.setProgram(program);
        BasicBlock block = program.createBasicBlock();
        Variable instance = program.createVariable();
        Variable callback = program.createVariable();

        InvokeInstruction call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        List<ValueType> types = new ArrayList<>();
        ValueType[] parameterTypes = method.getParameterTypes();
        List<Variable> arguments = new ArrayList<>(call.getArguments());

        if (!method.hasModifier(ElementModifier.STATIC)) {
            GetFieldInstruction getField = new GetFieldInstruction();
            getField.setReceiver(program.createVariable());
            getField.setInstance(instance);
            getField.setField(new FieldReference(className, "instance"));
            getField.setFieldType(ValueType.object(method.getOwnerName()));
            block.add(getField);
            call.setInstance(getField.getReceiver());
        }
        for (int i = 0; i < parameterTypes.length; ++i) {
            GetFieldInstruction getField = new GetFieldInstruction();
            getField.setReceiver(program.createVariable());
            getField.setInstance(instance);
            getField.setField(new FieldReference(className, "param" + i));
            getField.setFieldType(parameterTypes[i]);
            block.add(getField);
            arguments.add(getField.getReceiver());
            types.add(parameterTypes[i]);
        }

        types.add(ValueType.parse(AsyncCallback.class));
        arguments.add(callback);

        types.add(ValueType.VOID);
        call.setMethod(new MethodReference(method.getOwnerName(), method.getName(), types.toArray(new ValueType[0])));
        call.setArguments(arguments.toArray(new Variable[0]));
        block.add(call);

        if (method.getResultType() == ValueType.VOID) {
            block.add(new ExitInstruction());
        } else {
            Variable result = program.createVariable();
            call.setReceiver(result);
            ExitInstruction exit = new ExitInstruction();
            exit.setValueToReturn(castToObject(block, result, method.getResultType()));
            block.add(exit);
        }

        return runMethod;
    }

    private Variable castToObject(BasicBlock block, Variable value, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            InvokeInstruction invoke = new InvokeInstruction();
            invoke.setType(InvocationType.SPECIAL);
            invoke.setArguments(value);
            invoke.setReceiver(block.getProgram().createVariable());
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    invoke.setMethod(new MethodReference(Boolean.class, "valueOf", boolean.class, Boolean.class));
                    break;
                case BYTE:
                    invoke.setMethod(new MethodReference(Byte.class, "valueOf", byte.class, Byte.class));
                    break;
                case SHORT:
                    invoke.setMethod(new MethodReference(Short.class, "valueOf", short.class, Short.class));
                    break;
                case CHARACTER:
                    invoke.setMethod(new MethodReference(Character.class, "valueOf", char.class, Character.class));
                    break;
                case INTEGER:
                    invoke.setMethod(new MethodReference(Integer.class, "valueOf", int.class, Integer.class));
                    break;
                case LONG:
                    invoke.setMethod(new MethodReference(Long.class, "valueOf", long.class, Long.class));
                    break;
                case FLOAT:
                    invoke.setMethod(new MethodReference(Float.class, "valueOf", float.class, Float.class));
                    break;
                case DOUBLE:
                    invoke.setMethod(new MethodReference(Double.class, "valueOf", double.class, Double.class));
                    break;
            }

            block.add(invoke);
            return invoke.getReceiver();
        }

        return value;
    }
}
