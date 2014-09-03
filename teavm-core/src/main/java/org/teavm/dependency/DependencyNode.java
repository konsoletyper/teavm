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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.common.IntegerArray;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyNode implements ValueDependencyInfo {
    private DependencyChecker dependencyChecker;
    private Set<DependencyConsumer> followers = new HashSet<>();
    private BitSet types = new BitSet();
    private ConcurrentMap<DependencyNode, DependencyNodeToNodeTransition> transitions = new ConcurrentHashMap<>();
    private volatile String tag;
    private final AtomicReference<DependencyNode> arrayItemNode = new AtomicReference<>();
    private volatile CountDownLatch arrayItemNodeLatch = new CountDownLatch(1);
    private int degree;

    DependencyNode(DependencyChecker dependencyChecker) {
        this(dependencyChecker, 0);
    }

    DependencyNode(DependencyChecker dependencyChecker, int degree) {
        this.dependencyChecker = dependencyChecker;
        this.degree = degree;
    }

    public void propagate(DependencyAgentType agentType) {
        if (!(agentType instanceof DependencyType)) {
            throw new IllegalArgumentException("The given type does not belong to the same dependency checker");
        }
        DependencyType type = (DependencyType)agentType;
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

    public void addConsumer(DependencyConsumer consumer) {
        if (followers.add(consumer)) {
            IntegerArray indexes = new IntegerArray(8);
            for (int index = types.nextSetBit(0); index >= 0; index = types.nextSetBit(index + 1)) {
                indexes.add(index);
            }
            for (int index : indexes.getAll()) {
                dependencyChecker.schedulePropagation(consumer, dependencyChecker.types.get(index));
            }
        }
    }

    public void connect(DependencyNode node, DependencyTypeFilter filter) {
        DependencyNodeToNodeTransition transition = new DependencyNodeToNodeTransition(this, node, filter);
        if (transitions.putIfAbsent(node, transition) == null) {
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
        DependencyNode result = arrayItemNode.get();
        if (result == null) {
            result = new DependencyNode(dependencyChecker, degree + 1);
            if (arrayItemNode.compareAndSet(null, result)) {
                if (DependencyChecker.shouldLog) {
                    arrayItemNode.get().tag = tag + "[";
                }
                arrayItemNodeLatch.countDown();
                arrayItemNodeLatch = null;
            } else {
                result = arrayItemNode.get();
            }
        }
        CountDownLatch latch = arrayItemNodeLatch;
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return result;
            }
        }
        return result;
    }

    @Override
    public boolean hasArrayType() {
        return arrayItemNode.get() != null && !arrayItemNode.get().types.isEmpty();
    }

    public boolean hasType(DependencyAgentType type) {
        if (!(type instanceof DependencyType)) {
            return false;
        }
        DependencyType typeImpl = (DependencyType)type;
        return typeImpl.getDependencyChecker() == dependencyChecker && types.get(typeImpl.index);
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
