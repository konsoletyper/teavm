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
package org.teavm.classlib.java.util.stream.intimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.stream.TDoubleStream;
import org.teavm.classlib.java.util.stream.TIntStream;
import org.teavm.classlib.java.util.stream.TLongStream;
import org.teavm.classlib.java.util.stream.TStream;

public abstract class TSimpleIntStreamImpl implements TIntStream {
    @Override
    public TIntStream filter(IntPredicate predicate) {
        return new TFilteringIntStreamImpl(this, predicate);
    }

    @Override
    public TIntStream map(IntUnaryOperator mapper) {
        return new TMappingIntStreamImpl(this, mapper);
    }

    @Override
    public <U> TStream<U> mapToObj(IntFunction<? extends U> mapper) {
        return new TMappingToObjStreamImpl<>(this, mapper);
    }

    @Override
    public TLongStream mapToLong(IntToLongFunction mapper) {
        return new TMappingToLongStreamImpl(this, mapper);
    }

    @Override
    public TDoubleStream mapToDouble(IntToDoubleFunction mapper) {
        return new TMappingToDoubleStreamImpl(this, mapper);
    }

    @Override
    public TIntStream flatMap(IntFunction<? extends TIntStream> mapper) {
        return new TFlatMappingIntStreamImpl(this, mapper);
    }

    @Override
    public TIntStream distinct() {
        return new TDistinctIntStreamImpl(this);
    }

    @Override
    public TIntStream sorted() {
        int[] array = toArray();
        Arrays.sort(array);
        return TIntStream.of(array);
    }

    @Override
    public TIntStream peek(IntConsumer action) {
        return new TPeekingIntStreamImpl(this, action);
    }

    @Override
    public TIntStream limit(long maxSize) {
        return new TLimitingIntStreamImpl(this, (int) maxSize);
    }

    @Override
    public TIntStream skip(long n) {
        return new TSkippingIntStreamImpl(this, (int) n);
    }

    @Override
    public void forEach(IntConsumer action) {
        forEachOrdered(action);
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        next(e -> {
            action.accept(e);
            return true;
        });
    }

    @Override
    public int[] toArray() {
        int estimatedSize = estimateSize();
        if (estimatedSize < 0) {
            List<Integer> list = new ArrayList<>();
            next(list::add);
            int[] array = new int[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.get(i);
            }
            return array;
        } else {
            int[] array = new int[estimatedSize];
            ArrayFillingConsumer consumer = new ArrayFillingConsumer(array);
            boolean wantsMore = next(consumer);
            assert !wantsMore : "next() should have reported done status";
            if (consumer.index < array.length) {
                array = Arrays.copyOf(array, consumer.index);
            }
            return array;
        }
    }

    @Override
    public int reduce(int identity, IntBinaryOperator accumulator) {
        TReducingIntConsumer consumer = new TReducingIntConsumer(accumulator, identity, true);
        boolean wantsMore = next(consumer);
        assert !wantsMore : "next() should have returned true";
        return consumer.result;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator accumulator) {
        TReducingIntConsumer consumer = new TReducingIntConsumer(accumulator, 0, false);
        boolean wantsMore = next(consumer);
        assert !wantsMore : "next() should have returned true";
        return consumer.initialized ? OptionalInt.of(consumer.result) : OptionalInt.empty();
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R collection = supplier.get();
        next(e -> {
            accumulator.accept(collection, e);
            return true;
        });
        return collection;
    }

    @Override
    public OptionalInt min() {
        return reduce(Math::min);
    }

    @Override
    public OptionalInt max() {
        return reduce(Math::max);
    }

    @Override
    public long count() {
        TCountingIntConsumer consumer = new TCountingIntConsumer();
        next(consumer);
        return consumer.count;
    }

    @Override
    public int sum() {
        TSumIntConsumer consumer = new TSumIntConsumer();
        next(consumer);
        return consumer.sum;
    }

    @Override
    public OptionalDouble average() {
        TSumIntAsDoubleConsumer consumer = new TSumIntAsDoubleConsumer();
        next(consumer);
        return consumer.count > 0 ? OptionalDouble.of(consumer.sum / consumer.count) : OptionalDouble.empty();
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        return next(predicate.negate());
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        return !next(predicate);
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalInt findFirst() {
        TFindFirstIntConsumer consumer = new TFindFirstIntConsumer();
        next(consumer);
        return consumer.hasAny ? OptionalInt.of(consumer.result) : OptionalInt.empty();
    }

    @Override
    public OptionalInt findAny() {
        return findFirst();
    }

    @Override
    public TLongStream asLongStream() {
        return new TIntAsLongStream(this);
    }

    @Override
    public TDoubleStream asDoubleStream() {
        return new TIntAsDoubleStream(this);
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new TSimpleIntStreamIterator(this);
    }

    @Override
    public Spliterator.OfInt spliterator() {
        return new TSimpleIntStreamSpliterator(this);
    }

    @Override
    public TStream<Integer> boxed() {
        return new TBoxedIntStream(this);
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public TIntStream sequential() {
        return this;
    }

    @Override
    public TIntStream parallel() {
        return this;
    }

    @Override
    public TIntStream unordered() {
        return this;
    }

    @Override
    public TIntStream onClose(Runnable closeHandler) {
        return new TCloseHandlingIntStream(this, closeHandler);
    }

    @Override
    public void close() throws Exception {
    }

    protected int estimateSize() {
        return -1;
    }

    public abstract boolean next(IntPredicate consumer);

    class ArrayFillingConsumer implements IntPredicate {
        int[] array;
        int index;

        ArrayFillingConsumer(int[] array) {
            this.array = array;
        }

        @Override
        public boolean test(int t) {
            array[index++] = t;
            return true;
        }
    }
}
