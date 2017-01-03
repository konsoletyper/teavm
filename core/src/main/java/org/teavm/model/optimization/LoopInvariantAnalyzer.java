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
package org.teavm.model.optimization;

import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.analysis.NullnessInformation;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class LoopInvariantAnalyzer implements InstructionVisitor {
    private NullnessInformation nullness;
    public boolean canMove;
    public boolean constant;
    public boolean sideEffect;

    public LoopInvariantAnalyzer(NullnessInformation nullness) {
        this.nullness = nullness;
    }

    public void reset() {
        canMove = false;
        constant = false;
        sideEffect = false;
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        constant = true;
    }

    @Override
    public void visit(BinaryInstruction insn) {
        canMove = true;
        if (insn.getOperation() == BinaryOperation.DIVIDE) {
            sideEffect = insn.getOperandType() == NumericOperandType.INT
                    || insn.getOperandType() == NumericOperandType.LONG;
        }
    }

    @Override
    public void visit(NegateInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(AssignInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(CastInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(BranchingInstruction insn) {
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
    }

    @Override
    public void visit(JumpInstruction insn) {
    }

    @Override
    public void visit(SwitchInstruction insn) {
    }

    @Override
    public void visit(ExitInstruction insn) {
    }

    @Override
    public void visit(RaiseInstruction insn) {
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
    }

    @Override
    public void visit(ConstructInstruction insn) {
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
    }

    @Override
    public void visit(GetFieldInstruction insn) {
    }

    @Override
    public void visit(PutFieldInstruction insn) {
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        canMove = true;
        if (!nullness.isNotNull(insn.getArray())) {
            sideEffect = true;
        }
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        canMove = true;
        if (!nullness.isNotNull(insn.getArray())) {
            sideEffect = true;
        }
    }

    @Override
    public void visit(GetElementInstruction insn) {
    }

    @Override
    public void visit(PutElementInstruction insn) {
    }

    @Override
    public void visit(InvokeInstruction insn) {
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        canMove = true;
    }

    @Override
    public void visit(InitClassInstruction insn) {
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        canMove = true;
        if (!nullness.isNotNull(insn.getValue())) {
            sideEffect = true;
        }
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
    }
}
