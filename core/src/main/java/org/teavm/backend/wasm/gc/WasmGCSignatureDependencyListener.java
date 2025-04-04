/*
 *  Copyright 2025 Alexey Andreev.
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
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ValueType;

public class WasmGCSignatureDependencyListener extends AbstractDependencyListener {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        for (var i = 0; i < method.getMethod().parameterCount(); ++i) {
            linkType(agent, method.getMethod().parameterType(i));
        }
        linkType(agent, method.getMethod().getResultType());
    }

    @Override
    public void fieldReached(DependencyAgent agent, FieldDependency field) {
        linkType(agent, field.getField().getType());
    }

    private void linkType(DependencyAgent agent, ValueType type) {
        if (type instanceof ValueType.Object) {
            agent.linkClass(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Array) {
            linkType(agent, ((ValueType.Array) type).getItemType());
        }
    }
}
