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
import java.util.List;

public class MutableDirectedGraph implements Graph {
    private List<IntSet> successors = new ArrayList<>();
    private List<IntSet> predecessors = new ArrayList<>();

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
    }

    public Graph copyToImmutable() {
        GraphBuilder builder = new GraphBuilder(successors.size());
        for (int i = 0; i < successors.size(); ++i) {
            for (IntCursor cursor : successors.get(i)) {
                builder.addEdge(i, cursor.value);
            }
        }
        return builder.build();
    }

    @Override
    public int size() {
        return successors.size();
    }

    public void addEdge(int from, int to) {
        int max = Math.max(from, to);
        while (max >= successors.size()) {
            successors.add(new IntOpenHashSet(1));
            predecessors.add(new IntOpenHashSet(1));
        }
        successors.get(from).add(to);
        predecessors.get(to).add(from);
    }

    public void deleteEdge(int from, int to) {
        if (from >= successors.size() || to >= successors.size()) {
            return;
        }
        successors.get(from).removeAllOccurrences(to);
        predecessors.get(to).removeAllOccurrences(from);
    }

    public void detachNode(int node) {
        for (IntCursor succ : successors.get(node)) {
            predecessors.get(succ.value).removeAllOccurrences(node);
        }
        for (IntCursor pred : predecessors.get(node)) {
            successors.get(pred.value).removeAllOccurrences(node);
        }
        predecessors.get(node).clear();
        successors.get(node).clear();
    }

    @Override
    public int[] incomingEdges(int node) {
        return predecessors.get(node).toArray();
    }

    @Override
    public int copyIncomingEdges(int node, int[] target) {
        int index = 0;
        for (IntCursor cursor : predecessors.get(node)) {
            target[index++] = cursor.value;
        }
        return index;
    }

    @Override
    public int[] outgoingEdges(int node) {
        return successors.get(node).toArray();
    }

    @Override
    public int copyOutgoingEdges(int node, int[] target) {
        int index = 0;
        for (IntCursor cursor : successors.get(node)) {
            target[index++] = cursor.value;
        }
        return index;
    }

    @Override
    public int incomingEdgesCount(int node) {
        return predecessors.get(node).size();
    }

    @Override
    public int outgoingEdgesCount(int node) {
        return successors.get(node).size();
    }
}
