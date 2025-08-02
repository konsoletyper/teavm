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
package org.teavm.jso.impl;

import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ValueType;

public class JSWrapperDependency extends AbstractDependencyListener {
    private DependencyNode externalClassesNode;

    @Override
    public void started(DependencyAgent agent) {
        externalClassesNode = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        var cls = agent.getClassSource().get(className);
        if (cls.getAnnotations().get(JSClassToExpose.class.getName()) != null) {
            externalClassesNode.propagate(agent.getType(ValueType.object(className)));
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod().getOwnerName().equals(JS_WRAPPER_CLASS)) {
            switch (method.getMethod().getName()) {
                case "jsToWrapper":
                    method.getResult().propagate(agent.getType(ValueType.object(JS_WRAPPER_CLASS)));
                    break;
                case "dependencyJavaToJs":
                case "marshallJavaToJs":
                    method.getVariable(1).connect(externalClassesNode);
                    break;
                case "dependencyJsToJava":
                case "unmarshallJavaFromJs":
                    externalClassesNode.connect(method.getResult());
                    break;
                case "wrap":
                    method.getResult().propagate(agent.getType(ValueType.object(JS_WRAPPER_CLASS)));
                    externalClassesNode.connect(method.getResult());
                    break;
            }
        }
    }
}
