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

import java.util.*;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyNode implements ValueDependencyInfo {
    private DependencyChecker dependencyChecker;
    private Set<DependencyConsumer> followers = new HashSet<>();
    private BitSet types = new BitSet();
    private Map<DependencyNode, DependencyNodeToNodeTransition> transitions = new HashMap<>();
    private volatile String tag;
    private DependencyNode arrayItemNode;
    private int degree;

    DependencyNode(DependencyChecker dependencyChecker) {
        this(dependencyChecker, 0);
    }

    DependencyNode(DependencyChecker dependencyChecker, int degree) {
        this.dependencyChecker = dependencyChecker;
        this.degree = degree;
    }

    public void propagate(DependencyType type) {
        if (type.getDependencyChecker() != dependencyChecker) {
            throw new IllegalArgumentException("The given type does not belong to the same dependency checker");
        }
        if (degree > 2) {
            return;
        }
        if (!types.get(type.index)) {
            types.set(type.index);
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + type.getName());
            }
            for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
                dependencyChecker.schedulePropagation(consumer, type);
            }
        }
    }

    public void propagate(DependencyType[] agentTypes) {
        DependencyType[] types = new DependencyType[agentTypes.length];
        int j = 0;
        for (int i = 0; i < agentTypes.length; ++i) {
            DependencyType type = agentTypes[i];
            if (type.getDependencyChecker() != dependencyChecker) {
                throw new IllegalArgumentException("The given type does not belong to the same dependency checker");
            }
            if (!this.types.get(type.index)) {
                types[j++] = type;
            }
        }
        for (int i = 0; i < j; ++i) {
            this.types.set(types[i].index);
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + types[i].getName());
            }
        }
        for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
            dependencyChecker.schedulePropagation(consumer, Arrays.copyOf(types, j));
        }
    }

    public void addConsumer(DependencyConsumer consumer) {
        if (followers.add(consumer)) {
            List<DependencyType> types = new ArrayList<>();
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                types.add(dependencyChecker.types.get(index));
            }
            dependencyChecker.schedulePropagation(consumer, types.toArray(new DependencyType[types.size()]));
        }
    }

    public void connect(DependencyNode node, DependencyTypeFilter filter) {
        DependencyNodeToNodeTransition transition = new DependencyNodeToNodeTransition(this, node, filter);
        if (!transitions.containsKey(node)) {
            transitions.put(node, transition);
            if (DependencyChecker.shouldLog) {
                System.out.println("Connecting " + tag + " to " + node.tag);
            }
            addConsumer(transition);
        }
    }

    public void connect(DependencyNode node) {
        connect(node, null);
    }

    @Override
    public DependencyNode getArrayItem() {
        if (arrayItemNode == null) {
            arrayItemNode = new DependencyNode(dependencyChecker, degree + 1);
            if (DependencyChecker.shouldLog) {
                arrayItemNode.tag = tag + "[";
            }
            arrayItemNode.addConsumer(new DependencyConsumer() {
                @Override public void consume(DependencyType type) {
                    DependencyNode.this.propagate(type);
                }
            });
        }
        return arrayItemNode;
    }

    @Override
    public boolean hasArrayType() {
        return arrayItemNode != null && arrayItemNode.types.isEmpty();
    }

    public boolean hasType(DependencyType type) {
        return type.getDependencyChecker() == dependencyChecker && types.get(type.index);
    }

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyChecker.getType(type));
    }

    @Override
    public String[] getTypes() {
        List<String> result = new ArrayList<>();
        for (int index = types.nextSetBit(0); index >= 0; index = types.nextSetBit(index + 1)) {
            result.add(dependencyChecker.types.get(index).getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
