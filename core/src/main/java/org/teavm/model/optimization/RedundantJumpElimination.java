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
package org.teavm.model.optimization;

import java.util.Arrays;
import org.teavm.common.Graph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.util.InstructionTransitionExtractor;
import org.teavm.model.util.ProgramUtils;

public class RedundantJumpElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        int[] incomingCount = new int[cfg.size()];
        Arrays.setAll(incomingCount, cfg::incomingEdgesCount);

        boolean changed = false;
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        for (int i = 1; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            if (block == null) {
                continue;
            }
            Instruction insn = block.getLastInstruction();
            if (!(insn instanceof JumpInstruction)) {
                continue;
            }

            BasicBlock target = ((JumpInstruction) insn).getTarget();
            if (incomingCount[target.getIndex()] > 1) {
                continue;
            }

            if (!block.getTryCatchBlocks().isEmpty() || !target.getTryCatchBlocks().isEmpty()) {
                continue;
            }

            block.getLastInstruction().delete();
            for (Phi phi : target.getPhis()) {
                if (phi.getIncomings().isEmpty()) {
                    continue;
                }
                Incoming incoming = phi.getIncomings().get(0);
                AssignInstruction assign = new AssignInstruction();
                assign.setReceiver(phi.getReceiver());
                assign.setAssignee(incoming.getValue());
                block.add(assign);
            }
            while (target.getFirstInstruction() != null) {
                Instruction instruction = target.getFirstInstruction();
                instruction.delete();
                block.add(instruction);
            }

            Instruction lastInsn = block.getLastInstruction();
            if (lastInsn != null) {
                lastInsn.acceptVisitor(transitionExtractor);
                BasicBlock[] successors = transitionExtractor.getTargets();
                if (successors != null) {
                    for (BasicBlock successor : successors) {
                        successor.getPhis().stream()
                                .flatMap(phi -> phi.getIncomings().stream())
                                .filter(incoming -> incoming.getSource() == target)
                                .forEach(incoming -> incoming.setSource(block));
                    }
                }
            }

            for (TryCatchBlock tryCatch : target.getTryCatchBlocks()) {
                for (Phi phi : tryCatch.getHandler().getPhis()) {
                    phi.getIncomings().removeIf(incoming -> incoming.getSource() == target);
                }
            }

            incomingCount[target.getIndex()] = 2;
            program.deleteBasicBlock(target.getIndex());
            --i;
            changed = true;
        }

        if (changed) {
            program.pack();
        }
        return changed;
    }
}
