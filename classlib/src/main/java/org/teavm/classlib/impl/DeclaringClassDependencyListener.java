/*
 *  Copyright 2019 konsoletyper.
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

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ClassReader;

public class DeclaringClassDependencyListener extends AbstractDependencyListener {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getClassName().equals("java.lang.Class")
                && method.getReference().getName().equals("getDeclaringClass")) {
            method.getVariable(0).getClassValueNode().addConsumer(t -> {
                ClassReader cls = agent.getClassSource().get(t.getName());
                if (cls != null && cls.getOwnerName() != null) {
                    agent.linkClass(cls.getOwnerName());
                }
            });
        }
    }
}
