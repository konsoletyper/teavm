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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.List;

public class MutableDirectedGraph implements Graph {
    private List<NodeSet> successors = new ArrayList<>();
    private List<NodeSet> predecessors = new ArrayList<>();

    public MutableDirectedGraph() {
    }

    public MutableDirectedGraph(Graph graph) {
        int[] data = new int[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            int sz = graph.copyOutgoingEdges(i, data);
            for (int j = 0; j < sz; ++j) {
                addEdge(i, data[j]);
            }
        }
        while (successors.size() < graph.size()) {
            successors.add(new NodeSet());
            predecessors.add(new NodeSet());
        }
    }

    public Graph copyToImmutable() {
        GraphBuilder builder = new GraphBuilder(successors.size());
        for (int i = 0; i < successors.size(); ++i) {
            for (IntCursor successor : successors.get(i).list) {
                builder.addEdge(i, successor.value);
            }
        }
        return builder.build();
    }

    public int addNode() {
        int index = successors.size();
        successors.add(new NodeSet());
        predecessors.add(new NodeSet());
        return index;
    }

    @Override
    public int size() {
        return successors.size();
    }

    public void addEdge(int from, int to) {
        int max = Math.max(from, to);
        while (max >= successors.size()) {
            successors.add(new NodeSet());
            predecessors.add(new NodeSet());
        }

        NodeSet successorNodes = successors.get(from);
        if (successorNodes.set.add(to)) {
            successorNodes.list.add(to);
            NodeSet predecessorNodes = predecessors.get(to);
            predecessorNodes.set.add(from);
            predecessorNodes.list.add(from);
        }
    }

    public void deleteEdge(int from, int to) {
        if (from >= successors.size() || to >= successors.size()) {
            return;
        }

        NodeSet successorNodes = successors.get(from);
        if (successorNodes.set.removeAll(to) > 0) {
            successorNodes.list.removeAll(to);
            NodeSet predecessorNodes = predecessors.get(to);
            predecessorNodes.set.removeAll(from);
            predecessorNodes.list.removeAll(from);
        }
    }

    public void detachNode(int node) {
        for (IntCursor succ : successors.get(node).list) {
            NodeSet predecessorNodes = predecessors.get(succ.value);
            predecessorNodes.set.removeAll(node);
            predecessorNodes.list.removeAll(node);
        }
        for (IntCursor pred : predecessors.get(node).list) {
            NodeSet successorNodes = successors.get(pred.value);
            successorNodes.set.removeAll(node);
            successorNodes.list.removeAll(node);
        }

        NodeSet predecessorNodes = predecessors.get(node);
        predecessorNodes.list.clear();
        predecessorNodes.set.clear();
        NodeSet successorNodes = successors.get(node);
        successorNodes.list.clear();
        predecessorNodes.list.clear();
    }

    @Override
    public int[] incomingEdges(int node) {
        return predecessors.get(node).list.toArray();
    }

    @Override
    public int copyIncomingEdges(int node, int[] target) {
        int index = 0;
        for (IntCursor cursor : predecessors.get(node).list) {
            target[index++] = cursor.value;
        }
        return index;
    }

    @Override
    public int[] outgoingEdges(int node) {
        return successors.get(node).list.toArray();
    }

    @Override
    public int copyOutgoingEdges(int node, int[] target) {
        int index = 0;
        for (IntCursor cursor : successors.get(node).list) {
            target[index++] = cursor.value;
        }
        return index;
    }

    @Override
    public int incomingEdgesCount(int node) {
        return predecessors.get(node).list.size();
    }

    @Override
    public int outgoingEdgesCount(int node) {
        return successors.get(node).list.size();
    }

    @Override
    public String toString() {
        return GraphUtils.printToDot(this);
    }

    static class NodeSet {
        IntHashSet set = new IntHashSet(1);
        IntArrayList list = new IntArrayList(1);
    }
}
