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
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class TAtomicReferenceArray<E> implements Serializable {
    private final Object[] array;
    private volatile int version;

    public TAtomicReferenceArray(int length) {
        array = new Object[length];
    }

    public TAtomicReferenceArray(E[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    @SuppressWarnings("unchecked")
    public final E get(int i) {
        checkIndex(i);
        return (E) array[i];
    }

    public final void set(int i, E newValue) {
        checkIndex(i);
        array[i] = newValue;
        version++;
    }

    public final void lazySet(int i, E newValue) {
        set(i, newValue);
    }

    @SuppressWarnings("unchecked")
    public final E getAndSet(int i, E newValue) {
        checkIndex(i);
        E result = (E) array[i];
        array[i] = newValue;
        version++;
        return result;
    }

    public final boolean compareAndSet(int i, E expect, E update) {
        checkIndex(i);
        Object current = array[i];
        if (current != expect) {
            return false;
        }
        array[i] = update;
        version++;
        return true;
    }

    public final boolean weakCompareAndSet(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetVolatile(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
    }

    public final boolean weakCompareAndSetPlain(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
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
