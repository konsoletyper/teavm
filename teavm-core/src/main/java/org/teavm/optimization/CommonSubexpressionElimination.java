package org.teavm.optimization;

import java.util.HashMap;
import java.util.Map;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.instructions.*;
import org.teavm.model.util.ProgramUtils;

/**
 *
 * @author Alexey Andreev
 */
public class CommonSubexpressionElimination implements MethodOptimization {
    private Map<String, Integer> knownValues = new HashMap<>();
    private Instruction currentInsn;
    private int[] map;
    private Program program;

    @Override
    public void optimize(MethodHolder method) {
        program = method.getProgram();
        Graph cfg = ProgramUtils.buildControlFlowGraph(method.getProgram());
        Graph dom = GraphUtils.buildDominatorGraph(GraphUtils.buildDominatorTree(cfg), cfg.size());
        map = new int[program.variableCount()];
        for (int i = 0; i < map.length; ++i) {
            map[i] = i;
        }

        int[] stack = new int[cfg.size() * 2];
        int top = 0;
        for (int i = 0; i < cfg.size(); ++i) {
            if (cfg.incomingEdgesCount(i) == 0) {
                stack[top++] = i;
            }
        }
        while (top > 0) {
            int v = stack[--top];
            BasicBlock block = program.basicBlockAt(v);
            for (int i = 0; i < block.getInstructions().size(); ++i) {
                currentInsn = block.getInstructions().get(i);
                currentInsn.acceptVisitor(optimizer);
                if (currentInsn != null) {
                    block.getInstructions().set(i, currentInsn);
                } else {
                    block.getInstructions().remove(i--);
                }
            }
            for (int succ : dom.outgoingEdges(v)) {
                stack[top++] = succ;
            }
        }
        program.pack();
    }

    private void bind(int var, String value) {
        Integer known = knownValues.get(value);
        if (known != null) {
            currentInsn = null;
            map[var] = known;
        } else {
            knownValues.put(value, var);
        }
    }

    private InstructionVisitor optimizer = new InstructionVisitor() {
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
            int a = map[insn.getFirstOperand().getIndex()];
            int b = map[insn.getSecondOperand().getIndex()];
            insn.setFirstOperand(program.variableAt(a));
            insn.setSecondOperand(program.variableAt(b));
            String value;
            switch (insn.getOperation()) {
                case ADD:
                    value = "+";
                    break;
                case SUBTRACT:
                    value = "-";
                    break;
                case MULTIPLY:
                    value = "*";
                    break;
                case DIVIDE:
                    value = "/";
                    break;
                case MODULO:
                    value = "%";
                    break;
                case COMPARE:
                    value = "$";
                    break;
                case AND:
                    value = "&";
                    break;
                case OR:
                    value = "|";
                    break;
                case XOR:
                    value = "^";
                    break;
                case SHIFT_LEFT:
                    value = "<<";
                    break;
                case SHIFT_RIGHT:
                    value = ">>";
                    break;
                case SHIFT_RIGHT_UNSIGNED:
                    value = ">>>";
                    break;
                default:
                    return;
            }
            value = "@" + a + value + "@" + b;
            bind(insn.getReceiver().getIndex(), value);
        }

        @Override
        public void visit(NegateInstruction insn) {
            int a = map[insn.getOperand().getIndex()];
            insn.setOperand(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "-@" + a);
        }

        @Override
        public void visit(AssignInstruction insn) {
            map[insn.getReceiver().getIndex()] = insn.getAssignee().getIndex();
            currentInsn = null;
        }

        @Override
        public void visit(CastInstruction insn) {
            bind(insn.getReceiver().getIndex(), "@" + insn.getValue().getIndex() + " cast " +
                    insn.getTargetType());
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            bind(insn.getReceiver().getIndex(), "@" + insn.getValue().getIndex() + " cast " +
                    insn.getTargetType());
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            bind(insn.getReceiver().getIndex(), "@" + insn.getValue().getIndex() + " cast " +
                    insn.getTargetType() + " " + insn.getDirection());
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
        }

        @Override
        public void visit(InvokeInstruction insn) {
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
        }
    };
}
