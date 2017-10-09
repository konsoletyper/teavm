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

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GraphIndexer {
    static final byte NONE = 0;
    static final byte VISITING = 1;
    static final byte VISITED = 2;
    private int[] indexToNode;
    private int[] nodeToIndex;
    private Graph graph;
    private DominatorTree domTree;
    private int lastIndex;
    private int[] weights;
    private int[] priorities;

    public GraphIndexer(Graph graph, int[] weights, int[] priorities) {
        int sz = graph.size();
        this.weights = weights.clone();
        propagateWeights(graph, this.weights);
        this.priorities = priorities.clone();
        propagatePriorities(graph, this.priorities);
        indexToNode = new int[sz + 1];
        nodeToIndex = new int[sz + 1];
        Arrays.fill(nodeToIndex, -1);
        Arrays.fill(indexToNode, -1);
        this.graph = graph;
        domTree = GraphUtils.buildDominatorTree(graph);
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

    private void propagateWeights(Graph graph, int[] weights) {
        int sz = graph.size();
        byte[] state = new byte[sz];
        IntegerStack stack = new IntegerStack(sz * 2);
        stack.push(0);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            switch (state[node]) {
                case VISITING:
                    state[node] = VISITED;
                    for (int succ : graph.outgoingEdges(node)) {
                        if (state[node] == VISITED) {
                            weights[node] += weights[succ];
                        }
                    }
                    break;
                case NONE:
                    state[node] = VISITING;
                    stack.push(node);
                    for (int succ : graph.outgoingEdges(node)) {
                        if (state[succ] == NONE) {
                            stack.push(succ);
                        }
                    }
                    break;
            }
        }
    }

    private void propagatePriorities(Graph graph, int[] priorities) {
        boolean allZero = true;
        for (int i = 0; i < priorities.length; ++i) {
            if (priorities[i] != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            return;
        }

        DominatorTree domTree = GraphUtils.buildDominatorTree(graph);
        Graph domGraph = GraphUtils.buildDominatorGraph(domTree, graph.size());
        IntegerStack stack = new IntegerStack(graph.size() * 2);
        for (int i = 0; i < domGraph.size(); ++i) {
            if (domGraph.outgoingEdgesCount(i) == 0) {
                stack.push(i);
            }
        }
        while (!stack.isEmpty()) {
            int node = stack.pop();
            int parent = domTree.immediateDominatorOf(node);
            if (parent < 0) {
                continue;
            }
            if (priorities[parent] < priorities[node]) {
                priorities[parent] = priorities[node];
                stack.push(parent);
            }
        }
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
                    IntegerArray terminalNodes = new IntegerArray(1);
                    for (int pred : graph.incomingEdges(node)) {
                        if (domTree.dominates(node, pred)) {
                            terminalNodes.add(pred);
                        }
                    }
                    int[] successors = graph.outgoingEdges(node);
                    List<WeightedNode> succList = new ArrayList<>(successors.length);
                    IntegerArray orderedSuccessors = new IntegerArray(successors.length);
                    if (terminalNodes.size() > 0) {
                        IntSet loopNodes = IntOpenHashSet.from(findNaturalLoop(node, terminalNodes.getAll()));
                        for (int succ : successors) {
                            if (loopNodes.contains(succ)) {
                                succList.add(new WeightedNode(succ, priorities[succ], weights[succ]));
                            }
                        }
                        Collections.sort(succList);
                        for (WeightedNode wnode : succList) {
                            orderedSuccessors.add(wnode.index);
                        }

                        IntSet outerSuccessors = new IntOpenHashSet(successors.length);
                        succList.clear();
                        for (IntCursor loopNode : loopNodes) {
                            for (int succ : graph.outgoingEdges(loopNode.value)) {
                                if (!loopNodes.contains(succ)) {
                                    if (outerSuccessors.add(succ)) {
                                        succList.add(new WeightedNode(succ, priorities[succ], weights[succ]));
                                    }
                                }
                            }
                        }
                        Collections.sort(succList);
                        for (WeightedNode wnode : succList) {
                            orderedSuccessors.add(wnode.index);
                        }
                    } else {
                        for (int succ : successors) {
                            succList.add(new WeightedNode(succ, priorities[succ], weights[succ]));
                        }
                        Collections.sort(succList);
                        for (WeightedNode wnode : succList) {
                            orderedSuccessors.add(wnode.index);
                        }
                    }
                    successors = orderedSuccessors.getAll();
                    for (int succ : successors) {
                        if (state[succ] == NONE) {
                            stack.push(succ);
                        }
                    }
                    break;
                }
            }
        }
    }

    private int[] findNaturalLoop(int head, int[] terminals) {
        IntSet loop = new IntOpenHashSet();
        loop.add(head);
        IntegerStack stack = new IntegerStack(1);
        for (int pred : terminals) {
            stack.push(pred);
        }
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!loop.add(node)) {
                continue;
            }
            for (int pred : graph.incomingEdges(node)) {
                stack.push(pred);
            }
        }
        return loop.toArray();
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

    static class WeightedNode implements Comparable<WeightedNode> {
        int index;
        int priority;
        int weight;

        public WeightedNode(int index, int priority, int weight) {
            this.index = index;
            this.priority = priority;
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedNode o) {
            int r = Integer.compare(priority, o.priority);
            if (r != 0) {
                return r;
            }
            return Integer.compare(weight, o.weight);
        }
    }
}
