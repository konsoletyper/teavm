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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.ProgramUtils;

public class GlobalValueNumbering implements MethodOptimization {
    private Map<String, KnownValue> knownValues = new HashMap<>();
    private boolean eliminate;
    private int[] map;
    private Number[] numericConstants;
    private Number evaluatedConstant;
    private int receiver;
    private Program program;
    private int currentBlockIndex;
    private DominatorTree domTree;
    private boolean namesPreserved;

    private static class KnownValue {
        int value;
        int location;
    }

    public GlobalValueNumbering(boolean namesPreserved) {
        this.namesPreserved = namesPreserved;
    }

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        return optimize(program);
    }

    public boolean optimize(Program program) {
        boolean affected = false;
        this.program = program;
        knownValues.clear();
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        domTree = GraphUtils.buildDominatorTree(cfg);
        Graph dom = GraphUtils.buildDominatorGraph(domTree, cfg.size());
        map = new int[program.variableCount()];
        numericConstants = new Number[program.variableCount()];
        for (int i = 0; i < map.length; ++i) {
            map[i] = i;
        }
        List<List<Incoming>> outgoings = ProgramUtils.getPhiOutputs(program);

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

            if (block.getExceptionVariable() != null) {
                int var = map[block.getExceptionVariable().getIndex()];
                block.setExceptionVariable(program.variableAt(var));
            }

            for (Instruction currentInsn : block) {
                evaluatedConstant = null;
                currentInsn.acceptVisitor(optimizer);
                if (eliminate) {
                    affected = true;
                    currentInsn.delete();
                    eliminate = false;
                } else if (evaluatedConstant != null) {
                    if (evaluatedConstant instanceof Integer) {
                        IntegerConstantInstruction newInsn = new IntegerConstantInstruction();
                        newInsn.setConstant((Integer) evaluatedConstant);
                        newInsn.setReceiver(program.variableAt(receiver));
                        newInsn.setLocation(currentInsn.getLocation());
                        currentInsn.replace(newInsn);
                    } else if (evaluatedConstant instanceof Long) {
                        LongConstantInstruction newInsn = new LongConstantInstruction();
                        newInsn.setConstant((Long) evaluatedConstant);
                        newInsn.setReceiver(program.variableAt(receiver));
                        newInsn.setLocation(currentInsn.getLocation());
                        currentInsn.replace(newInsn);
                    } else if (evaluatedConstant instanceof Float) {
                        FloatConstantInstruction newInsn = new FloatConstantInstruction();
                        newInsn.setConstant((Float) evaluatedConstant);
                        newInsn.setReceiver(program.variableAt(receiver));
                        newInsn.setLocation(currentInsn.getLocation());
                        currentInsn.replace(newInsn);
                    } else if (evaluatedConstant instanceof Double) {
                        DoubleConstantInstruction newInsn = new DoubleConstantInstruction();
                        newInsn.setConstant((Double) evaluatedConstant);
                        newInsn.setReceiver(program.variableAt(receiver));
                        newInsn.setLocation(currentInsn.getLocation());
                        currentInsn.replace(newInsn);
                    }
                }
            }
            for (Incoming incoming : outgoings.get(v)) {
                int value = map[incoming.getValue().getIndex()];
                incoming.setValue(program.variableAt(value));
            }

            for (int succ : dom.outgoingEdges(v)) {
                stack[top++] = succ;
            }
        }
        for (int i = 0; i < map.length; ++i) {
            if (map[i] != i) {
                program.deleteVariable(i);
            }
        }

        program.pack();
        this.program = null;
        return affected;
    }

    private void bind(int var, String value) {
        String name = program.variableAt(map[var]).getDebugName();
        if (name == null || namesPreserved) {
            name = "";
        }

        KnownValue known = knownValues.get(name + ":" + value);
        if (known == null) {
            known = knownValues.get(":" + value);
        }
        boolean namesCompatible = !namesPreserved;
        if (!namesCompatible && known != null) {
            String knownName = program.variableAt(known.value).getDebugName();
            if (knownName == null) {
                knownName = "";
            }
            namesCompatible = knownName.isEmpty() || name.isEmpty() || knownName.equals(name);
        }
        if (known != null && domTree.dominates(known.location, currentBlockIndex) && known.value != var
                && namesCompatible) {
            eliminate = true;
            map[var] = known.value;
            if (!name.isEmpty()) {
                program.variableAt(known.value).setDebugName(name);
                knownValues.put(name + ":" + value, known);
            }
        } else {
            known = new KnownValue();
            known.location = currentBlockIndex;
            known.value = var;
            knownValues.put(name + ":" + value, known);
            if (!name.isEmpty()) {
                knownValues.put(":" + value, known);
            }
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
            numericConstants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            numericConstants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            numericConstants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            numericConstants[insn.getReceiver().getIndex()] = insn.getConstant();
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
            boolean commutative = false;
            String value;
            switch (insn.getOperation()) {
                case ADD:
                    value = "+";
                    commutative = true;
                    break;
                case SUBTRACT:
                    value = "-";
                    break;
                case MULTIPLY:
                    value = "*";
                    commutative = true;
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
                    commutative = true;
                    break;
                case OR:
                    value = "|";
                    commutative = true;
                    break;
                case XOR:
                    value = "^";
                    commutative = true;
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
            bind(insn.getReceiver().getIndex(), "@" + a + value + "@" + b);
            if (commutative) {
                bind(insn.getReceiver().getIndex(), "@" + b + value + "@" + a);
            }

            evaluateBinary(insn.getOperation(), insn.getOperandType(), a, b);
            if (evaluatedConstant != null) {
                numericConstants[insn.getReceiver().getIndex()] = evaluatedConstant;
            }
            receiver = insn.getReceiver().getIndex();
        }

        private void evaluateBinary(BinaryOperation operation, NumericOperandType type, int a, int b) {
            Number first = numericConstants[a];
            Number second = numericConstants[b];
            if (first == null || second == null) {
                return;
            }

            switch (type) {
                case INT: {
                    int p = first.intValue();
                    int q = second.intValue();
                    switch (operation) {
                        case ADD:
                            evaluatedConstant = p + q;
                            break;
                        case SUBTRACT:
                            evaluatedConstant = p - q;
                            break;
                        case MULTIPLY:
                            evaluatedConstant = p * q;
                            break;
                        case DIVIDE:
                            if (q != 0) {
                                evaluatedConstant = p / q;
                            }
                            break;
                        case MODULO:
                            if (q != 0) {
                                evaluatedConstant = p % q;
                            }
                            break;
                        case COMPARE:
                            evaluatedConstant = Integer.compare(p, q);
                            break;
                        case AND:
                            evaluatedConstant = p & q;
                            break;
                        case OR:
                            evaluatedConstant = p | q;
                            break;
                        case XOR:
                            evaluatedConstant = p ^ q;
                            break;
                        case SHIFT_LEFT:
                            evaluatedConstant = p << q;
                            break;
                        case SHIFT_RIGHT:
                            evaluatedConstant = p >> q;
                            break;
                        case SHIFT_RIGHT_UNSIGNED:
                            evaluatedConstant = p >>> q;
                            break;
                    }
                    break;
                }
                case LONG: {
                    long p = first.longValue();
                    long q = second.longValue();
                    switch (operation) {
                        case ADD:
                            evaluatedConstant = p + q;
                            break;
                        case SUBTRACT:
                            evaluatedConstant = p - q;
                            break;
                        case MULTIPLY:
                            evaluatedConstant = p * q;
                            break;
                        case DIVIDE:
                            if (q != 0) {
                                evaluatedConstant = p / q;
                            }
                            break;
                        case MODULO:
                            if (q != 0) {
                                evaluatedConstant = p % q;
                            }
                            break;
                        case COMPARE:
                            evaluatedConstant = Long.compare(p, q);
                            break;
                        case AND:
                            evaluatedConstant = p & q;
                            break;
                        case OR:
                            evaluatedConstant = p | q;
                            break;
                        case XOR:
                            evaluatedConstant = p ^ q;
                            break;
                        case SHIFT_LEFT:
                            evaluatedConstant = p << q;
                            break;
                        case SHIFT_RIGHT:
                            evaluatedConstant = p >> q;
                            break;
                        case SHIFT_RIGHT_UNSIGNED:
                            evaluatedConstant = p >>> q;
                            break;
                    }
                    break;
                }
                case FLOAT: {
                    float p = first.floatValue();
                    float q = second.floatValue();
                    switch (operation) {
                        case ADD:
                            evaluatedConstant = p + q;
                            break;
                        case SUBTRACT:
                            evaluatedConstant = p - q;
                            break;
                        case MULTIPLY:
                            evaluatedConstant = p * q;
                            break;
                        case DIVIDE:
                            if (q != 0) {
                                evaluatedConstant = p / q;
                            }
                            break;
                        case MODULO:
                            if (q != 0) {
                                evaluatedConstant = p % q;
                            }
                            break;
                        case COMPARE:
                            evaluatedConstant = Float.compare(p, q);
                            break;
                        case AND:
                        case OR:
                        case XOR:
                        case SHIFT_LEFT:
                        case SHIFT_RIGHT:
                        case SHIFT_RIGHT_UNSIGNED:
                            break;
                    }
                    break;
                }
                case DOUBLE: {
                    double p = first.doubleValue();
                    double q = second.doubleValue();
                    switch (operation) {
                        case ADD:
                            evaluatedConstant = p + q;
                            break;
                        case SUBTRACT:
                            evaluatedConstant = p - q;
                            break;
                        case MULTIPLY:
                            evaluatedConstant = p * q;
                            break;
                        case DIVIDE:
                            if (q != 0) {
                                evaluatedConstant = p / q;
                            }
                            break;
                        case MODULO:
                            if (q != 0) {
                                evaluatedConstant = p % q;
                            }
                            break;
                        case COMPARE:
                            evaluatedConstant = Double.compare(p, q);
                            break;
                        case AND:
                        case OR:
                        case XOR:
                        case SHIFT_LEFT:
                        case SHIFT_RIGHT:
                        case SHIFT_RIGHT_UNSIGNED:
                            break;
                    }
                    break;
                }
            }
        }

        @Override
        public void visit(NegateInstruction insn) {
            int a = map[insn.getOperand().getIndex()];
            insn.setOperand(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "-@" + a);

            Number value = numericConstants[a];
            if (value != null) {
                switch (insn.getOperandType()) {
                    case INT:
                        evaluatedConstant = -value.intValue();
                        break;
                    case LONG:
                        evaluatedConstant = -value.longValue();
                        break;
                    case FLOAT:
                        evaluatedConstant = -value.floatValue();
                        break;
                    case DOUBLE:
                        evaluatedConstant = -value.doubleValue();
                        break;
                }
            }

            receiver = insn.getReceiver().getIndex();
        }

        @Override
        public void visit(AssignInstruction insn) {
            if (namesPreserved) {
                if (insn.getReceiver().getDebugName() != null && insn.getAssignee().getDebugName() != null) {
                    if (!insn.getAssignee().getDebugName().equals(insn.getReceiver().getDebugName())) {
                        return;
                    }
                }
            }
            if (insn.getReceiver().getDebugName() != null) {
                insn.getAssignee().setDebugName(insn.getReceiver().getDebugName());
            }
            if (insn.getReceiver().getLabel() != null) {
                insn.getAssignee().setLabel(insn.getReceiver().getLabel());
            }
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

            Number value = numericConstants[a];
            if (value != null) {
                switch (insn.getTargetType()) {
                    case INT:
                        evaluatedConstant = value.intValue();
                        break;
                    case LONG:
                        evaluatedConstant = value.longValue();
                        break;
                    case FLOAT:
                        evaluatedConstant = value.floatValue();
                        break;
                    case DOUBLE:
                        evaluatedConstant = value.doubleValue();
                        break;
                }
            }

            receiver = insn.getReceiver().getIndex();
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            int a = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(a));
            bind(insn.getReceiver().getIndex(), "@" + a + "::" + insn.getTargetType() + " " + insn.getDirection());

            Number value = numericConstants[a];
            if (value != null) {
                switch (insn.getDirection()) {
                    case TO_INTEGER:
                        evaluatedConstant = value;
                        break;
                    case FROM_INTEGER: {
                        switch (insn.getTargetType()) {
                            case BYTE:
                                evaluatedConstant = value.intValue() << 24 >> 24;
                                break;
                            case SHORT:
                                evaluatedConstant = value.intValue() << 16 >> 16;
                                break;
                            case CHAR:
                                evaluatedConstant = value.intValue() & 0xFFFF;
                                break;
                        }
                    }
                }
            }

            receiver = insn.getReceiver().getIndex();
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
            int a = map[insn.getArray().getIndex()];
            insn.setArray(program.variableAt(a));
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
            insn.getArguments().replaceAll(mapper);
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            Optional.ofNullable(insn.getInstance()).map(mapper).ifPresent(insn::setInstance);
            insn.getArguments().replaceAll(mapper);
        }

        private UnaryOperator<Variable> mapper = var -> program.variableAt(map[var.getIndex()]);

        @Override
        public void visit(IsInstanceInstruction insn) {
            int val = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(val));
            bind(insn.getReceiver().getIndex(), "@" + val + " :? " + insn.getType());
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            int val = map[insn.getValue().getIndex()];
            insn.setValue(program.variableAt(val));
            bind(insn.getReceiver().getIndex(), "nullCheck @" + val);
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            int val = map[insn.getObjectRef().getIndex()];
            insn.setObjectRef(program.variableAt(val));
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            int val = map[insn.getObjectRef().getIndex()];
            insn.setObjectRef(program.variableAt(val));
        }
    };
}
