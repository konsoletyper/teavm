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
package org.teavm.backend.javascript.intrinsics.reflection;

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.model.MethodReference;

public class ProxyGeneratorContext {
    private Map<MethodReference, String> properties = new HashMap<>();
    private int indexGen;

    public String getPropertyName(MethodReference methodRef) {
        return properties.computeIfAbsent(methodRef, ref -> {
            while (true) {
                var result = RenderingUtil.indexToId(indexGen++);
                if (!RenderingUtil.KEYWORDS.contains(result)) {
                    return result;
                }
            }
        });
    }
}
