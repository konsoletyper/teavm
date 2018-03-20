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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntPredicate;
import org.junit.Test;

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

        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph);
        sortSccs(sccs);

        assertThat(sccs.length, is(2));
        assertThat(sccs[0], is(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }));
        assertThat(sccs[1], is(new int[] { 11, 12 }));
    }

    @Test
    public void stronglyConnectedComponentCalculated2() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(0, 3);
        builder.addEdge(1, 2);
        builder.addEdge(2, 1);
        builder.addEdge(3, 2);
        builder.addEdge(2, 4);
        builder.addEdge(4, 5);
        builder.addEdge(4, 1);
        builder.addEdge(5, 3);
        Graph graph = builder.build();

        graph = GraphUtils.subgraph(graph, node -> node != 0);
        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph);
        sortSccs(sccs);

        assertThat(sccs.length, is(1));
        assertThat(sccs[0], is(new int[] { 1, 2, 3, 4, 5 }));
    }

    @Test
    public void stronglyConnectedComponentCalculated3() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(1, 3);
        builder.addEdge(3, 1);
        builder.addEdge(2, 3);
        Graph graph = builder.build();

        graph = GraphUtils.subgraph(graph, filter);
        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph);
        sortSccs(sccs);

        assertThat(sccs.length, is(1));
        assertThat(sccs[0], is(new int[] { 1, 3 }));
    }

    @Test
    public void stronglyConnectedComponentCalculated4() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(1, 2);
        builder.addEdge(1, 3);
        builder.addEdge(1, 4);
        builder.addEdge(2, 1);
        builder.addEdge(2, 3);
        builder.addEdge(3, 4);
        builder.addEdge(4, 5);
        builder.addEdge(4, 6);
        builder.addEdge(5, 6);
        builder.addEdge(6, 5);
        builder.addEdge(6, 7);
        builder.addEdge(7, 4);
        builder.addEdge(7, 3);
        builder.addEdge(7, 8);
        builder.addEdge(8, 7);
        Graph graph = builder.build();

        graph = GraphUtils.subgraph(graph, node -> node != 0);
        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph);
        sortSccs(sccs);

        assertThat(sccs.length, is(2));
        assertThat(sccs[0], is(new int[] { 1, 2 }));
        assertThat(sccs[1], is(new int[] { 3, 4, 5, 6, 7, 8 }));
    }

    @Test
    public void irreducibleGraphSplit() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(0, 3);
        builder.addEdge(1, 2);
        builder.addEdge(2, 1);
        builder.addEdge(3, 2);
        builder.addEdge(2, 4);
        builder.addEdge(4, 5);
        builder.addEdge(4, 1);
        builder.addEdge(5, 3);

        Graph graph = builder.build();
        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = { 1, 4, 1, 10, 1, 1 };
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equialent", isEquialent(backend, graph));
    }

    @Test
    public void irreducibleGraphSplit2() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(1, 2);
        builder.addEdge(2, 1);
        Graph graph = builder.build();

        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = new int[graph.size()];
        Arrays.fill(weights, 1);
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equialent", isEquialent(backend, graph));
    }

    @Test
    public void irreducibleGraphSplit3() {
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(1, 2);
        builder.addEdge(1, 3);
        builder.addEdge(1, 4);
        builder.addEdge(2, 1);
        builder.addEdge(2, 3);
        builder.addEdge(3, 4);
        builder.addEdge(4, 5);
        builder.addEdge(4, 6);
        builder.addEdge(5, 6);
        builder.addEdge(6, 5);
        builder.addEdge(6, 7);
        builder.addEdge(7, 4);
        builder.addEdge(7, 3);
        builder.addEdge(7, 8);
        builder.addEdge(8, 7);
        Graph graph = builder.build();
        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = new int[graph.size()];
        Arrays.fill(weights, 1);
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equivalent", isEquialent(backend, graph));
    }

    private boolean isEquialent(DefaultGraphSplittingBackend backend, Graph proto) {
        Graph graph = backend.getGraph();
        for (int node = 0; node < graph.size(); ++node) {
            int nodeProto = backend.prototype(node);
            IntSet succProto = new IntHashSet();
            for (int succ : graph.outgoingEdges(node)) {
                succProto.add(backend.prototype(succ));
            }
            if (succProto.size() != proto.outgoingEdgesCount(nodeProto)) {
                return false;
            }
            for (int succ : proto.outgoingEdges(nodeProto)) {
                if (!succProto.contains(succ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private IntPredicate filter = (int node) -> true;

    private void sortSccs(int[][] sccs) {
        for (int i = 0; i < sccs.length; ++i) {
            Arrays.sort(sccs[i]);
        }
        Arrays.sort(sccs, Comparator.comparingInt(o -> o[0]));
    }
}