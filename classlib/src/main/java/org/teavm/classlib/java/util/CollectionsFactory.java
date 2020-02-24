/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.teavm.classlib.java.util.TObjects.requireNonNull;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TNullPointerException;

/**
 * Factory-methods for List/Set/Map.of(...).
 */
class CollectionsFactory {

    private CollectionsFactory() {
    }

    /**
     * Create an immutable list for the {@code List.of(...)} factory methods.
     *
     * @throws TNullPointerException if any element is null
     */
    @SafeVarargs
    static <E> TList<E> createList(E... elements) {
        if (elements == null || elements.length == 0) {
            return TCollections.emptyList();
        }

        final TList<E> list = new TArrayList<>();
        for (E element : elements) {
            list.add(requireNonNull(element, "element"));
        }

        return TCollections.unmodifiableList(list);
    }

    /**
     * Create an immutable set for the {@code Set.of(...)} factory methods.
     *
     * @throws TNullPointerException     if any element is null
     * @throws TIllegalArgumentException if duplicate elements are given
     */
    @SafeVarargs
    static <E> TSet<E> createSet(E... elements) {
        if (elements == null || elements.length == 0) {
            return TCollections.emptySet();
        }

        final TSet<E> set = new THashSet<>();
        for (E element : elements) {
            if (!set.add(requireNonNull(element, "element"))) {
                throw new TIllegalArgumentException("duplicate element: " + element);
            }
        }

        return TCollections.unmodifiableSet(set);
    }

    /**
     * Create an immutable set for the {@code Map.of(...)} and {@code Map.ofEntries(...)} factory methods.
     *
     * @throws TNullPointerException     if any key or value is null
     * @throws TIllegalArgumentException if duplicate keys are given
     */
    @SafeVarargs
    static <K, V> TMap<K, V> createMap(TMap.Entry<K, V>... entries) {
        if (entries == null || entries.length == 0) {
            return TCollections.emptyMap();
        }

        final TMap<K, V> map = new THashMap<>();
        for (TMap.Entry<K, V> entry : entries) {
            if (map.put(requireNonNull(entry.getKey(), "key"), requireNonNull(entry.getValue(), "value")) != null) {
                throw new TIllegalArgumentException("duplicate key: " + entry.getKey());
            }
        }

        return TCollections.unmodifiableMap(map);
    }
}
