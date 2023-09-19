/*
 *  Copyright 2021 Alexey Andreev.
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

import com.carrotsearch.hppc.IntArrayList;
import java.util.Arrays;

/**
 * <p>Converts irreducible graph to reducible one using node splitting algorithm described at
 * the paper &ldquo;Handling irreducible loops: optimized node splitting vs. DJ-graphs&rdquo; by
 * Sebastian Unger and Frank Mueller.</p>
 *
 * <p>Appendix A of the paper contains pseudocode. We refer to this pseudocode below.</p>
 *
 * @author Alexey Andreev
 */
class IrreducibleGraphSplitter {
    private GraphSplittingBackend backend;
    private DominatorTree dom;
    private int[][] domNodes;
    private MutableDirectedGraph cfg;
    private int[] weights;
    private IntArrayList[] realNodes;
    private int[][] spBackEdges;
    private int[] tmpArray;
    private int[] tmpArray2;
    private IntArrayList copiedRealNodes = new IntArrayList();
    private int additionalWeight;
    private int[] collapseMap;

    IrreducibleGraphSplitter(GraphSplittingBackend backend, Graph src, int[] weights) {
        this(backend, src, weights, initRealNodes(src.size()));
    }

    private static int[][] initRealNodes(int size) {
        int[][] result = new int[size][];
        for (int i = 0; i < size; ++i) {
            result[i] = new int[] { i };
        }
        return result;
    }

    private IrreducibleGraphSplitter(GraphSplittingBackend backend, Graph src, int[] weights, int[][] realNodes) {
        int size = src.size();
        if (size != weights.length || size != realNodes.length) {
            throw new IllegalArgumentException("Node count " + src.size() + " is not equal to weight array "
                    + weights.length);
        }
        this.backend = backend;
        tmpArray = new int[src.size()];
        tmpArray2 = new int[src.size()];
        cfg = new MutableDirectedGraph(src);
        dom = GraphUtils.buildDominatorTree(src);
        collapseMap = new int[size];
        for (int i = 0; i < size; ++i) {
            collapseMap[i] = i;
        }
        buildDomGraph();
        dfs();
        this.realNodes = new IntArrayList[realNodes.length];
        for (int i = 0; i < cfg.size(); ++i) {
            this.realNodes[i] = IntArrayList.from(realNodes[i]);
        }
        this.weights = weights.clone();
    }

    // n-th element of output array (domGraph) will contain nodes, directly dominated by node n.
    private void buildDomGraph() {
        int size = cfg.size();
        var domGraphCount = new int[size];
        for (int i = 0; i < size; ++i) {
            int j = dom.immediateDominatorOf(i);
            if (j >= 0) {
                domGraphCount[j]++;
            }
        }
        var domGraph = new int[size][];
        for (int i = 0; i < size; ++i) {
            domGraph[i] = new int[domGraphCount[i]];
            domGraphCount[i] = 0;
        }
        for (int i = 0; i < size; ++i) {
            int j = dom.immediateDominatorOf(i);
            if (j >= 0) {
                domGraph[j][domGraphCount[j]++] = i;
            }
        }
        this.domNodes = domGraph;
    }

    // Find back edges.
    // The n-th element of output array (sbBackEdges) will contain null if there is no back edges leading to n,
    // or array of nodes m_i, where each edge  m_i -> n is a back edge in spanning tree
    // (m_i -> n is called 'SB back edge' in the paper).
    private void dfs() {
        int size = cfg.size();

        spBackEdges = new int[size][];
        int[] spBackEdgeCount = new int[size];
        for (int i = 0; i < size; ++i) {
            int count = cfg.incomingEdgesCount(i);
            if (count > 0) {
                spBackEdges[i] = new int[cfg.incomingEdgesCount(i)];
            }
        }

        int[] state = new int[size];
        int[] stack = new int[size * 2];
        int top = 0;
        stack[top++] = 0;

        while (top > 0) {
            int node = stack[--top];
            switch (state[node]) {
                case 0:
                    state[node] = 1;
                    stack[top++] = node;
                    for (int successor : cfg.outgoingEdges(node)) {
                        if (state[successor] == 0) {
                            stack[top++] = successor;
                        } else if (state[successor] == 1) {
                            spBackEdges[successor][spBackEdgeCount[successor]++] = node;
                        }
                    }
                    break;
                case 1:
                    state[node] = 2;
                    break;
            }
        }

        for (int i = 0; i < size; ++i) {
            int[] back = spBackEdges[i];
            if (back == null) {
                continue;
            }
            int count = spBackEdgeCount[i];
            if (count == 0) {
                spBackEdges[i] = null;
            } else if (count < spBackEdges[i].length) {
                spBackEdges[i] = Arrays.copyOf(back, count);
            }
        }
    }

