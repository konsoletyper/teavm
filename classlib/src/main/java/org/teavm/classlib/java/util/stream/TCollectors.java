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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static <T> TCollector<T, ?, List<T>> toUnmodifiableList() {
        return collectingAndThen(toList(), Collections::unmodifiableList);
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

    public static <E, K> TCollector<E, ?, Map<K, List<E>>> groupingBy(Function<? super E, ? extends K> keyExtractor) {
        return groupingBy(keyExtractor, toList());
    }

    public static <E, K, V, I> TCollector<E, ?, Map<K, V>> groupingBy(
            Function<? super E, ? extends K> keyExtractor,
            TCollector<? super E, I, V> downstream) {
        return groupingBy(keyExtractor, HashMap::new, downstream);
    }

    public static <E, K, V, I, M extends Map<K, V>> TCollector<E, ?, M> groupingBy(
            Function<? super E, ? extends K> keyExtractor,
            Supplier<M> mapFactory,
            TCollector<? super E, I, V> downstream) {
        BiConsumer<Map<K, I>, E> mapAppender = (m, t) -> {
            K key = keyExtractor.apply(t);
            I container = m.computeIfAbsent(key, k -> downstream.supplier().get());
            downstream.accumulator().accept(container, t);
        };
        BinaryOperator<Map<K, I>> mapMerger = (m1, m2) -> {
            for (Map.Entry<K, I> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), downstream.combiner());
            }
            return m1;
        };

        if (downstream.characteristics().contains(TCollector.Characteristics.IDENTITY_FINISH)) {
            return TCollector.of(castFactory(mapFactory), mapAppender, mapMerger,
                    castFunction(Function.identity()), TCollector.Characteristics.IDENTITY_FINISH);
        } else {
            Function<I, I> replacer = castFunction(downstream.finisher());
            Function<Map<K, I>, M> finisher = toReplace -> {
                toReplace.replaceAll((k, v) -> replacer.apply(v));
                return (M) toReplace;
            };
            return TCollector.of(castFactory(mapFactory), mapAppender, mapMerger, finisher);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A, C> Supplier<A> castFactory(Supplier<C> supp) {
        return (Supplier<A>) supp;
    }

    @SuppressWarnings("unchecked")
    private static <A, B, C, D> Function<A, B> castFunction(Function<C, D> func) {
        return (Function<A, B>) func;
    }

    public static <T, A, R, K> TCollector<T, A, K> collectingAndThen(
            TCollector<T, A, R> downstream,
            Function<R, K> finisher) {

        EnumSet<TCollector.Characteristics> newCharacteristics = EnumSet.copyOf(downstream.characteristics());
        newCharacteristics.remove(TCollector.Characteristics.IDENTITY_FINISH);

        return new TCollectorImpl<>(downstream.supplier(),
                downstream.accumulator(),
                downstream.combiner(),
                downstream.finisher().andThen(finisher),
                newCharacteristics);
    }

    private static class Reducer<T> {
        private final BinaryOperator<T> op;
        private boolean present;
        private T value;

        private Reducer(BinaryOperator<T> op) {
            this.op = op;
        }

        private Reducer(BinaryOperator<T> op, T value) {
            this.op = op;
            consume(value);
        }

        private void consume(T t) {
            if (present) {
                value = op.apply(value, t);
            } else {
                value = t;
                present = true;
            }
        }

        private Reducer<T> merge(Reducer<T> other) {
            if (other.present) {
                consume(other.value);
            }
            return this;
        }

        private Optional<T> getOpt() {
            return present ? Optional.of(value) : Optional.empty();
        }

        private T get() {
            return value;
        }
    }

    public static <T> TCollector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        return TCollector.of(() -> new Reducer<>(op), Reducer::consume, Reducer::merge, Reducer::getOpt);
    }

    public static <T> TCollector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return TCollector.of(() -> new Reducer<>(op, identity), Reducer::consume, Reducer::merge, Reducer::get);
    }

    public static <T, U> TCollector<T, ?, U> reducing(U identity,
            Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
        return TCollector.of(() -> new Reducer<>(op, identity),
                (red, t) -> red.consume(mapper.apply(t)), Reducer::merge, Reducer::get);
    }

    public static <T> TCollector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }

    public static <T> TCollector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }

    public static <T> TCollector<T, ?, Long> counting() {
        return reducing(0L, e -> 1L, Long::sum);
    }
}
