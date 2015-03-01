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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class GraphTest {
    @Test
    public void stronglyConnectedComponentsCalculated() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(1, 2);
        builder.addEdge(2, 3);
        builder.addEdge(2, 4);
        builder.addEdge(3, 5);
        builder.addEdge(4, 5);
        builder.addEdge(5, 6);
        builder.addEdge(6, 1);
        builder.addEdge(6, 7);
        builder.addEdge(7, 8);
        builder.addEdge(7, 9);
        builder.addEdge(8, 1);
        builder.addEdge(9, 10);
        builder.addEdge(10, 11);
        builder.addEdge(11, 12);
        builder.addEdge(12, 11);
        builder.addEdge(12, 13);
        Graph graph = builder.build();

        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph, new int[] { 0 }, new GraphNodeFilter() {
            @Override public boolean match(int node) {
                return true;
            }
        });

        for (int i = 0; i < sccs.length; ++i) {
            Arrays.sort(sccs[i]);
        }
        Arrays.sort(sccs, new Comparator<int[]>() {
            @Override public int compare(int[] o1, int[] o2) {
                return Integer.compare(o1[0], o2[0]);
            }
        });

        assertThat(sccs[0], is(new int[] { 0 }));
        assertThat(sccs[1], is(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }));
        assertThat(sccs[2], is(new int[] { 9 }));
        assertThat(sccs[3], is(new int[] { 10 }));
        assertThat(sccs[4], is(new int[] { 11, 12 }));
        assertThat(sccs[5], is(new int[] { 13 }));
    }
}