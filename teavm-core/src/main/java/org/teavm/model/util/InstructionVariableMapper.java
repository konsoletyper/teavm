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

import org.teavm.model.Variable;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public abstract class InstructionVariableMapper implements InstructionVisitor {
    protected abstract Variable map(Variable var);

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(BinaryInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setFirstOperand(map(insn.getFirstOperand()));
        insn.setSecondOperand(map(insn.getSecondOperand()));
    }

    @Override
    public void visit(NegateInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setOperand(map(insn.getOperand()));
    }

    @Override
    public void visit(AssignInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setAssignee(map(insn.getAssignee()));
    }

    @Override
    public void visit(CastInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(BranchingInstruction insn) {
        insn.setOperand(map(insn.getOperand()));
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        insn.setFirstOperand(map(insn.getFirstOperand()));
        insn.setSecondOperand(map(insn.getSecondOperand()));
    }

    @Override
    public void visit(JumpInstruction insn) {
    }

    @Override
    public void visit(SwitchInstruction insn) {
        insn.setCondition(map(insn.getCondition()));
    }

    @Override
    public void visit(ExitInstruction insn) {
        if (insn.getValueToReturn() != null) {
            insn.setValueToReturn(map(insn.getValueToReturn()));
        }
    }

    @Override
    public void visit(RaiseInstruction insn) {
        insn.setException(map(insn.getException()));
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setSize(map(insn.getSize()));
    }

    @Override
    public void visit(ConstructInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        for (int i = 0; i < insn.getDimensions().size(); ++i) {
            insn.getDimensions().set(i, map(insn.getDimensions().get(i)));
        }
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        if (insn.getInstance() != null) {
            insn.setInstance(map(insn.getInstance()));
        }
        insn.setReceiver(map(insn.getReceiver()));
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        if (insn.getInstance() != null) {
            insn.setInstance(map(insn.getInstance()));
        }
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setArray(map(insn.getArray()));
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setArray(map(insn.getArray()));
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setArray(map(insn.getArray()));
    }

    @Override
    public void visit(GetElementInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setArray(map(insn.getArray()));
        insn.setIndex(map(insn.getIndex()));
    }

    @Override
    public void visit(PutElementInstruction insn) {
        insn.setValue(map(insn.getValue()));
        insn.setArray(map(insn.getArray()));
        insn.setIndex(map(insn.getIndex()));
    }

    @Override
    public void visit(InvokeInstruction insn) {
        if (insn.getReceiver() != null) {
            insn.setReceiver(map(insn.getReceiver()));
        }
        if (insn.getInstance() != null) {
            insn.setInstance(map(insn.getInstance()));
        }
        for (int i = 0; i < insn.getArguments().size(); ++i) {
            insn.getArguments().set(i, map(insn.getArguments().get(i)));
        }
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(InitClassInstruction insn) {
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        insn.setReceiver(map(insn.getReceiver()));
        insn.setValue(map(insn.getValue()));
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        insn.setObjectRef(map(insn.getObjectRef()));
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        insn.setObjectRef(map(insn.getObjectRef()));
    }
    
    
    
    
}
