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
import java.util.Comparator;

/**
 *
 * @author Alexey Andreev
 */
public class GraphIndexer {
    static final byte NONE = 0;
    static final byte VISITING = 1;
    static final byte VISITED = 2;
    private int[] indexToNode;
    private int[] nodeToIndex;
    private Graph graph;

    public GraphIndexer(Graph graph) {
        sort(graph);
    }

    private static class LoopEntrance {
        int head;
        int follower;
    }

    private int sort(Graph graph) {
        LoopGraph loopGraph = new LoopGraph(graph);
        int sz = graph.size();
        int[] indexToNode = new int[sz + 1];
        int[] nodeToIndex = new int[sz + 1];
        int[] visitIndex = new int[sz + 1];
        Arrays.fill(nodeToIndex, -1);
        Arrays.fill(indexToNode, -1);
        Arrays.fill(visitIndex, -1);
        byte[] state = new byte[sz];
        int lastIndex = 0;
        int lastVisitIndex = 0;
        IntegerStack stack = new IntegerStack(sz * 2);
        stack.push(loopGraph.loopAt(0) != null ? loopGraph.loopAt(0).getHead() : 0);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            switch (state[node]) {
                case VISITING: {
                    state[node] = VISITED;
                    nodeToIndex[node] = lastIndex++;
                    break;
                }
                case NONE: {
                    visitIndex[node] = lastVisitIndex++;
                    state[node] = VISITING;
                    stack.push(node);
                    int[] successors = graph.outgoingEdges(node);
                    LoopEntrance[] edges = new LoopEntrance[successors.length];
                    for (int i = 0; i < edges.length; ++i) {
                        int successor = successors[i];
                        Loop successorLoop = loopGraph.loopAt(successor);
                        LoopEntrance edge = new LoopEntrance();
                        edge.head = successorLoop != null ?
                                visitIndex[successorLoop.getHead()] : -1;
                        edge.follower = successor;
                        edges[i] = edge;
                    }
                    Arrays.sort(edges, new Comparator<LoopEntrance>() {
                        @Override
                        public int compare(LoopEntrance o1, LoopEntrance o2) {
                            return Integer.compare(o2.head, o1.head);
                        }
                    });
                    for (LoopEntrance edge : edges) {
                        int next = edge.follower;
                        switch (state[next]) {
                            case NONE:
                                stack.push(next);
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                }
            }
        }
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
        this.indexToNode = indexToNode;
        this.nodeToIndex = nodeToIndex;
        return lastIndex + 1;
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
