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

import org.teavm.dependency.*;
import org.teavm.model.*;
import org.teavm.platform.Platform;

public class ClassLookupDependencySupport extends AbstractDependencyListener {
    private DependencyNode allClasses;

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
        allClasses.propagate(agent.getType(className));
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals(Platform.class.getName()) && ref.getName().equals("lookupClass")) {
            allClasses.addConsumer(type -> {
                ClassReader cls = agent.getClassSource().get(type.getName());
                if (cls == null) {
                    return;
                }
                MethodReader initMethod = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
                if (initMethod != null) {
                    agent.linkMethod(initMethod.getReference(), location).use();
                }
            });
        }
    }
}
