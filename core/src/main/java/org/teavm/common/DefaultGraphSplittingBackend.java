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

import com.carrotsearch.hppc.IntIntHashMap;

public class DefaultGraphSplittingBackend implements GraphSplittingBackend {
    private MutableDirectedGraph graph;
    private int index;
    private IntegerArray prototypeNodes;
    private IntegerArray copyIndexes;
    private int[] copyCount;

    public DefaultGraphSplittingBackend(Graph graph) {
        this.graph = new MutableDirectedGraph(graph);
        prototypeNodes = new IntegerArray(graph.size());
        copyIndexes = new IntegerArray(graph.size());
        copyCount = new int[graph.size()];
        index = graph.size();
        for (int i = 0; i < graph.size(); ++i) {
            prototypeNodes.add(i);
            copyIndexes.add(0);
        }
    }

    public Graph getGraph() {
        return graph.copyToImmutable();
    }

    public int prototype(int index) {
        return prototypeNodes.get(index);
    }

    @Override
    public int[] split(int[] remaining, int[] toCopy) {
        var copies = new int[toCopy.length];
        var map = new IntIntHashMap();
        for (int i = 0; i < toCopy.length; ++i) {
            copies[i] = index++;
            map.put(toCopy[i], copies[i]);
            int proto = prototypeNodes.get(toCopy[i]);
            prototypeNodes.add(proto);
            copyIndexes.add(++copyCount[proto]);
        }

        for (int i = 0; i < remaining.length; ++i) {
            int node = remaining[i];
            for (int succ : graph.outgoingEdges(node)) {
                int succCopy = map.getOrDefault(succ, -1);
                if (succCopy < 0) {
                    continue;
                }
                graph.deleteEdge(node, succ);
                graph.addEdge(node, succCopy);
            }
        }

        for (int i = 0; i < toCopy.length; ++i) {
            int node = toCopy[i];
            int nodeCopy = copies[i];
            for (int succ : graph.outgoingEdges(node)) {
                int succCopy = map.getOrDefault(succ, -1);
                if (succCopy >= 0) {
                    graph.addEdge(nodeCopy, succCopy);
                } else {
                    graph.addEdge(nodeCopy, succ);
                }
            }
        }

        return copies;
    }
}

