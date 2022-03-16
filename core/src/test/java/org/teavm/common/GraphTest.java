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
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
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
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
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
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
    }

    @Test
    public void irreducibleGraphSplit4() {
        GraphBuilder builder = new GraphBuilder();

        builder.addEdge(0, 1);
        builder.addEdge(0, 2);
        builder.addEdge(0, 4);
        builder.addEdge(1, 2);
        builder.addEdge(2, 3);
        builder.addEdge(3, 4);
        builder.addEdge(4, 1);

        Graph graph = builder.build();
        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = new int[graph.size()];
        Arrays.fill(weights, 1);
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
    }

    @Test
    public void irreducibleGraphSplit5() {
        GraphBuilder builder = new GraphBuilder();

        addEdges(builder, 0, 1);
        addEdges(builder, 1, 2, 3);
        addEdges(builder, 2, 5);
        addEdges(builder, 3, 2, 4);
        addEdges(builder, 4, 5);
        addEdges(builder, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22);
        addEdges(builder, 6, 69, 70);
        addEdges(builder, 7, 69);
        addEdges(builder, 8, 32);
        addEdges(builder, 9, 63);
        addEdges(builder, 10, 61);
        addEdges(builder, 11, 59);
        addEdges(builder, 12, 57);
        addEdges(builder, 13, 55);
        addEdges(builder, 14, 53);
        addEdges(builder, 15, 42);
        addEdges(builder, 16, 44);
        addEdges(builder, 17, 46);
        addEdges(builder, 18, 48);
        addEdges(builder, 19, 50);
        addEdges(builder, 20, 65);
        addEdges(builder, 21, 67);
        addEdges(builder, 23, 24, 25);
        addEdges(builder, 24, 67, 68);
        addEdges(builder, 25, 26, 27);
        addEdges(builder, 26, 28, 29);
        addEdges(builder, 28, 30, 31);
        addEdges(builder, 30, 65, 66);
        addEdges(builder, 31, 32, 33);
        addEdges(builder, 32, 34, 35, 36, 37, 38, 39, 40, 41);
        addEdges(builder, 34, 63, 64);
        addEdges(builder, 35, 61, 62);
        addEdges(builder, 36, 59, 60);
        addEdges(builder, 37, 57, 58);
        addEdges(builder, 38, 55, 56);
        addEdges(builder, 39, 53, 54);
        addEdges(builder, 40, 42, 43);
        addEdges(builder, 42, 44, 45);
        addEdges(builder, 44, 46, 47);
        addEdges(builder, 46, 48, 49);
        addEdges(builder, 48, 50, 51);
        addEdges(builder, 50, 52);
        addEdges(builder, 52, 23);
        addEdges(builder, 53, 52);
        addEdges(builder, 55, 52);
        addEdges(builder, 57, 52);
        addEdges(builder, 59, 52);
        addEdges(builder, 61, 52);
        addEdges(builder, 63, 52);
        addEdges(builder, 65, 52);
        addEdges(builder, 69, 23);

        Graph graph = builder.build();
        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = new int[graph.size()];
        Arrays.fill(weights, 1);
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
    }

    @Test
    public void irreducibleGraphSplit6() {
        GraphBuilder builder = new GraphBuilder();
        addEdges(builder, 0, 1, 3, 6, 9);
        addEdges(builder, 1, 2);
        addEdges(builder, 2, 3);
        addEdges(builder, 3, 4);
        addEdges(builder, 4, 5, 8);
        addEdges(builder, 5, 6);
        addEdges(builder, 6, 7);
        addEdges(builder, 7, 11);
        addEdges(builder, 8, 9);
        addEdges(builder, 9, 10);
        addEdges(builder, 10, 11);
        addEdges(builder, 11, 12);
        addEdges(builder, 12, 1);

        Graph graph = builder.build();
        DefaultGraphSplittingBackend backend = new DefaultGraphSplittingBackend(graph);
        int[] weights = new int[graph.size()];
        Arrays.fill(weights, 1);
        GraphUtils.splitIrreducibleGraph(graph, weights, backend);
        Graph result = backend.getGraph();

        assertTrue("Should be irreducible", GraphUtils.isIrreducible(graph));
        assertFalse("Should be reducible", GraphUtils.isIrreducible(result));
        assertTrue("Should be equivalent", isEquivalent(backend, graph));
    }
    
    private static void addEdges(GraphBuilder builder, int from, int... to) {
        for (int target : to) {
            builder.addEdge(from, target);
        }
    }

    private boolean isEquivalent(DefaultGraphSplittingBackend backend, Graph proto) {
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
        for (int[] scc : sccs) {
            Arrays.sort(scc);
        }
        Arrays.sort(sccs, Comparator.comparingInt(o -> o[0]));
    }
}