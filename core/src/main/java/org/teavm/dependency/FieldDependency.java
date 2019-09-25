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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.model.CallLocation;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;

public class FieldDependency implements FieldDependencyInfo {
    DependencyNode value;
    private FieldReader field;
    private boolean present;
    private FieldReference reference;
    List<LocationListener> locationListeners;
    Set<CallLocation> locations;
    boolean activated;

    FieldDependency(DependencyNode value, FieldReader field, FieldReference reference) {
        this.value = value;
        this.field = field;
        this.reference = reference;
    }

    @Override
    public DependencyNode getValue() {
        return value;
    }

    public FieldReader getField() {
        return field;
    }

    @Override
    public FieldReference getReference() {
        return reference;
    }

    @Override
    public boolean isMissing() {
        return field == null && !present;
    }

    public FieldDependency addLocation(CallLocation location) {
        DefaultCallGraphNode node = value.dependencyAnalyzer.callGraph.getNode(location.getMethod());
        if (locations == null) {
            locations = new LinkedHashSet<>();
        }
        if (locations.add(location)) {
            node.addFieldAccess(reference, location.getSourceLocation());
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

    void cleanup() {
        if (field != null) {
            field = null;
            present = true;
        }
    }
}
