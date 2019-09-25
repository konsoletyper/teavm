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
package org.teavm.model.lowlevel;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.interop.Address;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.FieldReference;
import org.teavm.model.IncomingReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PhiReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;

public class NativePointerFinder {
    private Characteristics characteristics;

    public NativePointerFinder(Characteristics characteristics) {
        this.characteristics = characteristics;
    }

    public boolean[] findNativePointers(MethodReference method, ProgramReader program) {
        IntDeque stack = new IntArrayDeque();

        for (int i = 0; i < method.parameterCount(); ++i) {
            if (isNativeType(method.parameterType(i))) {
                stack.addLast(i + 1);
            }
        }

        Analyzer analyzer = new Analyzer(program.variableCount(), stack);
        for (BasicBlockReader block : program.getBasicBlocks()) {
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    analyzer.assignmentGraph.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
            block.readAllInstructions(analyzer);
        }

        boolean[] result = new boolean[program.variableCount()];
        Graph graph = analyzer.assignmentGraph.build();
        while (!stack.isEmpty()) {
            int v = stack.removeLast();
            if (result[v]) {
                continue;
            }
            result[v] = true;

            for (int succ : graph.outgoingEdges(v)) {
                if (!result[succ]) {
                    stack.addLast(succ);
                }
            }
        }

        return result;
    }


    class Analyzer extends AbstractInstructionReader {
        GraphBuilder assignmentGraph;
        IntDeque steps;

        Analyzer(int variableCount, IntDeque steps) {
            assignmentGraph = new GraphBuilder(variableCount);
            this.steps = steps;
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            assignmentGraph.addEdge(assignee.getIndex(), receiver.getIndex());
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            assignmentGraph.addEdge(value.getIndex(), receiver.getIndex());
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (receiver != null && isNativeType(method.getReturnType())) {
                steps.addLast(receiver.getIndex());
            }
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            if (isNativeType(fieldType)) {
                steps.addLast(receiver.getIndex());
            }
        }
    }

    private boolean isNativeType(ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        String className = ((ValueType.Object) type).getClassName();
        return characteristics.isStructure(className) || className.equals(Address.class.getName())
                || characteristics.isFunction(className);
    }
}
