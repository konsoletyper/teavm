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

import org.teavm.model.*;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

/**
 *
 * @author Alexey Andreev
 */
public final class ProgramEmitter {
    private Program program;
    private BasicBlock block;
    private InstructionLocation currentLocation;

    private ProgramEmitter(Program program, BasicBlock block) {
        this.program = program;
        this.block = block;
    }

    public Program getProgram() {
        return program;
    }

    public BasicBlock getBlock() {
        return block;
    }

    public void setBlock(BasicBlock block) {
        this.block = block;
    }

    public BasicBlock createBlock() {
        BasicBlock block = program.createBasicBlock();
        setBlock(block);
        return block;
    }

    public ValueEmitter constant(Class<?> cls) {
        return constant(ValueType.parse(cls));
    }

    public ValueEmitter constant(ValueType value) {
        Variable var = program.createVariable();
        ClassConstantInstruction insn = new ClassConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constant(String value) {
        Variable var = program.createVariable();
        StringConstantInstruction insn = new StringConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constant(int value) {
        Variable var = program.createVariable();
        IntegerConstantInstruction insn = new IntegerConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constant(long value) {
        Variable var = program.createVariable();
        LongConstantInstruction insn = new LongConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constant(float value) {
        Variable var = program.createVariable();
        FloatConstantInstruction insn = new FloatConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constant(double value) {
        Variable var = program.createVariable();
        DoubleConstantInstruction insn = new DoubleConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constantNull() {
        Variable var = program.createVariable();
        NullConstantInstruction insn = new NullConstantInstruction();
        insn.setReceiver(var);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter getField(FieldReference field, ValueType type) {
        Variable var = program.createVariable();
        GetFieldInstruction insn = new GetFieldInstruction();
        insn.setField(field);
        insn.setFieldType(type);
        insn.setReceiver(var);
        addInstruction(insn);
        return wrap(var);
    }

    public ProgramEmitter setField(FieldReference field, ValueType type, ValueEmitter value) {
        PutFieldInstruction insn = new PutFieldInstruction();
        insn.setField(field);
        insn.setFieldType(type);
        insn.setValue(value.getVariable());
        addInstruction(insn);
        return this;
    }

    public ValueEmitter invoke(MethodReference method, ValueEmitter... arguments) {
        Variable result = null;
        if (method.getReturnType() != ValueType.VOID) {
            result = program.createVariable();
        }
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(method);
        insn.setReceiver(result);
        for (ValueEmitter arg : arguments) {
            insn.getArguments().add(arg.variable);
        }
        addInstruction(insn);
        return result != null ? wrap(result) : null;
    }

    public ProgramEmitter invokeAndIgnore(MethodReference method, ValueEmitter... arguments) {
        invoke(method, arguments);
        return this;
    }

    public ValueEmitter construct(MethodReference method, ValueEmitter... arguments) {
        Variable var = program.createVariable();
        ConstructInstruction insn = new ConstructInstruction();
        insn.setReceiver(var);
        insn.setType(method.getClassName());
        addInstruction(insn);
        ValueEmitter instance = wrap(var);
        instance.invokeSpecial(method, arguments);
        return instance;
    }

    public ValueEmitter constructArray(ValueType type, ValueEmitter size) {
        Variable var = program.createVariable();
        ConstructArrayInstruction insn = new ConstructArrayInstruction();
        insn.setReceiver(var);
        insn.setSize(size.getVariable());
        insn.setItemType(type);
        addInstruction(insn);
        return wrap(var);
    }

    public ValueEmitter constructArray(ValueType type, int size) {
        return constructArray(type, constant(size));
    }

    public ValueEmitter constructArray(Class<?> type, int size) {
        return constructArray(ValueType.parse(type), size);
    }

    public ValueEmitter constructArray(Class<?> type, ValueEmitter size) {
        return constructArray(ValueType.parse(type), size);
    }

    public ProgramEmitter jump(BasicBlock block) {
        JumpInstruction insn = new JumpInstruction();
        insn.setTarget(block);
        addInstruction(insn);
        this.block = block;
        return this;
    }

    public void exit() {
        ExitInstruction insn = new ExitInstruction();
        addInstruction(insn);
    }

    public ValueEmitter wrap(Variable var) {
        return new ValueEmitter(this, block, var);
    }

    public ValueEmitter wrapNew() {
        return wrap(program.createVariable());
    }

    public InstructionLocation getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(InstructionLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void addInstruction(Instruction insn) {
        if (currentLocation != null) {
            insn.setLocation(currentLocation);
        }
        block.getInstructions().add(insn);
    }

    public static ProgramEmitter create(MethodHolder method) {
        Program program = new Program();
        method.setProgram(program);
        BasicBlock zeroBlock = program.createBasicBlock();
        BasicBlock block = program.createBasicBlock();

        JumpInstruction insn = new JumpInstruction();
        insn.setTarget(block);
        zeroBlock.getInstructions().add(insn);

        return new ProgramEmitter(program, block);
    }
}
