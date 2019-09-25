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

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class ExceptionHandlingDependencyListener extends AbstractDependencyListener {
    private static final MethodReference FILL_IN_STACK_TRACE = new MethodReference(Throwable.class,
            "fillInStackTrace", Throwable.class);
    private static final FieldReference STACK_TRACE = new FieldReference(Throwable.class.getName(), "stackTrace");
    private static final MethodReference STACK_TRACE_ELEMENT_INIT = new MethodReference(StackTraceElement.class,
            "<init>", String.class, String.class, String.class, int.class, void.class);

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().equals(FILL_IN_STACK_TRACE)) {
            DependencyNode node = agent.linkField(STACK_TRACE).getValue();
            node.propagate(agent.getType("[Ljava/lang/StackTraceElement;"));
            node.getArrayItem().propagate(agent.getType("java.lang.StackTraceElement"));

            MethodDependency initElem = agent.linkMethod(STACK_TRACE_ELEMENT_INIT);
            initElem.use();
            DependencyType stringType = agent.getType("java.lang.String");
            initElem.propagate(0, agent.getType(STACK_TRACE_ELEMENT_INIT.getClassName()));
            initElem.propagate(1, stringType);
            initElem.propagate(2, stringType);
            initElem.propagate(3, stringType);
        }
    }
}
