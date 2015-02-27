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

import org.teavm.common.*;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;

/**
 * <p>Converts irreducible graph to reducible one using node splitting algorithm described at
 * the paper &ldquo;Handling irreducible loops: optimized node splitting vs. DJ-graphs&rdquo; by
 * Sebastian Unger and Frank Mueller.</p>
 *
 * @author Alexey Andreev
 */
public class IrreducibleGraphConverter {
    private Graph cfg;
    private GraphSplittingBackend backend;

    public void convertToReducible(Graph cfg, int[] weight, GraphSplittingBackend backend) {
        this.backend = backend;
        int[] identityNodeMap = new int[cfg.size()];
        for (int i = 0; i < identityNodeMap.length; ++i) {
            identityNodeMap[i] = i;
        }
        this.cfg = cfg;
        handleLoops(new DJGraph(cfg, weight), identityNodeMap);
        this.backend = null;
    }

    private void handleLoops(DJGraph djGraph, int[] nodeMap) {
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
            for (int[] scc : sccs) {
                handleStronglyConnectedComponent(djGraph, scc, nodeMap);
                djGraph.collapse(scc);
            }
        }
    }

    private int[] reachUnder(DJGraph djGraph, int top) {
        // TODO: implement
        return null;
    }

    private void handleStronglyConnectedComponent(DJGraph djGraph, int[] scc, int[] nodeMap) {
        // Find shared dominator
        int sharedDom = scc[0];
        for (int i = 1; i < scc.length; ++i) {
            sharedDom = djGraph.getDomTree().commonDominatorOf(sharedDom, scc[i]);
        }

        // Partition SCC into domains
        DisjointSet partitions = new DisjointSet();
        for (int i = 0; i < scc.length; ++i) {
            partitions.create();
        }
        for (int i = 0; i < scc.length; ++i) {
            int node = scc[i];
            int idom = djGraph.getDomTree().immediateDominatorOf(node);
            if (idom != sharedDom) {
                partitions.union(node, idom);
            }
        }
        int[] domains = partitions.pack(scc.length);
        int domainCount = 0;
        for (int domain : domains) {
            domainCount = Math.max(domainCount, domain + 1);
        }

        // For each domain calculate its weight
        int[] domainWeight = new int [domainCount];
        for (int i = 0; i < scc.length; ++i) {
            int node = scc[i];
            domainWeight[domains[node]] += djGraph.weightOf(node);
        }

        // Find domain to split around
        int domain = 0;
        int maxWeight = domainWeight[0];
        for (int i = 1; i < domainWeight.length; ++i) {
            if (domainWeight[i] > maxWeight) {
                domain = i;
                maxWeight = domainWeight[i];
            }
        }

        // Find header of this domain
        IntSet domainNodes = new IntOpenHashSet(scc.length);
        for (int i = 0; i < scc.length; ++i) {
            int node = scc[i];
            if (domains[node] == domain) {
                domainNodes.add(node);
            }
        }

        // Split
        int[] newNodes = splitStronglyConnectedComponent(domainNodes, scc, nodeMap);

        // Construct DJ-subgraph
        int[] newNodeMap = new int[1 + scc.length + newNodes.length];
        for (int i = 0; i < scc.length; ++i) {
            newNodeMap[i + 1] = nodeMap[scc[i]];
        }
        for (int i = 0; i < newNodes.length; ++i) {
            newNodeMap[i + 1 + scc.length] = newNodes[i];
        }
        GraphBuilder builder = new GraphBuilder(newNodeMap.length);

        for (int i = 0; i < scc.length; ++i) {

        }
    }

    private int[] splitStronglyConnectedComponent(IntSet domain, int[] scc, int[] nodeMap) {
        IntegerArray nonDomain = new IntegerArray(scc.length);
        for (int node : scc) {
            if (!domain.contains(node)) {
                nonDomain.add(node);
            }
        }
        int[] mappedNonDomain = new int[nonDomain.size()];
        for (int i = 0; i < nonDomain.size(); ++i) {
            mappedNonDomain[i] = nodeMap[nonDomain.get(i)];
        }
        return backend.split(mappedNonDomain);
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
