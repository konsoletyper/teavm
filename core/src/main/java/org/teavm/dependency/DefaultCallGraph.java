/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.dependency;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.callgraph.CallGraph;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;

class DefaultCallGraph implements CallGraph, Serializable {
    public Map<MethodReference, DefaultCallGraphNode> nodes = new HashMap<>();
    Map<FieldReference, Set<DefaultFieldAccessSite>> fieldAccessSites = new HashMap<>();

    @Override
    public DefaultCallGraphNode getNode(MethodReference method) {
        return nodes.computeIfAbsent(method, k -> new DefaultCallGraphNode(this, k));
    }

    @Override
    public Collection<DefaultFieldAccessSite> getFieldAccess(FieldReference reference) {
        Set<DefaultFieldAccessSite> resultSet = fieldAccessSites.get(reference);
        return resultSet != null ? Collections.unmodifiableSet(resultSet) : Collections.emptySet();
    }

    void addFieldAccess(DefaultFieldAccessSite accessSite) {
        fieldAccessSites.computeIfAbsent(accessSite.getField(), k -> new LinkedHashSet<>()).add(accessSite);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        SerializableCallGraphBuilder builder = new SerializableCallGraphBuilder();
        out.writeObject(builder.build(this));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        SerializableCallGraph scg = (SerializableCallGraph) in.readObject();
        nodes = new LinkedHashMap<>();
        fieldAccessSites = new LinkedHashMap<>();
        new CallGraphBuilder().build(scg, this);
    }

    static class SerializableCallGraphBuilder {
        List<SerializableCallGraph.Node> nodes = new ArrayList<>();
        ObjectIntMap<DefaultCallGraphNode> nodeToIndex = new ObjectIntHashMap<>();
        List<SerializableCallGraph.CallSite> callSites = new ArrayList<>();
        List<DefaultCallSite> originalCallSites = new ArrayList<>();
        ObjectIntMap<DefaultCallSite> callSiteToIndex = new ObjectIntHashMap<>();
        List<SerializableCallGraph.FieldAccess> fieldAccessList = new ArrayList<>();
        ObjectIntMap<DefaultFieldAccessSite> fieldAccessToIndex = new ObjectIntHashMap<>();
        List<DefaultCallGraphNode> nodesToProcess = new ArrayList<>();
        List<DefaultCallSite> callSitesToProcess = new ArrayList<>();
        List<DefaultFieldAccessSite> fieldAccessToProcess = new ArrayList<>();

        SerializableCallGraph build(DefaultCallGraph cg) {
            SerializableCallGraph scg = new SerializableCallGraph();

            scg.nodeIndexes = cg.nodes.values().stream()
                    .mapToInt(this::getNode)
                    .toArray();
            scg.fieldAccessIndexes = cg.fieldAccessSites.values().stream()
                    .flatMapToInt(accessSites -> accessSites.stream().mapToInt(this::getFieldAccess))
                    .toArray();

            while (step()) {
                // just repeat
            }

            scg.nodes = nodes.toArray(new SerializableCallGraph.Node[0]);
            scg.callSites = callSites.toArray(new SerializableCallGraph.CallSite[0]);
            scg.fieldAccessList = fieldAccessList.toArray(new SerializableCallGraph.FieldAccess[0]);

            return scg;
        }

        boolean step() {
            return processNodes() | processCallSites() | processFieldAccess();
        }

        boolean processNodes() {
            boolean hasAny = false;
            for (DefaultCallGraphNode node : nodesToProcess.toArray(new DefaultCallGraphNode[0])) {
                int index = nodeToIndex.get(node);
                SerializableCallGraph.Node serializableNode = nodes.get(index);
                serializableNode.method = node.getMethod();
                serializableNode.callSites = node.getCallSites().stream()
                        .mapToInt(this::getCallSite)
                        .toArray();
                serializableNode.callerCallSites = node.getCallerCallSites().stream()
                        .mapToInt(this::getCallSite)
                        .toArray();
                serializableNode.fieldAccessSites = node.getFieldAccessSites().stream()
                        .mapToInt(this::getFieldAccess)
                        .toArray();
                hasAny = true;
            }
            nodesToProcess.clear();
            return hasAny;
        }

