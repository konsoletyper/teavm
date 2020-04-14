/*
 *  Copyright 2020 Alexey Andreev.
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
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class TAtomicReference<V> implements Serializable {
    private V value;
    private volatile int version;

    public TAtomicReference() {
    }

    public TAtomicReference(V value) {
        this.value = value;
    }

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        value = newValue;
        version++;
    }

    public final void lazySet(V newValue) {
        value = newValue;
        version++;
    }

    public final V getAndSet(V newValue) {
        V result = value;
        value = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(V expect, V update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(V expect, V update) {
        if (value != expect) {
            return false;
        }
        value = update;
        version++;
        return true;
    }

    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        int expectedVersion;
        V result;
        V newValue;
        do {
            expectedVersion = version;
            result = value;
            newValue = updateFunction.apply(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        int expectedVersion;
        V newValue;
        do {
            expectedVersion = version;
            newValue = updateFunction.apply(value);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return newValue;
    }

    public final V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
        int expectedVersion;
        V newValue;
        V result;
        do {
            expectedVersion = version;
            result = value;
            newValue = accumulatorFunction.apply(value, x);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return result;
    }

    public final V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
        int expectedVersion;
        V newValue;
        do {
            expectedVersion = version;
            newValue = accumulatorFunction.apply(value, x);
        } while (expectedVersion != version);
        ++version;
        value = newValue;
        return newValue;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public final V getPlain() {
        return value;
    }

    public final void setPlain(V value) {
        this.value = value;
    }

    public final V getOpaque() {
        return value;
    }

    public final void setOpaque(V value) {
        this.value = value;
    }

    public final V getAcquire() {
        return value;
    }

    public final void setRelease(V value) {
        this.value = value;
    }

    public final V compareAndExchange(V expectedValue, V newValue) {
        if (value == expectedValue) {
            value = newValue;
            return expectedValue;
        } else {
            return value;
        }
    }
}
