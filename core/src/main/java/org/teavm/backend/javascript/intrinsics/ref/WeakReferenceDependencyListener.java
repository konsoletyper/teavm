/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.backend.javascript.intrinsics.ref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class WeakReferenceDependencyListener extends AbstractDependencyListener {
    private DependencyNode initRef;

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        initRef = agent.createNode();
        if (method.getMethod().getOwnerName().equals(WeakReference.class.getName())) {
            referenceMethodReached(agent, method);
        } else if (method.getMethod().getOwnerName().equals(ReferenceQueue.class.getName())) {
            queueMethodReached(agent, method);
        }
    }

    private void referenceMethodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "<init>": {
                if (method.getParameterCount() == 3) {
                    var field = agent.linkField(new FieldReference(method.getMethod().getOwnerName(), "value"));
                    method.getVariable(2).connect(field.getValue());
                    method.getVariable(1).connect(initRef);
                }
                break;
            }
            case "get": {
                var field = agent.linkField(new FieldReference(method.getMethod().getOwnerName(), "value"));
                field.getValue().connect(method.getResult());
                break;
            }
        }
    }

    private void queueMethodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "poll":
                initRef.connect(method.getResult());
                break;
            case "<init>":
                agent.linkField(new FieldReference(ReferenceQueue.class.getName(), "inner"));
                agent.linkField(new FieldReference(ReferenceQueue.class.getName(), "registry"));
                break;
            case "registerCallback": {
                var reportMethod = agent.linkMethod(new MethodReference(ReferenceQueue.class,
                        "reportNext", Reference.class, boolean.class));
                initRef.connect(reportMethod.getVariable(1));
                reportMethod.use();
                break;
            }
        }
    }
}
