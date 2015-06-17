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
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.ElementModifier;
import org.teavm.platform.Platform;

/**
 *
 * @author Alexey Andreev
 */
public class AnnotationDependencySupport implements DependencyListener {
    private DependencyNode allAnnotations;

    @Override
    public void started(DependencyAgent agent) {
        allAnnotations = agent.createNode();
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className, CallLocation location) {
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        if (method.getMethod().getName().equals("$$_readAnnotations_$$") &&
                method.getMethod().hasModifier(ElementModifier.STATIC)) {
            method.getResult().getArrayItem().connect(allAnnotations);
        } else if (method.getReference().getClassName().equals(Platform.class.getName()) &&
                method.getReference().getName().equals("getAnnotations")) {
            method.getResult().propagate(agent.getType("[" + Annotation.class.getName()));
            allAnnotations.connect(method.getResult().getArrayItem());
        }
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field, CallLocation location) {
    }
}
