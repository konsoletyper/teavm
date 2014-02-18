/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.javascript;

import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class JavascriptEntryPoint {
    private String publicName;
    MethodReference reference;
    private MethodDependency graph;

    JavascriptEntryPoint(String publicName, MethodReference reference, MethodDependency graph) {
        this.publicName = publicName;
        this.reference = reference;
        this.graph = graph;
    }

    String getPublicName() {
        return publicName;
    }

    public JavascriptEntryPoint withValue(int argument, String type) {
        if (argument > reference.parameterCount()) {
            throw new IllegalArgumentException("Illegal argument #" + argument + " of " + reference.parameterCount());
        }
        graph.getVariable(argument).propagate(type);
        return this;
    }
}
