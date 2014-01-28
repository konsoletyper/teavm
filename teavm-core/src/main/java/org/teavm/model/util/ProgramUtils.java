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

import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class ProgramUtils {
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
        }
        return graphBuilder.build();
    }

    public static Program copy(Program program) {
        Program copy = new Program();
        CopyVisitor insnCopier = new CopyVisitor();
        insnCopier.programCopy = copy;
        for (int i = 0; i < program.variableCount(); ++i) {
            copy.createVariable();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            copy.createBasicBlock();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            BasicBlock blockCopy = copy.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(insnCopier);
                blockCopy.getInstructions().add(insnCopier.copy);
            }
            for (Phi phi : block.getPhis()) {
                Phi phiCopy = new Phi();
                phiCopy.setReceiver(copy.variableAt(phi.getReceiver().getIndex()));
                for (Incoming incoming : phi.getIncomings()) {
                    Incoming incomingCopy = new Incoming();
                    incomingCopy.setSource(copy.basicBlockAt(incoming.getSource().getIndex()));
                    incomingCopy.setValue(copy.variableAt(incoming.getValue().getIndex()));
                    phiCopy.getIncomings().add(incomingCopy);
                }
                blockCopy.getPhis().add(phiCopy);
            }
        }
        return copy;
    }

    private static class CopyVisitor implements InstructionVisitor {
        Instruction copy;
        Program programCopy;

        @Override
        public void visit(EmptyInstruction insn) {
            copy = new EmptyInstruction();
        }

        private Variable copyVar(Variable var) {
            return programCopy.variableAt(var.getIndex());
        }

        private BasicBlock copyBlock(BasicBlock block) {
            return programCopy.basicBlockAt(block.getIndex());
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            ClassConstantInstruction insnCopy = new ClassConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            NullConstantInstruction insnCopy = new NullConstantInstruction();
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            IntegerConstantInstruction insnCopy = new IntegerConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            LongConstantInstruction insnCopy = new LongConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            FloatConstantInstruction insnCopy = new FloatConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            DoubleConstantInstruction insnCopy = new DoubleConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            StringConstantInstruction insnCopy = new StringConstantInstruction();
            insnCopy.setConstant(insn.getConstant());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(BinaryInstruction insn) {
            BinaryInstruction insnCopy = new BinaryInstruction(insn.getOperation(), insn.getOperandType());
            insnCopy.setFirstOperand(copyVar(insn.getFirstOperand()));
            insnCopy.setSecondOperand(copyVar(insn.getSecondOperand()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(NegateInstruction insn) {
            NegateInstruction insnCopy = new NegateInstruction(insn.getOperandType());
            insnCopy.setOperand(copyVar(insn.getOperand()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(AssignInstruction insn) {
            AssignInstruction insnCopy = new AssignInstruction();
            insnCopy.setAssignee(copyVar(insn.getAssignee()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(CastInstruction insn) {
            CastInstruction insnCopy = new CastInstruction();
            insnCopy.setValue(copyVar(insn.getValue()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            insnCopy.setTargetType(insn.getTargetType());
            copy = insnCopy;
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            CastNumberInstruction insnCopy = new CastNumberInstruction(insn.getSourceType(), insn.getTargetType());
            insnCopy.setValue(copyVar(insn.getValue()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            CastIntegerInstruction insnCopy = new CastIntegerInstruction(insn.getTargetType(), insn.getDirection());
            insnCopy.setValue(copyVar(insn.getValue()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(BranchingInstruction insn) {
            BranchingInstruction insnCopy = new BranchingInstruction(insn.getCondition());
            insnCopy.setOperand(copyVar(insn.getOperand()));
            insnCopy.setConsequent(copyBlock(insn.getConsequent()));
            insnCopy.setAlternative(copyBlock(insn.getAlternative()));
            copy = insnCopy;
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            BinaryBranchingInstruction insnCopy = new BinaryBranchingInstruction(insn.getCondition());
            insnCopy.setFirstOperand(copyVar(insn.getFirstOperand()));
            insnCopy.setSecondOperand(copyVar(insn.getSecondOperand()));
            insnCopy.setConsequent(copyBlock(insn.getConsequent()));
            insnCopy.setAlternative(copyBlock(insn.getAlternative()));
            copy = insnCopy;
        }

        @Override
        public void visit(JumpInstruction insn) {
            JumpInstruction insnCopy = new JumpInstruction();
            insnCopy.setTarget(copyBlock(insn.getTarget()));
            copy = insnCopy;
        }

        @Override
        public void visit(SwitchInstruction insn) {
            SwitchInstruction insnCopy = new SwitchInstruction();
            insnCopy.setCondition(copyVar(insn.getCondition()));
            insnCopy.setDefaultTarget(copyBlock(insn.getDefaultTarget()));
            for (SwitchTableEntry entry : insn.getEntries()) {
                SwitchTableEntry entryCopy = new SwitchTableEntry();
                entryCopy.setCondition(entry.getCondition());
                entryCopy.setTarget(copyBlock(entry.getTarget()));
                insnCopy.getEntries().add(entryCopy);
            }
            copy = insnCopy;
        }

        @Override
        public void visit(ExitInstruction insn) {
            ExitInstruction insnCopy = new ExitInstruction();
            insnCopy.setValueToReturn(insn.getValueToReturn() != null ? copyVar(insn.getValueToReturn()) : null);
            copy = insnCopy;
        }

        @Override
        public void visit(RaiseInstruction insn) {
            RaiseInstruction insnCopy = new RaiseInstruction();
            insnCopy.setException(copyVar(insn.getException()));
            copy = insnCopy;
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            ConstructArrayInstruction insnCopy = new ConstructArrayInstruction();
            insnCopy.setItemType(insn.getItemType());
            insnCopy.setSize(copyVar(insn.getSize()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(ConstructInstruction insn) {
            ConstructInstruction insnCopy = new ConstructInstruction();
            insnCopy.setType(insn.getType());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            ConstructMultiArrayInstruction insnCopy = new ConstructMultiArrayInstruction();
            insnCopy.setItemType(insn.getItemType());
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            for (Variable dim : insn.getDimensions()) {
                insnCopy.getDimensions().add(copyVar(dim));
            }
            copy = insnCopy;
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            GetFieldInstruction insnCopy = new GetFieldInstruction();
            insnCopy.setField(insn.getField());
            insnCopy.setFieldType(insn.getFieldType());
            insnCopy.setInstance(insn.getInstance() != null ? copyVar(insn.getInstance()) : null);
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            PutFieldInstruction insnCopy = new PutFieldInstruction();
            insnCopy.setField(insn.getField());
            insnCopy.setInstance(insn.getInstance() != null ? copyVar(insn.getInstance()) : null);
            insnCopy.setValue(copyVar(insn.getValue()));
            copy = insnCopy;
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            ArrayLengthInstruction insnCopy = new ArrayLengthInstruction();
            insnCopy.setArray(copyVar(insn.getArray()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            CloneArrayInstruction insnCopy = new CloneArrayInstruction();
            insnCopy.setArray(copyVar(insn.getArray()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            UnwrapArrayInstruction insnCopy = new UnwrapArrayInstruction(insn.getElementType());
            insnCopy.setArray(copyVar(insn.getArray()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            copy = insnCopy;
        }

        @Override
        public void visit(GetElementInstruction insn) {
            GetElementInstruction insnCopy = new GetElementInstruction();
            insnCopy.setArray(copyVar(insn.getArray()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            insnCopy.setIndex(copyVar(insn.getIndex()));
            copy = insnCopy;
        }

        @Override
        public void visit(PutElementInstruction insn) {
            PutElementInstruction insnCopy = new PutElementInstruction();
            insnCopy.setArray(copyVar(insn.getArray()));
            insnCopy.setValue(copyVar(insn.getValue()));
            insnCopy.setIndex(copyVar(insn.getIndex()));
            copy = insnCopy;
        }

        @Override
        public void visit(InvokeInstruction insn) {
            InvokeInstruction insnCopy = new InvokeInstruction();
            insnCopy.setMethod(insn.getMethod());
            insnCopy.setType(insn.getType());
            insnCopy.setInstance(insn.getInstance() != null ? copyVar(insn.getInstance()) : null);
            insnCopy.setReceiver(insn.getReceiver() != null ? copyVar(insn.getReceiver()) : null);
            for (Variable arg : insn.getArguments()) {
                insnCopy.getArguments().add(copyVar(arg));
            }
            copy = insnCopy;
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            IsInstanceInstruction insnCopy = new IsInstanceInstruction();
            insnCopy.setValue(copyVar(insn.getValue()));
            insnCopy.setReceiver(copyVar(insn.getReceiver()));
            insnCopy.setType(insn.getType());
            copy = insnCopy;
        }

        @Override
        public void visit(InitClassInstruction insn) {
            InitClassInstruction insnCopy = new InitClassInstruction();
            insnCopy.setClassName(insn.getClassName());
            copy = insnCopy;
        }
    }
}
