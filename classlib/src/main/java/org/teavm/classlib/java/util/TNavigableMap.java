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

/**
 *
 * @author Alexey Andreev
 * @param <K>
 * @param <V>
 */
public interface TNavigableMap<K, V> extends TSortedMap<K, V> {
    Entry<K, V> lowerEntry(K key);

    K lowerKey(K key);

    Entry<K, V> floorEntry(K key);

    K floorKey(K key);

    Entry<K, V> ceilingEntry(K key);

    K ceilingKey(K key);

    Entry<K, V> higherEntry(K key);

    K higherKey(K key);

    @Override
    Entry<K, V> firstEntry();

    @Override
    Entry<K, V> lastEntry();

    @Override
    Entry<K, V> pollFirstEntry();

    @Override
    Entry<K, V> pollLastEntry();

    TNavigableMap<K, V> descendingMap();

    TNavigableSet<K> navigableKeySet();

    TNavigableSet<K> descendingKeySet();

    TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    TNavigableMap<K, V> headMap(K toKey, boolean inclusive);

    TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);

    @Override
    default TSequencedSet<K> sequencedKeySet() {
        return navigableKeySet();
    }

    @Override
    default TNavigableMap<K, V> reversed() {
        return descendingMap();
    }
}
