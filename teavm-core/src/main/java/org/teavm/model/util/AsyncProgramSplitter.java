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
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class AsyncProgramSplitter {
    private List<Part> parts = new ArrayList<>();
    private Map<Long, Integer> partMap = new HashMap<>();
    private ClassReaderSource classSource;
    private Set<MethodReference> asyncMethods = new HashSet<>();

    public AsyncProgramSplitter(ClassReaderSource classSource, Set<MethodReference> asyncMethods) {
        this.classSource = classSource;
        this.asyncMethods = asyncMethods;
    }

    public void split(Program program) {
        parts.clear();
        Program initialProgram = createStubCopy(program);
        Part initialPart = new Part();
        initialPart.program = initialProgram;
        initialPart.blockSuccessors = new int[program.basicBlockCount()];
        Arrays.fill(initialPart.blockSuccessors, -1);
        parts.add(initialPart);
        partMap.put(0L, 0);
        Step initialStep = new Step();
        initialStep.source = 0;
        initialStep.targetPart = initialPart;
        Queue<Step> queue = new ArrayDeque<>();
        queue.add(initialStep);

        taskLoop: while (!queue.isEmpty()) {
            Step step = queue.remove();
            BasicBlock targetBlock = step.targetPart.program.basicBlockAt(step.source);
            if (targetBlock.instructionCount() > 0) {
                continue;
            }
            BasicBlock sourceBlock = program.basicBlockAt(step.source);
            int last = 0;
            for (int i = 0; i < sourceBlock.getInstructions().size(); ++i) {
                Instruction insn = sourceBlock.getInstructions().get(i);
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction)insn;
                    if (!asyncMethods.contains(findRealMethod(invoke.getMethod()))) {
                        continue;
                    }
                } else if (!(insn instanceof MonitorEnterInstruction)) {
                    continue;
                }

                // If we met asynchronous invocation...
                // Copy portion of current block from last occurrence (or from start) to i'th instruction.
                targetBlock.getInstructions().addAll(ProgramUtils.copyInstructions(sourceBlock,
                        last, i, targetBlock.getProgram()));
                targetBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(sourceBlock,
                        targetBlock.getProgram()));
                for (TryCatchBlock tryCatch : targetBlock.getTryCatchBlocks()) {
                    if (tryCatch.getHandler() != null) {
                        Step next = new Step();
                        next.source = tryCatch.getHandler().getIndex();
                        next.targetPart = step.targetPart;
                        queue.add(next);
                    }
                }
                last = i;

                // If this instruction already separates program, end with current block and refer to the
                // existing part
                long key = ((long)step.source << 32) | i;
                if (partMap.containsKey(key)) {
                    step.targetPart.blockSuccessors[targetBlock.getIndex()] = partMap.get(key);
                    continue taskLoop;
                }

                // Create a new part
                Program nextProgram = createStubCopy(program);
                Part part = new Part();
                part.program = nextProgram;
                int partId = parts.size();
                parts.add(part);
                part.blockSuccessors = new int[program.basicBlockCount() + 1];
                Arrays.fill(part.blockSuccessors, -1);

                // Mark current instruction as a separator and remember which part is in charge.
                partMap.put(key, partId);
                step.targetPart.blockSuccessors[targetBlock.getIndex()] = partId;

                // Continue with a new block in the new part
                targetBlock = nextProgram.createBasicBlock();
                if (step.source > 0) {
                    JumpInstruction jumpToNextBlock = new JumpInstruction();
                    jumpToNextBlock.setTarget(targetBlock);
                    nextProgram.basicBlockAt(0).getInstructions().add(jumpToNextBlock);
                    nextProgram.basicBlockAt(0).getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(sourceBlock,
                            nextProgram));
                }
                step.targetPart = part;
            }
            targetBlock.getInstructions().addAll(ProgramUtils.copyInstructions(sourceBlock,
                    last, sourceBlock.getInstructions().size(), targetBlock.getProgram()));
            targetBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(sourceBlock, targetBlock.getProgram()));
            for (TryCatchBlock tryCatch : targetBlock.getTryCatchBlocks()) {
                if (tryCatch.getHandler() != null) {
                    Step next = new Step();
                    next.source = tryCatch.getHandler().getIndex();
                    next.targetPart = step.targetPart;
                    queue.add(next);
                }
            }
            InstructionTransitionExtractor successorExtractor = new InstructionTransitionExtractor();
            sourceBlock.getLastInstruction().acceptVisitor(successorExtractor);
            for (BasicBlock successor : successorExtractor.getTargets()) {
                BasicBlock targetSuccessor = targetBlock.getProgram().basicBlockAt(successor.getIndex());
                if (targetSuccessor.instructionCount() == 0) {
                    Step next = new Step();
                    next.source = successor.getIndex();
                    next.targetPart = step.targetPart;
                    queue.add(next);
                }
            }
        }

        for (Part part : parts) {
            IntegerArray blockSuccessors = IntegerArray.of(part.blockSuccessors);
            AsyncProgramSplittingBackend splittingBackend = new AsyncProgramSplittingBackend(
                    new ProgramNodeSplittingBackend(part.program), blockSuccessors);
            Graph graph = ProgramUtils.buildControlFlowGraphWithTryCatch(part.program);
            int[] weights = new int[graph.size()];
            for (int i = 0; i < part.program.basicBlockCount(); ++i) {
                weights[i] = part.program.basicBlockAt(i).getInstructions().size();
            }
            GraphUtils.splitIrreducibleGraph(graph, weights, splittingBackend);
            part.blockSuccessors = splittingBackend.blockSuccessors.getAll();
        }
        partMap.clear();
    }

    private MethodReference findRealMethod(MethodReference method) {
        String clsName = method.getClassName();
        while (clsName != null) {
            ClassReader cls = classSource.get(clsName);
            if (cls == null) {
                break;
            }
            MethodReader methodReader = cls.getMethod(method.getDescriptor());
            if (methodReader != null) {
                return new MethodReference(clsName, method.getDescriptor());
            }
            clsName = cls.getParent();
            if (clsName != null && clsName.equals(cls.getName())) {
                break;
            }
        }
        return method;
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

    public int[] getBlockSuccessors(int index) {
        int[] result = parts.get(index).blockSuccessors;
        return Arrays.copyOf(result, result.length);
    }

    private static class Part {
        Program program;
        int[] blockSuccessors;
    }

    private static class Step {
        Part targetPart;
        int source;
    }

    private static class AsyncProgramSplittingBackend implements GraphSplittingBackend {
        private GraphSplittingBackend inner;
        private IntegerArray blockSuccessors;

        public AsyncProgramSplittingBackend(GraphSplittingBackend inner, IntegerArray blockSuccessors) {
            this.inner = inner;
            this.blockSuccessors = blockSuccessors;
        }

        @Override
        public int[] split(int[] domain, int[] nodes) {
            int[] copies = inner.split(domain, nodes);
            for (int i = 0; i < copies.length; ++i) {
                int copy = copies[i];
                int node = nodes[i];
                if (blockSuccessors.size() <= copy) {
                    blockSuccessors.add(-1);
                }
                blockSuccessors.set(copy, blockSuccessors.get(node));
            }
            return copies;
        }
    }
}
