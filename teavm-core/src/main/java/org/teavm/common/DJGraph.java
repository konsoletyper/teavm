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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DJGraph {
    private DominatorTree domTree;
    private Graph graph;
    private LCATree spanningTree;
    private int[] spanningTreeNode;
    private int[] spanningTreeIndex;
    private int[][] levelContent;

    public DJGraph(Graph src) {
        domTree = GraphUtils.buildDominatorTree(src);
        buildGraph(src);
        buildLevels();
        dfs();
    }

    private void buildGraph(Graph graph) {
        GraphBuilder builder = new GraphBuilder(graph.size());

        // Add join edges
        for (int i = 0; i < graph.size(); ++i) {
            for (int j : graph.outgoingEdges(i)) {
                builder.addEdge(i, j);
            }
        }

        // Add dom edges
        for (int i = 1; i < graph.size(); ++i) {
            int j = domTree.immediateDominatorOf(i);
            builder.addEdge(j, i);
        }

        graph = builder.build();
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
        levelContent = new int[builder.size()][];
        for (int i = 0; i < builder.size(); ++i) {
            levelContent[i] = builder.get(i).getAll();
        }
    }

    private void dfs() {
        spanningTreeNode = new int[graph.size()];
        spanningTreeIndex = new int[graph.size()];
        Arrays.fill(spanningTreeIndex, -1);
        Arrays.fill(spanningTreeNode, -1);
        boolean[] visited = new boolean[graph.size()];
        IntegerStack stack = new IntegerStack(graph.size() * 2);
        stack.push(0);
        stack.push(-1);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            int source = stack.pop();
            if (visited[node]) {
                continue;
            }
            int index = spanningTree.addNode(spanningTreeIndex[source]);
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

    public Graph getGraph() {
        return graph;
    }

    public boolean isAncestorInSpanningTree(int anc, int node) {
        anc = spanningTreeIndex[anc];
        node = spanningTreeIndex[node];
        if (anc < 0 || node < 0) {
            return false;
        }
        return spanningTree.lcaOf(anc, node) == anc;
    }

    public boolean isDomEdge(int i, int j) {
        return domTree.immediateDominatorOf(j) == i;
    }

    public boolean isJoinEdge(int i, int j) {
        return !isDomEdge(i, j);
    }

    public boolean isBackJoin(int i, int j) {
        return isJoinEdge(i, j) && !domTree.dominates(j, i);
    }

    public boolean isCrossJoin(int i, int j) {
        return isJoinEdge(i, j) && domTree.dominates(j, i);
    }

    public boolean isSpanningBack(int i, int j) {
        return spanningTree.lcaOf(i, j) == j;
    }

    public boolean isSpanningCross(int i, int j) {
        int c = spanningTree.lcaOf(i, j);
        return c != i && c != j;
    }

    public int levelOf(int node) {
        return domTree.levelOf(node);
    }

    public int[] level(int level) {
        int[] result = levelContent[level];
        return Arrays.copyOf(result, result.length);
    }

    public int levelCount() {
        return levelContent.length;
    }
}
