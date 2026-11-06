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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.Arrays;

public class GraphBuilder {
    private static final int[] EMPTY = new int[0];
    private GraphImpl builtGraph;
    private int[] firstEdges = EMPTY;
    private IntSet[] otherEdges = new IntSet[0];
    private int sz;

    public GraphBuilder() {
    }

    public GraphBuilder(int sz) {
        ensureCapacity(sz);
        this.sz = sz;
    }

    public void clear() {
        Arrays.fill(firstEdges, -1);
        Arrays.fill(otherEdges, null);
        sz = 0;
        builtGraph = null;
    }

    private void ensureCapacity(int capacity) {
        if (capacity <= firstEdges.length) {
            return;
        }
        int oldCapacity = firstEdges.length;
        int newCapacity = Math.max(capacity, oldCapacity * 3 / 2 + 1);
        firstEdges = Arrays.copyOf(firstEdges, newCapacity);
        Arrays.fill(firstEdges, oldCapacity, newCapacity, -1);
        otherEdges = Arrays.copyOf(otherEdges, newCapacity);
    }

    public void addEdge(int from, int to) {
        if (to < 0 || from < 0) {
            throw new IllegalArgumentException();
        }
        sz = Math.max(sz, Math.max(from, to) + 1);
        builtGraph = null;
        ensureCapacity(from + 1);
        int first = firstEdges[from];
        if (first < 0) {
            firstEdges[from] = to;
        } else if (first != to) {
            IntSet set = otherEdges[from];
            if (set == null) {
                otherEdges[from] = IntHashSet.from(to);
            } else {
                set.add(to);
            }
        }
    }

    public void removeEdge(int from, int to) {
        if (to < 0 || from < 0) {
            throw new IllegalArgumentException();
        }
        if (from >= firstEdges.length) {
            return;
        }
        IntSet set = otherEdges[from];
        if (firstEdges[from] == to) {
            if (set == null || set.isEmpty()) {
                firstEdges[from] = -1;
            } else {
                int replacement = set.iterator().next().value;
                set.removeAll(replacement);
                firstEdges[from] = replacement;
            }
            builtGraph = null;
        } else if (set != null && set.removeAll(to) > 0) {
            builtGraph = null;
        }
    }

    public Graph build() {
        if (builtGraph == null) {
            int[][] outgoingEdgeList = new int[sz][];
            int[] incomingCount = new int[sz];
            int nodesWithEdges = Math.min(sz, firstEdges.length);
            for (int i = 0; i < nodesWithEdges; ++i) {
                int first = firstEdges[i];
                if (first < 0) {
                    outgoingEdgeList[i] = EMPTY;
                    continue;
                }
                IntSet set = otherEdges[i];
                int[] edges;
                if (set == null || set.isEmpty()) {
                    edges = new int[] { first };
                } else {
                    edges = new int[set.size() + 1];
                    edges[0] = first;
                    int index = 1;
                    for (IntCursor cursor : set) {
                        edges[index++] = cursor.value;
                    }
                    Arrays.sort(edges);
                }
                outgoingEdgeList[i] = edges;
                for (int j : edges) {
                    incomingCount[j]++;
                }
            }
            for (int i = nodesWithEdges; i < sz; ++i) {
                outgoingEdgeList[i] = EMPTY;
            }
            int[][] incomingEdgeList = new int[sz][];
            for (int i = 0; i < sz; ++i) {
                int count = incomingCount[i];
                incomingEdgeList[i] = count == 0 ? EMPTY : new int[count];
            }
            for (int i = sz - 1; i >= 0; --i) {
                for (int j : outgoingEdgeList[i]) {
                    incomingEdgeList[j][--incomingCount[j]] = i;
                }
            }
            builtGraph = new GraphImpl(incomingEdgeList, outgoingEdgeList);
        }
        return builtGraph;
    }

    private static final class GraphImpl implements Graph {
        private final int[][] incomingEdgeList;
        private final int[][] outgoingEdgeList;

        GraphImpl(int[][] incomingEdgeList, int[][] outgoingEdgeList) {
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

        @Override
        public String toString() {
            return GraphUtils.printToDot(this);
        }
    }
}
