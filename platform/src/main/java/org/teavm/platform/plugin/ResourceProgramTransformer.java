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

import java.util.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

class ResourceProgramTransformer {
    private ClassReaderSource innerSource;
    private Program program;

    public ResourceProgramTransformer(ClassReaderSource innerSource, Program program) {
        this.innerSource = innerSource;
        this.program = program;
    }

    public void transformProgram() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transformBasicBlock(program.basicBlockAt(i));
        }
    }

    private void transformBasicBlock(BasicBlock block) {
        for (Instruction insn : block) {
            if (insn instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction) insn;
                List<Instruction> replacement = transformInvoke(invoke);
                if (replacement != null) {
                    insn.insertNextAll(replacement);
                    insn.delete();
                }
            }
        }
    }

    private List<Instruction> transformInvoke(InvokeInstruction insn) {
        if (insn.getType() != InvocationType.VIRTUAL) {
            return null;
        }
        MethodReference method = insn.getMethod();
        if (method.getClassName().equals(ResourceArray.class.getName())
                || method.getClassName().equals(ResourceMap.class.getName())) {
            if (method.getName().equals("keys")) {
                return transformKeys(insn);
            }
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
            return Arrays.asList(accessInsn);
        }
        ClassReader iface = innerSource.get(method.getClassName());
        if (iface == null || !innerSource.isSuperType(Resource.class.getName(), iface.getName()).orElse(false)) {
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

    private List<Instruction> transformKeys(InvokeInstruction insn) {
        Variable tmp = program.createVariable();

        InvokeInstruction keysInsn = new InvokeInstruction();
        keysInsn.setType(InvocationType.SPECIAL);
        keysInsn.setMethod(new MethodReference(ResourceAccessor.class, "keys", Object.class, Object.class));
        keysInsn.getArguments().add(insn.getInstance());
        keysInsn.setReceiver(tmp);

        InvokeInstruction transformInsn = new InvokeInstruction();
        transformInsn.setType(InvocationType.SPECIAL);
        transformInsn.setMethod(new MethodReference(ResourceAccessor.class, "keysToStrings",
                Object.class, String[].class));
        transformInsn.getArguments().add(tmp);
        transformInsn.setReceiver(insn.getReceiver());

        return Arrays.asList(keysInsn, transformInsn);
    }

    private List<Instruction> transformGetterInvocation(InvokeInstruction insn, String property) {
        if (insn.getReceiver() == null) {
            return Collections.emptyList();
        }
        ValueType type = insn.getMethod().getDescriptor().getResultType();
        List<Instruction> instructions = new ArrayList<>();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
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
            switch (((ValueType.Object) type).getClassName()) {
                case "java.lang.String": {
                    Variable resultVar = insn.getProgram().createVariable();
                    getProperty(insn, property, instructions, resultVar);
                    InvokeInstruction castInvoke = new InvokeInstruction();
                    castInvoke.setType(InvocationType.SPECIAL);
                    castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castToString",
                            Object.class, String.class));
                    castInvoke.getArguments().add(resultVar);
                    castInvoke.setReceiver(insn.getReceiver());
                    instructions.add(castInvoke);
                    return instructions;
                }
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
        Variable nameVar = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setConstant(property);
        nameInsn.setReceiver(nameVar);
        instructions.add(nameInsn);
        InvokeInstruction accessorInvoke = new InvokeInstruction();
        accessorInvoke.setType(InvocationType.SPECIAL);
        accessorInvoke.setMethod(new MethodReference(ResourceAccessor.class, "getProperty",
                Object.class, String.class, Object.class));
        accessorInvoke.getArguments().add(insn.getInstance());
        accessorInvoke.getArguments().add(nameVar);
        accessorInvoke.setReceiver(resultVar);
        instructions.add(accessorInvoke);
    }

    private void getAndCastProperty(InvokeInstruction insn, String property, List<Instruction> instructions,
            Class<?> primitive) {
        Variable resultVar = program.createVariable();
        getProperty(insn, property, instructions, resultVar);
        InvokeInstruction castInvoke = new InvokeInstruction();
        castInvoke.setType(InvocationType.SPECIAL);
        String primitiveCapitalized = primitive.getName();
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0))
                + primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castTo" + primitiveCapitalized,
                Object.class, primitive));
        castInvoke.getArguments().add(resultVar);
        castInvoke.setReceiver(insn.getReceiver());
        instructions.add(castInvoke);
    }

    private List<Instruction> transformSetterInvocation(InvokeInstruction insn, String property) {
        ValueType type = insn.getMethod().getDescriptor().parameterType(0);
        List<Instruction> instructions = new ArrayList<>();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    castAndSetProperty(insn, property, instructions, boolean.class);
                    return instructions;
                case BYTE:
                    castAndSetProperty(insn, property, instructions, byte.class);
                    return instructions;
                case SHORT:
                    castAndSetProperty(insn, property, instructions, short.class);
                    return instructions;
                case INTEGER:
                    castAndSetProperty(insn, property, instructions, int.class);
                    return instructions;
                case FLOAT:
                    castAndSetProperty(insn, property, instructions, float.class);
                    return instructions;
                case DOUBLE:
                    castAndSetProperty(insn, property, instructions, double.class);
                    return instructions;
                case CHARACTER:
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            switch (((ValueType.Object) type).getClassName()) {
                case "java.lang.String": {
                    Variable castVar = insn.getProgram().createVariable();
                    InvokeInstruction castInvoke = new InvokeInstruction();
                    castInvoke.setType(InvocationType.SPECIAL);
                    castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castFromString",
                            String.class, Object.class));
                    castInvoke.getArguments().add(insn.getArguments().get(0));
                    castInvoke.setReceiver(castVar);
                    instructions.add(castInvoke);
                    setProperty(insn, property, instructions, castVar);
                    return instructions;
                }
                default: {
                    setProperty(insn, property, instructions, insn.getArguments().get(0));
                    return instructions;
                }
            }
        }
        return null;
    }

    private void setProperty(InvokeInstruction insn, String property, List<Instruction> instructions,
            Variable valueVar) {
        Variable nameVar = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setConstant(property);
        nameInsn.setReceiver(nameVar);
        instructions.add(nameInsn);
        InvokeInstruction accessorInvoke = new InvokeInstruction();
        accessorInvoke.setType(InvocationType.SPECIAL);
        accessorInvoke.setMethod(new MethodReference(ResourceAccessor.class, "put",
                Object.class, String.class, Object.class, void.class));
        accessorInvoke.getArguments().add(insn.getInstance());
        accessorInvoke.getArguments().add(nameVar);
        accessorInvoke.getArguments().add(valueVar);
        instructions.add(accessorInvoke);
    }

    private void castAndSetProperty(InvokeInstruction insn, String property, List<Instruction> instructions,
            Class<?> primitive) {
        Variable castVar = program.createVariable();
        InvokeInstruction castInvoke = new InvokeInstruction();
        castInvoke.setType(InvocationType.SPECIAL);
        String primitiveCapitalized = primitive.getName();
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0))
                + primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castFrom" + primitiveCapitalized,
                primitive, Object.class));
        castInvoke.getArguments().add(insn.getArguments().get(0));
        castInvoke.setReceiver(castVar);
        instructions.add(castInvoke);
        setProperty(insn, property, instructions, castVar);
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
