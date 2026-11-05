/*
 *  Copyright 2026 TeaVM contributors.
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
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class TAtomicReferenceArray<E> implements Serializable {
    private final AtomicReference<E>[] elements;

    @SuppressWarnings("unchecked")
    public TAtomicReferenceArray(int length) {
        if (length < 0) {
            throw new NegativeArraySizeException();
        }
        elements = (AtomicReference<E>[]) new AtomicReference<?>[length];
        for (int i = 0; i < length; i++) {
            elements[i] = new AtomicReference<>();
        }
    }

    public TAtomicReferenceArray(E[] array) {
        this(Objects.requireNonNull(array).length);
        for (int i = 0; i < array.length; i++) {
            elements[i].set(array[i]);
        }
    }

    public final int length() {
        return elements.length;
    }

    private AtomicReference<E> elementAt(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException();
        }
        return elements[index];
    }

    public final E get(int index) {
        return elementAt(index).get();
    }

    public final void set(int index, E value) {
        elementAt(index).set(value);
    }

    public final void lazySet(int index, E value) {
        elementAt(index).lazySet(value);
    }

    public final E getAndSet(int index, E value) {
        return elementAt(index).getAndSet(value);
    }

    public final boolean compareAndSet(int index, E expectedValue, E newValue) {
        return elementAt(index).compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSet(int index, E expectedValue, E newValue) {
        return compareAndSet(index, expectedValue, newValue);
    }

    public final E getAndUpdate(int index, UnaryOperator<E> updateFunction) {
        E previous;
        E next;
        do {
            previous = get(index);
            next = Objects.requireNonNull(updateFunction).apply(previous);
        } while (!compareAndSet(index, previous, next));
        return previous;
    }

    public final E updateAndGet(int index, UnaryOperator<E> updateFunction) {
        E previous;
        E next;
        do {
            previous = get(index);
            next = Objects.requireNonNull(updateFunction).apply(previous);
        } while (!compareAndSet(index, previous, next));
        return next;
    }

    public final E getAndAccumulate(int index, E value, BinaryOperator<E> accumulatorFunction) {
        E previous;
        E next;
        do {
            previous = get(index);
            next = Objects.requireNonNull(accumulatorFunction).apply(previous, value);
        } while (!compareAndSet(index, previous, next));
        return previous;
    }

    public final E accumulateAndGet(int index, E value, BinaryOperator<E> accumulatorFunction) {
        E previous;
        E next;
        do {
            previous = get(index);
            next = Objects.requireNonNull(accumulatorFunction).apply(previous, value);
        } while (!compareAndSet(index, previous, next));
        return next;
    }

    public final E getPlain(int index) {
        return elementAt(index).getPlain();
    }

    public final void setPlain(int index, E value) {
        elementAt(index).setPlain(value);
    }

    public final E getOpaque(int index) {
        return elementAt(index).getOpaque();
    }

    public final void setOpaque(int index, E value) {
        elementAt(index).setOpaque(value);
    }

    public final E getAcquire(int index) {
        return elementAt(index).getAcquire();
    }

    public final void setRelease(int index, E value) {
        elementAt(index).setRelease(value);
    }

    public final E compareAndExchange(int index, E expectedValue, E newValue) {
        return elementAt(index).compareAndExchange(expectedValue, newValue);
    }

    public final E compareAndExchangeAcquire(int index, E expectedValue, E newValue) {
        return compareAndExchange(index, expectedValue, newValue);
    }

    public final E compareAndExchangeRelease(int index, E expectedValue, E newValue) {
        return compareAndExchange(index, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int index, E expectedValue, E newValue) {
        return compareAndSet(index, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(int index, E expectedValue, E newValue) {
        return compareAndSet(index, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(int index, E expectedValue, E newValue) {
        return compareAndSet(index, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int index, E expectedValue, E newValue) {
        return compareAndSet(index, expectedValue, newValue);
    }

    @Override
    public String toString() {
        Object[] snapshot = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            snapshot[i] = get(i);
        }
        return Arrays.toString(snapshot);
    }
}
