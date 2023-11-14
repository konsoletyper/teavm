/*
 *  Copyright 2019 Alexey Andreev.
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

import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class StringNativeDependency implements DependencyPlugin {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getName().equals("intern")) {
            agent.linkMethod(new MethodReference(String.class, "hashCode", int.class))
                    .propagate(0, agent.getType("java.lang.String"))
                    .use();
            agent.linkMethod(new MethodReference(String.class, "equals", Object.class, boolean.class))
                    .propagate(0, agent.getType("java.lang.String"))
                    .propagate(1, agent.getType("java.lang.String"))
                    .use();
        }
    }
}
