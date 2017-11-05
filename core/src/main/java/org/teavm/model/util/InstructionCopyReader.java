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
package org.teavm.model.util;

import java.util.List;
import java.util.stream.Collectors;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
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
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
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
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.SwitchTableEntryReader;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class InstructionCopyReader implements InstructionReader {
    private Instruction copy;
    private Program programCopy;
    private TextLocation location;

    public InstructionCopyReader(Program programCopy) {
        this.programCopy = programCopy;
    }

    public Instruction getCopy() {
        return copy;
    }

    public void resetLocation() {
        location = null;
    }

    @Override
    public void location(TextLocation location) {
        this.location = location;
    }

    private Variable copyVar(VariableReader var) {
        return programCopy.variableAt(var.getIndex());
    }

    private BasicBlock copyBlock(BasicBlockReader block) {
        return programCopy.basicBlockAt(block.getIndex());
    }

    @Override
    public void nop() {
        copy = new EmptyInstruction();
        copy.setLocation(location);
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        ClassConstantInstruction insnCopy = new ClassConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void nullConstant(VariableReader receiver) {
        NullConstantInstruction insnCopy = new NullConstantInstruction();
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void integerConstant(VariableReader receiver, int cst) {
        IntegerConstantInstruction insnCopy = new IntegerConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void longConstant(VariableReader receiver, long cst) {
        LongConstantInstruction insnCopy = new LongConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void floatConstant(VariableReader receiver, float cst) {
        FloatConstantInstruction insnCopy = new FloatConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void doubleConstant(VariableReader receiver, double cst) {
        DoubleConstantInstruction insnCopy = new DoubleConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        StringConstantInstruction insnCopy = new StringConstantInstruction();
        insnCopy.setConstant(cst);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type) {
        BinaryInstruction insnCopy = new BinaryInstruction(op, type);
        insnCopy.setFirstOperand(copyVar(first));
        insnCopy.setSecondOperand(copyVar(second));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        NegateInstruction insnCopy = new NegateInstruction(type);
        insnCopy.setOperand(copyVar(operand));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        AssignInstruction insnCopy = new AssignInstruction();
        insnCopy.setAssignee(copyVar(assignee));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        CastInstruction insnCopy = new CastInstruction();
        insnCopy.setValue(copyVar(value));
        insnCopy.setReceiver(copyVar(receiver));
        insnCopy.setTargetType(targetType);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) {
        CastNumberInstruction insnCopy = new CastNumberInstruction(sourceType, targetType);
        insnCopy.setValue(copyVar(value));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection dir) {
        CastIntegerInstruction insnCopy = new CastIntegerInstruction(type, dir);
        insnCopy.setValue(copyVar(value));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) {
        BranchingInstruction insnCopy = new BranchingInstruction(cond);
        insnCopy.setOperand(copyVar(operand));
        insnCopy.setConsequent(copyBlock(consequent));
        insnCopy.setAlternative(copyBlock(alternative));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) {
        BinaryBranchingInstruction insnCopy = new BinaryBranchingInstruction(cond);
        insnCopy.setFirstOperand(copyVar(first));
        insnCopy.setSecondOperand(copyVar(second));
        insnCopy.setConsequent(copyBlock(consequent));
        insnCopy.setAlternative(copyBlock(alternative));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void jump(BasicBlockReader target) {
        JumpInstruction insnCopy = new JumpInstruction();
        insnCopy.setTarget(copyBlock(target));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) {
        SwitchInstruction insnCopy = new SwitchInstruction();
        insnCopy.setCondition(copyVar(condition));
        insnCopy.setDefaultTarget(copyBlock(defaultTarget));
        for (SwitchTableEntryReader entry : table) {
            SwitchTableEntry entryCopy = new SwitchTableEntry();
            entryCopy.setCondition(entry.getCondition());
            entryCopy.setTarget(copyBlock(entry.getTarget()));
            insnCopy.getEntries().add(entryCopy);
        }
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        ExitInstruction insnCopy = new ExitInstruction();
        insnCopy.setValueToReturn(valueToReturn != null ? copyVar(valueToReturn) : null);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void raise(VariableReader exception) {
        RaiseInstruction insnCopy = new RaiseInstruction();
        insnCopy.setException(copyVar(exception));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        ConstructArrayInstruction insnCopy = new ConstructArrayInstruction();
        insnCopy.setItemType(itemType);
        insnCopy.setSize(copyVar(size));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType,
            List<? extends VariableReader> dimensions) {
        ConstructMultiArrayInstruction insnCopy = new ConstructMultiArrayInstruction();
        insnCopy.setItemType(itemType);
        insnCopy.setReceiver(copyVar(receiver));
        for (VariableReader dim : dimensions) {
            insnCopy.getDimensions().add(copyVar(dim));
        }
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void create(VariableReader receiver, String type) {
        ConstructInstruction insnCopy = new ConstructInstruction();
        insnCopy.setType(type);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
            ValueType fieldType) {
        GetFieldInstruction insnCopy = new GetFieldInstruction();
        insnCopy.setField(field);
        insnCopy.setFieldType(fieldType);
        insnCopy.setInstance(instance != null ? copyVar(instance) : null);
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value,
            ValueType fieldType) {
        PutFieldInstruction insnCopy = new PutFieldInstruction();
        insnCopy.setField(field);
        insnCopy.setInstance(instance != null ? copyVar(instance) : null);
        insnCopy.setValue(copyVar(value));
        insnCopy.setFieldType(fieldType);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void arrayLength(VariableReader receiver, VariableReader array) {
        ArrayLengthInstruction insnCopy = new ArrayLengthInstruction();
        insnCopy.setArray(copyVar(array));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        CloneArrayInstruction insnCopy = new CloneArrayInstruction();
        insnCopy.setArray(copyVar(array));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        UnwrapArrayInstruction insnCopy = new UnwrapArrayInstruction(elementType);
        insnCopy.setArray(copyVar(array));
        insnCopy.setReceiver(copyVar(receiver));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
            ArrayElementType type) {
        GetElementInstruction insnCopy = new GetElementInstruction(type);
        insnCopy.setArray(copyVar(array));
        insnCopy.setReceiver(copyVar(receiver));
        insnCopy.setIndex(copyVar(index));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value, ArrayElementType type) {
        PutElementInstruction insnCopy = new PutElementInstruction(type);
        insnCopy.setArray(copyVar(array));
        insnCopy.setValue(copyVar(value));
        insnCopy.setIndex(copyVar(index));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        InvokeInstruction insnCopy = new InvokeInstruction();
        insnCopy.setMethod(method);
        insnCopy.setType(type);
        insnCopy.setInstance(instance != null ? copyVar(instance) : null);
        insnCopy.setReceiver(receiver != null ? copyVar(receiver) : null);
        for (VariableReader arg : arguments) {
            insnCopy.getArguments().add(copyVar(arg));
        }
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
            List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
            List<RuntimeConstant> bootstrapArguments) {
        InvokeDynamicInstruction insnCopy = new InvokeDynamicInstruction();
        insnCopy.setMethod(method);
        insnCopy.setBootstrapMethod(bootstrapMethod);
        insnCopy.getBootstrapArguments().addAll(bootstrapArguments);
        if (instance != null) {
            insnCopy.setInstance(copyVar(instance));
        }
        insnCopy.getArguments().addAll(arguments.stream().map(this::copyVar).collect(Collectors.toList()));
        insnCopy.setReceiver(receiver != null ? copyVar(receiver) : null);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        IsInstanceInstruction insnCopy = new IsInstanceInstruction();
        insnCopy.setValue(copyVar(value));
        insnCopy.setReceiver(copyVar(receiver));
        insnCopy.setType(type);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void initClass(String className) {
        InitClassInstruction insnCopy = new InitClassInstruction();
        insnCopy.setClassName(className);
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        NullCheckInstruction insnCopy = new NullCheckInstruction();
        insnCopy.setReceiver(copyVar(receiver));
        insnCopy.setValue(copyVar(value));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        MonitorEnterInstruction insnCopy = new MonitorEnterInstruction();
        insnCopy.setObjectRef(copyVar(objectRef));
        copy = insnCopy;
        copy.setLocation(location);
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        MonitorExitInstruction insnCopy = new MonitorExitInstruction();
        insnCopy.setObjectRef(copyVar(objectRef));
        copy = insnCopy;
        copy.setLocation(location);
    }
}
