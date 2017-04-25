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

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.util.InstructionTransitionExtractor;

public class ConstantConditionElimination implements MethodOptimization {
    private int[] constants;
    private boolean[] constantDefined;
    private boolean[] nullConstants;

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        constants = new int[program.variableCount()];
        constantDefined = new boolean[program.variableCount()];
        nullConstants = new boolean[program.variableCount()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof IntegerConstantInstruction) {
                    IntegerConstantInstruction constInsn = (IntegerConstantInstruction) insn;
                    int receiver = constInsn.getReceiver().getIndex();
                    constants[receiver] = constInsn.getConstant();
                    constantDefined[receiver] = true;
                } else if (insn instanceof NullConstantInstruction) {
                    NullConstantInstruction constInsn = (NullConstantInstruction) insn;
                    int receiver = constInsn.getReceiver().getIndex();
                    nullConstants[receiver] = true;
                }
            }
        }

        boolean changed = false;
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction insn = block.getLastInstruction();
            BasicBlock target = constantTarget(insn);
            if (target != null) {
                block.getLastInstruction().acceptVisitor(transitionExtractor);

                for (BasicBlock successor : transitionExtractor.getTargets()) {
                    if (successor != target) {
                        for (Phi phi : successor.getPhis()) {
                            for (int j = 0; j < phi.getIncomings().size(); ++j) {
                                if (phi.getIncomings().get(j).getSource() == block) {
                                    phi.getIncomings().remove(j--);
                                }
                            }
                        }
                    }
                }

                JumpInstruction jump = new JumpInstruction();
                jump.setTarget(target);
                jump.setLocation(insn.getLocation());
                block.getLastInstruction().replace(jump);

                changed = true;
            }
        }

        if (changed) {
            new UnreachableBasicBlockEliminator().optimize(program);
        }

        return changed;
    }

    private BasicBlock constantTarget(Instruction instruction) {
        if (instruction instanceof BranchingInstruction) {
            BranchingInstruction branching = (BranchingInstruction) instruction;
            switch (branching.getCondition()) {
                case NULL:
                    if (nullConstants[branching.getOperand().getIndex()]) {
                        return branching.getConsequent();
                    }
                    break;
                case NOT_NULL:
                    if (nullConstants[branching.getOperand().getIndex()]) {
                        return branching.getAlternative();
                    }
                    break;
                default: {
                    int operand = branching.getOperand().getIndex();
                    if (constantDefined[operand]) {
                        return checkCondition(branching.getCondition(), constants[operand])
                                ? branching.getConsequent() : branching.getAlternative();
                    }
                    break;
                }
            }
        } else if (instruction instanceof BinaryBranchingInstruction) {
            BinaryBranchingInstruction branching = (BinaryBranchingInstruction) instruction;
            int first = branching.getFirstOperand().getIndex();
            int second = branching.getSecondOperand().getIndex();
            switch (branching.getCondition()) {
                case EQUAL:
                case NOT_EQUAL:
                    if (constantDefined[first] && constantDefined[second]) {
                        boolean result = constants[first] == constants[second];
                        if (branching.getCondition() == BinaryBranchingCondition.NOT_EQUAL) {
                            result = !result;
                        }
                        return result ? branching.getConsequent() : branching.getAlternative();
                    }
                    break;
                default:
                    break;
            }
        }

        return null;
    }

    private boolean checkCondition(BranchingCondition condition, int constant) {
        switch (condition) {
            case EQUAL:
                return constant == 0;
            case NOT_EQUAL:
                return constant != 0;
            case GREATER:
                return constant > 0;
            case GREATER_OR_EQUAL:
                return constant >= 0;
            case LESS:
                return constant < 0;
            case LESS_OR_EQUAL:
                return constant <= 0;
            case NOT_NULL:
            case NULL:
                break;
        }
        return false;
    }
}
