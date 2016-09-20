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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.IntegerArray;

public class GraphColorer {
    public void colorize(Graph graph, int[] colors) {
        colorize(graph, colors, new int[graph.size()], new String[graph.size()]);
    }

    public void colorize(Graph graph, int[] colors, int[] categories, String[] names) {
        IntegerArray colorCategories = new IntegerArray(graph.size());
        List<String> colorNames = new ArrayList<>();
        for (int i = 0; i < colors.length; ++i) {
            int color = colors[i];
            if (colors[i] < 0) {
                continue;
            }
            while (colorCategories.size() <= color) {
                colorCategories.add(-1);
            }
            while (colorNames.size() <= color) {
                colorNames.add(null);
            }
            colorCategories.set(color, categories[i]);
            colorNames.set(color, names[i]);
        }
        BitSet usedColors = new BitSet();
        for (int v : getOrdering(graph)) {
            if (colors[v] >= 0) {
                continue;
            }
            usedColors.clear();
            usedColors.set(0);
            for (int succ : graph.outgoingEdges(v)) {
                if (colors[succ] >= 0) {
                    usedColors.set(colors[succ]);
                }
            }
            int color = 0;
            while (true) {
                color = usedColors.nextClearBit(color);
                while (colorCategories.size() <= color) {
                    colorCategories.add(-1);
                    colorNames.add(null);
                }
                int category = colorCategories.get(color);
                String name = colorNames.get(color);
                if ((category < 0 || category == categories[v])
                        && (name == null || names[v] == null || name.equals(names[v]))) {
                    colors[v] = color;
                    colorCategories.set(color, categories[v]);
                    if (names[v] != null) {
                        colorNames.set(color, names[v]);
                    } else {
                        names[v] = name;
                    }
                    break;
                }
                ++color;
            }
        }
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
