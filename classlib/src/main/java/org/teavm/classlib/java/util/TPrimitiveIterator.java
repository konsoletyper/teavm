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
package org.teavm.classlib.java.util;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface TPrimitiveIterator<T, S> extends Iterator<T> {
    void forEachRemaining(S action);

    interface OfInt extends TPrimitiveIterator<Integer, IntConsumer> {
        @Override
        default void forEachRemaining(Consumer<? super Integer> action) {
            while (hasNext()) {
                action.accept(nextInt());
            }
        }

        @Override
        default void forEachRemaining(IntConsumer action) {
            while (hasNext()) {
                action.accept(nextInt());
            }
        }

        int nextInt();

        @Override
        default Integer next() {
            return nextInt();
        }
    }

    interface OfLong extends TPrimitiveIterator<Long, LongConsumer> {
        @Override
        default void forEachRemaining(Consumer<? super Long> action) {
            while (hasNext()) {
                action.accept(nextLong());
            }
        }

        @Override
        default void forEachRemaining(LongConsumer action) {
            while (hasNext()) {
                action.accept(nextLong());
            }
        }

        long nextLong();

        @Override
        default Long next() {
            return nextLong();
        }
    }

    interface OfDouble extends TPrimitiveIterator<Double, DoubleConsumer> {
        @Override
        default void forEachRemaining(Consumer<? super Double> action) {
            while (hasNext()) {
                action.accept(nextDouble());
            }
        }

        @Override
        default void forEachRemaining(DoubleConsumer action) {
            while (hasNext()) {
                action.accept(nextDouble());
            }
        }

        double nextDouble();

        @Override
        default Double next() {
            return nextDouble();
        }
    }
}
