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

class TCheckedNavigableMap<K, V> extends TCheckedSortedMap<K, V> implements TNavigableMap<K, V> {
    private final TNavigableMap<K, V> map;

    TCheckedNavigableMap(TNavigableMap<K, V> map, Class<K> keyType, Class<V> valueType) {
        super(map, keyType, valueType);
        this.map = map;
    }

    @Override
    public TMap.Entry<K, V> lowerEntry(K key) {
        return map.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        return map.lowerKey(key);
    }

    @Override
    public TMap.Entry<K, V> floorEntry(K key) {
        return map.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        return map.floorKey(key);
    }

    @Override
    public TMap.Entry<K, V> ceilingEntry(K key) {
        return map.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        return map.ceilingKey(key);
    }

    @Override
    public TMap.Entry<K, V> higherEntry(K key) {
        return map.higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
        return map.higherKey(key);
    }

    @Override
    public TMap.Entry<K, V> firstEntry() {
        return map.firstEntry();
    }

    @Override
    public TMap.Entry<K, V> lastEntry() {
        return map.lastEntry();
    }

    @Override
    public TMap.Entry<K, V> pollFirstEntry() {
        return map.pollFirstEntry();
    }

    @Override
    public TMap.Entry<K, V> pollLastEntry() {
        return map.pollLastEntry();
    }

    @Override
    public TNavigableMap<K, V> descendingMap() {
        return new TCheckedNavigableMap<>(map.descendingMap(), keyType, valueType);
    }

    @Override
    public TNavigableSet<K> navigableKeySet() {
        return new TCheckedNavigableSet<>(map.navigableKeySet(), keyType);
    }

    @Override
    public TNavigableSet<K> descendingKeySet() {
        return new TCheckedNavigableSet<>(map.descendingKeySet(), keyType);
    }

    @Override
    public TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new TCheckedNavigableMap<>(map.subMap(fromKey, fromInclusive, toKey, toInclusive), keyType,
                valueType);
    }

    @Override
    public TNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new TCheckedNavigableMap<>(map.headMap(toKey, inclusive), keyType, valueType);
    }

    @Override
    public TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new TCheckedNavigableMap<>(map.tailMap(fromKey, inclusive), keyType, valueType);
    }

    @Override
    public TNavigableMap<K, V> reversed() {
        return descendingMap();
    }
}
