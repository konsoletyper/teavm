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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;

public class PointsToAnalysis {
    private ClassHierarchy hierarchy;
    private AnalysisState state;
    private List<Node> nodes = new ArrayList<>();

    public PointsToAnalysis(ClassReaderSource classes) {
        hierarchy = new ClassHierarchy(classes);
        state = new AnalysisState(hierarchy);
    }

    public AnalysisState state() {
        return state;
    }

    public ClassHierarchy hierarchy() {
        return hierarchy;
    }

    public void perform() {
        while (!state.nodesToEnter.isEmpty()) {
            dfs();
            propagate();
            sendToConsumers();
        }
    }

    private void dfs() {
        for (var node : nodes) {
            node.solverState = 0;
        }
        nodes.clear();
        var stack = new ArrayDeque<Node>();
        for (var node : state.nodesToEnter) {
            node.solverState = 0;
            stack.push(node);
        }
        while (!stack.isEmpty()) {
            var node = stack.pop();
            switch (node.solverState) {
                case 0:
                    node.solverState = 0;
                    stack.push(node);
                    var constraint = node.firstCopyConstraint;
                    while (constraint != null) {
                        if (constraint.to.solverState == 0) {
                            stack.push(constraint.to);
                        }
                        constraint = constraint.next;
                    }
                    break;
                case 1:
                    nodes.add(node);
                    break;
            }
        }
        Collections.reverse(nodes);
        for (var i = 0; i < nodes.size(); i++) {
            nodes.get(i).index = i;
        }
    }

    private void propagate() {
        while (propagateForward()) {
            // keep iterating
        }
    }

    private boolean propagateForward() {
        var needsMoreIterations = false;
        for (var node : nodes) {
            var diff = applyIncomingLocations(node);
            if (diff != null) {
                node.locations = state.locationSets().union(node.locations, diff);
                var locationsJustAdded = node.locationsJustAdded;
                if (locationsJustAdded == null) {
                    locationsJustAdded = state.locationSets().empty();
                }
                node.locationsJustAdded = state.locationSets().union(locationsJustAdded, diff);
                for (var constraint = node.firstCopyConstraint; constraint != null; constraint = constraint.next) {
                    constraint.to.scheduleLocations(diff);
                }
            }
        }
        return needsMoreIterations;
    }

    private LocationSet applyIncomingLocations(Node node) {
        if (node.incomingLocations == null) {
            return null;
        }
        if (node.incomingLocations.size() > 2) {
            node.incomingLocations.sort(Comparator.comparing(LocationSet::size).reversed());
        }
        var delta = node.incomingLocations.get(0);
        for (var i = 1; i < node.incomingLocations.size(); i++) {
            delta = state.locationSets().union(delta, node.incomingLocations.get(i));
        }
        node.incomingLocations = null;

        delta = state.locationSets().diff(delta, node.locations);
        if (delta.isEmpty()) {
            return null;
        }
        if (node.typeFilter != null) {
            delta = state.locationSets().filter(delta, state.typeFilter(node.typeFilter));
            if (delta.isEmpty()) {
                return null;
            }
        }
        if (node.customFilter != null) {
            delta = state.locationSets().filter(delta, node.customFilter);
            if (delta.isEmpty()) {
                return null;
            }
        }

        return delta;
    }

    private void sendToConsumers() {
        for (var node : nodes) {
            if (node.locationsJustAdded != null) {
                if (node.consumers == null) {
                    var data = node.locationsJustAdded;
                    for (var consumer : node.consumers) {
                        consumer.accept(data);
                    }
                }
                node.locationsJustAdded = null;
            }
        }
    }
}
