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
package org.teavm.callgraph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class DefaultCallGraph implements CallGraph, Serializable {
    private transient Map<MethodReference, DefaultCallGraphNode> nodes = new HashMap<>();
    private List<Map.Entry<MethodReference, DefaultCallGraphNode>> nodeList;
    private transient Map<FieldReference, Set<DefaultFieldAccessSite>> fieldAccessSites = new HashMap<>();
    private List<Map.Entry<FieldReference, DefaultFieldAccessSite>> fieldAccessSiteList;
    private transient Map<String, Set<DefaultClassAccessSite>> classAccessSites = new HashMap<>();
    private List<Map.Entry<String, DefaultClassAccessSite>> classAccessSiteList;

    @Override
    public DefaultCallGraphNode getNode(MethodReference method) {
        ensureDeserialized();
        return nodes.computeIfAbsent(method, k -> new DefaultCallGraphNode(this, method));
    }

    @Override
    public Collection<DefaultFieldAccessSite> getFieldAccess(FieldReference reference) {
        ensureDeserialized();
        Set<DefaultFieldAccessSite> resultSet = fieldAccessSites.get(reference);
        return resultSet != null ? Collections.unmodifiableSet(resultSet) : Collections.emptySet();
    }

    void addFieldAccess(DefaultFieldAccessSite accessSite) {
        ensureDeserialized();
        fieldAccessSites.computeIfAbsent(accessSite.getField(), k -> new HashSet<>()).add(accessSite);
    }

    @Override
    public Collection<DefaultClassAccessSite> getClassAccess(String className) {
        ensureDeserialized();
        Set<DefaultClassAccessSite> resultSet = classAccessSites.get(className);
        return resultSet != null ? Collections.unmodifiableSet(resultSet) : Collections.emptySet();
    }

    void addClassAccess(DefaultClassAccessSite accessSite) {
        ensureDeserialized();
        classAccessSites.computeIfAbsent(accessSite.getClassName(), k -> new HashSet<>()).add(accessSite);
    }

    private void ensureDeserialized() {
        if (nodes != null) {
            return;
        }

        nodes = new HashMap<>();
        for (Map.Entry<MethodReference, DefaultCallGraphNode> entry : nodeList) {
            nodes.put(entry.getKey(), entry.getValue());
        }
        nodeList = null;

        fieldAccessSites = new HashMap<>();
        for (Map.Entry<FieldReference, DefaultFieldAccessSite> entry : fieldAccessSiteList) {
            fieldAccessSites.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(entry.getValue());
        }
        fieldAccessSiteList = null;

        classAccessSites = new HashMap<>();
        for (Map.Entry<String, DefaultClassAccessSite> entry : classAccessSiteList) {
            classAccessSites.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(entry.getValue());
        }
        classAccessSiteList = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ensureDeserialized();
        nodeList = new ArrayList<>(nodes.entrySet());

        fieldAccessSiteList = new ArrayList<>();
        for (Map.Entry<FieldReference, Set<DefaultFieldAccessSite>> entry : fieldAccessSites.entrySet()) {
            for (DefaultFieldAccessSite site : entry.getValue()) {
                fieldAccessSiteList.add(new AbstractMap.SimpleEntry<>(entry.getKey(), site));
            }
        }

        classAccessSiteList = new ArrayList<>();
        for (Map.Entry<String, Set<DefaultClassAccessSite>> entry : classAccessSites.entrySet()) {
            for (DefaultClassAccessSite site : entry.getValue()) {
                classAccessSiteList.add(new AbstractMap.SimpleEntry<>(entry.getKey(), site));
            }
        }

        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
