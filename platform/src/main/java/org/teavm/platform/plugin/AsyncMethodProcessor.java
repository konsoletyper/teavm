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
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.runtime.Fiber;

public class AsyncMethodProcessor implements ClassHolderTransformer {
    private boolean lowLevel;

    public AsyncMethodProcessor(boolean lowLevel) {
        this.lowLevel = lowLevel;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        int suffix = 0;
        for (MethodHolder method : cls.getMethods()) {
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
                    generateLowLevelCall(method, suffix++);
                }
            }
        }
    }

    private void generateLowLevelCall(MethodHolder method, int suffix) {
        String className = method.getOwnerName() + "$" + method.getName() + "$" + suffix;
        AnnotationHolder classNameAnnot = new AnnotationHolder(AsyncCallClass.class.getName());
        classNameAnnot.getValues().put("value", new AnnotationValue(className));
        method.getAnnotations().add(classNameAnnot);

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
}
