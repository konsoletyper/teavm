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

import java.util.*;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DefaultCallGraph implements CallGraph {
    private Map<MethodReference, DefaultCallGraphNode> nodes = new HashMap<>();
    private Map<FieldReference, Set<DefaultFieldAccessSite>> fieldAccessSites = new HashMap<>();
    private Map<String, Set<DefaultClassAccessSite>> classAccessSites = new HashMap<>();

    @Override
    public DefaultCallGraphNode getNode(MethodReference method) {
        DefaultCallGraphNode node = nodes.get(method);
        if (node == null) {
            node = new DefaultCallGraphNode(this, method);
            nodes.put(method, node);
        }
        return nodes.get(method);
    }

    @Override
    public Collection<DefaultFieldAccessSite> getFieldAccess(FieldReference reference) {
        Set<DefaultFieldAccessSite> resultSet = fieldAccessSites.get(reference);
        return resultSet != null ? Collections.unmodifiableSet(resultSet)
                : Collections.<DefaultFieldAccessSite>emptySet();
    }

    void addFieldAccess(DefaultFieldAccessSite accessSite) {
        Set<DefaultFieldAccessSite> sites = fieldAccessSites.get(accessSite.getField());
        if (sites == null) {
            sites = new HashSet<>();
            fieldAccessSites.put(accessSite.getField(), sites);
        }
        sites.add(accessSite);
    }

    @Override
    public Collection<DefaultClassAccessSite> getClassAccess(String className) {
        Set<DefaultClassAccessSite> resultSet = classAccessSites.get(className);
        return resultSet != null ? Collections.unmodifiableSet(resultSet)
                : Collections.<DefaultClassAccessSite>emptySet();
    }

    void addClassAccess(DefaultClassAccessSite accessSite) {
        Set<DefaultClassAccessSite> sites = classAccessSites.get(accessSite.getClassName());
        if (sites == null) {
            sites = new HashSet<>();
            classAccessSites.put(accessSite.getClassName(), sites);
        }
        sites.add(accessSite);
    }
}
