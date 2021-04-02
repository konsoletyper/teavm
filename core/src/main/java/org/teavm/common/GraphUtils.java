/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.common;

import com.carrotsearch.hppc.IntStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public final class GraphUtils {
    static final byte NONE = 0;
    static final byte VISITING = 1;
    static final byte VISITED = 2;

    private GraphUtils() {
    }

    public static int[] findBackEdges(Graph graph) {
        int sz = graph.size();
        int[] stack = new int[sz * 2];
        int stackSize = 0;
        byte[] state = new byte[sz];
        for (int i = 0; i < sz; ++i) {
            if (graph.incomingEdgesCount(i) == 0) {
                stack[stackSize++] = i;
            }
        }
        IntegerArray result = new IntegerArray(2);
        while (stackSize > 0) {
            int node = stack[--stackSize];
            switch (state[node]) {
                case NONE:
                    state[node] = VISITING;
                    stack[stackSize++] = node;
                    for (int next : graph.outgoingEdges(node)) {
                        switch (state[next]) {
                            case NONE:
                                stack[stackSize++] = next;
                                break;
                            case VISITING:
                                result.add(node);
                                result.add(next);
                                break;
                        }
                    }
                    break;
                case VISITING:
                    state[node] = VISITED;
                    break;
            }
        }
        return result.getAll();
    }

    public static Graph removeLoops(Graph graph) {
        int sz = graph.size();
        int[] stack = new int[sz * 2];
        int stackSize = 0;
        byte[] state = new byte[sz];
        for (int i = 0; i < sz; ++i) {
            if (graph.incomingEdgesCount(i) == 0) {
                stack[stackSize++] = i;
            }
        }
        GraphBuilder builder = new GraphBuilder(graph.size());
        while (stackSize > 0) {
            int node = stack[--stackSize];
            switch (state[node]) {
                case NONE:
                    state[node] = VISITING;
                    stack[stackSize++] = node;
                    for (int next : graph.outgoingEdges(node)) {
                        switch (state[next]) {
                            case NONE:
                                stack[stackSize++] = next;
                                builder.addEdge(node, next);
                                break;
                            case VISITED:
                                builder.addEdge(node, next);
                                break;
                        }
                    }
                    break;
                case VISITING:
                    state[node] = VISITED;
                    break;
            }
        }
        return builder.build();
    }

    public static boolean isIrreducible(Graph graph) {
        DominatorTree dom = buildDominatorTree(graph);
        int[] backEdges = findBackEdges(graph);
        for (int i = 0; i < backEdges.length; i += 2) {
            if (!dom.dominates(backEdges[i + 1], backEdges[i])) {
                return true;
            }
        }
        return false;
    }

    public static Graph subgraph(Graph graph, IntPredicate filter) {
        if (graph instanceof FilteredGraph) {
            FilteredGraph filteredGraph = (FilteredGraph) graph;
            IntPredicate oldFilter = filteredGraph.filter;
            IntPredicate newFilter = v -> oldFilter.test(v) && filter.test(v);
            return new FilteredGraph(filteredGraph.innerGraph, newFilter);
        } else {
            return new FilteredGraph(graph, filter);
        }
    }

    static class FilteredGraph implements Graph {
        final Graph innerGraph;
        final IntPredicate filter;

        FilteredGraph(Graph innerGraph, IntPredicate filter) {
            this.innerGraph = innerGraph;
            this.filter = filter;
        }

        @Override
        public int size() {
            return innerGraph.size();
        }

        @Override
        public int[] incomingEdges(int node) {
            if (!filter.test(node)) {
                return new int[0];
            }
            return filterNodes(innerGraph.incomingEdges(node));
        }

        private int[] filterNodes(int[] nodes) {
            int j = 0;
            for (int v : nodes) {
                if (filter.test(v)) {
                    nodes[j++] = v;
                }
            }
            if (j < nodes.length) {
                nodes = Arrays.copyOf(nodes, j);
            }
            return nodes;
        }

        @Override
        public int copyIncomingEdges(int node, int[] target) {
            if (!filter.test(node)) {
                return 0;
            }
            int[] result = incomingEdges(node);
            System.arraycopy(result, 0, target, 0, result.length);
            return result.length;
        }

        @Override
        public int[] outgoingEdges(int node) {
            if (!filter.test(node)) {
                return new int[0];
            }
            return filterNodes(innerGraph.outgoingEdges(node));
        }

        @Override
        public int copyOutgoingEdges(int node, int[] target) {
            if (!filter.test(node)) {
                return 0;
            }
            int[] result = outgoingEdges(node);
            System.arraycopy(result, 0, target, 0, result.length);
            return result.length;
        }

        @Override
        public int incomingEdgesCount(int node) {
            return incomingEdges(node).length;
        }

        @Override
        public int outgoingEdgesCount(int node) {
            return outgoingEdges(node).length;
        }

        @Override
        public String toString() {
            return GraphUtils.printToDot(this);
        }
    }

    /*
     * Tarjan's algorithm
     * See pseudocode at https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
     * This is a stackless version.
     */
    public static int[][] findStronglyConnectedComponents(Graph graph) {
        List<int[]> components = new ArrayList<>();

        int index = 0;
        IntStack procStack = new IntStack();
        IntStack stack = new IntStack();
        int[] nodeIndex = new int[graph.size()];
        int[] nodeLowLink = new int[graph.size()];
        boolean[] nodeOnStack = new boolean[graph.size()];

        Arrays.fill(nodeIndex, -1);
        Arrays.fill(nodeLowLink, -2);

        for (int i = 0; i < graph.size(); ++i) {
            procStack.push(i);
            procStack.push(0);
        }

        while (!procStack.isEmpty()) {
            int state = procStack.pop();
            int v = procStack.pop();

            switch (state) {
                case 0: {
                    if (nodeIndex[v] >= 0) {
                        break;
                    }
                    nodeIndex[v] = index;
                    nodeLowLink[v] = index;
                    index++;
                    stack.push(v);
                    nodeOnStack[v] = true;

                    procStack.push(v);
                    procStack.push(3);

                    for (int w : graph.outgoingEdges(v)) {
                        procStack.push(w);
                        procStack.push(v);
                        procStack.push(1);
                    }
                    break;
                }

                case 1: {
                    int w = procStack.pop();

                    if (nodeIndex[w] < 0) {
                        procStack.push(w);
                        procStack.push(v);
                        procStack.push(2);

                        procStack.push(w);
                        procStack.push(0);
                    } else if (nodeOnStack[w]) {
                        nodeLowLink[v] = Math.min(nodeLowLink[v], nodeIndex[w]);
                    }

                    break;
                }

                case 2: {
                    int w = procStack.pop();
                    nodeLowLink[v] = Math.min(nodeLowLink[v], nodeLowLink[w]);
                    break;
                }

                case 3: {
                    if (nodeLowLink[v] == nodeIndex[v]) {
                        IntegerArray scc = new IntegerArray(4);
                        int w;
                        do {
                            w = stack.pop();
                            nodeOnStack[w] = false;
                            scc.add(w);
                        } while (w != v);

                        if (scc.size() > 1) {
                            components.add(scc.getAll());
                        } else {
                            for (int successor : graph.outgoingEdges(v)) {
                                if (successor == v) {
                                    components.add(scc.getAll());
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        return components.toArray(new int[0][]);
    }

    public static DominatorTree buildDominatorTree(Graph graph) {
        return buildDominatorTree(graph, 0);
    }

    public static DominatorTree buildDominatorTree(Graph graph, int start) {
        DominatorTreeBuilder builder = new DominatorTreeBuilder(graph, start);
        builder.build();
        return new DefaultDominatorTree(builder.dominators, builder.vertices);
    }

    public static Graph buildDominatorGraph(DominatorTree domTree, int sz) {
        GraphBuilder graph = new GraphBuilder(sz);
        for (int i = 0; i < sz; ++i) {
            int idom = domTree.immediateDominatorOf(i);
            if (idom >= 0) {
                graph.addEdge(idom, i);
            }
        }
        return graph.build();
    }

    public static int[] dfs(Graph graph) {
        int[] result = new int[graph.size()];
        int[] state = new int[graph.size()];
        int[] stack = new int[graph.size() * 2];
        int top = 0;
        stack[top++] = 0;
        int index = graph.size();

        while (top > 0) {
            int node = stack[--top];
            switch (state[node]) {
                case 0:
                    state[node] = 1;
                    stack[top++] = node;
                    for (int successor : graph.outgoingEdges(node)) {
                        if (state[successor] == 0) {
                            stack[top++] = successor;
                        }
                    }
                    break;
                case 1:
                    result[node] = --index;
                    state[node] = 2;
                    break;
            }
        }

        return result;
    }

    public static void splitIrreducibleGraph(Graph graph, int[] weights, GraphSplittingBackend backend) {
        new IrreducibleGraphSplitter(backend, graph, weights).splitLoops();
    }

    public static int[][] findDominanceFrontiers(Graph cfg, DominatorTree domTree) {
        IntegerArray[] tmpFrontiers = new IntegerArray[cfg.size()];
        int[][] domFrontiers = new int[cfg.size()][];

        // For each node calculate the number of descendants in dominator tree
        int[] descCount = new int[cfg.size()];
        for (int i = 0; i < cfg.size(); ++i) {
            int idom = domTree.immediateDominatorOf(i);
            if (idom >= 0) {
                descCount[idom]++;
            }
        }

        // Push final nodes onto stack
        int[] stack = new int[cfg.size() * 2];
        int head = 0;
        for (int i = 0; i < cfg.size(); ++i) {
            if (descCount[i] == 0) {
                stack[head++] = i;
            }
        }

        // Process dominator tree in bottom-up order
        while (head > 0) {
            int node = stack[--head];
            IntegerArray frontier = tmpFrontiers[node];
            if (frontier == null) {
                frontier = new IntegerArray(1);
            }
            int idom = domTree.immediateDominatorOf(node);
            for (int successor : cfg.outgoingEdges(node)) {
                // If successor's immediate dominator is not the node,
                // then add successor to node's dominance frontiers
                if (domTree.immediateDominatorOf(successor) != node) {
                    frontier.add(successor);
                }
            }

            tmpFrontiers[node] = null;
            int[] frontierSet = makeSet(frontier);
            domFrontiers[node] = frontierSet;

            if (idom >= 0) {
                // Propagate current set to immediate dominator
                for (int element : frontierSet) {
                    if (domTree.immediateDominatorOf(element) != idom) {
                        IntegerArray idomFrontier = tmpFrontiers[idom];
                        if (idomFrontier == null) {
                            idomFrontier = new IntegerArray(1);
                            tmpFrontiers[idom] = idomFrontier;
                        }
                        idomFrontier.add(element);
                    }
                }

                // Schedule processing the immediate dominator if all of its ancestors
                // in dominator tree have been processed
                if (--descCount[idom] == 0) {
                    stack[head++] = idom;
                }
            }
        }

        return domFrontiers;
    }

    private static int[] makeSet(IntegerArray array) {
        int[] items = array.getAll();
        int[] set = new int[items.length];
        int sz = 0;
        int last = -1;
        for (int item : items) {
            if (item != last) {
                set[sz++] = item;
                last = item;
            }
        }
        if (sz != set.length) {
            set = Arrays.copyOf(set, sz);
        }
        return set;
    }

    public static String printToDot(Graph graph) {
        StringBuilder sb = new StringBuilder("digraph G {\n");
        for (int i = 0; i < graph.size(); ++i) {
            String successors = Arrays.stream(graph.outgoingEdges(i))
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", "));
            sb.append("  ").append(i).append(" -> {").append(successors).append("}\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
