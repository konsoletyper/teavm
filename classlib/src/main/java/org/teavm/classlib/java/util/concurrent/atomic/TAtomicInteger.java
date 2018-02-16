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

public class TAtomicInteger extends Number implements Serializable {
    private int value;

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
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final int getAndSet(int newValue) {
        int result = value;
        value = newValue;
        return result;
    }

    public final boolean compareAndSet(int expect, int update) {
        if (value != expect) {
            return false;
        }
        value = update;
        return true;
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        if (value != expect) {
            return false;
        }
        value = update;
        return true;
    }

    public final int getAndIncrement() {
        return value++;
    }

    public final int getAndDecrement() {
        return value--;
    }

    public final int getAndAdd(int delta) {
        int result = value;
        value += delta;
        return result;
    }

    public final int incrementAndGet() {
        return ++value;
    }

    public final int decrementAndGet() {
        return --value;
    }

    public final int addAndGet(int delta) {
        value += delta;
        return value;
    }

    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int result = value;
        value = updateFunction.applyAsInt(value);
        return result;
    }

    public final int updateAndGet(IntUnaryOperator updateFunction) {
        value = updateFunction.applyAsInt(value);
        return value;
    }

    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int result = value;
        value = accumulatorFunction.applyAsInt(value, x);
        return result;
    }

    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        value = accumulatorFunction.applyAsInt(value, x);
        return value;
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
