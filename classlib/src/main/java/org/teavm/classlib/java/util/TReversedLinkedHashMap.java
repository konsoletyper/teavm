/*
 *  Copyright 2023 ihromant.
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

class TReversedLinkedHashMap<K, V> extends TAbstractMap<K, V> implements TSequencedMap<K, V> {
    private final TLinkedHashMap<K, V> base;

    TReversedLinkedHashMap(TLinkedHashMap<K, V> base) {
        this.base = base;
    }

    @Override
    public boolean equals(Object o) {
        return base.equals(o);
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return base.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return base.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return base.get(key);
    }

    @Override
    public V put(K key, V value) {
        return base.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return base.remove(key);
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        base.putAll(m);
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public TSet<K> keySet() {
        return base.sequencedKeySet().reversed();
    }

    @Override
    public TCollection<V> values() {
        return base.sequencedValues().reversed();
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        return base.sequencedEntrySet().reversed();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return base.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = base.tail;
            do {
                action.accept(entry.key, entry.value);
                entry = entry.chainBackward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = base.tail;
            do {
                entry.value = function.apply(entry.key, entry.value);
                entry = entry.chainBackward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return base.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return base.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return base.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return base.replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return base.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return base.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return base.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return base.merge(key, value, remappingFunction);
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        return base;
    }

    @Override
    public TMap.Entry<K, V> firstEntry() {
        return base.lastEntry();
    }

    @Override
    public TMap.Entry<K, V> lastEntry() {
        return base.firstEntry();
    }

    @Override
    public TMap.Entry<K, V> pollFirstEntry() {
        return base.pollLastEntry();
    }

    @Override
    public TMap.Entry<K, V> pollLastEntry() {
        return base.pollFirstEntry();
    }

    @Override
    public V putFirst(K k, V v) {
        return base.putLast(k, v);
    }

    @Override
    public V putLast(K k, V v) {
        return base.putFirst(k, v);
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        return new TLinkedHashMapKeySet<>(base, true);
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        return new TLinkedHashMapValues<>(base, true);
    }

    @Override
    public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
        return new TLinkedHashMapEntrySet<>(base, true);
    }
}
