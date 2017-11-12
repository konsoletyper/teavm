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
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class DependencyNode implements ValueDependencyInfo {
    private static final int SMALL_TYPES_THRESHOLD = 6;
    private DependencyAnalyzer dependencyAnalyzer;
    private List<DependencyConsumer> followers;
    private int[] smallTypes;
    private BitSet types;
    private List<DependencyNodeToNodeTransition> transitions;
    private volatile String tag;
    private DependencyNode arrayItemNode;
    private DependencyNode classValueNode;
    private int degree;
    boolean locked;
    MethodReference method;
    private ValueType typeFilter;
    private SuperClassFilter cachedTypeFilter;

    DependencyNode(DependencyAnalyzer dependencyAnalyzer, ValueType typeFilter) {
        this(dependencyAnalyzer, typeFilter, 0);
    }

    private DependencyNode(DependencyAnalyzer dependencyAnalyzer, ValueType typeFilter, int degree) {
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.degree = degree;
        this.typeFilter = typeFilter;
    }

    private boolean addType(DependencyType type) {
        if (types == null) {
            if (smallTypes == null) {
                if (locked) {
                    throw new IllegalStateException("Error propagating type " + type.getName()
                            + " to node in " + method);
                }
                smallTypes = new int[] { type.index };
                return true;
            }
        }
        if (smallTypes != null) {
            for (int i = 0; i < smallTypes.length; ++i) {
                if (smallTypes[i] == type.index) {
                    return false;
                }
            }
            if (smallTypes.length == SMALL_TYPES_THRESHOLD) {
                types = new BitSet(dependencyAnalyzer.types.size() * 2);
                for (int existingType : smallTypes) {
                    types.set(existingType);
                }
                smallTypes = null;
            } else {
                if (locked) {
                    throw new IllegalStateException("Error propagating type " + type.getName() + " to node in method "
                            + method);
                }
                smallTypes = Arrays.copyOf(smallTypes, smallTypes.length + 1);
                smallTypes[smallTypes.length - 1] = type.index;
                return true;
            }
        }
        if (!types.get(type.index)) {
            if (locked) {
                throw new IllegalStateException("Error propagating type " + type.getName() + " to node " + tag);
            }
            types.set(type.index);
            return true;
        }
        return false;
    }

    public void propagate(DependencyType type) {
        if (degree > 2) {
            return;
        }
        if (addType(type) && filter(type)) {
            if (DependencyAnalyzer.shouldLog) {
                System.out.println(tag + " -> " + type.getName());
            }
            scheduleSingleType(type);
        }
    }

    private void scheduleSingleType(DependencyType type) {
        if (followers != null) {
            for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
                dependencyAnalyzer.schedulePropagation(consumer, type);
            }
        }
        if (transitions != null) {
            for (DependencyNodeToNodeTransition consumer : transitions.toArray(
                    new DependencyNodeToNodeTransition[transitions.size()])) {
                dependencyAnalyzer.schedulePropagation(consumer, type);
            }
        }
    }

    public void propagate(DependencyType[] newTypes) {
        if (degree > 2) {
            return;
        }

        int j = 0;
        boolean copied = false;
        for (int i = 0; i < newTypes.length; ++i) {
            DependencyType type = newTypes[i];
            if (addType(type) && filter(type)) {
                newTypes[j++] = type;
            } else if (!copied) {
                copied = true;
                newTypes = newTypes.clone();
            }
        }
        if (j == 0) {
            return;
        }
        if (DependencyAnalyzer.shouldLog) {
            for (int i = 0; i < j; ++i) {
                System.out.println(tag + " -> " + newTypes[i].getName());
            }
        }

        if (followers == null && transitions == null) {
            return;
        }
        if (j < newTypes.length) {
            if (j == 1) {
                scheduleSingleType(newTypes[0]);
                return;
            }
            newTypes = Arrays.copyOf(newTypes, j);
        }
        if (followers != null) {
            for (DependencyConsumer consumer : followers.toArray(new DependencyConsumer[followers.size()])) {
                dependencyAnalyzer.schedulePropagation(consumer, newTypes);
            }
        }
        if (transitions != null) {
            for (DependencyNodeToNodeTransition consumer : transitions.toArray(
                    new DependencyNodeToNodeTransition[transitions.size()])) {
                dependencyAnalyzer.schedulePropagation(consumer, newTypes);
            }
        }
    }

    private boolean filter(DependencyType type) {
        if (typeFilter == null) {
            return true;
        }

        if (cachedTypeFilter == null) {
            String superClass;
            if (typeFilter instanceof ValueType.Object) {
                superClass = ((ValueType.Object) typeFilter).getClassName();
            } else {
                superClass = "java.lang.Object";
            }
            cachedTypeFilter = dependencyAnalyzer.getSuperClassFilter(superClass);
        }

        return cachedTypeFilter.match(type);
    }

    public void addConsumer(DependencyConsumer consumer) {
        if (followers == null) {
            followers = new ArrayList<>(1);
        }
        if (followers.contains(consumer)) {
            return;
        }
        followers.add(consumer);

        propagateTypes(consumer);
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
            transitions = new ArrayList<>(1);
        }

        transitions.add(transition);
        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Connecting " + tag + " to " + node.tag);
        }

        propagateTypes(transition);
    }

    private void propagateTypes(DependencyConsumer transition) {
        if (this.types != null) {
            DependencyType[] types = new DependencyType[this.types.cardinality()];
            int j = 0;
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                types[j++] = type;
            }
            dependencyAnalyzer.schedulePropagation(transition, types);
        } else if (this.smallTypes != null) {
            DependencyType[] types = new DependencyType[smallTypes.length];
            for (int i = 0; i < types.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                types[i] = type;
            }
            dependencyAnalyzer.schedulePropagation(transition, types);
        }
    }

    private void propagateTypes(DependencyNodeToNodeTransition transition) {
        if (this.types != null) {
            DependencyType[] types = new DependencyType[this.types.cardinality()];
            int j = 0;
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                types[j++] = type;
            }
            dependencyAnalyzer.schedulePropagation(transition, types);
        } else if (this.smallTypes != null) {
            DependencyType[] types = new DependencyType[smallTypes.length];
            for (int i = 0; i < types.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                types[i] = type;
            }
            dependencyAnalyzer.schedulePropagation(transition, types);
        }
    }

    public void connect(DependencyNode node) {
        connect(node, null);
    }

    @Override
    public DependencyNode getArrayItem() {
        if (arrayItemNode == null) {
            ValueType itemTypeFilter = typeFilter instanceof ValueType.Array
                    ? ((ValueType.Array) typeFilter).getItemType()
                    : null;
            arrayItemNode = new DependencyNode(dependencyAnalyzer, itemTypeFilter, degree + 1);
            if (DependencyAnalyzer.shouldTag) {
                arrayItemNode.tag = tag + "[";
            }
        }
        return arrayItemNode;
    }

    @Override
    public DependencyNode getClassValueNode() {
        if (classValueNode == null) {
            classValueNode = new DependencyNode(dependencyAnalyzer, null, degree);
            classValueNode.classValueNode = classValueNode;
            if (DependencyAnalyzer.shouldTag) {
                classValueNode.tag = tag + "@";
            }
        }
        return classValueNode;
    }

    @Override
    public boolean hasArrayType() {
        return arrayItemNode != null && (arrayItemNode.types != null || arrayItemNode.smallTypes != null);
    }

    public boolean hasType(DependencyType type) {
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

    @Override
    public boolean hasType(String type) {
        return hasType(dependencyAnalyzer.getType(type));
    }

    @Override
    public String[] getTypes() {
        if (smallTypes != null) {
            String[] result = new String[smallTypes.length];
            int j = 0;
            for (int i = 0; i < result.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                if (filter(type)) {
                    result[j++] = type.getName();
                }
            }
            if (j < result.length) {
                result = Arrays.copyOf(result, j);
            }
            return result;
        }
        if (types == null) {
            return new String[0];
        }
        String[] result = new String[types.cardinality()];
        int j = 0;
        for (int index = types.nextSetBit(0); index >= 0; index = types.nextSetBit(index + 1)) {
            DependencyType type = dependencyAnalyzer.types.get(index);
            if (filter(type)) {
                result[j++] = type.getName();
            }
        }
        if (j < result.length) {
            result = Arrays.copyOf(result, j);
        }
        return result;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
