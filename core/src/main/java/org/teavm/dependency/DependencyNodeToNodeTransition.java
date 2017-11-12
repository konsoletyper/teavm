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

import com.carrotsearch.hppc.IntSet;
import java.util.Arrays;
import java.util.BitSet;

class DependencyNodeToNodeTransition implements DependencyConsumer {
    private DependencyNode source;
    DependencyNode destination;
    private DependencyTypeFilter filter;
    private BitSet knownFilteredOffTypes;
    IntSet pendingTypes;

    DependencyNodeToNodeTransition(DependencyNode source, DependencyNode destination, DependencyTypeFilter filter) {
        this.source = source;
        this.destination = destination;
        this.filter = filter;
    }

    @Override
    public void consume(DependencyType type) {
        if (!filterType(type)) {
            return;
        }
        if (type.getName().startsWith("[")) {
            source.getArrayItem().connect(destination.getArrayItem());
            destination.getArrayItem().connect(source.getArrayItem());
        }
        if (type.getName().equals("java.lang.Class")) {
            source.getClassValueNode().connect(destination.getClassValueNode());
        }
        if (!destination.hasType(type)) {
            destination.propagate(type);
        }
    }

    void consume(DependencyType[] types) {
        int j = 0;
        boolean copied = false;
        for (DependencyType type : types) {
            boolean added = false;
            if (filterType(type)) {
                if (!destination.hasType(type)) {
                    types[j++] = type;
                    added = true;
                }

                if (type.getName().startsWith("[")) {
                    source.getArrayItem().connect(destination.getArrayItem());
                    destination.getArrayItem().connect(source.getArrayItem());
                }
                if (type.getName().equals("java.lang.Class")) {
                    source.getClassValueNode().connect(destination.getClassValueNode());
                }
            }
            if (!added && !copied) {
                copied = true;
                types = types.clone();
            }
        }

        if (j == 0) {
            return;
        }

        if (j == 1) {
            destination.propagate(types[0]);
        } else {
            if (j < types.length) {
                types = Arrays.copyOf(types, j);
            }

            destination.propagate(types);
        }
    }

    private boolean filterType(DependencyType type) {
        if (filter == null) {
            return true;
        }

        if (knownFilteredOffTypes != null && knownFilteredOffTypes.get(type.index)) {
            return false;
        }
        if (!filter.match(type)) {
            if (knownFilteredOffTypes == null) {
                knownFilteredOffTypes = new BitSet(64);
            }
            knownFilteredOffTypes.set(type.index);
            return false;
        }

        return true;
    }
}
