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
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
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
import org.teavm.model.optimization.RedundantJumpElimination;
import org.teavm.model.util.ProgramUtils;
import org.teavm.runtime.ExceptionHandling;

public class NullCheckTransformation {
    public void apply(Program program, ValueType returnType) {
        int[] mappings = new int[program.basicBlockCount()];
        for (int i = 0; i < mappings.length; ++i) {
            mappings[i] = i;
        }

        BasicBlock returnBlock = null;

        int count = program.basicBlockCount();
        for (int i = 0; i < count; ++i) {
            BasicBlock next = program.basicBlockAt(i);
            BasicBlock block = null;
            int newIndex = i;
            while (next != null) {
                block = next;
                newIndex = block.getIndex();
                next = null;
                for (Instruction instruction : block) {
                    if (!(instruction instanceof NullCheckInstruction)) {
                        continue;
                    }
                    NullCheckInstruction nullCheck = (NullCheckInstruction) instruction;

                    BasicBlock continueBlock = program.createBasicBlock();

                    continueBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));
                    while (nullCheck.getNext() != null) {
                        Instruction nextInstruction = nullCheck.getNext();
                        nextInstruction.delete();
                        continueBlock.add(nextInstruction);
                    }

                    BasicBlock throwBlock = program.createBasicBlock();

                    throwBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));
                    InvokeInstruction throwNPE = new InvokeInstruction();
                    throwNPE.setType(InvocationType.SPECIAL);
                    throwNPE.setMethod(new MethodReference(ExceptionHandling.class, "throwNullPointerException",
                            void.class));
                    throwNPE.setLocation(nullCheck.getLocation());
                    throwBlock.add(throwNPE);

                    if (returnBlock == null) {
                        returnBlock = program.createBasicBlock();
                    }

                    JumpInstruction jumpToFakeReturn = new JumpInstruction();
                    jumpToFakeReturn.setTarget(returnBlock);
                    jumpToFakeReturn.setLocation(nullCheck.getLocation());
                    throwBlock.add(jumpToFakeReturn);

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
                    break;
                }
            }

            mappings[i] = newIndex;
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

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    int source = incoming.getSource().getIndex();
                    if (source < mappings.length && mappings[source] != source) {
                        incoming.setSource(program.basicBlockAt(mappings[source]));
                    }
                }
            }
        }

        RedundantJumpElimination.optimize(program);
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
