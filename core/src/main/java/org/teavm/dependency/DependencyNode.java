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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class DependencyNode implements ValueDependencyInfo {
    private DependencyNode ref;
    int orderIndex;
    DependencyAnalyzer dependencyAnalyzer;
    private List<DependencyConsumer> consumers;

    private TypeSet actualTypes;
    private TypeSet pendingTypes;
    DependencyNode nextPendingTypes;
    boolean inPropagationList;

    private Map<DependencyNode, Transition> pendingTransitions;
    private Map<DependencyNode, Transition> transitions;
    private Map<DependencyNode, Transition> backTransitions;
    DependencyNode nextPendingTransitions;
    private boolean hasNewTransitions;

    private Map<ValueType, DependencyNode> inputNodes;
    private Map<ValueType, DependencyNode> outputNodes;

    String tag;
    DependencyNode arrayItemNode;
    private Set<DependencyNode> arrayNodes;
    DependencyNode classValueNode;
    Set<DependencyNode> classParentNodes;
    int degree;
    private boolean hub;
    private DependencyNode typeRepresentativeOf;
    boolean locked;
    MethodReference method;
    ValueType typeFilter;
    private DependencyTypeFilter cachedTypeFilter;
    private boolean hasArray;
    private boolean hasClass;

    int lowLink = -1;
    int index = -1;
    boolean onStack;

    DependencyNode(DependencyAnalyzer dependencyAnalyzer, ValueType typeFilter) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.typeFilter = typeFilter;
    }

    void assertThatNotAlias() {
        assert ref == null;
    }

    public void propagate(DependencyType type) {
        if (ref != null) {
            input().propagateImpl(type);
        } else {
            propagateImpl(type);
        }
    }

    private void propagateImpl(DependencyType type) {
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
            nextPendingTypes = dependencyAnalyzer.lastPendingTypes;
            dependencyAnalyzer.lastPendingTypes = this;
        } else {
            dependencyAnalyzer.hasPendingTypesInList = true;
        }
    }

    private void propagate(int[] newTypes) {
        if (newTypes.length == 0) {
            return;
        }
        if (newTypes.length == 1) {
            propagateImpl(dependencyAnalyzer.types.get(newTypes[0]));
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
            propagateImpl(dependencyAnalyzer.types.get(newTypes[0]));
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
                getClassValueNodeImpl().connectImpl(transition.destination.getClassValueNodeImpl());
            }
        }
        if (pendingTransitions != null) {
            for (var transition : pendingTransitions.values().toArray(Transition[]::new)) {
                getClassValueNodeImpl().connectImpl(transition.destination.getClassValueNodeImpl());
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
            propagateImpl(dependencyAnalyzer.types.get(newTypes.nextSetBit(0)));
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
        if (ref != null) {
            output().addConsumerImpl(consumer);
        } else {
            addConsumerImpl(consumer);
        }
    }

    private void addConsumerImpl(DependencyConsumer consumer) {
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
        if (ref != null) {
            output().connectImpl(node);
        } else {
            connectImpl(node);
        }
    }

    private void connectImpl(DependencyNode node) {
        node = node.input();
        if (connectWithoutChildNodes(node)) {
            if (hasArray) {
                connectArrayItemNodes(node);
            }
            if (hasClass) {
                getClassValueNode().connectImpl(node.getClassValueNode());
            }
        }
    }

    private boolean connectWithoutChildNodes(DependencyNode node) {
        if (this == node) {
            return false;
        }
        if (typeRepresentativeOf != null && typeRepresentativeOf == node.typeRepresentativeOf) {
            return false;
        }
        if (node.typeRepresentativeOf == this || typeRepresentativeOf == node) {
            return false;
        }
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null");
        }
        if (transitions != null && transitions.containsKey(node)) {
            return false;
        }

        if (pendingTransitions == null) {
            assertThatNotAlias();
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
                arrayItemNode.arrayNodes.add(node);
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(node.tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
            if (node.arrayItemNode != null && arrayItemNode == null) {
                arrayItemNode = node.arrayItemNode;
                arrayItemNode.arrayNodes.add(this);
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
            if (node.arrayItemNode == null && arrayItemNode == null) {
                node.arrayItemNode = getArrayItem();
                arrayItemNode.arrayNodes.add(node);
                if (DependencyAnalyzer.shouldLog) {
                    System.out.println(node.tag + " gets array item node " + arrayItemNode.tag);
                }
                return;
            }
        }
        getArrayItem().connectImpl(node.getArrayItem());
        node.getArrayItem().connectImpl(getArrayItem());
    }

    @Override
    public DependencyNode getArrayItem() {
        return ref != null ? output().getArrayItemImpl() : getArrayItemImpl();
    }

    private DependencyNode getArrayItemImpl() {
        if (arrayItemNode == null) {
            arrayItemNode = dependencyAnalyzer.createArrayItemNode(this);
            arrayItemNode.arrayNodes = new LinkedHashSet<>();
            arrayItemNode.arrayNodes.add(this);
        }
        return arrayItemNode.output();
    }

    @Override
    public DependencyNode getClassValueNode() {
        return ref != null ? output().getClassValueNodeImpl() : getClassValueNodeImpl();
    }

    private DependencyNode getClassValueNodeImpl() {
        if (classValueNode == null) {
            classValueNode = dependencyAnalyzer.createClassValueNode(degree, this);
            if (classValueNode.classParentNodes == null) {
                classValueNode.classParentNodes = new LinkedHashSet<>();
            }
            classValueNode.classParentNodes.add(this);
        }
        return classValueNode.resolve();
    }

    @Override
    public boolean hasArrayType() {
        return ref != null ? output().hasArrayTypeImpl() : hasArrayTypeImpl();
    }

    private boolean hasArrayTypeImpl() {
        return arrayItemNode != null && arrayItemNode.actualTypes != null && arrayItemNode.actualTypes.hasAnyType();
    }

    public boolean hasType(DependencyType type) {
        return ref != null ? output().hasType(type.index) : hasType(type.index);
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
        return ref != null ? output().getTypesImpl() : getTypesImpl();
    }

    private String[] getTypesImpl() {
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
        return ref != null ? output().hasMoreTypesThanImpl(limit) : hasMoreTypesThanImpl(limit);
    }

    private boolean hasMoreTypesThanImpl(int limit) {
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
            return this;
        }
        var result = ref.resolve();
        if (result != ref) {
            ref = result;
        }
        return result;
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
        for (var transition : pendingTransitions.values()) {
            if (transition.destination.backTransitions == null) {
                transition.destination.backTransitions = new LinkedHashMap<>();
            }
            transition.destination.backTransitions.put(this, transition);
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

    static void merge(DependencyAnalyzer analyzer, List<DependencyNode> nodes, Collection<Transition> transitions,
            Collection<DependencyNode> nodesToUpdate, Collection<DependencyNode> nodesToCleanup) {
        if (nodes.size() == 1) {
            return;
        }

        var resolvedNodes = new LinkedHashSet<DependencyNode>();
        DependencyNode result = null;
        int count = 0;
        for (var node : nodes) {
            node = node.resolve();
            if (node.hub && result == null) {
                result = node;
                ++count;
            } else if (node != result) {
                if (resolvedNodes.add(node)) {
                    ++count;
                }
                if (node.hub) {
                    if (node.inputNodes != null) {
                        resolvedNodes.addAll(node.inputNodes.values());
                    }
                    if (node.outputNodes != null) {
                        resolvedNodes.addAll(node.outputNodes.values());
                    }
                }
            }
        }
        if (count < 2) {
            return;
        }
        if (result != null) {
            if (result.inputNodes != null) {
                resolvedNodes.removeAll(result.inputNodes.values());
            }
            if (result.outputNodes != null) {
                resolvedNodes.removeAll(result.outputNodes.values());
            }
        }
        if (resolvedNodes.isEmpty()) {
            return;
        }
        nodes = new ArrayList<>(resolvedNodes);
        nodesToUpdate.addAll(nodes);
        nodesToCleanup.addAll(nodes);

        if (result == null) {
            result = analyzer.createNode();
            result.hub = true;
            if (DependencyAnalyzer.shouldTag) {
                var last = nodes.get(nodes.size() - 1);
                if (last.tag != null) {
                    result.tag = last.tag + "@hub";
                }
            }
        }
        nodesToUpdate.add(result);

        if (DependencyAnalyzer.shouldLog) {
            for (var node : nodes) {
                System.out.println("Merging " + node.tag + " into " + result.tag);
            }
        }

        for (var node : nodes) {
            node.ref = result;
        }

        result.addMergedTypes(nodes);
        result.sendMergedTypesToConsumers(nodes);
        result.addMergedConsumers(nodes);
        result.sendTypesToMergedConsumers(nodes);

        for (var node : nodes) {
            if (node.transitions != null) {
                transitions.addAll(node.transitions.values());
            }
            if (node.backTransitions != null) {
                transitions.addAll(node.backTransitions.values());
            }
        }
        result.establishNewArrayItemConnections(nodes);
        result.establishNewClassValueConnections(nodes);

        var arrayItemNodes = new LinkedHashSet<DependencyNode>();
        if (result.arrayItemNode != null) {
            arrayItemNodes.add(result.arrayItemNode.resolve());
        }
        for (var node : nodes) {
            if (node.arrayItemNode != null) {
                arrayItemNodes.add(node.arrayItemNode);
                nodesToUpdate.add(node.arrayItemNode);
            }
            if (node.arrayNodes != null) {
                nodesToUpdate.addAll(node.arrayNodes);
            }
        }
        if (arrayItemNodes.size() > 1) {
            merge(analyzer, new ArrayList<>(arrayItemNodes), transitions, nodesToUpdate, nodesToCleanup);
        }

        var classNodes = new LinkedHashSet<DependencyNode>();
        if (result.classValueNode != null) {
            classNodes.add(result.classValueNode.resolve());
        }
        for (var node : nodes) {
            if (node.classValueNode != null) {
                classNodes.add(node.classValueNode);
                nodesToUpdate.add(node.classValueNode);
            }
            if (node.classParentNodes != null) {
                nodesToUpdate.addAll(node.classParentNodes);
            }
        }
        if (classNodes.size() > 1) {
            merge(analyzer, new ArrayList<>(classNodes), transitions, nodesToUpdate, nodesToCleanup);
        }
        result.hasNewTransitions = true;
    }

    private void sendMergedTypesToConsumers(Collection<DependencyNode> nodes) {
        if (consumers == null) {
            return;
        }
        var consumers = new ArrayList<>(this.consumers);
        for (var node : nodes) {
            if (node.actualTypes != null) {
                if (node.actualTypes.data instanceof int[]) {
                    var types = (int[]) node.actualTypes.data;
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> {
                        for (var typeIndex : types) {
                            if (actualTypes != null && actualTypes.hasType(typeIndex)) {
                                continue;
                            }
                            for (var consumer : consumers) {
                                consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                            }
                        }
                    });
                } else if (this.actualTypes.data instanceof BitSet) {
                    var bitSet = (BitSet) node.actualTypes.data;
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> {
                        for (var typeIndex = bitSet.nextSetBit(0); typeIndex >= 0;
                             typeIndex = bitSet.nextSetBit(typeIndex + 1)) {
                            if (actualTypes != null && actualTypes.hasType(typeIndex)) {
                                continue;
                            }
                            for (var consumer : consumers) {
                                consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                            }
                        }
                    });
                }
            }
        }
    }

    private void addMergedTypes(Collection<DependencyNode> nodes) {
        for (var node : nodes) {
            if (node.actualTypes != null) {
                if (actualTypes == null) {
                    actualTypes = node.actualTypes;
                } else {
                    actualTypes.addTypes(node.actualTypes);
                }
            }
            if (node.pendingTypes != null) {
                if (pendingTypes == null) {
                    pendingTypes = node.pendingTypes;
                } else {
                    pendingTypes.addTypes(node.pendingTypes);
                }
                node.pendingTypes = null;
            }
        }
    }

    private void addMergedConsumers(Collection<DependencyNode> nodes) {
        for (var node : nodes) {
            if (node.consumers != null) {
                var target = node.outputNodeForType(node.typeFilter);
                if (target.consumers == null) {
                    target.consumers = node.consumers;
                } else {
                    target.consumers.addAll(node.consumers);
                }
            }
        }
    }

    private void sendTypesToMergedConsumers(Collection<DependencyNode> nodes) {
        if (actualTypes == null) {
            return;
        }
        for (var node : nodes) {
            if (node.consumers != null) {
                var actualTypes = node.actualTypes;
                var consumers = node.consumers;
                var filter = node.getFilter();
                if (this.actualTypes.data instanceof int[]) {
                    var types = (int[]) this.actualTypes.data;
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> {
                        for (var typeIndex : types) {
                            if (actualTypes != null && actualTypes.hasType(typeIndex)) {
                                continue;
                            }
                            if (!filter.match(dependencyAnalyzer.types.get(typeIndex))) {
                                continue;
                            }
                            for (var consumer : consumers) {
                                consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                            }
                        }
                    });
                } else if (this.actualTypes.data instanceof BitSet) {
                    var bitSet = (BitSet) this.actualTypes.data;
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> {
                        for (var typeIndex = bitSet.nextSetBit(0); typeIndex >= 0;
                             typeIndex = bitSet.nextSetBit(typeIndex + 1)) {
                            if (actualTypes != null && actualTypes.hasType(typeIndex)) {
                                continue;
                            }
                            if (!filter.match(dependencyAnalyzer.types.get(typeIndex))) {
                                continue;
                            }
                            for (var consumer : consumers) {
                                consumer.consume(dependencyAnalyzer.types.get(typeIndex));
                            }
                        }
                    });
                }
            }
            node.actualTypes = null;
            node.consumers = null;
        }
    }

    private void establishNewArrayItemConnections(Collection<DependencyNode> nodes) {
        var nodesHaveArray = nodes.stream().anyMatch(n -> n.hasArray);
        if (nodesHaveArray) {
            if (!hasArray) {
                hasArray = true;
                dependencyAnalyzer.tasksOnMergePhase.add(() -> resolve().propagateArray());
            }
            for (var node : nodes) {
                if (!node.hasArray) {
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> node.resolve().propagateArray());
                }
            }
        }
    }

    private void establishNewClassValueConnections(Collection<DependencyNode> nodes) {
        var nodesHaveClass = nodes.stream().anyMatch(n -> n.hasClass);
        if (nodesHaveClass) {
            if (!hasClass) {
                hasClass = true;
                dependencyAnalyzer.tasksOnMergePhase.add(() -> resolve().propagateClass());
            }
            for (var node : nodes) {
                if (!node.hasClass) {
                    dependencyAnalyzer.tasksOnMergePhase.add(() -> node.resolve().propagateClass());
                }
            }
        }
    }

    static void updateTransitions(Collection<Transition> transitions, Collection<DependencyNode> nodes) {
        for (var transition : transitions) {
            transition.source.transitions.remove(transition.destination, transition);
            transition.destination.backTransitions.remove(transition.source, transition);
        }

        for (var transition : transitions) {
            if (transition.source.resolve() == transition.destination.resolve()) {
                continue;
            }

            var newSource = transition.source.output();
            var newDestination = transition.destination.input();
            var newSourceOwner = newSource.typeRepresentativeOf != null
                    ? newSource.typeRepresentativeOf
                    : newSource;
            var newDestOwner = newDestination.typeRepresentativeOf != null
                    ? newDestination.typeRepresentativeOf
                    : newDestination;
            if (newSourceOwner == newDestOwner) {
                continue;
            }

            if (newSource != transition.source || newDestination != transition.destination) {
                transition.fresh = true;
            }
            transition.source = newSource;
            if (newSource.transitions == null) {
                newSource.transitions = new LinkedHashMap<>();
            }
            var existing = newSource.transitions.putIfAbsent(newDestination, transition);
            if (existing != null) {
                continue;
            }

            transition.destination = newDestination;
            if (newDestination.backTransitions == null) {
                newDestination.backTransitions = new LinkedHashMap<>();
            }
            newDestination.backTransitions.put(newSource, transition);
        }

        for (var node : nodes) {
            var resolvedNode = node.resolve();
            if (node.arrayItemNode != null) {
                var resolvedArrayItemNode = node.arrayItemNode.resolve();
                if (resolvedNode.arrayItemNode == null || resolvedNode.arrayItemNode.ref != null) {
                    resolvedNode.arrayItemNode = resolvedArrayItemNode;
                } else {
                    assert resolvedNode.arrayItemNode == resolvedArrayItemNode;
                }
            }
            if (node.classValueNode != null) {
                var resolvedClassValueNode = node.classValueNode.resolve();
                if (resolvedNode.classValueNode == null || resolvedNode.classValueNode.ref != null) {
                    resolvedNode.classValueNode = resolvedClassValueNode;
                } else {
                    assert resolvedNode.classValueNode == resolvedClassValueNode;
                }
            }
            if (node.arrayNodes != null) {
                var newArrayNodes = new LinkedHashSet<DependencyNode>();
                for (var arrayNode : node.arrayNodes) {
                    newArrayNodes.add(arrayNode.resolve());
                }
                if (resolvedNode.arrayNodes == null) {
                    resolvedNode.arrayNodes = new LinkedHashSet<>();
                }
                resolvedNode.arrayNodes.addAll(newArrayNodes);
            }
            if (node.classParentNodes != null) {
                var newClassParentNodes = new LinkedHashSet<DependencyNode>();
                for (var arrayNode : node.classParentNodes) {
                    newClassParentNodes.add(arrayNode.resolve());
                }
                if (resolvedNode.classParentNodes == null) {
                    resolvedNode.classParentNodes = new LinkedHashSet<>();
                }
                resolvedNode.classParentNodes.addAll(newClassParentNodes);
            }

            if (node.hub) {
                if (node.outputNodes != null) {
                    node.outputNodes.values().removeIf(n -> n.ref != null);
                }
                if (node.inputNodes != null) {
                    node.inputNodes.values().removeIf(n -> n.ref != null);
                }
            }
        }

        for (var node : nodes) {
            if (node.ref == null) {
                if (node.arrayNodes != null) {
                    node.arrayNodes.removeIf(n -> n.ref != null);
                }
                if (node.classParentNodes != null) {
                    node.classParentNodes.removeIf(n -> n.ref != null);
                }
                if (node.hub) {
                    if (node.transitions != null) {
                        for (var transition : node.transitions.values()) {
                            transition.fresh = true;
                        }
                    }
                    if (node.outputNodes != null) {
                        for (var outputNode : node.outputNodes.values()) {
                            outputNode.hasNewTransitions = true;
                            if (outputNode.transitions != null) {
                                for (var transition : outputNode.transitions.values()) {
                                    transition.fresh = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static void cleanup(Collection<DependencyNode> nodes) {
        for (var node : nodes) {
            if (node.ref != null) {
                node.arrayItemNode = null;
                node.arrayNodes = null;
                node.classValueNode = null;
                node.classParentNodes = null;
                node.actualTypes = null;
                node.transitions = null;
                node.backTransitions = null;
                node.consumers = null;
                node.typeRepresentativeOf = null;
            }
        }
    }

    private DependencyNode input() {
        if (ref == null) {
            return this;
        }
        return ref.resolve().inputNodeForType(typeFilter);
    }

    private DependencyNode output() {
        if (ref == null) {
            return this;
        }
        var result = ref.resolve().outputNodeForType(typeFilter);
        result.assertThatNotAlias();
        return result;
    }

    private DependencyNode outputNodeForType(ValueType type) {
        if (type == null) {
            return this;
        }
        if (outputNodes == null) {
            outputNodes = new LinkedHashMap<>();
        }
        var result = outputNodes.get(type);
        if (result == null) {
            result = dependencyAnalyzer.createNode(type);
            outputNodes.put(type, result);
            if (DependencyAnalyzer.shouldTag) {
                result.tag = tag + "@output:" + type;
            }
            var transition = new Transition(this, result);
            transition.fresh = true;
            if (transitions == null) {
                transitions = new LinkedHashMap<>();
            }
            transitions.put(result, transition);
            result.backTransitions = new LinkedHashMap<>();
            result.backTransitions.put(this, transition);
            result.typeRepresentativeOf = this;
        }
        return result;
    }

    private DependencyNode inputNodeForType(ValueType type) {
        if (type == null) {
            return this;
        }
        if (inputNodes == null) {
            inputNodes = new LinkedHashMap<>();
        }
        var result = inputNodes.get(type);
        if (result == null) {
            result = dependencyAnalyzer.createNode(type);
            inputNodes.put(type, result);
            if (DependencyAnalyzer.shouldTag && tag != null) {
                result.tag = tag + "@input:" + type;
            }
            var transition = new Transition(result, this);
            transition.fresh = true;
            if (backTransitions == null) {
                backTransitions = new LinkedHashMap<>();
            }
            backTransitions.put(result, transition);
            result.transitions = new LinkedHashMap<>();
            result.transitions.put(this, transition);
            result.typeRepresentativeOf = this;
        }
        return result;
    }

    Collection<? extends Transition> successors() {
        return transitions != null ? transitions.values() : null;
    }

    void pack() {
        consumers = null;
        transitions = null;
        backTransitions = null;
        method = null;
    }

    void verify() {
        if (ref != null) {
            if (transitions != null || backTransitions != null || pendingTypes != null || actualTypes != null) {
                fail();
            }
        } else {
            if (transitions != null) {
                for (var entry : transitions.entrySet()) {
                    var transition = entry.getValue();
                    if (transition.source == transition.destination) {
                        fail();
                    }
                    if (transition.destination != entry.getKey()) {
                        fail();
                    }
                    if (transition.source != this) {
                        fail();
                    }
                    if (transition.destination.ref != null) {
                        fail();
                    }
                    if (transition.destination.backTransitions == null) {
                        fail();
                    }
                    if (transition.destination.backTransitions.get(transition.source) != transition) {
                        fail();
                    }
                }
            }
            if (backTransitions != null) {
                for (var entry : backTransitions.entrySet()) {
                    var transition = entry.getValue();
                    if (transition.source == transition.destination) {
                        fail();
                    }
                    if (transition.source != entry.getKey()) {
                        fail();
                    }
                    if (transition.destination != this) {
                        fail();
                    }
                    if (transition.source.ref != null) {
                        fail();
                    }
                    if (transition.source.transitions == null) {
                        fail();
                    }
                    if (transition.source.transitions.get(transition.destination) != transition) {
                        fail();
                    }
                }
            }
            if (outputNodes != null) {
                for (var node : outputNodes.values()) {
                    if (node.ref != null) {
                        fail();
                    }
                }
            }
            if (inputNodes != null) {
                for (var node : inputNodes.values()) {
                    if (node.ref != null) {
                        fail();
                    }
                }
            }
            if (arrayItemNode != null) {
                if (arrayItemNode.ref != null) {
                    fail();
                }
                if (arrayItemNode.arrayNodes == null) {
                    fail();
                }
                if (!arrayItemNode.arrayNodes.contains(this)) {
                    fail();
                }
            }
            if (arrayNodes != null) {
                for (var arrayNode : arrayNodes) {
                    if (arrayNode.ref != null) {
                        fail();
                    }
                    if (arrayNode.arrayItemNode != this) {
                        fail();
                    }
                }
            }
            if (classValueNode != null) {
                if (classValueNode.ref != null) {
                    fail();
                }
                if (classValueNode.classParentNodes == null) {
                    fail();
                }
                if (!classValueNode.classParentNodes.contains(this)) {
                    fail();
                }
            }
            if (classParentNodes != null) {
                for (var clsNode : classParentNodes) {
                    if (clsNode.ref != null) {
                        fail();
                    }
                    if (clsNode.classValueNode != this) {
                        fail();
                    }
                }
            }
        }
    }

    private void fail() {
        throw new IllegalStateException();
    }
}
