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
package org.teavm.classlib.java.util.stream.doubleimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.stream.TDoubleStream;
import org.teavm.classlib.java.util.stream.TIntStream;
import org.teavm.classlib.java.util.stream.TLongStream;
import org.teavm.classlib.java.util.stream.TStream;

public abstract class TSimpleDoubleStreamImpl implements TDoubleStream {
    @Override
    public TDoubleStream filter(DoublePredicate predicate) {
        return new TFilteringDoubleStreamImpl(this, predicate);
    }

    @Override
    public TDoubleStream map(DoubleUnaryOperator mapper) {
        return new TMappingDoubleStreamImpl(this, mapper);
    }

    @Override
    public <U> TStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return new TMappingToObjStreamImpl<>(this, mapper);
    }

    @Override
    public TIntStream mapToInt(DoubleToIntFunction mapper) {
        return new TMappingToIntStreamImpl(this, mapper);
    }

    @Override
    public TLongStream mapToLong(DoubleToLongFunction mapper) {
        return new TMappingToLongStreamImpl(this, mapper);
    }

    @Override
    public TDoubleStream flatMap(DoubleFunction<? extends TDoubleStream> mapper) {
        return new TFlatMappingDoubleStreamImpl(this, mapper);
    }

    @Override
    public TDoubleStream distinct() {
        return new TDistinctDoubleStreamImpl(this);
    }

    @Override
    public TDoubleStream sorted() {
        double[] array = toArray();
        Arrays.sort(array);
        return TDoubleStream.of(array);
    }

    @Override
    public TDoubleStream peek(DoubleConsumer action) {
        return new TPeekingDoubleStreamImpl(this, action);
    }

    @Override
    public TDoubleStream limit(long maxSize) {
        return new TLimitingDoubleStreamImpl(this, (int) maxSize);
    }

    @Override
    public TDoubleStream skip(long n) {
        return new TSkippingDoubleStreamImpl(this, (int) n);
    }

    @Override
    public void forEach(DoubleConsumer action) {
        forEachOrdered(action);
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        while (true) {
            boolean hasMore = next(e -> {
                action.accept(e);
                return true;
            });
            if (!hasMore) {
                return;
            }
        }
    }

    @Override
    public double[] toArray() {
        int estimatedSize = estimateSize();
        if (estimatedSize < 0) {
            List<Double> list = new ArrayList<>();
            while (next(list::add)) {
                // go on
            }
            double[] array = new double[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.get(i);
            }
            return array;
        } else {
            double[] array = new double[estimatedSize];
            ArrayFillingConsumer consumer = new ArrayFillingConsumer(array);
            while (next(consumer)) {
                // go on
            }
            if (consumer.index < array.length) {
                array = Arrays.copyOf(array, consumer.index);
            }
            return array;
        }
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator accumulator) {
        TReducingDoubleConsumer consumer = new TReducingDoubleConsumer(accumulator, identity, true);
        while (next(consumer)) {
            // go on
        }
        return consumer.result;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator accumulator) {
        TReducingDoubleConsumer consumer = new TReducingDoubleConsumer(accumulator, 0, false);
        while (next(consumer)) {
            // go on
        }
        return consumer.initialized ? OptionalDouble.of(consumer.result) : OptionalDouble.empty();
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R collection = supplier.get();
        while (true) {
            boolean hasMore = next(e -> {
                accumulator.accept(collection, e);
                return true;
            });
            if (!hasMore) {
                break;
            }
        }
        return collection;
    }

    @Override
    public OptionalDouble min() {
        return reduce(Math::min);
    }

    @Override
    public OptionalDouble max() {
        return reduce(Math::max);
    }

    @Override
    public long count() {
        TCountingDoubleConsumer consumer = new TCountingDoubleConsumer();
        while (next(consumer)) {
            // go on
        }
        return consumer.count;
    }

    @Override
    public double sum() {
        TSumDoubleConsumer consumer = new TSumDoubleConsumer();
        while (next(consumer)) {
            // go on
        }
        return consumer.sum;
    }

    @Override
    public OptionalDouble average() {
        TAverageDoubleConsumer consumer = new TAverageDoubleConsumer();
        while (next(consumer)) {
            // go on
        }
        return consumer.count > 0 ? OptionalDouble.of(consumer.sum / consumer.count) : OptionalDouble.empty();
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        TAnyMatchConsumer consumer = new TAnyMatchConsumer(predicate);
        while (!consumer.matched && next(consumer)) {
            // go on
        }
        return consumer.matched;
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        TAllMatchConsumer consumer = new TAllMatchConsumer(predicate);
        while (consumer.matched && next(consumer)) {
            // go on
        }
        return consumer.matched;
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        TFindFirstDoubleConsumer consumer = new TFindFirstDoubleConsumer();
        while (!consumer.hasAny && next(consumer)) {
            // go on
        }
        return consumer.hasAny ? OptionalDouble.of(consumer.result) : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble findAny() {
        return findFirst();
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return new TSimpleDoubleStreamIterator(this);
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return new TSimpleDoubleStreamSpliterator(this);
    }

    @Override
    public TStream<Double> boxed() {
        return new TBoxedDoubleStream(this);
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public TDoubleStream sequential() {
        return this;
    }

    @Override
    public TDoubleStream parallel() {
        return this;
    }

    @Override
    public TDoubleStream unordered() {
        return this;
    }

    @Override
    public TDoubleStream onClose(Runnable closeHandler) {
        return new TCloseHandlingDoubleStream(this, closeHandler);
    }

    @Override
    public void close() throws Exception {
    }

    protected int estimateSize() {
        return -1;
    }

    public abstract boolean next(DoublePredicate consumer);

    static class ArrayFillingConsumer implements DoublePredicate {
        double[] array;
        int index;

        ArrayFillingConsumer(double[] array) {
            this.array = array;
        }

        @Override
        public boolean test(double t) {
            array[index++] = t;
            return true;
        }
    }
}
