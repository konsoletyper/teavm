/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.ast.cache;

import java.util.HashMap;
import java.util.Map;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.model.MethodReference;

public class InMemoryRegularMethodNodeCache implements MethodNodeCache {
    private Map<MethodReference, RegularMethodNode> cache = new HashMap<>();
    private Map<MethodReference, AsyncMethodNode> asyncCache = new HashMap<>();

    @Override
    public RegularMethodNode get(MethodReference methodReference) {
        return cache.get(methodReference);
    }

    @Override
    public void store(MethodReference methodReference, RegularMethodNode node) {
        cache.put(methodReference, node);
    }

    @Override
    public AsyncMethodNode getAsync(MethodReference methodReference) {
        return asyncCache.get(methodReference);
    }

    @Override
    public void storeAsync(MethodReference methodReference, AsyncMethodNode node) {
        asyncCache.put(methodReference, node);
    }
}
