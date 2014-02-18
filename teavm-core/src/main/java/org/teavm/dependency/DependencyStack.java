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
package org.teavm.dependency;

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyStack {
    public static final DependencyStack ROOT = new DependencyStack();
    private MethodReference method;
    private DependencyStack cause;

    private DependencyStack() {
    }

    public DependencyStack(MethodReference method) {
        this(method, ROOT);
    }

    public DependencyStack(MethodReference method, DependencyStack cause) {
        if (method == null || cause == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }
        this.method = method;
        this.cause = cause;
    }

    public MethodReference getMethod() {
        return method;
    }

    public DependencyStack getCause() {
        return cause;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DependencyStack stack = this;
        while (true) {
            if (stack.method == null) {
                sb.append("  used by ROOT\n");
                break;
            } else {
                sb.append(" used by " + stack.method);
                stack = stack.cause;
            }
        }
        return sb.toString();
    }
}
