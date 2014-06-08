package org.teavm.platform.plugin;

import java.util.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
class ResourceProgramTransformer {
    private ClassReaderSource innerSource;
    private Program program;
    private Set<Variable> arrayItemsVars = new HashSet<>();

    public ResourceProgramTransformer(ClassReaderSource innerSource, Program program) {
        this.innerSource = innerSource;
        this.program = program;
    }

    public void transformProgram() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transformBasicBlock(program.basicBlockAt(i));
        }
        if (!arrayItemsVars.isEmpty()) {
            for (int i = 0; i < program.basicBlockCount(); ++i) {
                postProcessBasicBlock(program.basicBlockAt(i));
            }
        }
    }

    private void transformBasicBlock(BasicBlock block) {
        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction insn = instructions.get(i);
            if (insn instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction)insn;
                List<Instruction> replacement = transformInvoke(invoke);
                if (replacement != null) {
                    instructions.set(i, new EmptyInstruction());
                    instructions.addAll(i, replacement);
                    i += replacement.size();
                }
            }
        }
    }

    private void postProcessBasicBlock(BasicBlock block) {
        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction insn = instructions.get(i);
            if (!(insn instanceof CastInstruction)) {
                continue;
            }
            CastInstruction cast = (CastInstruction)insn;
            if (!arrayItemsVars.contains(cast.getReceiver())) {
                continue;
            }
            if (!(cast.getTargetType() instanceof ValueType.Object)) {
                continue;
            }
            String targetTypeName = ((ValueType.Object)cast.getTargetType()).getClassName();
            Variable var = cast.getValue();
            Variable recv = cast.getReceiver();
            switch (targetTypeName) {
                case "java.lang.Integer":
                    instructions.set(i, castToWrapper(var, recv, int.class, Integer.class));
                    break;
                case "java.lang.Boolean":
                    instructions.set(i, castToWrapper(var, recv, boolean.class, Boolean.class));
                    break;
                case "java.lang.Byte":
                    instructions.set(i, castToWrapper(var, recv, byte.class, Byte.class));
                    break;
                case "java.lang.Short":
                    instructions.set(i, castToWrapper(var, recv, short.class, Short.class));
                    break;
                case "java.lang.Float":
                    instructions.set(i, castToWrapper(var, recv, float.class, Float.class));
                    break;
                case "java.lang.Double":
                    instructions.set(i, castToWrapper(var, recv, double.class, Double.class));
                    break;
                case "java.lang.String": {
                    InvokeInstruction castInvoke = new InvokeInstruction();
                    castInvoke.setType(InvocationType.SPECIAL);
                    castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castToString",
                            Object.class, String.class));
                    castInvoke.getArguments().add(var);
                    castInvoke.setReceiver(recv);
                    instructions.set(i, castInvoke);
                    break;
                }
            }
        }
    }

    private List<Instruction> transformInvoke(InvokeInstruction insn) {
        if (insn.getType() != InvocationType.VIRTUAL) {
            return null;
        }
        MethodReference method = insn.getMethod();
        if (method.getClassName().equals(ResourceArray.class.getName()) ||
                method.getClassName().equals(ResourceMap.class.getName())) {
            if (method.getName().equals("get")) {
                arrayItemsVars.add(insn.getReceiver());
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
        accessorInvoke.setMethod(new MethodReference(ResourceAccessor.class, "get",
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
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0)) +
                primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castTo" + primitiveCapitalized,
                Object.class, primitive));
        castInvoke.getArguments().add(resultVar);
        castInvoke.setReceiver(insn.getReceiver());
        instructions.add(castInvoke);
    }

    private Instruction castToWrapper(Variable var, Variable receiver, Class<?> primitive, Class<?> wrapper) {
        InvokeInstruction castInvoke = new InvokeInstruction();
        castInvoke.setType(InvocationType.SPECIAL);
        String primitiveCapitalized = primitive.getName();
        primitiveCapitalized = Character.toUpperCase(primitiveCapitalized.charAt(0)) +
                primitiveCapitalized.substring(1);
        castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castTo" + primitiveCapitalized + "Wrapper",
                Object.class, wrapper));
        castInvoke.getArguments().add(var);
        castInvoke.setReceiver(receiver);
        return castInvoke;
    }

    private void getAndCastPropertyToWrapper(InvokeInstruction insn, String property, List<Instruction> instructions,
            Class<?> primitive, Class<?> wrapper) {
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
