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
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Sigma;
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
    BitSet synthesizedVariables = new BitSet();
    PhiUpdater phiUpdater;
    private List<NullCheckInstruction> notNullInstructions = new ArrayList<>();
    private Graph assignmentGraph;
    private int[] notNullPredecessorsLeft;
    Nullness[] statuses;
    private int[][] variablePairs;

    NullnessInformationBuilder(Program program, MethodDescriptor methodDescriptor) {
        this.program = program;
        this.methodDescriptor = methodDescriptor;
    }

    void build() {
        extendProgram();
        buildVariablePairs();
        buildAssignmentGraph();
        propagateNullness();
    }

    private void buildVariablePairs() {
        List<IntSet> pairsBuilder = new ArrayList<>(
                Collections.nCopies(program.variableCount(), null));

        for (BasicBlock block : program.getBasicBlocks()) {
            Instruction lastInstruction = block.getLastInstruction();
            if (lastInstruction instanceof BinaryBranchingInstruction) {
                BinaryBranchingInstruction branching = (BinaryBranchingInstruction) lastInstruction;
                addVariablePair(pairsBuilder, branching.getFirstOperand(), branching.getSecondOperand());
                addVariablePair(pairsBuilder, branching.getSecondOperand(), branching.getFirstOperand());
            }
        }

        variablePairs = new int[pairsBuilder.size()][];
        for (int i = 0; i < variablePairs.length; ++i) {
            IntSet itemBuilder = pairsBuilder.get(i);
            variablePairs[i] = itemBuilder != null ? itemBuilder.toArray() : null;
        }
    }

    private void addVariablePair(List<IntSet> target, Variable first, Variable second) {
        IntSet pairs = target.get(first.getIndex());
        if (pairs == null) {
            pairs = new IntHashSet();
            target.set(first.getIndex(), pairs);
        }
        pairs.add(second.getIndex());
    }

    private void extendProgram() {
        insertAdditionalVariables();

        phiUpdater = new PhiUpdater();
        phiUpdater.setSigmaPredicate(instruction -> {
            if (instruction instanceof BinaryBranchingInstruction) {
                switch (((BinaryBranchingInstruction) instruction).getCondition()) {
                    case REFERENCE_EQUAL:
                    case REFERENCE_NOT_EQUAL:
                        return true;
                    default:
                        break;
                }
            } else if (instruction instanceof BranchingInstruction) {
                switch (((BranchingInstruction) instruction).getCondition()) {
                    case NULL:
                    case NOT_NULL:
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });
        phiUpdater.updatePhis(program, methodDescriptor.parameterCount() + 1);

        collectAdditionalVariables();
    }

    private void insertAdditionalVariables() {
        DominatorWalker walker = new DominatorWalker(program);
        NullExtensionVisitor ev = new NullExtensionVisitor();
        walker.walk(ev);
    }

    private void collectAdditionalVariables() {
        for (NullCheckInstruction notNullInstruction : notNullInstructions) {
            synthesizedVariables.set(notNullInstruction.getReceiver().getIndex());
        }

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
        assignmentGraph = GraphUtils.removeLoops(builder.build());

        notNullPredecessorsLeft = new int[assignmentGraph.size()];
        for (int i = 0; i < assignmentGraph.size(); ++i) {
            notNullPredecessorsLeft[i] = assignmentGraph.incomingEdgesCount(i);
        }
    }

    private void initNullness(IntDeque queue) {
        NullnessInitVisitor visitor = new NullnessInitVisitor(queue);
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(visitor);
            }

            Instruction last = block.getLastInstruction();
            if (!(last instanceof BranchingInstruction)) {
                continue;
            }
            BranchingInstruction branching = (BranchingInstruction) last;
            Sigma[] sigmas = phiUpdater.getSigmasAt(block.getIndex());
            if (sigmas == null) {
                continue;
            }

            Sigma sigma = null;
            for (int i = 0; i < sigmas.length; ++i) {
                if (sigmas[i].getValue() == branching.getOperand()) {
                    sigma = sigmas[i];
                    break;
                }
            }
            if (sigma == null) {
                continue;
            }

            Variable trueVar;
            Variable falseVar;
            if (sigma.getOutgoings().get(0).getTarget() == branching.getConsequent()) {
                trueVar = sigma.getOutgoings().get(0).getValue();
                falseVar = sigma.getOutgoings().get(1).getValue();
            } else {
                trueVar = sigma.getOutgoings().get(1).getValue();
                falseVar = sigma.getOutgoings().get(0).getValue();
            }

            switch (branching.getCondition()) {
                case NULL:
                    queue.addLast(trueVar.getIndex());
                    queue.addLast(0);
                    queue.addLast(falseVar.getIndex());
                    queue.addLast(1);
                    break;
                case NOT_NULL:
                    queue.addLast(trueVar.getIndex());
                    queue.addLast(1);
                    queue.addLast(falseVar.getIndex());
                    queue.addLast(0);
                    break;
                default:
                    break;
            }
        }

        queue.addLast(0);
        queue.addLast(1);
    }

    private void propagateNullness() {
        statuses = new Nullness[program.variableCount()];

        IntDeque deque = new IntArrayDeque();
        initNullness(deque);

        while (!deque.isEmpty()) {
            int node = deque.removeFirst();
            if (statuses[node] != null) {
                deque.removeFirst();
                continue;
            }
            Nullness status = deque.removeFirst() == 1 ? Nullness.NOT_NULL : Nullness.NULL;
            statuses[node] = status;

            int[] pairs = variablePairs[node];
            if (pairs != null) {
                int pairStatus = status == Nullness.NULL ? 1 : 0;
                for (int pair : pairs) {
                    deque.addLast(pair);
                    deque.addLast(pairStatus);
                }
            }

            successors: for (int successor : assignmentGraph.outgoingEdges(node)) {
                if (--notNullPredecessorsLeft[successor] != 0) {
                    continue;
                }

                for (int sibling : assignmentGraph.incomingEdges(successor)) {
                    if (statuses[sibling] != statuses[node]) {
                        continue successors;
                    }
                }
                deque.addLast(successor);
                deque.addLast(statuses[node] == Nullness.NULL ? 0 : 1);
            }
        }
    }

    class NullExtensionVisitor extends AbstractInstructionVisitor implements DominatorWalkerCallback<State> {
        State currentState;
        BasicBlock currentBlock;
        BitSet notNullVariables = new BitSet();

        NullExtensionVisitor() {
            notNullVariables.set(0);
        }

        @Override
        public State visit(BasicBlock block) {
            currentState = new State();

            if (block.getExceptionVariable() != null) {
                notNullVariables.set(block.getExceptionVariable().getIndex());
            }

            currentBlock = block;
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
            markAsNotNull(insn.getReceiver());
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            markAsNotNull(insn.getReceiver());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            markAsNotNull(insn.getReceiver());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            markAsNotNull(insn.getReceiver());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            markAsNotNull(insn.getReceiver());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            markAsNotNull(insn.getReceiver());
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
            markAsNotNull(var);
            currentState.newlyNonNull.add(var.getIndex());
        }

        private void markAsNotNull(Variable var) {
            notNullVariables.set(var.getIndex());
        }
    }

    static class State {
        IntSet newlyNonNull = new IntHashSet();
    }

    class NullnessInitVisitor extends AbstractInstructionVisitor {
        private IntDeque queue;

        NullnessInitVisitor(IntDeque queue) {
            this.queue = queue;
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(0);
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }

        @Override
        public void visit(ConstructInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            queue.addLast(insn.getReceiver().getIndex());
            queue.addLast(1);
        }
    }

    enum Nullness {
        NULL,
        NOT_NULL
    }
}
