/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.gc;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class SystemArrayCopyDependencySupport extends AbstractDependencyListener {
    private static final MethodReference COPY_METHOD = new MethodReference(System.class,
            "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod().getReference().equals(COPY_METHOD)) {
            var implMethod = agent.linkMethod(new MethodReference(System.class,
                    "arrayCopyImpl", Object.class, int.class, Object.class, int.class, int.class, void.class));
            method.getVariable(1).connect(implMethod.getVariable(1));
            method.getVariable(3).connect(implMethod.getVariable(3));
            implMethod.use();
        }
    }
}
