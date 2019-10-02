/*
 *  Copyright 2013 Alexey Andreev.
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

import org.teavm.model.InvokeDynamicInstruction;

public interface InstructionVisitor {
    void visit(EmptyInstruction insn);

    void visit(ClassConstantInstruction insn);

    void visit(NullConstantInstruction insn);

    void visit(IntegerConstantInstruction insn);

    void visit(LongConstantInstruction insn);

    void visit(FloatConstantInstruction insn);

    void visit(DoubleConstantInstruction insn);

    void visit(StringConstantInstruction insn);

    void visit(BinaryInstruction insn);

    void visit(NegateInstruction insn);

    void visit(AssignInstruction insn);

    void visit(CastInstruction insn);

    void visit(CastNumberInstruction insn);

    void visit(CastIntegerInstruction insn);

    void visit(BranchingInstruction insn);

    void visit(BinaryBranchingInstruction insn);

    void visit(JumpInstruction insn);

    void visit(SwitchInstruction insn);

    void visit(ExitInstruction insn);

    void visit(RaiseInstruction insn);

    void visit(ConstructArrayInstruction insn);

    void visit(ConstructInstruction insn);

    void visit(ConstructMultiArrayInstruction insn);

    void visit(GetFieldInstruction insn);

    void visit(PutFieldInstruction insn);

    void visit(ArrayLengthInstruction insn);

    void visit(CloneArrayInstruction insn);

    void visit(UnwrapArrayInstruction insn);

    void visit(GetElementInstruction insn);

    void visit(PutElementInstruction insn);

    void visit(InvokeInstruction insn);

    void visit(InvokeDynamicInstruction insn);

    void visit(IsInstanceInstruction insn);

    void visit(InitClassInstruction insn);

    void visit(NullCheckInstruction insn);

    void visit(MonitorEnterInstruction insn);

    void visit(MonitorExitInstruction insn);

    void visit(BoundCheckInstruction insn);
}
