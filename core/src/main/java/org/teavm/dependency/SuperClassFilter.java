/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.dependency;

import com.carrotsearch.hppc.IntIntHashMap;
import java.util.BitSet;
import org.teavm.common.OptionalPredicate;
import org.teavm.model.ValueType;

class SuperClassFilter implements DependencyTypeFilter {
    private static final int SMALL_CACHE_THRESHOLD = 16;
    private OptionalPredicate<ValueType> predicate;
    private IntIntHashMap smallCache;
    private BitSet knownTypes;
    private BitSet cache;

    SuperClassFilter(DependencyAnalyzer dependencyAnalyzer, String superClass) {
        predicate = dependencyAnalyzer.getClassHierarchy().getSuperclassPredicate(superClass);
    }

    @Override
    public boolean match(DependencyType type) {
        if (knownTypes != null) {
            if (knownTypes.get(type.index)) {
                return cache.get(type.index);
            }
            boolean result = predicate.test(type.getValueType(), false);
            knownTypes.set(type.index);
            cache.set(type.index, result);
            return result;
        }

        if (smallCache == null) {
            smallCache = new IntIntHashMap();
        }

        var result = smallCache.getOrDefault(type.index, -1);
        if (result != -1) {
            return result != 0;
        }

        var value = predicate.test(type.getValueType(), false);
        smallCache.put(type.index, value ? 1 : 0);
        if (smallCache.size() > SMALL_CACHE_THRESHOLD) {
            knownTypes = new BitSet();
            cache = new BitSet();
            for (var entry : smallCache) {
                knownTypes.set(entry.key);
                if (entry.value != 0) {
                    cache.set(entry.key);
                }
            }
            smallCache = null;
        }
        return value;
    }
}
