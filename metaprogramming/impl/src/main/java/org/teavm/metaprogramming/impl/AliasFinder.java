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
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.common.DisjointSet;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.PhiReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.ArrayElementType;

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

    private static class AliasReader extends AbstractInstructionReader {
        DisjointSet disjointSet;
        Object[] constants;
        ArrayElement[] arrayElements;

        private AliasReader(DisjointSet disjointSet, int variableCount) {
            this.disjointSet = disjointSet;
            this.constants = new Object[variableCount];
            this.arrayElements = new ArrayElement[variableCount];
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            constants[receiver.getIndex()] = cst;
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
        public void assign(VariableReader receiver, VariableReader assignee) {
            disjointSet.union(receiver.getIndex(), assignee.getIndex());
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
    }
}
