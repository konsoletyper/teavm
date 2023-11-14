/*
 *  Copyright 2023 Alexey Andreev.
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

import java.lang.reflect.Modifier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TNoSuchFieldException;

public abstract class TAtomicLongFieldUpdater<T> {
    protected TAtomicLongFieldUpdater() {
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public static <U> TAtomicLongFieldUpdater<U> newUpdater(TClass<U> tclass, String fieldName) {
        try {
            var field = tclass.getDeclaredField(fieldName);
            if (!Modifier.isVolatile(field.getModifiers()) || Modifier.isStatic(field.getModifiers())
                    || !field.getType().equals(long.class)) {
                throw new IllegalArgumentException();
            } else {
                field.checkGetAccess();
                field.checkSetAccess();
                return new TReflectionBasedAtomicLongFieldUpdater<>(field);
            }
        } catch (TNoSuchFieldException | TIllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract boolean compareAndSet(T obj, long expect, long update);

    public abstract boolean weakCompareAndSet(T obj, long expect, long update);

    public abstract void set(T obj, long newValue);

    public abstract void lazySet(T obj, long newValue);

    public abstract long get(T obj);

    public long getAndSet(T obj, long newValue) {
        while (true) {
            var currentValue = get(obj);
            if (compareAndSet(obj, currentValue, newValue)) {
                return currentValue;
            }
        }
    }

    public long getAndIncrement(T obj) {
        return getAndAdd(obj, 1);
    }

    public long getAndDecrement(T obj) {
        return getAndAdd(obj, -1);
    }

    public long getAndAdd(T obj, long delta) {
        while (true) {
            var currentValue = get(obj);
            if (compareAndSet(obj, currentValue, currentValue + delta)) {
                return currentValue;
            }
        }
    }

    public long incrementAndGet(T obj) {
        return addAndGet(obj, 1);
    }

    public long decrementAndGet(T obj) {
        return addAndGet(obj, -1);
    }

    public long addAndGet(T obj, long delta) {
        while (true) {
            var currentValue = get(obj);
            if (compareAndSet(obj, currentValue, currentValue + delta)) {
                return currentValue + delta;
            }
        }
    }

    public final long getAndUpdate(T obj, LongUnaryOperator updateFunction) {
        while (true) {
            var currentValue = get(obj);
            var newValue = updateFunction.applyAsLong(currentValue);
            if (compareAndSet(obj, currentValue, newValue)) {
                return currentValue;
            }
        }
    }

    public final long updateAndGet(T obj, LongUnaryOperator updateFunction) {
        while (true) {
            var currentValue = get(obj);
            var newValue = updateFunction.applyAsLong(currentValue);
            if (compareAndSet(obj, currentValue, newValue)) {
                return newValue;
            }
        }
    }

    public final long getAndAccumulate(T obj, long x, LongBinaryOperator accumulatorFunction) {
        while (true) {
            var currentValue = get(obj);
            var newValue = accumulatorFunction.applyAsLong(currentValue, x);
            if (compareAndSet(obj, currentValue, newValue)) {
                return currentValue;
            }
        }
    }

    public final long accumulateAndGet(T obj, long x, LongBinaryOperator accumulatorFunction) {
        while (true) {
            var currentValue = get(obj);
            var newValue = accumulatorFunction.applyAsLong(currentValue, x);
            if (compareAndSet(obj, currentValue, newValue)) {
                return newValue;
            }
        }
    }
}
