/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.teavm.classlib.ServiceLoaderFilter;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.parsing.resource.ResourceProvider;

public class ServiceLoaderSupport extends AbstractDependencyListener implements ServiceLoaderInformation {
    private static final MethodReference LOAD_METHOD = new MethodReference(ServiceLoader.class, "load", Class.class,
            ServiceLoader.class);
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", void.class);
    private Map<String, List<String>> serviceMap = new HashMap<>();
    private ClassLoader classLoader;
    private ResourceProvider resourceProvider;

    public ServiceLoaderSupport(ClassLoader classLoader, ResourceProvider resourceProvider) {
        this.classLoader = classLoader;
        this.resourceProvider = resourceProvider;
    }

    @Override
    public Collection<? extends String> serviceTypes() {
        return serviceMap.keySet();
    }

    @Override
    public Collection<? extends String> serviceImplementations(String type) {
        Collection<? extends String> result = serviceMap.get(type);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals("java.util.ServiceLoader") && ref.getName().equals("loadServices")) {
            List<ServiceLoaderFilter> filters = getFilters(agent);
            method.getResult().propagate(agent.getType(ValueType.arrayOf(ValueType.object("java.lang.Object"))));
            DependencyNode sourceNode = agent.linkMethod(LOAD_METHOD).getVariable(1).getClassValueNode();
            sourceNode.connect(method.getResult().getArrayItem());
            sourceNode.addConsumer(type -> {
                if (!(type.getValueType() instanceof ValueType.Object)) {
                    return;
                }
                var className = ((ValueType.Object) type.getValueType()).getClassName();
                CallLocation location = new CallLocation(LOAD_METHOD);
                for (String implementationType : getImplementations(className)) {
                    if (filters.stream().anyMatch(filter -> !filter.apply(className, implementationType))) {
                        continue;
                    }
                    serviceMap.computeIfAbsent(className, k -> new ArrayList<>()).add(implementationType);

                    MethodReference ctor = new MethodReference(implementationType, INIT_METHOD);
                    var depType = agent.getType(ValueType.object(implementationType));
                    agent.linkMethod(ctor).propagate(0, depType).addLocation(location).use();
                    method.getResult().getArrayItem().propagate(agent.getType(ValueType.object(implementationType)));
                }
            });
        }
    }

    private Set<String> getImplementations(String type) {
        Set<String> result = new LinkedHashSet<>();
        try {
            var resources = resourceProvider.getResources("META-INF/services/" + type);
            while (resources.hasNext()) {
                var resource = resources.next();
                try (InputStream stream = resource.open()) {
                    parseServiceFile(stream, result);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private void parseServiceFile(InputStream input, Set<String> consumer) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            consumer.add(line);
        }
    }

    private List<ServiceLoaderFilter> getFilters(DependencyAgent agent) {
        List<ServiceLoaderFilter> filters = new ArrayList<>();
        for (String filterTypeName : getImplementations(ServiceLoaderFilter.class.getName())) {
            Class<?> filterType;
            try {
                filterType = Class.forName(filterTypeName, true, classLoader);
            } catch (ClassNotFoundException e) {
                agent.getDiagnostics().error(null, "Could not load ServiceLoader filter class '{{c0}}'",
                        filterTypeName);
                continue;
            }

            if (!ServiceLoaderFilter.class.isAssignableFrom(filterType)) {
                agent.getDiagnostics().error(null, "Class '{{c0}}' does not implement ServiceLoaderFilter interface",
                        filterTypeName);
                continue;
            }

            try {
                filters.add((ServiceLoaderFilter) filterType.getConstructor().newInstance());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                    | InstantiationException e) {
                agent.getDiagnostics().error(null, "Could not instantiate ServiceLoader filter '{{c0}}'",
                        filterTypeName);
            }
        }
        return filters;
    }
}
