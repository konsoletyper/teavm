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

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import org.teavm.model.ClassReaderSource;

class SuperClassFilter implements DependencyTypeFilter {
    private ClassReaderSource classSource;
    private String superType;
    private IntIntMap cache = new IntIntOpenHashMap();

    SuperClassFilter(ClassReaderSource classSource, String superType) {
        this.classSource = classSource;
        this.superType = superType;
    }

    @Override
    public boolean match(DependencyType type) {
        int result = cache.getOrDefault(type.index, -1);
        if (result < 0) {
            result = classSource.isSuperType(superType, type.getName()).orElse(false) ? 1 : 0;
            cache.put(type.index, result);
        }
        return result != 0;
    }
}
