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

import org.teavm.backend.wasm.runtime.gc.WasmGCResources;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCResourceDependency extends AbstractDependencyListener {
    private static final MethodReference ACQUIRE_METHOD = new MethodReference(WasmGCResources.class,
            "acquireResources", WasmGCResources.Resource[].class);

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod().getReference().equals(ACQUIRE_METHOD)) {
            var create = agent.linkMethod(new MethodReference(WasmGCResources.class,
                    "create", String.class, int.class, int.class, WasmGCResources.Resource.class));
            create.propagate(1, agent.getType("java.lang.String"));
            create.use();
            method.getResult().propagate(agent.getType(ValueType.arrayOf(ValueType.object(
                    WasmGCResources.Resource.class.getName())).toString()));
            method.getResult().getArrayItem().propagate(agent.getType(WasmGCResources.Resource.class.getName()));
        }
    }
}
