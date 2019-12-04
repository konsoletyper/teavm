/*
 *  Copyright 2019 wolfie.
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

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface TConcurrentMap<K, V> extends Map<K, V> {
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return ((v = get(key)) != null) ? v : defaultValue;
    }

    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch(IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                continue;
            }
            action.accept(k, v);
        }
    }

    V putIfAbsent(K key, V value);

    boolean remove(Object key, Object value);

    boolean replace(K key, V oldValue, V newValue);

    V replace(K key, V value);

    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach((k,v) -> {
            while(!replace(k, v, function.apply(k, v))) {
                /** v changed or k is gone */
                if ( (v = get(k)) == null) {
                    /** k is no longer in the map. */
                    break;
                }
            }
        });
    }

    @Override
    default V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        V newValue;
        return ((v = get(key)) == null &&
                (newValue = mappingFunction.apply(key)) != null &&
                (v = putIfAbsent(key, newValue)) == null) ? newValue : v;
    }

    @Override
    default V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        while((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (replace(key, oldValue, newValue)) {
                    return newValue;
                }
            } else if (remove(key, oldValue)) {
                return null;
            }
        }
        return oldValue;
    }

    @Override
    default V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        for(;;) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                /** delete mapping */
                if (oldValue != null || containsKey(key)) {
                    /** something to remove */
                    if (remove(key, oldValue)) {
                        // removed the old value as expected */
                        return null;
                    }

                    /** some other value replaced old value. try again. */
                    oldValue = get(key);
                } else {
                    /** nothing to do. Leave things as they were. */
                    return null;
                }
            } else {
                /** add or replace old mapping */
                if (oldValue != null) {
                    /** replace */
                    if (replace(key, oldValue, newValue)) {
                        // replaced as expected. */
                        return newValue;
                    }

                    /** some other value replaced old value. try again. */
                    oldValue = get(key);
                } else {
                    /** add (replace if oldValue was null) */
                    if ((oldValue = putIfAbsent(key, newValue)) == null) {
                        // replaced */
                        return newValue;
                    }

                    /** some other value replaced old value. try again. */
                }
            }
        }
    }

    @Override
    default V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        for (;;) {
            if (oldValue != null) {
                V newValue = remappingFunction.apply(oldValue, value);
                if (newValue != null) {
                    if (replace(key, oldValue, newValue)) {
                        return newValue;
                    }
                } else if (remove(key, oldValue)) {
                    return null;
                }
                oldValue = get(key);
            } else {
                if ((oldValue = putIfAbsent(key, value)) == null) {
                    return value;
                }
            }
        }
    }
}