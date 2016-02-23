/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

class TestExceptionDependency extends AbstractDependencyListener {
    private MethodReference getMessageRef = new MethodReference(ExceptionHelper.class, "showException",
            Throwable.class, String.class);
    private DependencyNode allClasses;

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
        if (isException(agent.getClassSource(), className)) {
            allClasses.propagate(agent.getType(className));
        }
    }

    private boolean isException(ClassReaderSource classSource, String className) {
        while (className != null) {
            if (className.equals("java.lang.Throwable")) {
                return true;
            }
            ClassReader cls = classSource.get(className);
            if (cls == null) {
                return false;
            }
            className = cls.getParent();
        }
        return false;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        if (method.getReference().equals(getMessageRef)) {
            allClasses.connect(method.getVariable(1));
        }
    }
}
