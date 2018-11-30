/*
 *  Copyright 2015 Alexey Andreev.
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

import java.lang.annotation.Annotation;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformAnnotationProvider;

public class AnnotationDependencySupport extends AbstractDependencyListener {
    private DependencyNode allClasses;
    private MethodDependency getAnnotationsDep;

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        allClasses.propagate(agent.getType(className));
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getClassName().equals(Platform.class.getName())
                && method.getReference().getName().equals("getAnnotations")) {
            method.getResult().propagate(agent.getType("[" + ValueType.parse(Annotation.class).toString()));
            if (getAnnotationsDep == null) {
                getAnnotationsDep = agent.linkMethod(new MethodReference(PlatformAnnotationProvider.class,
                        "getAnnotations", Annotation[].class));
            }
            method.addLocationListener(getAnnotationsDep::addLocation);

            allClasses.addConsumer(type -> {
                if (type.getName().endsWith("$$__annotations__$$")) {
                    return;
                }
                String className = type.getName() + "$$__annotations__$$";
                MethodDependency initMethod = agent.linkMethod(new MethodReference(className, "<init>",
                        ValueType.VOID));
                initMethod.propagate(0, className);
                initMethod.use();
                MethodDependency readMethod = agent.linkMethod(new MethodReference(className,
                        "getAnnotations", ValueType.parse(Annotation[].class)));
                readMethod.getResult().getArrayItem().connect(method.getResult().getArrayItem());
                readMethod.use();

                method.addLocationListener(location -> {
                    initMethod.addLocation(location);
                    readMethod.addLocation(location);
                });
            });
        }
    }
}
