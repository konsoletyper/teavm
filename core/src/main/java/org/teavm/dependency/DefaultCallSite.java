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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;

public class DefaultCallSite implements CallSite, Serializable {
    private Map<CallGraphNode, Set<TextLocation>> locations;
    private TextLocation singleLocation;
    MethodReference method;
    Set<DefaultCallGraphNode> callers;
    private DefaultCallGraphNode singleCaller;
    Set<DefaultCallGraphNode> calledMethods;
    DefaultCallGraphNode singleCalledMethod;
    Collection<? extends DefaultCallGraphNode> readonlyCalledMethods;

    DefaultCallSite(MethodReference method, Set<DefaultCallGraphNode> callers) {
        this.method = method;
        this.callers = callers;
        locations = new HashMap<>();
        calledMethods = new LinkedHashSet<>();
    }

    DefaultCallSite(DefaultCallGraphNode callee, DefaultCallGraphNode caller) {
        this.singleCalledMethod = callee;
        this.singleCaller = caller;
    }

    @Override
    public Collection<? extends TextLocation> getLocations(CallGraphNode caller) {
        if (singleLocation != null) {
            return caller == this.singleCaller ? Collections.singleton(singleLocation) : Collections.emptySet();
        }
        if (locations == null) {
            return Collections.emptyList();
        }
        Set<TextLocation> result = locations.get(caller);
        return result != null ? Collections.unmodifiableSet(result) : Collections.emptySet();
    }

    public void addLocation(DefaultCallGraphNode caller, TextLocation location) {
        if (locations == null) {
            if (singleLocation == null && callers == null) {
                singleLocation = location;
                return;
            }
            locations = new LinkedHashMap<>();
            if (singleLocation != null) {
                Set<TextLocation> singleLocations = new LinkedHashSet<>();
                singleLocations.add(singleLocation);
                locations.put(singleCaller, singleLocations);
            }
        }
        locations.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(location);
    }

    @Override
    public Collection<? extends DefaultCallGraphNode> getCalledMethods() {
        if (singleCalledMethod != null) {
            return Collections.singletonList(singleCalledMethod);
        }
        if (readonlyCalledMethods == null) {
            readonlyCalledMethods = Collections.unmodifiableCollection(calledMethods);
        }
        return readonlyCalledMethods;
    }

    @Override
    public Collection<? extends DefaultCallGraphNode> getCallers() {
        return callers != null ? callers : Collections.singletonList(singleCaller);
    }
}
