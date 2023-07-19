/*
 *  Copyright 2023 Alexey Andreev.
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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntStack;
import java.util.Arrays;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class SystemArrayCopyOptimization implements MethodOptimization {
    private static final MethodReference ARRAY_COPY_METHOD = new MethodReference(System.class,
            "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private static final MethodReference FAST_ARRAY_COPY_METHOD = new MethodReference(System.class,
            "fastArraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        TypeInference typeInference = null;
        var somethingChanged = false;
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    var invoke = (InvokeInstruction) instruction;
                    var method = invoke.getMethod();
                    if (method.equals(ARRAY_COPY_METHOD)) {
                        if (typeInference == null) {
                            typeInference = new TypeInference(program, context.getMethod().getDescriptor());
                        }
                        var sourceType = typeInference.typeOf(invoke.getArguments().get(0));
                        var destType = typeInference.typeOf(invoke.getArguments().get(2));
                        if (sourceType != null && destType != null) {
                            if (sourceType.equals(destType)
                                    || context.getHierarchy().isSuperType(destType, sourceType, false)) {
                                invoke.setMethod(FAST_ARRAY_COPY_METHOD);
                                somethingChanged = true;
                            }
                        }
                    }
                }
            }
        }
        return somethingChanged;
    }

    private static class TypeInference {
        private ValueType[] types;
        private int[] assignments;
        private int[] elementAssignments;
        private int[][] phis;
        private boolean[] present;
        private boolean[] calculating;

        TypeInference(Program program, MethodDescriptor descriptor) {
            types = new ValueType[program.variableCount()];
            assignments = new int[program.variableCount()];
            elementAssignments = new int[program.variableCount()];
            phis = new int[program.variableCount()][];
            Arrays.fill(assignments, -1);
            Arrays.fill(elementAssignments, -1);
            present = new boolean[program.variableCount()];
            calculating = new boolean[program.variableCount()];

            var visitor = new InitialTypeVisitor(types, assignments, elementAssignments);
            var params = Math.min(descriptor.parameterCount(), program.variableCount() - 1);
            for (var i = 0; i < params; ++i) {
                visitor.type(program.variableAt(i + 1), descriptor.parameterType(i));
            }
            for (var block : program.getBasicBlocks()) {
                for (var insn : block) {
                    insn.acceptVisitor(visitor);
                }
                for (var phi : block.getPhis()) {
                    var sourceIndexes = new IntHashSet();
                    for (var incoming : phi.getIncomings()) {
                        sourceIndexes.add(incoming.getValue().getIndex());
                    }
                    var inputs = sourceIndexes.toArray();
                    Arrays.sort(inputs);
                    phis[phi.getReceiver().getIndex()] = inputs;
                }
            }

            for (int i = 0; i < types.length; ++i) {
                if (types[i] != null) {
                    present[i] = true;
                }
            }
        }

        ValueType typeOf(Variable variable) {
            if (!present[variable.getIndex()]) {
                calculate(variable.getIndex());
            }
            return types[variable.getIndex()];
        }

        private void calculate(int initialVariable) {
            var stack = new IntStack();
            stack.push(initialVariable);
            while (!stack.isEmpty()) {
                var variable = stack.pop();
                if (calculating[variable]) {
                    calculating[variable] = false;
                    present[variable] = true;
                    var inputs = phis[variable];
                    ValueType type;
                    if (inputs != null) {
                        type = null;
                        var initialized = false;
                        for (var input : inputs) {
                            if (calculating[input]) {
                                continue;
                            }
                            if (initialized) {
                                if (!type.equals(types[input])) {
                                    type = null;
                                    break;
                                }
                            } else {
                                type = types[input];
                            }
                        }
                    } else if (assignments[variable] >= 0) {
                        type = types[assignments[variable]];
                    } else if (elementAssignments[variable] >= 0) {
                        type = types[elementAssignments[variable]];
                        type = type instanceof ValueType.Array ? ((ValueType.Array) type).getItemType() : null;
                    } else {
                        type = null;
                    }
                    types[variable] = type;
                } else {
                    calculating[variable] = true;
                    stack.push(variable);
                    var inputs = phis[variable];
                    if (inputs != null) {
                        for (var input : inputs) {
                            if (!calculating[input] && !present[input]) {
                                stack.push(input);
                            }
                        }
                    }
                    var assign = assignments[variable];
                    if (assign >= 0 && !present[assign]) {
                        stack.push(assign);
                    }
                    var elemAssign = elementAssignments[variable];
                    if (elemAssign >= 0 && !present[elemAssign]) {
                        stack.push(elemAssign);
                    }
                }
            }
        }
    }

    private static class InitialTypeVisitor extends AbstractInstructionVisitor {
        private ValueType[] types;
        private int[] assignments;
        private int[] elementAssignments;

        InitialTypeVisitor(ValueType[] types, int[] assignments, int[] elementAssignments) {
            this.types = types;
            this.assignments = assignments;
            this.elementAssignments = elementAssignments;
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            types[insn.getReceiver().getIndex()] = insn.getItemType();
            super.visit(insn);
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            var type = insn.getItemType();
            for (var i = 1; i < insn.getDimensions().size(); ++i) {
                type = ValueType.arrayOf(type);
            }
            types[insn.getReceiver().getIndex()] = insn.getItemType();
        }

        @Override
        public void visit(CastInstruction insn) {
            type(insn.getReceiver(), insn.getTargetType());
        }

        @Override
        public void visit(InvokeInstruction insn) {
            type(insn.getReceiver(), insn.getMethod().getReturnType());
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            type(insn.getReceiver(), insn.getFieldType());
        }

        void type(Variable target, ValueType type) {
            if (target != null && type instanceof ValueType.Array) {
                types[target.getIndex()] = ((ValueType.Array) type).getItemType();
            }
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            assignments[insn.getReceiver().getIndex()] = insn.getArray().getIndex();
        }

        @Override
        public void visit(GetElementInstruction insn) {
            elementAssignments[insn.getReceiver().getIndex()] = insn.getArray().getIndex();
        }

        @Override
        public void visit(AssignInstruction insn) {
            assignments[insn.getReceiver().getIndex()] = insn.getAssignee().getIndex();
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            assignments[insn.getReceiver().getIndex()] = insn.getValue().getIndex();
        }
    }
}
