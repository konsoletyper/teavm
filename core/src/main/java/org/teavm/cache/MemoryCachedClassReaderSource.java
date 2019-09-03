/*
 *  Copyright 2019 Alexey Andreev.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;

public class MemoryCachedClassReaderSource implements ClassReaderSource, CacheStatus {
    private Map<String, Entry> cache = new HashMap<>();
    private Function<String, ClassReader> provider;
    private ClassIO classIO;
    private final Set<String> freshClasses = new HashSet<>();

    public MemoryCachedClassReaderSource(ReferenceCache referenceCache, SymbolTable symbolTable,
            SymbolTable fileTable, SymbolTable varTable) {
        classIO = new ClassIO(referenceCache, symbolTable, fileTable, varTable);
    }

    public void setProvider(Function<String, ClassReader> provider) {
        this.provider = provider;
    }

    @Override
    public boolean isStaleClass(String className) {
        return !freshClasses.contains(className);
    }

    @Override
    public boolean isStaleMethod(MethodReference method) {
        return isStaleClass(method.getClassName());
    }

    public void populate(String name) {
        getEntry(name);
    }

    @Override
    public ClassReader get(String name) {
        Entry entry = getEntry(name);
        if (entry.data == null) {
            return null;
        }

        ClassReader cls = entry.reader;
        if (cls == null) {
            ByteArrayInputStream input = new ByteArrayInputStream(entry.data);
            try {
                cls = classIO.readClass(input, name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            entry.reader = cls;
        }
        return cls;
    }

    private Entry getEntry(String name) {
        return cache.computeIfAbsent(name, className -> {
            ClassReader cls = provider != null ? provider.apply(className) : null;
            Entry en = new Entry();
            if (cls != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try {
                    classIO.writeClass(output, cls);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                en.data = output.toByteArray();
            }
            return en;
        });
    }

    public void commit() {
        freshClasses.addAll(cache.keySet());
    }

    public void evict(Collection<? extends String> classes) {
        cache.keySet().removeAll(classes);
        freshClasses.removeAll(classes);
    }

    public void invalidate() {
        cache.clear();
        freshClasses.clear();
    }

    static class Entry {
        byte[] data;
        ClassReader reader;
    }
}
