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
package org.teavm.common;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DJGraph {
    private DominatorTree domTree;
    private MutableDirectedGraph cfg;
    private MutableDirectedGraph graph;
    private LCATree spanningTree;
    private int[] spanningTreeNode;
    private int[] spanningTreeIndex;
    private int[][] levelContent;
    private int[] mergeRoot;
    private int[] weight;
    private IntegerArray[] mergeClasses;

    public DJGraph(Graph src, int[] weight) {
        if (src.size() != weight.length) {
            throw new IllegalArgumentException("Node count " + src.size() + " is not equal to weight array "
                    + weight.length);
        }
        this.cfg = new MutableDirectedGraph(src);
        domTree = GraphUtils.buildDominatorTree(src);
        buildGraph(src);
        buildLevels();
        spanningTree = new LCATree(src.size());
        dfs();
        mergeRoot = new int[src.size()];
        mergeClasses = new IntegerArray[src.size()];
        for (int i = 0; i < mergeRoot.length; ++i) {
            mergeRoot[i] = i;
            mergeClasses[i] = IntegerArray.of(i);
        }
        this.weight = Arrays.copyOf(weight, weight.length);
    }

    private void buildGraph(Graph src) {
        graph = new MutableDirectedGraph();

        // Add join edges
        for (int i = 0; i < src.size(); ++i) {
            for (int j : src.outgoingEdges(i)) {
                graph.addEdge(i, j);
            }
        }

        // Add dom edges
        for (int i = 0; i < graph.size(); ++i) {
            int j = domTree.immediateDominatorOf(i);
            if (j >= 0) {
                graph.addEdge(j, i);
            }
        }
    }

    private void buildLevels() {
        List<IntegerArray> builder = new ArrayList<>();
        for (int i = 0; i < graph.size(); ++i) {
            int level = domTree.levelOf(i);
            while (level >= builder.size()) {
                builder.add(new IntegerArray(1));
            }
            builder.get(level).add(i);
        }
        levelContent = new int[builder.size() - 1][];
        for (int i = 1; i < builder.size(); ++i) {
            levelContent[i - 1] = builder.get(i).getAll();
        }
    }

    private void dfs() {
        spanningTreeNode = new int[graph.size()];
        spanningTreeIndex = new int[graph.size()];
        Arrays.fill(spanningTreeIndex, -1);
        Arrays.fill(spanningTreeNode, -1);
        boolean[] visited = new boolean[graph.size()];
        IntegerStack stack = new IntegerStack(graph.size() * 2);
        stack.push(-1);
        stack.push(0);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            int source = stack.pop();
            if (visited[node]) {
                continue;
            }
            int index = source >= 0 ? spanningTree.addNode(spanningTreeIndex[source]) : 0;
            spanningTreeNode[index] = node;
            spanningTreeIndex[node] = index;
            visited[node] = true;
            for (int succ : graph.outgoingEdges(node)) {
                stack.push(node);
                stack.push(succ);
            }
        }
    }

    public DominatorTree getDomTree() {
        return domTree;
    }

    public MutableDirectedGraph getCfg() {
        return cfg;
    }

    public Graph getGraph() {
        return graph;
    }

    public boolean isAncestorInSpanningTree(int anc, int node) {
        anc = spanningTreeIndex[mergeRoot[anc]];
        node = spanningTreeIndex[mergeRoot[node]];
        if (anc < 0 || node < 0) {
            return false;
        }
        return spanningTree.lcaOf(anc, node) == anc;
    }

    public boolean isDomEdge(int i, int j) {
        return domTree.immediateDominatorOf(mergeRoot[j]) == mergeRoot[i];
    }

    public boolean isJoinEdge(int i, int j) {
        return !isDomEdge(i, j);
    }

    public boolean isBackJoin(int i, int j) {
        return isJoinEdge(i, j) && domTree.dominates(mergeRoot[j], mergeRoot[i]);
    }

    public boolean isCrossJoin(int i, int j) {
        return isJoinEdge(i, j) && !domTree.dominates(mergeRoot[j], mergeRoot[i]);
    }

    public boolean isSpanningBack(int i, int j) {
        i = spanningTreeIndex[mergeRoot[i]];
        j = spanningTreeIndex[mergeRoot[j]];
        return spanningTree.lcaOf(i, j) == j;
    }

    public boolean isSpanningCross(int i, int j) {
        i = spanningTreeIndex[mergeRoot[i]];
        j = spanningTreeIndex[mergeRoot[j]];
        int c = spanningTree.lcaOf(i, j);
        return c != i && c != j;
    }

    public int weightOf(int node) {
        return weight[node];
    }

    public int weightOf(int... nodes) {
        int result = 0;
        for (int node : nodes) {
            result += weight[node];
        }
        return result;
    }

    public int levelOf(int node) {
        return domTree.levelOf(mergeRoot[node]) - 1;
    }

    public int[] level(int level) {
        int[] result = levelContent[level];
        return Arrays.copyOf(result, result.length);
    }

    public int levelCount() {
        return levelContent.length;
    }

    public int[] classRepresentatives(int node) {
        return mergeClasses[node].getAll();
    }

    public int classOf(int node) {
        return mergeRoot[node];
    }

    public int collapse(int[] nodes) {
        // Replace nodes with their classes and find common dominator among them
        IntSet set = new IntOpenHashSet();
        int top = nodes[0];
        for (int node : nodes) {
            node = mergeRoot[node];
            top = domTree.commonDominatorOf(top, node);
            set.add(node);
        }
        if (!set.contains(top)) {
            throw new IllegalArgumentException("All nodes must have one common dominator");
        }

        // Alter classes
        IntegerArray cls = mergeClasses[top];
        for (IntCursor node : set) {
            mergeRoot[node.value] = top;
            if (node.value != top) {
                cls.addAll(mergeClasses[node.value].getAll());
                mergeClasses[node.value].clear();
            }
            weight[top] += weight[node.value];
        }

        // Alter graphs
        for (IntCursor node : set) {
            if (node.value != top) {
                for (int succ : graph.outgoingEdges(node.value)) {
                    graph.addEdge(top, succ);
                }
                for (int pred : graph.incomingEdges(node.value)) {
                    graph.addEdge(top, pred);
                }
                graph.detachNode(node.value);

                for (int succ : cfg.outgoingEdges(node.value)) {
                    cfg.addEdge(top, succ);
                }
                for (int pred : cfg.incomingEdges(node.value)) {
                    cfg.addEdge(top, pred);
                }
                cfg.detachNode(node.value);
            }
        }
        return top;
    }
}
