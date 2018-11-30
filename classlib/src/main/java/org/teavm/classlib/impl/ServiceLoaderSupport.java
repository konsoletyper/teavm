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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ServiceLoaderSupport extends AbstractDependencyListener implements Generator {
    private static final MethodReference LOAD_METHOD = new MethodReference(ServiceLoader.class, "load", Class.class,
            ServiceLoader.class);
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
            for (int i = 0; i < implementations.size(); ++i) {
                if (i > 0) {
                    writer.append(", ");
                }
                String implName = implementations.get(i);
                if (context.getClassSource().getClassNames().contains(implName)) {
                    writer.append("[").appendClass(implName).append(", ").appendMethodBody(
                            new MethodReference(implName, new MethodDescriptor("<init>", ValueType.VOID)))
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

    private void parseServiceFile(DependencyAgent agent, DependencyNode targetNode, String service,
            InputStream input, CallLocation location) throws IOException {
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
            serviceMap.computeIfAbsent(service, k -> new ArrayList<>()).add(line);

            MethodReference ctor = new MethodReference(line, new MethodDescriptor("<init>", ValueType.VOID));
            agent.linkMethod(ctor).addLocation(location).use();
            targetNode.propagate(agent.getType(line));
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals("java.util.ServiceLoader") && ref.getName().equals("loadServices")) {
            method.getResult().propagate(agent.getType("[Ljava/lang/Object;"));
            DependencyNode sourceNode = agent.linkMethod(LOAD_METHOD).getVariable(1).getClassValueNode();
            sourceNode.connect(method.getResult().getArrayItem());
            sourceNode.addConsumer(type -> initConstructor(agent, method.getResult().getArrayItem(),
                    type.getName(), new CallLocation(LOAD_METHOD)));
        }
    }

    private void initConstructor(DependencyAgent agent, DependencyNode targetNode, String type,
            CallLocation location) {
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/services/" + type);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream stream = resource.openStream()) {
                    parseServiceFile(agent, targetNode, type, stream, location);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
