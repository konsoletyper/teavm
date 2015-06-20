/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.emit;

import org.teavm.model.BasicBlock;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class ValueEmitter {
    ProgramEmitter pe;
    BasicBlock block;
    Variable variable;

    ValueEmitter(ProgramEmitter programEmitter, BasicBlock block, Variable variable) {
        this.pe = programEmitter;
        this.block = block;
        this.variable = variable;
    }

    public ProgramEmitter getProgramEmitter() {
        return pe;
    }

    public BasicBlock getBlock() {
        return block;
    }

    public Variable getVariable() {
        return variable;
    }

    public ValueEmitter getField(FieldReference field, ValueType type) {
        Variable var = pe.getProgram().createVariable();
        GetFieldInstruction insn = new GetFieldInstruction();
        insn.setField(field);
        insn.setFieldType(type);
        insn.setReceiver(var);
        insn.setInstance(variable);
        pe.addInstruction(insn);
        return pe.var(var);
    }

    public void setField(FieldReference field, ValueType type, ValueEmitter value) {
        PutFieldInstruction insn = new PutFieldInstruction();
        insn.setField(field);
        insn.setFieldType(type);
        insn.setInstance(variable);
        insn.setValue(value.getVariable());
        pe.addInstruction(insn);
    }

    public ValueEmitter binary(BinaryOperation op, NumericOperandType type, ValueEmitter other) {
        Variable var = pe.getProgram().createVariable();
        BinaryInstruction insn = new BinaryInstruction(op, type);
        insn.setFirstOperand(variable);
        insn.setSecondOperand(other.variable);
        insn.setReceiver(var);
        pe.addInstruction(insn);
        return pe.var(var);
    }

    public ValueEmitter add(NumericOperandType type, ValueEmitter other) {
        return binary(BinaryOperation.ADD, type, other);
    }

    public ValueEmitter iadd(ValueEmitter other) {
        return add(NumericOperandType.INT, other);
    }

    public ValueEmitter sub(NumericOperandType type, ValueEmitter other) {
        return binary(BinaryOperation.SUBTRACT, type, other);
    }

    public ValueEmitter isub(ValueEmitter other) {
        return sub(NumericOperandType.INT, other);
    }

    public ValueEmitter compare(NumericOperandType type, ValueEmitter other) {
        return binary(BinaryOperation.COMPARE, type, other);
    }

    public ValueEmitter icompare(ValueEmitter other) {
        return compare(NumericOperandType.INT, other);
    }

    public ValueEmitter neg(NumericOperandType type) {
        Variable var = pe.getProgram().createVariable();
        NegateInstruction insn = new NegateInstruction(type);
        insn.setOperand(variable);
        insn.setReceiver(var);
        return pe.var(var);
    }

    public ValueEmitter ineg() {
        return neg(NumericOperandType.INT);
    }

    public ValueEmitter invoke(InvocationType type, MethodReference method, ValueEmitter... arguments) {
        Variable result = null;
        if (method.getReturnType() != ValueType.VOID) {
            result = pe.getProgram().createVariable();
        }
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(type);
        insn.setMethod(method);
        insn.setInstance(variable);
        insn.setReceiver(result);
        for (ValueEmitter arg : arguments) {
            insn.getArguments().add(arg.variable);
        }
        pe.addInstruction(insn);
        return result != null ? pe.var(result) : null;
    }

    public ValueEmitter invokeSpecial(MethodReference method, ValueEmitter... arguments) {
        return invoke(InvocationType.SPECIAL, method, arguments);
    }

    public ValueEmitter invokeVirtual(MethodReference method, ValueEmitter... arguments) {
        return invoke(InvocationType.VIRTUAL, method, arguments);
    }

    public ValueEmitter join(ValueEmitter other) {
        Variable var = pe.getProgram().createVariable();
        Phi phi = new Phi();
        phi.setReceiver(var);
        Incoming incoming = new Incoming();
        incoming.setSource(block);
        incoming.setValue(variable);
        phi.getIncomings().add(incoming);
        incoming = new Incoming();
        incoming.setSource(other.block);
        incoming.setValue(other.variable);
        phi.getIncomings().add(incoming);
        pe.getBlock().getPhis().add(phi);
        return new ValueEmitter(pe, pe.getBlock(), var);
    }

    public ForkEmitter fork(BinaryBranchingCondition condition, ValueEmitter other) {
        final BinaryBranchingInstruction insn = new BinaryBranchingInstruction(condition);
        insn.setFirstOperand(variable);
        insn.setSecondOperand(other.variable);
        pe.addInstruction(insn);
        return new ForkEmitter() {
            @Override public void setThen(BasicBlock block) {
                insn.setConsequent(block);
            }
            @Override public void setElse(BasicBlock block) {
                insn.setAlternative(block);
            }
        };
    }

    public ForkEmitter fork(BranchingCondition condition) {
        final BranchingInstruction insn = new BranchingInstruction(condition);
        insn.setOperand(variable);
        pe.addInstruction(insn);
        return new ForkEmitter() {
            @Override public void setThen(BasicBlock block) {
                insn.setConsequent(block);
            }
            @Override public void setElse(BasicBlock block) {
                insn.setAlternative(block);
            }
        };
    }

    public void returnValue() {
        ExitInstruction insn = new ExitInstruction();
        insn.setValueToReturn(variable);
        pe.addInstruction(insn);
    }

    public ValueEmitter cast(ValueType type) {
        Variable result = pe.getProgram().createVariable();
        CastInstruction insn = new CastInstruction();
        insn.setValue(variable);
        insn.setReceiver(result);
        insn.setTargetType(type);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter cast(NumericOperandType from, NumericOperandType to) {
        Variable result = pe.getProgram().createVariable();
        CastNumberInstruction insn = new CastNumberInstruction(from, to);
        insn.setValue(variable);
        insn.setReceiver(result);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter cast(IntegerSubtype subtype, CastIntegerDirection dir) {
        Variable result = pe.getProgram().createVariable();
        CastIntegerInstruction insn = new CastIntegerInstruction(subtype, dir);
        insn.setValue(variable);
        insn.setReceiver(result);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter toInteger(IntegerSubtype from) {
        return cast(from, CastIntegerDirection.TO_INTEGER);
    }

    public ValueEmitter fromInteger(IntegerSubtype to) {
        return cast(to, CastIntegerDirection.FROM_INTEGER);
    }

    public ValueEmitter getElement(ValueEmitter index) {
        Variable result = pe.getProgram().createVariable();
        GetElementInstruction insn = new GetElementInstruction();
        insn.setArray(variable);
        insn.setIndex(index.variable);
        insn.setReceiver(variable);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter getElement(int index) {
        return getElement(pe.constant(index));
    }

    public void setElement(ValueEmitter index, ValueEmitter value) {
        PutElementInstruction insn = new PutElementInstruction();
        insn.setArray(variable);
        insn.setIndex(index.variable);
        insn.setValue(value.variable);
        pe.addInstruction(insn);
    }

    public void setElement(int index, ValueEmitter value) {
        setElement(pe.constant(index), value);
    }

    public ValueEmitter unwrapArray(ArrayElementType elementType) {
        Variable result = pe.getProgram().createVariable();
        UnwrapArrayInstruction insn = new UnwrapArrayInstruction(elementType);
        insn.setArray(variable);
        insn.setReceiver(result);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter arrayLength() {
        Variable result = pe.getProgram().createVariable();
        ArrayLengthInstruction insn = new ArrayLengthInstruction();
        insn.setArray(variable);
        insn.setReceiver(result);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter instanceOf(ValueType type) {
        Variable result = pe.getProgram().createVariable();
        IsInstanceInstruction insn = new IsInstanceInstruction();
        insn.setValue(variable);
        insn.setReceiver(result);
        insn.setType(type);
        pe.addInstruction(insn);
        return pe.var(result);
    }

    public ValueEmitter cloneArray() {
        Variable result = pe.getProgram().createVariable();
        CloneArrayInstruction insn = new CloneArrayInstruction();
        insn.setArray(variable);
        insn.setReceiver(result);
        pe.addInstruction(insn);
        return pe.var(result);
    }
}
