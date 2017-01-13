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
import org.objectweb.asm.tree.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.InstructionTransitionExtractor;
import org.teavm.model.util.ProgramUtils;

public class ProgramParser {
    private ReferenceCache referenceCache;
    private static final byte ROOT = 0;
    private static final byte SINGLE = 1;
    private static final byte DOUBLE_FIRST_HALF = 2;
    private static final byte DOUBLE_SECOND_HALF = 3;
    private String fileName;
    private StackFrame[] stackBefore;
    private StackFrame[] stackAfter;
    private StackFrame stack;
    private int index;
    private int[] nextIndexes;
    private Map<Label, Integer> labelIndexes;
    private Map<Label, Integer> lineNumbers;
    private List<List<Instruction>> targetInstructions;
    private List<Instruction> builder = new ArrayList<>();
    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private int minLocal;
    private Program program;
    private String currentClassName;
    private Map<Integer, List<LocalVariableNode>> localVariableMap = new HashMap<>();
    private Map<Instruction, Map<Integer, String>> variableDebugNames = new HashMap<>();

    public ProgramParser(ReferenceCache methodReferenceCache) {
        this.referenceCache = methodReferenceCache;
    }

    private static class Step {
        public final int source;
        public final int target;

        public Step(int source, int target) {
            this.source = source;
            this.target = target;
        }
    }

    private static class StackFrame {
        final StackFrame next;
        final byte type;
        final int depth;

        StackFrame(int depth) {
            this.next = null;
            this.type = ROOT;
            this.depth = depth;
        }

        StackFrame(StackFrame next, byte type) {
            this.next = next;
            this.type = type;
            this.depth = next != null ? next.depth + 1 : 0;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Program parse(MethodNode method, String className) {
        program = new Program();
        this.currentClassName = className;
        InsnList instructions = method.instructions;
        if (instructions.size() == 0) {
            return program;
        }
        prepare(method);
        program.createBasicBlock();
        getBasicBlock(0);
        JumpInstruction insn = new JumpInstruction();
        insn.setTarget(program.basicBlockAt(1));
        program.basicBlockAt(0).add(insn);
        doAnalyze(method);
        assemble(method);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (int j = 0; j < block.getTryCatchBlocks().size(); ++j) {
                TryCatchBlock tryCatch = block.getTryCatchBlocks().get(j);
                if (tryCatch.getHandler() == block) {
                    block.getTryCatchBlocks().remove(j--);
                }
            }
        }
        int signatureVars = countSignatureVariables(method.desc);
        while (program.variableCount() <= signatureVars) {
            program.createVariable();
        }
        program.basicBlockAt(0).getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(
                program.basicBlockAt(1), program));
        return program;
    }

    private int countSignatureVariables(String desc) {
        int count = 1;
        for (Type paramType : Type.getArgumentTypes(desc)) {
            count += paramType.getSize();
        }
        return count;
    }

    private int pushSingle() {
        stack = new StackFrame(stack, SINGLE);
        return stack.depth;
    }

    private int pushDouble() {
        stack = new StackFrame(stack, DOUBLE_FIRST_HALF);
        stack = new StackFrame(stack, DOUBLE_SECOND_HALF);
        return stack.next.depth;
    }

    private int popSingle() {
        if (stack == null || stack.type != SINGLE) {
            throw new AssertionError("Illegal stack state at " + index);
        }
        int depth = stack.depth;
        stack = stack.next;
        return depth;
    }

    private int popDouble() {
        if (stack == null || stack.type != DOUBLE_SECOND_HALF) {
            throw new AssertionError("***Illegal stack state at " + index);
        }
        stack = stack.next;
        if (stack == null || stack.type != DOUBLE_FIRST_HALF) {
            throw new AssertionError("***Illegal stack state at " + index);
        }
        int depth = stack.depth;
        stack = stack.next;
        return depth;
    }

    public Map<Integer, String> getDebugNames(Instruction insn) {
        Map<Integer, String> map = variableDebugNames.get(insn);
        return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }

