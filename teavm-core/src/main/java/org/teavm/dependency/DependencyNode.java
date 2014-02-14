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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyNode implements DependencyValueInformation {
    private DependencyChecker dependencyChecker;
    private static final Object mapValue = new Object();
    private ConcurrentMap<DependencyConsumer, Object> followers = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> types = new ConcurrentHashMap<>();
    private ConcurrentMap<DependencyNode, DependencyNodeToNodeTransition> transitions = new ConcurrentHashMap<>();
    private volatile String tag;
    private final AtomicReference<DependencyNode> arrayItemNode = new AtomicReference<>();
    private volatile CountDownLatch arrayItemNodeLatch = new CountDownLatch(1);

    DependencyNode(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    public void propagate(String type) {
        if (types.putIfAbsent(type, mapValue) == null) {
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + type);
            }
            for (DependencyConsumer consumer : followers.keySet().toArray(new DependencyConsumer[0])) {
                dependencyChecker.schedulePropagation(consumer, type);
            }
        }
    }

    public void addConsumer(DependencyConsumer consumer) {
        if (followers.putIfAbsent(consumer, mapValue) == null) {
            for (String type : types.keySet().toArray(new String[0])) {
                dependencyChecker.schedulePropagation(consumer, type);
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
            result = new DependencyNode(dependencyChecker);
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

    @Override
    public boolean hasType(String type) {
        return types.containsKey(type);
    }

    @Override
    public String[] getTypes() {
        return types != null ? types.keySet().toArray(new String[types.size()]) : new String[0];
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
