/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.transformation;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.Arrays;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DominatorWalker;
import org.teavm.model.util.DominatorWalkerCallback;
import org.teavm.model.util.PhiUpdater;

public class BoundCheckInsertion {
    public void transformProgram(Program program, MethodReference methodReference) {
        if (program.basicBlockCount() == 0) {
            return;
        }

        InsertionVisitor visitor = new InsertionVisitor(program.variableCount());
        new DominatorWalker(program).walk(visitor);
        if (visitor.changed) {
            new PhiUpdater().updatePhis(program, methodReference.parameterCount() + 1);
        }
    }

    static class InsertionVisitor extends AbstractInstructionVisitor
            implements DominatorWalkerCallback<BlockBounds> {
        BlockBounds bounds;
        boolean changed;
        private boolean[] isConstant;
        private boolean[] isConstantSizedArray;
        private int[] constantValue;
        private IntSet[] upperArrayLengths;
        private boolean[] nonNegative;
        private int[] map;
        private int[] arrayLengthVars;
        private int[] arrayLengthReverseVars;
        private int[] comparisonLeft;
        private int[] comparisonRight;
        private int conditionBlock;
        private int comparisonValue;
        private int comparisonVariable;
        private ComparisonMode comparisonMode;

        InsertionVisitor(int variableCount) {
            isConstant = new boolean[variableCount];
            isConstantSizedArray = new boolean[variableCount];
            constantValue = new int[variableCount];
            upperArrayLengths = new IntSet[variableCount];
            nonNegative = new boolean[variableCount];
            map = new int[variableCount];
            for (int i = 0; i < variableCount; ++i) {
                map[i] = i;
            }
            arrayLengthVars = new int[variableCount];
            Arrays.fill(arrayLengthVars, -1);
            arrayLengthReverseVars = new int[variableCount];
            comparisonLeft = new int[variableCount];
            Arrays.fill(comparisonLeft, -1);
            comparisonRight = new int[variableCount];
        }

        @Override
        public BlockBounds visit(BasicBlock block) {
            bounds = new BlockBounds();

            if (comparisonMode != null && conditionBlock == block.getIndex()) {
                switch (comparisonMode) {
                    case LESS_THAN_ARRAY_LENGTH:
                        addArrayBound(comparisonVariable, comparisonValue);
                        break;
                    case NON_NEGATIVE:
                        markAsNonNegative(comparisonVariable);
                        break;
                }
            }

            for (Instruction instruction : block) {
                instruction.acceptVisitor(this);
            }

            bounds.comparisonMode = comparisonMode;
            bounds.comparisonValue = comparisonValue;
            bounds.comparisonVariable = comparisonVariable;
            bounds.conditionBlock = conditionBlock;

            return bounds;
        }

        @Override
        public void endVisit(BasicBlock block, BlockBounds state) {
            IntArrayList addedArrayBounds = state.addedArrayBounds;
            int size = addedArrayBounds.size();
            for (int i = 0; i < size; i += 2) {
                int index = addedArrayBounds.get(i);
                int array = addedArrayBounds.get(i + 1);
                upperArrayLengths[index].removeAll(array);
            }

            for (IntCursor cursor : state.nonNegatives) {
                nonNegative[cursor.value] = false;
            }

            comparisonMode = state.comparisonMode;
            comparisonValue = state.comparisonValue;
            comparisonVariable = state.comparisonVariable;
            conditionBlock = state.conditionBlock;
        }

        @Override
        public void visit(JumpInstruction insn) {
            prepareJump();
        }

        @Override
        public void visit(BranchingInstruction insn) {
            prepareJump();
            int operand = index(insn.getOperand());
            int left = comparisonLeft[operand];
            int right = comparisonRight[operand];

            if (left >= 0) {
                switch (insn.getCondition()) {
                    case LESS:
                        if (arrayLengthVars[right] >= 0) {
                            comparisonMode = ComparisonMode.LESS_THAN_ARRAY_LENGTH;
                            comparisonValue = arrayLengthVars[right];
                            comparisonVariable = left;
                            conditionBlock = insn.getConsequent().getIndex();
                        } else if (isConstant[left] && constantValue[left] >= -1) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = right;
                            conditionBlock = insn.getConsequent().getIndex();
                        } else if (isConstant[right] && constantValue[right] >= 0) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = left;
                            conditionBlock = insn.getAlternative().getIndex();
                        }
                        break;

                    case GREATER_OR_EQUAL:
                        if (arrayLengthVars[right] >= 0) {
                            comparisonMode = ComparisonMode.LESS_THAN_ARRAY_LENGTH;
                            comparisonValue = arrayLengthVars[right];
                            comparisonVariable = left;
                            conditionBlock = insn.getAlternative().getIndex();
                        } else if (isConstant[left] && constantValue[left] >= -1) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = right;
                            conditionBlock = insn.getAlternative().getIndex();
                        } else if (isConstant[right] && constantValue[right] >= 0) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = left;
                            conditionBlock = insn.getConsequent().getIndex();
                        }
                        break;

