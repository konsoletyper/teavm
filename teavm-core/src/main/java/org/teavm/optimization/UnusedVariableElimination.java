package org.teavm.optimization;

import org.teavm.common.Graph;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class UnusedVariableElimination implements MethodOptimization {
    @Override
    public void optimize(MethodHolder method) {
        if (method.getProgram() == null) {
            return;
        }
        Graph graph = VariableUsageGraphBuilder.build(method.getProgram());
        boolean[] escaping = VariableEscapeAnalyzer.findEscapingVariables(method.getProgram());
        boolean[] used = new boolean[escaping.length];

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

        Program program = method.getProgram();
        InstructionOptimizer insnOptimizer = new InstructionOptimizer(used);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (int j = 0; j < block.getInstructions().size(); ++j) {
                insnOptimizer.eliminate = false;
                block.getInstructions().get(j).acceptVisitor(insnOptimizer);
                if (insnOptimizer.eliminate) {
                    block.getInstructions().remove(j--);
                }
            }
            for (int j = 0; j < block.getPhis().size(); ++j) {
                Phi phi = block.getPhis().get(j);
                if (!used[phi.getReceiver().getIndex()]) {
                    block.getPhis().remove(j--);
                }
            }
        }
    }

    private class InstructionOptimizer implements InstructionVisitor {
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
        public void visit(IsInstanceInstruction insn) {
            requestUsage(insn.getReceiver());
        }
    }
}
