/*
 *  Copyright 2015 Alexey Andreev.
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

import java.util.*;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class AsyncProgramSplitter {
    private List<Part> parts = new ArrayList<>();
    private Map<Long, Integer> partMap = new HashMap<>();
    private Set<MethodReference> asyncMethods = new HashSet<>();

    public AsyncProgramSplitter(Set<MethodReference> asyncMethods) {
        this.asyncMethods = asyncMethods;
    }

    public void split(Program program) {
        parts.clear();
        Program initialProgram = createStubCopy(program);
        Part initialPart = new Part();
        initialPart.program = initialProgram;
        initialPart.blockSuccessors = new int[program.basicBlockCount()];
        parts.add(initialPart);
        partMap.put(0L, 0);
        Step initialStep = new Step();
        initialStep.source = 0;
        initialStep.targetPart = initialPart;
        Queue<Step> queue = new ArrayDeque<>();
        queue.add(initialStep);

        while (!queue.isEmpty()) {
            Step step = queue.remove();
            BasicBlock targetBlock = step.targetPart.program.basicBlockAt(step.source);
            if (targetBlock.instructionCount() > 0) {
                continue;
            }
            BasicBlock sourceBlock = program.basicBlockAt(step.source);
            int end = step.sourceIndex;
            boolean asyncOccured = false;
            for (int i = step.sourceIndex; i < sourceBlock.getInstructions().size(); ++i) {
                end = i;
                Instruction insn = sourceBlock.getInstructions().get(i);
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction)insn;
                    if (asyncMethods.contains(invoke.getMethod())) {
                        asyncOccured = true;
                        long key = ((long)step.source << 32) | i;
                        if (partMap.containsKey(key)) {
                            step.targetPart.blockSuccessors[step.sourceIndex] = partMap.get(key);
                            break;
                        }
                        Program nextProgram = createStubCopy(program);
                        BasicBlock nextBlock = nextProgram.basicBlockAt(step.source);
                        if (step.source > 0) {
                            JumpInstruction jumpToNextBlock = new JumpInstruction();
                            jumpToNextBlock.setTarget(nextBlock);
                            nextProgram.basicBlockAt(0).getInstructions().add(jumpToNextBlock);
                        }
                        Part part = new Part();
                        part.input = invoke.getReceiver() != null ? invoke.getReceiver().getIndex() : null;
                        part.program = nextProgram;
                        int partId = parts.size();
                        part.blockSuccessors = new int[program.basicBlockCount()];
                        Arrays.fill(part.blockSuccessors, partId);
                        partMap.put(key, partId);
                        step.targetPart.blockSuccessors[step.source] = partId;
                        parts.add(part);
                        Step next = new Step();
                        next.source = step.source;
                        next.sourceIndex = i + 1;
                        next.targetPart = part;
                        queue.add(next);
                        break;
                    }
                }
            }
            targetBlock.getInstructions().addAll(ProgramUtils.copyInstructions(sourceBlock, step.sourceIndex, end + 1,
                    targetBlock.getProgram()));
            if (step.sourceIndex == 0) {
                targetBlock.getPhis().addAll(ProgramUtils.copyPhis(sourceBlock, targetBlock.getProgram()));
            }
            ProgramUtils.copyTryCatches(sourceBlock, targetBlock.getProgram());
            for (TryCatchBlock tryCatch : targetBlock.getTryCatchBlocks()) {
                if (tryCatch.getHandler() != null) {
                    Step next = new Step();
                    next.source = tryCatch.getHandler().getIndex();
                    next.sourceIndex = 0;
                    next.targetPart = step.targetPart;
                    queue.add(next);
                }
            }
            if (!asyncOccured) {
                InstructionTransitionExtractor successorExtractor = new InstructionTransitionExtractor();
                sourceBlock.getLastInstruction().acceptVisitor(successorExtractor);
                for (BasicBlock successor : successorExtractor.getTargets()) {
                    BasicBlock targetSuccessor = targetBlock.getProgram().basicBlockAt(successor.getIndex());
                    if (targetSuccessor.instructionCount() == 0) {
                        Step next = new Step();
                        next.source = successor.getIndex();
                        next.sourceIndex = 0;
                        next.targetPart = step.targetPart;
                        queue.add(next);
                    }
                }
            }
        }

        partMap.clear();
    }

    private Program createStubCopy(Program program) {
        Program copy = new Program();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            copy.createBasicBlock();
        }
        for (int i = 0; i < program.variableCount(); ++i) {
            copy.createVariable();
        }
        return copy;
    }

    public int size() {
        return parts.size();
    }

    public Program getProgram(int index) {
        return parts.get(index).program;
    }

    public Integer getInput(int index) {
        return parts.get(index).input;
    }

    public int[] getBlockSuccessors(int index) {
        int[] result = parts.get(index).blockSuccessors;
        return Arrays.copyOf(result, result.length);
    }

    private static class Part {
        Program program;
        Integer input;
        int[] blockSuccessors;
    }

    private static class Step {
        Part targetPart;
        int source;
        int sourceIndex;
    }
}
