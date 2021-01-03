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

import static java.util.Objects.requireNonNull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface TMap<K, V> {
    interface Entry<K1, V1> {
        K1 getKey();

        V1 getValue();

        V1 setValue(V1 value);

        static <K extends Comparable<? super K>, V> TComparator<TMap.Entry<K, V>> comparingByKey() {
            return (a, b) -> a.getKey().compareTo(b.getKey());
        }

        static <K, V extends Comparable<? super V>> TComparator<TMap.Entry<K, V>> comparingByValue() {
            return (a, b) -> a.getValue().compareTo(b.getValue());
        }

        static <K, V> TComparator<TMap.Entry<K, V>> comparingByKey(TComparator<? super K> comp) {
            return (a, b) -> comp.compare(a.getKey(), b.getKey());
        }

        static <K, V> TComparator<TMap.Entry<K, V>> comparingByValue(TComparator<? super V> comp) {
            return (a, b) -> comp.compare(a.getValue(), b.getValue());
        }
    }

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    default V getOrDefault(K key, V defaultValue) {
        return containsKey(key) ? get(key) : defaultValue;
    }

    V put(K key, V value);

    V remove(Object key);

    default boolean remove(Object key, Object value) {
        if (containsKey(key) && Objects.equals(get(key), value)) {
            remove(key);
            return true;
        }
        return false;
    }

    void putAll(TMap<? extends K, ? extends V> m);

    void clear();

    TSet<K> keySet();

    TCollection<V> values();

    TSet<Entry<K, V>> entrySet();

    default boolean replace(K key, V value, V newValue) {
        if (containsKey(key) && TObjects.equals(get(key), value)) {
            put(key, newValue);
            return true;
        } else {
            return false;
        }
    }

    default V replace(K key, V value) {
        if (containsKey(key)) {
            return put(key, value);
        } else {
            return null;
        }
    }

    default V putIfAbsent(K key, V value) {
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }

        return v;
    }

    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = get(key);
        if (v == null) {
            V newValue = mappingFunction.apply(key);
            if (newValue != null) {
                put(key, newValue);
            }
            return newValue;
        }
        return v;
    }

    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V v = get(key);
        if (v != null) {
            V oldValue = v;
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                put(key, newValue);
            } else {
                remove(key);
            }
            return newValue;
        }
        return null;
    }

    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue = get(key);
        V newValue = remappingFunction.apply(key, oldValue);
        if (oldValue != null) {
            if (newValue != null) {
                put(key, newValue);
            } else {
                remove(key);
            }
        } else if (newValue != null) {
            put(key, newValue);
        }
        return newValue;
    }

    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value
                : remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            remove(key);
        } else {
            put(key, newValue);
        }
        return newValue;
    }

    default void forEach(BiConsumer<? super K, ? super V> action) {
        final TIterator<Entry<K, V>> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<K, V> entry = iterator.next();
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    static <K, V> TMap<K, V> of() {
        return TCollections.emptyMap();
    }

    static <K, V> TMap<K, V> of(K k1, V v1) {
        return new TTemplateCollections.SingleEntryMap<>(k1, v1);
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2) {
        return new TTemplateCollections.TwoEntriesMap<>(k1, v1, k2, v2);
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5),
                entry(k6, v6)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
            K k6, V v6, K k7, V v7) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5),
                entry(k6, v6),
                entry(k7, v7)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
            K k6, V v6, K k7, V v7, K k8, V v8) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5),
                entry(k6, v6),
                entry(k7, v7),
                entry(k8, v8)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
            K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5),
                entry(k6, v6),
                entry(k7, v7),
                entry(k8, v8),
                entry(k9, v9)
        );
    }

    static <K, V> TMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
            K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return new TTemplateCollections.NEtriesMap<>(
                entry(k1, v1),
                entry(k2, v2),
                entry(k3, v3),
                entry(k4, v4),
                entry(k5, v5),
                entry(k6, v6),
                entry(k7, v7),
                entry(k8, v8),
                entry(k9, v9),
                entry(k10, v10)
        );
    }

    @SafeVarargs
    static <K, V> TMap<K, V> ofEntries(TMap.Entry<K, V>... entries) {
        return new TTemplateCollections.NEtriesMap<>(entries);
    }

    static <K, V> TMap.Entry<K, V> entry(K k, V v) {
        return new TTemplateCollections.ImmutableEntry<>(requireNonNull(k), requireNonNull(v));
    }
}
