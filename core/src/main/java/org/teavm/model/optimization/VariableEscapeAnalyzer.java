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

public final class VariableEscapeAnalyzer {
    private VariableEscapeAnalyzer() {
    }

    public static boolean[] findEscapingVariables(Program program) {
        boolean[] escaping = new boolean[program.variableCount()];
        InstructionAnalyzer analyzer = new InstructionAnalyzer(escaping);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                insn.acceptVisitor(analyzer);
            }
        }
        return escaping;
    }

    private static class InstructionAnalyzer extends AbstractInstructionVisitor {
        private boolean[] escaping;

        InstructionAnalyzer(boolean[] escaping) {
            this.escaping = escaping;
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
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null) {
                escaping[insn.getInstance().getIndex()] = true;
            }
            escaping[insn.getValue().getIndex()] = true;
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
        public void visit(MonitorEnterInstruction insn) {
            escaping[insn.getObjectRef().getIndex()] = true;
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            escaping[insn.getObjectRef().getIndex()] = true;
        }
    }
}
