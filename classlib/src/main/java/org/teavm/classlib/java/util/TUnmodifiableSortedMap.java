/*
 *  Copyright 2026 Alexey Andreev.
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

class TUnmodifiableSortedMap<K, V> extends TUnmodifiableSequencedMap<K, V> implements TSortedMap<K, V> {
    TUnmodifiableSortedMap(TSortedMap<K, ? extends V> underlyingMap) {
        super(underlyingMap);
    }

    @SuppressWarnings("unchecked")
    private TSortedMap<K, ? extends V> underlyingSortedMap() {
        return (TSortedMap<K, ? extends V>) underlyingMap;
    }

    @Override
    public TComparator<? super K> comparator() {
        return underlyingSortedMap().comparator();
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        return new TUnmodifiableSortedMap<>(underlyingSortedMap().subMap(fromKey, toKey));
    }

    @Override
    public TSortedMap<K, V> headMap(K toKey) {
        return new TUnmodifiableSortedMap<>(underlyingSortedMap().headMap(toKey));
    }

    @Override
    public TSortedMap<K, V> tailMap(K fromKey) {
        return new TUnmodifiableSortedMap<>(underlyingSortedMap().tailMap(fromKey));
    }

    @Override
    public K firstKey() {
        return underlyingSortedMap().firstKey();
    }

    @Override
    public K lastKey() {
        return underlyingSortedMap().lastKey();
    }
}
