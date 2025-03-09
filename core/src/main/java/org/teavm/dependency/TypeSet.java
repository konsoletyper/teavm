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

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Predicate;

class TypeSet {
    static final int SMALL_TYPES_THRESHOLD = 3;
    static final DependencyType[] EMPTY_TYPES = new DependencyType[0];
    private DependencyAnalyzer dependencyAnalyzer;
    private int[] smallTypes;
    private BitSet types;
    private int typesCount;

    TypeSet(DependencyAnalyzer dependencyAnalyzer) {
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    void addType(DependencyType type) {
        if (types == null) {
            if (smallTypes == null) {
                smallTypes = new int[] { type.index };
                return;
            }
        }
        if (smallTypes != null) {
            if (smallTypes.length == SMALL_TYPES_THRESHOLD) {
                types = new BitSet(dependencyAnalyzer.types.size() * 2);
                for (int existingType : smallTypes) {
                    types.set(existingType);
                }
                typesCount = smallTypes.length;
                smallTypes = null;
            } else {
                smallTypes = Arrays.copyOf(smallTypes, smallTypes.length + 1);
                smallTypes[smallTypes.length - 1] = type.index;
                return;
            }
        }
        types.set(type.index);
        typesCount++;
    }

    DependencyType[] getTypes() {
        if (this.types != null) {
            DependencyType[] types = new DependencyType[this.types.cardinality()];
            int j = 0;
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                types[j++] = type;
            }
            return types;
        } else if (this.smallTypes != null) {
            DependencyType[] types = new DependencyType[smallTypes.length];
            for (int i = 0; i < types.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                types[i] = type;
            }
            return types;
        } else {
            return EMPTY_TYPES;
        }
    }

    boolean hasMoreTypesThan(int limit, Predicate<DependencyType> filter) {
        if (this.types != null) {
            if (filter == null) {
                return this.types.cardinality() > limit;
            }
            for (int index = this.types.nextSetBit(0); index >= 0; index = this.types.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                if (filter.test(type)) {
                    if (--limit < 0) {
                        return true;
                    }
                }
            }
            return false;
        } else if (this.smallTypes != null) {
            if (this.smallTypes.length <= limit) {
                return false;
            }
            if (filter == null) {
                return true;
            }
            for (int i = 0; i < smallTypes.length; ++i) {
                DependencyType type = dependencyAnalyzer.types.get(smallTypes[i]);
                if (filter.test(type)) {
                    if (--limit < 0) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    boolean hasType(DependencyType type) {
        if (smallTypes != null) {
            for (int i = 0; i < smallTypes.length; ++i) {
                if (smallTypes[i] == type.index) {
                    return true;
                }
            }
            return false;
        }
        return types != null && types.get(type.index);
    }

    boolean hasAnyType() {
        return types != null || smallTypes != null;
    }

    TypeSet copy(DependencyNode origin) {
        TypeSet result = new TypeSet(dependencyAnalyzer);
        result.types = types != null ? (BitSet) types.clone() : null;
        result.smallTypes = smallTypes;
        result.typesCount = typesCount;
        return result;
    }

    int typeCount() {
        return smallTypes != null ? smallTypes.length : types != null ? typesCount : 0;
    }
}
