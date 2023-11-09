/*
 *  Copyright 2015 Alexey Andreev.
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
import java.util.List;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.runtime.Fiber;

public class AsyncMethodProcessor implements ClassHolderTransformer {
    private boolean lowLevel;

    public AsyncMethodProcessor(boolean lowLevel) {
        this.lowLevel = lowLevel;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        int suffix = 0;
        for (var method : List.copyOf(cls.getMethods())) {
            if (method.hasModifier(ElementModifier.NATIVE)
                    && method.getAnnotations().get(Async.class.getName()) != null
                    && method.getAnnotations().get(GeneratedBy.class.getName()) == null) {
                ValueType[] signature = new ValueType[method.parameterCount() + 2];
                for (int i = 0; i < method.parameterCount(); ++i) {
                    signature[i] = method.parameterType(i);
                }
                signature[method.parameterCount()] = ValueType.parse(AsyncCallback.class);
                signature[method.parameterCount() + 1] = ValueType.VOID;
                MethodDescriptor asyncDesc = new MethodDescriptor(method.getName(), signature);
                MethodHolder asyncMethod = cls.getMethod(asyncDesc);
                if (asyncMethod != null) {
                    if (asyncMethod.hasModifier(ElementModifier.STATIC)
                            != method.hasModifier(ElementModifier.STATIC)) {
                        context.getDiagnostics().error(new CallLocation(method.getReference()),
                                "Methods {{m0}} and {{m1}} must both be either static or non-static",
                                method.getReference(), asyncMethod.getReference());
                    }
                }

                if (lowLevel) {
                    generateLowLevelCall(method, suffix++, context);
                } else {
                    generateCallerMethod(cls, method);
                }
            }
        }
    }

    private void generateLowLevelCall(MethodHolder method, int suffix, ClassHolderTransformerContext context) {
        String className = method.getOwnerName() + "$" + method.getName() + "$" + suffix;
        context.submit(generateCall(method, className));

        method.getModifiers().remove(ElementModifier.NATIVE);

        Program program = new Program();
        method.setProgram(program);

        BasicBlock startBlock = program.createBasicBlock();
        BasicBlock block = program.createBasicBlock();

        JumpInstruction jumpToBlock = new JumpInstruction();
        jumpToBlock.setTarget(block);
        startBlock.add(jumpToBlock);

        InvokeInstruction constructorInvocation = new InvokeInstruction();
        constructorInvocation.setType(InvocationType.SPECIAL);
        List<ValueType> signature = new ArrayList<>();

        Variable instanceVar = program.createVariable();
        List<Variable> arguments = new ArrayList<>(constructorInvocation.getArguments());
        if (!method.hasModifier(ElementModifier.STATIC)) {
            arguments.add(instanceVar);
            signature.add(ValueType.object(method.getOwnerName()));
        }
        for (int i = 0; i < method.parameterCount(); ++i) {
            arguments.add(program.createVariable());
            signature.add(method.parameterType(i));
        }
        signature.add(ValueType.VOID);

        ConstructInstruction newInstruction = new ConstructInstruction();
        newInstruction.setReceiver(program.createVariable());
        newInstruction.setType(className);
        block.add(newInstruction);

        constructorInvocation.setInstance(newInstruction.getReceiver());
        constructorInvocation.setMethod(new MethodReference(className, "<init>", signature.toArray(new ValueType[0])));
        constructorInvocation.setArguments(arguments.toArray(new Variable[0]));
        block.add(constructorInvocation);

        InvokeInstruction suspendInvocation = new InvokeInstruction();
        suspendInvocation.setType(InvocationType.SPECIAL);
        suspendInvocation.setMethod(new MethodReference(Fiber.class, "suspend", Fiber.AsyncCall.class, Object.class));
        suspendInvocation.setArguments(newInstruction.getReceiver());
        suspendInvocation.setReceiver(program.createVariable());
        block.add(suspendInvocation);

        Variable result = suspendInvocation.getReceiver();
        ExitInstruction exitInstruction = new ExitInstruction();
        ValueType returnType = method.getResultType();
        if (returnType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) returnType).getKind()) {
                case BOOLEAN:
                    result = castPrimitive(block, result, "Boolean", returnType);
                    break;
                case BYTE:
                    result = castPrimitive(block, result, "Byte", returnType);
                    break;
                case SHORT:
                    result = castPrimitive(block, result, "Short", returnType);
                    break;
                case CHARACTER:
                    result = castPrimitive(block, result, "Char", returnType);
                    break;
                case INTEGER:
                    result = castPrimitive(block, result, "Int", returnType);
                    break;
                case FLOAT:
                    result = castPrimitive(block, result, "Float", returnType);
                    break;
                case LONG:
                    result = castPrimitive(block, result, "Long", returnType);
                    break;
                case DOUBLE:
                    result = castPrimitive(block, result, "Double", returnType);
                    break;
            }
        } else if (returnType == ValueType.VOID) {
            result = null;
        } else {
            CastInstruction cast = new CastInstruction();
            cast.setValue(result);
            cast.setTargetType(returnType);
            cast.setReceiver(program.createVariable());
            block.add(cast);
            result = cast.getReceiver();
        }

        exitInstruction.setValueToReturn(result);
        block.add(exitInstruction);
    }

    private Variable castPrimitive(BasicBlock block, Variable value, String name, ValueType type) {
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(Fiber.class.getName(), "get" + name,
                ValueType.object("java.lang.Object"), type));
        invoke.setArguments(value);
        invoke.setReceiver(block.getProgram().createVariable());
        block.add(invoke);
        return invoke.getReceiver();
    }

    private void generateCallerMethod(ClassHolder cls, MethodHolder method) {
        method.getAnnotations().remove(Async.class.getName());

        var mappedSignature = method.getSignature();
        mappedSignature[mappedSignature.length - 1] = ValueType.object("java.lang.Object");
        var callerMethod = new MethodHolder(method.getName() + "$_asyncCall_$", mappedSignature);
        var annot = new AnnotationHolder(AsyncCaller.class.getName());
        annot.getValues().put("value", new AnnotationValue(getAsyncReference(method.getReference()).toString()));
        callerMethod.getAnnotations().add(annot);
        callerMethod.getAnnotations().add(new AnnotationHolder(Async.class.getName()));
        callerMethod.getModifiers().add(ElementModifier.NATIVE);
        cls.addMethod(callerMethod);

        method.getModifiers().remove(ElementModifier.NATIVE);
        var program = new Program();
        var block = program.createBasicBlock();
        var thisVar = program.createVariable();
        var call = new InvokeInstruction();
        call.setMethod(callerMethod.getReference());
        call.setType(InvocationType.SPECIAL);
        if (!method.hasModifier(ElementModifier.STATIC)) {
            call.setInstance(thisVar);
        } else {
            callerMethod.getModifiers().add(ElementModifier.STATIC);
        }
        var args = new Variable[method.parameterCount()];
        for (var i = 0; i < method.parameterCount(); ++i) {
            args[i] = program.createVariable();
        }
        call.setArguments(args);
        block.add(call);

        var exit = new ExitInstruction();
        var returnType = method.getResultType();
        if (returnType instanceof ValueType.Primitive) {
            call.setReceiver(program.createVariable());
            exit.setValueToReturn(unbox(call.getReceiver(), ((ValueType.Primitive) returnType).getKind(),
                    block, program));
        } else if (!(returnType instanceof ValueType.Void)) {
            call.setReceiver(program.createVariable());
            var cast = new CastInstruction();
            cast.setValue(call.getReceiver());
            cast.setTargetType(returnType);
            cast.setReceiver(program.createVariable());
            block.add(cast);
            exit.setValueToReturn(cast.getReceiver());
        }

        block.add(exit);

        method.setProgram(program);
    }

    private MethodReference getAsyncReference(MethodReference methodRef) {
        var signature = new ValueType[methodRef.parameterCount() + 2];
        for (int i = 0; i < methodRef.parameterCount(); ++i) {
            signature[i] = methodRef.getDescriptor().parameterType(i);
        }
        signature[methodRef.parameterCount()] = ValueType.parse(AsyncCallback.class);
        signature[methodRef.parameterCount() + 1] = ValueType.VOID;
        return new MethodReference(methodRef.getClassName(), methodRef.getName(), signature);
    }

    private Variable unbox(Variable value, PrimitiveType type, BasicBlock block, Program program) {
        var cast = new CastInstruction();
        cast.setValue(value);
        cast.setReceiver(program.createVariable());
        block.add(cast);

        var call = new InvokeInstruction();
        call.setInstance(cast.getReceiver());
        call.setReceiver(program.createVariable());
        call.setType(InvocationType.VIRTUAL);
        block.add(call);

        switch (type) {
            case BOOLEAN:
                call.setMethod(new MethodReference(Boolean.class, "booleanValue", boolean.class));
                break;
            case BYTE:
                call.setMethod(new MethodReference(Byte.class, "byteValue", boolean.class));
                break;
            case SHORT:
                call.setMethod(new MethodReference(Short.class, "shortValue", short.class));
                break;
            case CHARACTER:
                call.setMethod(new MethodReference(Character.class, "charValue", char.class));
                break;
            case INTEGER:
                call.setMethod(new MethodReference(Integer.class, "intValue", int.class));
                break;
            case LONG:
                call.setMethod(new MethodReference(Long.class, "longValue", int.class));
                break;
            case FLOAT:
                call.setMethod(new MethodReference(Float.class, "floatValue", int.class));
                break;
            case DOUBLE:
                call.setMethod(new MethodReference(Double.class, "doubleValue", int.class));
                break;
        }

        cast.setTargetType(ValueType.object(call.getMethod().getClassName()));
        return call.getReceiver();
    }

    private ClassHolder generateCall(MethodReader method, String className) {
        ClassHolder cls = generateClassDecl(method, className);
        if (cls == null) {
            return null;
        }
        cls.addMethod(generateConstructor(method, cls.getName()));
        cls.addMethod(generateRun(method, cls.getName()));
        return cls;
    }

    private ClassHolder generateClassDecl(MethodReader method, String className) {
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

        block.add(new ExitInstruction());

        return runMethod;
    }
}
