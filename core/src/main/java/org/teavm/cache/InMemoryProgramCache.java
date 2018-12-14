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
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;

public class InMemoryProgramCache implements ProgramCache {
    private Map<MethodReference, Item> cache = new HashMap<>();
    private Map<MethodReference, Item> newItems = new HashMap<>();

    @Override
    public Program get(MethodReference method, CacheStatus cacheStatus) {
        Item item = cache.get(method);
        if (item == null) {
            return null;
        }

        if (Arrays.stream(item.dependencies).anyMatch(cacheStatus::isStaleClass)) {
            return null;
        }

        return item.program;
    }

    @Override
    public void store(MethodReference method, Program program, Supplier<String[]> dependencies) {
        newItems.put(method, new Item(program, dependencies.get().clone()));
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
        final Program program;
        final String[] dependencies;

        Item(Program program, String[] dependencies) {
            this.program = program;
            this.dependencies = dependencies;
        }
    }
}
