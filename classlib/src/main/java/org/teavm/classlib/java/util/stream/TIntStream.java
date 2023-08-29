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
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TIntSummaryStatistics;
import org.teavm.classlib.java.util.stream.intimpl.TArrayIntStreamImpl;
import org.teavm.classlib.java.util.stream.intimpl.TEmptyIntStreamImpl;
import org.teavm.classlib.java.util.stream.intimpl.TGenerateIntStream;
import org.teavm.classlib.java.util.stream.intimpl.TGenericConcatIntStream;
import org.teavm.classlib.java.util.stream.intimpl.TIntStreamBuilder;
import org.teavm.classlib.java.util.stream.intimpl.TIterateIntStream;
import org.teavm.classlib.java.util.stream.intimpl.TRangeIntStream;
import org.teavm.classlib.java.util.stream.intimpl.TSimpleIntStreamImpl;
import org.teavm.classlib.java.util.stream.intimpl.TSingleIntStreamImpl;
import org.teavm.classlib.java.util.stream.intimpl.TSpecializedConcatIntStream;

public interface TIntStream extends TBaseStream<Integer, TIntStream> {
    interface Builder extends IntConsumer {
        @Override
        void accept(int t);

        default Builder add(int t) {
            accept(t);
            return this;
        }

        TIntStream build();
    }

    TIntStream filter(IntPredicate predicate);

    TIntStream map(IntUnaryOperator mapper);

    <U> TStream<U> mapToObj(IntFunction<? extends U> mapper);

    TLongStream mapToLong(IntToLongFunction mapper);

    TDoubleStream mapToDouble(IntToDoubleFunction mapper);

    TIntStream flatMap(IntFunction<? extends TIntStream> mapper);

    default TIntStream mapMulti(IntMapMultiConsumer mapper) {
        return flatMap(e -> {
            Builder builder = builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    TIntStream distinct();

    TIntStream sorted();

    TIntStream peek(IntConsumer action);

    TIntStream limit(long maxSize);

    TIntStream takeWhile(IntPredicate predicate);

    TIntStream dropWhile(IntPredicate predicate);

    TIntStream skip(long n);

    void forEach(IntConsumer action);

    void forEachOrdered(IntConsumer action);

    int[] toArray();

    int reduce(int identity, IntBinaryOperator accumulator);

    OptionalInt reduce(IntBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);

    int sum();

    OptionalInt min();

    OptionalInt max();

    long count();

    OptionalDouble average();

    TIntSummaryStatistics summaryStatistics();

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    OptionalInt findFirst();

    OptionalInt findAny();

    TLongStream asLongStream();

    TDoubleStream asDoubleStream();

    TStream<Integer> boxed();

    @Override
    PrimitiveIterator.OfInt iterator();

    @Override
    Spliterator.OfInt spliterator();

    static Builder builder() {
        return new TIntStreamBuilder();
    }

    static TIntStream empty() {
        return new TEmptyIntStreamImpl();
    }

    static TIntStream of(int t) {
        return new TSingleIntStreamImpl(t);
    }

    static TIntStream of(int... values) {
        return new TArrayIntStreamImpl(values, 0, values.length);
    }

    static TIntStream iterate(int seed, IntUnaryOperator f) {
        return new TIterateIntStream(seed, f);
    }

    static TIntStream iterate(int seed, IntPredicate pr, IntUnaryOperator f) {
        return new TIterateIntStream(seed, pr, f);
    }

    static TIntStream generate(IntSupplier s) {
        return new TGenerateIntStream(s);
    }

    static TIntStream range(int startInclusive, int endExclusive) {
        return new TRangeIntStream(startInclusive, endExclusive);
    }

    static TIntStream rangeClosed(int startInclusive, int endInclusive) {
        return new TRangeIntStream(startInclusive, endInclusive + 1);
    }

    static TIntStream concat(TIntStream a, TIntStream b) {
        if (a instanceof TSimpleIntStreamImpl && b instanceof TSimpleIntStreamImpl) {
            return new TSpecializedConcatIntStream((TSimpleIntStreamImpl) a, (TSimpleIntStreamImpl) b);
        } else {
            return new TGenericConcatIntStream(a, b);
        }
    }

    @FunctionalInterface
    interface IntMapMultiConsumer {
        void accept(int value, IntConsumer ic);
    }
}
