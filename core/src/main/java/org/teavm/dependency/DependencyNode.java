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
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class DependencyNode implements ValueDependencyInfo {
    private DependencyNode ref;
    DependencyAnalyzer dependencyAnalyzer;
    List<DependencyConsumer> consumers;

    private TypeSet actualTypes;
    private TypeSet pendingTypes;
    DependencyNode nextPendingTypes;

    private Map<DependencyNode, Transition> pendingTransitions;
    Map<DependencyNode, Transition> transitions;
    DependencyNode nextPendingTransitions;
    boolean hasNewTransitions;

    String tag;
    private DependencyNode arrayItemNode;
    DependencyNode classValueNode;
    DependencyNode classNodeParent;
    private boolean classNodeComplete;
    int degree;
    boolean locked;
    MethodReference method;
    ValueType typeFilter;
    private DependencyTypeFilter cachedTypeFilter;

    int lowLink = -1;
    int index = -1;
    boolean onStack;

    DependencyNode(DependencyAnalyzer dependencyAnalyzer, ValueType typeFilter) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.typeFilter = typeFilter;
    }

    public void propagate(DependencyType type) {
        if (hasType(type.index) || !filter(type)) {
            return;
        }

        if (DependencyAnalyzer.shouldLog) {
            System.out.println(tag + " -> " + type.getName());
        }

        if (pendingTypes == null) {
            pendingTypes = new TypeSet(dependencyAnalyzer);
            setAsPendingTypes();
        }
        pendingTypes.addType(type.index);
    }

    private void setAsPendingTypes() {
        nextPendingTypes = dependencyAnalyzer.lastPendingTypes;
        dependencyAnalyzer.lastPendingTypes = this;
    }

    void propagate(int[] newTypes) {
        if (newTypes.length == 0) {
            return;
        }
        if (newTypes.length == 1) {
            propagate(dependencyAnalyzer.types.get(newTypes[0]));
            return;
        }

        if (typeFilter == null && actualTypes == null) {
            scheduleMultipleTypes(newTypes);
            return;
        }

        int j = 0;
        boolean copied = false;
        for (int i = 0; i < newTypes.length; ++i) {
            var type = newTypes[i];
            if (!hasType(type) && filter(dependencyAnalyzer.types.get(type))) {
                newTypes[j++] = type;
            } else if (!copied) {
                copied = true;
                newTypes = newTypes.clone();
            }
        }
        if (j == 0) {
            return;
        }

        if (j == 1) {
            propagate(dependencyAnalyzer.types.get(newTypes[0]));
            return;
        }

        if (j < newTypes.length) {
            newTypes = Arrays.copyOf(newTypes, j);
        }

        scheduleMultipleTypes(newTypes);
    }

    private void scheduleMultipleTypes(int[] newTypes) {
        if (pendingTypes == null) {
            pendingTypes = new TypeSet(dependencyAnalyzer);
            setAsPendingTypes();
        }
        pendingTypes.addTypes(newTypes);
    }

    void propagate(BitSet newTypes) {
        if (typeFilter == null && actualTypes == null) {
            scheduleMultipleTypes(newTypes);
            return;
        }

        boolean copied = false;
        var count = 0;
        for (int type = newTypes.nextSetBit(0); type >= 0; type = newTypes.nextSetBit(type + 1)) {
            if (hasType(type) || filter(dependencyAnalyzer.types.get(type))) {
                if (!copied) {
                    copied = true;
                    newTypes = (BitSet) newTypes.clone();
                    count = newTypes.cardinality();
                }
                newTypes.clear(type);
                --count;
            }
        }

        if (count == 0) {
            return;
        }
        if (count == 1) {
            propagate(dependencyAnalyzer.types.get(newTypes.nextSetBit(0)));
            return;
        }
        if (count <= TypeSet.SMALL_TYPES_THRESHOLD) {
            var array = new int[count];
            var index = 0;
            for (int type = newTypes.nextSetBit(0); type >= 0; type = newTypes.nextSetBit(type + 1)) {
                array[index++] = type;
            }
            scheduleMultipleTypes(array);
            return;
        }

        scheduleMultipleTypes(newTypes);
    }

    private void scheduleMultipleTypes(BitSet newTypes) {
        if (pendingTypes == null) {
            pendingTypes = new TypeSet(dependencyAnalyzer);
            setAsPendingTypes();
        }
        pendingTypes.addTypes(newTypes);
    }

    boolean filter(DependencyType type) {
        if (typeFilter == null) {
            return true;
        }

        return getFilter().match(type);
    }

    DependencyTypeFilter getFilter() {
        if (cachedTypeFilter == null) {
            if (typeFilter == null) {
                cachedTypeFilter = t -> true;
            } else {
                String superClass;
                if (typeFilter instanceof ValueType.Object) {
                    superClass = ((ValueType.Object) typeFilter).getClassName();
                } else {
                    superClass = typeFilter.toString();
                }
                cachedTypeFilter = dependencyAnalyzer.getSuperClassFilter(superClass);
            }
        }
        return cachedTypeFilter;
    }

    public void addConsumer(DependencyConsumer consumer) {
        if (consumers == null) {
            consumers = new ArrayList<>(1);
        }
        if (consumers.contains(consumer)) {
            return;
        }
        consumers.add(consumer);
    }

    public void connect(DependencyNode node) {
        if (connectWithoutChildNodes(node)) {
            connectArrayItemNodes(node);

            if (classNodeParent == null) {
                if (classValueNode != null && classValueNode != this) {
                    if (filter(dependencyAnalyzer.classType) && node.filter(dependencyAnalyzer.classType)) {
                        classValueNode.connect(node.getClassValueNode());
                    }
                }
            }
        }
    }

    private boolean connectWithoutChildNodes(DependencyNode node) {
        if (this == node) {
            return false;
        }
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null");
        }
        if (transitions != null && transitions.containsKey(node)) {
            return false;
        }

        if (pendingTransitions == null) {
            pendingTransitions = new LinkedHashMap<>();
            nextPendingTransitions = dependencyAnalyzer.lastPendingTransition;
            dependencyAnalyzer.lastPendingTransition = this;
        }
        if (pendingTransitions.containsKey(node)) {
            return false;
        }

        var transition = new Transition(this, node);
        pendingTransitions.put(node, transition);
        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Connecting " + tag + " to " + node.tag);
        }

        return true;
    }

    private void connectArrayItemNodes(DependencyNode node) {
        if (!isArray(typeFilter) || !isArray(node.typeFilter)) {
            return;
        }
        if (Objects.equals(typeFilter, node.typeFilter)) {
            if (arrayItemNode != null && node.arrayItemNode == null) {
                node.arrayItemNode = arrayItemNode;
                return;
            }
            if (node.arrayItemNode != null && arrayItemNode == null) {
                arrayItemNode = node.arrayItemNode;
                return;
            }
            if (node.arrayItemNode == null && arrayItemNode == null) {
                node.arrayItemNode = getArrayItem();
                return;
            }
        }
        getArrayItem().connect(node.getArrayItem());
        node.getArrayItem().connect(getArrayItem());
    }

    private static boolean isArray(ValueType type) {
        if (type == null || type.isObject("java.lang.Object")) {
            return true;
        }
        return type instanceof ValueType.Array;
    }

    private void connectClassValueNodes() {
        if (classNodeComplete) {
            return;
        }
        classNodeComplete = true;

        if (classNodeParent == null || classNodeParent.transitions == null) {
            return;
        }

        if (!classNodeParent.filter(dependencyAnalyzer.classType)) {
            return;
        }

        for (Transition transition : classNodeParent.transitions.values().toArray(Transition[]::new)) {
            if (transition.getDestination().classNodeParent != null) {
                continue;
            }
            if (transition.getSource().filter(dependencyAnalyzer.classType)) {
                connect(transition.getDestination().getClassValueNode());
            }
        }
    }

    @Override
    public DependencyNode getArrayItem() {
        if (ref != null) {
            return resolve().getArrayItem();
        }
        if (arrayItemNode == null) {
            arrayItemNode = dependencyAnalyzer.createArrayItemNode(this);
        }
        return arrayItemNode;
    }

    @Override
    public DependencyNode getClassValueNode() {
        if (ref != null) {
            return resolve().getClassValueNode();
        }
        if (classValueNode == null) {
            classValueNode = dependencyAnalyzer.createClassValueNode(degree, this);
            classValueNode.connectClassValueNodes();
        }
        return classValueNode;
    }

    @Override
    public boolean hasArrayType() {
        if (ref != null) {
            return resolve().hasArrayType();
        }
        return arrayItemNode != null && arrayItemNode.actualTypes != null && arrayItemNode.actualTypes.hasAnyType();
    }

    public boolean hasType(DependencyType type) {
        if (ref != null) {
            return resolve().hasType(type);
        }
        return actualTypes != null && actualTypes.hasType(type);
    }

    boolean hasType(int type) {
        return actualTypes != null && actualTypes.hasType(type);
    }

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyAnalyzer.getType(type));
    }

    @Override
    public String[] getTypes() {
        if (ref != null) {
            return resolve().getTypes();
        }
        if (actualTypes == null) {
            return new String[0];
        }
        DependencyType[] types = actualTypes.getTypes();
        String[] result = new String[types.length];
        int i = 0;
        for (DependencyType type : types) {
            if (filter(type)) {
                result[i++] = type.getName();
            }
        }
        return i == result.length ? result : Arrays.copyOf(result, i);
    }

    @Override
    public boolean hasMoreTypesThan(int limit) {
        if (ref != null) {
            return resolve().hasMoreTypesThan(limit);
        }
        if (actualTypes == null) {
            return false;
        }
        return actualTypes.hasMoreTypesThan(limit, typeFilter != null ? getFilter()::match : null);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    DependencyNode resolve() {
        if (ref == null) {
            return null;
        }
        ref = ref.resolve();
        return ref;
    }

    void applyPendingTransitions() {
        if (transitions != null) {
            pendingTransitions.keySet().removeAll(transitions.keySet());
            var oldSize = transitions.size();
            transitions.putAll(pendingTransitions);
            if (transitions.size() > oldSize) {
                hasNewTransitions = true;
            }
        } else {
            transitions = new HashMap<>(pendingTransitions);
            hasNewTransitions = true;
        }
        pendingTransitions = null;
    }

    void applyPendingTypes() {
        nextPendingTypes = null;

        if (pendingTypes != null) {
            if (actualTypes == null) {
                actualTypes = new TypeSet(dependencyAnalyzer);
            }
            actualTypes.addTypes(pendingTypes);
            var localPendingTypes = pendingTypes;
            pendingTypes = null;
            hasNewTransitions = false;
            if (transitions != null) {
                if (localPendingTypes.data instanceof int[]) {
                    var types = (int[]) localPendingTypes.data;
                    for (var transition : transitions.values()) {
                        transition.fresh = false;
                        transition.getDestination().propagate(types);
                    }
                } else if (localPendingTypes.data instanceof BitSet) {
                    var types = (BitSet) localPendingTypes.data;
                    for (var transition : transitions.values()) {
                        transition.fresh = false;
                        transition.getDestination().propagate(types);
                    }
                }
            }
            if (consumers != null) {
                if (localPendingTypes.data instanceof int[]) {
                    var types = (int[]) localPendingTypes.data;
                    for (var typeIndex : types) {
                        var type = dependencyAnalyzer.types.get(typeIndex);
                        for (var consumer : consumers) {
                            consumer.consume(type);
                        }
                    }
                } else if (localPendingTypes.data instanceof BitSet) {
                    var types = (BitSet) localPendingTypes.data;
                    for (var typeIndex = types.nextSetBit(0); typeIndex >= 0;
                         typeIndex = types.nextSetBit(typeIndex + 1)) {
                        var type = dependencyAnalyzer.types.get(typeIndex);
                        for (var consumer : consumers) {
                            consumer.consume(type);
                        }
                    }
                }
            }
        } else if (hasNewTransitions) {
            hasNewTransitions = false;
            if (actualTypes != null) {
                if (actualTypes.data instanceof int[]) {
                    var types = (int[]) actualTypes.data;
                    for (var transition : transitions.values()) {
                        if (transition.fresh) {
                            transition.fresh = false;
                            transition.getDestination().propagate(types);
                        }
                    }
                } else if (actualTypes.data instanceof BitSet) {
                    var types = (BitSet) actualTypes.data;
                    for (var transition : transitions.values()) {
                        if (transition.fresh) {
                            transition.fresh = false;
                            transition.getDestination().propagate(types);
                        }
                    }
                }
            }
        }
    }

    void merge(DependencyNode node) {
        transitions.putAll(node.transitions);
        node.transitions = null;
        if (isCompatibleTypeFilter(node)) {
            node.ref = this;
            transitions.remove(node);
        }
    }

    private boolean isCompatibleTypeFilter(DependencyNode node) {
        if (node.typeFilter == null || node.typeFilter == typeFilter) {
            return true;
        }
        if (typeFilter != null) {
            if (node.typeFilter == null) {
                return true;
            }
            if (dependencyAnalyzer.getClassHierarchy().isSuperType(node.typeFilter, typeFilter, false)) {
                return true;
            }
        }
        return false;
    }
}
