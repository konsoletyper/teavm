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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntStack;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;

public class DominatorWalker {
    private Program program;
    private DominatorTree dom;
    private Graph cfg;
    private Graph domGraph;
    private int[] roots;
    private int[] order;

    public DominatorWalker(Program program) {
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        dom = GraphUtils.buildDominatorTree(cfg);
        buildWalkTree();
        order = dfs(cfg);
    }

    // Callbacks rely on facts that a block establishes holding in every block visited under it. This is true
    // only if every path to a block passes through the whole body of its parent. An exception can leave a
    // protected block before its instructions run, so facts that a protected block establishes don't hold in its
    // exception handlers, nor in blocks reachable from those handlers. Therefore build the dominator tree of
    // a graph where node 2 * i is the entry of block i and node 2 * i + 1 is its body, with exception edges
    // leaving the entry node, and visit each block under the nearest block whose body dominates it.
    // A block without such a block, like a handler of a protected first block, is visited as a separate root.
    private void buildWalkTree() {
        int blockCount = program.basicBlockCount();
        DominatorTree splitDom = GraphUtils.buildDominatorTree(ProgramUtils.buildControlFlowGraph2(program));
        GraphBuilder builder = new GraphBuilder(blockCount);
        IntArrayList rootList = new IntArrayList();
        rootList.add(0);
        for (int i = 1; i < blockCount; ++i) {
            int node = i * 2;
            int parent = splitDom.immediateDominatorOf(node);
            while (parent >= 0 && parent % 2 == 0) {
                node = parent;
                parent = splitDom.immediateDominatorOf(node);
            }
            if (parent >= 0) {
                builder.addEdge(parent / 2, i);
            } else if (node == 0) {
                rootList.add(i);
            }
        }
        domGraph = builder.build();
        roots = rootList.toArray();
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

        int head = 0;
        for (int i = roots.length - 1; i >= 0; --i) {
            stack[head++] = roots[i];
        }
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
