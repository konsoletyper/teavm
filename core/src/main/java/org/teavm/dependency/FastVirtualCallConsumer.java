/*
 *  Copyright 2018 Alexey Andreev.
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

class FastVirtualCallConsumer implements DependencyConsumer {
    private final DependencyNode node;
    private final MethodDescriptor methodDesc;
    private final DependencyAnalyzer analyzer;
    private final Map<MethodReference, CallLocation> callLocations = new LinkedHashMap<>();
    private final Set<MethodDependency> methods = new LinkedHashSet<>(100, 0.5f);

    FastVirtualCallConsumer(DependencyNode node, MethodDescriptor methodDesc, DependencyAnalyzer analyzer) {
        this.node = node;
        this.methodDesc = methodDesc;
        this.analyzer = analyzer;
    }

    @Override
    public void consume(DependencyType type) {
        String className = type.getName();
        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". "
                    + "Target class is " + className);
        }
        if (className.startsWith("[")) {
            className = "java.lang.Object";
            type = analyzer.getType(className);
        }

        MethodDependency methodDep = analyzer.linkMethod(className, methodDesc);
        if (!methods.add(methodDep)) {
            return;
        }

        for (CallLocation location : callLocations.values()) {
            methodDep.addLocation(location);
        }

        if (!methodDep.isMissing()) {
            methodDep.use();
        }
    }

    void addLocation(CallLocation location) {
        if (callLocations.putIfAbsent(location.getMethod(), location) == null) {
            for (MethodDependency method : methods) {
                method.addLocation(location);
            }
        }
    }
}
