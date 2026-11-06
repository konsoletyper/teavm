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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableMap<K, V> extends TAbstractMap<K, V> {
    final TMap<? extends K, ? extends V> underlyingMap;
    private TSet<Entry<K, V>> entrySetCache;

    TUnmodifiableMap(TMap<? extends K, ? extends V> underlyingMap) {
        this.underlyingMap = underlyingMap;
    }

    TSet<K> createKeySet() {
        return new TUnmodifiableSet<>(underlyingMap.keySet());
    }

    TCollection<V> createValues() {
        return new TUnmodifiableCollection<>(underlyingMap.values());
    }

    TSet<Entry<K, V>> createEntrySet() {
        return new TUnmodifiableEntrySet<>(underlyingMap.entrySet());
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        if (entrySetCache == null) {
            entrySetCache = createEntrySet();
        }
        return entrySetCache;
    }

    @Override
    public TSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = createKeySet();
        }
        return cachedKeySet;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = createValues();
        }
        return cachedValues;
    }

    @Override
    public boolean containsKey(Object key) {
        return underlyingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return underlyingMap.containsValue(value);
    }

    @Override
    public int size() {
        return underlyingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return underlyingMap.isEmpty();
    }

    @Override
    public V get(Object key) {
        return underlyingMap.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        return ((TMap<K, V>) underlyingMap).getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        underlyingMap.forEach(action);
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V replace(K key, V value) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || underlyingMap.equals(o);
    }

    @Override
    public int hashCode() {
        return underlyingMap.hashCode();
    }

    @Override
    public String toString() {
        return underlyingMap.toString();
    }
}
