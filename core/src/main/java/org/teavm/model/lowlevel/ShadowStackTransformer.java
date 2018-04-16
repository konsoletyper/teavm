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

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.runtime.ShadowStack;

public class ShadowStackTransformer {
    private Characteristics managedMethodRepository;
    private GCShadowStackContributor gcContributor;
    private List<CallSiteDescriptor> callSites = new ArrayList<>();

    public ShadowStackTransformer(Characteristics managedMethodRepository) {
        gcContributor = new GCShadowStackContributor(managedMethodRepository);
        this.managedMethodRepository = managedMethodRepository;
    }

    public List<CallSiteDescriptor> getCallSites() {
        return callSites;
    }

    public void apply(Program program, MethodReader method) {
        if (!managedMethodRepository.isManaged(method.getReference())) {
            return;
        }

        int shadowStackSize = gcContributor.contribute(program, method);
        boolean exceptions = new ExceptionHandlingShadowStackContributor(managedMethodRepository, callSites,
                method.getReference(), program).contribute();

        if (shadowStackSize > 0 || exceptions) {
            addStackAllocation(program, shadowStackSize);
            addStackRelease(program, shadowStackSize);
        }
    }

    private void addStackAllocation(Program program, int maxDepth) {
        BasicBlock block = program.basicBlockAt(0);
        List<Instruction> instructionsToAdd = new ArrayList<>();
        Variable sizeVariable = program.createVariable();

        IntegerConstantInstruction sizeConstant = new IntegerConstantInstruction();
        sizeConstant.setReceiver(sizeVariable);
        sizeConstant.setConstant(maxDepth);
        instructionsToAdd.add(sizeConstant);

        InvokeInstruction invocation = new InvokeInstruction();
        invocation.setType(InvocationType.SPECIAL);
        invocation.setMethod(new MethodReference(ShadowStack.class, "allocStack", int.class, void.class));
        invocation.getArguments().add(sizeVariable);
        instructionsToAdd.add(invocation);

        block.addFirstAll(instructionsToAdd);
    }

    private void addStackRelease(Program program, int maxDepth) {
        List<BasicBlock> blocks = new ArrayList<>();
        boolean hasResult = false;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction instruction = block.getLastInstruction();
            if (instruction instanceof ExitInstruction) {
                blocks.add(block);
                if (((ExitInstruction) instruction).getValueToReturn() != null) {
                    hasResult = true;
                }
            }
        }

        BasicBlock exitBlock;
        if (blocks.size() == 1) {
            exitBlock = blocks.get(0);
        } else {
            exitBlock = program.createBasicBlock();
            ExitInstruction exit = new ExitInstruction();
            exitBlock.add(exit);

            if (hasResult) {
                Phi phi = new Phi();
                phi.setReceiver(program.createVariable());
                exitBlock.getPhis().add(phi);
                exit.setValueToReturn(phi.getReceiver());

                for (BasicBlock block : blocks) {
                    ExitInstruction oldExit = (ExitInstruction) block.getLastInstruction();
                    Incoming incoming = new Incoming();
                    incoming.setSource(block);
                    incoming.setValue(oldExit.getValueToReturn());
                    phi.getIncomings().add(incoming);
                }
            }

            for (BasicBlock block : blocks) {
                ExitInstruction oldExit = (ExitInstruction) block.getLastInstruction();
                JumpInstruction jumpToExit = new JumpInstruction();
                jumpToExit.setTarget(exitBlock);
                jumpToExit.setLocation(oldExit.getLocation());
                jumpToExit.setLocation(oldExit.getLocation());

                block.getLastInstruction().replace(jumpToExit);
            }
        }

        List<Instruction> instructionsToAdd = new ArrayList<>();
        Variable sizeVariable = program.createVariable();

        IntegerConstantInstruction sizeConstant = new IntegerConstantInstruction();
        sizeConstant.setReceiver(sizeVariable);
        sizeConstant.setConstant(maxDepth);
        instructionsToAdd.add(sizeConstant);

        InvokeInstruction invocation = new InvokeInstruction();
        invocation.setType(InvocationType.SPECIAL);
        invocation.setMethod(new MethodReference(ShadowStack.class, "releaseStack", int.class, void.class));
        invocation.getArguments().add(sizeVariable);
        instructionsToAdd.add(invocation);

        exitBlock.getLastInstruction().insertPreviousAll(instructionsToAdd);
    }
}
