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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.TryCatchJoint;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.PhiUpdater;

class NullnessInformationBuilder {
    private Program program;
    private MethodDescriptor methodDescriptor;
    private BitSet notNullVariables = new BitSet();
    private BitSet nullVariables = new BitSet();
    private BitSet synthesizedVariables = new BitSet();
    private PhiUpdater phiUpdater;
    private List<NullConstantInstruction> nullInstructions = new ArrayList<>();
    private List<NullCheckInstruction> notNullInstructions = new ArrayList<>();
    private Graph assignmentGraph;
    private int[] nullPredecessorsLeft;
    private int[] notNullPredecessorsLeft;

    NullnessInformationBuilder(Program program, MethodDescriptor methodDescriptor) {
        this.program = program;
        this.methodDescriptor = methodDescriptor;
    }

    void build() {
        extendProgram();
        buildAssignmentGraph();
        findKnownNullness();
    }

    private void extendProgram() {
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
        NullExtensionVisitor ev = new NullExtensionVisitor();
        for (BasicBlock block : program.getBasicBlocks()) {
            Instruction lastInstruction = block.getLastInstruction();
            if (lastInstruction instanceof BranchingInstruction) {
                BranchingInstruction branching = (BranchingInstruction) lastInstruction;
                if (branching.getCondition() == BranchingCondition.NULL) {
                    insertNullAndNotNull(branching.getOperand(), branching.getConsequent(), branching.getAlternative());
                } else if (branching.getCondition() == BranchingCondition.NOT_NULL) {
                    insertNullAndNotNull(branching.getOperand(), branching.getAlternative(), branching.getConsequent());
                }
            }

            for (Instruction instruction = block.getFirstInstruction(); instruction != null;) {
                Instruction next = instruction.getNext();
                instruction.acceptVisitor(ev);
                instruction = next;
            }
        }
    }

    private void collectAdditionalVariables() {
        for (NullConstantInstruction nullInstruction : nullInstructions) {
            nullVariables.set(nullInstruction.getReceiver().getIndex());
        }
        for (NullCheckInstruction notNullInstruction : notNullInstructions) {
            notNullVariables.set(notNullInstruction.getReceiver().getIndex());
        }
        synthesizedVariables.or(nullVariables);
        synthesizedVariables.or(notNullVariables);

        nullInstructions.clear();
        notNullInstructions.clear();
    }

    private void insertNullAndNotNull(Variable variable, BasicBlock nullBlock, BasicBlock notNullBlock) {
        NullCheckInstruction notNullInstruction = new NullCheckInstruction();
        notNullInstruction.setValue(variable);
        notNullInstruction.setReceiver(variable);
        notNullBlock.addFirst(notNullInstruction);
        notNullInstructions.add(notNullInstruction);

        NullConstantInstruction nullInstruction = new NullConstantInstruction();
        nullInstruction.setReceiver(variable);
        nullBlock.addFirst(nullInstruction);
        nullInstructions.add(nullInstruction);
    }

    private void buildAssignmentGraph() {
        GraphBuilder builder = new GraphBuilder();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    builder.addEdge(incoming.getSource().getIndex(), phi.getReceiver().getIndex());
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                for (TryCatchJoint joint : tryCatch.getJoints()) {
                    for (Variable sourceVar : joint.getSourceVariables()) {
                        builder.addEdge(sourceVar.getIndex(), joint.getReceiver().getIndex());
                    }
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

        // TODO: handle SCCs

        nullPredecessorsLeft = new int[assignmentGraph.size()];
        notNullPredecessorsLeft = new int[assignmentGraph.size()];
        for (int i = 0; i < assignmentGraph.size(); ++i) {
            nullPredecessorsLeft[i] = assignmentGraph.incomingEdgesCount(i);
            notNullPredecessorsLeft[i] = assignmentGraph.incomingEdgesCount(i);
        }
    }

    private void findKnownNullness() {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(nullnessVisitor);
            }
        }
    }

    class NullExtensionVisitor extends AbstractInstructionVisitor {
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

        private void insertNotNullInstruction(Instruction currentInstruction, Variable var) {
            NullCheckInstruction insn = new NullCheckInstruction();
            insn.setReceiver(var);
            insn.setValue(var);
            notNullInstructions.add(insn);
            currentInstruction.insertNext(insn);
        }
    }

    private InstructionVisitor nullnessVisitor = new AbstractInstructionVisitor() {
        @Override
        public void visit(ClassConstantInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            nullVariables.set(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            notNullVariables.set(insn.getReceiver().getIndex());
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
            super.visit(insn);
        }
    };
}
