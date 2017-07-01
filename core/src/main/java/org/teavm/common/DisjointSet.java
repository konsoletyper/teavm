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

public class DisjointSet {
    private int[] parent = new int[16];
    private int[] rank = new int[16];
    private int[] setSize = new int[16];
    private int maxRank;
    private int size;

    public int find(int node) {
        int initial = node;
        int depth = 0;
        while (parent[node] != node) {
            node = parent[node];
            ++depth;
        }
        if (depth > 1) {
            parent[initial] = node;
        }
        return node;
    }

    public int[] pack(int count) {
        int[] packed = new int[count];
        int[] packedIndexes = new int[size];
        Arrays.fill(packedIndexes, -1);
        int lastIndex = 0;
        for (int i = 0; i < count; ++i) {
            int cls = find(i);
            int packedIndex = packedIndexes[cls];
            if (packedIndex < 0) {
                packedIndex = lastIndex++;
                packedIndexes[cls] = packedIndex;
            }
            packed[i] = packedIndex;
        }
        return packed;
    }

    private void ensureCapacity(int size) {
        if (parent.length >= size) {
            return;
        }
        int newCap = size * 3 / 2 + 1;
        parent = Arrays.copyOf(parent, newCap);
        rank = Arrays.copyOf(rank, newCap);
        setSize = Arrays.copyOf(setSize, newCap);
    }

    public int create() {
        int node = size;
        ++size;
        ensureCapacity(size);
        parent[node] = node;
        rank[node] = 1;
        setSize[node] = 1;
        return node;
    }

    public int union(int a, int b) {
        a = find(a);
        b = find(b);
        if (a == b) {
            return a;
        }
        if (rank[a] > rank[b]) {
            parent[b] = a;
            setSize[a] += setSize[b];
            return a;
        } else if (rank[b] < rank[a]) {
            parent[a] = b;
            setSize[b] += setSize[a];
            return b;
        } else {
            parent[b] = a;
            setSize[a] += setSize[b];
            rank[a]++;
            maxRank = Math.max(maxRank, rank[a]);
            return a;
        }
    }

    public int sizeOf(int node) {
        return setSize[node];
    }

    public int size() {
        return size;
    }
}
