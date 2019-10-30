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
package org.teavm.model.analysis;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullCheckInstruction;

public class AliasAnalysis {
    private Graph interferenceGraph;
    private boolean[] variablesWithExternalObject;
    private int[] arrayOfVariablesWithExternalObject;

    public void analyze(Program program, MethodDescriptor methodDescriptor) {
        DfgBuildVisitor visitor = prepare(program, methodDescriptor);
        IntSet[] instances = propagate(visitor, program.variableCount());
        buildInterferenceGraph(instances, visitor.constructedObjectCounter);
    }

    public int[] affectedVariables(int variable) {
        return interferenceGraph.outgoingEdges(variable);
    }

    public boolean affectsEverything(int variable) {
        return variablesWithExternalObject[variable];
    }

    public int[] getExternalObjects() {
        return arrayOfVariablesWithExternalObject;
    }

    private DfgBuildVisitor prepare(Program program, MethodDescriptor methodDescriptor) {
        DfgBuildVisitor visitor = new DfgBuildVisitor(program.variableCount());

        for (int i = 1; i <= methodDescriptor.parameterCount(); ++i) {
            if (methodDescriptor.parameterType(i - 1) instanceof ValueType.Object) {
                visitor.queue.addLast(i);
                visitor.queue.addLast(0);
            }
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    visitor.builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
            if (block.getExceptionVariable() != null) {
                visitor.queue.addLast(block.getExceptionVariable().getIndex());
                visitor.queue.addLast(0);
            }
            for (Instruction instruction : block) {
                instruction.acceptVisitor(visitor);
            }
        }

        return visitor;
    }

    private IntSet[] propagate(DfgBuildVisitor visitor, int variableCount) {
        Graph dfg = visitor.builder.build();
        IntDeque queue = visitor.queue;
        IntSet[] instances = new IntSet[variableCount];

        while (!queue.isEmpty()) {
            int v = queue.removeFirst();
            int instance = queue.removeFirst();

            IntSet instancesByVar = instances[v];
            if (instancesByVar == null) {
                instancesByVar = new IntHashSet();
                instances[v] = instancesByVar;
            }

            if (instancesByVar.contains(instance) || instancesByVar.contains(0)) {
                continue;
            }

            if (instance == 0) {
                instancesByVar.clear();
            }
            instancesByVar.add(instance);

            for (int successor : dfg.outgoingEdges(v)) {
                if (instances[successor] == null
                        || (!instances[successor].contains(instance) && !instances[successor].contains(0))) {
                    queue.addLast(successor);
                    queue.addLast(instance);
                }
            }
        }

        return instances;
    }

    private void buildInterferenceGraph(IntSet[] instances, int instanceCount) {
        GraphBuilder builder = new GraphBuilder(instances.length);
        variablesWithExternalObject = new boolean[instances.length];
        IntSet setOfVariablesWithExternalObject = new IntHashSet();

        IntSet[] instanceBackMap = new IntSet[instanceCount];
        for (int i = 0; i < instances.length; i++) {
            IntSet instancesByVar = instances[i];
            if (instancesByVar == null) {
                continue;
            }
            for (IntCursor cursor : instancesByVar) {
                int instance = cursor.value;
                if (instance == 0) {
                    variablesWithExternalObject[i] = true;
                    setOfVariablesWithExternalObject.add(i);
                } else {
                    IntSet variables = instanceBackMap[instance];
                    if (variables == null) {
                        variables = new IntHashSet();
                        instanceBackMap[instance] = variables;
                    }
                    variables.add(i);
                }
            }
        }

        for (int v = 0; v < instances.length; v++) {
            builder.addEdge(v, v);

            IntSet instancesByVar = instances[v];
            if (instancesByVar == null) {
                continue;
            }

            IntSet set;
            if (instancesByVar.size() == 1) {
                set = instanceBackMap[instancesByVar.iterator().next().value];
            } else {
                IntHashSet hashSet = new IntHashSet();
                for (IntCursor cursor : instancesByVar) {
                    hashSet.addAll(instanceBackMap[cursor.value]);
                }
                set = hashSet;
            }

            if (set == null) {
                continue;
            }

            int[] array = set.toArray();
            for (int i = 0; i < array.length - 1; ++i) {
                for (int j = i + 1; j < array.length; ++j) {
                    builder.addEdge(array[i], array[j]);
                    builder.addEdge(array[j], array[i]);
                }
            }
        }

        interferenceGraph = builder.build();
        arrayOfVariablesWithExternalObject = setOfVariablesWithExternalObject.toArray();
    }

    static class DfgBuildVisitor extends AbstractInstructionVisitor {
        GraphBuilder builder;
        int constructedObjectCounter = 1;
        IntDeque queue = new IntArrayDeque();

        DfgBuildVisitor(int variableCount) {
            builder = new GraphBuilder(variableCount);
        }

        @Override
        public void visit(CastInstruction insn) {
            builder.addEdge(insn.getValue().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(AssignInstruction insn) {
            builder.addEdge(insn.getAssignee().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            builder.addEdge(insn.getValue().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(constructedObjectCounter++);
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getReceiver() != null && insn.getMethod().getReturnType() instanceof ValueType.Object) {
                queue.addLast(insn.getReceiver().getIndex());
                queue.addLast(0);
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(0);
        }

        @Override
        public void visit(GetElementInstruction insn) {
            if (insn.getType() == ArrayElementType.OBJECT) {
                queue.addLast(insn.getReceiver().getIndex());
                queue.addLast(0);
            }
        }
    }
}
