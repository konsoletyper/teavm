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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

public class MemoryCachedClassReaderSource implements ClassReaderSource, CacheStatus {
    private ClassReaderSource underlyingSource;
    private final Map<String, Optional<ClassReader>> cache = new HashMap<>();
    private final Set<String> freshClasses = new HashSet<>();

    public void setUnderlyingSource(ClassReaderSource underlyingSource) {
        this.underlyingSource = underlyingSource;
    }

    @Override
    public boolean isStaleClass(String className) {
        return !freshClasses.contains(className);
    }

    @Override
    public boolean isStaleMethod(MethodReference method) {
        return isStaleClass(method.getClassName());
    }

    @Override
    public ClassReader get(String name) {
        return cache.computeIfAbsent(name, key -> {
            if (underlyingSource == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(underlyingSource.get(key));
        }).orElse(null);
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
}
