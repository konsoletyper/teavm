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

import java.util.BitSet;
import org.teavm.common.OptionalPredicate;

class SuperClassFilter implements DependencyTypeFilter {
    private static final int[] EMPTY_ARRAY = new int[0];
    private DependencyType superType;
    private OptionalPredicate<String> predicate;
    private BitSet knownTypes = new BitSet();
    private BitSet cache = new BitSet();

    SuperClassFilter(DependencyAnalyzer dependencyAnalyzer, DependencyType superType) {
        this.superType = superType;
        predicate = dependencyAnalyzer.getClassHierarchy().getSuperclassPredicate(superType.getName());
    }

    @Override
    public boolean match(DependencyType type) {
        if (!superType.subtypeExists) {
            return superType.index == type.index;
        }
        if (knownTypes.get(type.index)) {
            return cache.get(type.index);
        }
        boolean result = predicate.test(type.getName(), false);
        knownTypes.set(type.index);
        cache.set(type.index, result);
        return result;
    }

    @Override
    public int[] tryExtract(BitSet types) {
        if (superType.subtypeExists) {
            return null;
        }
        return types.get(superType.index) ? new int[] { superType.index } : EMPTY_ARRAY;
    }
}
