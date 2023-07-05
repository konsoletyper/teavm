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
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TDoubleSummaryStatistics;
import org.teavm.classlib.java.util.stream.doubleimpl.TArrayDoubleStreamImpl;
import org.teavm.classlib.java.util.stream.doubleimpl.TDoubleStreamBuilder;
import org.teavm.classlib.java.util.stream.doubleimpl.TEmptyDoubleStreamImpl;
import org.teavm.classlib.java.util.stream.doubleimpl.TGenerateDoubleStream;
import org.teavm.classlib.java.util.stream.doubleimpl.TGenericConcatDoubleStream;
import org.teavm.classlib.java.util.stream.doubleimpl.TIterateDoubleStream;
import org.teavm.classlib.java.util.stream.doubleimpl.TSimpleDoubleStreamImpl;
import org.teavm.classlib.java.util.stream.doubleimpl.TSingleDoubleStreamImpl;
import org.teavm.classlib.java.util.stream.doubleimpl.TSpecializedConcatDoubleStream;

public interface TDoubleStream extends TBaseStream<Double, TDoubleStream> {
    interface Builder extends DoubleConsumer {
        @Override
        void accept(double t);

        default Builder add(double t) {
            accept(t);
            return this;
        }

        TDoubleStream build();
    }

    TDoubleStream filter(DoublePredicate predicate);

    TDoubleStream map(DoubleUnaryOperator mapper);

    <U> TStream<U> mapToObj(DoubleFunction<? extends U> mapper);

    TIntStream mapToInt(DoubleToIntFunction mapper);

    TLongStream mapToLong(DoubleToLongFunction mapper);

    TDoubleStream flatMap(DoubleFunction<? extends TDoubleStream> mapper);

    default TDoubleStream mapMulti(DoubleMapMultiConsumer mapper) {
        return flatMap(e -> {
            Builder builder = builder();
            mapper.accept(e, builder);
            return builder.build();
        });
    }

    TDoubleStream distinct();

    TDoubleStream sorted();

    TDoubleStream peek(DoubleConsumer action);

    TDoubleStream limit(long maxSize);

    TDoubleStream takeWhile(DoublePredicate predicate);

    TDoubleStream dropWhile(DoublePredicate predicate);

    TDoubleStream skip(long n);

    void forEach(DoubleConsumer action);

    void forEachOrdered(DoubleConsumer action);

    double[] toArray();

    double reduce(double identity, DoubleBinaryOperator accumulator);

    OptionalDouble reduce(DoubleBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

    double sum();

    OptionalDouble min();

    OptionalDouble max();

    long count();

    OptionalDouble average();

    TDoubleSummaryStatistics summaryStatistics();

    boolean anyMatch(DoublePredicate predicate);

    boolean allMatch(DoublePredicate predicate);

    boolean noneMatch(DoublePredicate predicate);

    OptionalDouble findFirst();

    OptionalDouble findAny();

    TStream<Double> boxed();

    @Override
    PrimitiveIterator.OfDouble iterator();

    @Override
    Spliterator.OfDouble spliterator();

    static TDoubleStream.Builder builder() {
        return new TDoubleStreamBuilder();
    }

    static TDoubleStream empty() {
        return new TEmptyDoubleStreamImpl();
    }

    static TDoubleStream of(double t) {
        return new TSingleDoubleStreamImpl(t);
    }

    static TDoubleStream of(double... values) {
        return new TArrayDoubleStreamImpl(values, 0, values.length);
    }

    static TDoubleStream iterate(double seed, DoubleUnaryOperator f) {
        return new TIterateDoubleStream(seed, f);
    }

    static TDoubleStream iterate(double seed, DoublePredicate pr, DoubleUnaryOperator f) {
        return new TIterateDoubleStream(seed, pr, f);
    }

    static TDoubleStream generate(DoubleSupplier s) {
        return new TGenerateDoubleStream(s);
    }

    static TDoubleStream concat(TDoubleStream a, TDoubleStream b) {
        if (a instanceof TSimpleDoubleStreamImpl && b instanceof TSimpleDoubleStreamImpl) {
            return new TSpecializedConcatDoubleStream((TSimpleDoubleStreamImpl) a, (TSimpleDoubleStreamImpl) b);
        } else {
            return new TGenericConcatDoubleStream(a, b);
        }
    }

    @FunctionalInterface
    interface DoubleMapMultiConsumer {
        void accept(double value, DoubleConsumer dc);
    }
}
