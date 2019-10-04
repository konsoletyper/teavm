/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.model.lowlevel;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.runtime.ExceptionHandling;

public class CheckInstructionTransformation {
    private BasicBlock returnBlock;
    private BasicBlock next;

    public void apply(Program program, ValueType returnType) {
        BasicBlockSplitter splitter = new BasicBlockSplitter(program);

        returnBlock = null;
        int count = program.basicBlockCount();
        for (int i = 0; i < count; ++i) {
            next = program.basicBlockAt(i);
            BasicBlock block;
            while (next != null) {
                block = next;
                next = null;
                for (Instruction instruction : block) {
                    if (instruction instanceof NullCheckInstruction) {
                        replaceNullCheck(splitter, program, (NullCheckInstruction) instruction);
                    } else if (instruction instanceof BoundCheckInstruction) {
                        replaceBoundCheck(splitter, program, (BoundCheckInstruction) instruction);
                    }
                }
            }
        }

        if (returnBlock != null) {
            ExitInstruction fakeExit = new ExitInstruction();
            if (returnType != ValueType.VOID) {
                Variable fakeReturnVar = program.createVariable();
                createFakeReturnValue(returnBlock, fakeReturnVar, returnType);
                fakeExit.setValueToReturn(fakeReturnVar);
            }
            returnBlock.add(fakeExit);
        }

        splitter.fixProgram();
    }

    private void replaceNullCheck(BasicBlockSplitter splitter, Program program, NullCheckInstruction nullCheck) {
        BasicBlock block = nullCheck.getBasicBlock();
        BasicBlock continueBlock = splitter.split(block, nullCheck);
        BasicBlock throwBlock = program.createBasicBlock();

        InvokeInstruction throwNPE = new InvokeInstruction();
        throwNPE.setType(InvocationType.SPECIAL);
        throwNPE.setMethod(new MethodReference(ExceptionHandling.class, "throwNullPointerException",
                void.class));
        throwNPE.setLocation(nullCheck.getLocation());
        throwBlock.add(throwNPE);

        jumpToReturn(program, nullCheck, throwBlock);

        BranchingInstruction jumpIfNull = new BranchingInstruction(BranchingCondition.NULL);
        jumpIfNull.setOperand(nullCheck.getValue());
        jumpIfNull.setConsequent(throwBlock);
        jumpIfNull.setAlternative(continueBlock);
        jumpIfNull.setLocation(nullCheck.getLocation());
        nullCheck.replace(jumpIfNull);

        AssignInstruction assign = new AssignInstruction();
        assign.setAssignee(nullCheck.getValue());
        assign.setReceiver(nullCheck.getReceiver());
        assign.setLocation(nullCheck.getLocation());
        continueBlock.addFirst(assign);

        next = continueBlock;
    }

    private void replaceBoundCheck(BasicBlockSplitter splitter, Program program, BoundCheckInstruction boundCheck) {
        BasicBlock block = boundCheck.getBasicBlock();
        BasicBlock continueBlock = splitter.split(block, boundCheck);
        BasicBlock throwBlock = program.createBasicBlock();

        BasicBlock ifPositiveBlock = continueBlock;
        if (boundCheck.isLower() && boundCheck.getArray() != null) {
            ifPositiveBlock = program.createBasicBlock();
        }

        InvokeInstruction throwAIOOBE = new InvokeInstruction();
        throwAIOOBE.setType(InvocationType.SPECIAL);
        throwAIOOBE.setMethod(new MethodReference(ExceptionHandling.class, "throwArrayIndexOutOfBoundsException",
                void.class));
        throwAIOOBE.setLocation(boundCheck.getLocation());
        throwBlock.add(throwAIOOBE);

        jumpToReturn(program, boundCheck, throwBlock);

        if (boundCheck.isLower()) {
            BranchingInstruction jumpIfLess = new BranchingInstruction(BranchingCondition.LESS);
            jumpIfLess.setOperand(boundCheck.getIndex());
            jumpIfLess.setConsequent(throwBlock);
            jumpIfLess.setAlternative(ifPositiveBlock);
            jumpIfLess.setLocation(boundCheck.getLocation());
            boundCheck.replace(jumpIfLess);
        }

        if (boundCheck.getArray() != null) {
            ArrayLengthInstruction arrayLength = new ArrayLengthInstruction();
            arrayLength.setArray(boundCheck.getArray());
            arrayLength.setReceiver(program.createVariable());
            arrayLength.setLocation(boundCheck.getLocation());

            BinaryInstruction compare = new BinaryInstruction(BinaryOperation.COMPARE, NumericOperandType.INT);
            compare.setFirstOperand(boundCheck.getIndex());
            compare.setSecondOperand(arrayLength.getReceiver());
            compare.setReceiver(program.createVariable());
            compare.setLocation(boundCheck.getLocation());

            BranchingInstruction jumpIfGreater = new BranchingInstruction(BranchingCondition.GREATER_OR_EQUAL);
            jumpIfGreater.setOperand(compare.getReceiver());
            jumpIfGreater.setConsequent(throwBlock);
            jumpIfGreater.setAlternative(continueBlock);
            jumpIfGreater.setLocation(boundCheck.getLocation());

            if (boundCheck.isLower()) {
                ifPositiveBlock.add(jumpIfGreater);
            } else {
                boundCheck.replace(jumpIfGreater);
            }

            jumpIfGreater.insertPrevious(arrayLength);
            jumpIfGreater.insertPrevious(compare);
        }

        AssignInstruction assign = new AssignInstruction();
        assign.setAssignee(boundCheck.getIndex());
        assign.setReceiver(boundCheck.getReceiver());
        assign.setLocation(boundCheck.getLocation());
        continueBlock.addFirst(assign);
    }

    private void jumpToReturn(Program program, Instruction instruction, BasicBlock throwBlock) {
        if (returnBlock == null) {
            returnBlock = program.createBasicBlock();
        }

        JumpInstruction jumpToFakeReturn = new JumpInstruction();
        jumpToFakeReturn.setTarget(returnBlock);
        jumpToFakeReturn.setLocation(instruction.getLocation());
        throwBlock.add(jumpToFakeReturn);
    }

    private void createFakeReturnValue(BasicBlock block, Variable variable, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INTEGER:
                case CHARACTER:
                    IntegerConstantInstruction intZero = new IntegerConstantInstruction();
                    intZero.setReceiver(variable);
                    block.add(intZero);
                    return;
                case LONG:
                    LongConstantInstruction longZero = new LongConstantInstruction();
                    longZero.setReceiver(variable);
                    block.add(longZero);
                    return;
                case FLOAT:
                    FloatConstantInstruction floatZero = new FloatConstantInstruction();
                    floatZero.setReceiver(variable);
                    block.add(floatZero);
                    return;
                case DOUBLE:
                    DoubleConstantInstruction doubleZero = new DoubleConstantInstruction();
                    doubleZero.setReceiver(variable);
                    block.add(doubleZero);
                    return;
            }
        }

        NullConstantInstruction nullConstant = new NullConstantInstruction();
        nullConstant.setReceiver(variable);
        block.add(nullConstant);
    }
}
