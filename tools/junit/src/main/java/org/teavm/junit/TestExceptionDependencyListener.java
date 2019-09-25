/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.junit;

import static org.teavm.junit.TestExceptionPlugin.GET_MESSAGE;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

class TestExceptionDependencyListener extends AbstractDependencyListener {
    private DependencyNode allClasses;

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
        allClasses.addConsumer(c -> {
            if (agent.getClassSource().isSuperType("java.lang.Throwable", c.getName()).orElse(false)) {
                MethodDependency methodDep = agent.linkMethod(new MethodReference(c.getName(), GET_MESSAGE));
                methodDep.getVariable(0).propagate(c);
                methodDep.use();
            }
        });

        agent.linkClass("java.lang.Throwable");
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        allClasses.propagate(agent.getType(className));
    }
}
