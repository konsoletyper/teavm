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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.util.ProgramUtils;

public class ClassInitElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dom = GraphUtils.buildDominatorTree(cfg);
        Graph domGraph = GraphUtils.buildDominatorGraph(dom, program.basicBlockCount());

        Step start = new Step(0);
        start.initializedClasses.add(context.getMethod().getOwnerName());
        Deque<Step> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            Step step = stack.pop();
            int node = step.node;
            BasicBlock block = program.basicBlockAt(node);

            Instruction nextInsn;
            for (Instruction insn = block.getFirstInstruction(); insn != null; insn = nextInsn) {
                nextInsn = insn.getNext();
                if (insn instanceof InitClassInstruction) {
                    InitClassInstruction initClass = (InitClassInstruction) insn;
                    if (!step.initializedClasses.add(initClass.getClassName())) {
                        insn.delete();
                    }
                    continue;
                }
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;
                    step.initializedClasses.add(invoke.getMethod().getClassName());
                }
            }

            for (int successor : domGraph.outgoingEdges(node)) {
                Step next = new Step(successor);
                next.initializedClasses.addAll(step.initializedClasses);
                stack.push(next);
            }
        }

        return false;
    }

    class Step {
        int node;
        Set<String> initializedClasses = new HashSet<>();

        public Step(int node) {
            this.node = node;
        }
    }
}
