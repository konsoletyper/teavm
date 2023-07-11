/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TMap;

public interface TConcurrentMap<K, V> extends TMap<K, V> {
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        return result != null ? result : defaultValue;
    }

    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        TIterator<Entry<K, V>> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<K, V> entry = iterator.next();
            K key;
            V value;
            try {
                key = entry.getKey();
                value = entry.getValue();
            } catch (IllegalStateException e) {
                continue;
            }
            action.accept(key, value);
        }
    }

    @Override
    V putIfAbsent(K key, V value);

    @Override
    boolean remove(Object key, Object value);

    @Override
    boolean replace(K key, V oldValue, V newValue);

    @Override
    V replace(K key, V value);

    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        TIterator<Entry<K, V>> iterator = entrySet().iterator();
        main: while (iterator.hasNext()) {
            Entry<K, V> entry = iterator.next();
            while (true) {
                K key;
                V value;
                try {
                    key = entry.getKey();
                    value = entry.getValue();
                } catch (IllegalStateException e) {
                    continue main;
                }
                if (replace(key, value, function.apply(key, value))) {
                    break;
                }
            }
        }
    }

    @Override
    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V oldValue = get(key);
        if (oldValue == null) {
            V newValue = mappingFunction.apply(key);
            return newValue != null ? putIfAbsent(key, newValue) : null;
        } else {
            return oldValue;
        }
    }

    @Override
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        while (true) {
            V oldValue = get(key);
            if (oldValue == null) {
                return null;
            }
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (replace(key, oldValue, newValue)) {
                    return newValue;
                }
            } else {
                if (remove(key, oldValue)) {
                    return null;
                }
            }
        }
    }

    @Override
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V newValue;
        while (true) {
            V oldValue = get(key);
            newValue = remappingFunction.apply(key, oldValue);
            if (oldValue != null) {
                if (newValue != null) {
                    if (replace(key, oldValue, newValue)) {
                        break;
                    }
                } else {
                    if (remove(key, oldValue)) {
                        break;
                    }
                }
            } else if (newValue != null) {
                if (putIfAbsent(key, newValue) == null) {
                    break;
                }
            } else {
                break;
            }
        }
        return newValue;
    }

    @Override
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V newValue;
        while (true) {
            V oldValue = get(key);
            newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
            if (newValue == null) {
                if (oldValue != null) {
                    remove(key, oldValue);
                } else {
                    break;
                }
            } else {
                if (oldValue == null) {
                    if (putIfAbsent(key, newValue) == null) {
                        break;
                    }
                } else {
                    if (replace(key, oldValue, newValue)) {
                        break;
                    }
                }
            }
        }
        return newValue;
    }
}
