/*
*  Copyright 2011 Alexey Andreev.
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
package org.teavm.parsing;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.InstructionTransitionExtractor;

/**
 *
 * @author Alexey Andreev
 */
public class ProgramParser {
    private int[] depthsBefore;
    private int[] depthsAfter;
    private int currentDepth;
    private int index;
    private int[] nextIndexes;
    private Map<Label, Integer> labelIndexes;
    private List<List<Instruction>> targetInstructions;
    private List<Instruction> builder = new ArrayList<>();
    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private int minLocal;
    private Program program;

    private static class Step {
        public final int source;
        public final int target;

        public Step(int source, int target) {
            this.source = source;
            this.target = target;
        }
    }

    public Program parser(MethodNode method) {
        program = new Program();
        InsnList instructions = method.instructions;
        if (instructions.size() == 0) {
            return program;
        }
        prepare(method);
        prepareParameters(method);
        getBasicBlock(0);
        doAnalyze(method);
        assemble();
        return program;
    }

    private void prepareParameters(MethodNode method) {
        int var = 0;
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            getVariable(var++);
        }
        ValueType[] desc = MethodDescriptor.parseSignature(method.desc);
        for (ValueType paramType : desc) {
            getVariable(var++);
            if (paramType instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive)paramType).getKind()) {
                    case LONG:
                    case DOUBLE:
                        getVariable(var++);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void prepare(MethodNode method) {
        InsnList instructions = method.instructions;
        minLocal = 0;
        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            minLocal = 1;
        }
        labelIndexes = new HashMap<>();
        for (int i = 0; i < instructions.size(); ++i) {
            AbstractInsnNode node = instructions.get(i);
            if (node instanceof LabelNode) {
                labelIndexes.put(((LabelNode)node).getLabel(), i);
            }
        }
        targetInstructions = new ArrayList<>(instructions.size());
        targetInstructions.addAll(Collections.<List<Instruction>>nCopies(
                instructions.size(), null));
        basicBlocks.addAll(Collections.<BasicBlock>nCopies(instructions.size(), null));
        depthsBefore = new int[instructions.size()];
        depthsAfter = new int[instructions.size()];
        Arrays.fill(depthsBefore, -1);
        Arrays.fill(depthsAfter, -1);
    }

    private void doAnalyze(MethodNode method) {
        InsnList instructions = method.instructions;
        Deque<Step> stack = new ArrayDeque<>();
        stack.push(new Step(-1, 0));
        while (!stack.isEmpty()) {
            Step step = stack.pop();
            index = step.target;
            if (depthsBefore[index] != -1) {
                continue;
            }
            currentDepth = step.source != -1 ? depthsAfter[step.source] :
                    minLocal + method.maxLocals;
            depthsBefore[index] = currentDepth;
            nextIndexes = new int[] { index + 1 };
            instructions.get(index).accept(methodVisitor);
            depthsAfter[index] = currentDepth;
            flushInstructions();
            if (nextIndexes.length != 1) {
                emitNextBasicBlock();
            }
            for (int next : nextIndexes) {
                stack.push(new Step(index, next));
            }
        }
    }

    private void assemble() {
        BasicBlock basicBlock = null;
        for (int i = 0; i < basicBlocks.size(); ++i) {
            BasicBlock newBasicBlock = basicBlocks.get(i);
            if (newBasicBlock != null) {
                if (basicBlock != null && !hasProperLastInstruction(basicBlock)) {
                    JumpInstruction insn = new JumpInstruction();
                    insn.setTarget(newBasicBlock);
                    basicBlock.getInstructions().add(insn);
                }
                basicBlock = newBasicBlock;
            }
            List<Instruction> builtInstructions = targetInstructions.get(i);
            if (builtInstructions != null) {
                basicBlock.getInstructions().addAll(builtInstructions);
            }
        }
    }

    private boolean hasProperLastInstruction(BasicBlock basicBlock) {
        Instruction lastInsn = basicBlock.getLastInstruction();
        if (lastInsn == null) {
            return false;
        }
        InstructionTransitionExtractor extractor = new InstructionTransitionExtractor();
        lastInsn.acceptVisitor(extractor);
        return extractor.getTargets() != null;
    }

    private void flushInstructions() {
        targetInstructions.set(index, builder);
        builder = new ArrayList<>();
    }

    private BasicBlock getBasicBlock(int index) {
        BasicBlock block = basicBlocks.get(index);
        if (block == null) {
            block = program.createBasicBlock();
            basicBlocks.set(index, block);
        }
        return block;
    }

    private void emitNextBasicBlock() {
        if (index + 1 < basicBlocks.size()) {
            getBasicBlock(index+ 1);
        }
    }

    private Variable getVariable(int index) {
        while (index >= program.variableCount()) {
            program.createVariable();
        }
        return program.variableAt(index);
    }

    private void emitAssignInsn(int source, int target) {
        AssignInstruction insn = new AssignInstruction();
        insn.setAssignee(getVariable(source));
        insn.setReceiver(getVariable(target));
        builder.add(insn);
    }

    private MethodVisitor methodVisitor = new MethodVisitor() {
        @Override
        public void visitVarInsn(int opcode, int local) {
            switch (opcode) {
                case Opcodes.ILOAD:
                case Opcodes.FLOAD:
                case Opcodes.ALOAD:
                    emitAssignInsn(minLocal + local, currentDepth);
                    currentDepth++;
                    break;
                case Opcodes.LLOAD:
                case Opcodes.DLOAD:
                    emitAssignInsn(minLocal + local, currentDepth);
                    currentDepth += 2;
                    break;
                case Opcodes.ISTORE:
                case Opcodes.FSTORE:
                case Opcodes.ASTORE:
                    currentDepth--;
                    emitAssignInsn(currentDepth, minLocal + local);
                    break;
                case Opcodes.LSTORE:
                case Opcodes.DSTORE:
                    currentDepth -= 2;
                    emitAssignInsn(currentDepth, minLocal + local);
                    break;
            }
        }

        private ValueType parseType(String type) {
            if (type.startsWith("[")) {
                return ValueType.parse(type);
            } else {
                return ValueType.object(type.replace('/', '.'));
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            switch (opcode) {
                case Opcodes.NEW: {
                    String cls = type.replace('/', '.');
                    ConstructInstruction insn = new ConstructInstruction();
                    insn.setReceiver(getVariable(currentDepth));
                    insn.setType(cls);
                    builder.add(insn);
                    currentDepth++;
                    break;
                }
                case Opcodes.ANEWARRAY: {
                    int var = currentDepth - 1;
                    ValueType valueType = parseType(type);
                    ConstructArrayInstruction insn = new ConstructArrayInstruction();
                    insn.setSize(getVariable(var));
                    insn.setReceiver(getVariable(var));
                    insn.setItemType(valueType);
                    builder.add(insn);
                    break;
                }
                case Opcodes.INSTANCEOF: {
                    int var = currentDepth - 1;
                    IsInstanceInstruction insn = new IsInstanceInstruction();
                    insn.setReceiver(getVariable(var));
                    insn.setValue(getVariable(var));
                    insn.setType(parseType(type));
                    builder.add(insn);
                    break;
                }
                case Opcodes.CHECKCAST: {
                    int var = currentDepth - 1;
                    CastInstruction insn = new CastInstruction();
                    insn.setValue(getVariable(var));
                    insn.setReceiver(getVariable(var));
                    insn.setTargetType(parseType(type));
                    builder.add(insn);
                    break;
                }
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
            SwitchTableEntry[] table = new SwitchTableEntry[labels.length];
            nextIndexes = new int[labels.length + 1];
            for (int i = 0; i < labels.length; ++i) {
                Label label = labels[i];
                int target = labelIndexes.get(label);
                SwitchTableEntry entry = new SwitchTableEntry();
                entry.setCondition(i + min);
                entry.setTarget(getBasicBlock(target));
                table[i] = entry;
                nextIndexes[i] = target;
            }
            SwitchInstruction insn = new SwitchInstruction();
            insn.setCondition(getVariable(--currentDepth));
            insn.getEntries().addAll(Arrays.asList(table));
            builder.add(insn);
            int defaultIndex = labelIndexes.get(dflt);
            insn.setDefaultTarget(getBasicBlock(defaultIndex));
            nextIndexes[labels.length] = defaultIndex;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
                boolean visible) {
            return null;
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            ValueType arrayType = parseType(desc);
            Variable[] dimensions = new Variable[dims];
            for (int i = dims - 1; i >= 0; --i) {
                dimensions[i] = getVariable(--currentDepth);
                arrayType = ValueType.arrayOf(arrayType);
            }
            int var = currentDepth++;
            ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
            insn.setItemType(arrayType);
            insn.setReceiver(getVariable(var));
            insn.getDimensions().addAll(Arrays.asList(dimensions));
            builder.add(insn);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            switch (opcode) {
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC: {
                    String ownerCls;
                    if (owner.startsWith("[")) {
                        if (name.equals("clone") && desc.startsWith("()")) {
                            int var = currentDepth - 1;
                            CloneArrayInstruction insn = new CloneArrayInstruction();
                            insn.setArray(getVariable(var));
                            insn.setReceiver(getVariable(var));
                            builder.add(insn);
                            break;
                        }
                        ownerCls = "java.lang.Object";
                    } else {
                        ownerCls = owner.replace('/', '.');
                    }
                    Type[] types = Type.getArgumentTypes(desc);
                    Variable[] args = new Variable[types.length];
                    int j = args.length;
                    for (int i = types.length - 1; i >= 0; --i) {
                        if (types[i].getSize() == 2) {
                            --currentDepth;
                        }
                        args[--j] = getVariable(--currentDepth);
                    }
                    MethodDescriptor method = new MethodDescriptor(name, MethodDescriptor.parseSignature(desc));
                    int instance = -1;
                    if (opcode != Opcodes.INVOKESTATIC) {
                        instance = --currentDepth;
                    }
                    Type returnType = Type.getReturnType(desc);
                    int result = -1;
                    if (returnType.getSize() > 0) {
                        int var = currentDepth++;
                        result = var;
                        if (returnType.getSize() == 2) {
                            currentDepth++;
                        }
                    }
                    if (instance == -1) {
                        InvokeInstruction insn = new InvokeInstruction();
                        insn.setMethod(new MethodReference(ownerCls, method));
                        if (result >= 0) {
                            insn.setReceiver(getVariable(result));
                        }
                        insn.getArguments().addAll(Arrays.asList(args));
                        builder.add(insn);
                    } else {
                        InvokeInstruction insn = new InvokeInstruction();
                        if (opcode == Opcodes.INVOKESPECIAL) {
                            insn.setType(InvocationType.SPECIAL);
                        } else {
                            insn.setType(InvocationType.VIRTUAL);
                        }
                        insn.setMethod(new MethodReference(ownerCls, method));
                        if (result >= 0) {
                            insn.setReceiver(getVariable(result));
                        }
                        insn.setInstance(getVariable(instance));
                        insn.getArguments().addAll(Arrays.asList(args));
                        builder.add(insn);
                    }
                    break;
                }
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            SwitchTableEntry[] table = new SwitchTableEntry[labels.length];
            nextIndexes = new int[labels.length + 1];
            for (int i = 0; i < labels.length; ++i) {
                Label label = labels[i];
                int target = labelIndexes.get(label);
                SwitchTableEntry entry = new SwitchTableEntry();
                entry.setCondition(keys[i]);
                entry.setTarget(getBasicBlock(target));
                table[i] = entry;
                nextIndexes[i] = target;
            }
            SwitchInstruction insn = new SwitchInstruction();
            insn.setCondition(getVariable(--currentDepth));
            insn.getEntries().addAll(Arrays.asList(table));
            builder.add(insn);
            int defaultTarget = labelIndexes.get(dflt);
            insn.setDefaultTarget(getBasicBlock(defaultTarget));
            nextIndexes[labels.length] = labelIndexes.get(dflt);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start,
                Label end, int index) {
        }

        @Override
        public void visitLineNumber(int line, Label start) {
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Integer) {
                pushConstant((Integer)cst);
            } else if (cst instanceof Float) {
                pushConstant((Float)cst);
            } else if (cst instanceof Long) {
                pushConstant((Long)cst);
            } else if (cst instanceof Double) {
                pushConstant((Double)cst);
            } else if (cst instanceof String) {
                int var = currentDepth++;
                StringConstantInstruction insn = new StringConstantInstruction();
                insn.setConstant((String)cst);
                insn.setReceiver(getVariable(var));
                builder.add(insn);
            } else if (cst instanceof Type) {
                int var = currentDepth++;
                ClassConstantInstruction insn = new ClassConstantInstruction();
                insn.setConstant(ValueType.parse(((Type)cst).getDescriptor()));
                insn.setReceiver(getVariable(var));
                builder.add(insn);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void visitLabel(Label label) {
        }

        private void emitBranching(BranchingCondition condition, int value, int target) {
            BranchingInstruction insn = new BranchingInstruction(condition);
            insn.setOperand(getVariable(value));
            insn.setConsequent(getBasicBlock(target));
            insn.setAlternative(getBasicBlock(index + 1));
            builder.add(insn);
        }

        private void emitBranching(BinaryBranchingCondition condition, int first, int second,
                int target) {
            BinaryBranchingInstruction insn = new BinaryBranchingInstruction(condition);
            insn.setFirstOperand(getVariable(first));
            insn.setSecondOperand(getVariable(second));
            insn.setConsequent(getBasicBlock(target));
            insn.setAlternative(getBasicBlock(index + 1));
            builder.add(insn);
        }

        private void emitBinary(BinaryOperation operation, NumericOperandType operandType,
                int first, int second, int receiver) {
            BinaryInstruction insn = new BinaryInstruction(operation, operandType);
            insn.setFirstOperand(getVariable(first));
            insn.setSecondOperand(getVariable(second));
            insn.setReceiver(getVariable(receiver));
            builder.add(insn);
        }

        private void emitNeg(NumericOperandType operandType, int operand, int receiver) {
            NegateInstruction insn = new NegateInstruction(operandType);
            insn.setOperand(getVariable(operand));
            insn.setReceiver(getVariable(receiver));
        }

        private void emitCast(ValueType targetType, int value, int result) {
            CastInstruction insn = new CastInstruction();
            insn.setReceiver(getVariable(result));
            insn.setValue(getVariable(value));
            insn.setTargetType(targetType);
            builder.add(insn);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            int target = labelIndexes.get(label);
            int var = currentDepth - 1;
            switch (opcode) {
                case Opcodes.IFEQ:
                    currentDepth--;
                    emitBranching(BranchingCondition.EQUAL, var, target);
                    break;
                case Opcodes.IFNE:
                    currentDepth--;
                    emitBranching(BranchingCondition.NOT_EQUAL, var, target);
                    break;
                case Opcodes.IFNULL:
                    currentDepth--;
                    emitBranching(BranchingCondition.NULL, var, target);
                    break;
                case Opcodes.IFNONNULL:
                    currentDepth--;
                    emitBranching(BranchingCondition.NOT_NULL, var, target);
                    break;
                case Opcodes.IFGT:
                    currentDepth--;
                    emitBranching(BranchingCondition.GREATER, var, target);
                    break;
                case Opcodes.IFGE:
                    currentDepth--;
                    emitBranching(BranchingCondition.GREATER_OR_EQUAL, var, target);
                    break;
                case Opcodes.IFLT:
                    currentDepth--;
                    emitBranching(BranchingCondition.LESS, var, target);
                    break;
                case Opcodes.IFLE:
                    currentDepth--;
                    emitBranching(BranchingCondition.LESS_OR_EQUAL, var, target);
                    break;
                case Opcodes.IF_ACMPEQ: {
                    currentDepth -= 2;
                    emitBranching(BinaryBranchingCondition.REFERENCE_EQUAL, var - 1, var, target);
                    break;
                }
                case Opcodes.IF_ACMPNE: {
                    currentDepth -= 2;
                    emitBranching(BinaryBranchingCondition.REFERENCE_NOT_EQUAL, var - 1, var, target);
                    break;
                }
                case Opcodes.IF_ICMPEQ: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.EQUAL, r, target);
                    break;
                }
                case Opcodes.IF_ICMPNE: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.NOT_EQUAL, r, target);
                    break;
                }
                case Opcodes.IF_ICMPGE: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.GREATER_OR_EQUAL, r, target);
                    break;
                }
                case Opcodes.IF_ICMPGT: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.GREATER, r, target);
                    break;
                }
                case Opcodes.IF_ICMPLE: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.LESS_OR_EQUAL, r, target);
                    break;
                }
                case Opcodes.IF_ICMPLT: {
                    int r = currentDepth;
                    currentDepth -= 2;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, var - 1, var, r);
                    emitBranching(BranchingCondition.LESS, r, target);
                    break;
                }
                case Opcodes.GOTO: {
                    JumpInstruction insn = new JumpInstruction();
                    insn.setTarget(getBasicBlock(target));
                    builder.add(insn);
                    nextIndexes = new int[] { labelIndexes.get(label) };
                    return;
                }
                default:
                    throw new RuntimeException("Unknown opcode");
            }
            nextIndexes = new int[] { labelIndexes.get(label), index + 1 };
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            switch (opcode) {
                case Opcodes.BIPUSH: {
                    int var = currentDepth++;
                    IntegerConstantInstruction insn = new IntegerConstantInstruction();
                    insn.setConstant(operand);
                    insn.setReceiver(getVariable(var));
                    builder.add(insn);
                    break;
                }
                case Opcodes.SIPUSH: {
                    int var = currentDepth++;
                    IntegerConstantInstruction insn = new IntegerConstantInstruction();
                    insn.setConstant(operand);
                    insn.setReceiver(getVariable(var));
                    builder.add(insn);
                    break;
                }
                case Opcodes.NEWARRAY: {
                    int var = currentDepth - 1;
                    ValueType itemType;
                    switch (operand) {
                        case Opcodes.T_BOOLEAN:
                            itemType = ValueType.BOOLEAN;
                            break;
                        case Opcodes.T_BYTE:
                            itemType = ValueType.BYTE;
                            break;
                        case Opcodes.T_SHORT:
                            itemType = ValueType.SHORT;
                            break;
                        case Opcodes.T_LONG:
                            itemType = ValueType.LONG;
                            break;
                        case Opcodes.T_INT:
                            itemType = ValueType.INTEGER;
                            break;
                        case Opcodes.T_CHAR:
                            itemType = ValueType.CHARACTER;
                            break;
                        case Opcodes.T_DOUBLE:
                            itemType = ValueType.DOUBLE;
                            break;
                        case Opcodes.T_FLOAT:
                            itemType = ValueType.FLOAT;
                            break;
                        default:
                            throw new RuntimeException("Illegal opcode");
                    }
                    ConstructArrayInstruction insn = new ConstructArrayInstruction();
                    insn.setSize(getVariable(var));
                    insn.setReceiver(getVariable(var));
                    insn.setItemType(itemType);
                    builder.add(insn);
                    break;
                }
            }
        }

        private void pushConstant(int value) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(currentDepth++));
            builder.add(insn);
        }

        private void pushConstant(long value) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(currentDepth));
            builder.add(insn);
            currentDepth += 2;
        }

        private void pushConstant(double value) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(currentDepth));
            builder.add(insn);
            currentDepth += 2;
        }

        private void pushConstant(float value) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(currentDepth++));
            builder.add(insn);
        }

        private void loadArrayElement(int sz, ArrayElementType type) {
            int arrIndex = --currentDepth;
            int array = --currentDepth;
            int var = currentDepth;
            currentDepth += sz;
            UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(type);
            unwrapInsn.setArray(getVariable(array));
            unwrapInsn.setReceiver(unwrapInsn.getArray());
            builder.add(unwrapInsn);
            GetElementInstruction insn = new GetElementInstruction();
            insn.setArray(getVariable(array));
            insn.setIndex(getVariable(arrIndex));
            insn.setReceiver(getVariable(var));
            builder.add(insn);
        }

        private void storeArrayElement(int sz, ArrayElementType type) {
            currentDepth -= sz;
            int value = currentDepth;
            int arrIndex = --currentDepth;
            int array = --currentDepth;
            UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(type);
            unwrapInsn.setArray(getVariable(array));
            unwrapInsn.setReceiver(unwrapInsn.getArray());
            builder.add(unwrapInsn);
            PutElementInstruction insn = new PutElementInstruction();
            insn.setArray(getVariable(array));
            insn.setIndex(getVariable(arrIndex));
            insn.setValue(getVariable(value));
            builder.add(insn);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.ACONST_NULL: {
                    int var = currentDepth++;
                    NullConstantInstruction insn = new NullConstantInstruction();
                    insn.setReceiver(getVariable(var));
                    builder.add(insn);
                    break;
                }
                case Opcodes.ICONST_M1:
                    pushConstant(-1);
                    break;
                case Opcodes.ICONST_0:
                    pushConstant(0);
                    break;
                case Opcodes.ICONST_1:
                    pushConstant(1);
                    break;
                case Opcodes.ICONST_2:
                    pushConstant(2);
                    break;
                case Opcodes.ICONST_3:
                    pushConstant(3);
                    break;
                case Opcodes.ICONST_4:
                    pushConstant(4);
                    break;
                case Opcodes.ICONST_5:
                    pushConstant(5);
                    break;
                case Opcodes.LCONST_0:
                    pushConstant(0L);
                    break;
                case Opcodes.LCONST_1:
                    pushConstant(1L);
                    break;
                case Opcodes.DCONST_0:
                    pushConstant(0.0);
                    break;
                case Opcodes.DCONST_1:
                    pushConstant(1.0);
                    break;
                case Opcodes.FCONST_0:
                    pushConstant(0F);
                    break;
                case Opcodes.FCONST_1:
                    pushConstant(1F);
                    break;
                case Opcodes.FCONST_2:
                    pushConstant(2F);
                    break;
                case Opcodes.BALOAD:
                    loadArrayElement(1, ArrayElementType.BYTE);
                    break;
                case Opcodes.IALOAD:
                    loadArrayElement(1, ArrayElementType.INT);
                    break;
                case Opcodes.FALOAD:
                    loadArrayElement(1, ArrayElementType.FLOAT);
                    break;
                case Opcodes.SALOAD:
                    loadArrayElement(1, ArrayElementType.SHORT);
                    break;
                case Opcodes.CALOAD:
                    loadArrayElement(1, ArrayElementType.CHAR);
                    break;
                case Opcodes.AALOAD:
                    loadArrayElement(1, ArrayElementType.OBJECT);
                    break;
                case Opcodes.DALOAD:
                    loadArrayElement(2, ArrayElementType.DOUBLE);
                    break;
                case Opcodes.LALOAD:
                    loadArrayElement(2, ArrayElementType.LONG);
                    break;
                case Opcodes.BASTORE:
                    storeArrayElement(1, ArrayElementType.BYTE);
                    break;
                case Opcodes.IASTORE:
                    storeArrayElement(1, ArrayElementType.INT);
                    break;
                case Opcodes.FASTORE:
                    storeArrayElement(1, ArrayElementType.FLOAT);
                    break;
                case Opcodes.SASTORE:
                    storeArrayElement(1, ArrayElementType.SHORT);
                    break;
                case Opcodes.CASTORE:
                    storeArrayElement(1, ArrayElementType.CHAR);
                    break;
                case Opcodes.AASTORE:
                    storeArrayElement(1, ArrayElementType.OBJECT);
                    break;
                case Opcodes.DASTORE:
                    storeArrayElement(2, ArrayElementType.DOUBLE);
                    break;
                case Opcodes.LASTORE:
                    storeArrayElement(2, ArrayElementType.LONG);
                    break;
                case Opcodes.POP:
                    --currentDepth;
                    break;
                case Opcodes.POP2:
                    currentDepth -= 2;
                    break;
                case Opcodes.DUP:
                    emitAssignInsn(currentDepth - 1, currentDepth++);
                    break;
                case Opcodes.DUP_X1:
                    emitAssignInsn(currentDepth - 1, currentDepth);
                    emitAssignInsn(currentDepth - 2, currentDepth - 1);
                    emitAssignInsn(currentDepth, currentDepth - 2);
                    ++currentDepth;
                    break;
                case Opcodes.DUP_X2:
                    emitAssignInsn(currentDepth - 1, currentDepth);
                    emitAssignInsn(currentDepth - 2, currentDepth - 1);
                    emitAssignInsn(currentDepth - 3, currentDepth - 2);
                    emitAssignInsn(currentDepth, currentDepth - 3);
                    ++currentDepth;
                    break;
                case Opcodes.DUP2:
                    emitAssignInsn(currentDepth - 2, currentDepth);
                    emitAssignInsn(currentDepth - 1, currentDepth + 1);
                    currentDepth += 2;
                    break;
                case Opcodes.DUP2_X1:
                    emitAssignInsn(currentDepth - 1, currentDepth + 1);
                    emitAssignInsn(currentDepth - 2, currentDepth);
                    emitAssignInsn(currentDepth - 3, currentDepth - 1);
                    emitAssignInsn(currentDepth - 4, currentDepth - 2);
                    emitAssignInsn(currentDepth, currentDepth - 4);
                    emitAssignInsn(currentDepth + 1, currentDepth - 3);
                    currentDepth += 2;
                    break;
                case Opcodes.DUP2_X2:
                    emitAssignInsn(currentDepth - 1, currentDepth + 1);
                    emitAssignInsn(currentDepth - 2, currentDepth);
                    emitAssignInsn(currentDepth - 3, currentDepth - 1);
                    emitAssignInsn(currentDepth - 4, currentDepth - 2);
                    emitAssignInsn(currentDepth - 5, currentDepth - 3);
                    emitAssignInsn(currentDepth - 6, currentDepth - 4);
                    emitAssignInsn(currentDepth, currentDepth - 6);
                    emitAssignInsn(currentDepth + 1, currentDepth - 5);
                    currentDepth += 2;
                    break;
                case Opcodes.SWAP:
                    emitAssignInsn(currentDepth - 2, currentDepth);
                    emitAssignInsn(currentDepth - 1, currentDepth - 2);
                    emitAssignInsn(currentDepth, currentDepth - 1);
                    break;
                case Opcodes.ISUB: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FSUB: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IADD: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.ADD, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FADD: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.ADD, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IMUL: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FMUL: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IDIV: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FDIV: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IREM: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FREM: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.LADD: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.ADD, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DADD: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.ADD, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LSUB: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DSUB: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LMUL: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DMUL: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.DDIV: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LDIV: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LREM: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DREM: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.INEG: {
                    int var = currentDepth - 1;
                    emitNeg(NumericOperandType.INT, var, var);
                    break;
                }
                case Opcodes.FNEG: {
                    int var = currentDepth - 1;
                    emitNeg(NumericOperandType.FLOAT, var, var);
                    break;
                }
                case Opcodes.LNEG: {
                    int var = currentDepth - 2;
                    emitNeg(NumericOperandType.LONG, var, var);
                    break;
                }
                case Opcodes.DNEG: {
                    int var = currentDepth - 2;
                    emitNeg(NumericOperandType.DOUBLE, var, var);
                    break;
                }
                case Opcodes.FCMPG:
                case Opcodes.FCMPL: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.LCMP: {
                    int b = currentDepth - 2;
                    int a = currentDepth - 4;
                    int r = a;
                    currentDepth -= 3;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DCMPG:
                case Opcodes.DCMPL: {
                    int b = currentDepth - 2;
                    int a = currentDepth - 4;
                    int r = a;
                    currentDepth -= 3;
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.ISHL: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.SHIFT_LEFT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.ISHR: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.SHIFT_RIGHT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IUSHR: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.SHIFT_RIGHT_UNSIGNED,
                            NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.LSHL: {
                    int b = --currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.SHIFT_LEFT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LSHR: {
                    int b = --currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.SHIFT_RIGHT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LUSHR: {
                    int b = --currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.SHIFT_RIGHT_UNSIGNED,
                            NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.IAND: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.AND, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IOR: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.OR, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IXOR: {
                    int b = --currentDepth;
                    int a = --currentDepth;
                    int r = currentDepth++;
                    emitBinary(BinaryOperation.XOR, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.LAND: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.AND, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LOR: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.OR, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LXOR: {
                    currentDepth -= 2;
                    int b = currentDepth;
                    int a = currentDepth - 2;
                    int r = currentDepth - 2;
                    emitBinary(BinaryOperation.XOR, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.I2B: {
                    int val = currentDepth - 1;
                    emitCast(ValueType.BYTE, val, val);
                    break;
                }
                case Opcodes.I2C: {
                    int val = currentDepth - 1;
                    emitCast(ValueType.CHARACTER, val, val);
                    break;
                }
                case Opcodes.I2S: {
                    int val = currentDepth - 1;
                    emitCast(ValueType.SHORT, val, val);
                    break;
                }
                case Opcodes.I2F: {
                    int val = currentDepth - 1;
                    emitCast(ValueType.FLOAT, val, val);
                    break;
                }
                case Opcodes.I2L: {
                    int val = currentDepth - 1;
                    ++currentDepth;
                    emitCast(ValueType.LONG, val, val);
                    break;
                }
                case Opcodes.I2D: {
                    int val = currentDepth - 1;
                    ++currentDepth;
                    emitCast(ValueType.DOUBLE, val, val);
                    break;
                }
                case Opcodes.F2I: {
                    int val = currentDepth - 1;
                    emitCast(ValueType.INTEGER, val, val);
                    break;
                }
                case Opcodes.F2L: {
                    int val = currentDepth - 1;
                    ++currentDepth;
                    emitCast(ValueType.LONG, val, val);
                    break;
                }
                case Opcodes.F2D: {
                    int val = currentDepth - 1;
                    ++currentDepth;
                    emitCast(ValueType.DOUBLE, val, val);
                    break;
                }
                case Opcodes.D2L: {
                    int val = currentDepth - 2;
                    emitCast(ValueType.LONG, val, val);
                    break;
                }
                case Opcodes.D2I: {
                    --currentDepth;
                    int val = currentDepth - 1;
                    emitCast(ValueType.INTEGER, val, val);
                    break;
                }
                case Opcodes.D2F: {
                    --currentDepth;
                    int val = currentDepth - 1;
                    emitCast(ValueType.FLOAT, val, val);
                    break;
                }
                case Opcodes.L2I: {
                    --currentDepth;
                    int val = currentDepth - 1;
                    emitCast(ValueType.INTEGER, val, val);
                    break;
                }
                case Opcodes.L2F: {
                    --currentDepth;
                    int val = currentDepth - 1;
                    emitCast(ValueType.FLOAT, val, val);
                    break;
                }
                case Opcodes.L2D: {
                    int val = currentDepth - 2;
                    emitCast(ValueType.DOUBLE, val, val);
                    break;
                }
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN: {
                    ExitInstruction insn = new ExitInstruction();
                    insn.setValueToReturn(getVariable(--currentDepth));
                    builder.add(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    currentDepth -= 2;
                    ExitInstruction insn = new ExitInstruction();
                    insn.setValueToReturn(getVariable(currentDepth));
                    builder.add(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.RETURN: {
                    ExitInstruction insn = new ExitInstruction();
                    builder.add(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.ARRAYLENGTH: {
                    int a = currentDepth - 1;
                    UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(ArrayElementType.OBJECT);
                    unwrapInsn.setArray(getVariable(a));
                    unwrapInsn.setReceiver(getVariable(a));
                    builder.add(unwrapInsn);
                    ArrayLengthInstruction insn = new ArrayLengthInstruction();
                    insn.setArray(getVariable(a));
                    insn.setReceiver(getVariable(a));
                    builder.add(insn);
                    break;
                }
                case Opcodes.ATHROW: {
                    RaiseInstruction insn = new RaiseInstruction();
                    insn.setException(getVariable(--currentDepth));
                    builder.add(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                    --currentDepth;
                    break;
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            var += minLocal;
            IntegerConstantInstruction intInsn = new IntegerConstantInstruction();
            intInsn.setConstant(increment);
            intInsn.setReceiver(getVariable(currentDepth));
            builder.add(intInsn);
            emitBinary(BinaryOperation.ADD, NumericOperandType.INT, var, currentDepth, var);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            String ownerCls = owner.replace('/', '.');
            switch (opcode) {
                case Opcodes.GETFIELD: {
                    int instance = currentDepth - 1;
                    ValueType type = ValueType.parse(desc);
                    int value = instance;
                    if (desc.equals("D") || desc.equals("J")) {
                        currentDepth++;
                    }
                    GetFieldInstruction insn = new GetFieldInstruction();
                    insn.setInstance(getVariable(instance));
                    insn.setField(new FieldReference(ownerCls, name));
                    insn.setFieldType(type);
                    insn.setReceiver(getVariable(value));
                    builder.add(insn);
                    break;
                }
                case Opcodes.PUTFIELD: {
                    if (desc.equals("D") || desc.equals("J")) {
                        currentDepth -= 2;
                    } else {
                        currentDepth--;
                    }
                    int value = currentDepth;
                    int instance = --currentDepth;
                    PutFieldInstruction insn = new PutFieldInstruction();
                    insn.setInstance(getVariable(instance));
                    insn.setField(new FieldReference(ownerCls, name));
                    insn.setValue(getVariable(value));
                    builder.add(insn);
                    break;
                }
                case Opcodes.GETSTATIC: {
                    ValueType type = ValueType.parse(desc);
                    int value = currentDepth++;
                    if (desc.equals("D") || desc.equals("J")) {
                        currentDepth++;
                    }
                    GetFieldInstruction insn = new GetFieldInstruction();
                    insn.setField(new FieldReference(ownerCls, name));
                    insn.setFieldType(type);
                    insn.setReceiver(getVariable(value));
                    builder.add(insn);
                    break;
                }
                case Opcodes.PUTSTATIC: {
                    if (desc.equals("D") || desc.equals("J")) {
                        currentDepth--;
                    }
                    int value = --currentDepth;
                    PutFieldInstruction insn = new PutFieldInstruction();
                    insn.setField(new FieldReference(ownerCls, name));
                    insn.setValue(getVariable(value));
                    builder.add(insn);
                    break;
                }
            }
        }

        @Override
        public void visitEnd() {
        }

        @Override
        public void visitCode() {
        }

        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return null;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }
    };
}
