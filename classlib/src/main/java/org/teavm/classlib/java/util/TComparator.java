/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.util.function.TFunction;
import org.teavm.classlib.java.util.function.TToDoubleFunction;
import org.teavm.classlib.java.util.function.TToIntFunction;
import org.teavm.classlib.java.util.function.TToLongFunction;

/**
 *
 * @author Alexey Andreev
 * @param <T>
 */
@FunctionalInterface
public interface TComparator<T> {
    int compare(T o1, T o2);

    default TComparator<T> reversed() {
        return (a, b) -> compare(b, a);
    }

    default TComparator<T> thenComparing(TComparator<? super T> other) {
        return (a, b) -> {
            int r = compare(a, b);
            if (r == 0) {
                r = other.compare(a, b);
            }
            return r;
        };
    }

    default <U> TComparator<T> thenComparing(TFunction<? super T, ? extends U> keyExtractor,
            TComparator<? super U> keyComparator) {
        return thenComparing(comparing(keyExtractor, keyComparator));
    }

    default <U extends Comparable<? super U>> TComparator<T> thenComparing(
            TFunction<? super T, ? extends U> keyExtractor) {
        return (a, b) -> {
            int r = compare(a, b);
            if (r == 0) {
                U k = keyExtractor.apply(a);
                U m = keyExtractor.apply(b);
                r = k.compareTo(m);
            }
            return r;
        };
    }

    default TComparator<T> thenComparingInt(TToIntFunction<? super T> keyExtractor) {
        return (a, b) -> {
            int r = compare(a, b);
            if (r == 0) {
                r = Integer.compare(keyExtractor.applyAsInt(a), keyExtractor.applyAsInt(b));
            }
            return r;
        };
    }

    default TComparator<T> thenComparingLong(TToLongFunction<? super T> keyExtractor) {
        return (a, b) -> {
            int r = compare(a, b);
            if (r == 0) {
                r = Long.compare(keyExtractor.applyAsLong(a), keyExtractor.applyAsLong(b));
            }
            return r;
        };
    }

    default TComparator<T> thenComparingDouble(TToDoubleFunction<? super T> keyExtractor) {
        return (a, b) -> {
            int r = compare(a, b);
            if (r == 0) {
                r = Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b));
            }
            return r;
        };
    }

    static <T, U> TComparator<T> comparing(TFunction<? super T, ? extends U> keyExtractor,
            TComparator<? super U> keyComparator) {
        return (a, b) -> keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    static <T, U extends TComparable<? super U>> TComparator<T> comparing(
            TFunction<? super T, ? extends U> keyExtractor) {
        return (a, b) -> {
            U k = keyExtractor.apply(a);
            U m = keyExtractor.apply(b);
            return k.compareTo(m);
        };
    }

    class NaturalOrder implements TComparator<Object> {
        private static final TComparator<Object> INSTANCE = new NaturalOrder();

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }

        @SuppressWarnings("unchecked")
        static <T> TComparator<T> instance() {
            return (TComparator<T>) INSTANCE;
        }
    }

    static <T extends TComparable<? super T>> TComparator<T> naturalOrder() {
        return NaturalOrder.instance();
    }

    static <T extends TComparable<? super T>> TComparator<T> reverseOrder() {
        return TCollections.reverseOrder();
    }

    static <T> TComparator<T> nullsFirst(TComparator<? super T> comparator) {
        return (a, b) -> a == null && b == null ? 0 : a == null ? -1 : b == null ? 1 : comparator.compare(a, b);
    }

    static <T> TComparator<T> nullsLast(TComparator<? super T> comparator) {
        return (a, b) -> a == null && b == null ? 0 : a == null ? 1 : b == null ? -1 : comparator.compare(a, b);
    }

    static <T> TComparator<T> comparingInt(TToIntFunction<? super T> keyExtractor) {
        return (a, b) -> Integer.compare(keyExtractor.applyAsInt(a), keyExtractor.applyAsInt(b));
    }

    static <T> TComparator<T> comparingLong(TToLongFunction<? super T> keyExtractor) {
        return (a, b) -> Long.compare(keyExtractor.applyAsLong(a), keyExtractor.applyAsLong(b));
    }

    static <T> TComparator<T> comparingDouble(TToDoubleFunction<? super T> keyExtractor) {
        return (a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b));
    }
}
