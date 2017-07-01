/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.Variable;
import org.teavm.model.instructions.*;

public class UsageExtractor implements InstructionVisitor {
    private Variable[] usedVariables;

    public Variable[] getUsedVariables() {
        return usedVariables;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(BinaryInstruction insn) {
        usedVariables = new Variable[] { insn.getFirstOperand(), insn.getSecondOperand() };
    }

    @Override
    public void visit(NegateInstruction insn) {
        usedVariables = new Variable[] { insn.getOperand() };
    }

    @Override
    public void visit(AssignInstruction insn) {
        usedVariables = new Variable[] { insn.getAssignee() };
    }

    @Override
    public void visit(CastInstruction insn) {
        usedVariables = new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        usedVariables = new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        usedVariables = new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(BranchingInstruction insn) {
        usedVariables = new Variable[] { insn.getOperand() };
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        usedVariables = new Variable[] { insn.getFirstOperand(), insn.getSecondOperand() };
    }

    @Override
    public void visit(JumpInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(SwitchInstruction insn) {
        usedVariables = new Variable[] { insn.getCondition() };
    }

    @Override
    public void visit(ExitInstruction insn) {
        usedVariables = insn.getValueToReturn() != null ? new Variable[] { insn.getValueToReturn() } : new Variable[0];
    }

    @Override
    public void visit(RaiseInstruction insn) {
        usedVariables = new Variable[] { insn.getException() };
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        usedVariables = new Variable[] { insn.getSize() };
    }

    @Override
    public void visit(ConstructInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        usedVariables = insn.getDimensions().toArray(new Variable[0]);
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        usedVariables = insn.getInstance() != null ? new Variable[] { insn.getInstance() } : new Variable[0];
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        usedVariables = insn.getInstance() != null ? new Variable[] { insn.getInstance(), insn.getValue() }
                : new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        usedVariables = new Variable[] { insn.getArray() };
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        usedVariables = new Variable[] { insn.getArray() };
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        usedVariables = new Variable[] { insn.getArray() };
    }

    @Override
    public void visit(GetElementInstruction insn) {
        usedVariables = new Variable[] { insn.getArray(), insn.getIndex() };
    }

    @Override
    public void visit(PutElementInstruction insn) {
        usedVariables = new Variable[] { insn.getArray(), insn.getIndex(), insn.getValue() };
    }

    @Override
    public void visit(InvokeInstruction insn) {
        if (insn.getInstance() != null) {
            usedVariables = new Variable[insn.getArguments().size() + 1];
            insn.getArguments().toArray(usedVariables);
            usedVariables[insn.getArguments().size()] = insn.getInstance();
        } else {
            usedVariables = new Variable[insn.getArguments().size()];
            insn.getArguments().toArray(usedVariables);
        }
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        if (insn.getInstance() != null) {
            usedVariables = new Variable[insn.getArguments().size() + 1];
            insn.getArguments().toArray(usedVariables);
            usedVariables[insn.getArguments().size()] = insn.getInstance();
        } else {
            usedVariables = new Variable[insn.getArguments().size()];
            insn.getArguments().toArray(usedVariables);
        }
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        usedVariables = new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(InitClassInstruction insn) {
        usedVariables = new Variable[0];
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        usedVariables = new Variable[] { insn.getValue() };
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        usedVariables = new Variable[] {insn.getObjectRef() };
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        usedVariables = new Variable[] {insn.getObjectRef() };
    }
}
