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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.util.DominatorWalker;
import org.teavm.model.util.DominatorWalkerCallback;
import org.teavm.model.util.DominatorWalkerContext;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeObject;

public class WriteBarrierInsertion {
    private static final MethodReference BARRIER_METHOD = new MethodReference(GC.class, "writeBarrier",
            RuntimeObject.class, void.class);
    private Characteristics characteristics;

    public WriteBarrierInsertion(Characteristics characteristics) {
        this.characteristics = characteristics;
    }

    public void apply(Program program) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        new DominatorWalker(program).walk(new WalkerCallbackImpl(program.variableCount()));
    }

    class WalkerCallbackImpl extends AbstractInstructionVisitor implements DominatorWalkerCallback<State> {
        private DominatorWalkerContext context;
        private boolean[] constantVariables;
        IntHashSet installedBarriers = new IntHashSet();
        State state;

        WalkerCallbackImpl(int variableCount) {
            constantVariables = new boolean[variableCount];
        }

        @Override
        public void setContext(DominatorWalkerContext context) {
            this.context = context;
        }

        @Override
        public State visit(BasicBlock block) {
            state = new State();

            if (context.isExceptionHandler(block.getIndex()) || !context.allPredecessorsVisited(block.getIndex())) {
                invalidateBarriers();
            } else {
                for (Phi phi : block.getPhis()) {
                    if (phi.getIncomings().stream().allMatch(
                            incoming -> installedBarriers.contains(incoming.getValue().getIndex()))) {
                        markAsInstalled(phi.getReceiver().getIndex());
                    }
                }
            }

            for (Instruction instruction : block) {
                instruction.acceptVisitor(this);
            }

            return state;
        }

        @Override
        public void endVisit(BasicBlock block, State state) {
            if (state.oldBarriers != null) {
                installedBarriers.clear();
                installedBarriers.addAll(state.oldBarriers);
            } else {
                for (IntCursor cursor : state.newBarriers) {
                    installedBarriers.remove(cursor.value);
                }
            }
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            constantVariables[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            constantVariables[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            constantVariables[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null && isManagedReferenceType(insn.getFieldType())) {
                installBarrier(insn, insn.getInstance(), insn.getValue());
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(ConstructInstruction insn) {
            invalidateBarriers();
            markAsInstalled(insn.getReceiver().getIndex());
        }

        @Override
        public void visit(InitClassInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            invalidateBarriers();
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            invalidateBarriers();
        }

        private boolean isManagedReferenceType(ValueType type) {
            if (type instanceof ValueType.Array) {
                return true;
            }
            if (type instanceof ValueType.Object) {
                return characteristics.isManaged(((ValueType.Object) type).getClassName());
            }
            return false;
        }

        @Override
        public void visit(PutElementInstruction insn) {
            if (insn.getType() == ArrayElementType.OBJECT) {
                installBarrier(insn, insn.getArray(), insn.getValue());
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            assign(insn.getAssignee(), insn.getReceiver());
        }

        @Override
        public void visit(CastInstruction insn) {
            assign(insn.getValue(), insn.getReceiver());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            assign(insn.getValue(), insn.getReceiver());
        }

        private void assign(Variable from, Variable to) {
            if (installedBarriers.contains(from.getIndex())) {
                markAsInstalled(to.getIndex());
            }
            if (constantVariables[from.getIndex()]) {
                constantVariables[to.getIndex()] = true;
            }
        }

        private void installBarrier(Instruction instruction, Variable variable, Variable value) {
            if (constantVariables[value.getIndex()]) {
                return;
            }
            if (markAsInstalled(variable.getIndex())) {
                InvokeInstruction invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(BARRIER_METHOD);
                invoke.setArguments(variable);
                invoke.setLocation(instruction.getLocation());
                instruction.insertPrevious(invoke);
            }
        }

        private boolean markAsInstalled(int index) {
            if (!installedBarriers.add(index)) {
                return false;
            }
            if (state.newBarriers != null) {
                state.newBarriers.add(index);
            }
            return true;
        }

        private void invalidateBarriers() {
            if (state.newBarriers != null) {
                state.oldBarriers = new IntArrayList();
                for (IntCursor cursor : installedBarriers) {
                    if (!state.newBarriers.contains(cursor.value)) {
                        state.oldBarriers.add(cursor.value);
                    }
                }
                state.newBarriers = null;
            }
            installedBarriers.clear();
        }
    }

    static class State {
        IntSet newBarriers = new IntHashSet();
        IntArrayList oldBarriers;
    }
}
