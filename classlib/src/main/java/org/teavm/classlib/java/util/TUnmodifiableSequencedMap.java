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

import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableSequencedMap<K, V> extends TUnmodifiableMap<K, V> implements TSequencedMap<K, V> {
    TUnmodifiableSequencedMap(TSequencedMap<? extends K, ? extends V> underlyingMap) {
        super(underlyingMap);
    }

    private TSequencedMap<? extends K, ? extends V> underlyingSequencedMap() {
        return (TSequencedMap<? extends K, ? extends V>) underlyingMap;
    }

    @Override
    TSet<K> createKeySet() {
        return new TUnmodifiableSequencedSet<>(underlyingSequencedMap().sequencedKeySet());
    }

    @Override
    TCollection<V> createValues() {
        return new TUnmodifiableSequencedCollection<>(underlyingSequencedMap().sequencedValues());
    }

    @Override
    TSet<Entry<K, V>> createEntrySet() {
        return new TUnmodifiableSequencedEntrySet<>(underlyingSequencedMap().sequencedEntrySet());
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        return new TUnmodifiableSequencedMap<>(underlyingSequencedMap().reversed());
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        return (TSequencedSet<K>) keySet();
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        return (TSequencedCollection<V>) values();
    }

    @Override
    public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
        return (TSequencedSet<Entry<K, V>>) entrySet();
    }

    @Override
    public Entry<K, V> firstEntry() {
        var entry = underlyingSequencedMap().firstEntry();
        return entry != null ? TUnmodifiableEntrySet.wrap(entry) : null;
    }

    @Override
    public Entry<K, V> lastEntry() {
        var entry = underlyingSequencedMap().lastEntry();
        return entry != null ? TUnmodifiableEntrySet.wrap(entry) : null;
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V putFirst(K k, V v) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V putLast(K k, V v) {
        throw new TUnsupportedOperationException();
    }
}
