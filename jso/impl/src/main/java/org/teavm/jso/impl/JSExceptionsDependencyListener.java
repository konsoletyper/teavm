/*
 *  Copyright 2018 Alexey Andreev.
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
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.jso.JSExceptions;

public class JSExceptionsDependencyListener extends AbstractDependencyListener {
    private DependencyNode allExceptions;

    @Override
    public void started(DependencyAgent agent) {
        allExceptions = agent.createNode();
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getClassName().equals(JSExceptions.class.getName())) {
            if (method.getReference().getName().equals("getJavaException")) {
                allExceptions.connect(method.getResult());
            }
        } else if (method.getReference().getClassName().equals(JS.class.getName())) {
            switch (method.getReference().getName()) {
                case "get":
                case "set":
                case "invoke":
                    allExceptions.connect(method.getThrown());
                    break;
            }
        } else {
            method.getThrown().connect(allExceptions);
        }
    }
}
