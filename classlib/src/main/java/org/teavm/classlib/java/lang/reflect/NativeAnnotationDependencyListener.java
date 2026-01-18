/*
 *  Copyright 2025 Alexey Andreev.
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

import java.lang.annotation.Annotation;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class NativeAnnotationDependencyListener extends BaseAnnotationDependencyListener {
    public NativeAnnotationDependencyListener() {
        super(true, false);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        super.methodReached(agent, method);

        MethodReference methodRef = method.getMethod().getReference();
        if (methodRef.getClassName().equals("java.lang.Class")
                && methodRef.getName().equals("getDeclaredAnnotationsImpl")) {
            method.getResult().propagate(agent.getType(ValueType.arrayOf(ValueType.object(
                    Annotation.class.getName()))));
            reachGetAnnotations(agent, method.getVariable(0), method.getResult().getArrayItem());
        }
    }

    private void reachGetAnnotations(DependencyAgent agent, DependencyNode inputNode, DependencyNode outputNode) {
        inputNode.getClassValueNode().addConsumer(type -> {
            if (!(type.getValueType() instanceof ValueType.Object)) {
                return;
            }
            var className = ((ValueType.Object) type.getValueType()).getClassName();
            var cls = agent.getClassSource().get(className);
            if (cls == null) {
                return;
            }

            annotHelper.propagateAnnotationImplementations(agent, cls.getAnnotations().all(), outputNode);
        });
    }
}
