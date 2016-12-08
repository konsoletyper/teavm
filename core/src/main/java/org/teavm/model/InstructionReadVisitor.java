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
package org.teavm.model;

import java.util.Collections;
import org.teavm.model.instructions.*;

public class InstructionReadVisitor implements InstructionVisitor {
    InstructionReader reader;

    public InstructionReadVisitor(InstructionReader reader) {
        this.reader = reader;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        reader.nop();
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        reader.classConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        reader.nullConstant(insn.getReceiver());
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        reader.integerConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        reader.longConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        reader.floatConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        reader.doubleConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        reader.stringConstant(insn.getReceiver(), insn.getConstant());
    }

    @Override
    public void visit(BinaryInstruction insn) {
        reader.binary(insn.getOperation(), insn.getReceiver(), insn.getFirstOperand(), insn.getSecondOperand(),
                insn.getOperandType());
    }

    @Override
    public void visit(NegateInstruction insn) {
        reader.negate(insn.getReceiver(), insn.getOperand(), insn.getOperandType());
    }

    @Override
    public void visit(AssignInstruction insn) {
        reader.assign(insn.getReceiver(), insn.getAssignee());
    }

    @Override
    public void visit(CastInstruction insn) {
        reader.cast(insn.getReceiver(), insn.getValue(), insn.getTargetType());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        reader.cast(insn.getReceiver(), insn.getValue(), insn.getSourceType(), insn.getTargetType());
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        reader.cast(insn.getReceiver(), insn.getValue(), insn.getTargetType(), insn.getDirection());
    }

    @Override
    public void visit(BranchingInstruction insn) {
        reader.jumpIf(insn.getCondition(), insn.getOperand(), insn.getConsequent(), insn.getAlternative());
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        reader.jumpIf(insn.getCondition(), insn.getFirstOperand(), insn.getSecondOperand(), insn.getConsequent(),
                insn.getAlternative());
    }

    @Override
    public void visit(JumpInstruction insn) {
        reader.jump(insn.getTarget());
    }

    @Override
    public void visit(SwitchInstruction insn) {
        reader.choose(insn.getCondition(), Collections.unmodifiableList(insn.getEntries()), insn.getDefaultTarget());
    }

    @Override
    public void visit(ExitInstruction insn) {
        reader.exit(insn.getValueToReturn());
    }

    @Override
    public void visit(RaiseInstruction insn) {
        reader.raise(insn.getException());
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        reader.createArray(insn.getReceiver(), insn.getItemType(), insn.getSize());
    }

    @Override
    public void visit(ConstructInstruction insn) {
        reader.create(insn.getReceiver(), insn.getType());
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        reader.createArray(insn.getReceiver(), insn.getItemType(), Collections.unmodifiableList(insn.getDimensions()));
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        reader.getField(insn.getReceiver(), insn.getInstance(), insn.getField(), insn.getFieldType());
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        reader.putField(insn.getInstance(), insn.getField(), insn.getValue(), insn.getFieldType());
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        reader.arrayLength(insn.getReceiver(), insn.getArray());
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        reader.cloneArray(insn.getReceiver(), insn.getArray());
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        reader.unwrapArray(insn.getReceiver(), insn.getArray(), insn.getElementType());
    }

    @Override
    public void visit(GetElementInstruction insn) {
        reader.getElement(insn.getReceiver(), insn.getArray(), insn.getIndex(), insn.getType());
    }

    @Override
    public void visit(PutElementInstruction insn) {
        reader.putElement(insn.getArray(), insn.getIndex(), insn.getValue(), insn.getType());
    }

    @Override
    public void visit(InvokeInstruction insn) {
        reader.invoke(insn.getReceiver(), insn.getInstance(), insn.getMethod(),
                Collections.unmodifiableList(insn.getArguments()), insn.getType());
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        reader.invokeDynamic(insn.getReceiver(), insn.getInstance(), insn.getMethod(),
                Collections.unmodifiableList(insn.getArguments()), insn.getBootstrapMethod(),
                Collections.unmodifiableList(insn.getBootstrapArguments()));
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        reader.isInstance(insn.getReceiver(), insn.getValue(), insn.getType());
    }

    @Override
    public void visit(InitClassInstruction insn) {
        reader.initClass(insn.getClassName());
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        reader.nullCheck(insn.getReceiver(), insn.getValue());
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        reader.monitorEnter(insn.getObjectRef());
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        reader.monitorExit(insn.getObjectRef());
    }
}
