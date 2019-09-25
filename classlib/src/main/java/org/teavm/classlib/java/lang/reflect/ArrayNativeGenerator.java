/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ArrayNativeGenerator implements Generator, DependencyPlugin {
    private static final String[] primitives = { "Byte", "Short", "Char", "Int", "Long", "Float", "Double",
            "Boolean" };
    private static final String[] primitiveWrappers = { "Byte", "Short", "Character", "Integer", "Long",
            "Float", "Double", "Boolean" };
    private static final ValueType[] primitiveTypes = { ValueType.BYTE, ValueType.SHORT, ValueType.CHARACTER,
            ValueType.INTEGER, ValueType.LONG, ValueType.FLOAT, ValueType.DOUBLE, ValueType.BOOLEAN };

    private Set<MethodReference> reachedMethods = new HashSet<>();

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (!reachedMethods.add(method.getReference())) {
            return;
        }
        switch (method.getReference().getName()) {
            case "getLength":
                reachGetLength(agent, method);
                break;
            case "newInstance":
                method.getVariable(1).getClassValueNode().addConsumer(t -> {
                    String arrayTypeName;
                    if (t.getName().startsWith("[")) {
                        arrayTypeName = t.getName();
                    } else if (t.getName().startsWith("~")) {
                        arrayTypeName = t.getName().substring(1);
                    } else {
                        arrayTypeName = ValueType.object(t.getName()).toString();
                    }
                    if (!arrayTypeName.startsWith("[[[")) {
                        method.getResult().propagate(agent.getType("[" + arrayTypeName));
                    }
                });
                break;
            case "getImpl":
                reachGet(agent, method);
                break;
            case "setImpl":
                reachSet(agent, method);
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getLength":
                generateGetLength(context, writer);
                break;
            case "newInstanceImpl":
                generateNewInstance(context, writer);
                break;
            case "getImpl":
                generateGet(context, writer);
                break;
            case "setImpl":
                generateSet(context, writer);
                break;
        }
    }

    private void generateGetLength(GeneratorContext context, SourceWriter writer) throws IOException {
        String array = context.getParameterName(1);
        writer.append("if (" + array + " === null || " + array + ".constructor.$meta.item === undefined) {")
                .softNewLine().indent();
        MethodReference cons = new MethodReference("java.lang.IllegalArgumentException", "<init>", ValueType.VOID);
        writer.append("$rt_throw(").appendInit(cons).append("());").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return " + array + ".data.length;").softNewLine();
    }

    private void reachGetLength(DependencyAgent agent, MethodDependency method) {
        method.getVariable(1).addConsumer(type -> {
            if (!type.getName().startsWith("[")) {
                MethodReference cons = new MethodReference(IllegalArgumentException.class, "<init>", void.class);
                agent.linkMethod(cons).use();
            }
        });
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer) throws IOException {
        String type = context.getParameterName(1);
        String length = context.getParameterName(2);
        writer.append("if (").append(type).append(".$meta.primitive) {").softNewLine().indent();
        for (String primitive : primitives) {
            writer.append("if (" + type + " == $rt_" + primitive.toLowerCase() + "cls()) {").indent().softNewLine();
            writer.append("return $rt_create" + primitive + "Array(" + length + ");").softNewLine();
            writer.outdent().append("}").softNewLine();
        }
        writer.outdent().append("} else {").indent().softNewLine();
        writer.append("return $rt_createArray(" + type + ", " + length + ")").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateGet(GeneratorContext context, SourceWriter writer) throws IOException {
        String array = context.getParameterName(1);
        writer.append("var item = " + array + ".data[" + context.getParameterName(2) + "];").softNewLine();
        writer.append("var type = " + array + ".constructor.$meta.item;").softNewLine();
        for (int i = 0; i < primitives.length; ++i) {
            String wrapper = "java.lang." + primitiveWrappers[i];
            MethodReference methodRef = new MethodReference(wrapper, "valueOf",
                    primitiveTypes[i], ValueType.object(wrapper));
            ClassReader cls = context.getClassSource().get(methodRef.getClassName());
            if (cls == null || cls.getMethod(methodRef.getDescriptor()) == null) {
                continue;
            }
            writer.append("if (type === $rt_" + primitives[i].toLowerCase() + "cls()) {").indent().softNewLine();
            writer.append("return ").appendMethodBody(methodRef).append("(item);").softNewLine();
            writer.outdent().append("} else ");
        }
        writer.append("{").indent().softNewLine();
        writer.append("return item;").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateSet(GeneratorContext context, SourceWriter writer) throws IOException {
        String array = context.getParameterName(1);
        String item = context.getParameterName(3);
        writer.append("var type = " + array + ".constructor.$meta.item;").softNewLine();
        boolean first = true;
        for (int i = 0; i < primitives.length; ++i) {
            String wrapper = "java.lang." + primitiveWrappers[i];
            MethodReference methodRef = new MethodReference(wrapper, primitives[i].toLowerCase() + "Value",
                    primitiveTypes[i]);
            ClassReader cls = context.getClassSource().get(methodRef.getClassName());
            if (cls == null || cls.getMethod(methodRef.getDescriptor()) == null) {
                continue;
            }
            if (!first) {
                writer.append(" else ");
            }
            first = false;
            writer.append("if (type === $rt_" + primitives[i].toLowerCase() + "cls()) {").indent().softNewLine();
            writer.append(item + " = ").appendMethodBody(methodRef).append("(" + item + ");").softNewLine();
            writer.outdent().append("}");
        }
        writer.softNewLine();
        writer.append(array + ".data[" + context.getParameterName(2) + "] = " + item + ";").softNewLine();
    }

    private void reachGet(DependencyAgent agent, MethodDependency method) {
        method.getVariable(1).getArrayItem().connect(method.getResult());
        method.getVariable(1).addConsumer(type -> {
            if (type.getName().startsWith("[")) {
                String typeName = type.getName().substring(1);
                for (int i = 0; i < primitiveTypes.length; ++i) {
                    if (primitiveTypes[i].toString().equals(typeName)) {
                        String wrapper = "java.lang." + primitiveWrappers[i];
                        MethodReference methodRef = new MethodReference(wrapper, "valueOf",
                                primitiveTypes[i], ValueType.object(wrapper));
                        agent.linkMethod(methodRef).use();
                        method.getResult().propagate(agent.getType("java.lang." + primitiveWrappers[i]));
                    }
                }
            }
        });
    }

    private void reachSet(DependencyAgent agent, MethodDependency method) {
        method.getVariable(3).connect(method.getVariable(1).getArrayItem());
        method.getVariable(1).addConsumer(type -> {
            if (type.getName().startsWith("[")) {
                String typeName = type.getName().substring(1);
                for (int i = 0; i < primitiveTypes.length; ++i) {
                    if (primitiveTypes[i].toString().equals(typeName)) {
                        String wrapper = "java.lang." + primitiveWrappers[i];
                        MethodReference methodRef = new MethodReference(wrapper,
                                primitives[i].toLowerCase() + "Value", primitiveTypes[i]);
                        agent.linkMethod(methodRef).use();
                    }
                }
            }
        });
    }
}
