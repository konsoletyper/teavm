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
import com.carrotsearch.hppc.IntIntMap;

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
    public int[] split(int[] domain, int[] nodes) {
        int[] copies = new int[nodes.length];
        IntIntMap map = new IntIntHashMap();
        for (int i = 0; i < nodes.length; ++i) {
            copies[i] = index++;
            map.put(nodes[i], copies[i] + 1);
            int proto = prototypeNodes.get(nodes[i]);
            prototypeNodes.add(proto);
            copyIndexes.add(++copyCount[proto]);
        }

        for (int i = 0; i < domain.length; ++i) {
            int node = domain[i];
            for (int succ : graph.outgoingEdges(node)) {
                int succCopy = map.get(succ);
                if (succCopy == 0) {
                    continue;
                }
                --succCopy;
                graph.deleteEdge(node, succ);
                graph.addEdge(node, succCopy);
            }
        }

        for (int i = 0; i < nodes.length; ++i) {
            int node = nodes[i];
            int nodeCopy = copies[i];
            for (int succ : graph.outgoingEdges(node)) {
                int succCopy = map.get(succ);
                if (succCopy != 0) {
                    graph.addEdge(nodeCopy, succCopy - 1);
                } else {
                    graph.addEdge(nodeCopy, succ);
                }
            }
        }

        return copies;
    }
}

