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

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DominatorWalker;
import org.teavm.model.util.DominatorWalkerCallback;
import org.teavm.model.util.PhiUpdater;

class NullnessInformationBuilder {
    private Program program;
    private MethodDescriptor methodDescriptor;
    BitSet notNullVariables = new BitSet();
    BitSet nullVariables = new BitSet();
    BitSet synthesizedVariables = new BitSet();
    PhiUpdater phiUpdater;
    private List<NullConstantInstruction> nullInstructions = new ArrayList<>();
    private List<NullCheckInstruction> notNullInstructions = new ArrayList<>();
    private Graph assignmentGraph;
    private int[] notNullPredecessorsLeft;
    private int[] sccIndexes;

    NullnessInformationBuilder(Program program, MethodDescriptor methodDescriptor) {
        this.program = program;
        this.methodDescriptor = methodDescriptor;
    }

    void build() {
        extendProgram();
        buildAssignmentGraph();
        propagateNullness();
    }

    private void extendProgram() {
        notNullVariables.set(0);
        insertAdditionalVariables();

        Variable[] parameters = new Variable[methodDescriptor.parameterCount() + 1];
        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = program.variableAt(i);
        }
        phiUpdater = new PhiUpdater();
        phiUpdater.updatePhis(program, parameters);

        collectAdditionalVariables();
    }

    private void insertAdditionalVariables() {
        DominatorWalker walker = new DominatorWalker(program);
        NullExtensionVisitor ev = new NullExtensionVisitor();
        walker.walk(ev);
    }

    private void collectAdditionalVariables() {
        for (NullConstantInstruction nullInstruction : nullInstructions) {
            nullVariables.set(nullInstruction.getReceiver().getIndex());
            synthesizedVariables.set(nullInstruction.getReceiver().getIndex());
        }
        for (NullCheckInstruction notNullInstruction : notNullInstructions) {
            notNullVariables.set(notNullInstruction.getReceiver().getIndex());
            synthesizedVariables.set(notNullInstruction.getReceiver().getIndex());
        }

        nullInstructions.clear();
        notNullInstructions.clear();
    }

    private void buildAssignmentGraph() {
        GraphBuilder builder = new GraphBuilder(program.variableCount());

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }

            for (Instruction instruction : block) {
                if (instruction instanceof AssignInstruction) {
                    AssignInstruction assignment = (AssignInstruction) instruction;
                    builder.addEdge(assignment.getAssignee().getIndex(), assignment.getReceiver().getIndex());
                }
            }
        }
        assignmentGraph = builder.build();

        sccIndexes = new int[program.variableCount()];
        if (assignmentGraph.size() > 0) {
            int[][] sccs = GraphUtils.findStronglyConnectedComponents(assignmentGraph, new int[]{0});
            for (int i = 0; i < sccs.length; ++i) {
                for (int sccNode : sccs[i]) {
                    sccIndexes[sccNode] = i + 1;
                }
            }
        }

        notNullPredecessorsLeft = new int[assignmentGraph.size()];
        for (int i = 0; i < assignmentGraph.size(); ++i) {
            notNullPredecessorsLeft[i] = assignmentGraph.incomingEdgesCount(i);
            if (sccIndexes[i] > 0) {
                for (int predecessor : assignmentGraph.outgoingEdges(i)) {
                    if (sccIndexes[predecessor] == sccIndexes[i]) {
                        notNullPredecessorsLeft[i]--;
                    }
                }
            }
        }
    }

    private void propagateNullness() {
        if (assignmentGraph.size() == 0) {
            return;
        }

        IntDeque deque = new IntArrayDeque();
        for (int i = notNullVariables.nextSetBit(0); i >= 0; i = notNullVariables.nextSetBit(i + 1)) {
            deque.addLast(i);
        }
        boolean[] visited = new boolean[program.variableCount()];

        while (!deque.isEmpty()) {
            int node = deque.removeFirst();
            if (visited[node]) {
                continue;
            }
            visited[node] = true;
            notNullVariables.set(node);
            for (int successor : assignmentGraph.outgoingEdges(node)) {
                if (sccIndexes[successor] == 0 || sccIndexes[successor] != sccIndexes[node]) {
                    if (--notNullPredecessorsLeft[successor] == 0) {
                        deque.addLast(successor);
                    }
                }
            }
        }
    }

    class NullExtensionVisitor extends AbstractInstructionVisitor implements DominatorWalkerCallback<State> {
        State currentState;
        BasicBlock currentBlock;
        IntIntMap nullSuccessors = new IntIntOpenHashMap();
        IntIntMap notNullSuccessors = new IntIntOpenHashMap();
        private DominatorTree dom;

        @Override
        public void setDomTree(DominatorTree domTree) {
            dom = domTree;
        }

        @Override
        public State visit(BasicBlock block) {
            currentState = new State();

            if (block.getExceptionVariable() != null) {
                notNullVariables.set(block.getExceptionVariable().getIndex());
            }

            currentBlock = block;
            if (nullSuccessors.containsKey(block.getIndex())) {
                int varIndex = nullSuccessors.remove(block.getIndex());
                insertNullInstruction(program.variableAt(varIndex));
            }
            if (notNullSuccessors.containsKey(block.getIndex())) {
                int varIndex = notNullSuccessors.remove(block.getIndex());
                insertNotNullInstruction(null, program.variableAt(varIndex));
            }

            for (Instruction insn : block) {
                insn.acceptVisitor(this);
            }

            return currentState;
        }

        @Override
        public void endVisit(BasicBlock block, State state) {
            for (int rollbackToNull : state.newlyNonNull.toArray()) {
                notNullVariables.clear(rollbackToNull);
            }
            for (int rollbackToNotNull : state.newlyNull.toArray()) {
                nullVariables.clear(rollbackToNotNull);
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() != null) {
                insertNotNullInstruction(insn, insn.getInstance());
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null) {
                insertNotNullInstruction(insn, insn.getInstance());
            }
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            insertNotNullInstruction(insn, insn.getArray());
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            insertNotNullInstruction(insn, insn.getArray());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            insertNotNullInstruction(insn, insn.getArray());
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() != null) {
                insertNotNullInstruction(insn, insn.getInstance());
            }
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            insertNotNullInstruction(insn, insn.getObjectRef());
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            insertNotNullInstruction(insn, insn.getObjectRef());
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            nullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(AssignInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex(), notNullVariables.get(insn.getAssignee().getIndex()));
            nullVariables.set(insn.getReceiver().getIndex(), nullVariables.get(insn.getAssignee().getIndex()));
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(BranchingInstruction insn) {
            switch (insn.getCondition()) {
                case NOT_NULL:
                    setNotNullSuccessor(insn.getConsequent(), insn.getOperand());
                    setNullSuccessor(insn.getAlternative(), insn.getOperand());
                    break;
                case NULL:
                    setNullSuccessor(insn.getConsequent(), insn.getOperand());
                    setNotNullSuccessor(insn.getAlternative(), insn.getOperand());
                    break;
                default:
                    break;
            }
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            Variable first = insn.getFirstOperand();
            Variable second = insn.getSecondOperand();
            if (nullVariables.get(first.getIndex())) {
                first = second;
            } else if (!nullVariables.get(second.getIndex())) {
                return;
            }

            switch (insn.getCondition()) {
                case REFERENCE_EQUAL:
                    setNotNullSuccessor(insn.getConsequent(), first);
                    setNullSuccessor(insn.getAlternative(), first);
                    break;
                case REFERENCE_NOT_EQUAL:
                    setNullSuccessor(insn.getConsequent(), first);
                    setNotNullSuccessor(insn.getAlternative(), first);
                    break;
                default:
                    break;
            }
        }

        private void setNullSuccessor(BasicBlock successor, Variable value) {
            if (shouldSetSuccessor(successor, value)) {
                nullSuccessors.put(successor.getIndex(), value.getIndex());
            }
        }

        private void setNotNullSuccessor(BasicBlock successor, Variable value) {
            if (shouldSetSuccessor(successor, value)) {
                notNullSuccessors.put(successor.getIndex(), value.getIndex());
            }
        }

        private boolean shouldSetSuccessor(BasicBlock successor, Variable value) {
            for (Phi phi : successor.getPhis()) {
                if (phi.getIncomings().stream().anyMatch(incoming -> incoming.getValue() == value)) {
                    return false;
                }
            }
            return true;
        }

        private void insertNotNullInstruction(Instruction currentInstruction, Variable var) {
            if (notNullVariables.get(var.getIndex())) {
                return;
            }
            NullCheckInstruction insn = new NullCheckInstruction();
            insn.setReceiver(var);
            insn.setValue(var);
            notNullInstructions.add(insn);
            if (currentInstruction != null) {
                currentInstruction.insertNext(insn);
            } else {
                currentBlock.addFirst(insn);
            }
            markAsNonNull(var);
        }

        private void insertNullInstruction(Variable var) {
            if (nullVariables.get(var.getIndex())) {
                return;
            }
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(var);
            nullInstructions.add(insn);
            currentBlock.addFirst(insn);
            markAsNull(var);
        }

        private void markAsNonNull(Variable var) {
            if (notNullVariables.get(var.getIndex())) {
                return;
            }
            notNullVariables.set(var.getIndex());
            currentState.newlyNonNull.add(var.getIndex());
        }

        private void markAsNull(Variable var) {
            if (nullVariables.get(var.getIndex())) {
                return;
            }
            nullVariables.set(var.getIndex());
            currentState.newlyNull.add(var.getIndex());
        }
    }

    static class State {
        IntSet newlyNonNull = new IntOpenHashSet();
        IntSet newlyNull = new IntOpenHashSet();
    }
}
