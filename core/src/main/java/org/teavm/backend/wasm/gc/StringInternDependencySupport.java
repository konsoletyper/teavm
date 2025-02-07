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

import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class StringInternDependencySupport extends AbstractDependencyListener {
    private static final MethodReference STRING_INTERN = new MethodReference(String.class, "intern", String.class);

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod().getReference().equals(STRING_INTERN)) {
            var query = agent.linkMethod(new MethodReference(StringInternPool.class, "query",
                    String.class, String.class));
            query.getVariable(1).propagate(agent.getType("java.lang.String"));
            query.use();

            var entryTypeName = StringInternPool.class.getName() + "$Entry";
            var remove = agent.linkMethod(new MethodReference(
                    StringInternPool.class.getName(),
                    "remove",
                    ValueType.object(entryTypeName),
                    ValueType.VOID));
            remove.getVariable(1).propagate(agent.getType(entryTypeName));
            remove.use();

            var clinit = agent.linkMethod(new MethodReference(StringInternPool.class, "<clinit>",
                    void.class));
            clinit.use();

            var getValue = agent.linkMethod(new MethodReference(StringInternPool.class.getName() + "$Entry",
                    "getValue", ValueType.parse(String.class)));
            getValue.getResult().propagate(agent.getType(String.class.getName()));
        }
    }
}
