/*
 *  Copyright 2013 Alexey Andreev.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GraphBuilder {
    private GraphImpl builtGraph;
    private List<IntSet> addedEdges = new ArrayList<>();
    private int sz;

    public GraphBuilder() {
    }

    public GraphBuilder(int sz) {
        addedEdges.addAll(Collections.nCopies(sz, null));
        this.sz = sz;
    }

    public void clear() {
        addedEdges.clear();
        sz = 0;
    }

    public void addEdge(int from, int to) {
        if (to < 0 || from < 0) {
            throw new IllegalArgumentException();
        }
        sz = Math.max(sz, Math.max(from, to) + 1);
        builtGraph = null;
        if (addedEdges.size() == from) {
            addedEdges.add(IntOpenHashSet.from(to));
        } else if (addedEdges.size() <= from) {
            addedEdges.addAll(Collections.nCopies(from - addedEdges.size(), null));
            addedEdges.add(IntOpenHashSet.from(to));
        } else {
            IntSet set = addedEdges.get(from);
            if (set == null) {
                addedEdges.set(from, IntOpenHashSet.from(to));
            } else {
                set.add(to);
            }
        }
    }

    public void removeEdge(int from, int to) {
        if (to < 0 || from < 0) {
            throw new IllegalArgumentException();
        }
        if (from >= addedEdges.size() || to >= addedEdges.size()) {
            return;
        }
        addedEdges.get(from).removeAllOccurrences(to);
    }

    public Graph build() {
        if (builtGraph == null) {
            IntSet[] incomingEdges = new IntSet[sz];
            for (int i = 0; i < sz; ++i) {
                incomingEdges[i] = new IntOpenHashSet();
            }
            int[][] outgoingEdgeList = new int[sz][];
            for (int i = 0; i < addedEdges.size(); ++i) {
                IntSet edgeList = addedEdges.get(i);
                outgoingEdgeList[i] = edgeList != null ? edgeList.toArray() : new int[0];
                Arrays.sort(outgoingEdgeList[i]);
                for (int j : outgoingEdgeList[i]) {
                    incomingEdges[j].add(i);
                }
            }
            for (int i = addedEdges.size(); i < sz; ++i) {
                outgoingEdgeList[i] = new int[0];
            }
            int[][] incomingEdgeList = new int[sz][];
            for (int i = 0; i < sz; ++i) {
                incomingEdgeList[i] = incomingEdges[i].toArray();
                Arrays.sort(incomingEdgeList[i]);
            }
            builtGraph = new GraphImpl(incomingEdgeList, outgoingEdgeList);
        }
        return builtGraph;
    }

    private static class GraphImpl implements Graph {
        private final int[][] incomingEdgeList;
        private final int[][] outgoingEdgeList;

        public GraphImpl(int[][] incomingEdgeList, int[][] outgoingEdgeList) {
            this.incomingEdgeList = incomingEdgeList;
            this.outgoingEdgeList = outgoingEdgeList;
        }

        @Override
        public int size() {
            return incomingEdgeList.length;
        }

        @Override
        public int[] incomingEdges(int node) {
            int[] result = incomingEdgeList[node];
            return Arrays.copyOf(result, result.length);
        }

        @Override
        public int copyIncomingEdges(int node, int[] target) {
            int[] result = incomingEdgeList[node];
            System.arraycopy(result, 0, target, 0, result.length);
            return result.length;
        }

        @Override
        public int[] outgoingEdges(int node) {
            int[] result = outgoingEdgeList[node];
            return Arrays.copyOf(result, result.length);
        }

        @Override
        public int copyOutgoingEdges(int node, int[] target) {
            int[] result = outgoingEdgeList[node];
            System.arraycopy(result, 0, target, 0, result.length);
            return result.length;
        }

        @Override
        public int incomingEdgesCount(int node) {
            return incomingEdgeList[node].length;
        }

        @Override
        public int outgoingEdgesCount(int node) {
            return outgoingEdgeList[node].length;
        }
    }
}
