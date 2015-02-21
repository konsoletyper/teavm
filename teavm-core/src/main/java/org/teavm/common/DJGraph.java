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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DJGraph {
    private DominatorTree domTree;
    private Graph graph;
    private Graph backEdges;

    public DJGraph(Graph src) {
        domTree = GraphUtils.buildDominatorTree(src);
        buildGraph(src);
        dfs();
    }

    private void buildGraph(Graph graph) {
        GraphBuilder builder = new GraphBuilder(graph.size());
        for (int i = 0; i < graph.size(); ++i) {
            for (int j : graph.outgoingEdges(i)) {
                builder.addEdge(i, j);
            }
        }
        for (int i = 1; i < graph.size(); ++i) {
            int j = domTree.immediateDominatorOf(i);
            boolean needsDomEdge = true;
            for (int k : graph.incomingEdges(i)) {
                if (k == j) {
                    needsDomEdge = false;
                    break;
                }
            }
            if (needsDomEdge) {
                builder.addEdge(j, i);
            }
        }
        graph = builder.build();
    }

    private void dfs() {
        GraphBuilder builder = new GraphBuilder();
        boolean[] visited = new boolean[graph.size()];
        IntegerStack stack = new IntegerStack(graph.size() * 2);
        stack.push(0);
        stack.push(-1);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            int source = stack.pop();
            if (visited[node]) {
                builder.addEdge(node, source);
                continue;
            }
            visited[node] = true;
            for (int succ : graph.outgoingEdges(node)) {
                stack.push(node);
                stack.push(succ);
            }
        }
        backEdges = builder.build();
    }

    public DominatorTree getDomTree() {
        return domTree;
    }

    public Graph getGraph() {
        return graph;
    }

    public int[] getSpanningTreeBackEdges(int node) {
        return backEdges.outgoingEdges(node);
    }
}
