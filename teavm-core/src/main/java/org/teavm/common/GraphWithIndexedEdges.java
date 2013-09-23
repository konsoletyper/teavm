package org.teavm.common;

import java.util.Arrays;

/**
 *
 * @author Alexey Andreev
 */
public class GraphWithIndexedEdges implements Graph {
    private int[][] incoming;
    private int[][] outgoing;
    private int[][] edgeIndex;

    public GraphWithIndexedEdges(Graph graph) {
        incoming = new int[graph.size()][];
        outgoing = new int[graph.size()][];
        edgeIndex = new int[graph.size()][];
        for (int i = 0; i < graph.size(); ++i) {
            outgoing[i] = graph.outgoingEdges(i);
            incoming[i] = new int[graph.incomingEdgesCount(i)];
            edgeIndex[i] = new int[graph.outgoingEdgesCount(i)];
        }
        int[] lastIncoming = new int[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            int[] localOutgoing = outgoing[i];
            int[] localEdgeIndex = edgeIndex[i];
            for (int j = 0; j < localOutgoing.length; ++j) {
                int target = localOutgoing[j];
                int index = lastIncoming[target]++;
                incoming[target][index] = i;
                localEdgeIndex[j] = index;
            }
        }
    }

    @Override
    public int size() {
        return incoming.length;
    }

    @Override
    public int[] incomingEdges(int node) {
        int[] edges = incoming[node];
        return Arrays.copyOf(edges, edges.length);
    }

    @Override
    public int copyIncomingEdges(int node, int[] target) {
        int[] edges = incoming[node];
        System.arraycopy(edges, 0, target, 0, edges.length);
        return edges.length;
    }

    @Override
    public int[] outgoingEdges(int node) {
        int[] edges = outgoing[node];
        return Arrays.copyOf(edges, edges.length);
    }

    @Override
    public int copyOutgoingEdges(int node, int[] target) {
        int[] edges = outgoing[node];
        System.arraycopy(edges, 0, target, 0, edges.length);
        return edges.length;
    }

    @Override
    public int incomingEdgesCount(int node) {
        return incoming[node].length;
    }

    @Override
    public int outgoingEdgesCount(int node) {
        return outgoing[node].length;
    }

    /**
     * For given edge, identified by source node and index in source node edge array,
     * returns index in target node edge array. Works in constant time.
     */
    public int getEdgeIndex(int sourceNode, int sourceEdgeIndex) {
        return edgeIndex[sourceNode][sourceEdgeIndex];
    }
}
