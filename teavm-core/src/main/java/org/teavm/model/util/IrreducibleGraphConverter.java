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
package org.teavm.model.util;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.*;

/**
 * <p>Converts irreducible graph to reducible one using node splitting algorithm described at
 * the paper &ldquo;Handling irreducible loops: optimized node splitting vs. DJ-graphs&rdquo; by
 * Sebastian Unger and Frank Mueller.</p>
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class IrreducibleGraphConverter {
    private MutableDirectedGraph graph;
    private DisjointSet nodeClasses = new DisjointSet();
    private List<IntegerArray> classContents = new ArrayList<>();
    private DJGraph djGraph;
    private GraphSplittingBackend backend;

    public void convertToReducible(Graph cfg, GraphSplittingBackend backend) {
        this.backend = backend;
        buildMutableCFG(cfg);
        rebuildDJGraph();
        splitLoops(0, allNodesOf(cfg));
        this.backend = null;
    }

    private boolean splitLoops(int top, IntSet nodesToHandle) {
        boolean hasCrossEdge = false;
        for (int child : djGraph.getGraph().outgoingEdges(top)) {
            if (!djGraph.isDomEdge(top, child)) {
                continue;
            }
            hasCrossEdge |= nodesToHandle.contains(child) && splitLoops(child, nodesToHandle);
        }
        if (hasCrossEdge) {
            handleIrreducibleLoopChildren(top, nodesToHandle);
        }
        for (int pred : graph.incomingEdges(top)) {
            if (djGraph.isSpanningBack(pred, top) && djGraph.isCrossJoin(top, pred)) {
                return true;
            }
        }
        return false;
    }

    private void handleIrreducibleLoopChildren(int top, IntSet nodesToHandle) {
        List<int[]> sccs = findStronglyConnectedComponents(top, nodesToHandle, djGraph.levelOf(top));
        for (int[] scc : sccs) {
            if (scc.length > 1) {
                handleStronglyConnectedComponent(top, scc);
            }
        }
    }

    private void handleStronglyConnectedComponent(int top, int[] nodes) {

    }

    /*
     * Tarjan's algorithm
     */
    private List<int[]> findStronglyConnectedComponents(int start, IntSet nodesToHandle, int topLevel) {
        List<int[]> components = new ArrayList<>();
        boolean[] done = new boolean[djGraph.getGraph().size()];
        int[] visitIndex = new int[djGraph.getGraph().size()];
        Arrays.fill(visitIndex, -1);
        int[] headerIndex = new int[djGraph.getGraph().size()];
        int lastIndex = 0;
        IntegerStack stack = new IntegerStack(nodesToHandle.size());
        stack.push(-1);
        stack.push(start);

        IntegerArray currentComponent = new IntegerArray(1);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (visitIndex[node] == 0) {
                if (done[node]) {
                    currentComponent.add(node);
                    int hdr = node;
                    for (int successor : djGraph.getGraph().outgoingEdges(node)) {
                        if (!nodesToHandle.contains(successor) || djGraph.levelOf(node) < topLevel) {
                            continue;
                        }
                        if (!done[successor]) {
                            hdr = Math.min(hdr, visitIndex[successor]);
                        } else {
                            hdr = Math.min(hdr, headerIndex[successor]);
                        }
                    }
                    if (hdr == node) {
                        components.add(currentComponent.getAll());
                        currentComponent.clear();
                    }
                    headerIndex[node] = hdr;
                } else {
                    done[node] = true;
                }
            } else {
                visitIndex[node] = ++lastIndex;
                stack.push(node);
                for (int successor : djGraph.getGraph().outgoingEdges(node)) {
                    if (!nodesToHandle.contains(successor) || djGraph.levelOf(node) >= topLevel) {
                        continue;
                    }
                    stack.push(node);
                }
            }
        }
        return components;
    }

    private void buildMutableCFG(Graph cfg) {
        graph = new MutableDirectedGraph(cfg);
        for (int i = 0; i < cfg.size(); ++i) {
            nodeClasses.create();
            classContents.add(IntegerArray.of(i));
        }
    }

    private IntSet allNodesOf(Graph cfg) {
        int[] allNodes = new int[cfg.size()];
        for (int i = 0; i < cfg.size(); ++i) {
            allNodes[i] = i;
        }
        return IntOpenHashSet.from(allNodes);
    }

    private void rebuildDJGraph() {
        djGraph = new DJGraph(graph);
    }
}
