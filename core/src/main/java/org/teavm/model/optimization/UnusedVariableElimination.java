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
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodReader;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class UnusedVariableElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        return optimize(context.getMethod(), program);
    }

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

            for (Instruction insn : block) {
                insnOptimizer.eliminate = false;
                insn.acceptVisitor(insnOptimizer);
                if (insnOptimizer.eliminate) {
                    insn.delete();
                }
            }

            for (int j = 0; j < block.getPhis().size(); ++j) {
                Phi phi = block.getPhis().get(j);
                if (!used[phi.getReceiver().getIndex()]) {
                    block.getPhis().remove(j--);
                }
            }
        }

        for (int i = 0; i < program.variableCount(); ++i) {
            if (!used[i]) {
                program.deleteVariable(i);
            }
        }
        program.pack();

        return false;
    }

    private static class InstructionOptimizer extends AbstractInstructionVisitor {
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
        public void visit(NullCheckInstruction insn) {
            requestUsage(insn.getReceiver());
        }
    }
}
