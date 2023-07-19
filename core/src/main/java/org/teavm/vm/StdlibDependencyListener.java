/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.vm;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class StdlibDependencyListener extends AbstractDependencyListener {
    private static final MethodReference ARRAY_COPY_METHOD = new MethodReference(System.class,
            "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private static final MethodReference FAST_ARRAY_COPY_METHOD = new MethodReference(System.class,
            "fastArraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private boolean reached;

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (!reached && method.getReference().equals(ARRAY_COPY_METHOD)) {
            reached = true;
            agent.linkMethod(FAST_ARRAY_COPY_METHOD).use();
        }
    }
}
