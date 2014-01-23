/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.BitSet;
import org.teavm.common.Graph;

/**
 *
 * @author Alexey Andreev
 */
public class GraphColorer {
    public int[] colorize(Graph graph, int preservedColors) {
        int[] colors = new int[graph.size()];
        boolean[] visited = new boolean[graph.size()];
        for (int i = 0; i < preservedColors; ++i) {
            colors[i] = i;
            visited[i] = true;
        }
        BitSet usedColors = new BitSet();
        for (int v : getOrdering(graph)) {
            if (v < preservedColors) {
                continue;
            }
            usedColors.clear();
            usedColors.set(0);
            for (int succ : graph.outgoingEdges(v)) {
                if (visited[succ]) {
                    usedColors.set(colors[succ]);
                }
            }
            colors[v] = usedColors.nextClearBit(0);
            visited[v] = true;
        }
        return colors;
    }

    private int[] getOrdering(Graph graph) {
        boolean[] visited = new boolean[graph.size()];
        int[] ordering = new int[graph.size()];
        int index = 0;
        int[] queue = new int[graph.size() * 2];
        for (int root = 0; root < graph.size(); ++root) {
            if (visited[root]) {
                continue;
            }
            int head = 0;
            int tail = 0;
            queue[head++] = root;
            while (head != tail) {
                int v = queue[tail++];
                if (tail == queue.length) {
                    tail = 0;
                }
                if (visited[v]) {
                    continue;
                }
                visited[v] = true;
                ordering[index++] = v;
                for (int succ : graph.outgoingEdges(v)) {
                    if (visited[succ]) {
                        continue;
                    }
                    if (++head == queue.length) {
                        head = 0;
                    }
                    queue[head] = succ;
                }
            }
        }
        return ordering;
    }
}
