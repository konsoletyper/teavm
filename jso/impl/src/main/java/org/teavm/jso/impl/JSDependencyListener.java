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
package org.teavm.jso.impl;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.jso.JSExportClasses;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class JSDependencyListener extends AbstractDependencyListener {
    private JSBodyRepository repository;

    JSDependencyListener(JSBodyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        var callbackMethods = repository.callbackMethods.get(ref);
        if (callbackMethods != null) {
            for (MethodReference callbackMethod : callbackMethods) {
                agent.linkMethod(callbackMethod).addLocation(new CallLocation(ref)).use();
            }
        }
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        ClassReader cls = agent.getClassSource().get(className);
        for (MethodReader method : cls.getMethods()) {
            AnnotationReader exposeAnnot = method.getAnnotations().get(JSMethodToExpose.class.getName());
            if (exposeAnnot == null) {
                exposeAnnot = method.getAnnotations().get(JSGetterToExpose.class.getName());
            }
            if (exposeAnnot == null) {
                exposeAnnot = method.getAnnotations().get(JSSetterToExpose.class.getName());
            }
            if (exposeAnnot == null) {
                exposeAnnot = method.getAnnotations().get(JSConstructorToExpose.class.getName());
            }
            if (exposeAnnot != null) {
                MethodDependency methodDep = agent.linkMethod(method.getReference());
                if (methodDep.getMethod() != null) {
                    if (!methodDep.getMethod().hasModifier(ElementModifier.STATIC)) {
                        methodDep.getVariable(0).propagate(agent.getType(className));
                    }
                    methodDep.use();
                }
            }
        }

        var exportClassesAnnot = cls.getAnnotations().get(JSExportClasses.class.getName());
        if (exportClassesAnnot != null) {
            for (var classRef : exportClassesAnnot.getValue("value").getList()) {
                if (classRef.getJavaClass() instanceof ValueType.Object) {
                    var classRefName = ((ValueType.Object) classRef.getJavaClass()).getClassName();
                    agent.linkClass(classRefName);
                }
            }
        }
    }
}
