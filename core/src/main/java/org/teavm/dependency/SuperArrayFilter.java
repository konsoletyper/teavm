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
package org.teavm.dependency;

import java.util.BitSet;
import org.teavm.model.ValueType;

class SuperArrayFilter implements DependencyTypeFilter {
    private DependencyAnalyzer analyzer;
    private DependencyTypeFilter itemTypeFilter;
    private BitSet knownTypes = new BitSet();
    private BitSet cache = new BitSet();

    SuperArrayFilter(DependencyAnalyzer analyzer, DependencyTypeFilter itemTypeFilter) {
        this.analyzer = analyzer;
        this.itemTypeFilter = itemTypeFilter;
    }

    @Override
    public boolean match(DependencyType type) {
        if (knownTypes.get(type.index)) {
            return cache.get(type.index);
        }
        boolean result = matchCacheMiss(type);
        knownTypes.set(type.index);
        cache.set(type.index, result);
        return result;
    }

    private boolean matchCacheMiss(DependencyType type) {
        if (!(type.getValueType() instanceof ValueType.Array)) {
            return false;
        }

        var itemValueType = ((ValueType.Array) type.getValueType()).getItemType();
        return itemTypeFilter.match(analyzer.getType(itemValueType));
    }

    @Override
    public int[] tryExtract(BitSet types) {
        int[] result = itemTypeFilter.tryExtract(types);
        if (result == null) {
            return null;
        }

        for (int i = 0; i < result.length; ++i) {
            var valueType = analyzer.types.get(i).getValueType();
            result[i] = analyzer.getType(ValueType.arrayOf(valueType)).index;
        }
        return result;
    }
}
