/*
 *  Copyright 2018 Alexey Andreev.
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
    private int version;

    public TAtomicLong() {
    }

    public TAtomicLong(long value) {
        this.value = value;
    }

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        value = newValue;
        version++;
    }

    public final void lazySet(long newValue) {
        value = newValue;
        version++;
    }

    public final long getAndSet(long newValue) {
        long result = value;
        value = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(long expect, long update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(long expect, long update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final long getAndIncrement() {
        version++;
        return value++;
    }

    public final long getAndDecrement() {
        version++;
        return value--;
    }

    public final long getAndAdd(long delta) {
        version++;
        long result = value;
        value += delta;
        return result;
    }

    public final long incrementAndGet() {
        version++;
        return ++value;
    }

    public final long decrementAndGet() {
        version++;
        return --value;
    }

    public final long addAndGet(long delta) {
        version++;
        value += delta;
        return value;
    }

    public final long getAndUpdate(LongUnaryOperator updateFunction) {
        int expectedVersion;
        long result;
        long newValue;
        do {
            expectedVersion = version;
            result = value;
            newValue = updateFunction.applyAsLong(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final long updateAndGet(LongUnaryOperator updateFunction) {
        int expectedVersion;
        long newValue;
        do {
            expectedVersion = version;
            newValue = updateFunction.applyAsLong(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return newValue;
    }

    public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        int expectedVersion;
        long newValue;
        long result;
        do {
            expectedVersion = version;
            result = value;
            newValue = accumulatorFunction.applyAsLong(value, x);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        int expectedVersion;
        long newValue;
        do {
            expectedVersion = version;
            newValue = accumulatorFunction.applyAsLong(value, x);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return newValue;
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