                    case GREATER:
                        if (arrayLengthVars[left] >= 0) {
                            comparisonMode = ComparisonMode.LESS_THAN_ARRAY_LENGTH;
                            comparisonValue = arrayLengthVars[left];
                            comparisonVariable = right;
                            conditionBlock = insn.getConsequent().getIndex();
                        } else if (isConstant[left] && constantValue[left] >= 0) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = right;
                            conditionBlock = insn.getAlternative().getIndex();
                        } else if (isConstant[right] && constantValue[right] >= -1) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = left;
                            conditionBlock = insn.getConsequent().getIndex();
                        }
                        break;

                    case LESS_OR_EQUAL:
                        if (arrayLengthVars[left] >= 0) {
                            comparisonMode = ComparisonMode.LESS_THAN_ARRAY_LENGTH;
                            comparisonValue = arrayLengthVars[left];
                            comparisonVariable = right;
                            conditionBlock = insn.getAlternative().getIndex();
                        } else if (isConstant[left] && constantValue[left] >= 0) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = right;
                            conditionBlock = insn.getConsequent().getIndex();
                        } else if (isConstant[right] && constantValue[right] >= -1) {
                            comparisonMode = ComparisonMode.NON_NEGATIVE;
                            comparisonVariable = left;
                            conditionBlock = insn.getAlternative().getIndex();
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            prepareJump();
        }

        private void prepareJump() {
            conditionBlock = -1;
            comparisonMode = null;
            comparisonValue = 0;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            int receiver = index(insn.getReceiver());
            isConstant[receiver] = true;
            constantValue[receiver] = insn.getConstant();
        }

        @Override
        public void visit(BinaryInstruction insn) {
            int first = index(insn.getFirstOperand());
            int second = index(insn.getSecondOperand());
            int receiver = index(insn.getReceiver());
            if (isConstant[first] && isConstant[second]) {
                int a = constantValue[first];
                int b = constantValue[second];
                int r;
                switch (insn.getOperation()) {
                    case ADD:
                        r = a + b;
                        break;
                    case SUBTRACT:
                        r = a - b;
                        break;
                    case COMPARE:
                        r = Integer.compare(a, b);
                        break;
                    case DIVIDE:
                        r = a / b;
                        break;
                    case MODULO:
                        r = a % b;
                        break;
                    case MULTIPLY:
                        r = a * b;
                        break;
                    case AND:
                        r = a & b;
                        break;
                    case OR:
                        r = a | b;
                        break;
                    case XOR:
                        r = a ^ b;
                        break;
                    case SHIFT_LEFT:
                        r = a << b;
                        break;
                    case SHIFT_RIGHT:
                        r = a >> b;
                        break;
                    case SHIFT_RIGHT_UNSIGNED:
                        r = a >>> b;
                        break;
                    default:
                        return;
                }

                isConstant[receiver] = true;
                constantValue[receiver] = r;
            } else if (insn.getOperation() == BinaryOperation.COMPARE
                    && insn.getOperandType() == NumericOperandType.INT) {
                comparisonLeft[receiver] = first;
                comparisonRight[receiver] = second;
            }
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            int size = index(insn.getSize());
            int receiver = index(insn.getReceiver());
            if (isConstant[size]) {
                isConstantSizedArray[receiver] = true;
                constantValue[receiver] = constantValue[size];
            }
            arrayLengthVars[size] = receiver;
            arrayLengthReverseVars[receiver] = size;
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            int array = index(insn.getArray());
            int receiver = index(insn.getReceiver());
            if (arrayLengthVars[receiver] >= 0) {
                map[receiver] = arrayLengthReverseVars[receiver];
            } else {
                if (isConstantSizedArray[array]) {
                    isConstant[receiver] = true;
                    constantValue[receiver] = constantValue[array];
                }
                arrayLengthVars[receiver] = array;
                arrayLengthReverseVars[array] = receiver;
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            assign(insn.getAssignee(), insn.getReceiver());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            assign(insn.getArray(), insn.getReceiver());
        }

        private void assign(Variable from, Variable to) {
            map[to.getIndex()] = map[from.getIndex()];
        }

        @Override
        public void visit(GetElementInstruction insn) {
            tryInsertBoundCheck(insn.getIndex(), insn.getArray(), insn);
        }

        @Override
        public void visit(PutElementInstruction insn) {
            tryInsertBoundCheck(insn.getIndex(), insn.getArray(), insn);
        }

        private void tryInsertBoundCheck(Variable indexVar, Variable arrayVar, Instruction instruction) {
            boolean lower = true;
            boolean upper = true;
            int index = index(indexVar);
            int array = index(arrayVar);
            if (isConstant[index]) {
                if (isConstantSizedArray[array]) {
                    upper = false;
                }
            }
            if (upper) {
                IntSet bounds = upperArrayLengths[index];
                if (bounds != null && bounds.contains(array)) {
                    upper = false;
                }
            }

            if ((isConstant[index] && constantValue[index] >= 0) || nonNegative[index]) {
                lower = false;
            }

            if (upper) {
                addArrayBound(index, array);
            }
            markAsNonNegative(index);

            if (lower || upper) {
                BoundCheckInstruction boundCheck = new BoundCheckInstruction();
                if (lower) {
                    boundCheck.setLower(true);
                }
                if (upper) {
                    boundCheck.setArray(arrayVar);
                }
                boundCheck.setIndex(indexVar);
                boundCheck.setReceiver(indexVar);
                boundCheck.setLocation(instruction.getLocation());
                instruction.insertPrevious(boundCheck);
                changed = true;
            }
        }

        private void addArrayBound(int index, int array) {
            IntSet upperSet = upperArrayLengths[index];
            if (upperSet == null) {
                upperSet = new IntHashSet();
                upperArrayLengths[index] = upperSet;
            }
            if (upperSet.add(array)) {
                bounds.addedArrayBounds.add(index, array);
            }
        }

        private void markAsNonNegative(int index) {
            if (!nonNegative[index]) {
                nonNegative[index] = true;
                bounds.nonNegatives.add(index);
            }
        }

        private int index(Variable var) {
            return map[var.getIndex()];
        }
    }

    static class BlockBounds {
        IntArrayList addedArrayBounds = new IntArrayList();
        IntArrayList nonNegatives = new IntArrayList();

        int conditionBlock;
        int comparisonValue;
        int comparisonVariable;
        ComparisonMode comparisonMode;
    }

    enum ComparisonMode {
        LESS_THAN_ARRAY_LENGTH,
        NON_NEGATIVE
    }
}
