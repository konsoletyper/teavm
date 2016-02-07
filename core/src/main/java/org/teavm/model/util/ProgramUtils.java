/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.*;
import java.util.stream.Collectors;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public final class ProgramUtils {
    private ProgramUtils() {
    }

    public static Graph buildControlFlowGraph(Program program) {
        GraphBuilder graphBuilder = new GraphBuilder(program.basicBlockCount());
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction insn = block.getLastInstruction();
            if (insn != null) {
                insn.acceptVisitor(transitionExtractor);
                if (transitionExtractor.getTargets() != null) {
                    for (BasicBlock successor : transitionExtractor.getTargets()) {
                        graphBuilder.addEdge(i, successor.getIndex());
                    }
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                graphBuilder.addEdge(i, tryCatch.getHandler().getIndex());
            }
        }
        return graphBuilder.build();
    }

    public static Graph buildControlFlowGraphWithTryCatch(Program program) {
        GraphBuilder graphBuilder = new GraphBuilder(program.basicBlockCount());
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction insn = block.getLastInstruction();
            if (insn != null) {
                insn.acceptVisitor(transitionExtractor);
                if (transitionExtractor.getTargets() != null) {
                    for (BasicBlock successor : transitionExtractor.getTargets()) {
                        graphBuilder.addEdge(i, successor.getIndex());
                        for (TryCatchBlock succTryCatch : successor.getTryCatchBlocks()) {
                            graphBuilder.addEdge(i, succTryCatch.getHandler().getIndex());
                        }
                    }
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                graphBuilder.addEdge(i, tryCatch.getHandler().getIndex());
            }
        }
        return graphBuilder.build();
    }

    public static Map<InstructionLocation, InstructionLocation[]> getLocationCFG(Program program) {
        return new LocationGraphBuilder().build(program);
    }

    public static Program copy(ProgramReader program) {
        Program copy = new Program();
        InstructionCopyReader insnCopier = new InstructionCopyReader();
        insnCopier.programCopy = copy;
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = copy.createVariable();
            var.getDebugNames().addAll(program.variableAt(i).readDebugNames());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            copy.createBasicBlock();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            BasicBlock blockCopy = copy.basicBlockAt(i);
            blockCopy.getInstructions().addAll(copyInstructions(block, 0, block.instructionCount(), copy));
            blockCopy.getPhis().addAll(copyPhis(block, copy));
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                TryCatchBlock tryCatchCopy = new TryCatchBlock();
                tryCatchCopy.setExceptionType(tryCatch.getExceptionType());
                tryCatchCopy.setExceptionVariable(copy.variableAt(tryCatch.getExceptionVariable().getIndex()));
                tryCatchCopy.setHandler(copy.basicBlockAt(tryCatch.getHandler().getIndex()));
                blockCopy.getTryCatchBlocks().add(tryCatchCopy);
            }
        }
        return copy;
    }

    public static List<Instruction> copyInstructions(BasicBlockReader block, int from, int to, Program target) {
        List<Instruction> result = new ArrayList<>();
        InstructionCopyReader copyReader = new InstructionCopyReader();
        copyReader.programCopy = target;
        for (int i = from; i < to; ++i) {
            block.readInstruction(i, copyReader);
            copyReader.copy.setLocation(copyReader.location);
            result.add(copyReader.copy);
        }
        return result;
    }

    public static List<Phi> copyPhis(BasicBlockReader block, Program target) {
        List<Phi> result = new ArrayList<>();
        for (PhiReader phi : block.readPhis()) {
            Phi phiCopy = new Phi();
            phiCopy.setReceiver(target.variableAt(phi.getReceiver().getIndex()));
            for (IncomingReader incoming : phi.readIncomings()) {
                Incoming incomingCopy = new Incoming();
                incomingCopy.setSource(target.basicBlockAt(incoming.getSource().getIndex()));
                incomingCopy.setValue(target.variableAt(incoming.getValue().getIndex()));
                phiCopy.getIncomings().add(incomingCopy);
            }
            result.add(phiCopy);
        }
        return result;
    }

    public static List<TryCatchBlock> copyTryCatches(BasicBlockReader block, Program target) {
        List<TryCatchBlock> result = new ArrayList<>();
        for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
            TryCatchBlock tryCatchCopy = new TryCatchBlock();
            tryCatchCopy.setExceptionType(tryCatch.getExceptionType());
            tryCatchCopy.setExceptionVariable(target.variableAt(tryCatch.getExceptionVariable().getIndex()));
            tryCatchCopy.setHandler(target.basicBlockAt(tryCatch.getHandler().getIndex()));
            result.add(tryCatchCopy);
        }
        return result;
    }

    private static class InstructionCopyReader implements InstructionReader {
        Instruction copy;
        Program programCopy;
        InstructionLocation location;

        @Override
        public void location(InstructionLocation location) {
            this.location = location;
        }

        private Variable copyVar(VariableReader var) {
            if (var == null) {
                throw new NullPointerException();
            }
            return programCopy.variableAt(var.getIndex());
        }

        private BasicBlock copyBlock(BasicBlockReader block) {
            return programCopy.basicBlockAt(block.getIndex());
        }

        @Override
        public void nop() {
            copy = new EmptyInstruction();
            copy.setLocation(location);
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            ClassConstantInstruction insnCopy = new ClassConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void nullConstant(VariableReader receiver) {
            NullConstantInstruction insnCopy = new NullConstantInstruction();
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            IntegerConstantInstruction insnCopy = new IntegerConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            LongConstantInstruction insnCopy = new LongConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            FloatConstantInstruction insnCopy = new FloatConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            DoubleConstantInstruction insnCopy = new DoubleConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            StringConstantInstruction insnCopy = new StringConstantInstruction();
            insnCopy.setConstant(cst);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
            BinaryInstruction insnCopy = new BinaryInstruction(op, type);
            insnCopy.setFirstOperand(copyVar(first));
            insnCopy.setSecondOperand(copyVar(second));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
            NegateInstruction insnCopy = new NegateInstruction(type);
            insnCopy.setOperand(copyVar(operand));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            AssignInstruction insnCopy = new AssignInstruction();
            insnCopy.setAssignee(copyVar(assignee));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            CastInstruction insnCopy = new CastInstruction();
            insnCopy.setValue(copyVar(value));
            insnCopy.setReceiver(copyVar(receiver));
            insnCopy.setTargetType(targetType);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
            CastNumberInstruction insnCopy = new CastNumberInstruction(sourceType, targetType);
            insnCopy.setValue(copyVar(value));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection dir) {
            CastIntegerInstruction insnCopy = new CastIntegerInstruction(type, dir);
            insnCopy.setValue(copyVar(value));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            BranchingInstruction insnCopy = new BranchingInstruction(cond);
            insnCopy.setOperand(copyVar(operand));
            insnCopy.setConsequent(copyBlock(consequent));
            insnCopy.setAlternative(copyBlock(alternative));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            BinaryBranchingInstruction insnCopy = new BinaryBranchingInstruction(cond);
            insnCopy.setFirstOperand(copyVar(first));
            insnCopy.setSecondOperand(copyVar(second));
            insnCopy.setConsequent(copyBlock(consequent));
            insnCopy.setAlternative(copyBlock(alternative));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void jump(BasicBlockReader target) {
            JumpInstruction insnCopy = new JumpInstruction();
            insnCopy.setTarget(copyBlock(target));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            SwitchInstruction insnCopy = new SwitchInstruction();
            insnCopy.setCondition(copyVar(condition));
            insnCopy.setDefaultTarget(copyBlock(defaultTarget));
            for (SwitchTableEntryReader entry : table) {
                SwitchTableEntry entryCopy = new SwitchTableEntry();
                entryCopy.setCondition(entry.getCondition());
                entryCopy.setTarget(copyBlock(entry.getTarget()));
                insnCopy.getEntries().add(entryCopy);
            }
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            ExitInstruction insnCopy = new ExitInstruction();
            insnCopy.setValueToReturn(valueToReturn != null ? copyVar(valueToReturn) : null);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void raise(VariableReader exception) {
            RaiseInstruction insnCopy = new RaiseInstruction();
            insnCopy.setException(copyVar(exception));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            ConstructArrayInstruction insnCopy = new ConstructArrayInstruction();
            insnCopy.setItemType(itemType);
            insnCopy.setSize(copyVar(size));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            ConstructMultiArrayInstruction insnCopy = new ConstructMultiArrayInstruction();
            insnCopy.setItemType(itemType);
            insnCopy.setReceiver(copyVar(receiver));
            for (VariableReader dim : dimensions) {
                insnCopy.getDimensions().add(copyVar(dim));
            }
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void create(VariableReader receiver, String type) {
            ConstructInstruction insnCopy = new ConstructInstruction();
            insnCopy.setType(type);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            GetFieldInstruction insnCopy = new GetFieldInstruction();
            insnCopy.setField(field);
            insnCopy.setFieldType(fieldType);
            insnCopy.setInstance(instance != null ? copyVar(instance) : null);
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value,
                ValueType fieldType) {
            PutFieldInstruction insnCopy = new PutFieldInstruction();
            insnCopy.setField(field);
            insnCopy.setInstance(instance != null ? copyVar(instance) : null);
            insnCopy.setValue(copyVar(value));
            insnCopy.setFieldType(fieldType);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            ArrayLengthInstruction insnCopy = new ArrayLengthInstruction();
            insnCopy.setArray(copyVar(array));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            CloneArrayInstruction insnCopy = new CloneArrayInstruction();
            insnCopy.setArray(copyVar(array));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            UnwrapArrayInstruction insnCopy = new UnwrapArrayInstruction(elementType);
            insnCopy.setArray(copyVar(array));
            insnCopy.setReceiver(copyVar(receiver));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
            GetElementInstruction insnCopy = new GetElementInstruction();
            insnCopy.setArray(copyVar(array));
            insnCopy.setReceiver(copyVar(receiver));
            insnCopy.setIndex(copyVar(index));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value) {
            PutElementInstruction insnCopy = new PutElementInstruction();
            insnCopy.setArray(copyVar(array));
            insnCopy.setValue(copyVar(value));
            insnCopy.setIndex(copyVar(index));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            InvokeInstruction insnCopy = new InvokeInstruction();
            insnCopy.setMethod(method);
            insnCopy.setType(type);
            insnCopy.setInstance(instance != null ? copyVar(instance) : null);
            insnCopy.setReceiver(receiver != null ? copyVar(receiver) : null);
            for (VariableReader arg : arguments) {
                insnCopy.getArguments().add(copyVar(arg));
            }
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            InvokeDynamicInstruction insnCopy = new InvokeDynamicInstruction();
            insnCopy.setMethod(method);
            insnCopy.setBootstrapMethod(bootstrapMethod);
            insnCopy.getBootstrapArguments().addAll(bootstrapArguments);
            if (instance != null) {
                insnCopy.setInstance(copyVar(instance));
            }
            insnCopy.getArguments().addAll(arguments.stream().map(this::copyVar).collect(Collectors.toList()));
            insnCopy.setReceiver(receiver != null ? copyVar(receiver) : null);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
            IsInstanceInstruction insnCopy = new IsInstanceInstruction();
            insnCopy.setValue(copyVar(value));
            insnCopy.setReceiver(copyVar(receiver));
            insnCopy.setType(type);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void initClass(String className) {
            InitClassInstruction insnCopy = new InitClassInstruction();
            insnCopy.setClassName(className);
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            NullCheckInstruction insnCopy = new NullCheckInstruction();
            insnCopy.setReceiver(copyVar(receiver));
            insnCopy.setValue(copyVar(value));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
            MonitorEnterInstruction insnCopy = new MonitorEnterInstruction();
            insnCopy.setObjectRef(copyVar(objectRef));
            copy = insnCopy;
            copy.setLocation(location);
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            MonitorExitInstruction insnCopy = new MonitorExitInstruction();
            insnCopy.setObjectRef(copyVar(objectRef));
            copy = insnCopy;
            copy.setLocation(location);
        }
    }
}
