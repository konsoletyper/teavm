/*
 *  Copyright 2026 Gregory Mitchell.
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
    private final TSortedMap<K, V> map;

    TUnmodifiableSortedMap(TSortedMap<K, V> map) {
        super(map);
        this.map = map;
    }

    @Override
    public TComparator<? super K> comparator() {
        return map.comparator();
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        return new TUnmodifiableSortedMap<>(map.subMap(fromKey, toKey));
    }

    @Override
    public TSortedMap<K, V> headMap(K toKey) {
        return new TUnmodifiableSortedMap<>(map.headMap(toKey));
    }

    @Override
    public TSortedMap<K, V> tailMap(K fromKey) {
        return new TUnmodifiableSortedMap<>(map.tailMap(fromKey));
    }

    @Override
    public K firstKey() {
        return map.firstKey();
    }

    @Override
    public K lastKey() {
        return map.lastKey();
    }
}
