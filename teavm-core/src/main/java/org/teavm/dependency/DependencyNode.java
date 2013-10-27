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

/**
 *
 * @author Alexey Andreev
 */
public class DependencyNode implements DependencyConsumer {
    private DependencyChecker dependencyChecker;
    private static final Object mapValue = new Object();
    private ConcurrentMap<DependencyConsumer, Object> followers = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> types = new ConcurrentHashMap<>();
    private volatile String tag;

    DependencyNode(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    @Override
    public void propagate(String type) {
        if (types.putIfAbsent(type, mapValue) == null) {
            if (DependencyChecker.shouldLog) {
                System.out.println(tag + " -> " + type);
            }
            for (DependencyConsumer follower : followers.keySet().toArray(new DependencyConsumer[0])) {
                if (follower.hasType(type)) {
                    dependencyChecker.schedulePropagation(follower, type);
                }
            }
        }
    }

    public void connect(DependencyConsumer follower) {
        if (followers.putIfAbsent(follower, mapValue) == null) {
            for (String type : types.keySet().toArray(new String[0])) {
                if (follower.hasType(type)) {
                    dependencyChecker.schedulePropagation(follower, type);
                }
            }
        }
    }

    @Override
    public boolean hasType(String type) {
        return types.containsKey(type);
    }

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
