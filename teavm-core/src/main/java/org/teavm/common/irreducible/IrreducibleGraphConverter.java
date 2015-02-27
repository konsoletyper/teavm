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
package org.teavm.common.irreducible;

import com.carrotsearch.hppc.IntSet;
import org.teavm.common.Graph;
import org.teavm.common.GraphNodeFilter;
import org.teavm.common.GraphUtils;

/**
 * <p>Converts irreducible graph to reducible one using node splitting algorithm described at
 * the paper &ldquo;Handling irreducible loops: optimized node splitting vs. DJ-graphs&rdquo; by
 * Sebastian Unger and Frank Mueller.</p>
 *
 * @author Alexey Andreev
 */
public class IrreducibleGraphConverter {
    private GraphSplittingBackend backend;

    public void convertToReducible(Graph cfg, GraphSplittingBackend backend) {
        this.backend = backend;
        handleLoops(new DJGraph(cfg));
        this.backend = null;
    }

    private void handleLoops(DJGraph djGraph) {
        for (int level = djGraph.levelCount() - 1; level >= 0; --level) {
            boolean irreducible = false;
            for (int node : djGraph.level(level)) {
                for (int pred : djGraph.getGraph().incomingEdges(node)) {
                    if (djGraph.isCrossJoin(pred, node)) {
                        if (!irreducible && djGraph.isSpanningBack(node, pred)) {
                            irreducible = true;
                        }
                    } else if (djGraph.isBackJoin(node, pred)) {
                        djGraph.collapse(reachUnder(djGraph, pred));
                    }
                }
            }
            DJGraphNodeFilter filter = new DJGraphNodeFilter(djGraph, level, null);
            int[][] sccs = GraphUtils.findStronglyConnectedComponents(djGraph.getGraph(), djGraph.level(level), filter);
        }
    }

    private int[] reachUnder(DJGraph djGraph, int top) {
        // TODO: implement
        return null;
    }

    static class DJGraphNodeFilter implements GraphNodeFilter {
        private DJGraph graph;
        private int level;
        private IntSet nodes;

        public DJGraphNodeFilter(DJGraph graph, int level, IntSet nodes) {
            this.graph = graph;
            this.level = level;
            this.nodes = nodes;
        }

        @Override
        public boolean match(int node) {
            return nodes.contains(node) && graph.levelOf(node) >= level;
        }
    }
}
