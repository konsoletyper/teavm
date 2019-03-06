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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.ControlFlowEntry;
import org.teavm.ast.RegularMethodNode;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;

public class InMemoryMethodNodeCache implements MethodNodeCache {
    private Map<MethodReference, RegularItem> cache = new HashMap<>();
    private Map<MethodReference, RegularItem> newItems = new HashMap<>();
    private Map<MethodReference, AsyncItem> asyncCache = new HashMap<>();
    private Map<MethodReference, AsyncItem> newAsyncItems = new HashMap<>();
    private AstIO io;

    public InMemoryMethodNodeCache(ReferenceCache referenceCache, InMemorySymbolTable symbolTable,
            InMemorySymbolTable fileSymbolTable, InMemorySymbolTable variableSymbolTable) {
        io = new AstIO(referenceCache, symbolTable, fileSymbolTable, variableSymbolTable);
    }

    @Override
    public AstCacheEntry get(MethodReference methodReference, CacheStatus cacheStatus) {
        RegularItem item = cache.get(methodReference);
        if (item == null) {
            return null;
        }

        if (Arrays.stream(item.dependencies).anyMatch(cacheStatus::isStaleClass)) {
            return null;
        }

        VarDataInput input = new VarDataInput(new ByteArrayInputStream(item.entry));
        try {
            ControlFlowEntry[] cfg = io.readControlFlow(input);
            RegularMethodNode ast = io.read(input, methodReference);
            return new AstCacheEntry(ast, cfg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void store(MethodReference methodReference, AstCacheEntry entry, Supplier<String[]> dependencies) {
        newItems.put(methodReference, new RegularItem(entry, dependencies.get().clone()));
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

        VarDataInput input = new VarDataInput(new ByteArrayInputStream(item.node));
        try {
            return io.readAsync(input, methodReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    final class RegularItem {
        final byte[] entry;
        final String[] dependencies;

        RegularItem(AstCacheEntry entry, String[] dependencies) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                VarDataOutput data = new VarDataOutput(output);
                io.write(data, entry.cfg);
                io.write(data, entry.method);
                this.entry = output.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.dependencies = dependencies;
        }
    }

    final class AsyncItem {
        final byte[] node;
        final String[] dependencies;

        AsyncItem(AsyncMethodNode node, String[] dependencies) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                VarDataOutput data = new VarDataOutput(output);
                io.writeAsync(data, node);
                this.node = output.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.dependencies = dependencies;
        }
    }
}
