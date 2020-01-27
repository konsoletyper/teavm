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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.classlib.ServiceLoaderFilter;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class ServiceLoaderSupport extends AbstractDependencyListener implements Generator {
    private static final MethodReference LOAD_METHOD = new MethodReference(ServiceLoader.class, "load", Class.class,
            ServiceLoader.class);
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", void.class);
    private Map<String, List<String>> serviceMap = new HashMap<>();
    private ClassLoader classLoader;

    public ServiceLoaderSupport(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        writer.append("if (!").appendClass("java.util.ServiceLoader").append(".$$services$$) {").indent()
                .softNewLine();
        writer.appendClass("java.util.ServiceLoader").append(".$$services$$ = true;").softNewLine();
        for (Map.Entry<String, List<String>> entry : serviceMap.entrySet()) {
            writer.appendClass(entry.getKey()).append(".$$serviceList$$ = [");
            List<String> implementations = entry.getValue();
            boolean first = true;
            for (String implName : implementations) {
                if (context.getClassSource().getClassNames().contains(implName)) {
                    if (!first) {
                        writer.append(", ");
                    }
                    first = false;
                    writer.append("[").appendClass(implName).append(", ").appendMethodBody(
                            new MethodReference(implName, INIT_METHOD))
                            .append("]");
                }
            }
            writer.append("];").softNewLine();
        }
        writer.outdent().append("}").softNewLine();
        String param = context.getParameterName(1);
        writer.append("var cls = " + param + ";").softNewLine();
        writer.append("if (!cls.$$serviceList$$) {").indent().softNewLine();
        writer.append("return $rt_createArray($rt_objcls(), 0);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var result = $rt_createArray($rt_objcls(), cls.$$serviceList$$.length);").softNewLine();
        writer.append("for (var i = 0; i < result.data.length; ++i) {").indent().softNewLine();
        writer.append("var serviceDesc = cls.$$serviceList$$[i];").softNewLine();
        writer.append("result.data[i] = new serviceDesc[0]();").softNewLine();
        writer.append("serviceDesc[1](result.data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals("java.util.ServiceLoader") && ref.getName().equals("loadServices")) {
            List<ServiceLoaderFilter> filters = getFilters(agent);
            method.getResult().propagate(agent.getType("[Ljava/lang/Object;"));
            DependencyNode sourceNode = agent.linkMethod(LOAD_METHOD).getVariable(1).getClassValueNode();
            sourceNode.connect(method.getResult().getArrayItem());
            sourceNode.addConsumer(type -> {
                CallLocation location = new CallLocation(LOAD_METHOD);
                for (String implementationType : getImplementations(type.getName())) {
                    if (filters.stream().anyMatch(filter -> !filter.apply(type.getName(), implementationType))) {
                        continue;
                    }
                    serviceMap.computeIfAbsent(type.getName(), k -> new ArrayList<>()).add(implementationType);

                    MethodReference ctor = new MethodReference(implementationType, INIT_METHOD);
                    agent.linkMethod(ctor).addLocation(location).use();
                    method.getResult().getArrayItem().propagate(agent.getType(implementationType));
                }
            });
        }
    }

    private Set<String> getImplementations(String type) {
        Set<String> result = new LinkedHashSet<>();
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/services/" + type);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream stream = resource.openStream()) {
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
