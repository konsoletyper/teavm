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
package org.teavm.model.lowlevel;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.runtime.Allocator;

public class ClassInitializerTransformer {
    public void transform(Program program) {
        BasicBlockSplitter splitter = new BasicBlockSplitter(program);

        int count = program.basicBlockCount();
        for (int i = 0; i < count; ++i) {
            BasicBlock next = program.basicBlockAt(i);
            BasicBlock block;
            while (next != null) {
                block = next;
                next = null;
                for (Instruction instruction : block) {
                    if (!(instruction instanceof InitClassInstruction)) {
                        continue;
                    }
                    String className = ((InitClassInstruction) instruction).getClassName();

                    BasicBlock continueBlock = splitter.split(block, instruction);

                    BasicBlock initBlock = program.createBasicBlock();
                    instruction.delete();
                    initBlock.add(instruction);
                    JumpInstruction jumpToContinue = new JumpInstruction();
                    jumpToContinue.setTarget(continueBlock);
                    initBlock.add(jumpToContinue);

                    createInitCheck(program, block, className, continueBlock, initBlock, instruction.getLocation());

                    next = continueBlock;
                    break;
                }
            }
        }

        splitter.fixProgram();
    }

    private void createInitCheck(Program program, BasicBlock block, String className, BasicBlock continueBlock,
            BasicBlock initBlock, TextLocation location) {
        Variable clsVariable = program.createVariable();
        Variable initializedVariable = program.createVariable();

        ClassConstantInstruction clsConstant = new ClassConstantInstruction();
        clsConstant.setReceiver(clsVariable);
        clsConstant.setConstant(ValueType.object(className));
        clsConstant.setLocation(location);
        block.add(clsConstant);

        InvokeInstruction checkInitialized = new InvokeInstruction();
        checkInitialized.setType(InvocationType.SPECIAL);
        checkInitialized.setMethod(new MethodReference(Allocator.class, "isInitialized", Class.class, boolean.class));
        checkInitialized.setArguments(clsVariable);
        checkInitialized.setReceiver(initializedVariable);
        checkInitialized.setLocation(location);
        block.add(checkInitialized);

        BranchingInstruction branching = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        branching.setOperand(initializedVariable);
        branching.setConsequent(continueBlock);
        branching.setAlternative(initBlock);
        branching.setLocation(location);
        block.add(branching);
    }
}
