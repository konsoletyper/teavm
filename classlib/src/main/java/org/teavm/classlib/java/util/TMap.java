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

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author Alexey Andreev
 * @param <K>
 * @param <V>
 */
public interface TMap<K, V> {
    interface Entry<K1, V1> {
        K1 getKey();

        V1 getValue();

        V1 setValue(V1 value);
    }

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

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
}
