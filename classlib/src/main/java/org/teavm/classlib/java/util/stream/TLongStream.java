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

import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TLongSummaryStatistics;
import org.teavm.classlib.java.util.stream.longimpl.TArrayLongStreamImpl;
import org.teavm.classlib.java.util.stream.longimpl.TEmptyLongStreamImpl;
import org.teavm.classlib.java.util.stream.longimpl.TGenerateLongStream;
import org.teavm.classlib.java.util.stream.longimpl.TGenericConcatLongStream;
import org.teavm.classlib.java.util.stream.longimpl.TIterateLongStream;
import org.teavm.classlib.java.util.stream.longimpl.TLongStreamBuilder;
import org.teavm.classlib.java.util.stream.longimpl.TRangeLongStream;
import org.teavm.classlib.java.util.stream.longimpl.TSimpleLongStreamImpl;
import org.teavm.classlib.java.util.stream.longimpl.TSingleLongStreamImpl;
import org.teavm.classlib.java.util.stream.longimpl.TSpecializedConcatLongStream;

public interface TLongStream extends TBaseStream<Long, TLongStream> {
    interface Builder extends LongConsumer {
        @Override
        void accept(long t);

        default Builder add(long t) {
            accept(t);
            return this;
        }

        TLongStream build();
    }

    TLongStream filter(LongPredicate predicate);

    TLongStream map(LongUnaryOperator mapper);

    <U> TStream<U> mapToObj(LongFunction<? extends U> mapper);

    TIntStream mapToInt(LongToIntFunction mapper);

    TDoubleStream mapToDouble(LongToDoubleFunction mapper);

    TLongStream flatMap(LongFunction<? extends TLongStream> mapper);

    default TLongStream mapMulti(LongMapMultiConsumer mapper) {
        return flatMap(e -> {
            Builder builder = builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    TLongStream distinct();

    TLongStream sorted();

    TLongStream peek(LongConsumer action);

    TLongStream limit(long maxSize);

    TLongStream takeWhile(LongPredicate predicate);

    TLongStream dropWhile(LongPredicate predicate);

    TLongStream skip(long n);

    void forEach(LongConsumer action);

    void forEachOrdered(LongConsumer action);

    long[] toArray();

    long reduce(long identity, LongBinaryOperator accumulator);

    OptionalLong reduce(LongBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long sum();

    OptionalLong min();

    OptionalLong max();

    long count();

    OptionalDouble average();

    TLongSummaryStatistics summaryStatistics();

    boolean anyMatch(LongPredicate predicate);

    boolean allMatch(LongPredicate predicate);

    boolean noneMatch(LongPredicate predicate);

    OptionalLong findFirst();

    OptionalLong findAny();

    TDoubleStream asDoubleStream();

    TStream<Long> boxed();

    @Override
    PrimitiveIterator.OfLong iterator();

    @Override
    Spliterator.OfLong spliterator();

    static Builder builder() {
        return new TLongStreamBuilder();
    }

    static TLongStream empty() {
        return new TEmptyLongStreamImpl();
    }

    static TLongStream of(long t) {
        return new TSingleLongStreamImpl(t);
    }

    static TLongStream of(long... values) {
        return new TArrayLongStreamImpl(values, 0, values.length);
    }

    static TLongStream iterate(long seed, LongUnaryOperator f) {
        return new TIterateLongStream(seed, f);
    }

    static TLongStream iterate(long seed, LongPredicate pr, LongUnaryOperator f) {
        return new TIterateLongStream(seed, pr, f);
    }

    static TLongStream generate(LongSupplier s) {
        return new TGenerateLongStream(s);
    }

    static TLongStream range(long startInclusive, long endExclusive) {
        return new TRangeLongStream(startInclusive, endExclusive);
    }

    static TLongStream rangeClosed(long startInclusive, long endInclusive) {
        return new TRangeLongStream(startInclusive, endInclusive + 1);
    }

    static TLongStream concat(TLongStream a, TLongStream b) {
        if (a instanceof TSimpleLongStreamImpl && b instanceof TSimpleLongStreamImpl) {
            return new TSpecializedConcatLongStream((TSimpleLongStreamImpl) a, (TSimpleLongStreamImpl) b);
        } else {
            return new TGenericConcatLongStream(a, b);
        }
    }

    @FunctionalInterface
    interface LongMapMultiConsumer {
        void accept(long value, LongConsumer lc);
    }
}
