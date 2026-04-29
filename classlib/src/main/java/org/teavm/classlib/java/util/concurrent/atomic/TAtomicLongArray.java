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
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class TAtomicLongArray implements Serializable {
    private final long[] array;
    private volatile int version;

    public TAtomicLongArray(int length) {
        array = new long[length];
    }

    public TAtomicLongArray(long[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final long get(int i) {
        checkIndex(i);
        return array[i];
    }

    public final void set(int i, long newValue) {
        checkIndex(i);
        array[i] = newValue;
        version++;
    }

    public final void lazySet(int i, long newValue) {
        set(i, newValue);
    }

    public final long getAndSet(int i, long newValue) {
        checkIndex(i);
        long result = array[i];
        array[i] = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(int i, long expect, long update) {
        checkIndex(i);
        if (array[i] != expect) {
            return false;
        }
        array[i] = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(int i, long expect, long update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetVolatile(int i, long expect, long update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetPlain(int i, long expect, long update) {
        return compareAndSet(i, expect, update);
    }

    public final long getAndIncrement(int i) {
        checkIndex(i);
        version++;
        return array[i]++;
    }

    public final long getAndDecrement(int i) {
        checkIndex(i);
        version++;
        return array[i]--;
    }

    public final long getAndAdd(int i, long delta) {
        checkIndex(i);
        version++;
        long result = array[i];
        array[i] += delta;
        return result;
    }

    public final long incrementAndGet(int i) {
        checkIndex(i);
        version++;
        return ++array[i];
    }

    public final long decrementAndGet(int i) {
        checkIndex(i);
        version++;
        return --array[i];
    }

    public final long addAndGet(int i, long delta) {
        checkIndex(i);
        version++;
        array[i] += delta;
        return array[i];
    }

    public final long getAndUpdate(int i, LongUnaryOperator updateFunction) {
        checkIndex(i);
        int expectedVersion;
        long result;
        long newValue;
        do {
            expectedVersion = version;
            result = array[i];
            newValue = updateFunction.applyAsLong(result);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return result;
    }

    public final long updateAndGet(int i, LongUnaryOperator updateFunction) {
        checkIndex(i);
        int expectedVersion;
        long newValue;
        do {
            expectedVersion = version;
            newValue = updateFunction.applyAsLong(array[i]);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return newValue;
    }

    public final long getAndAccumulate(int i, long x, LongBinaryOperator accumulatorFunction) {
        checkIndex(i);
        int expectedVersion;
        long result;
        long newValue;
        do {
            expectedVersion = version;
            result = array[i];
            newValue = accumulatorFunction.applyAsLong(result, x);
        } while (expectedVersion != version);
        version++;
        array[i] = newValue;
        return result;
    }

    public final long accumulateAndGet(int i, long x, LongBinaryOperator accumulatorFunction) {
        checkIndex(i);
        int expectedVersion;
        long newValue;
        do {
            expectedVersion = version;
            newValue = accumulatorFunction.applyAsLong(array[i], x);
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
