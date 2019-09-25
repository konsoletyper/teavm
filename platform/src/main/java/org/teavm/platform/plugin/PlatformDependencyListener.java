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

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.platform.Platform;

public class PlatformDependencyListener extends AbstractDependencyListener {
    private DependencyNode allClasses;

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        allClasses.propagate(agent.getType(className));
    }

    @Override
    public void methodReached(final DependencyAgent agent, MethodDependency method) {
        if (!method.getReference().getClassName().equals(Platform.class.getName())) {
            return;
        }
        switch (method.getReference().getName()) {
            case "objectFromResource":
                allClasses.connect(method.getResult());
                break;
        }
    }
}
