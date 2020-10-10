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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TObjects;

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

    public static <E, K, V> TCollector<E, ?, Map<K, V>> toMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        return TCollector.of(HashMap::new,
                (map, el) -> {
                    K k = keyMapper.apply(el);
                    V newV = TObjects.requireNonNull(valueMapper.apply(el));
                    V oldV = map.putIfAbsent(k, newV);
                    if (oldV != null) {
                        throw new IllegalStateException(
                                "Key " + k + " corresponds to values " + oldV + " and " + newV);
                    }
                },
                (m1, m2) -> {
                    for (Map.Entry<K, V> e : m2.entrySet()) {
                        V newV = TObjects.requireNonNull(e.getValue());
                        V oldV = m1.putIfAbsent(e.getKey(), newV);
                        if (oldV != null) {
                            throw new IllegalStateException(
                                    "Key " + e.getKey() + " corresponds to values " + oldV + " and " + newV);
                        }
                    }
                    return m1;
                },
                TCollector.Characteristics.IDENTITY_FINISH);
    }

    public static <E, K, V> TCollector<E, ?, Map<K, V>> toMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <E, K, V, M extends Map<K, V>> TCollector<E, ?, M> toMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper, BinaryOperator<V> mergeFunction, Supplier<M> mapFactory) {
        return TCollector.of(mapFactory,
                (map, el) -> map.merge(keyMapper.apply(el), valueMapper.apply(el), mergeFunction),
                (m1, m2) -> {
                    for (Map.Entry<K, V> e : m2.entrySet()) {
                        m1.merge(e.getKey(), e.getValue(), mergeFunction);
                    }
                    return m1;
                },
                TCollector.Characteristics.IDENTITY_FINISH);
    }
}
