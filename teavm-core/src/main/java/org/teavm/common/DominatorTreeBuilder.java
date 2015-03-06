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

import java.util.Arrays;

/**
 * Build dominator tree using Lengauer & Tarjan algorithm.
 * @author Alexey Andreev
 */
class DominatorTreeBuilder {
    private Graph graph;
    private int[] semidominators;
    int[] vertices;
    private int[] parents;
    private int[] ancestors;
    private int[] labels;
    int[] dominators;
    private IntegerArray[] bucket;
    private int[] path;
    private int effectiveSize;

    public DominatorTreeBuilder(Graph graph) {
        this.graph = graph;
        semidominators = new int[graph.size()];
        vertices = new int[graph.size()];
        parents = new int[graph.size()];
        dominators = new int[graph.size()];
        ancestors = new int[graph.size()];
        labels = new int[graph.size()];
        path = new int[graph.size()];
        bucket = new IntegerArray[graph.size()];
    }

    public void build() {
        for (int i = 0; i < labels.length; ++i) {
            labels[i] = i;
        }
        Arrays.fill(ancestors, -1);
        dfs();
        for (int i = effectiveSize - 1; i >= 0; --i) {
            int w = vertices[i];
            if (parents[w] < 0) {
                continue;
            }
            for (int v : graph.incomingEdges(w)) {
                int u = eval(v);
                if (semidominators[u] >= 0) {
                    semidominators[w] = Math.min(semidominators[w], semidominators[u]);
                }
            }
            addToBucket(vertices[semidominators[w]], w);
            link(parents[w], w);
            for (int v : getBucket(w)) {
                int u = eval(v);
                dominators[v] = semidominators[u] < semidominators[v] ? u : parents[w];
            }
            bucket[w] = null;
        }
        for (int i = 0; i < graph.size(); ++i) {
            int w = vertices[i];
            if (w < 0 || parents[w] < 0) {
                continue;
            }
            if (dominators[w] != vertices[semidominators[w]]) {
                dominators[w] = dominators[dominators[w]];
            }
        }
    }

    private void addToBucket(int v, int w) {
        IntegerArray ws = bucket[v];
        if (ws == null) {
            ws = new IntegerArray(1);
            bucket[v] = ws;
        }
        ws.add(w);
    }

    private int[] getBucket(int v) {
        IntegerArray ws = bucket[v];
        return ws != null ? ws.getAll() : new int[0];
    }

    private void link(int v, int w) {
        ancestors[w] = v;
    }

    private int eval(int v) {
        int ancestor = ancestors[v];
        if (ancestor == -1) {
            return v;
        }
        int i = 0;
        while (ancestor >= 0) {
            path[i++] = v;
            v = ancestor;
            ancestor = ancestors[v];
        }
        ancestor = v;
        while (--i >= 0) {
            v = path[i];
            if (semidominators[labels[v]] > semidominators[labels[ancestor]]) {
                labels[v] = labels[ancestor];
            }
            ancestors[v] = ancestor;
            ancestor = v;
        }
        return labels[v];
    }

    private void dfs() {
        Arrays.fill(semidominators, -1);
        Arrays.fill(vertices, -1);
        IntegerStack stack = new IntegerStack(graph.size());
        for (int i = graph.size() - 1; i >= 0; --i) {
            if (graph.incomingEdgesCount(i) == 0) {
                stack.push(i);
                parents[i] = -1;
            }
        }
        int i = 0;
        while (!stack.isEmpty()) {
            int v = stack.pop();
            if (semidominators[v] >= 0) {
                continue;
            }
            // We don't need vertex index after its dominator has computed.
            semidominators[v] = i;
            vertices[i++] = v;
            for (int w : graph.outgoingEdges(v)) {
                if (semidominators[w] < 0) {
                    parents[w] = v;
                    stack.push(w);
                }
            }
        }
        effectiveSize = i;
    }
}
