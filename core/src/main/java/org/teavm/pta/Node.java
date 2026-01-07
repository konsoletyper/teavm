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
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class Node {
    private AnalysisState state;
    private String description;
    List<Consumer<Collection<? extends Location>>> consumers;
    private Map<Node, CopyConstraint> copyConstraintIndex;
    CopyConstraint firstCopyConstraint;
    private CopyConstraint lastCopyConstraint;
    private int copyConstraintCount;
    LocationSet locations;
    LocationSet locationsJustAdded;
    List<LocationSet> incomingLocations;
    ValueType typeFilter;
    Predicate<Location> customFilter;
    int index;
    int solverState;

    Node(AnalysisState state, String description) {
        this.state = state;
        this.description = description;
        this.locations = state.locationSets().empty();
    }

    public Source<Location> locationSource() {
        return consumer -> {
            if (consumers == null) {
                consumers = new ArrayList<>();
            }
            consumers.add(consumer);
        };
    }

    public void takeLocations(Source<Location> locations) {
        locations.sink(update -> scheduleLocations(state.locationSets().ofCollection(update)));
    }

    void scheduleLocations(LocationSet locations) {
        if (incomingLocations == null) {
            incomingLocations = new ArrayList<>();
        }
        incomingLocations.add(locations);
    }

    boolean addCopyConstraint(Node to, TextLocation textLocation) {
        if (to == this || hasCopyConstraint(to)) {
            return false;
        }
        var constraint = new CopyConstraint(this, to, textLocation);
        if (lastCopyConstraint != null) {
            lastCopyConstraint.next = constraint;
        } else {
            firstCopyConstraint = constraint;
        }
        lastCopyConstraint = constraint;
        if (++copyConstraintCount > 3 && copyConstraintIndex == null) {
            copyConstraintIndex = new HashMap<>();
            constraint = firstCopyConstraint;
            while (constraint != null) {
                copyConstraintIndex.put(to, constraint);
                constraint = constraint.next;
            }
        }

        if (!this.locations.isEmpty()) {
            to.scheduleLocations(this.locations);
        }

        return true;
    }

    private boolean hasCopyConstraint(Node to) {
        if (copyConstraintIndex != null) {
            return copyConstraintIndex.containsKey(to);
        }
        var constraint = firstCopyConstraint;
        while (constraint != null) {
            if (constraint.to == to) {
                return true;
            }
            constraint = constraint.next;
        }
        return false;
    }

    @Override
    public String toString() {
        return description;
    }
}
