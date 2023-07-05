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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import org.teavm.classlib.java.util.TTemplateCollections;
import org.teavm.classlib.java.util.stream.impl.TArrayStreamImpl;
import org.teavm.classlib.java.util.stream.impl.TEmptyStreamImpl;
import org.teavm.classlib.java.util.stream.impl.TGenerateStream;
import org.teavm.classlib.java.util.stream.impl.TGenericConcatStream;
import org.teavm.classlib.java.util.stream.impl.TIterateStream;
import org.teavm.classlib.java.util.stream.impl.TSimpleStreamImpl;
import org.teavm.classlib.java.util.stream.impl.TSingleStreamImpl;
import org.teavm.classlib.java.util.stream.impl.TSpecializedConcatStream;
import org.teavm.classlib.java.util.stream.impl.TStreamBuilder;

public interface TStream<T> extends TBaseStream<T, TStream<T>> {
    interface Builder<T> extends Consumer<T> {
        @Override
        void accept(T t);

        default Builder<T> add(T t) {
            accept(t);
            return this;
        }

        TStream<T> build();
    }

    TStream<T> filter(Predicate<? super T> predicate);

    <R> TStream<R> map(Function<? super T, ? extends R> mapper);

    TIntStream mapToInt(ToIntFunction<? super T> mapper);

    TLongStream mapToLong(ToLongFunction<? super T> mapper);

    TDoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    <R> TStream<R> flatMap(Function<? super T, ? extends TStream<? extends R>> mapper);

    TIntStream flatMapToInt(Function<? super T, ? extends TIntStream> mapper);

    TLongStream flatMapToLong(Function<? super T, ? extends TLongStream> mapper);

    TDoubleStream flatMapToDouble(Function<? super T, ? extends TDoubleStream> mapper);

    default <R> TStream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
        return flatMap(e -> {
            TStream.Builder<R> builder = builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    default TIntStream mapMultiToInt(BiConsumer<? super T, ? super IntConsumer> mapper) {
        return flatMapToInt(e -> {
            TIntStream.Builder builder = TIntStream.builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    default TLongStream mapMultiToLong(BiConsumer<? super T, ? super LongConsumer> mapper) {
        return flatMapToLong(e -> {
            TLongStream.Builder builder = TLongStream.builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    default TDoubleStream mapMultiToDouble(BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        return flatMapToDouble(e -> {
            TDoubleStream.Builder builder = TDoubleStream.builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    TStream<T> distinct();

    TStream<T> sorted();

    TStream<T> sorted(Comparator<? super T> comparator);

    TStream<T> peek(Consumer<? super T> action);

    TStream<T> limit(long maxSize);

    TStream<T> takeWhile(Predicate<? super T> predicate);

    TStream<T> dropWhile(Predicate<? super T> predicate);

    TStream<T> skip(long n);

    void forEach(Consumer<? super T> action);

    void forEachOrdered(Consumer<? super T> action);

    Object[] toArray();

    <A> A[] toArray(IntFunction<A[]> generator);

    T reduce(T identity, BinaryOperator<T> accumulator);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    <R, A> R collect(TCollector<? super T, A, R> collector);

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    long count();

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    Optional<T> findAny();

    static <T> TStream.Builder<T> builder() {
        return new TStreamBuilder<>();
    }

    static <T> TStream<T> empty() {
        return new TEmptyStreamImpl<>();
    }

    static <T> TStream<T> of(T t) {
        return new TSingleStreamImpl<>(t);
    }

    @SafeVarargs
    static <T> TStream<T> of(T... values) {
        return new TArrayStreamImpl<>(values, 0, values.length);
    }

    static <T> TStream<T> iterate(T seed, UnaryOperator<T> f) {
        return new TIterateStream<>(seed, f);
    }

    static <T> TStream<T> iterate(T seed, Predicate<? super T> pr, UnaryOperator<T> f) {
        return new TIterateStream<>(seed, pr, f);
    }

    static <T> TStream<T> generate(Supplier<T> s) {
        return new TGenerateStream<>(s);
    }

    @SuppressWarnings("unchecked")
    static <T> TStream<T> concat(TStream<? extends T> a, TStream<? extends T> b) {
        if (a instanceof TSimpleStreamImpl && b instanceof TSimpleStreamImpl) {
            return new TSpecializedConcatStream<>((TSimpleStreamImpl<T>) a, (TSimpleStreamImpl<T>) b);
        } else {
            return new TGenericConcatStream<>(a, b);
        }
    }

    @SuppressWarnings("unchecked")
    default List<T> toList() {
        return (List<T>) new TTemplateCollections.ImmutableArrayList<>(toArray());
    }
}