    private void prepare(MethodNode method) {
        InsnList instructions = method.instructions;
        minLocal = 0;
        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            minLocal = 1;
        }
        labelIndexes = new HashMap<>();
        lineNumbers = new HashMap<>();
        for (int i = 0; i < instructions.size(); ++i) {
            AbstractInsnNode node = instructions.get(i);
            if (node instanceof LabelNode) {
                labelIndexes.put(((LabelNode) node).getLabel(), i);
            }
            if (node instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) node;
                lineNumbers.put(lineNumberNode.start.getLabel(), lineNumberNode.line);
            }
        }
        for (LocalVariableNode localVar : method.localVariables) {
            int location = labelIndexes.get(localVar.start.getLabel());
            localVariableMap.computeIfAbsent(location, k -> new ArrayList<>()).add(localVar);
        }
        targetInstructions = new ArrayList<>(instructions.size());
        targetInstructions.addAll(Collections.nCopies(instructions.size(), null));
        basicBlocks.addAll(Collections.nCopies(instructions.size(), null));
        stackBefore = new StackFrame[instructions.size()];
        stackAfter = new StackFrame[instructions.size()];
    }

    private void doAnalyze(MethodNode method) {
        InsnList instructions = method.instructions;
        Deque<Step> workStack = new ArrayDeque<>();
        for (Object obj : method.tryCatchBlocks) {
            TryCatchBlockNode tryCatchNode = (TryCatchBlockNode) obj;
            if (tryCatchNode.start == tryCatchNode.handler) {
                continue;
            }
            workStack.push(new Step(-2, labelIndexes.get(tryCatchNode.handler.getLabel())));
        }
        workStack.push(new Step(-1, 0));
        while (!workStack.isEmpty()) {
            Step step = workStack.pop();
            index = step.target;
            if (stackBefore[index] != null) {
                continue;
            }
            switch (step.source) {
                case -1:
                    stack = new StackFrame(minLocal + method.maxLocals - 1);
                    break;
                case -2:
                    stack = new StackFrame(minLocal + method.maxLocals - 1);
                    pushSingle();
                    break;
                default:
                    stack = stackAfter[step.source];
                    break;
            }
            stackBefore[index] = stack;
            nextIndexes = new int[] { index + 1 };
            instructions.get(index).accept(methodVisitor);
            stackAfter[index] = stack;
            flushInstructions();
            if (nextIndexes.length != 1) {
                emitNextBasicBlock();
            }
            for (int next : nextIndexes) {
                workStack.push(new Step(index, next));
            }
        }

        for (Object obj : method.tryCatchBlocks) {
            TryCatchBlockNode tryCatchNode = (TryCatchBlockNode) obj;
            if (tryCatchNode.start == tryCatchNode.handler) {
                continue;
            }
            int start = labelIndexes.get(tryCatchNode.start.getLabel());
            int end = labelIndexes.get(tryCatchNode.end.getLabel());
            getBasicBlock(start);
            getBasicBlock(end);
            for (int i = start; i < end; ++i) {
                BasicBlock block = basicBlocks.get(i);
                if (block != null) {
                    TryCatchBlock tryCatch = new TryCatchBlock();
                    if (tryCatchNode.type != null) {
                        tryCatch.setExceptionType(tryCatchNode.type.replace('/', '.'));
                    }
                    tryCatch.setHandler(getBasicBlock(labelIndexes.get(tryCatchNode.handler.getLabel())));
                    tryCatch.getHandler().setExceptionVariable(program.variableAt(minLocal + method.maxLocals));
                    block.getTryCatchBlocks().add(tryCatch);
                }
            }
        }
    }

    private void assemble(MethodNode methodNode) {
        BasicBlock basicBlock = null;
        Map<Integer, String> accumulatedDebugNames = new HashMap<>();
        Integer lastLineNumber = null;
        TextLocation lastLocation = null;
        for (int i = 0; i < basicBlocks.size(); ++i) {
            BasicBlock newBasicBlock = basicBlocks.get(i);
            if (newBasicBlock != null) {
                if (basicBlock != null && !hasProperLastInstruction(basicBlock)) {
                    JumpInstruction insn = new JumpInstruction();
                    insn.setTarget(newBasicBlock);
                    basicBlock.add(insn);
                }
                basicBlock = newBasicBlock;
                if (basicBlock.instructionCount() > 0) {
                    Map<Integer, String> debugNames = new HashMap<>(accumulatedDebugNames);
                    variableDebugNames.put(basicBlock.getFirstInstruction(), debugNames);
                }
            }
            List<Instruction> builtInstructions = targetInstructions.get(i);
            List<LocalVariableNode> localVarNodes = localVariableMap.get(i);
            if (localVarNodes != null) {
                if (builtInstructions == null || builtInstructions.isEmpty()) {
                    builtInstructions = Arrays.asList(new EmptyInstruction());
                }
                Map<Integer, String> debugNames = new HashMap<>();
                variableDebugNames.put(builtInstructions.get(0), debugNames);
                for (LocalVariableNode localVar : localVarNodes) {
                    debugNames.put(localVar.index + minLocal, localVar.name);
                }
                accumulatedDebugNames.putAll(debugNames);
            }
            AbstractInsnNode insnNode = methodNode.instructions.get(i);
            if (insnNode instanceof LabelNode) {
                Label label = ((LabelNode) insnNode).getLabel();
                Integer lineNumber = lineNumbers.get(label);
                if (lineNumber != null && !lineNumber.equals(lastLineNumber)) {
                    lastLineNumber = lineNumber;
                    lastLocation = new TextLocation(fileName, lastLineNumber);
                }
            }
            if (builtInstructions != null) {
                for (Instruction insn : builtInstructions) {
                    insn.setLocation(lastLocation);
                }
                basicBlock.addAll(builtInstructions);
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
            getBasicBlock(index + 1);
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
        addInstruction(insn);
    }

    private void addInstruction(Instruction insn) {
        builder.add(insn);
    }

    private int mapLocal(int local) {
        return local;
    }

    // TODO: invokedynamic support (a great task, involving not only parser, but every layer of TeaVM)
    private MethodVisitor methodVisitor = new MethodVisitor(Opcodes.ASM5) {
        @Override
        public void visitVarInsn(int opcode, int local) {
            switch (opcode) {
                case Opcodes.ILOAD:
                case Opcodes.FLOAD:
                case Opcodes.ALOAD:
                    emitAssignInsn(minLocal + mapLocal(local), pushSingle());
                    break;
                case Opcodes.LLOAD:
                case Opcodes.DLOAD:
                    emitAssignInsn(minLocal + mapLocal(local), pushDouble());
                    break;
                case Opcodes.ISTORE:
                case Opcodes.FSTORE:
                case Opcodes.ASTORE:
                    emitAssignInsn(popSingle(), minLocal + mapLocal(local));
                    break;
                case Opcodes.LSTORE:
                case Opcodes.DSTORE:
                    emitAssignInsn(popDouble(), minLocal + mapLocal(local));
                    break;
            }
        }

        private ValueType parseType(String type) {
            if (type.startsWith("[")) {
                return referenceCache.parseValueTypeCached(type);
            } else {
                return referenceCache.getCached(ValueType.object(type.replace('/', '.')));
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            switch (opcode) {
                case Opcodes.NEW: {
                    String cls = type.replace('/', '.');
                    ConstructInstruction insn = new ConstructInstruction();
                    insn.setReceiver(getVariable(pushSingle()));
                    insn.setType(cls);
                    addInstruction(insn);
                    break;
                }
                case Opcodes.ANEWARRAY: {
                    ValueType valueType = parseType(type);
                    ConstructArrayInstruction insn = new ConstructArrayInstruction();
                    insn.setSize(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    insn.setItemType(valueType);
                    addInstruction(insn);
                    break;
                }
                case Opcodes.INSTANCEOF: {
                    IsInstanceInstruction insn = new IsInstanceInstruction();
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    insn.setType(parseType(type));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.CHECKCAST: {
                    CastInstruction insn = new CastInstruction();
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    insn.setTargetType(parseType(type));
                    addInstruction(insn);
                    break;
                }
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
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
            insn.setCondition(getVariable(popSingle()));
            insn.getEntries().addAll(Arrays.asList(table));
            addInstruction(insn);
            int defaultIndex = labelIndexes.get(dflt);
            insn.setDefaultTarget(getBasicBlock(defaultIndex));
            nextIndexes[labels.length] = defaultIndex;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            ValueType arrayType = parseType(desc);
            Variable[] dimensions = new Variable[dims];
            for (int i = dims - 1; i >= 0; --i) {
                dimensions[i] = getVariable(popSingle());
            }
            ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
            insn.setItemType(arrayType);
            insn.setReceiver(getVariable(pushSingle()));
            insn.getDimensions().addAll(Arrays.asList(dimensions));
            addInstruction(insn);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            InvokeDynamicInstruction insn = new InvokeDynamicInstruction();
            insn.setBootstrapMethod(parseHandle(bsm));

            switch (insn.getBootstrapMethod().getKind()) {
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case INVOKE_STATIC:
                case INVOKE_CONSTRUCTOR:
                    break;
                default:
                    insn.setInstance(getVariable(popSingle()));
                    break;
            }
            Type[] types = Type.getArgumentTypes(desc);
            Variable[] args = new Variable[types.length];
            int j = args.length;
            for (int i = types.length - 1; i >= 0; --i) {
                args[--j] = types[i].getSize() == 2 ? getVariable(popDouble()) : getVariable(popSingle());
            }
            insn.getArguments().addAll(Arrays.asList(args));

            Type returnType = Type.getReturnType(desc);
            if (returnType.getSize() > 0) {
                insn.setReceiver(getVariable(returnType.getSize() == 2 ? pushDouble() : pushSingle()));
            }

            insn.setMethod(referenceCache.getCached(
                    new MethodDescriptor(name, MethodDescriptor.parseSignature(desc))));
            for (Object bsmArg : bsmArgs) {
                insn.getBootstrapArguments().add(convertConstant(bsmArg));
            }

            addInstruction(insn);
        }

        private RuntimeConstant convertConstant(Object value) {
            if (value instanceof Integer) {
                return new RuntimeConstant((Integer) value);
            } else if (value instanceof Long) {
                return new RuntimeConstant((Long) value);
            } else if (value instanceof Float) {
                return new RuntimeConstant((Float) value);
            } else if (value instanceof Double) {
                return new RuntimeConstant((Double) value);
            } else if (value instanceof String) {
                return new RuntimeConstant((String) value);
            } else if (value instanceof Type) {
                Type type = (Type) value;
                if (type.getSort() == Type.METHOD) {
                    return new RuntimeConstant(MethodDescriptor.parseSignature(type.getDescriptor()));
                } else {
                    return new RuntimeConstant(referenceCache.parseValueTypeCached(type.getDescriptor()));
                }
            } else if (value instanceof Handle) {
                return new RuntimeConstant(parseHandle((Handle) value));
            } else {
                throw new IllegalArgumentException("Unknown runtime constant: " + value);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            switch (opcode) {
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC: {
                    String ownerCls;
                    if (owner.startsWith("[")) {
                        if (name.equals("clone") && desc.startsWith("()")) {
                            CloneArrayInstruction insn = new CloneArrayInstruction();
                            insn.setArray(getVariable(popSingle()));
                            insn.setReceiver(getVariable(pushSingle()));
                            addInstruction(insn);
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
                        args[--j] = types[i].getSize() == 2 ? getVariable(popDouble()) : getVariable(popSingle());
                    }
                    MethodDescriptor method = referenceCache.getCached(
                            new MethodDescriptor(name, MethodDescriptor.parseSignature(desc)));
                    int instance = -1;
                    if (opcode != Opcodes.INVOKESTATIC) {
                        instance = popSingle();
                    }
                    Type returnType = Type.getReturnType(desc);
                    int result = -1;
                    if (returnType.getSize() > 0) {
                        result = returnType.getSize() == 2 ? pushDouble() : pushSingle();
                    }
                    if (instance == -1) {
                        InvokeInstruction insn = new InvokeInstruction();
                        insn.setType(InvocationType.SPECIAL);
                        insn.setMethod(referenceCache.getCached(new MethodReference(ownerCls, method)));
                        if (result >= 0) {
                            insn.setReceiver(getVariable(result));
                        }
                        insn.getArguments().addAll(Arrays.asList(args));
                        addInstruction(insn);
                    } else {
                        InvokeInstruction insn = new InvokeInstruction();
                        if (opcode == Opcodes.INVOKESPECIAL) {
                            insn.setType(InvocationType.SPECIAL);
                        } else {
                            insn.setType(InvocationType.VIRTUAL);
                        }
                        insn.setMethod(referenceCache.getCached(new MethodReference(ownerCls, method)));
                        if (result >= 0) {
                            insn.setReceiver(getVariable(result));
                        }
                        insn.setInstance(getVariable(instance));
                        insn.getArguments().addAll(Arrays.asList(args));
                        addInstruction(insn);
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
            insn.setCondition(getVariable(popSingle()));
            insn.getEntries().addAll(Arrays.asList(table));
            addInstruction(insn);
            int defaultTarget = labelIndexes.get(dflt);
            insn.setDefaultTarget(getBasicBlock(defaultTarget));
            nextIndexes[labels.length] = labelIndexes.get(dflt);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        }

        @Override
        public void visitLineNumber(int line, Label start) {
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Integer) {
                pushConstant((Integer) cst);
            } else if (cst instanceof Float) {
                pushConstant((Float) cst);
            } else if (cst instanceof Long) {
                pushConstant((Long) cst);
            } else if (cst instanceof Double) {
                pushConstant((Double) cst);
            } else if (cst instanceof String) {
                StringConstantInstruction insn = new StringConstantInstruction();
                insn.setConstant((String) cst);
                insn.setReceiver(getVariable(pushSingle()));
                addInstruction(insn);
            } else if (cst instanceof Type) {
                ClassConstantInstruction insn = new ClassConstantInstruction();
                insn.setConstant(referenceCache.getCached(ValueType.parse(((Type) cst).getDescriptor())));
                insn.setReceiver(getVariable(pushSingle()));
                addInstruction(insn);
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
            addInstruction(insn);
        }

        private void emitBranching(BinaryBranchingCondition condition, int first, int second,
                int target) {
            BinaryBranchingInstruction insn = new BinaryBranchingInstruction(condition);
            insn.setFirstOperand(getVariable(first));
            insn.setSecondOperand(getVariable(second));
            insn.setConsequent(getBasicBlock(target));
            insn.setAlternative(getBasicBlock(index + 1));
            addInstruction(insn);
        }

        private void emitBinary(BinaryOperation operation, NumericOperandType operandType,
                int first, int second, int receiver) {
            BinaryInstruction insn = new BinaryInstruction(operation, operandType);
            insn.setFirstOperand(getVariable(first));
            insn.setSecondOperand(getVariable(second));
            insn.setReceiver(getVariable(receiver));
            addInstruction(insn);
        }

        private void emitNeg(NumericOperandType operandType, int operand, int receiver) {
            NegateInstruction insn = new NegateInstruction(operandType);
            insn.setOperand(getVariable(operand));
            insn.setReceiver(getVariable(receiver));
            addInstruction(insn);
        }

        private void emitNumberCast(NumericOperandType source, NumericOperandType target, int value, int result) {
            CastNumberInstruction insn = new CastNumberInstruction(source, target);
            insn.setReceiver(getVariable(result));
            insn.setValue(getVariable(value));
            addInstruction(insn);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            int target = labelIndexes.get(label);
            switch (opcode) {
                case Opcodes.IFEQ:
                    emitBranching(BranchingCondition.EQUAL, popSingle(), target);
                    break;
                case Opcodes.IFNE:
                    emitBranching(BranchingCondition.NOT_EQUAL, popSingle(), target);
                    break;
                case Opcodes.IFNULL:
                    emitBranching(BranchingCondition.NULL, popSingle(), target);
                    break;
                case Opcodes.IFNONNULL:
                    emitBranching(BranchingCondition.NOT_NULL, popSingle(), target);
                    break;
                case Opcodes.IFGT:
                    emitBranching(BranchingCondition.GREATER, popSingle(), target);
                    break;
                case Opcodes.IFGE:
                    emitBranching(BranchingCondition.GREATER_OR_EQUAL, popSingle(), target);
                    break;
                case Opcodes.IFLT:
                    emitBranching(BranchingCondition.LESS, popSingle(), target);
                    break;
                case Opcodes.IFLE:
                    emitBranching(BranchingCondition.LESS_OR_EQUAL, popSingle(), target);
                    break;
                case Opcodes.IF_ACMPEQ: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBranching(BinaryBranchingCondition.REFERENCE_EQUAL, a, b, target);
                    break;
                }
                case Opcodes.IF_ACMPNE: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBranching(BinaryBranchingCondition.REFERENCE_NOT_EQUAL, a, b, target);
                    break;
                }
                case Opcodes.IF_ICMPEQ: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.EQUAL, a, target);
                    break;
                }
                case Opcodes.IF_ICMPNE: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.NOT_EQUAL, a, target);
                    break;
                }
                case Opcodes.IF_ICMPGE: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.GREATER_OR_EQUAL, a, target);
                    break;
                }
                case Opcodes.IF_ICMPGT: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.GREATER, a, target);
                    break;
                }
                case Opcodes.IF_ICMPLE: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.LESS_OR_EQUAL, a, target);
                    break;
                }
                case Opcodes.IF_ICMPLT: {
                    int b = popSingle();
                    int a = popSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.INT, a, b, a);
                    emitBranching(BranchingCondition.LESS, a, target);
                    break;
                }
                case Opcodes.GOTO: {
                    JumpInstruction insn = new JumpInstruction();
                    insn.setTarget(getBasicBlock(target));
                    addInstruction(insn);
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
                    IntegerConstantInstruction insn = new IntegerConstantInstruction();
                    insn.setConstant(operand);
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.SIPUSH: {
                    IntegerConstantInstruction insn = new IntegerConstantInstruction();
                    insn.setConstant(operand);
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.NEWARRAY: {
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
                    insn.setSize(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    insn.setItemType(itemType);
                    addInstruction(insn);
                    break;
                }
            }
        }

        private void pushConstant(int value) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(pushSingle()));
            addInstruction(insn);
        }

        private void pushConstant(long value) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(pushDouble()));
            addInstruction(insn);
        }

        private void pushConstant(double value) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(pushDouble()));
            addInstruction(insn);
        }

        private void pushConstant(float value) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(getVariable(pushSingle()));
            addInstruction(insn);
        }

        private void loadArrayElement(int sz, ArrayElementType type) {
            int arrIndex = popSingle();
            int array = popSingle();
            int var = sz == 1 ? pushSingle() : pushDouble();
            UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(type);
            unwrapInsn.setArray(getVariable(array));
            unwrapInsn.setReceiver(unwrapInsn.getArray());
            addInstruction(unwrapInsn);
            GetElementInstruction insn = new GetElementInstruction(type);
            insn.setArray(getVariable(array));
            insn.setIndex(getVariable(arrIndex));
            insn.setReceiver(getVariable(var));
            addInstruction(insn);
        }

        private void storeArrayElement(int sz, ArrayElementType type) {
            int value = sz == 1 ? popSingle() : popDouble();
            int arrIndex = popSingle();
            int array = popSingle();
            UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(type);
            unwrapInsn.setArray(getVariable(array));
            unwrapInsn.setReceiver(unwrapInsn.getArray());
            addInstruction(unwrapInsn);
            PutElementInstruction insn = new PutElementInstruction(type);
            insn.setArray(getVariable(array));
            insn.setIndex(getVariable(arrIndex));
            insn.setValue(getVariable(value));
            addInstruction(insn);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.ACONST_NULL: {
                    NullConstantInstruction insn = new NullConstantInstruction();
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
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
                case Opcodes.BALOAD: {
                    loadArrayElement(1, ArrayElementType.BYTE);
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.BYTE,
                            CastIntegerDirection.TO_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.IALOAD:
                    loadArrayElement(1, ArrayElementType.INT);
                    break;
                case Opcodes.FALOAD:
                    loadArrayElement(1, ArrayElementType.FLOAT);
                    break;
                case Opcodes.SALOAD: {
                    loadArrayElement(1, ArrayElementType.SHORT);
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.SHORT,
                            CastIntegerDirection.TO_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.CALOAD: {
                    loadArrayElement(1, ArrayElementType.CHAR);
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.CHAR,
                            CastIntegerDirection.TO_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
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
                    popSingle();
                    break;
                case Opcodes.POP2:
                    if (stack.type == SINGLE) {
                        popSingle();
                        popSingle();
                    } else {
                        popDouble();
                    }
                    break;
                case Opcodes.DUP: {
                    popSingle();
                    int orig = pushSingle();
                    int copy = pushSingle();
                    emitAssignInsn(orig, copy);
                    break;
                }
                case Opcodes.DUP_X1: {
                    popSingle();
                    popSingle();
                    int ins = pushSingle();
                    int b = pushSingle();
                    int a = pushSingle();
                    emitAssignInsn(a - 1, a);
                    emitAssignInsn(b - 1, b);
                    emitAssignInsn(a, ins);
                    break;
                }
                case Opcodes.DUP_X2: {
                    popSingle();
                    if (stack.type == SINGLE) {
                        popSingle();
                        popSingle();
                        int ins = pushSingle();
                        int c = pushSingle();
                        int b = pushSingle();
                        int a = pushSingle();
                        emitAssignInsn(a - 1, a);
                        emitAssignInsn(b - 1, b);
                        emitAssignInsn(c - 1, c);
                        emitAssignInsn(a, ins);
                    } else {
                        popDouble();
                        int ins = pushSingle();
                        int b = pushDouble();
                        int a = pushSingle();
                        emitAssignInsn(a - 1, a);
                        emitAssignInsn(b - 1, b);
                        emitAssignInsn(a, ins);
                    }
                    break;
                }
                case Opcodes.DUP2: {
                    if (stack.type == SINGLE) {
                        popSingle();
                        popSingle();
                        int origA = pushSingle();
                        int origB = pushSingle();
                        int copyA = pushSingle();
                        int copyB = pushSingle();
                        emitAssignInsn(origA, copyA);
                        emitAssignInsn(origB, copyB);
                    } else {
                        popDouble();
                        int orig = pushDouble();
                        int copy = pushDouble();
                        emitAssignInsn(orig, copy);
                    }
                    break;
                }
                case Opcodes.DUP2_X1: {
                    if (stack.type == SINGLE) {
                        popSingle();
                        popSingle();
                        popSingle();
                        int ins1 = pushSingle();
                        int ins2 = pushSingle();
                        int b = pushSingle();
                        int a1 = pushSingle();
                        int a2 = pushSingle();
                        emitAssignInsn(a2 - 2, a2);
                        emitAssignInsn(a1 - 2, a1);
                        emitAssignInsn(b - 2, b);
                        emitAssignInsn(a1, ins1);
                        emitAssignInsn(a2, ins2);
                        break;
                    } else {
                        popDouble();
                        popSingle();
                        int ins = pushDouble();
                        int b = pushSingle();
                        int a = pushDouble();
                        emitAssignInsn(a - 2, a);
                        emitAssignInsn(b - 2, b);
                        emitAssignInsn(a, ins);
                        break;
                    }
                }
                case Opcodes.DUP2_X2: {
                    if (stack.type == SINGLE) {
                        popSingle();
                        popSingle();
                        if (stack.type == SINGLE) {
                            popSingle();
                            popSingle();
                            int ins1 = pushSingle();
                            int ins2 = pushSingle();
                            int c = pushSingle();
                            int b = pushSingle();
                            int a1 = pushSingle();
                            int a2 = pushSingle();
                            emitAssignInsn(a2 - 2, a2);
                            emitAssignInsn(a1 - 2, a1);
                            emitAssignInsn(b - 2, b);
                            emitAssignInsn(c - 2, c);
                            emitAssignInsn(a1, ins1);
                            emitAssignInsn(a2, ins2);
                        } else {
                            popDouble();
                            int ins1 = pushSingle();
                            int ins2 = pushSingle();
                            int b = pushDouble();
                            int a1 = pushSingle();
                            int a2 = pushSingle();
                            emitAssignInsn(a2 - 2, a2);
                            emitAssignInsn(a1 - 2, a1);
                            emitAssignInsn(b - 2, b);
                            emitAssignInsn(a1, ins1);
                            emitAssignInsn(a2, ins2);
                        }
                    } else {
                        popDouble();
                        if (stack.type == SINGLE) {
                            popSingle();
                            popSingle();
                            int ins = pushDouble();
                            int c = pushSingle();
                            int b = pushSingle();
                            int a = pushDouble();
                            emitAssignInsn(a - 2, a);
                            emitAssignInsn(b - 2, b);
                            emitAssignInsn(c - 2, c);
                            emitAssignInsn(a, ins);
                        } else {
                            popDouble();
                            int ins = pushDouble();
                            int b = pushDouble();
                            int a = pushDouble();
                            emitAssignInsn(a - 2, a);
                            emitAssignInsn(b - 2, b);
                            emitAssignInsn(a, ins);
                        }
                    }
                    break;
                }
                case Opcodes.SWAP: {
                    int b = popSingle();
                    int a = popSingle();
                    pushSingle();
                    pushSingle();
                    int tmp = b + 1;
                    emitAssignInsn(a, tmp);
                    emitAssignInsn(b, a);
                    emitAssignInsn(tmp, b);
                    break;
                }
                case Opcodes.ISUB: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FSUB: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IADD: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.ADD, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FADD: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.ADD, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IMUL: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FMUL: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IDIV: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FDIV: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.IREM: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.FREM: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.LADD: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.ADD, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DADD: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.ADD, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LSUB: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DSUB: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.SUBTRACT, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LMUL: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DMUL: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.MULTIPLY, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.DDIV: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.LDIV: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.DIVIDE, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LREM: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DREM: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.MODULO, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.INEG: {
                    int a = popSingle();
                    int r = pushSingle();
                    emitNeg(NumericOperandType.INT, a, r);
                    break;
                }
                case Opcodes.FNEG: {
                    int a = popSingle();
                    int r = pushSingle();
                    emitNeg(NumericOperandType.FLOAT, a, r);
                    break;
                }
                case Opcodes.LNEG: {
                    int a = popDouble();
                    int r = pushDouble();
                    emitNeg(NumericOperandType.LONG, a, r);
                    break;
                }
                case Opcodes.DNEG: {
                    int a = popDouble();
                    int r = pushDouble();
                    emitNeg(NumericOperandType.DOUBLE, a, r);
                    break;
                }
                case Opcodes.FCMPG:
                case Opcodes.FCMPL: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.FLOAT, a, b, r);
                    break;
                }
                case Opcodes.LCMP: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.DCMPG:
                case Opcodes.DCMPL: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.COMPARE, NumericOperandType.DOUBLE, a, b, r);
                    break;
                }
                case Opcodes.ISHL: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.SHIFT_LEFT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.ISHR: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.SHIFT_RIGHT, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IUSHR: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.SHIFT_RIGHT_UNSIGNED,
                            NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.LSHL: {
                    int b = popSingle();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.SHIFT_LEFT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LSHR: {
                    int b = popSingle();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.SHIFT_RIGHT, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LUSHR: {
                    int b = popSingle();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.SHIFT_RIGHT_UNSIGNED, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.IAND: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.AND, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IOR: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.OR, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.IXOR: {
                    int b = popSingle();
                    int a = popSingle();
                    int r = pushSingle();
                    emitBinary(BinaryOperation.XOR, NumericOperandType.INT, a, b, r);
                    break;
                }
                case Opcodes.LAND: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.AND, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LOR: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.OR, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.LXOR: {
                    int b = popDouble();
                    int a = popDouble();
                    int r = pushDouble();
                    emitBinary(BinaryOperation.XOR, NumericOperandType.LONG, a, b, r);
                    break;
                }
                case Opcodes.I2B: {
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.BYTE,
                            CastIntegerDirection.FROM_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.I2C: {
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.CHAR,
                            CastIntegerDirection.FROM_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.I2S: {
                    CastIntegerInstruction insn = new CastIntegerInstruction(IntegerSubtype.SHORT,
                            CastIntegerDirection.FROM_INTEGER);
                    insn.setValue(getVariable(popSingle()));
                    insn.setReceiver(getVariable(pushSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.I2F: {
                    int a = popSingle();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.INT, NumericOperandType.FLOAT, a, r);
                    break;
                }
                case Opcodes.I2L: {
                    int a = popSingle();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.INT, NumericOperandType.LONG, a, r);
                    break;
                }
                case Opcodes.I2D: {
                    int a = popSingle();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.INT, NumericOperandType.DOUBLE, a, r);
                    break;
                }
                case Opcodes.F2I: {
                    int a = popSingle();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.FLOAT, NumericOperandType.INT, a, r);
                    break;
                }
                case Opcodes.F2L: {
                    int a = popSingle();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.FLOAT, NumericOperandType.LONG, a, r);
                    break;
                }
                case Opcodes.F2D: {
                    int a = popSingle();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.FLOAT, NumericOperandType.DOUBLE, a, r);
                    break;
                }
                case Opcodes.D2L: {
                    int a = popDouble();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.DOUBLE, NumericOperandType.LONG, a, r);
                    break;
                }
                case Opcodes.D2I: {
                    int a = popDouble();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.DOUBLE, NumericOperandType.INT, a, r);
                    break;
                }
                case Opcodes.D2F: {
                    int a = popDouble();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.DOUBLE, NumericOperandType.FLOAT, a, r);
                    break;
                }
                case Opcodes.L2I: {
                    int a = popDouble();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.LONG, NumericOperandType.INT, a, r);
                    break;
                }
                case Opcodes.L2F: {
                    int a = popDouble();
                    int r = pushSingle();
                    emitNumberCast(NumericOperandType.LONG, NumericOperandType.FLOAT, a, r);
                    break;
                }
                case Opcodes.L2D: {
                    int a = popDouble();
                    int r = pushDouble();
                    emitNumberCast(NumericOperandType.LONG, NumericOperandType.DOUBLE, a, r);
                    break;
                }
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN: {
                    ExitInstruction insn = new ExitInstruction();
                    insn.setValueToReturn(getVariable(popSingle()));
                    addInstruction(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    ExitInstruction insn = new ExitInstruction();
                    insn.setValueToReturn(getVariable(popDouble()));
                    addInstruction(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.RETURN: {
                    ExitInstruction insn = new ExitInstruction();
                    addInstruction(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.ARRAYLENGTH: {
                    int a = popSingle();
                    int r = pushSingle();
                    UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(ArrayElementType.OBJECT);
                    unwrapInsn.setArray(getVariable(a));
                    unwrapInsn.setReceiver(getVariable(r));
                    addInstruction(unwrapInsn);
                    ArrayLengthInstruction insn = new ArrayLengthInstruction();
                    insn.setArray(getVariable(a));
                    insn.setReceiver(getVariable(r));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.ATHROW: {
                    RaiseInstruction insn = new RaiseInstruction();
                    insn.setException(getVariable(popSingle()));
                    addInstruction(insn);
                    nextIndexes = new int[0];
                    return;
                }
                case Opcodes.MONITORENTER: {
                    MonitorEnterInstruction insn = new MonitorEnterInstruction();
                    insn.setObjectRef(getVariable(popSingle()));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.MONITOREXIT: {
                    MonitorExitInstruction insn = new MonitorExitInstruction();
                    insn.setObjectRef(getVariable(popSingle()));
                    addInstruction(insn);
                    break;
                }

            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            var = minLocal + mapLocal(var);
            int tmp = pushSingle();
            popSingle();
            IntegerConstantInstruction intInsn = new IntegerConstantInstruction();
            intInsn.setConstant(increment);
            intInsn.setReceiver(getVariable(tmp));
            addInstruction(intInsn);
            emitBinary(BinaryOperation.ADD, NumericOperandType.INT, var, tmp, var);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            String ownerCls = owner.replace('/', '.');
            switch (opcode) {
                case Opcodes.GETFIELD: {
                    int instance = popSingle();
                    ValueType type = referenceCache.parseValueTypeCached(desc);
                    int value = desc.equals("D") || desc.equals("J") ? pushDouble() : pushSingle();
                    GetFieldInstruction insn = new GetFieldInstruction();
                    insn.setInstance(getVariable(instance));
                    insn.setField(referenceCache.getCached(new FieldReference(ownerCls, name)));
                    insn.setFieldType(type);
                    insn.setReceiver(getVariable(value));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.PUTFIELD: {
                    int value = desc.equals("D") || desc.equals("J") ? popDouble() : popSingle();
                    int instance = popSingle();
                    PutFieldInstruction insn = new PutFieldInstruction();
                    insn.setInstance(getVariable(instance));
                    insn.setField(referenceCache.getCached(new FieldReference(ownerCls, name)));
                    insn.setValue(getVariable(value));
                    insn.setFieldType(referenceCache.parseValueTypeCached(desc));
                    addInstruction(insn);
                    break;
                }
                case Opcodes.GETSTATIC: {
                    ValueType primitiveClassLiteral = getPrimitiveTypeField(owner + "." + name);
                    if (primitiveClassLiteral != null) {
                        ClassConstantInstruction insn = new ClassConstantInstruction();
                        insn.setConstant(primitiveClassLiteral);
                        insn.setReceiver(getVariable(pushSingle()));
                        addInstruction(insn);
                    } else {
                        ValueType type = referenceCache.parseValueTypeCached(desc);
                        int value = desc.equals("D") || desc.equals("J") ? pushDouble() : pushSingle();
                        GetFieldInstruction insn = new GetFieldInstruction();
                        insn.setField(referenceCache.getCached(new FieldReference(ownerCls, name)));
                        insn.setFieldType(type);
                        insn.setReceiver(getVariable(value));
                        addInstruction(insn);
                    }
                    break;
                }
                case Opcodes.PUTSTATIC: {
                    int value = desc.equals("D") || desc.equals("J") ? popDouble() : popSingle();
                    PutFieldInstruction insn = new PutFieldInstruction();
                    insn.setField(referenceCache.getCached(new FieldReference(ownerCls, name)));
                    insn.setValue(getVariable(value));
                    insn.setFieldType(referenceCache.parseValueTypeCached(desc));
                    addInstruction(insn);
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

    private static MethodHandle parseHandle(Handle handle) {
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD:
                return MethodHandle.fieldGetter(handle.getOwner().replace('/', '.'), handle.getName(),
                        ValueType.parse(handle.getDesc()));
            case Opcodes.H_GETSTATIC:
                return MethodHandle.staticFieldGetter(handle.getOwner().replace('/', '.'), handle.getName(),
                        ValueType.parse(handle.getDesc()));
            case Opcodes.H_PUTFIELD:
                return MethodHandle.fieldSetter(handle.getOwner().replace('/', '.'), handle.getName(),
                        ValueType.parse(handle.getDesc()));
            case Opcodes.H_PUTSTATIC:
                return MethodHandle.staticFieldSetter(handle.getOwner().replace('/', '.'), handle.getName(),
                        ValueType.parse(handle.getDesc()));
            case Opcodes.H_INVOKEVIRTUAL:
                return MethodHandle.virtualCaller(handle.getOwner().replace('/', '.'), handle.getName(),
                        MethodDescriptor.parseSignature(handle.getDesc()));
            case Opcodes.H_INVOKESTATIC:
                return MethodHandle.staticCaller(handle.getOwner().replace('/', '.'), handle.getName(),
                        MethodDescriptor.parseSignature(handle.getDesc()));
            case Opcodes.H_INVOKESPECIAL:
                return MethodHandle.specialCaller(handle.getOwner().replace('/', '.'), handle.getName(),
                        MethodDescriptor.parseSignature(handle.getDesc()));
            case Opcodes.H_NEWINVOKESPECIAL:
                return MethodHandle.constructorCaller(handle.getOwner().replace('/', '.'), handle.getName(),
                        MethodDescriptor.parseSignature(handle.getDesc()));
            case Opcodes.H_INVOKEINTERFACE:
                return MethodHandle.interfaceCaller(handle.getOwner().replace('/', '.'), handle.getName(),
                        MethodDescriptor.parseSignature(handle.getDesc()));
            default:
                throw new IllegalArgumentException("Unknown handle tag: " + handle.getTag());
        }
    }

    private static ValueType getPrimitiveTypeField(String fieldName) {
        switch (fieldName) {
            case "java/lang/Boolean.TYPE":
                return ValueType.BOOLEAN;
            case "java/lang/Byte.TYPE":
                return ValueType.BYTE;
            case "java/lang/Short.TYPE":
                return ValueType.SHORT;
            case "java/lang/Character.TYPE":
                return ValueType.CHARACTER;
            case "java/lang/Integer.TYPE":
                return ValueType.INTEGER;
            case "java/lang/Long.TYPE":
                return ValueType.LONG;
            case "java/lang/Float.TYPE":
                return ValueType.FLOAT;
            case "java/lang/Double.TYPE":
                return ValueType.DOUBLE;
            case "java/lang/Void.TYPE":
                return ValueType.VOID;
            default:
                return null;
        }
    }
}
