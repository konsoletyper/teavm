/*
 *  Copyright 2012 Alexey Andreev.
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.callgraph.DefaultCallGraphNode;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class MethodDependency implements MethodDependencyInfo {
    private DependencyAnalyzer dependencyAnalyzer;
    DependencyNode[] variableNodes;
    private int parameterCount;
    DependencyNode resultNode;
    DependencyNode thrown;
    MethodHolder method;
    private MethodReference reference;
    boolean used;
    boolean external;
    DependencyPlugin dependencyPlugin;
    boolean dependencyPluginAttached;
    private List<LocationListener> locationListeners;
    private Set<CallLocation> locations;
    boolean activated;

    MethodDependency(DependencyAnalyzer dependencyAnalyzer, DependencyNode[] variableNodes, int parameterCount,
            DependencyNode resultNode, DependencyNode thrown, MethodHolder method, MethodReference reference) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.variableNodes = Arrays.copyOf(variableNodes, variableNodes.length);
        this.parameterCount = parameterCount;
        this.thrown = thrown;
        this.resultNode = resultNode;
        this.method = method;
        this.reference = reference;
    }

    public DependencyAgent getDependencyAgent() {
        return dependencyAnalyzer.getAgent();
    }

    @Override
    public DependencyNode[] getVariables() {
        return Arrays.copyOf(variableNodes, variableNodes.length);
    }

    void setVariables(DependencyNode[] variables) {
        this.variableNodes = variables;
    }

    @Override
    public int getVariableCount() {
        return variableNodes.length;
    }

    @Override
    public DependencyNode getVariable(int index) {
        return variableNodes[index];
    }

    @Override
    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public DependencyNode getResult() {
        return resultNode;
    }

    @Override
    public DependencyNode getThrown() {
        return thrown;
    }

    @Override
    public MethodReference getReference() {
        return reference;
    }

    public MethodReader getMethod() {
        return method;
    }

    @Override
    public boolean isMissing() {
        return method == null;
    }

    @Override
    public boolean isUsed() {
        return used;
    }

    public MethodDependency addLocation(CallLocation location) {
        DefaultCallGraphNode node = dependencyAnalyzer.callGraph.getNode(location.getMethod());
        if (locations == null) {
            locations = new LinkedHashSet<>();
        }
        if (locations.add(location)) {
            node.addCallSite(reference, location.getSourceLocation());
            if (locationListeners != null) {
                for (LocationListener listener : locationListeners.toArray(new LocationListener[0])) {
                    listener.locationAdded(location);
                }
            }
        }
        return this;
    }

    public void addLocationListener(LocationListener listener) {
        if (locationListeners == null) {
            locationListeners = new ArrayList<>();
            locationListeners.add(listener);
            if (locations != null) {
                for (CallLocation location : locations.toArray(new CallLocation[0])) {
                    listener.locationAdded(location);
                }
            }
        }
    }

    public MethodDependency propagate(int parameterIndex, Class<?> type) {
        return propagate(parameterIndex, dependencyAnalyzer.getType(type.getName()));
    }

    public MethodDependency propagate(int parameterIndex, String type) {
        return propagate(parameterIndex, dependencyAnalyzer.getType(type));
    }

    public MethodDependency propagate(int parameterIndex, DependencyType type) {
        getVariable(parameterIndex).propagate(type);
        return this;
    }

    public void use() {
        use(true);
    }

    void use(boolean external) {
        if (!used) {
            used = true;
            if (!isMissing()) {
                dependencyAnalyzer.scheduleMethodAnalysis(this);
            }
        }
        if (external) {
            this.external = true;
        }
    }

    @Override
    public boolean isCalled() {
        return external;
    }
}
