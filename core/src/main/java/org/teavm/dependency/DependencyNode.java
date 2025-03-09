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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class DependencyNode implements ValueDependencyInfo {
    int orderIndex;
    DependencyAnalyzer dependencyAnalyzer;
    private List<DependencyConsumer> consumers;

    private TypeSet actualTypes;
    private TypeSet pendingTypes;
    DependencyNode nextPendingTypes;
    boolean inPendingTypesList;
    boolean inPropagationList;

    private Map<DependencyNode, Transition> pendingTransitions;
    private Map<DependencyNode, Transition> transitions;
    DependencyNode nextPendingTransitions;
    private boolean hasNewTransitions;

    String tag;
    private DependencyNode arrayItemNode;
    DependencyNode classValueNode;
    int degree;
    boolean locked;
    MethodReference method;
    ValueType typeFilter;
    private DependencyTypeFilter cachedTypeFilter;
    private boolean hasArray;
    private boolean hasClass;

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

        if (!hasArray) {
            if (dependencyAnalyzer.types.get(type.index).getName().startsWith("[")) {
                propagateArray();
            }
        }
        if (!hasClass) {
            if (type.index == dependencyAnalyzer.classType.index) {
                propagateClass();
            }
        }
    }

    private void setAsPendingTypes() {
        if (!inPropagationList) {
            if (!inPendingTypesList) {
                inPendingTypesList = true;
                nextPendingTypes = dependencyAnalyzer.lastPendingTypes;
                dependencyAnalyzer.lastPendingTypes = this;
            }
        } else {
            dependencyAnalyzer.hasPendingTypesInList = true;
        }
    }

    private void propagate(int[] newTypes) {
        if (newTypes.length == 0) {
            return;
        }
        if (newTypes.length == 1) {
            propagate(dependencyAnalyzer.types.get(newTypes[0]));
            return;
        }

        if (typeFilter == null && actualTypes == null && pendingTypes == null) {
            scheduleMultipleTypes(newTypes);
            return;
        }

        int j = 0;
        boolean copied = false;
        for (int i = 0; i < newTypes.length; ++i) {
            var type = newTypes[i];
            if (!hasType(type) && !hasPendingType(type) && filter(dependencyAnalyzer.types.get(type))) {
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
        if (DependencyAnalyzer.shouldLog) {
            for (var typeIndex : newTypes) {
                System.out.println(tag + " -> " + dependencyAnalyzer.types.get(typeIndex).getName());
            }
        }
        if (pendingTypes == null) {
            pendingTypes = new TypeSet(dependencyAnalyzer);
            setAsPendingTypes();
        }
        pendingTypes.addTypes(newTypes);

        if (!hasArray) {
            for (var index : newTypes) {
                if (dependencyAnalyzer.types.get(index).getName().startsWith("[")) {
                    propagateArray();
                    break;
                }
            }
        }
        if (!hasClass) {
            for (var index : newTypes) {
                if (index == dependencyAnalyzer.classType.index) {
                    propagateClass();
                    break;
                }
            }
        }
    }

    private void propagateArray() {
        hasArray = true;
        if (transitions != null) {
            for (var transition : transitions.values().toArray(Transition[]::new)) {
                connectArrayItemNodes(transition.destination);
            }
        }
        if (pendingTransitions != null) {
            for (var transition : pendingTransitions.values().toArray(Transition[]::new)) {
                connectArrayItemNodes(transition.destination);
            }
        }
    }

    private void propagateClass() {
        hasClass = true;
        if (transitions != null) {
            for (var transition : transitions.values().toArray(Transition[]::new)) {
                getClassValueNode().connect(transition.destination.getClassValueNode());
            }
        }
        if (pendingTransitions != null) {
            for (var transition : pendingTransitions.values().toArray(Transition[]::new)) {
                getClassValueNode().connect(transition.destination.getClassValueNode());
            }
        }
    }

    private void propagate(BitSet newTypes) {
        if (typeFilter == null && actualTypes == null) {
            scheduleMultipleTypes(newTypes);
            return;
        }

        boolean copied = false;
        var count = 0;
        var newTypesBackup = newTypes;
        for (int type = newTypesBackup.nextSetBit(0); type >= 0; type = newTypesBackup.nextSetBit(type + 1)) {
            if (hasType(type) || hasPendingType(type) || !filter(dependencyAnalyzer.types.get(type))) {
                if (!copied) {
                    copied = true;
                    newTypes = (BitSet) newTypes.clone();
                }
                newTypes.clear(type);
            } else {
                count++;
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
        if (DependencyAnalyzer.shouldLog) {
            for (var typeIndex = newTypes.nextSetBit(0); typeIndex >= 0;
                    typeIndex = newTypes.nextSetBit(typeIndex + 1)) {
                System.out.println(tag + " -> " + dependencyAnalyzer.types.get(typeIndex).getName());
            }
        }
        if (pendingTypes == null) {
            pendingTypes = new TypeSet(dependencyAnalyzer);
            setAsPendingTypes();
        }
        pendingTypes.addTypes(newTypes);

        if (!hasArray) {
            for (var index = newTypes.nextSetBit(0); index >= 0; index = newTypes.nextSetBit(index + 1)) {
                if (dependencyAnalyzer.types.get(index).getName().startsWith("[")) {
                    propagateArray();
                    break;
                }
            }
        }
        if (!hasClass) {
            if (newTypes.get(dependencyAnalyzer.classType.index)) {
                propagateClass();
            }
        }
    }

    private boolean filter(DependencyType type) {
        if (typeFilter == null) {
            return true;
        }

        return getFilter().match(type);
    }

    private DependencyTypeFilter getFilter() {
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
        if (actualTypes != null) {
            if (actualTypes.data instanceof int[]) {
                for (var typeIndex : (int[]) actualTypes.data) {
                    consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                }
            } else if (actualTypes.data instanceof BitSet) {
                var bitSet = (BitSet) actualTypes.data;
                for (var typeIndex = bitSet.nextSetBit(0); typeIndex >= 0;
                        typeIndex = bitSet.nextSetBit(typeIndex + 1)) {
                    consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                }
            }
        }
    }

    public void connect(DependencyNode node) {
        if (connectWithoutChildNodes(node)) {
            if (hasArray) {
                connectArrayItemNodes(node);
            }
            if (hasClass) {
                getClassValueNode().connect(node.getClassValueNode());
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
        if (Objects.equals(typeFilter, node.typeFilter)) {
            if (arrayItemNode != null && node.arrayItemNode == null) {
                node.arrayItemNode = arrayItemNode;
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(node.tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
            if (node.arrayItemNode != null && arrayItemNode == null) {
                arrayItemNode = node.arrayItemNode;
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
            if (node.arrayItemNode == null && arrayItemNode == null) {
                node.arrayItemNode = getArrayItem();
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(node.tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
        }
        getArrayItem().connect(node.getArrayItem());
        node.getArrayItem().connect(getArrayItem());
    }

    @Override
    public DependencyNode getArrayItem() {
        if (arrayItemNode == null) {
            arrayItemNode = dependencyAnalyzer.createArrayItemNode(this);
        }
        return arrayItemNode;
    }

    @Override
    public DependencyNode getClassValueNode() {
        if (classValueNode == null) {
            classValueNode = dependencyAnalyzer.createClassValueNode(degree, this);
        }
        return classValueNode;
    }

    @Override
    public boolean hasArrayType() {
        return arrayItemNode != null && arrayItemNode.actualTypes != null && arrayItemNode.actualTypes.hasAnyType();
    }

    private boolean hasType(DependencyType type) {
        return hasType(type.index);
    }

    private boolean hasType(int type) {
        return actualTypes != null && actualTypes.hasType(type);
    }

    private boolean hasPendingType(int type) {
        return pendingTypes != null && pendingTypes.hasType(type);
    }

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyAnalyzer.getType(type));
    }

    @Override
    public String[] getTypes() {
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

    void applyPendingTransitions() {
        if (transitions != null) {
            pendingTransitions.keySet().removeAll(transitions.keySet());
            if (pendingTransitions.isEmpty()) {
                return;
            }
            transitions.putAll(pendingTransitions);
        } else {
            transitions = new LinkedHashMap<>(pendingTransitions);
        }
        if (actualTypes != null) {
            hasNewTransitions = true;
            for (var transition : pendingTransitions.values()) {
                transition.fresh = true;
            }
        }
        pendingTransitions = null;
    }

    void applyPendingTypes() {
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
                        if (!transition.fresh) {
                            transition.destination.propagate(types);
                        }
                    }
                } else if (localPendingTypes.data instanceof BitSet) {
                    var types = (BitSet) localPendingTypes.data;
                    for (var transition : transitions.values()) {
                        if (!transition.fresh) {
                            transition.destination.propagate(types);
                        }
                    }
                }
                propagateToFreshTransitions();
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
            propagateToFreshTransitions();
        }
    }

    private void propagateToFreshTransitions() {
        hasNewTransitions = false;
        if (actualTypes != null && transitions != null) {
            if (actualTypes.data instanceof int[]) {
                var types = (int[]) actualTypes.data;
                for (var transition : transitions.values()) {
                    if (transition.fresh) {
                        transition.fresh = false;
                        transition.destination.propagate(types);
                    }
                }
            } else if (actualTypes.data instanceof BitSet) {
                var types = (BitSet) actualTypes.data;
                for (var transition : transitions.values()) {
                    if (transition.fresh) {
                        transition.fresh = false;
                        transition.destination.propagate(types);
                    }
                }
            }
        }
    }

    Collection<? extends Transition> successors() {
        return transitions != null ? transitions.values() : null;
    }

    void pack() {
        consumers = null;
        transitions = null;
        method = null;
    }
}
