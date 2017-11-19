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
package org.teavm.classlib.java.util.stream.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import org.teavm.classlib.java.util.stream.TCollector;
import org.teavm.classlib.java.util.stream.TDoubleStream;
import org.teavm.classlib.java.util.stream.TIntStream;
import org.teavm.classlib.java.util.stream.TLongStream;
import org.teavm.classlib.java.util.stream.TStream;

public abstract class TSimpleStreamImpl<T> implements TStream<T> {
    @Override
    public TStream<T> filter(Predicate<? super T> predicate) {
        return new TFilteringStreamImpl<>(this, predicate);
    }

    @Override
    public <R> TStream<R> map(Function<? super T, ? extends R> mapper) {
        return new TMappingStreamImpl<>(this, mapper);
    }

    @Override
    public TIntStream mapToInt(ToIntFunction<? super T> mapper) {
        return new TMappingToIntStreamImpl<>(this, mapper);
    }

    @Override
    public TLongStream mapToLong(ToLongFunction<? super T> mapper) {
        return new TMappingToLongStreamImpl<>(this, mapper);
    }

    @Override
    public TDoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return new TMappingToDoubleStreamImpl<>(this, mapper);
    }

    @Override
    public <R> TStream<R> flatMap(Function<? super T, ? extends TStream<? extends R>> mapper) {
        return new TFlatMappingStreamImpl<>(this, mapper);
    }

    @Override
    public TIntStream flatMapToInt(Function<? super T, ? extends TIntStream> mapper) {
        return new TFlatMappingToIntStreamImpl<>(this, mapper);
    }

    @Override
    public TLongStream flatMapToLong(Function<? super T, ? extends TLongStream> mapper) {
        return new TFlatMappingToLongStreamImpl<>(this, mapper);
    }

    @Override
    public TDoubleStream flatMapToDouble(Function<? super T, ? extends TDoubleStream> mapper) {
        return new TFlatMappingToDoubleStreamImpl<>(this, mapper);
    }

    @Override
    public TStream<T> distinct() {
        return new TDistinctStreamImpl<>(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TStream<T> sorted() {
        return new TSortedStreamImpl<>(this, (a, b) -> ((Comparable<Object>) a).compareTo(b));
    }

    @Override
    public TStream<T> sorted(Comparator<? super T> comparator) {
        return new TSortedStreamImpl<>(this, comparator);
    }

    @Override
    public TStream<T> peek(Consumer<? super T> action) {
        return new TPeekingStreamImpl<>(this, action);
    }

    @Override
    public TStream<T> limit(long maxSize) {
        return new TLimitingStreamImpl<>(this, (int) maxSize);
    }

    @Override
    public TStream<T> skip(long n) {
        return new TSkippingStreamImpl<>(this, (int) n);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        forEachOrdered(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        next(e -> {
            action.accept(e);
            return true;
        });
    }

    @Override
    public Object[] toArray() {
        return toArray(Object[]::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(IntFunction<A[]> generator) {
        int estimatedSize = estimateSize();
        if (estimatedSize < 0) {
            List<T> list = new ArrayList<>();
            next(list::add);
            A[] array = generator.apply(list.size());
            for (int i = 0; i < array.length; ++i) {
                array[i] = (A) list.get(i);
            }
            return array;
        } else {
            A[] array = generator.apply(estimatedSize);
            ArrayFillingConsumer<A> consumer = new ArrayFillingConsumer<>(array);
            boolean wantsMore = next(consumer);
            assert !wantsMore : "next() should have reported done status";
            if (consumer.index < array.length) {
                array = Arrays.copyOf(array, consumer.index);
            }
            return array;
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        TReducingConsumer<T> consumer = new TReducingConsumer<>(accumulator, identity, true);
        boolean wantsMore = next(consumer);
        assert !wantsMore : "next() should have returned true";
        return consumer.result;
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        TReducingConsumer<T> consumer = new TReducingConsumer<>(accumulator, null, false);
        boolean wantsMore = next(consumer);
        assert !wantsMore : "next() should have returned true";
        return Optional.ofNullable(consumer.result);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        TReducingConsumer2<T, U> consumer = new TReducingConsumer2<>(accumulator, identity);
        boolean wantsMore = next(consumer);
        assert !wantsMore : "next() should have returned true";
        return consumer.result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        R collection = supplier.get();
        next(e -> {
            accumulator.accept(collection, e);
            return true;
        });
        return collection;
    }

    @Override
    public <R, A> R collect(TCollector<? super T, A, R> collector) {
        A collection = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        next(e -> {
            accumulator.accept(collection, e);
            return true;
        });
        return collector.finisher().apply(collection);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return reduce((a, b) -> comparator.compare(a, b) < 0 ? a : b);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return reduce((a, b) -> comparator.compare(a, b) > 0 ? a : b);
    }

    @Override
    public long count() {
        TCountingConsumer<T> consumer = new TCountingConsumer<>();
        next(consumer);
        return consumer.count;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return next(predicate.negate());
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return !next(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        TFindFirstConsumer<T> consumer = new TFindFirstConsumer<>();
        next(consumer);
        return Optional.ofNullable(consumer.result);
    }

    @Override
    public Optional<T> findAny() {
        return findFirst();
    }

    @Override
    public Iterator<T> iterator() {
        return new TSimpleStreamIterator<>(this);
    }

    @Override
    public Spliterator<T> spliterator() {
        return new TSimpleStreamSpliterator<>(this);
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public TStream<T> sequential() {
        return this;
    }

    @Override
    public TStream<T> parallel() {
        return this;
    }

    @Override
    public TStream<T> unordered() {
        return this;
    }

    @Override
    public TStream<T> onClose(Runnable closeHandler) {
        return new TCloseHandlingStream<>(this, closeHandler);
    }

    @Override
    public void close() throws Exception {
    }

    protected int estimateSize() {
        return -1;
    }

    public abstract boolean next(Predicate<? super T> consumer);

    class ArrayFillingConsumer<A> implements Predicate<T> {
        A[] array;
        int index;

        ArrayFillingConsumer(A[] array) {
            this.array = array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean test(T t) {
            array[index++] = (A) t;
            return true;
        }
    }
}
