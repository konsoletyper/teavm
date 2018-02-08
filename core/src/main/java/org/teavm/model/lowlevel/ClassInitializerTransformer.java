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
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.util.ProgramUtils;
import org.teavm.runtime.Allocator;

public class ClassInitializerTransformer {
    public void transform(Program program) {
        int[] basicBlockMap = new int[program.basicBlockCount()];
        for (int i = 0; i < basicBlockMap.length; ++i) {
            basicBlockMap[i] = i;
        }

        for (int i = 0; i < basicBlockMap.length; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction instruction : block) {
                if (!(instruction instanceof InitClassInstruction)) {
                    continue;
                }
                String className = ((InitClassInstruction) instruction).getClassName();
                block = instruction.getBasicBlock();

                BasicBlock continueBlock = program.createBasicBlock();
                while (instruction.getNext() != null) {
                    Instruction toMove = instruction.getNext();
                    toMove.delete();
                    continueBlock.add(toMove);
                }
                continueBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));

                BasicBlock initBlock = program.createBasicBlock();
                instruction.delete();
                initBlock.add(instruction);
                JumpInstruction jumpToContinue = new JumpInstruction();
                jumpToContinue.setTarget(continueBlock);
                initBlock.add(jumpToContinue);

                createInitCheck(program, block, className, continueBlock, initBlock);

                basicBlockMap[i] = continueBlock.getIndex();
            }
        }

        for (int i = 0; i < basicBlockMap.length; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    int source = incoming.getSource().getIndex();
                    BasicBlock mappedSource = program.basicBlockAt(basicBlockMap[source]);
                    incoming.setSource(mappedSource);
                }
            }
        }
    }

    private void createInitCheck(Program program, BasicBlock block, String className, BasicBlock continueBlock,
            BasicBlock initBlock) {
        Variable clsVariable = program.createVariable();
        Variable initializedVariable = program.createVariable();

        ClassConstantInstruction clsConstant = new ClassConstantInstruction();
        clsConstant.setReceiver(clsVariable);
        clsConstant.setConstant(ValueType.object(className));
        block.add(clsConstant);

        InvokeInstruction checkInitialized = new InvokeInstruction();
        checkInitialized.setType(InvocationType.SPECIAL);
        checkInitialized.setMethod(new MethodReference(Allocator.class, "isInitialized", Class.class, boolean.class));
        checkInitialized.getArguments().add(clsVariable);
        checkInitialized.setReceiver(initializedVariable);
        block.add(checkInitialized);

        BranchingInstruction branching = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        branching.setOperand(initializedVariable);
        branching.setConsequent(continueBlock);
        branching.setAlternative(initBlock);
        block.add(branching);
    }
}
