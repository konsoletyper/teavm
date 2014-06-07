/*
 *  Copyright 2014 Alexey Andreev.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
class ResourceTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                transformProgram(innerSource, program);
            }
        }
    }

    private void transformProgram(ClassReaderSource innerSource, Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transformBasicBlock(innerSource, program.basicBlockAt(i));
        }
    }

    private void transformBasicBlock(ClassReaderSource innerSource, BasicBlock block) {
        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction insn = instructions.get(i);
            if (insn instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction)insn;
                List<Instruction> replacement = transformInvoke(innerSource, invoke);
                if (replacement != null) {
                    instructions.set(i, new EmptyInstruction());
                    instructions.addAll(i, replacement);
                    i += replacement.size();
                }
            }
        }
    }

    private List<Instruction> transformInvoke(ClassReaderSource innerSource, InvokeInstruction insn) {
        if (insn.getType() != InvocationType.VIRTUAL) {
            return null;
        }
        MethodReference method = insn.getMethod();
        if (method.getClassName().equals(ResourceArray.class.getName()) ||
                method.getClassName().equals(ResourceMap.class.getName())) {
            InvokeInstruction accessInsn = new InvokeInstruction();
            accessInsn.setType(InvocationType.SPECIAL);
            ValueType[] types = new ValueType[method.getDescriptor().parameterCount() + 2];
            types[0] = ValueType.object("java.lang.Object");
            System.arraycopy(method.getDescriptor().getSignature(), 0, types, 1,
                    method.getDescriptor().parameterCount() + 1);
            accessInsn.setMethod(new MethodReference(ResourceAccessor.class.getName(), method.getName(), types));
            accessInsn.getArguments().add(insn.getInstance());
            accessInsn.getArguments().addAll(insn.getArguments());
            accessInsn.setReceiver(insn.getReceiver());
            return Arrays.<Instruction>asList(accessInsn);
        }
        ClassReader iface = innerSource.get(method.getClassName());
        if (iface.getAnnotations().get(Resource.class.getName()) == null) {
            return null;
        }
        if (method.getName().startsWith("get")) {
            if (method.getName().length() > 3) {
                return transformGetterInvocation(insn, getPropertyName(method.getName().substring(3)));
            }
        } else if (method.getName().startsWith("is")) {
            if (method.getName().length() > 2) {
                return transformGetterInvocation(insn, getPropertyName(method.getName().substring(2)));
            }
        } else if (method.getName().startsWith("set")) {
            if (method.getName().length() > 3) {
                return transformSetterInvocation(insn, getPropertyName(method.getName().substring(3)));
            }
        }
        return null;
    }

    private List<Instruction> transformGetterInvocation(InvokeInstruction insn, String property) {
        if (insn.getReceiver() == null) {
            return Collections.emptyList();
        }
        ValueType type = insn.getMethod().getDescriptor().getResultType();
        List<Instruction> instructions = new ArrayList<>();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    getAndCastProperty(insn, property, instructions, boolean.class);
                    return instructions;
                case BYTE:
                    getAndCastProperty(insn, property, instructions, byte.class);
                    return instructions;
                case SHORT:
                    getAndCastProperty(insn, property, instructions, short.class);
                    return instructions;
                case INTEGER:
                    getAndCastProperty(insn, property, instructions, int.class);
                    return instructions;
                case FLOAT:
                    getAndCastProperty(insn, property, instructions, float.class);
                    return instructions;
                case DOUBLE:
                    getAndCastProperty(insn, property, instructions, double.class);
                    return instructions;
                case CHARACTER:
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            switch (((ValueType.Object)type).getClassName()) {
                case "java.lang.Boolean":
                    getAndCastPropertyToWrapper(insn, property, instructions, boolean.class, Boolean.class);
                    return instructions;
                case "java.lang.Byte":
                    getAndCastPropertyToWrapper(insn, property, instructions, byte.class, Byte.class);
                    return instructions;
                case "java.lang.Short":
                    getAndCastPropertyToWrapper(insn, property, instructions, short.class, Short.class);
                    return instructions;
                case "java.lang.Integer":
                    getAndCastPropertyToWrapper(insn, property, instructions, int.class, Integer.class);
                    return instructions;
                case "java.lang.Float":
                    getAndCastPropertyToWrapper(insn, property, instructions, float.class, Float.class);
                    return instructions;
                case "java.lang.Double":
                    getAndCastPropertyToWrapper(insn, property, instructions, double.class, Double.class);
                    return instructions;
                default: {
                    Variable resultVar = insn.getProgram().createVariable();
                    getProperty(insn, property, instructions, resultVar);
                    CastInstruction castInsn = new CastInstruction();
                    castInsn.setReceiver(insn.getReceiver());
                    castInsn.setTargetType(type);
                    castInsn.setValue(resultVar);
                    instructions.add(castInsn);
                    return instructions;
                }
            }
        }
        return null;
    }

    private void getProperty(InvokeInstruction insn, String property, List<Instruction> instructions,
            Variable resultVar) {
        Program program = insn.getProgram();
        Variable nameVar = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setConstant(property);
        nameInsn.setReceiver(nameVar);
        instructions.add(nameInsn);
        InvokeInstruction accessorInvoke = new InvokeInstruction();
        accessorInvoke.setType(InvocationType.SPECIAL);
        accessorInvoke.setMethod(new MethodReference(ResourceAccessor.class, "get",
                Object.class, String.class, Object.class));
        accessorInvoke.getArguments().add(insn.getInstance());
        accessorInvoke.getArguments().add(nameVar);
        accessorInvoke.setReceiver(resultVar);
        instructions.add(accessorInvoke);
    }

    private void getAndCastProperty(InvokeInstruction insn, String property, List<Instruction> instructions,
            Class<?> primitive) {
        Program program = insn.getProgram();
        Variable resultVar = program.createVariable();
        getProperty(insn, property, instructions, resultVar);
        InvokeInstruction castInvoke = new InvokeInstruction();
        castInvoke.setType(InvocationType.SPECIAL);
        String primitiveCapitalized = primitive.getName();
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0)) +
                primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castTo" + primitiveCapitalized,
                Object.class, primitive));
        castInvoke.getArguments().add(resultVar);
        castInvoke.setReceiver(insn.getReceiver());
        instructions.add(castInvoke);
    }

    private void getAndCastPropertyToWrapper(InvokeInstruction insn, String property, List<Instruction> instructions,
            Class<?> primitive, Class<?> wrapper) {
        Program program = insn.getProgram();
        Variable resultVar = program.createVariable();
        getProperty(insn, property, instructions, resultVar);
        InvokeInstruction castInvoke = new InvokeInstruction();
        castInvoke.setType(InvocationType.SPECIAL);
        String primitiveCapitalized = primitive.getName();
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0)) +
                primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castTo" + primitiveCapitalized + "Wrapper",
                Object.class, wrapper));
        castInvoke.getArguments().add(resultVar);
        castInvoke.setReceiver(insn.getReceiver());
        instructions.add(castInvoke);
    }

    private List<Instruction> transformSetterInvocation(InvokeInstruction insn, String property) {
        return null;
    }

    private String getPropertyName(String name) {
        if (name.length() == 1) {
            return name.toLowerCase();
        }
        if (Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
