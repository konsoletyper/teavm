/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.pta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class AnalysisState {
    private ClassHierarchy hierarchy;
    private List<Location> locations = new ArrayList<>();
    private LocationSets locationSets;
    private Map<String, Location> classInstanceLocations = new HashMap<>();
    private Map<ValueType, Location> classLiteralLocations = new HashMap<>();
    List<Node> nodesToEnter = new ArrayList<>();
    private Map<MethodReference, MethodHolder> resolvedMethods = new HashMap<>();
    private Map<MethodReference, MethodAnalysis> methods = new HashMap<>();

    AnalysisState(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        locationSets = new LocationSets(locations);
    }

    public ClassHierarchy hierarchy() {
        return hierarchy;
    }

    public Predicate<Location> typeFilter(ValueType type) {
        return location -> hierarchy().isSuperType(type, location.type(), false);
    }

    public Node createNode(String description) {
        return new Node(this, description);
    }

    public void addConstraint(Constraint constraint) {
        constraint.apply(this);
    }

    public void addCopyConstraint(Node from, Node to, TextLocation textLocation) {
        if (from.addCopyConstraint(to, textLocation)) {
            addNodeToEnter(from);
        }
    }

    private void addNodeToEnter(Node node) {
        if (node.solverState == 0) {
            node.solverState = 1;
            nodesToEnter.add(node);
        }
    }

    public LocationSets locationSets() {
        return locationSets;
    }

    public Location classInstanceLocation(String type) {
        return classInstanceLocations.computeIfAbsent(type, k -> createLocation(ValueType.object(k),
                "Heap(" + type + ")", null));
    }

    public Location classLiteralLocation(ValueType type) {
        return classLiteralLocations.computeIfAbsent(type, k -> createLocation(ValueType.object("java.lang.Class"),
                "ClassLiteral(" + type + ")", type));
    }

    public Location createLocation(ValueType type, String description, Object extension) {
        var location = new Location(this, locations.size(), type, description, extension);
        locations.add(location);
        return location;
    }

    public Consumer<Collection<? extends Node>> copyToSink(Node targetNode, TextLocation textLocation) {
        return nodes -> {
            for (var node : nodes) {
                addCopyConstraint(node, targetNode, textLocation);
            }
        };
    }

    public Consumer<Collection<? extends Node>> copyFromSink(Node sourceNode, TextLocation textLocation) {
        return nodes -> {
            for (var node : nodes) {
                addCopyConstraint(sourceNode, node, textLocation);
            }
        };
    }

    public MethodAnalysis method(MethodReference method) {
        return methods.computeIfAbsent(method, k -> new MethodAnalysis(this, null));
    }
}
