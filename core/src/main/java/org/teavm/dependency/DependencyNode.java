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

    TypeSet typeSet;
    private Object pendingTypes;
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
        if (hasType(type) || !filter(type)) {
            return;
        }

        if (DependencyAnalyzer.shouldLog) {
            System.out.println(tag + " -> " + type.getName());
        }

        if (pendingTypes == null) {
            pendingTypes = new int[] { type.index };
            setAsPendingTypes();
            return;
        } else if (pendingTypes instanceof int[]) {
            var array = (int[]) pendingTypes;
            for (var typeIndex : array) {
                if (typeIndex == type.index) {
                    return;
                }
            }
            if (array.length + 1 <= TypeSet.SMALL_TYPES_THRESHOLD) {
                array = Arrays.copyOf(array, array.length + 1);
                array[array.length - 1] = type.index;
                pendingTypes = array;
                setAsPendingTypes();
                return;
            } else {
                var bitSet = new BitSet();
                for (var typeIndex : array) {
                    bitSet.set(typeIndex);
                }
                pendingTypes = bitSet;
            }
        }

        var bitSet = (BitSet) pendingTypes;
        bitSet.set(type.index);
    }

    private void setAsPendingTypes() {
        nextPendingTypes = dependencyAnalyzer.lastPendingTypes;
        dependencyAnalyzer.lastPendingTypes = this;
    }

    public void propagate(DependencyType[] newTypes) {
        if (newTypes.length == 0) {
            return;
        }
        if (newTypes.length == 1) {
            propagate(newTypes[0]);
            return;
        }

        int j = 0;
        boolean copied = false;
        for (int i = 0; i < newTypes.length; ++i) {
            DependencyType type = newTypes[i];
            if (!hasType(type) && filter(type)) {
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
            propagate(newTypes[0]);
            return;
        }

        if (j < newTypes.length) {
            newTypes = Arrays.copyOf(newTypes, j);
        }

        for (DependencyType newType : newTypes) {
            typeSet.addType(newType);
        }
        scheduleMultipleTypes(newTypes);
    }

    private void scheduleMultipleTypes(DependencyType[] newTypes) {
        if (newTypes.length > TypeSet.SMALL_TYPES_THRESHOLD) {
            if (pendingTypes == null) {
                setAsPendingTypes();
                pendingTypes = new BitSet();
            } else if (pendingTypes instanceof int[]) {
                var array = (int[]) pendingTypes;
                var bitSet = new BitSet();
                for (var typeIndex : array) {
                    bitSet.set(typeIndex);
                }
                pendingTypes = bitSet;
            }
        } else {
            if (pendingTypes == null) {
                pendingTypes = newTypes.clone();
                setAsPendingTypes();
                return;
            }

            if (pendingTypes instanceof int[]) {
                var array = (int[]) pendingTypes;
                var sizeToAdd = newTypes.length;
                for (var type : newTypes) {
                    for (var typeIndex : array) {
                        if (typeIndex == type.index) {
                            --sizeToAdd;
                        }
                    }
                }
                if (sizeToAdd == 0) {
                    return;
                }
                if (array.length + sizeToAdd < TypeSet.SMALL_TYPES_THRESHOLD) {
                    var i = array.length;
                    var oldLength = i;
                    array = Arrays.copyOf(array, array.length + sizeToAdd);
                    outer:
                    for (var newType : newTypes) {
                        for (var j = 0; j < oldLength; ++j) {
                            if (newType.index == array[j]) {
                                continue outer;
                            }
                        }
                        array[i++] = newType.index;
                    }
                    pendingTypes = array;
                    return;
                } else {
                    var bitSet = new BitSet();
                    for (var typeIndex : array) {
                        bitSet.set(typeIndex);
                    }
                    pendingTypes = bitSet;
                }
            }
        }

        var bitSet = (BitSet) pendingTypes;
        for (var newType : newTypes) {
            bitSet.set(newType.index);
        }
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

    public void connect(DependencyNode node, DependencyTypeFilter filter) {
        if (connectWithoutChildNodes(node, filter)) {
            connectArrayItemNodes(node);

            if (classNodeParent == null) {
                if (classValueNode != null && classValueNode != this) {
                    if (filter(dependencyAnalyzer.classType) && node.filter(dependencyAnalyzer.classType)
                            && (filter == null || filter.match(dependencyAnalyzer.classType))) {
                        classValueNode.connect(node.getClassValueNode());
                    }
                }
            }
        }
    }

    private boolean connectWithoutChildNodes(DependencyNode node, DependencyTypeFilter filter) {
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

        var transition = new Transition(this, node, filter);
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
            if (transition.destination.classNodeParent != null) {
                continue;
            }
            if (transition.destination.filter(dependencyAnalyzer.classType)
                    && (transition.filter == null || transition.filter.match(dependencyAnalyzer.classType))) {
                connect(transition.destination.getClassValueNode());
            }
        }
    }

    public void connect(DependencyNode node) {
        if (ref != null) {
            resolve().connect(node);
            return;
        }
        connect(node, null);
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
        return arrayItemNode != null && arrayItemNode.typeSet != null && arrayItemNode.typeSet.hasAnyType();
    }

    public boolean hasType(DependencyType type) {
        if (ref != null) {
            return resolve().hasType(type);
        }
        return typeSet != null && typeSet.hasType(type);
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
        if (typeSet == null) {
            return new String[0];
        }
        DependencyType[] types = typeSet.getTypes();
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
        if (typeSet == null) {
            return false;
        }
        return typeSet.hasMoreTypesThan(limit, typeFilter != null ? getFilter()::match : null);
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
            var localPendingTypes = pendingTypes;
            pendingTypes = null;
            hasNewTransitions = false;
            if (transitions != null) {
                if (localPendingTypes instanceof int[]) {
                    for (var transition : transitions.values()) {
                        transition.fresh = false;
                        transition.destination.pendingTypes = localPendingTypes;
                    }
                }
            }
            if (consumers != null) {

            }
        } else if (hasNewTransitions) {
            hasNewTransitions = false;
            if (typeSet != null) {
                var types = typeSet.getTypes();
                for (var transition : transitions.values()) {
                    if (transition.fresh) {
                        transition.fresh = false;
                        transition.destination.propagate(types);
                    }
                }
            } else {
                for (var transition : transitions.values()) {
                    transition.fresh = false;
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
