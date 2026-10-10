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

class TCheckedSortedMap<K, V> extends TCheckedMap<K, V> implements TSortedMap<K, V> {
    private final TSortedMap<K, V> map;
    final Class<K> keyType;
    final Class<V> valueType;

    TCheckedSortedMap(TSortedMap<K, V> map, Class<K> keyType, Class<V> valueType) {
        super(map, keyType, valueType);
        this.map = map;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public TComparator<? super K> comparator() {
        return map.comparator();
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        return new TCheckedSortedMap<>(map.subMap(fromKey, toKey), keyType, valueType);
    }

    @Override
    public TSortedMap<K, V> headMap(K toKey) {
        return new TCheckedSortedMap<>(map.headMap(toKey), keyType, valueType);
    }

    @Override
    public TSortedMap<K, V> tailMap(K fromKey) {
        return new TCheckedSortedMap<>(map.tailMap(fromKey), keyType, valueType);
    }

    @Override
    public K firstKey() {
        return map.firstKey();
    }

    @Override
    public K lastKey() {
        return map.lastKey();
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        TSequencedMap<K, V> reversed = map.reversed();
        return reversed instanceof TSortedMap
                ? new TCheckedSortedMap<>((TSortedMap<K, V>) reversed, keyType, valueType)
                : reversed;
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        return map.sequencedKeySet();
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        return map.sequencedValues();
    }

    @Override
    public TSequencedSet<TMap.Entry<K, V>> sequencedEntrySet() {
        return map.sequencedEntrySet();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
