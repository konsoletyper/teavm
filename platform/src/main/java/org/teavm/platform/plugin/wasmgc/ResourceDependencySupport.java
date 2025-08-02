/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.platform.plugin.wasmgc;

import java.util.HashSet;
import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ResourceDependencySupport extends AbstractDependencyListener {
    private Set<MethodReference> metadataMethods = new HashSet<>();

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        var annot = method.getMethod().getAnnotations().get(FieldMarker.class.getName());
        if (annot != null) {
            var type = method.getMethod().getResultType();
            if (type instanceof ValueType.Object) {
                method.getResult().propagate(agent.getType(type));
            }
        }
        if (metadataMethods.contains(method.getMethod().getReference())) {
            var resultType = method.getMethod().getResultType();
            if (resultType instanceof ValueType.Object) {
                method.getResult().propagate(agent.getType(resultType));
            }
        }
    }

    public void addMetadataMethod(MethodReference method) {
        metadataMethods.add(method);
    }
}
