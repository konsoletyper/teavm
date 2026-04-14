/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.dependencies;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.interop.Export;

public class WasmGCExportDependencyListener extends AbstractDependencyListener {
    @Override
    public void classReached(DependencyAgent agent, String className) {
        for (var reader : agent.getClassSource().get(className).getMethods()) {
            var annotation = reader.getAnnotations().get(Export.class.getName());
            if (annotation != null) {
                agent.linkMethod(reader.getReference()).use();
            }
        }
    }
}
