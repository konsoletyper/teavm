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
package org.teavm.javascript;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.*;
import org.teavm.model.util.InstructionTransitionExtractor;
import org.teavm.model.util.UsageExtractor;

/**
 *
 * @author Alexey Andreev
 */
class PhiEliminator {
    public static void eliminatePhis(Program program) {
        // Count how many times each variable is used
        int[] variableUsageCount = new int[program.variableCount()];
        int[] definitionRenamings = new int[program.variableCount()];
        for (int i = 0; i < definitionRenamings.length; ++i) {
            definitionRenamings[i] = i;
        }
        UsageExtractor usageExtractor = new UsageExtractor();
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        int blockCount = program.basicBlockCount();
        for (int i = 0; i < blockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(usageExtractor);
                for (Variable var : usageExtractor.getUsedVariables()) {
                    variableUsageCount[var.getIndex()]++;
                }
            }
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    variableUsageCount[incoming.getValue().getIndex()]++;
                }
            }
        }

        // Places assignments at the end of each block
        for (int i = 0; i < blockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            block.getLastInstruction().acceptVisitor(transitionExtractor);
            BasicBlock[] targets = transitionExtractor.getTargets();
            if (targets.length == 1) {

            } else {
                for (BasicBlock targetBlock : transitionExtractor.getTargets()) {
                    List<Incoming> incomings = new ArrayList<>();
                    for (Incoming incoming : getIncomings(block, targetBlock)) {
                        if (variableUsageCount[incoming.getSource().getIndex()] <= 1) {
                            definitionRenamings[incoming.getValue().getIndex()] =
                                    incoming.getPhi().getReceiver().getIndex();
                        } else {
                            incomings.add(incoming);
                        }
                    }
                }
            }
        }

        // Removes phi functions
        for (int i = 0; i < blockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            block.getPhis().clear();
        }
    }

    private static List<Incoming> getIncomings(BasicBlock block, BasicBlock targetBlock) {
        List<Incoming> incomings = new ArrayList<>();
        for (Phi phi : targetBlock.getPhis()) {
            for (Incoming incoming : phi.getIncomings()) {
                if (incoming.getSource() == block) {
                    incomings.add(incoming);
                }
            }
        }
        return incomings;
    }
}
