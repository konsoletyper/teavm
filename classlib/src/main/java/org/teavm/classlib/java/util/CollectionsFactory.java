/*
 *  Copyright 2020 Alexey Andreev.
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

import static java.util.Objects.requireNonNull;
import java.util.RandomAccess;

/**
 * Factory-methods for List/Set/Map.of(...).
 */
class CollectionsFactory {

    private CollectionsFactory() {
    }

    @SafeVarargs
    static <E> TList<E> createList(E... elements) {
        if (elements.length == 0) {
            return TCollections.emptyList();
        }

        // don't permit null elements
        for (E element : elements) {
            requireNonNull(element, "element");
        }

        return new ImmutableArrayList<>(elements);
    }

    @SafeVarargs
    static <E> TSet<E> createSet(E... elements) {
        if (elements.length == 0) {
            return TCollections.emptySet();
        }

        // don't permit null or duplicate elements
        final TSet<E> set = new THashSet<>();
        for (E element : elements) {
            if (!set.add(requireNonNull(element, "element"))) {
                throw new IllegalArgumentException("duplicate element: " + element);
            }
        }

        return TCollections.unmodifiableSet(set);
    }

    @SafeVarargs
    static <K, V> TMap<K, V> createMap(TMap.Entry<K, V>... entries) {
        if (entries.length == 0) {
            return TCollections.emptyMap();
        }

        // don't permit null or duplicate keys and don't permit null values
        final TMap<K, V> map = new THashMap<>();
        for (TMap.Entry<K, V> entry : entries) {
            if (map.put(requireNonNull(entry.getKey(), "key"), requireNonNull(entry.getValue(), "value")) != null) {
                throw new IllegalArgumentException("duplicate key: " + entry.getKey());
            }
        }

        return TCollections.unmodifiableMap(map);
    }

    static class ImmutableArrayList<T> extends TAbstractList<T> implements RandomAccess {
        private final T[] list;

        public ImmutableArrayList(T[] list) {
            this.list = list;
        }

        @Override
        public T get(int index) {
            return list[index];
        }

        @Override
        public int size() {
            return list.length;
        }
    }

}
