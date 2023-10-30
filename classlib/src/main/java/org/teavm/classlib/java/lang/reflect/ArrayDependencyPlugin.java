/*
 *  Copyright 2023 Alexey Andreev.
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

import java.util.HashSet;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ArrayDependencyPlugin implements DependencyPlugin {
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

    private void reachGetLength(DependencyAgent agent, MethodDependency method) {
        method.getVariable(1).addConsumer(type -> {
            if (!type.getName().startsWith("[")) {
                MethodReference cons = new MethodReference(IllegalArgumentException.class, "<init>", void.class);
                agent.linkMethod(cons).use();
            }
        });
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
