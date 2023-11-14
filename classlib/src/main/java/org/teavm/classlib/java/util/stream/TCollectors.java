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
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import org.teavm.classlib.java.util.TDoubleSummaryStatistics;
import org.teavm.classlib.java.util.TIntSummaryStatistics;
import org.teavm.classlib.java.util.TLongSummaryStatistics;
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

    public static <T> TCollector<T, ?, Set<T>> toUnmodifiableSet() {
        return collectingAndThen(toSet(), Collections::unmodifiableSet);
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

    public static <T, A, R, K> TCollector<T, ?, K> mapping(Function<? super T, ? extends A> mapper,
            TCollector<? super A, R, K> downstream) {
        BiConsumer<R, ? super A> downstreamAccumulator = downstream.accumulator();
        return TCollector.of(downstream.supplier(), (R r, T t) -> downstreamAccumulator.accept(r, mapper.apply(t)),
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics().toArray(TCollector.Characteristics[]::new));
    }

    public static <T, A, R, K> TCollector<T, ?, K> flatMapping(
            Function<? super T, ? extends TStream<? extends A>> mapper, TCollector<? super A, R, K> downstream) {
        BiConsumer<R, ? super A> downstreamAccumulator = downstream.accumulator();
        return TCollector.of(downstream.supplier(),
                (R r, T t) -> {
                    TStream<? extends A> result = mapper.apply(t);
                    if (result != null) {
                        result.forEach(a -> downstreamAccumulator.accept(r, a));
                    }
                },
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics().toArray(TCollector.Characteristics[]::new));
    }

    public static <T, A, R> TCollector<T, ?, R> filtering(Predicate<? super T> predicate,
            TCollector<? super T, A, R> downstream) {
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        return TCollector.of(downstream.supplier(),
                (r, t) -> {
                    if (predicate.test(t)) {
                        downstreamAccumulator.accept(r, t);
                    }
                },
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics().toArray(TCollector.Characteristics[]::new));
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
                    for (var e : m2.entrySet()) {
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

    public static <E, K, V> TCollector<E, ?, Map<K, V>> toUnmodifiableMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        return collectingAndThen(toMap(keyMapper, valueMapper), Collections::unmodifiableMap);
    }

    public static <E, K, V> TCollector<E, ?, Map<K, V>> toMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <E, K, V> TCollector<E, ?, Map<K, V>> toUnmodifiableMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return collectingAndThen(toMap(keyMapper, valueMapper, mergeFunction), Collections::unmodifiableMap);
    }

    public static <E, K, V, M extends Map<K, V>> TCollector<E, ?, M> toMap(Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper, BinaryOperator<V> mergeFunction, Supplier<M> mapFactory) {
        return TCollector.of(mapFactory,
                (map, el) -> map.merge(keyMapper.apply(el), valueMapper.apply(el), mergeFunction),
                (m1, m2) -> {
                    for (var e : m2.entrySet()) {
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
            for (var e : m2.entrySet()) {
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
                @SuppressWarnings("unchecked")
                var result = (M) toReplace;
                return result;
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

        var newCharacteristics = EnumSet.copyOf(downstream.characteristics());
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

    public static <T> TCollector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return TCollector.of(
                () -> new int[1],
                (a, t) -> a[0] = a[0] + mapper.applyAsInt(t),
                (a, b) -> {
                    a[0] = a[0] + b[0];
                    return a;
                },
                a -> a[0]);
    }

    public static <T> TCollector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        return collectingAndThen(summarizingLong(mapper), TLongSummaryStatistics::getSum);
    }

    public static <T> TCollector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        return collectingAndThen(summarizingDouble(mapper), TDoubleSummaryStatistics::getSum);
    }

    public static <T> TCollector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        return collectingAndThen(summarizingInt(mapper), TIntSummaryStatistics::getAverage);
    }

    public static <T> TCollector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        return collectingAndThen(summarizingLong(mapper), TLongSummaryStatistics::getAverage);
    }

    public static <T> TCollector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        return collectingAndThen(summarizingDouble(mapper), TDoubleSummaryStatistics::getAverage);
    }

    public static <T> TCollector<T, ?, TIntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return TCollector.of(
                TIntSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsInt(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                },
                TCollector.Characteristics.IDENTITY_FINISH
        );
    }

    public static <T> TCollector<T, ?, TLongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        return TCollector.of(
                TLongSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsLong(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                },
                TCollector.Characteristics.IDENTITY_FINISH
        );
    }

    public static <T> TCollector<T, ?, TDoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
        return TCollector.of(
                TDoubleSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsDouble(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                },
                TCollector.Characteristics.IDENTITY_FINISH
        );
    }

    private static <T, A1, A2, R1, R2, R> TCollector<T, ?, R> teeingUnwrap(TCollector<? super T, A1, R1> left,
            TCollector<? super T, A2, R2> right, BiFunction<? super R1, ? super R2, R> merger) {
        return TCollector.of(() -> new Pair<>(left.supplier().get(), right.supplier().get()),
                (p, t) -> {
                    left.accumulator().accept(p.a, t);
                    right.accumulator().accept(p.b, t);
                }, (p1, p2) -> {
                    p1.a = left.combiner().apply(p1.a, p2.a);
                    p2.b = right.combiner().apply(p1.b, p2.b);
                    return p1;
                }, p -> merger.apply(left.finisher().apply(p.a), right.finisher().apply(p.b)));
    }

    public static <T, R1, R2, R> TCollector<T, ?, R> teeing(TCollector<? super T, ?, R1> left,
            TCollector<? super T, ?, R2> right, BiFunction<? super R1, ? super R2, R> merger) {
        return teeingUnwrap(left, right, merger);
    }

    private static class Pair<A, B> {
        private A a;
        private B b;

        private Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

    public static <T, A, R> TCollector<T, ?, Map<Boolean, R>> partitioningBy(Predicate<? super T> predicate,
            TCollector<? super T, A, R> downstream) {
        BiConsumer<A, ? super T> acc = downstream.accumulator();
        return teeing(TCollector.of(downstream.supplier(), (res, el) -> {
                    if (!predicate.test(el)) {
                        acc.accept(res, el);
                    }
                }, downstream.combiner(), downstream.finisher()),
                TCollector.of(downstream.supplier(), (res1, el1) -> {
                    if (predicate.test(el1)) {
                        acc.accept(res1, el1);
                    }
                }, downstream.combiner(), downstream.finisher()),
                (fls, tr) -> Map.of(false, fls, true, tr));
    }

    public static <T> TCollector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }
}