    // This is an implementation of 'split_loop' function from the paper.
    // It does not take 'top' and 'set' parameter.
    // Instead, it always starts with 0 node as top and assumes that all the 'set' nodes are in the graph
    // We rewrote this method to use stack instead of recursion. The only place where we need recursion
    // is handleScc. We build a new instance of this class with corresponding subgraph.
    void splitLoops() {
        int size = cfg.size();
        var cross = new boolean[size];
        var stack = new int[size * 4];
        int head = 0;

        stack[head++] = 0;
        stack[head++] = 0;
        while (head > 0) {
            int state = stack[--head];
            int node = stack[--head];
            if (state == 0) {
                stack[head++] = node;
                stack[head++] = 1;
                int[] successors = domNodes[node];
                for (int i = successors.length - 1; i >= 0; --i) {
                    stack[head++] = successors[i];
                    stack[head++] = 0;
                }
            } else {
                if (cross[node]) {
                    handleIrreducibleChildren(node);
                }
                int[] back = spBackEdges[node];
                int parent = dom.immediateDominatorOf(node);
                if (back != null && parent >= 0) {
                    for (int predecessor : back) {
                        if (!dom.dominates(node, predecessor)) {
                            cross[parent] = true;
                            break;
                        }
                    }
                }
                if (domNodes[node].length > 1) {
                    collapse(node);
                }
            }
        }
    }

    private void handleIrreducibleChildren(int top) {
        var levelSubgraph = GraphUtils.subgraph(cfg, node -> node != top && dom.dominates(top, node));
        var sccs = GraphUtils.findStronglyConnectedComponents(levelSubgraph);
        for (int[] scc : sccs) {
            if (scc.length > 1) {
                handleStronglyConnectedComponent(top, scc);
            }
        }
    }

    private void handleStronglyConnectedComponent(int top, int[] scc) {
        var domainCount = 0;
        for (var node : scc) {
            if (dom.immediateDominatorOf(node) == top) {
                ++domainCount;
            }
        }
        if (domainCount < 2) {
            return;
        }

        // Find header node
        var domains = fillDomains(top, scc);
        var localWeights = fillWeights(domains, scc);

        int remaining = -1;
        int maxWeight = 0;
        for (var node : scc) {
            if (domains[node] != node) {
                continue;
            }
            if (remaining < 0 || localWeights[node] > maxWeight) {
                maxWeight = localWeights[node];
                remaining = node;
            }
        }

        var realRemainingNodesCount = 0;
        int realNodesToCopyCount = 0;
        int copyWeight = 0;
        int remainingNodeCount = 0;
        for (int node : scc) {
            if (domains[node] != remaining) {
                realNodesToCopyCount += realNodes[node].size();
                copyWeight += weights[node];
            } else {
                remainingNodeCount++;
                realRemainingNodesCount += realNodes[node].size();
            }
        }
        var realNodesToCopy = new int[realNodesToCopyCount];
        var realRemainingNodes = new int[realRemainingNodesCount];
        realNodesToCopyCount = 0;
        realRemainingNodesCount = 0;
        for (int node : scc) {
            var nodes = realNodes[node].toArray();
            if (domains[node] != remaining) {
                System.arraycopy(nodes, 0, realNodesToCopy, realNodesToCopyCount, nodes.length);
                realNodesToCopyCount += nodes.length;
            } else {
                System.arraycopy(nodes, 0, realRemainingNodes, realRemainingNodesCount, nodes.length);
                realRemainingNodesCount += nodes.length;
            }
        }

        var realNodesCopies = backend.split(realRemainingNodes, realNodesToCopy);
        copiedRealNodes.add(realNodesCopies);

        int subgraphSize = scc.length * 2 + 1 - remainingNodeCount;
        var subgraph = new GraphBuilder(subgraphSize);
        var subgraphRealNodes = new int[subgraphSize][];
        var subgraphWeights = new int[subgraphSize];
        var map = new int[cfg.size()];
        var copyMap = new int[cfg.size()];
        Arrays.fill(map, -1);
        Arrays.fill(copyMap, -1);

        subgraphRealNodes[0] = new int[0];
        subgraphWeights[0] = 0;
        for (int i = 0; i < scc.length; ++i) {
            int node = scc[i];
            map[node] = i + 1;
            subgraphRealNodes[i + 1] = realNodes[node].toArray();
            subgraphWeights[i + 1] = weights[node];
            if (domains[node] == node) {
                var hasPredecessorsOutsideLoop = false;
                for (var pred : cfg.incomingEdges(node)) {
                    if (domains[pred] < 0) {
                        hasPredecessorsOutsideLoop = true;
                        break;
                    }
                }
                if (hasPredecessorsOutsideLoop) {
                    subgraph.addEdge(0, i + 1);
                }
            }
        }
        int copyIndex = scc.length + 1;
        int realNodeCopiesIndex = 0;
        for (int node : scc) {
            if (domains[node] == remaining) {
                continue;
            }
            copyMap[node] = copyIndex;
            int realNodeCount = realNodes[node].size();
            subgraphRealNodes[copyIndex] = Arrays.copyOfRange(realNodesCopies, realNodeCopiesIndex,
                    realNodeCopiesIndex + realNodeCount);
            realNodeCopiesIndex += realNodeCount;
            subgraphWeights[copyIndex] = weights[node];
            copyIndex++;
        }

        for (int node : scc) {
            int subgraphNode = map[node];
            int subgraphNodeCopy = copyMap[node];
            var successors = cfg.outgoingEdges(node);
            for (int successor : successors) {
                // (x, y) = (node, successor)
                int subgraphSuccessor = map[successor];
                int subgraphSuccessorCopy = copyMap[successor];

                if (subgraphSuccessorCopy >= 0) {
                    // y in S
                    if (subgraphNodeCopy >= 0) {
                        // x in S
                        subgraph.addEdge(subgraphNodeCopy, subgraphSuccessorCopy); // 8.4
                        if (subgraphSuccessor >= 0) {
                            subgraph.addEdge(subgraphNode, subgraphSuccessor);     // 8.1
                        }
                    } else {
                        // x !in S (x in domain(h))
                        subgraph.addEdge(subgraphNode, subgraphSuccessorCopy);     // 8.2
                    }
                } else if (subgraphSuccessor >= 0) {
                    // y !in S (y in N\S)
                    if (subgraphNodeCopy >= 0) {
                        subgraph.addEdge(subgraphNodeCopy, subgraphSuccessor);     // 8.3
                    }
                    subgraph.addEdge(subgraphNode, subgraphSuccessor);             // 8.1
                }
            }
        }

        var subgraphSplitter = new IrreducibleGraphSplitter(backend, subgraph.build(),
                subgraphWeights, subgraphRealNodes);
        subgraphSplitter.splitLoops();
        realNodes[top].addAll(subgraphSplitter.copiedRealNodes);
        realNodes[top].add(realNodesCopies);
        weights[top] += subgraphSplitter.additionalWeight + copyWeight;
        copiedRealNodes.addAll(subgraphSplitter.copiedRealNodes);
        additionalWeight += subgraphSplitter.additionalWeight + copyWeight;
    }

