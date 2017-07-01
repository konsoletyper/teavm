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

public class LCATree {
    private int[] depths;
    private int[][] pathsToRoot;
    private int sz;

    public LCATree(int capacity) {
        depths = new int[capacity];
        pathsToRoot = new int[capacity][];
        sz = 1;
        depths[0] = 0;
        pathsToRoot[0] = new int[0];
    }

    public int size() {
        return sz;
    }

    public int addNode(int parent) {
        int node = sz;
        ++sz;
        int depth = depths[parent] + 1;
        depths[node] = depth;
        int h = 0;
        while (depth > 0) {
            depth /= 2;
            h++;
        }
        int[] path = new int[h];
        pathsToRoot[node] = path;
        path[0] = parent;
        for (int i = 1; i < h; ++i) {
            parent = pathsToRoot[parent][i - 1];
            path[i] = parent;
        }
        return node;
    }

    public int parentOf(int node) {
        int[] path = pathsToRoot[node];
        return path.length > 0 ? path[0] : -1;
    }

    public int depthOf(int node) {
        return depths[node];
    }

    public int lcaOf(int a, int b) {
        if (a == b) {
            return a;
        }
        if (depths[a] < depths[b]) {
            int t = a;
            a = b;
            b = t;
        }
        if (depths[a] != depths[b]) {
            int h = depths[a] - depths[b];
            int diff = 1;
            int diffln = 0;
            while (diff <= h) {
                ++diffln;
                diff *= 2;
            }
            --diffln;
            diff /= 2;
            while (h > 0) {
                h -= diff;
                a = pathsToRoot[a][diffln];
                while (diff > h) {
                    diff /= 2;
                    --diffln;
                }
            }
        }
        if (a == b) {
            return a;
        }
        while (true) {
            int i = 0;
            int len = Math.min(pathsToRoot[a].length, pathsToRoot[b].length);
            while (i < len) {
                int p = pathsToRoot[a][i];
                int q = pathsToRoot[b][i];
                if (p == q) {
                    break;
                }
                ++i;
            }
            if (--i < 0) {
                return pathsToRoot[a][0];
            }
            a = pathsToRoot[a][i];
            b = pathsToRoot[b][i];
            if (a == b) {
                return a;
            }
        }
    }
}
