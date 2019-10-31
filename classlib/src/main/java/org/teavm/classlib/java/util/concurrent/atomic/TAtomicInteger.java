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
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class TAtomicInteger extends Number implements Serializable {
    private int value;
    private volatile int version;

    public TAtomicInteger() {
    }

    public TAtomicInteger(int value) {
        this.value = value;
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
        version++;
    }

    public final void lazySet(int newValue) {
        value = newValue;
        version++;
    }

    public final int getAndSet(int newValue) {
        int result = value;
        value = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(int expect, int update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final int getAndIncrement() {
        version++;
        return value++;
    }

    public final int getAndDecrement() {
        version++;
        return value--;
    }

    public final int getAndAdd(int delta) {
        version++;
        int result = value;
        value += delta;
        return result;
    }

    public final int incrementAndGet() {
        version++;
        return ++value;
    }

    public final int decrementAndGet() {
        version++;
        return --value;
    }

    public final int addAndGet(int delta) {
        version++;
        value += delta;
        return value;
    }

    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int expectedVersion;
        int result;
        int newValue;
        do {
            expectedVersion = version;
            result = value;
            newValue = updateFunction.applyAsInt(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int expectedVersion;
        int newValue;
        do {
            expectedVersion = version;
            newValue = updateFunction.applyAsInt(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return newValue;
    }

    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int expectedVersion;
        int newValue;
        int result;
        do {
            expectedVersion = version;
            result = value;
            newValue = accumulatorFunction.applyAsInt(value, x);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int expectedVersion;
        int newValue;
        do {
            expectedVersion = version;
            newValue = accumulatorFunction.applyAsInt(value, x);
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
        return value;
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
