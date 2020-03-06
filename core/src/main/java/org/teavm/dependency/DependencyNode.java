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

import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.ObjectObjectHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class DependencyNode implements ValueDependencyInfo {
    private static final int DEGREE_THRESHOLD = 2;
    DependencyAnalyzer dependencyAnalyzer;
    List<DependencyConsumer> followers;
    TypeSet typeSet;
    ObjectObjectHashMap<DependencyNode, Transition> transitions;
    ObjectArrayList<Transition> transitionList;
    String tag;
    private DependencyNode arrayItemNode;
    DependencyNode classValueNode;
    DependencyNode classNodeParent;
    boolean classNodeComplete;
    int degree;
    boolean locked;
    MethodReference method;
    ValueType typeFilter;
    private DependencyTypeFilter cachedTypeFilter;

    int splitCount;
    public int propagateCount;

    DependencyNode(DependencyAnalyzer dependencyAnalyzer, ValueType typeFilter) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.typeFilter = typeFilter;
    }

    public void propagate(DependencyType type) {
        if (degree > DEGREE_THRESHOLD) {
            return;
        }
        if (!hasType(type) && filter(type)) {
            propagateCount++;
            moveToSeparateDomain();
            typeSet.addType(type);
            scheduleSingleType(type, null);
        }
    }

    private void scheduleSingleType(DependencyType type, Runnable action) {
        if (DependencyAnalyzer.shouldLog) {
            for (DependencyNode node : typeSet.domain) {
                if (node.filter(type)) {
                    System.out.println(node.tag + " -> " + type.getName());
                }
            }
        }

        Transition[] transitions = typeSet.getTransitions().toArray(Transition.class);
        List<ConsumerWithNode> consumerEntries = typeSet.getConsumers();

        if (action != null) {
            action.run();
        }

        for (Transition transition : transitions) {
            if (transition.source.filter(type) && transition.filterType(type)) {
                dependencyAnalyzer.schedulePropagation(transition, type);
            }
        }
        for (ConsumerWithNode entry : consumerEntries) {
            if (entry.node.filter(type)) {
                for (DependencyConsumer consumer : entry.consumers) {
                    dependencyAnalyzer.schedulePropagation(consumer, type);
                }
            }
        }
    }

    public void propagate(DependencyType[] newTypes) {
        if (degree > DEGREE_THRESHOLD) {
            return;
        }

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

        propagateCount++;
        if (j < newTypes.length) {
            newTypes = Arrays.copyOf(newTypes, j);
        }

        moveToSeparateDomain();
        for (DependencyType newType : newTypes) {
            typeSet.addType(newType);
        }
        scheduleMultipleTypes(newTypes, null);
    }

    void scheduleMultipleTypes(DependencyType[] newTypes, Runnable action) {
        if (DependencyAnalyzer.shouldLog) {
            for (DependencyNode node : typeSet.domain) {
                for (DependencyType type : newTypes) {
                    if (node.filter(type)) {
                        System.out.println(node.tag + " -> " + type.getName());
                    }
                }
            }
        }

        ObjectArrayList<Transition> transitions = new ObjectArrayList<>(typeSet.getTransitions());
        List<ConsumerWithNode> consumerEntries = typeSet.getConsumers();

        if (action != null) {
            action.run();
        }

        for (ObjectCursor<Transition> cursor : transitions) {
            Transition transition = cursor.value;
            DependencyType[] typesToPropagate = newTypes;
            if (transition.source.typeFilter != null || transition.filter != null) {
                int j = 0;
                for (int i = 0; i < typesToPropagate.length; ++i) {
                    DependencyType type = typesToPropagate[i];
                    if (transition.source.filter(type) && transition.filterType(type)) {
                        typesToPropagate[j++] = type;
                    } else if (typesToPropagate == newTypes) {
                        typesToPropagate = typesToPropagate.clone();
                    }
                }
                if (j < typesToPropagate.length) {
                    if (j == 0) {
                        continue;
                    }
                    if (j == 1) {
                        dependencyAnalyzer.schedulePropagation(transition, typesToPropagate[0]);
                        continue;
                    }
                    typesToPropagate = Arrays.copyOf(typesToPropagate, j);
                }
            }
            dependencyAnalyzer.schedulePropagation(transition, typesToPropagate);
        }

        for (ConsumerWithNode entry : consumerEntries) {
            DependencyType[] filteredTypes = newTypes;
            DependencyNode node = entry.node;
            if (node.typeFilter != null) {
                int j = 0;
                for (int i = 0; i < filteredTypes.length; ++i) {
                    DependencyType type = filteredTypes[i];
                    if (node.filter(type)) {
                        filteredTypes[j++] = type;
                    } else {
                        if (filteredTypes == newTypes) {
                            filteredTypes = filteredTypes.clone();
                        }
                    }
                }
                if (j == 0) {
                    continue;
                }
                if (j < filteredTypes.length) {
                    filteredTypes = Arrays.copyOf(filteredTypes, j);
                }
            }
            for (DependencyConsumer consumer : entry.consumers) {
                dependencyAnalyzer.schedulePropagation(consumer, filteredTypes);
            }
        }
    }

    static class DeferredConsumerTypes {
        final DependencyConsumer consumer;
        final DependencyType[] types;

        DeferredConsumerTypes(DependencyConsumer consumer, DependencyType[] types) {
            this.consumer = consumer;
            this.types = types;
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
        if (followers == null) {
            followers = new ArrayList<>(1);
        }
        if (followers.contains(consumer)) {
            return;
        }
        followers.add(consumer);
        if (typeSet != null) {
            typeSet.consumers = null;
        }

        propagateTypes(consumer);
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

    boolean connectWithoutChildNodes(DependencyNode node, DependencyTypeFilter filter) {
        if (this == node) {
            return false;
        }
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null");
        }
        if (transitions == null) {
            transitions = new ObjectObjectHashMap<>();
            transitionList = new ObjectArrayList<>();
        }
        if (transitions.containsKey(node)) {
            return false;
        }

        Transition transition = new Transition(this, node, filter);
        transitions.put(node, transition);
        transitionList.add(transition);
        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Connecting " + tag + " to " + node.tag);
        }

        if (typeSet != null) {
            if (typeSet == node.typeSet) {
                return false;
            }
            if (typeSet.transitions != null) {
                typeSet.transitions.add(transition);
            }

            DependencyType[] types = node.typeSet == null && filter == null && node.typeFilter == null
                    ? getTypesInternal()
                    : getTypesInternal(filter, this, node);
            if (types.length > 0) {
                if (node.typeSet == null) {
                    node.propagate(types);
                } else {
                    dependencyAnalyzer.schedulePropagation(transition, types);
                }
            }
        }

        return true;
    }

    private void connectArrayItemNodes(DependencyNode node) {
        if (degree > DEGREE_THRESHOLD || node.degree > DEGREE_THRESHOLD) {
            return;
        }
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

        for (Transition transition : classNodeParent.transitionList.toArray(Transition.class)) {
            if (transition.destination.classNodeParent != null) {
                continue;
            }
            if (transition.destination.filter(dependencyAnalyzer.classType)
                    && (transition.filter == null || transition.filter.match(dependencyAnalyzer.classType))) {
                connect(transition.destination.getClassValueNode());
            }
        }
    }

    private void propagateTypes(DependencyConsumer transition) {
        if (typeSet != null) {
            dependencyAnalyzer.schedulePropagation(transition, getTypesInternal());
        }
    }

    public void connect(DependencyNode node) {
        connect(node, null);
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
            classValueNode.connectClassValueNodes();
        }
        return classValueNode;
    }

    @Override
    public boolean hasArrayType() {
        return arrayItemNode != null && arrayItemNode.typeSet != null && arrayItemNode.typeSet.hasAnyType();
    }

    public boolean hasType(DependencyType type) {
        return typeSet != null && typeSet.hasType(type);
    }

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyAnalyzer.getType(type));
    }

    @Override
    public String[] getTypes() {
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
        if (typeSet == null) {
            return false;
        }
        return typeSet.hasMoreTypesThan(limit, typeFilter != null ? getFilter()::match : null);
    }

    DependencyType[] getTypesInternal() {
        if (typeSet == null) {
            return new DependencyType[0];
        }
        DependencyType[] types = typeSet.getTypes();
        if (typeFilter == null) {
            return types;
        }
        DependencyType[] result = new DependencyType[types.length];
        int i = 0;
        for (DependencyType type : types) {
            if (filter(type)) {
                result[i++] = type;
            }
        }
        return i == result.length ? result : Arrays.copyOf(result, i);
    }

    DependencyType[] getTypesInternal(DependencyTypeFilter filter, DependencyNode sourceNode,
            DependencyNode targetNode) {
        if (typeSet == null) {
            return TypeSet.EMPTY_TYPES;
        }
        return typeSet.getTypesForNode(sourceNode, targetNode, filter);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    void moveToSeparateDomain() {
        if (typeSet == null) {
            Collection<DependencyNode> domain = findDomain();
            typeSet = new TypeSet(dependencyAnalyzer, this);
            typeSet.domain.addAll(domain);
            for (DependencyNode node : domain) {
                node.typeSet = typeSet;
            }
            return;
        }

        if (typeSet.origin == this) {
            return;
        }

        Collection<DependencyNode> domain = findDomain();
        if (domain.contains(typeSet.origin)) {
            return;
        }

        typeSet.domain.removeAll(domain);
        typeSet.invalidate();

        typeSet = typeSet.copy(this);
        typeSet.domain.addAll(domain);

        for (DependencyNode node : domain) {
            node.typeSet = typeSet;
            node.splitCount++;
        }
    }

    Collection<DependencyNode> findDomain() {
        if (!dependencyAnalyzer.domainOptimizationEnabled()) {
            return Collections.singleton(this);
        }

        Set<DependencyNode> visited = new LinkedHashSet<>(50);
        Deque<DependencyNode> stack = new ArrayDeque<>(50);
        stack.push(this);

        while (!stack.isEmpty()) {
            DependencyNode node = stack.pop();
            if (!visited.add(node)) {
                continue;
            }
            if (visited.size() > 100) {
                break;
            }

            if (node.transitions != null) {
                for (ObjectCursor<Transition> cursor : node.transitionList) {
                    Transition transition = cursor.value;
                    if (transition.filter == null && transition.destination.typeSet == typeSet
                            && !visited.contains(transition.destination) && transition.isDestSubsetOfSrc()) {
                        stack.push(transition.destination);
                    }
                }
            }
        }

        return visited;
    }
}
