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
import java.util.*;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.*;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ServiceLoaderSupport implements Generator, DependencyListener {
    private Map<String, List<String>> serviceMap = new HashMap<>();
    private DependencyNode allClassesNode;
    private ClassLoader classLoader;
    private DependencyStack stack;

    public ServiceLoaderSupport(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        writer.append("if (!").appendClass("java.util.ServiceLoader").append(".$$services$$) {").indent()
                .softNewLine();
        writer.appendClass("java.util.ServiceLoader").append("$$services$$ = true;").softNewLine();
        for (Map.Entry<String, List<String>> entry : serviceMap.entrySet()) {
            writer.appendClass(entry.getKey()).append(".$$serviceList$$ = [");
            List<String> implementations = entry.getValue();
            for (int i = 0; i < implementations.size(); ++i) {
                if (i > 0) {
                    writer.append(", ");
                }
                String implName = implementations.get(i);
                writer.append("[").appendClass(implName).append(", ").appendMethodBody(
                        new MethodReference(implName, new MethodDescriptor("<init>", ValueType.VOID)))
                        .append("]");
            }
            writer.append("];").softNewLine();
        }
        writer.outdent().append("}").softNewLine();
        String param = context.getParameterName(1);
        writer.append("var cls = " + param + ".$data;").softNewLine();
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
    public void started(DependencyAgent agent) {
        allClassesNode = agent.createNode();
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className) {
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/services/" + className);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream stream = resource.openStream()) {
                    parseServiceFile(className, stream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseServiceFile(String service, InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            List<String> implementors = serviceMap.get(service);
            if (implementors == null) {
                implementors = new ArrayList<>();
                serviceMap.put(service, implementors);
            }
            implementors.add(line);
            allClassesNode.propagate(line);
        }
    }

    @Override
    public void methodAchieved(final DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals("java.util.ServiceLoader") && ref.getName().equals("loadServices")) {
            method.getResult().propagate("[java.lang.Object");
            stack = method.getStack();
            allClassesNode.connect(method.getResult().getArrayItem());
            method.getResult().getArrayItem().addConsumer(new DependencyConsumer() {
                @Override public void consume(String type) {
                    initConstructor(agent, type);
                }
            });
        }
    }

    private void initConstructor(DependencyAgent agent, String type) {
        MethodReference ctor = new MethodReference(type, new MethodDescriptor("<init>", ValueType.VOID));
        agent.linkMethod(ctor, stack).use();
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field) {
    }
}
