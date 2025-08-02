/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.dependency.ClassDependency;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ClassDependencyListener implements DependencyPlugin {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "initialize":
                method.getVariable(0).getClassValueNode().addConsumer(type -> {
                    if (!(type.getValueType() instanceof ValueType.Object)) {
                        return;
                    }
                    var className = ((ValueType.Object) type.getValueType()).getClassName();
                    ClassDependency classDep = agent.linkClass(className);
                    if (classDep != null) {
                        classDep.initClass(new CallLocation(method.getReference()));
                    }
                });
                break;
            case "getSimpleNameCacheLowLevel":
            case "getCanonicalNameCacheLowLevel":
            case "getNameCacheLowLevel":
                method.getResult().propagate(agent.getType(ValueType.object("java.lang.String")));
                break;
            case "getNameImpl":
                method.getResult().propagate(agent.getType(ValueType.object("java.lang.String")));
                break;
            case "last":
            case "previous":
                method.getResult().propagate(agent.getType(ValueType.object("java.lang.Class")));
                break;
            case "newInstanceImpl": {
                var location = new CallLocation(method.getReference());
                method.getVariable(0).getClassValueNode().addConsumer(type -> {
                    if (!(type.getValueType() instanceof ValueType.Object)) {
                        return;
                    }
                    var className = ((ValueType.Object) type.getValueType()).getClassName();
                    var ref = new MethodReference(className, "<init>", ValueType.VOID);
                    var methodDep = agent.linkMethod(ref);
                    methodDep.addLocation(location);
                    methodDep.getVariable(0).propagate(type);
                    methodDep.use();
                    method.getResult().propagate(type);
                });
                break;
            }
        }
    }
}
