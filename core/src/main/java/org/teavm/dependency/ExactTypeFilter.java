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

class ExactTypeFilter implements DependencyTypeFilter {
    private static final int[] EMPTY = new int[0];
    String typeName;
    int cache = -1;
    int index;

    ExactTypeFilter(DependencyType dependencyType) {
        this.typeName = dependencyType.getName();
        index = dependencyType.index;
    }

    @Override
    public boolean match(DependencyType type) {
        if (cache >= 0) {
            return type.index == cache;
        }
        boolean result = typeName.equals(type.getName());
        if (result) {
            cache = type.index;
        }
        return result;
    }

    @Override
    public int[] tryExtract(BitSet types) {
        return types.get(index) ? new int[] { index } : EMPTY;
    }
}
