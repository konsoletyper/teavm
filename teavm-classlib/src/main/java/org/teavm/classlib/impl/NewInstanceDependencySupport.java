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

import org.teavm.dependency.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class NewInstanceDependencySupport implements DependencyListener {
    private DependencyNode allClassesNode;
    private DependencyStack newInstanceStack;

    @Override
    public void started(DependencyAgent agent) {
        allClassesNode = agent.createNode();
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls == null) {
            return;
        }
        if (cls.hasModifier(ElementModifier.ABSTRACT) || cls.hasModifier(ElementModifier.INTERFACE)) {
            return;
        }
        MethodReader method = cls.getMethod(new MethodDescriptor("<init>", ValueType.VOID));
        if (method != null) {
            allClassesNode.propagate(agent.getType(className));
        }
    }

    @Override
    public void methodAchieved(final DependencyAgent agent, MethodDependency method) {
        MethodReader reader = method.getMethod();
        if (reader.getOwnerName().equals("java.lang.Class") && reader.getName().equals("newInstance")) {
            newInstanceStack = method.getStack();
            allClassesNode.connect(method.getResult());
            method.getResult().addConsumer(new DependencyConsumer() {
                @Override public void consume(DependencyAgentType type) {
                    attachConstructor(agent, type.getName());
                }
            });
        }
    }

    private void attachConstructor(DependencyAgent checker, String type) {
        MethodReference ref = new MethodReference(type, new MethodDescriptor("<init>", ValueType.VOID));
        checker.linkMethod(ref, newInstanceStack).use();
    }

    @Override
    public void fieldAchieved(DependencyAgent dependencyAgent, FieldDependency field) {
    }
}