        boolean processCallSites() {
            boolean hasAny = false;
            for (DefaultCallSite callSite : callSitesToProcess.toArray(new DefaultCallSite[0])) {
                int index = callSiteToIndex.get(callSite);
                SerializableCallGraph.CallSite scs = callSites.get(index);
                scs.virtual = callSite.callers != null;
                List<SerializableCallGraph.Location> locations = new ArrayList<>();
                for (DefaultCallGraphNode caller : callSite.getCallers()) {
                    for (TextLocation textLocation : callSite.getLocations(caller)) {
                        SerializableCallGraph.Location location = new SerializableCallGraph.Location();
                        location.caller = getNode(caller);
                        location.value = textLocation;
                        locations.add(location);
                    }
                }
                scs.locations = locations.toArray(new SerializableCallGraph.Location[0]);
                scs.callers = getNodes(callSite.getCallers());
                scs.calledMethods = getNodes(callSite.getCalledMethods());
                hasAny = true;
            }
            callSitesToProcess.clear();
            return hasAny;
        }

        boolean processFieldAccess() {
            boolean hasAny = false;
            for (DefaultFieldAccessSite accessSite : fieldAccessToProcess.toArray(new DefaultFieldAccessSite[0])) {
                int index = fieldAccessToIndex.get(accessSite);
                SerializableCallGraph.FieldAccess sfa = fieldAccessList.get(index);
                sfa.location = accessSite.getLocation();
                sfa.field = accessSite.getField();
                sfa.callee = getNode(accessSite.getCallee());
                hasAny = true;
            }
            fieldAccessToProcess.clear();
            return hasAny;
        }

        private int getNode(DefaultCallGraphNode node) {
            int index = nodeToIndex.getOrDefault(node, -1);
            if (index < 0) {
                index = nodeToIndex.size();
                nodeToIndex.put(node, index);
                nodes.add(new SerializableCallGraph.Node());
                nodesToProcess.add(node);
            }
            return index;
        }

        private int[] getNodes(Collection<? extends DefaultCallGraphNode> nodes) {
            int[] result = new int[nodes.size()];
            int index = 0;
            for (DefaultCallGraphNode node : nodes) {
                result[index++] = getNode(node);
            }
            return result;
        }

        private int getCallSite(DefaultCallSite callSite) {
            int index = callSiteToIndex.getOrDefault(callSite, -1);
            if (index < 0) {
                index = callSiteToIndex.size();
                callSiteToIndex.put(callSite, index);
                callSites.add(new SerializableCallGraph.CallSite());
                callSitesToProcess.add(callSite);
            }
            return index;
        }

        private int getFieldAccess(DefaultFieldAccessSite fieldAccessSite) {
            int index = fieldAccessToIndex.getOrDefault(fieldAccessSite, -1);
            if (index < 0) {
                index = fieldAccessToIndex.size();
                fieldAccessToIndex.put(fieldAccessSite, index);
                fieldAccessList.add(new SerializableCallGraph.FieldAccess());
                fieldAccessToProcess.add(fieldAccessSite);
            }
            return index;
        }
    }

    static class CallGraphBuilder {
        List<DefaultCallGraphNode> nodes = new ArrayList<>();
        List<DefaultCallSite> callSites = new ArrayList<>();
        List<DefaultFieldAccessSite> fieldAccessList = new ArrayList<>();

        void build(SerializableCallGraph scg, DefaultCallGraph cg) {
            for (SerializableCallGraph.Node serializableNode : scg.nodes) {
                nodes.add(new DefaultCallGraphNode(cg, serializableNode.method));
            }
            for (SerializableCallGraph.CallSite scs : scg.callSites) {
                DefaultCallSite callSite;
                if (scs.virtual) {
                    callSite = new DefaultCallSite(scs.method, mapNodes(scs.callers));
                    callSite.calledMethods.addAll(mapNodes(scs.calledMethods));
                } else {
                    callSite = new DefaultCallSite(nodes.get(scs.calledMethods[0]), nodes.get(scs.callers[0]));
                }
                for (SerializableCallGraph.Location location : scs.locations) {
                    callSite.addLocation(nodes.get(location.caller), location.value);
                }
                callSites.add(callSite);
            }
            for (SerializableCallGraph.FieldAccess sfa : scg.fieldAccessList) {
                fieldAccessList.add(new DefaultFieldAccessSite(sfa.location, nodes.get(sfa.callee), sfa.field));
            }

            for (int index : scg.nodeIndexes) {
                DefaultCallGraphNode node = nodes.get(index);
                cg.nodes.put(node.getMethod(), node);
            }
            for (int index : scg.fieldAccessIndexes) {
                cg.addFieldAccess(fieldAccessList.get(index));
            }
        }

        private Set<DefaultCallGraphNode> mapNodes(int[] nodes) {
            Set<DefaultCallGraphNode> result = new LinkedHashSet<>();
            for (int i = 0; i < nodes.length; ++i) {
                result.add(this.nodes.get(nodes[i]));
            }
            return result;
        }
    }
}
