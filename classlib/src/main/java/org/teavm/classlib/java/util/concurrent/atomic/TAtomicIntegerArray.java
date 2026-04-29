/*
 *  Copyright 2025 konsoletyper.
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
public class TAtomicIntegerArray implements Serializable {
    private final int[] array;
    private volatile int version;

    public TAtomicIntegerArray(int length) {
        array = new int[length];
    }

    public TAtomicIntegerArray(int[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final int get(int i) {
        checkIndex(i);
        return array[i];
    }

    public final void set(int i, int newValue) {
        checkIndex(i);
        array[i] = newValue;
        version++;
    }

    public final void lazySet(int i, int newValue) {
        set(i, newValue);
    }

    public final int getAndSet(int i, int newValue) {
        checkIndex(i);
        int result = array[i];
        array[i] = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        checkIndex(i);
        if (array[i] != expect) {
            return false;
        }
        array[i] = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetVolatile(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetPlain(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    public final int getAndIncrement(int i) {
        checkIndex(i);
        version++;
        return array[i]++;
    }

    public final int getAndDecrement(int i) {
        checkIndex(i);
        version++;
        return array[i]--;
    }

    public final int getAndAdd(int i, int delta) {
        checkIndex(i);
        version++;
        int result = array[i];
        array[i] += delta;
        return result;
    }

    public final int incrementAndGet(int i) {
        checkIndex(i);
        version++;
        return ++array[i];
    }

    public final int decrementAndGet(int i) {
        checkIndex(i);
        version++;
        return --array[i];
    }

    public final int addAndGet(int i, int delta) {
        checkIndex(i);
        version++;
        array[i] += delta;
        return array[i];
    }

    public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
        checkIndex(i);
        int expectedVersion;
        int result;
        int newValue;
        do {
            expectedVersion = version;
            result = array[i];
            newValue = updateFunction.applyAsInt(result);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return result;
    }

    public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
        checkIndex(i);
        int expectedVersion;
        int newValue;
        do {
            expectedVersion = version;
            newValue = updateFunction.applyAsInt(array[i]);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return newValue;
    }

    public final int getAndAccumulate(int i, int x, IntBinaryOperator accumulatorFunction) {
        checkIndex(i);
        int expectedVersion;
        int result;
        int newValue;
        do {
            expectedVersion = version;
            result = array[i];
            newValue = accumulatorFunction.applyAsInt(result, x);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return result;
    }

    public final int accumulateAndGet(int i, int x, IntBinaryOperator accumulatorFunction) {
        checkIndex(i);
        int expectedVersion;
        int newValue;
        do {
            expectedVersion = version;
            newValue = accumulatorFunction.applyAsInt(array[i], x);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return newValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private void checkIndex(int i) {
        if (i < 0 || i >= array.length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + array.length);
        }
    }
}
