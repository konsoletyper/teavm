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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.model.MethodReference;

public class InMemoryMethodNodeCache implements MethodNodeCache {
    private Map<MethodReference, RegularItem> cache = new HashMap<>();
    private Map<MethodReference, RegularItem> newItems = new HashMap<>();
    private Map<MethodReference, AsyncItem> asyncCache = new HashMap<>();
    private Map<MethodReference, AsyncItem> newAsyncItems = new HashMap<>();

    @Override
    public RegularMethodNode get(MethodReference methodReference, CacheStatus cacheStatus) {
        RegularItem item = cache.get(methodReference);
        if (item == null) {
            return null;
        }

        if (Arrays.stream(item.dependencies).anyMatch(cacheStatus::isStaleClass)) {
            return null;
        }

        return item.node;
    }

    @Override
    public void store(MethodReference methodReference, RegularMethodNode node, Supplier<String[]> dependencies) {
        newItems.put(methodReference, new RegularItem(node, dependencies.get().clone()));
    }

    @Override
    public AsyncMethodNode getAsync(MethodReference methodReference, CacheStatus cacheStatus) {
        AsyncItem item = asyncCache.get(methodReference);
        if (item == null) {
            return null;
        }

        if (Arrays.stream(item.dependencies).anyMatch(cacheStatus::isStaleClass)) {
            return null;
        }

        return item.node;
    }

    @Override
    public void storeAsync(MethodReference methodReference, AsyncMethodNode node, Supplier<String[]> dependencies) {
        newAsyncItems.put(methodReference, new AsyncItem(node, dependencies.get().clone()));
    }

    public void commit() {
        cache.putAll(newItems);
        asyncCache.putAll(newAsyncItems);
        newItems.clear();
        newAsyncItems.clear();
    }

    public void discard() {
        newItems.clear();
        newAsyncItems.clear();
    }

    public void invalidate() {
        cache.clear();
        newItems.clear();
        asyncCache.clear();
        newAsyncItems.clear();
    }

    static final class RegularItem {
        final RegularMethodNode node;
        final String[] dependencies;

        RegularItem(RegularMethodNode node, String[] dependencies) {
            this.node = node;
            this.dependencies = dependencies;
        }
    }

    static final class AsyncItem {
        final AsyncMethodNode node;
        final String[] dependencies;

        AsyncItem(AsyncMethodNode node, String[] dependencies) {
            this.node = node;
            this.dependencies = dependencies;
        }
    }
}
