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

import org.teavm.common.Graph;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class UnusedVariableElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodReader method, Program program) {
        if (method.getProgram() == null) {
            return false;
        }
        Graph graph = VariableUsageGraphBuilder.build(program);
        boolean[] escaping = VariableEscapeAnalyzer.findEscapingVariables(program);
        boolean[] used = new boolean[escaping.length];

        for (int i = 0; i <= method.parameterCount(); ++i) {
            used[i] = true;
        }

        int[] stack = new int[graph.size() * 2];
        int top = 0;
        for (int i = 0; i < used.length; ++i) {
            if (escaping[i]) {
                stack[top++] = i;
            }
        }

        while (top > 0) {
            int var = stack[--top];
            if (used[var]) {
                continue;
            }
            used[var] = true;
            for (int arg : graph.incomingEdges(var)) {
                if (!used[arg]) {
                    stack[top++] = arg;
                }
            }
        }

        InstructionOptimizer insnOptimizer = new InstructionOptimizer(used);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            if (block.getExceptionVariable() != null && !used[block.getExceptionVariable().getIndex()]) {
                block.setExceptionVariable(null);
            }

            for (int j = 0; j < block.getInstructions().size(); ++j) {
                insnOptimizer.eliminate = false;
                block.getInstructions().get(j).acceptVisitor(insnOptimizer);
                if (insnOptimizer.eliminate) {
                    block.getInstructions().remove(j--);
                }
            }

            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                for (int j = 0; j < tryCatch.getJoints().size(); ++j) {
                    TryCatchJoint joint = tryCatch.getJoints().get(j);
                    if (!used[joint.getReceiver().getIndex()]) {
                        tryCatch.getJoints().remove(j--);
                    }
                }
            }

            for (int j = 0; j < block.getPhis().size(); ++j) {
                Phi phi = block.getPhis().get(j);
                if (!used[phi.getReceiver().getIndex()]) {
                    block.getPhis().remove(j--);
                }
            }
        }

        return false;
    }

    private static class InstructionOptimizer implements InstructionVisitor {
        private boolean[] used;
        boolean eliminate;

        public InstructionOptimizer(boolean[] used) {
            this.used = used;
        }

        private void requestUsage(Variable var) {
            if (!used[var.getIndex()]) {
                eliminate = true;
            }
        }

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(BinaryInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(NegateInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(AssignInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(CastInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(BranchingInstruction insn) {
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(PutFieldInstruction insn) {
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(GetElementInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getReceiver() != null && !used[insn.getReceiver().getIndex()]) {
                insn.setReceiver(null);
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            if (insn.getReceiver() != null && !used[insn.getReceiver().getIndex()]) {
                insn.setReceiver(null);
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            requestUsage(insn.getReceiver());
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
        }
    }
}
