/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.lowlevel.dependency;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class WeakReferenceDependencyListener extends AbstractDependencyListener {
    private DependencyNode referentNode;
    private DependencyNode referenceNode;

    @Override
    public void started(DependencyAgent agent) {
        referentNode = agent.createNode();
        referenceNode = agent.createNode();
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference methodRef = method.getReference();
        if (methodRef.getClassName().equals(WeakReference.class.getName())) {
            switch (methodRef.getName()) {
                case "<init>":
                    reachReferenceInit(agent, method);
                    break;
                case "get":
                    reachReferenceGet(method);
                    break;
            }
        } else if (methodRef.getClassName().equals(ReferenceQueue.class.getName())) {
            switch (methodRef.getName()) {
                case "<init>":
                    reachQueueInit(agent, method);
                    break;
                case "poll":
                    reachQueuePoll(method);
                    break;
            }
        }
        super.methodReached(agent, method);
    }

    private void reachReferenceInit(DependencyAgent agent, MethodDependency method) {
        MethodDependency superMethod = agent.linkMethod(new MethodReference(Reference.class, "<init>", void.class));
        method.getVariable(0).connect(superMethod.getVariable(0));
        superMethod.use();

        method.getVariable(0).connect(referenceNode);
        method.getVariable(1).connect(referentNode);
    }

    private void reachReferenceGet(MethodDependency method) {
        referentNode.connect(method.getResult());
    }

    private void reachQueuePoll(MethodDependency method) {
        referenceNode.connect(method.getResult());
    }

    private void reachQueueInit(DependencyAgent agent, MethodDependency method) {
        MethodDependency superMethod = agent.linkMethod(new MethodReference(Object.class, "<init>", void.class));
        method.getVariable(0).connect(superMethod.getVariable(0));
        superMethod.use();
    }
}
