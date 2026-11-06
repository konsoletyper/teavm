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

import org.teavm.common.IntegerStack;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.TransitionExtractor;

public class UnreachableBasicBlockEliminator {
    public void removeUnreachableInstructions(Program program) {
        var transitionExtractor = new TransitionExtractor();
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                instruction.acceptVisitor(transitionExtractor);
                if (transitionExtractor.getTargets() != null) {
                    while (instruction.getNext() != null) {
                        instruction.getNext().delete();
                    }
                }
            }
        }
    }

    public void optimize(Program program, int paramCount) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        TransitionExtractor transitionExtractor = new TransitionExtractor();
        var reachable = new boolean[program.basicBlockCount()];
        var hasExceptionHandlers = false;
        var terminating = new boolean[program.basicBlockCount()];
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
            if (transitionExtractor.getTargets().length == 0) {
                terminating[i] = true;
            }
            for (var tryCatch : block.getTryCatchBlocks()) {
                stack.push(tryCatch.getHandler().getIndex());
                hasExceptionHandlers = true;
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

        boolean[] definedVars = null;
        if (hasExceptionHandlers) {
            definedVars = new boolean[program.variableCount()];
            for (var i = 0; i <= paramCount; ++i) {
                definedVars[i] = true;
            }
            var defs = new DefinitionExtractor();
            for (var i = 0; i < program.basicBlockCount(); ++i) {
                if (!reachable[i]) {
                    continue;
                }
                var block = program.basicBlockAt(i);
                for (var insn : block) {
                    insn.acceptVisitor(defs);
                    var vars = defs.getDefinedVariables();
                    if (vars != null) {
                        for (var v : vars) {
                            definedVars[v.getIndex()] = true;
                        }
                    }
                }
                for (var phi : block.getPhis()) {
                    definedVars[phi.getReceiver().getIndex()] = true;
                }
                if (block.getExceptionVariable() != null) {
                    definedVars[block.getExceptionVariable().getIndex()] = true;
                }
            }
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            if (block == null) {
                continue;
            }
            for (var iter = block.getPhis().iterator(); iter.hasNext();) {
                var phi = iter.next();
                var incomingList = phi.getIncomings();
                for (int j = 0; j < incomingList.size(); ++j) {
                    Incoming incoming = incomingList.get(j);
                    if (!reachable[incoming.getSource().getIndex()]) {
                        incomingList.remove(j--);
                    } else if (terminating[incoming.getSource().getIndex()]) {
                        var isExceptionPredecessor = false;
                        for (var tryCatch : incoming.getSource().getTryCatchBlocks()) {
                            if (tryCatch.getHandler() == block) {
                                isExceptionPredecessor = true;
                                break;
                            }
                        }
                        if (!isExceptionPredecessor || !definedVars[incoming.getValue().getIndex()]) {
                            incomingList.remove(j--);
                        }
                    }
                }
                if (incomingList.isEmpty()) {
                    iter.remove();
                }
            }
        }

        program.pack();
    }
}