    private int[] fillDomains(int top, int[] nodes) {
        var domains = tmpArray;
        Arrays.fill(domains, -1);
        for (var node : nodes) {
            var domain = domains[node];
            if (domain < 0) {
                while (true) {
                    var parent = dom.immediateDominatorOf(node);
                    if (parent == top) {
                        domain = node;
                        break;
                    }
                    domain = domains[parent];
                    if (domain >= 0) {
                        break;
                    }
                    domains[parent] = node;
                    node = parent;
                }
                while (true) {
                    var next = domains[node];
                    domains[node] = domain;
                    if (next == -1) {
                        break;
                    }
                    node = next;
                }
            }
        }

        return domains;
    }

    private int[] fillWeights(int[] domains, int[] nodes) {
        var localWeights = tmpArray2;
        for (var node : nodes) {
            if (domains[node] == node) {
                localWeights[node] = 0;
            }
        }
        for (var node : nodes) {
            localWeights[domains[node]] += weights[node];
        }
        return localWeights;
    }

    private void collapse(int top) {
        if (domNodes[top] == null || domNodes[top].length == 0) {
            return;
        }
        int count = findAllDominatedNodes(top);
        var nodes = tmpArray;

        var topRealNodes = realNodes[top];
        for (int i = 1; i < count; ++i) {
            int node = nodes[i];
            topRealNodes.addAll(realNodes[node]);
            realNodes[node] = null;
            weights[top] += weights[node];
            collapseMap[node] = top;
        }

        // Alter graphs
        for (int i = 1; i < count; ++i) {
            int node = nodes[i];
            for (int succ : cfg.outgoingEdges(node)) {
                int mappedSucc = collapseMap[succ];
                if (mappedSucc != top || succ == top) {
                    cfg.addEdge(top, mappedSucc);
                }
            }
            for (int pred : cfg.incomingEdges(node)) {
                int mappedPred = collapseMap[pred];
                if (mappedPred != top) {
                    cfg.addEdge(mappedPred, top);
                }
            }
            cfg.detachNode(node);
        }

        domNodes[top] = null;
    }

    private int findAllDominatedNodes(int top) {
        var result = tmpArray;
        int count = 0;

        int head = 0;
        result[count++] = top;
        while (head < count) {
            var successors = domNodes[result[head]];
            if (successors != null && successors.length > 0) {
                System.arraycopy(successors, 0, result, count, successors.length);
                count += successors.length;
            }
            ++head;
        }

        return count;
    }
}
