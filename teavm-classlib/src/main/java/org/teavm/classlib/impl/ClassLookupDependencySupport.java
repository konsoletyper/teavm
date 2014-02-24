/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.impl;

import org.teavm.dependency.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class ClassLookupDependencySupport implements DependencyListener {
    private DependencyNode allClasses;

    @Override
    public void started(DependencyChecker dependencyChecker) {
        allClasses = dependencyChecker.createNode();
    }

    @Override
    public void classAchieved(DependencyChecker dependencyChecker, String className) {
        allClasses.propagate(className);
    }

    @Override
    public void methodAchieved(final DependencyChecker dependencyChecker, MethodDependency method) {
        MethodReference ref = method.getReference();
        if (ref.getClassName().equals("java.lang.Class") && ref.getName().equals("forNameImpl")) {
            final DependencyStack stack = method.getStack();
            allClasses.addConsumer(new DependencyConsumer() {
                @Override public void consume(String type) {
                    ClassReader cls = dependencyChecker.getClassSource().get(type);
                    if (cls == null) {
                        return;
                    }
                    MethodReader initMethod = cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID));
                    if (initMethod != null) {
                        dependencyChecker.linkMethod(initMethod.getReference(), stack).use();
                    }
                }
            });
        }
    }

    @Override
    public void fieldAchieved(DependencyChecker dependencyChecker, FieldDependency field) {
    }
}
