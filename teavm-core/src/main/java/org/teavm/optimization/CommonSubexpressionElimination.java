package org.teavm.optimization;

import java.util.HashMap;
import java.util.Map;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.ProgramUtils;

/**
 *
 * @author Alexey Andreev
 */
public class CommonSubexpressionElimination implements MethodOptimization {
    private Map<String, KnownValue> knownValues = new HashMap<>();
    private boolean eliminate;
    private int[] map;
    private Program program;
    private int currentBlockIndex;
    private DominatorTree domTree;

    private static class KnownValue {
        int value;
        int location;
    }

    @Override
    public void optimize(MethodHolder method) {
        program = method.getProgram();
        knownValues.clear();
        Graph cfg = ProgramUtils.buildControlFlowGraph(method.getProgram());
        domTree = GraphUtils.buildDominatorTree(cfg);
        Graph dom = GraphUtils.buildDominatorGraph(domTree, cfg.size());
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
            currentBlockIndex = v;
            BasicBlock block = program.basicBlockAt(v);
            for (int i = 0; i < block.getInstructions().size(); ++i) {
                Instruction currentInsn = block.getInstructions().get(i);
                currentInsn.acceptVisitor(optimizer);
                if (eliminate) {
                    block.getInstructions().remove(i--);
                    eliminate = false;
                }
            }
            for (int succ : dom.outgoingEdges(v)) {
                stack[top++] = succ;
            }
        }
        for (int v = 0; v < program.basicBlockCount(); ++v) {
            BasicBlock block = program.basicBlockAt(v);
            for (int i = 0; i < block.getPhis().size(); ++i) {
                Phi phi = block.getPhis().get(i);
                for (Incoming incoming : phi.getIncomings()) {
                    int value = map[incoming.getValue().getIndex()];
                    incoming.setValue(program.variableAt(value));
                }
            }
        }

        for (int i = 0; i < map.length; ++i) {
            if (map[i] != i) {
                program.deleteVariable(i);
            }
        }

        program.pack();
        program = null;
    }

    private void bind(int var, String value) {
        KnownValue known = knownValues.get(value);
        if (known != null && domTree.dominates(known.location, currentBlockIndex)) {
            eliminate = true;
            map[var] = known.value;
        } else {
            known = new KnownValue();
            known.location = currentBlockIndex;
            known.value = var;
            knownValues.put(value, known);
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
            map[insn.getReceiver().getIndex()] = map[insn.getAssignee().getIndex()];
            eliminate = true;
        }

        @Override
        public void visit(CastInstruction insn) {
            int a = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + "::" + insn.getTargetType());
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            int a = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + "::" + insn.getTargetType());
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            int a = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + "::" + insn.getTargetType() + " " + insn.getDirection());
        }

        @Override
        public void visit(BranchingInstruction insn) {
            int a = map[insn.getOperand().getIndex()];
            insn.setOperand(program.variableAt(a));
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            int a = map[insn.getFirstOperand().getIndex()];
            int b = map[insn.getSecondOperand().getIndex()];
            insn.setFirstOperand(program.variableAt(a));
            insn.setSecondOperand(program.variableAt(b));
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
            int a = map[insn.getCondition().getIndex()];
            insn.setCondition(program.variableAt(a));
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                int a = map[insn.getValueToReturn().getIndex()];
                insn.setValueToReturn(program.variableAt(a));
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            int a = map[insn.getException().getIndex()];
            insn.setException(program.variableAt(a));
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            int a = map[insn.getSize().getIndex()];
            insn.setSize(program.variableAt(a));
        }

        @Override
        public void visit(ConstructInstruction insn) {
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            for (int i = 0; i < insn.getDimensions().size(); ++i) {
                int a = map[insn.getDimensions().get(i).getIndex()];
                insn.getDimensions().set(i, program.variableAt(a));
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() != null) {
                int instance = map[insn.getInstance().getIndex()];
                insn.setInstance(program.variableAt(instance));
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null) {
                int instance = map[insn.getInstance().getIndex()];
                insn.setInstance(program.variableAt(instance));
            }
            int val = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(val));
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            int a = map[insn.getArray().getIndex()];
            insn.setArray(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + ".length");
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            int a = map[insn.getArray().getIndex()];
            insn.setArray(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + ".data");
        }

        @Override
        public void visit(GetElementInstruction insn) {
            int a = map[insn.getArray().getIndex()];
            insn.setArray(program.variableAt(a));
            int index = map[insn.getIndex().getIndex()];
            insn.setIndex(program.variableAt(index));
        }

        @Override
        public void visit(PutElementInstruction insn) {
            int a = map[insn.getArray().getIndex()];
            insn.setArray(program.variableAt(a));
            int index = map[insn.getIndex().getIndex()];
            insn.setIndex(program.variableAt(index));
            int val = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(val));
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() != null) {
                int instance = map[insn.getInstance().getIndex()];
                insn.setInstance(program.variableAt(instance));
            }
            for (int i = 0; i < insn.getArguments().size(); ++i) {
                int arg = map[insn.getArguments().get(i).getIndex()];
                insn.getArguments().set(i, program.variableAt(arg));
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            int val = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(val));
            bind(insn.getReceiver().getIndex(), "@" + val + " :? " + insn.getType());
        }
    };
}
