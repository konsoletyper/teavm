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

import java.util.Arrays;

public class GraphIndexer {
    private static final byte NONE = 0;
    private static final byte VISITING = 1;
    private static final byte VISITED = 2;
    private int[] indexToNode;
    private int[] nodeToIndex;
    private Graph graph;
    private int lastIndex;

    public GraphIndexer(Graph graph) {
        int sz = graph.size();
        indexToNode = new int[sz + 1];
        nodeToIndex = new int[sz + 1];
        Arrays.fill(nodeToIndex, -1);
        Arrays.fill(indexToNode, -1);
        this.graph = graph;
        sort(graph);
        --lastIndex;
        for (int node = 0; node < sz; ++node) {
            int index = nodeToIndex[node];
            if (index < 0) {
                continue;
            }
            index = lastIndex - index;
            nodeToIndex[node] = index;
            indexToNode[index] = node;
        }
        indexToNode[sz] = sz;
        nodeToIndex[sz] = sz;
        GraphBuilder sorted = new GraphBuilder(lastIndex + 2);
        for (int i = 0; i <= lastIndex; ++i) {
            for (int next : graph.outgoingEdges(indexToNode[i])) {
                sorted.addEdge(i, nodeToIndex[next]);
            }
        }
        this.graph = sorted.build();
    }

    private void sort(Graph graph) {
        int sz = graph.size();
        byte[] state = new byte[sz];
        IntegerStack stack = new IntegerStack(sz * 2);
        stack.push(0);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            switch (state[node]) {
                case VISITING: {
                    state[node] = VISITED;
                    nodeToIndex[node] = lastIndex++;
                    break;
                }
                case NONE: {
                    state[node] = VISITING;
                    stack.push(node);

                    int[] successors = graph.outgoingEdges(node);
                    for (int successor : successors) {
                        if (state[successor] == NONE) {
                            stack.push(successor);
                        }
                    }
                    break;
                }
            }
        }
    }

    public int nodeAt(int index) {
        return indexToNode[index];
    }

    public int indexOf(int node) {
        return nodeToIndex[node];
    }

    public int size() {
        return indexToNode.length - 1;
    }

    public Graph getGraph() {
        return graph;
    }
}
