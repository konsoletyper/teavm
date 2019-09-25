/*
 *  Copyright 2013 Alexey Andreev.
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

import com.carrotsearch.hppc.IntHashSet;
import java.util.Arrays;
import java.util.Collection;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ValueType;

class Transition {
    DependencyNode source;
    DependencyNode destination;
    DependencyTypeFilter filter;
    IntHashSet pendingTypes;
    byte destSubsetOfSrc;

    Transition(DependencyNode source, DependencyNode destination, DependencyTypeFilter filter) {
        this.source = source;
        this.destination = destination;
        this.filter = filter;
    }

    void consume(DependencyType type) {
        if (!destination.hasType(type) && filterType(type) && destination.filter(type)) {
            propagate(type);
        }
    }

    private void propagate(DependencyType type) {
        if (destination.typeSet == source.typeSet) {
            return;
        }

        if (shouldMergeDomains()) {
            mergeDomains(new DependencyType[] { type });
        } else {
            destination.propagate(type);
        }
    }

    private void propagate(DependencyType[] types) {
        if (destination.typeSet == source.typeSet) {
            return;
        }

        if (shouldMergeDomains()) {
            mergeDomains(types);
        } else {
            destination.propagate(types);
        }
    }

    void mergeDomains(DependencyType[] types) {
        destination.moveToSeparateDomain();
        destination.scheduleMultipleTypes(types, () -> {
            Collection<DependencyNode> domainToMerge = destination.typeSet.domain;
            for (DependencyNode node : domainToMerge) {
                node.typeSet = source.typeSet;
                source.typeSet.domain.add(node);
            }
            source.typeSet.invalidate();
        });
    }

    boolean shouldMergeDomains() {
        if (!source.dependencyAnalyzer.domainOptimizationEnabled() || filter != null || !isDestSubsetOfSrc()) {
            return false;
        }
        if (destination.typeSet == null) {
            return true;
        }
        if (destination.typeSet == source.typeSet || destination.typeSet.origin == source
                || destination.typeSet.typeCount() > source.typeSet.typeCount()) {
            return false;
        }

        if (destination.splitCount > 4) {
            return false;
        }

        if (destination.typeSet.typeCount() == source.typeSet.typeCount()
                && destination.typeSet.origin != destination) {
            return false;
        }

        for (DependencyType type : destination.getTypesInternal()) {
            if (!source.hasType(type)) {
                return false;
            }
        }

        return true;
    }

    void consume(DependencyType[] types) {
        int j = 0;
        boolean copied = false;

        if (filter == null) {
            for (DependencyType type : types) {
                boolean added = false;
                if (!destination.hasType(type) && destination.filter(type)) {
                    types[j++] = type;
                    added = true;
                }

                if (!added && !copied) {
                    copied = true;
                    types = types.clone();
                }
            }
        } else {
            for (DependencyType type : types) {
                boolean added = false;
                if (filterType(type) && !destination.hasType(type) && destination.filter(type)) {
                    types[j++] = type;
                    added = true;
                }
                if (!added && !copied) {
                    copied = true;
                    types = types.clone();
                }
            }
        }

        if (j == 0) {
            return;
        }

        if (j == 1) {
            propagate(types[0]);
        } else {
            if (j < types.length) {
                types = Arrays.copyOf(types, j);
            }

            propagate(types);
        }
    }

    boolean filterType(DependencyType type) {
        if (pendingTypes != null && pendingTypes.contains(type.index)) {
            return false;
        }

        if (filter == null) {
            return true;
        }

        return filter.match(type);
    }

    boolean pointsToDomainOrigin() {
        return destination.typeSet == null || destination.typeSet.origin == destination;
    }

    boolean isDestSubsetOfSrc() {
        if (destSubsetOfSrc == 0) {
            destSubsetOfSrc = calculateDestSubsetOfSrc() ? (byte) 2 : 1;
        }
        return destSubsetOfSrc == 2;
    }

    private boolean calculateDestSubsetOfSrc() {
        if (source.typeFilter == null) {
            return true;
        }
        if (destination.typeFilter == null) {
            return false;
        }

        ValueType sourceType = source.typeFilter;
        ValueType destType = destination.typeFilter;
        ClassHierarchy hierarchy = source.dependencyAnalyzer.getClassHierarchy();

        return hierarchy.isSuperType(sourceType, destType, false);
    }
}
