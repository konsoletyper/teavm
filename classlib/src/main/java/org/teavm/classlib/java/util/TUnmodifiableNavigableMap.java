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

class TUnmodifiableNavigableMap<K, V> extends TUnmodifiableSortedMap<K, V> implements TNavigableMap<K, V> {
    private final TNavigableMap<K, V> map;

    TUnmodifiableNavigableMap(TNavigableMap<K, V> map) {
        super(map);
        this.map = map;
    }

    private static <K, V> TMap.Entry<K, V> immutable(TMap.Entry<K, V> entry) {
        return entry != null ? new TAbstractMap.SimpleImmutableEntry<>(entry) : null;
    }

    @Override
    public TMap.Entry<K, V> lowerEntry(K key) {
        return immutable(map.lowerEntry(key));
    }

    @Override
    public K lowerKey(K key) {
        return map.lowerKey(key);
    }

    @Override
    public TMap.Entry<K, V> floorEntry(K key) {
        return immutable(map.floorEntry(key));
    }

    @Override
    public K floorKey(K key) {
        return map.floorKey(key);
    }

    @Override
    public TMap.Entry<K, V> ceilingEntry(K key) {
        return immutable(map.ceilingEntry(key));
    }

    @Override
    public K ceilingKey(K key) {
        return map.ceilingKey(key);
    }

    @Override
    public TMap.Entry<K, V> higherEntry(K key) {
        return immutable(map.higherEntry(key));
    }

    @Override
    public K higherKey(K key) {
        return map.higherKey(key);
    }

    @Override
    public TMap.Entry<K, V> firstEntry() {
        return immutable(map.firstEntry());
    }

    @Override
    public TMap.Entry<K, V> lastEntry() {
        return immutable(map.lastEntry());
    }

    @Override
    public TNavigableMap<K, V> descendingMap() {
        return new TUnmodifiableNavigableMap<>(map.descendingMap());
    }

    @Override
    public TNavigableSet<K> navigableKeySet() {
        return new TUnmodifiableNavigableSet<>(map.navigableKeySet());
    }

    @Override
    public TNavigableSet<K> descendingKeySet() {
        return new TUnmodifiableNavigableSet<>(map.descendingKeySet());
    }

    @Override
    public TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new TUnmodifiableNavigableMap<>(map.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public TNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new TUnmodifiableNavigableMap<>(map.headMap(toKey, inclusive));
    }

    @Override
    public TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new TUnmodifiableNavigableMap<>(map.tailMap(fromKey, inclusive));
    }

    @Override
    public TNavigableMap<K, V> reversed() {
        return descendingMap();
    }
}
