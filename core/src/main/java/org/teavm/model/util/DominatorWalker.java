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

import com.carrotsearch.hppc.IntStack;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;

public class DominatorWalker {
    private Program program;
    private DominatorTree dom;
    private Graph cfg;
    private Graph domGraph;
    private int[] order;

    public DominatorWalker(Program program) {
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        dom = GraphUtils.buildDominatorTree(cfg);
        domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
        order = dfs(cfg);
    }

    private int[] dfs(Graph graph) {
        if (graph.size() == 0) {
            return new int[0];
        }

        int index = 0;
        int[] result = new int[graph.size()];
        IntStack stack = new IntStack(graph.size());
        byte[] state = new byte[graph.size()];
        stack.push(0);

        while (!stack.isEmpty()) {
            int node = stack.pop();
            switch (state[node]) {
                case 0: {
                    state[node] = 1;
                    stack.push(node);
                    for (int succ : graph.outgoingEdges(node)) {
                        if (state[succ] == 0) {
                            stack.push(succ);
                        }
                    }
                    break;
                }
                case 1: {
                    state[node] = 2;
                    result[node] = index++;
                    break;
                }
            }
        }

        return result;
    }

    public <T> void walk(DominatorWalkerCallback<T> callback) {
        int[] stack = new int[program.basicBlockCount() * 2];
        Object[] stateStack = new Object[stack.length];
        boolean[] backward = new boolean[stack.length];
        ContextImpl context = new ContextImpl(dom, cfg, findExceptionHandlers());
        callback.setContext(context);

        int head = 1;
        while (head > 0) {
            int node = stack[--head];
            BasicBlock block = program.basicBlockAt(node);
            if (backward[head]) {
                @SuppressWarnings("unchecked")
                T state = (T) stateStack[head];
                callback.endVisit(block, state);
                context.visited[block.getIndex()] = true;
            } else if (callback.filter(block)) {
                stack[head] = node;
                backward[head] = true;
                stateStack[head] = callback.visit(block);
                head++;

                int[] successors = domGraph.outgoingEdges(node);
                sort(successors);
                for (int i = successors.length - 1; i >= 0; --i) {
                    stack[head] = successors[i];
                    backward[head] = false;
                    head++;
                }
            }
        }
    }

    private boolean[] findExceptionHandlers() {
        boolean[] exceptionHandlers = new boolean[program.basicBlockCount()];
        for (BasicBlock block : program.getBasicBlocks()) {
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                exceptionHandlers[tryCatch.getHandler().getIndex()] = true;
            }
        }
        return exceptionHandlers;
    }

    private void sort(int[] nodes) {
        for (int i = 0; i < nodes.length - 1; ++i) {
            int a = nodes[i];
            int bestIndex = i;
            int best = order[a];
            for (int j = i + 1; j < nodes.length; ++j) {
                int b = nodes[j];
                int score = order[b];
                if (score < best) {
                    best = score;
                    a = b;
                    bestIndex = j;
                }
            }
            if (i != bestIndex) {
                nodes[bestIndex] = nodes[i];
                nodes[i] = a;
            }
        }
    }

    static class ContextImpl implements DominatorWalkerContext {
        private boolean[] visited;
        private boolean[] isExceptionHandler;
        private DominatorTree dom;
        private Graph cfg;

        ContextImpl(DominatorTree dom, Graph cfg, boolean[] isExceptionHandler) {
            this.dom = dom;
            this.cfg = cfg;
            this.isExceptionHandler = isExceptionHandler;
            visited = new boolean[cfg.size()];
        }

        @Override
        public DominatorTree getDominatorTree() {
            return dom;
        }

        @Override
        public Graph getControlFlowGraph() {
            return cfg;
        }

        @Override
        public boolean isVisited(int blockIndex) {
            return visited[blockIndex];
        }

        @Override
        public boolean isExceptionHandler(int blockIndex) {
            return isExceptionHandler[blockIndex];
        }
    }
}
