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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;

class DefaultCallGraphNode implements CallGraphNode {
    private DefaultCallGraph graph;
    private MethodReference method;
    private Map<MethodReference, DefaultCallSite> callSiteMap;
    private List<DefaultCallSite> callSites;
    private DefaultCallSite singleCallSite;
    private Collection<DefaultCallSite> safeCallSites;
    private DefaultCallSite singleCaller;
    private List<DefaultCallSite> callerCallSites;
    private List<DefaultCallSite> safeCallersCallSites;
    private Set<DefaultFieldAccessSite> fieldAccessSites = new LinkedHashSet<>();
    private Set<DefaultFieldAccessSite> safeFieldAccessSites;
    private DefaultCallSite virtualCallSite;

    DefaultCallGraphNode(DefaultCallGraph graph, MethodReference method) {
        this.graph = graph;
        this.method = method;
    }

    @Override
    public DefaultCallGraph getGraph() {
        return graph;
    }

    @Override
    public MethodReference getMethod() {
        return method;
    }

    @Override
    public Collection<DefaultCallSite> getCallSites() {
        if (callSites == null) {
            if (singleCallSite != null) {
                return Collections.singletonList(singleCallSite);
            }
            return Collections.emptyList();
        }
        if (safeCallSites == null) {
            safeCallSites = Collections.unmodifiableCollection(callSites);
        }
        return safeCallSites;
    }

    @Override
    public Collection<DefaultCallSite> getCallerCallSites() {
        if (callerCallSites == null) {
            if (singleCaller != null) {
                return Collections.singletonList(singleCaller);
            }
            return Collections.emptyList();
        }
        if (safeCallersCallSites == null) {
            safeCallersCallSites = Collections.unmodifiableList(callerCallSites);
        }
        return safeCallersCallSites;
    }

    DefaultCallSite addCallSite(MethodReference method) {
        DefaultCallGraphNode callee = graph.getNode(method);

        if (callSites == null) {
            if (singleCallSite == null) {
                singleCallSite = new DefaultCallSite(callee, this);
                callee.addCaller(singleCallSite);
                return singleCallSite;
            }
            if (singleCallSite.singleCalledMethod.getMethod().equals(method)) {
                return singleCallSite;
            }
            callSiteMap = new LinkedHashMap<>();
            callSites = new ArrayList<>();
            callSiteMap.put(singleCallSite.singleCalledMethod.getMethod(), singleCallSite);
            callSites.add(singleCallSite);
            singleCallSite = null;
        }

        DefaultCallSite callSite = callSiteMap.get(method);
        if (callSite == null) {
            callSite = new DefaultCallSite(callee, this);
            callee.addCaller(callSite);
            callSiteMap.put(method, callSite);
            callSites.add(callSite);
        }

        return callSite;
    }

    DefaultCallSite getVirtualCallSite() {
        if (virtualCallSite == null) {
            virtualCallSite = new DefaultCallSite(method, new LinkedHashSet<>());
        }
        return virtualCallSite;
    }

    void addVirtualCallSite(DefaultCallSite callSite) {
        if (callSite.callers == null) {
            throw new IllegalArgumentException("Call site is not virtual");
        }
        if (callSite.callers.add(this)) {
            if (callSites == null) {
                callSites = new ArrayList<>();
                callSiteMap = new LinkedHashMap<>();
                if (singleCallSite != null) {
                    callSites.add(singleCallSite);
                    callSiteMap.put(singleCallSite.method, singleCallSite);
                    singleCallSite = null;
                }
            }
            callSites.add(callSite);
        }
    }

    void addCaller(DefaultCallSite caller) {
        if (callerCallSites == null) {
            if (singleCaller == null) {
                singleCaller = caller;
                return;
            }
            callerCallSites = new ArrayList<>();
            callerCallSites.add(singleCaller);
            singleCaller = null;
        }
        callerCallSites.add(caller);
    }

    @Override
    public Collection<DefaultFieldAccessSite> getFieldAccessSites() {
        if (safeFieldAccessSites == null) {
            safeFieldAccessSites = Collections.unmodifiableSet(fieldAccessSites);
        }
        return safeFieldAccessSites;
    }

    boolean addFieldAccess(FieldReference field, TextLocation location) {
        DefaultFieldAccessSite site = new DefaultFieldAccessSite(location, this, field);
        if (fieldAccessSites.add(site)) {
            graph.addFieldAccess(site);
            return true;
        } else {
            return false;
        }
    }
}
