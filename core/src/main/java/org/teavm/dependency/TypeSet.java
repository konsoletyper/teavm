/*
 *  Copyright 2018 Alexey Andreev.
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

import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.ObjectHashSet;
import com.carrotsearch.hppc.ObjectStack;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class TypeSet {
    private static final int SMALL_TYPES_THRESHOLD = 3;
    private static final DependencyType[] EMPTY_TYPES = new DependencyType[0];
    private DependencyAnalyzer dependencyAnalyzer;
    DependencyNode origin;
    private int[] smallTypes;
    private BitSet types;
    private int typesCount;

    Set<DependencyNode> domain = new LinkedHashSet<>();
    ObjectHashSet<DependencyNode> loopNodes;
    ObjectArrayList<DependencyNodeToNodeTransition> transitions;
    ArrayList<ConsumerWithNode> consumers;

    TypeSet(DependencyAnalyzer dependencyAnalyzer, DependencyNode origin) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.origin = origin;
        domain.add(origin);
    }

    void addType(DependencyType type) {
        if (types == null) {
            if (smallTypes == null) {
                smallTypes = new int[] { type.index };
                return;
            }
        }
        if (smallTypes != null) {
            if (smallTypes.length == SMALL_TYPES_THRESHOLD) {
                types = new BitSet(dependencyAnalyzer.types.size() * 2);
                for (int existingType : smallTypes) {
                    types.set(existingType);
                }
                typesCount = smallTypes.length;
                smallTypes = null;
            } else {
                smallTypes = Arrays.copyOf(smallTypes, smallTypes.length + 1);
                smallTypes[smallTypes.length - 1] = type.index;
                return;
            }
        }
        types.set(type.index);
        typesCount++;
        return;
    }

    DependencyType[] getTypes() {
        if (this.types != null) {
            DependencyType[] types = new DependencyType[this.types.cardinality()];
            int j = 0;
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                types[j++] = type;
            }
            return types;
        } else if (this.smallTypes != null) {
            DependencyType[] types = new DependencyType[smallTypes.length];
            for (int i = 0; i < types.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                types[i] = type;
            }
            return types;
        } else {
            return EMPTY_TYPES;
        }
    }

    boolean hasType(DependencyType type) {
        if (smallTypes != null) {
            for (int i = 0; i < smallTypes.length; ++i) {
                if (smallTypes[i] == type.index) {
                    return true;
                }
            }
            return false;
        }
        return types != null && types.get(type.index);
    }

    boolean hasAnyType() {
        return types != null || smallTypes != null;
    }

    TypeSet copy(DependencyNode origin) {
        TypeSet result = new TypeSet(dependencyAnalyzer, origin);
        result.types = types != null ? (BitSet) types.clone() : null;
        result.smallTypes = smallTypes;
        result.typesCount = typesCount;
        return result;
    }

    void invalidate() {
        transitions = null;
        consumers = null;
        loopNodes = null;
    }

    ObjectArrayList<DependencyNodeToNodeTransition> getTransitions() {
        if (transitions == null) {
            transitions = new ObjectArrayList<>(domain.size() * 2);
            for (DependencyNode node : domain) {
                if (node.transitions != null) {
                    for (ObjectCursor<DependencyNodeToNodeTransition> cursor : node.transitionList) {
                        DependencyNodeToNodeTransition transition = cursor.value;
                        if (transition.filter != null || transition.destination.typeSet != this) {
                            transitions.add(transition);
                        }
                    }
                }
            }
        }
        return transitions;
    }

    List<ConsumerWithNode> getConsumers() {
        if (consumers == null) {
            consumers = new ArrayList<>();
            for (DependencyNode node : domain) {
                if (node.followers != null) {
                    consumers.add(new ConsumerWithNode(node.followers.toArray(new DependencyConsumer[0]), node));
                }
            }
            consumers.trimToSize();
        }
        return consumers;
    }

    boolean reachesOrigin(DependencyNode node) {
        if (loopNodes == null) {
            findLoopNodes();
        }
        return loopNodes.contains(node);
    }

    private void findLoopNodes() {
        if (domain.size() == 1) {
            return;
        }

        for (DependencyNode node : domain) {
            node.visitedFlag = false;
        }

        loopNodes = new ObjectHashSet<>(domain.size());
        loopNodes.add(origin);

        ObjectStack<DependencyNode> stack = new ObjectStack<>(domain.size());
        stack.push(origin);

        while (!stack.isEmpty()) {
            DependencyNode next = stack.pop();
            if (next.visitedFlag) {
                continue;
            }
            next.visitedFlag = true;

            if (next.transitions != null) {
                for (ObjectCursor<DependencyNodeToNodeTransition> cursor : next.transitionList) {
                    DependencyNodeToNodeTransition transition = cursor.value;
                    if (transition.destination.typeSet == this) {
                        if (!transition.destination.visitedFlag) {
                            stack.push(transition.destination);
                        } else if (loopNodes.contains(transition.destination)) {
                            loopNodes.add(next);
                        }
                    }
                }
            }
        }
    }

    int typeCount() {
        return smallTypes != null ? smallTypes.length : types != null ? typesCount : 0;
    }
}
