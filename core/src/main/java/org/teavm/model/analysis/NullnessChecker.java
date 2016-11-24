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
package org.teavm.model.analysis;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.util.ProgramUtils;

public class NullnessChecker {
    Set<Incoming> notNullIncomings;
    List<List<Incoming>> phiOutputs;
    boolean[] notNull;
    boolean[] nullLiteral;
    GraphBuilder graphBuilder;

    public boolean[] check(Program program) {
        notNullIncomings = new HashSet<>();
        notNull = new boolean[program.variableCount()];
        nullLiteral = new boolean[program.variableCount()];
        phiOutputs = ProgramUtils.getPhiOutputsByVariable(program);
        graphBuilder = new GraphBuilder(program.variableCount());

        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dom = GraphUtils.buildDominatorTree(cfg);
        Graph domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());

        Step[] stack = new Step[cfg.size() * 2];
        int head = 0;
        stack[head++] = new Step(0, notNull.clone());

        while (head > 0) {
            Step step = stack[--head];
            int node = step.node;

            BasicBlock block = program.basicBlockAt(node);
            InstructionReaderImpl reader = new InstructionReaderImpl(step.nonNull);
            block.readAllInstructions(reader);

            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    graphBuilder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }

            for (int successor : domGraph.outgoingEdges(node)) {
                boolean[] nextNonNull = step.nonNull.clone();
                if (successor == reader.notNullTarget && reader.notNullVariable >= 0) {
                    nextNonNull[reader.notNullVariable] = true;
                }
                stack[head++] = new Step(successor, nextNonNull);
            }
        }

        propagateNullness(graphBuilder.build());

        boolean[] result = notNull;

        notNullIncomings = null;
        notNull = null;
        nullLiteral = null;
        phiOutputs = null;
        graphBuilder = null;

        return result;
    }

    private void propagateNullness(Graph graph) {
        Queue<Integer> worklist = new ArrayDeque<>();
        for (int i = 0; i < notNull.length; ++i) {
            if (notNull[i] && graph.outgoingEdgesCount(i) > 0) {
                for (int j : graph.outgoingEdges(i)) {
                    if (!notNull[j]) {
                        worklist.add(j);
                    }
                }
            }
        }

        while (!worklist.isEmpty()) {
            int node = worklist.remove();
            if (notNull[node]) {
                continue;
            }
            notNull[node] = true;
            for (int next : graph.outgoingEdges(node)) {
                if (!notNull[next]) {
                    worklist.add(next);
                }
            }
        }
    }

    class InstructionReaderImpl extends AbstractInstructionReader {
        boolean[] currentNotNull;
        int notNullTarget;
        int notNullVariable = -1;

        public InstructionReaderImpl(boolean[] currentNotNull) {
            this.currentNotNull = currentNotNull;
        }

        @Override
        public void nullConstant(VariableReader receiver) {
            nullLiteral[receiver.getIndex()] = true;
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            if (currentNotNull[assignee.getIndex()]) {
                notNull[receiver.getIndex()] = true;
            }
            graphBuilder.addEdge(assignee.getIndex(), receiver.getIndex());
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (instance != null) {
                markAsCurrentNotNull(instance);
            }
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            if (instance != null) {
                markAsCurrentNotNull(instance);
            }
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
            if (instance != null) {
                markAsCurrentNotNull(instance);
            }
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType elementType) {
            markAsCurrentNotNull(array);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType elementType) {
            markAsCurrentNotNull(array);
        }

        @Override
        public void create(VariableReader receiver, String type) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            markAsCurrentNotNull(array);
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            notNull[receiver.getIndex()] = true;
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            switch (cond) {
                case NULL:
                    notNullVariable = operand.getIndex();
                    notNullTarget = alternative.getIndex();
                    break;
                case NOT_NULL:
                    notNullVariable = operand.getIndex();
                    notNullTarget = consequent.getIndex();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            switch (cond) {
                case REFERENCE_EQUAL:
                    if (nullLiteral[first.getIndex()]) {
                        notNullVariable = second.getIndex();
                    } else if (nullLiteral[second.getIndex()]) {
                        notNullVariable = first.getIndex();
                    }
                    notNullTarget = alternative.getIndex();
                    break;
                case REFERENCE_NOT_EQUAL:
                    if (nullLiteral[first.getIndex()]) {
                        notNullVariable = second.getIndex();
                    } else if (nullLiteral[second.getIndex()]) {
                        notNullVariable = first.getIndex();
                    }
                    notNullTarget = consequent.getIndex();
                    break;
                default:
                    break;
            }
        }

        private void markAsCurrentNotNull(VariableReader variable) {
            if (!currentNotNull[variable.getIndex()]) {
                currentNotNull[variable.getIndex()] = true;
                List<Incoming> outputs = phiOutputs.get(variable.getIndex());
                if (outputs != null) {
                    for (Incoming output : outputs) {
                        notNullIncomings.add(output);
                    }
                }
            }
        }
    }

    static class Step {
        int node;
        boolean[] nonNull;

        public Step(int node, boolean[] nonNull) {
            this.node = node;
            this.nonNull = nonNull;
        }
    }
}
