/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.model.util;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class AssignmentExtractor extends AbstractInstructionVisitor {
    private Variable result;

    public Variable getResult() {
        return result;
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(CastInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(AssignInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(BinaryInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(InvokeInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(NegateInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(BoundCheckInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(GetElementInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(ConstructInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        result = insn.getReceiver();
    }

    @Override
    public void visitDefault(Instruction insn) {
        result = null;
    }
}
