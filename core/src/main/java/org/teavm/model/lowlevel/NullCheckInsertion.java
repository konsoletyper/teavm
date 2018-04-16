/*
 *  Copyright 2018 Alexey Andreev.
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

import java.util.function.BiConsumer;
import java.util.function.Function;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.PhiUpdater;

public class NullCheckInsertion {
    private Characteristics characteristics;

    public NullCheckInsertion(Characteristics characteristics) {
        this.characteristics = characteristics;
    }

    public void transformProgram(Program program, MethodReference methodReference) {
        if (!characteristics.isManaged(methodReference)) {
            return;
        }

        InsertionVisitor visitor = new InsertionVisitor();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(visitor);
            }
        }
        if (visitor.changed) {
            new PhiUpdater().updatePhis(program, methodReference.parameterCount() + 1);
        }
    }

    class InsertionVisitor extends AbstractInstructionVisitor {
        boolean changed;

        @Override
        public void visit(RaiseInstruction insn) {
            addGuard(insn, RaiseInstruction::getException, RaiseInstruction::setException);
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (!characteristics.isStructure(insn.getField().getClassName())) {
                addGuard(insn, GetFieldInstruction::getInstance, GetFieldInstruction::setInstance);
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (!characteristics.isStructure(insn.getField().getClassName())) {
                addGuard(insn, PutFieldInstruction::getInstance, PutFieldInstruction::setInstance);
            }
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            addGuard(insn, CloneArrayInstruction::getArray, CloneArrayInstruction::setArray);
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            addGuard(insn, UnwrapArrayInstruction::getArray, UnwrapArrayInstruction::setArray);
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (!characteristics.isStructure(insn.getMethod().getClassName())
                    && characteristics.isManaged(insn.getMethod())) {
                addGuard(insn, InvokeInstruction::getInstance, InvokeInstruction::setInstance);
            }
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            addGuard(insn, MonitorEnterInstruction::getObjectRef, MonitorEnterInstruction::setObjectRef);
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            addGuard(insn, MonitorExitInstruction::getObjectRef, MonitorExitInstruction::setObjectRef);
        }

        private <T extends Instruction> void addGuard(T instruction, Function<T, Variable> get,
                BiConsumer<T, Variable> set) {
            Variable value = get.apply(instruction);
            if (value == null) {
                return;
            }

            NullCheckInstruction nullCheck = new NullCheckInstruction();
            nullCheck.setValue(value);
            nullCheck.setReceiver(value);
            nullCheck.setLocation(instruction.getLocation());
            set.accept(instruction, nullCheck.getReceiver());
            instruction.insertPrevious(nullCheck);
            changed = true;
        }
    }
}
