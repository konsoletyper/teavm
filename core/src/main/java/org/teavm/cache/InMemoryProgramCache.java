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
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.model.ReferenceCache;

public class InMemoryProgramCache implements ProgramCache {
    private Map<MethodReference, Item> cache = new HashMap<>();
    private Map<MethodReference, Item> newItems = new HashMap<>();
    private ProgramIO io;

    public InMemoryProgramCache(ReferenceCache referenceCache, InMemorySymbolTable symbolTable,
            InMemorySymbolTable fileSymbolTable, InMemorySymbolTable variableSymbolTable) {
        io = new ProgramIO(referenceCache, symbolTable, fileSymbolTable, variableSymbolTable);
    }

    @Override
    public Program get(MethodReference method, CacheStatus cacheStatus) {
        Item item = cache.get(method);
        if (item == null) {
            return null;
        }

        if (Arrays.stream(item.dependencies).anyMatch(cacheStatus::isStaleClass)) {
            return null;
        }
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(item.program);
            return io.read(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void store(MethodReference method, Program program, Supplier<String[]> dependencies) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            io.write(program, output);
            newItems.put(method, new Item(output.toByteArray(), dependencies.get().clone()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        cache.putAll(newItems);
        newItems.clear();
    }

    public int getPendingItemsCount() {
        return newItems.size();
    }

    public void discard() {
        newItems.clear();
    }

    public void invalidate() {
        cache.clear();
        newItems.clear();
    }

    static final class Item {
        final byte[] program;
        final String[] dependencies;

        Item(byte[] program, String[] dependencies) {
            this.program = program;
            this.dependencies = dependencies;
        }
    }
}
