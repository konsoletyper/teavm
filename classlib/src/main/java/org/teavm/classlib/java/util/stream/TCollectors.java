/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TCollectors {
    private TCollectors() {
    }

    public static <T, C extends Collection<T>> TCollector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return TCollector.of(collectionFactory, Collection::add, (a, b) -> {
            a.addAll(b);
            return a;
        });
    }

    public static <T> TCollector<T, ?, List<T>> toList() {
        return toCollection(ArrayList::new);
    }

    public static <T> TCollector<T, ?, Set<T>> toSet() {
        return toCollection(HashSet::new);
    }

    public static TCollector<CharSequence, ?, String> joining() {
        return TCollector.of(StringBuilder::new, StringBuilder::append, StringBuilder::append,
                StringBuilder::toString);
    }

    public static TCollector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static TCollector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix,
            CharSequence suffix) {
        BiConsumer<StringBuilder, CharSequence> accumulator = (sb, item) -> {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(item);
        };
        BinaryOperator<StringBuilder> combiner = (a, b) -> {
            if (a.length() > 0) {
                a.append(delimiter);
            }
            return a.append(b);
        };
        return TCollector.of(StringBuilder::new, accumulator, combiner,
                sb -> sb.insert(0, prefix).append(suffix).toString());
    }

    public static <T, K, U> TCollector<T, ?, Map<K,U>> toMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return TCollector.of(HashMap::new,
                uniqKeysMapAccumulator(keyMapper, valueMapper),
                uniqKeysMapMerger(),
                TCollector.Characteristics.IDENTITY_FINISH);
    }

    public static <T, K, U> TCollector<T, ?, Map<K,U>> toMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <T, K, U, M extends Map<K, U>> TCollector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction,
            Supplier<M> mapFactory) {
        BiConsumer<M, T> accumulator
                = (map, element) -> map.merge(keyMapper.apply(element),
                valueMapper.apply(element), mergeFunction);
        return TCollector.of(mapFactory, accumulator, mapMerger(mergeFunction), TCollector.Characteristics.IDENTITY_FINISH);
    }

    private static <K, V, M extends Map<K,V>> BinaryOperator<M> uniqKeysMapMerger() {
        return (m1, m2) -> {
            for (Map.Entry<K,V> e : m2.entrySet()) {
                K k = e.getKey();
                V v = Objects.requireNonNull(e.getValue());
                V u = m1.putIfAbsent(k, v);
                if (u != null) {
                    throw duplicateKeyException(k, u, v);
                }
            }
            return m1;
        };
    }

    private static <K, V, M extends Map<K,V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K,V> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            }
            return m1;
        };
    }

    private static <T, K, V> BiConsumer<Map<K, V>, T> uniqKeysMapAccumulator(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            K k = keyMapper.apply(element);
            V v = Objects.requireNonNull(valueMapper.apply(element));
            V u = map.putIfAbsent(k, v);
            if (u != null) {
                throw duplicateKeyException(k, u, v);
            }
        };
    }

    private static IllegalStateException duplicateKeyException(
            Object k, Object u, Object v) {
        return new IllegalStateException(String.format(
                "Duplicate key %s (attempted merging values %s and %s)",
                k, u, v));
    }
}
