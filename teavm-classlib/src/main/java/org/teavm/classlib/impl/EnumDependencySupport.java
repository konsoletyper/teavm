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
import org.teavm.model.ClassReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class EnumDependencySupport implements DependencyListener {
    private DependencyNode allEnums;
    private volatile DependencyStack enumConstantsStack;

    @Override
    public void started(DependencyAgent agent) {
        allEnums = agent.createNode();
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls == null || cls.getParent() == null || !cls.getParent().equals("java.lang.Enum")) {
            return;
        }
        allEnums.propagate(agent.getType(className));
        if (enumConstantsStack != null) {
            MethodReader method = cls.getMethod(new MethodDescriptor("values",
                    ValueType.arrayOf(ValueType.object(cls.getName()))));
            if (method != null) {
                agent.linkMethod(method.getReference(), enumConstantsStack).use();
            }
        }
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getClassName().equals("java.lang.Class") &&
                method.getReference().getName().equals("getEnumConstantsImpl")) {
            allEnums.connect(method.getResult().getArrayItem());
            method.getResult().propagate(agent.getType("[java.lang.Enum"));
            enumConstantsStack = method.getStack();
            for (String cls : agent.getAchievableClasses()) {
                classAchieved(agent, cls);
            }
        }
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field) {
    }
}
