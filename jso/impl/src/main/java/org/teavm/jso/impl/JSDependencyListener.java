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

import java.util.HashSet;
import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

class JSDependencyListener extends AbstractDependencyListener {
    private JSBodyRepository repository;
    private Set<String> reachedClasses = new HashSet<>();
    private Set<MethodReference> reachedMethods = new HashSet<>();

    JSDependencyListener(JSBodyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        MethodReference ref = method.getReference();
        if (!reachedMethods.add(ref)) {
            return;
        }
        Set<MethodReference> callbackMethods = repository.callbackMethods.get(ref);
        if (callbackMethods != null) {
            for (MethodReference callbackMethod : callbackMethods) {
                agent.linkMethod(callbackMethod, new CallLocation(ref)).use();
            }
        }
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
        if (!reachedClasses.add(className)) {
            return;
        }
        ClassReader cls = agent.getClassSource().get(className);
        for (MethodReader method : cls.getMethods()) {
            AnnotationReader exposeAnnot = method.getAnnotations().get(JSMethodToExpose.class.getName());
            if (exposeAnnot != null) {
                MethodDependency methodDep = agent.linkMethod(method.getReference(), null);
                methodDep.getVariable(0).propagate(agent.getType(className));
                methodDep.use();
            }
        }
    }
}
