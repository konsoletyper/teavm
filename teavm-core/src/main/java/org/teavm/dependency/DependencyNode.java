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
    private List<DependencyConsumer> followers;
    private BitSet types;
    private List<DependencyNodeToNodeTransition> transitions;
    private volatile String tag;
    private DependencyNode arrayItemNode;
    private int degree;
    int index;

    DependencyNode(DependencyChecker dependencyChecker, int index) {
        this(dependencyChecker, index, 0);
    }

    DependencyNode(DependencyChecker dependencyChecker, int index, int degree) {
        this.dependencyChecker = dependencyChecker;
        this.index = index;
        this.degree = degree;
    }

    public void propagate(DependencyType type) {
        if (type.getDependencyChecker() != dependencyChecker) {
            throw new IllegalArgumentException("The given type does not belong to the same dependency checker");
        }
        if (degree > 2) {
            return;
        }
        if (types == null) {
            types = new BitSet();
        }
        if (!types.get(type.index)) {
            types.set(type.index);
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + type.getName());
            }
            if (followers != null) {
                for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
                    dependencyChecker.schedulePropagation(consumer, type);
                }
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
            if (this.types == null || !this.types.get(type.index)) {
                types[j++] = type;
            }
        }
        if (this.types == null) {
            this.types = new BitSet();
        }
        for (int i = 0; i < j; ++i) {
            this.types.set(types[i].index);
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + types[i].getName());
            }
        }
        if (followers != null) {
            for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
                dependencyChecker.schedulePropagation(consumer, Arrays.copyOf(types, j));
            }
        }
    }

    public void addConsumer(DependencyConsumer consumer) {
        if (followers == null) {
            followers = new ArrayList<>();
        }
        if (followers.contains(consumer)) {
            return;
        }
        followers.add(consumer);
        if (this.types != null) {
            List<DependencyType> types = new ArrayList<>();
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                types.add(dependencyChecker.types.get(index));
            }
            dependencyChecker.schedulePropagation(consumer, types.toArray(new DependencyType[types.size()]));
        }
    }

    public void connect(DependencyNode node, DependencyTypeFilter filter) {
        if (this == node) {
            return;
        }
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null");
        }
        if (transitions != null) {
            for (DependencyNodeToNodeTransition transition : transitions) {
                if (transition.destination == node) {
                    return;
                }
            }
        }
        DependencyNodeToNodeTransition transition = new DependencyNodeToNodeTransition(this, node, filter);
        if (transitions == null) {
            transitions = new ArrayList<>();
        }

        transitions.add(transition);
        if (DependencyChecker.shouldLog) {
            System.out.println("Connecting " + tag + " to " + node.tag);
        }
        addConsumer(transition);
    }

    public void connect(DependencyNode node) {
        connect(node, null);
    }

    @Override
    public DependencyNode getArrayItem() {
        if (arrayItemNode == null) {
            arrayItemNode = new DependencyNode(dependencyChecker, dependencyChecker.nodes.size(), degree + 1);
            dependencyChecker.nodes.add(arrayItemNode);
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
        return arrayItemNode != null && arrayItemNode.types != null && !arrayItemNode.types.isEmpty();
    }

    public boolean hasType(DependencyType type) {
        return types != null && type.getDependencyChecker() == dependencyChecker && types.get(type.index);
    }

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyChecker.getType(type));
    }

    @Override
    public String[] getTypes() {
        if (types == null) {
            return new String[0];
        }
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
