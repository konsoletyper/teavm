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

import java.util.List;
import org.teavm.common.IntegerStack;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.util.InstructionTransitionExtractor;

public class UnreachableBasicBlockEliminator {
    public void optimize(Program program) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        boolean[] reachable = new boolean[program.basicBlockCount()];
        IntegerStack stack = new IntegerStack(program.basicBlockCount());
        stack.push(0);
        while (!stack.isEmpty()) {
            int i = stack.pop();
            if (reachable[i]) {
                continue;
            }
            reachable[i] = true;
            BasicBlock block = program.basicBlockAt(i);
            block.getLastInstruction().acceptVisitor(transitionExtractor);
            for (BasicBlock successor : transitionExtractor.getTargets()) {
                if (!reachable[successor.getIndex()]) {
                    stack.push(successor.getIndex());
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                stack.push(tryCatch.getHandler().getIndex());
            }
        }

        for (int i = 0; i < reachable.length; ++i) {
            if (!reachable[i]) {
                BasicBlock block = program.basicBlockAt(i);
                if (block.getLastInstruction() != null) {
                    block.getLastInstruction().acceptVisitor(transitionExtractor);
                    for (BasicBlock successor : transitionExtractor.getTargets()) {
                        successor.removeIncomingsFrom(block);
                    }
                }
                for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                    tryCatch.getHandler().removeIncomingsFrom(block);
                }
                program.deleteBasicBlock(i);
            }
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            if (block == null) {
                continue;
            }
            for (Phi phi : block.getPhis()) {
                List<Incoming> incomingList = phi.getIncomings();
                for (int j = 0; j < incomingList.size(); ++j) {
                    Incoming incoming = incomingList.get(j);
                    if (!reachable[incoming.getSource().getIndex()]) {
                        incomingList.remove(j--);
                    }
                }
            }
        }

        program.pack();
    }
}
