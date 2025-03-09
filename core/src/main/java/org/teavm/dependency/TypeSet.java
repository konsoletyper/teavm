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
    Object data;

    TypeSet(DependencyAnalyzer dependencyAnalyzer) {
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    void addType(int type) {
        if (data == null) {
            data = new int[] { type };
            return;
        } else if (data instanceof int[]) {
            var array = (int[]) data;
            if (array.length == SMALL_TYPES_THRESHOLD) {
                var bitSet = new BitSet(dependencyAnalyzer.types.size() * 2);
                for (int existingType : array) {
                    bitSet.set(existingType);
                }
                data = bitSet;
            } else {
                array = Arrays.copyOf(array, array.length + 1);
                array[array.length - 1] = type;
                data = array;
                return;
            }
        }
        var bitSet = (BitSet) data;
        bitSet.set(type);
    }

    void addTypes(int[] newTypes) {
        if (newTypes.length > SMALL_TYPES_THRESHOLD) {
            if (data == null) {
                data = new BitSet();
            } else if (data instanceof int[]) {
                var array = (int[]) data;
                var bitSet = new BitSet();
                for (var typeIndex : array) {
                    bitSet.set(typeIndex);
                }
                data = bitSet;
            }
        } else {
            if (data == null) {
                data = newTypes.clone();
                return;
            }

            if (data instanceof int[]) {
                var array = (int[]) data;
                var sizeToAdd = newTypes.length;
                for (var type : newTypes) {
                    for (var typeIndex : array) {
                        if (typeIndex == type) {
                            --sizeToAdd;
                            break;
                        }
                    }
                }
                if (sizeToAdd == 0) {
                    return;
                }
                if (array.length + sizeToAdd < SMALL_TYPES_THRESHOLD) {
                    var i = array.length;
                    var oldLength = i;
                    array = Arrays.copyOf(array, array.length + sizeToAdd);
                    outer:
                    for (var newType : newTypes) {
                        for (var j = 0; j < oldLength; ++j) {
                            if (newType == array[j]) {
                                continue outer;
                            }
                        }
                        array[i++] = newType;
                    }
                    data = array;
                    return;
                } else {
                    var bitSet = new BitSet();
                    for (var typeIndex : array) {
                        bitSet.set(typeIndex);
                    }
                    data = bitSet;
                }
            }
        }

        var bitSet = (BitSet) data;
        for (var newType : newTypes) {
            bitSet.set(newType);
        }
    }

    void addTypes(BitSet newTypes) {
        if (data == null) {
            data = new BitSet();
        } else if (data instanceof int[]) {
            var array = (int[]) data;
            var bitSet = new BitSet();
            for (var typeIndex : array) {
                bitSet.set(typeIndex);
            }
            data = bitSet;
        }

        var bitSet = (BitSet) data;
        bitSet.or(newTypes);
    }

    void addTypes(TypeSet other) {
        if (other == null) {
            return;
        }
        if (other.data instanceof int[]) {
            addTypes((int[]) other.data);
        } else if (other.data instanceof BitSet) {
            addTypes((BitSet) other.data);
        }
    }

    DependencyType[] getTypes() {
        if (data instanceof BitSet) {
            var bitSet = (BitSet) data;
            var types = new DependencyType[bitSet.cardinality()];
            int j = 0;
            for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
                var type = dependencyAnalyzer.types.get(index);
                types[j++] = type;
            }
            return types;
        } else if (data instanceof int[]) {
            var array = (int[]) data;
            var types = new DependencyType[array.length];
            for (int i = 0; i < types.length; ++i) {
                types[i] = dependencyAnalyzer.types.get(array[i]);
            }
            return types;
        } else {
            return EMPTY_TYPES;
        }
    }

    boolean hasMoreTypesThan(int limit, Predicate<DependencyType> filter) {
        if (data instanceof BitSet) {
            var bitSet = (BitSet) data;
            if (filter == null) {
                return bitSet.cardinality() > limit;
            }
            for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
                DependencyType type = dependencyAnalyzer.types.get(index);
                if (filter.test(type)) {
                    if (--limit < 0) {
                        return true;
                    }
                }
            }
            return false;
        } else if (data instanceof int[]) {
            var array = (int[]) data;
            if (array.length <= limit) {
                return false;
            }
            if (filter == null) {
                return true;
            }
            for (int i = 0; i < array.length; ++i) {
                var type = dependencyAnalyzer.types.get(array[i]);
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
        return hasType(type.index);
    }

    boolean hasType(int type) {
        if (data instanceof int[]) {
            var array = (int[]) data;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] == type) {
                    return true;
                }
            }
            return false;
        } else if (data instanceof BitSet) {
            var bitSet = (BitSet) data;
            return bitSet.get(type);
        } else {
            return false;
        }
    }

    boolean hasAnyType() {
        return data != null;
    }
}
