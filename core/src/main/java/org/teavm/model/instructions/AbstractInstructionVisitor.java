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
package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;

public abstract class AbstractInstructionVisitor implements InstructionVisitor {
    @Override
    public void visit(EmptyInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(BinaryInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(NegateInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(AssignInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(CastInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(BranchingInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(JumpInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(SwitchInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ExitInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(RaiseInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ConstructInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(GetElementInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(PutElementInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(InvokeInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(InitClassInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        visitDefault(insn);
    }

    @Override
    public void visit(BoundCheckInstruction insn) {
        visitDefault(insn);
    }

    public void visitDefault(Instruction insn) {
    }
}
