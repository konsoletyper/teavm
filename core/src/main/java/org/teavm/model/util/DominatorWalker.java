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
package org.teavm.model.util;

import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;

public class DominatorWalker {
    private Program program;
    private DominatorTree dom;
    private Graph domGraph;

    public DominatorWalker(Program program) {
        this.program = program;
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        dom = GraphUtils.buildDominatorTree(cfg);
        domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
    }

    public <T> void walk(DominatorWalkerCallback<T> callback) {
        int[] stack = new int[program.basicBlockCount() * 2];
        Object[] stateStack = new Object[stack.length];
        boolean[] backward = new boolean[stack.length];
        callback.setDomTree(dom);

        int head = 1;
        while (head > 0) {
            int node = stack[--head];
            BasicBlock block = program.basicBlockAt(node);
            if (backward[head]) {
                @SuppressWarnings("unchecked")
                T state = (T) stateStack[head];
                callback.endVisit(block, state);
            } else if (callback.filter(block)) {
                stack[head] = node;
                backward[head] = true;
                stateStack[head] = callback.visit(block);
                head++;

                int[] successors = domGraph.outgoingEdges(node);
                for (int i = successors.length - 1; i >= 0; --i) {
                    stack[head] = successors[i];
                    backward[head] = false;
                    head++;
                }
            }
        }
    }
}
