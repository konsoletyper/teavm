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
package org.teavm.metaprogramming.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.common.DisjointSet;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.PhiReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.TryCatchJointReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.SwitchTableEntryReader;

class AliasFinder {
    private int[] aliases;
    private ArrayElement[] arrayElements;

    void findAliases(ProgramReader program) {
        DisjointSet set = new DisjointSet();
        for (int i = 0; i < program.variableCount(); ++i) {
            set.create();
        }

        AliasReader reader = new AliasReader(set, program.variableCount());
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            block.readAllInstructions(reader);
            for (PhiReader phi : block.readPhis()) {
                Set<Integer> inputs = phi.readIncomings().stream()
                        .map(incoming -> incoming.getValue().getIndex())
                        .collect(Collectors.toSet());
                if (inputs.size() == 1) {
                    set.union(inputs.iterator().next(), phi.getReceiver().getIndex());
                }
            }
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                for (TryCatchJointReader joint : tryCatch.readJoints()) {
                    Set<Integer> inputs = joint.readSourceVariables().stream()
                            .map(sourceVar -> sourceVar.getIndex())
                            .collect(Collectors.toSet());
                    if (inputs.size() == 1) {
                        set.union(inputs.iterator().next(), joint.getReceiver().getIndex());
                    }
                }
            }
        }

        int[] map = new int[set.size()];
        Arrays.fill(map, -1);
        int[] variables = new int[program.variableCount()];
        for (int i = 0; i < variables.length; ++i) {
            int v = set.find(i);
            int master = map[v];
            if (master == -1) {
                master = i;
                map[v] = master;
            }
            variables[i] = master;
        }

        aliases = variables;
        Object[] constants = reader.constants;
        arrayElements = reader.arrayElements;

        for (int i = 0; i < arrayElements.length; ++i) {
            ArrayElement elem = arrayElements[i];
            if (elem == null) {
                continue;
            }
            elem.index = aliases[elem.index];
            if (constants[elem.index] instanceof Integer) {
                elem.index = (Integer) constants[elem.index];
            } else {
                arrayElements[i] = null;
            }
        }
    }

    static class ArrayElement {
        public int array;
        public int index;
    }

    int[] getAliases() {
        return aliases.clone();
    }

    ArrayElement[] getArrayElements() {
        return arrayElements.clone();
    }

    private static class AliasReader implements InstructionReader {
        DisjointSet disjointSet;
        Object[] constants;
        ArrayElement[] arrayElements;

        private AliasReader(DisjointSet disjointSet, int variableCount) {
            this.disjointSet = disjointSet;
            this.constants = new Object[variableCount];
            this.arrayElements = new ArrayElement[variableCount];
        }

        @Override
        public void location(TextLocation location) {
        }

        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void nullConstant(VariableReader receiver) {
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            constants[receiver.getIndex()] = cst;
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            disjointSet.union(receiver.getIndex(), assignee.getIndex());
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
        }

        @Override
        public void jump(BasicBlockReader target) {
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
        }

        @Override
        public void exit(VariableReader valueToReturn) {
        }

        @Override
        public void raise(VariableReader exception) {
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
        }

        @Override
        public void create(VariableReader receiver, String type) {
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            disjointSet.union(receiver.getIndex(), array.getIndex());
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            ArrayElement elem = new ArrayElement();
            elem.array = array.getIndex();
            elem.index = index.getIndex();
            arrayElements[receiver.getIndex()] = elem;
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType type) {
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        }

        @Override
        public void initClass(String className) {
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
        }
    }
}
