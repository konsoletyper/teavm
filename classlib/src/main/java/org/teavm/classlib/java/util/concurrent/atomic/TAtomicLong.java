/*
 *  Copyright 2018 martin below.
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
package org.teavm.classlib.java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

public class TAtomicLong extends Number implements Serializable {
    private long value;

    public TAtomicLong() {
    }

    public TAtomicLong(final long value) {
        this.value = value;
    }

    public final long get() {
        return value;
    }

    public final void set(final long newValue) {
        value = newValue;
    }

    public final void lazySet(final long newValue) {
        value = newValue;
    }

    public final long getAndSet(final long newValue) {
        long result = value;
        value = newValue;
        return result;
    }

    public final boolean compareAndSet(final long expect, final long update) {
        if (value != expect) {
            return false;
        }
        value = update;
        return true;
    }

    public final boolean weakCompareAndSet(final long expect, final long update) {
        return compareAndSet(expect, update);
    }

    public final long getAndIncrement() {
        return value++;
    }

    public final long getAndDecrement() {
        return value--;
    }

    public final long getAndAdd(long delta) {
        long result = value;
        value += delta;
        return result;
    }

    public final long incrementAndGet() {
        return ++value;
    }

    public final long decrementAndGet() {
        return --value;
    }

    public final long addAndGet(final long delta) {
        value += delta;
        return value;
    }

    public final long getAndUpdate(final LongUnaryOperator updateFunction) {
        long result = value;
        value = updateFunction.applyAsLong(value);
        return result;
    }

    public final long updateAndGet(final LongUnaryOperator updateFunction) {
        value = updateFunction.applyAsLong(value);
        return value;
    }

    public final long getAndAccumulate(long x, final LongBinaryOperator accumulatorFunction) {
        long result = value;
        value = accumulatorFunction.applyAsLong(value, x);
        return result;
    }

    public final long accumulateAndGet(long x, final LongBinaryOperator accumulatorFunction) {
        value = accumulatorFunction.applyAsLong(value, x);
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
