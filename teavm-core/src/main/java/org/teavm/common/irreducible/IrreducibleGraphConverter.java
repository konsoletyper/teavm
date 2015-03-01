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

import java.util.Arrays;
import org.teavm.common.*;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;

/**
 * <p>Converts irreducible graph to reducible one using node splitting algorithm described at
 * the paper &ldquo;Handling irreducible loops: optimized node splitting vs. DJ-graphs&rdquo; by
 * Sebastian Unger and Frank Mueller.</p>
 *
 * @author Alexey Andreev
 */
public class IrreducibleGraphConverter {
    private Graph cfg;
    private int[] cfgWeight;
    private GraphSplittingBackend backend;

    public void convertToReducible(Graph cfg, int[] weight, GraphSplittingBackend backend) {
        this.backend = backend;
        int[] identityNodeMap = new int[cfg.size()];
        for (int i = 0; i < identityNodeMap.length; ++i) {
            identityNodeMap[i] = i;
        }
        this.cfg = cfg;
        this.cfgWeight = weight;
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
                        djGraph.collapse(reachUnder(djGraph, pred, node));
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

    private int[] reachUnder(DJGraph djGraph, int back, int header) {
        IntSet naturalLoop = IntOpenHashSet.from(header);
        IntegerStack stack = new IntegerStack(djGraph.getGraph().size());
        stack.push(back);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!naturalLoop.add(node)) {
                continue;
            }
            for (int pred : djGraph.getGraph().incomingEdges(node)) {
                stack.push(pred);
            }
        }
        return naturalLoop.toArray();
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
        splitStronglyConnectedComponent(domainNodes, sharedDom, scc, nodeMap);

        // Collapse
        djGraph.collapse(scc);
    }

    private void splitStronglyConnectedComponent(IntSet domain, int sharedDom, int[] scc, int[] nodeMap) {
        // Find SCC \ domain
        int[] mappedNonDomain = new int[scc.length - domain.size()];
        int index = 0;
        for (int node : scc) {
            if (!domain.contains(node)) {
                mappedNonDomain[index++] = nodeMap[node];
            }
        }
        int[] mappedDomain = new int[domain.size()];
        index = 0;
        for (IntCursor cursor : domain) {
            mappedDomain[index++] = cursor.value;
        }

        // Delegate splitting to domain
        int[] newNodes = backend.split(mappedDomain, mappedNonDomain);

        // Calculate mappings
        int[] newNodeMap = new int[1 + scc.length + newNodes.length];
        int[] newNodeBackMap = new int[cfg.size()];
        Arrays.fill(newNodeBackMap, -1);
        newNodeMap[0] = nodeMap[sharedDom];
        newNodeBackMap[sharedDom] = 0;
        index = 1;
        for (int i = 0; i < mappedDomain.length; ++i) {
            newNodeMap[index] = mappedDomain[i];
            newNodeBackMap[mappedDomain[i]] = index;
            ++index;
        }
        for (int i = 0; i < mappedNonDomain.length; ++i) {
            newNodeMap[index] = mappedNonDomain[i];
            newNodeBackMap[mappedNonDomain[i]] = index;
            ++index;
        }

        // Build subgraph with new nodes
        GraphBuilder builder = new GraphBuilder(newNodeMap.length);
        int[] mappedWeight = new int[newNodeMap.length];
        mappedWeight[0] = cfgWeight[newNodeMap[0]];
        for (int succ : cfg.outgoingEdges(sharedDom)) {
            int j = newNodeBackMap[succ];
            if (j >= 0) {
                builder.addEdge(0, j);
            }
        }
        for (int i = 1; i <= mappedDomain.length; ++i) {
            mappedWeight[i] = cfgWeight[newNodeMap[i]];
            for (int succ : cfg.outgoingEdges(mappedDomain[i])) {
                int j = newNodeBackMap[succ];
                if (j > mappedDomain.length) {
                    builder.addEdge(i, j);
                } else if (j >= 0) {
                    builder.addEdge(i, j + mappedNonDomain.length);
                }
            }
        }
        for (int i = mappedDomain.length + 1; i <= scc.length; ++i) {
            mappedWeight[i] = cfgWeight[newNodeMap[i]];
            mappedWeight[i + mappedNonDomain.length] = cfgWeight[newNodeMap[i]];
            for (int succ : cfg.outgoingEdges(mappedNonDomain[i])) {
                int j = newNodeBackMap[succ];
                if (j >= 0) {
                    builder.addEdge(i, j);
                    if (j > mappedDomain.length) {
                        builder.addEdge(i + mappedNonDomain.length, j);
                    } else {
                        builder.addEdge(i + mappedNonDomain.length, j + mappedNonDomain.length);
                    }
                }
            }
        }

        handleLoops(new DJGraph(builder.build(), mappedWeight), newNodeMap);
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
