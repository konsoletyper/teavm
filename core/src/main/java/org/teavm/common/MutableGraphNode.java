/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MutableGraphNode {
    private int tag;
    final Map<MutableGraphNode, MutableGraphEdge> edges = new LinkedHashMap<>();

    public MutableGraphNode(int tag) {
        this.tag = tag;
    }

    private MutableGraphEdge connect(MutableGraphNode other) {
        MutableGraphEdge edge = edges.get(other);
        if (edge == null) {
            edge = new MutableGraphEdge();
            edge.first = this;
            edge.second = other;
            edges.put(other, edge);
            MutableGraphEdge back = new MutableGraphEdge();
            back.first = other;
            back.second = this;
            back.back = edge;
            edge.back = back;
            other.edges.put(this, back);
        }
        return edge;
    }

    public void connectAll(Collection<MutableGraphNode> nodes) {
        for (MutableGraphNode node : nodes) {
            connect(node);
        }
    }

    public Collection<MutableGraphEdge> getEdges() {
        return edges.values();
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(":");
        Iterator<MutableGraphEdge> edges = this.edges.values().iterator();
        if (edges.hasNext()) {
            sb.append(edges.next().getSecond().getTag());
        }
        while (edges.hasNext()) {
            sb.append(',').append(edges.next().getSecond().getTag());
        }
        return sb.toString();
    }

    public static Graph toGraph(List<MutableGraphNode> nodes) {
        for (int i = 0; i < nodes.size(); ++i) {
            nodes.get(i).setTag(i);
        }

        GraphBuilder builder = new GraphBuilder(nodes.size());
        for (int i = 0; i < nodes.size(); ++i) {
            for (MutableGraphEdge edge : nodes.get(i).getEdges()) {
                int successor = edge.getSecond().getTag();
                builder.addEdge(i, successor);
            }
        }

        return builder.build();
    }
}
