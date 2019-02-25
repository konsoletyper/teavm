/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.cache;

import java.util.function.Supplier;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.model.MethodReference;

public class EmptyMethodNodeCache implements MethodNodeCache {
    public static final EmptyMethodNodeCache INSTANCE = new EmptyMethodNodeCache();

    private EmptyMethodNodeCache() {
    }

    @Override
    public AstCacheEntry get(MethodReference methodReference, CacheStatus cacheStatus) {
        return null;
    }

    @Override
    public void store(MethodReference methodReference, AstCacheEntry node, Supplier<String[]> dependencies) {
    }

    @Override
    public AsyncMethodNode getAsync(MethodReference methodReference, CacheStatus cacheStatus) {
        return null;
    }

    @Override
    public void storeAsync(MethodReference methodReference, AsyncMethodNode node, Supplier<String[]> dependencies) {
    }
}
