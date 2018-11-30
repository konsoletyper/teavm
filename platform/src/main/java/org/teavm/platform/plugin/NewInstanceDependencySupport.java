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
package org.teavm.platform.plugin;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.Platform;

public class NewInstanceDependencySupport extends AbstractDependencyListener {
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", void.class);
    private DependencyNode allClassesNode;

    @Override
    public void started(DependencyAgent agent) {
        allClassesNode = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls == null) {
            return;
        }
        if (cls.hasModifier(ElementModifier.ABSTRACT) || cls.hasModifier(ElementModifier.INTERFACE)) {
            return;
        }
        MethodReader method = cls.getMethod(INIT_METHOD);
        if (method != null) {
            allClassesNode.propagate(agent.getType(className));
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReader reader = method.getMethod();
        if (reader.getOwnerName().equals(Platform.class.getName()) && reader.getName().equals("newInstanceImpl")) {
            allClassesNode.connect(method.getResult());
            MethodReference methodRef = reader.getReference();
            method.getResult().addConsumer(type -> attachConstructor(agent, type.getName(),
                    new CallLocation(methodRef)));
        }
    }

    private void attachConstructor(DependencyAgent agent, String type, CallLocation location) {
        MethodReference ref = new MethodReference(type, "<init>", ValueType.VOID);
        MethodDependency methodDep = agent.linkMethod(ref);
        methodDep.addLocation(location);
        methodDep.getVariable(0).propagate(agent.getType(type));
        methodDep.use();
    }
}
