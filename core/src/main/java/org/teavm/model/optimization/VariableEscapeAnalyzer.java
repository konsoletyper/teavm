/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.optimization;

import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public final class VariableEscapeAnalyzer {
    private VariableEscapeAnalyzer() {
    }

    public static boolean[] findEscapingVariables(Program program) {
        boolean[] escaping = new boolean[program.variableCount()];
        InstructionAnalyzer analyzer = new InstructionAnalyzer(escaping);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(analyzer);
            }
        }
        return escaping;
    }

    private static class InstructionAnalyzer implements InstructionVisitor {
        private boolean[] escaping;

        public InstructionAnalyzer(boolean[] escaping) {
            this.escaping = escaping;
        }

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
        }

        @Override
        public void visit(NullConstantInstruction insn) {
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
        }

        @Override
        public void visit(LongConstantInstruction insn) {
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
        }

        @Override
        public void visit(StringConstantInstruction insn) {
        }

        @Override
        public void visit(BinaryInstruction insn) {
        }

        @Override
        public void visit(NegateInstruction insn) {
        }

        @Override
        public void visit(AssignInstruction insn) {
        }

        @Override
        public void visit(CastInstruction insn) {
        }

        @Override
        public void visit(CastNumberInstruction insn) {
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
        }

        @Override
        public void visit(BranchingInstruction insn) {
            escaping[insn.getOperand().getIndex()] = true;
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            escaping[insn.getFirstOperand().getIndex()] = true;
            escaping[insn.getSecondOperand().getIndex()] = true;
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
            escaping[insn.getCondition().getIndex()] = true;
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                escaping[insn.getValueToReturn().getIndex()] = true;
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            escaping[insn.getException().getIndex()] = true;
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
        }

        @Override
        public void visit(ConstructInstruction insn) {
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
        }

        @Override
        public void visit(GetFieldInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null) {
                escaping[insn.getInstance().getIndex()] = true;
            }
            escaping[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
        }

        @Override
        public void visit(GetElementInstruction insn) {
        }

        @Override
        public void visit(PutElementInstruction insn) {
            escaping[insn.getArray().getIndex()] = true;
            escaping[insn.getIndex().getIndex()] = true;
            escaping[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() != null) {
                escaping[insn.getInstance().getIndex()] = true;
            }
            for (Variable arg : insn.getArguments()) {
                escaping[arg.getIndex()] = true;
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            if (insn.getInstance() != null) {
                escaping[insn.getInstance().getIndex()] = true;
            }
            for (Variable arg : insn.getArguments()) {
                escaping[arg.getIndex()] = true;
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            escaping[insn.getObjectRef().getIndex()] = true;
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            escaping[insn.getObjectRef().getIndex()] = true;
        }
    }
}
