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

/**
 * One or more variables that together maintain a running {@code long}
 * value updated using a supplied function. When updates (method
 * {@link #accumulate}) are contended across threads, the set of variables
 * may grow dynamically to reduce contention.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, this class
 * provides the same API as the JDK's LongAccumulator but without actual
 * striping since there is no contention.</p>
 */
public class TLongAccumulator extends TStriped64 implements Serializable {
    private final LongBinaryOperator function;
    private final long identity;

    public TLongAccumulator(LongBinaryOperator accumulatorFunction, long identity) {
        this.function = accumulatorFunction;
        this.identity = identity;
        this.value = identity;
    }

    public void accumulate(long x) {
        value = function.applyAsLong(value, x);
    }

    public long get() {
        return value;
    }

    public void reset() {
        value = identity;
    }

    public long getThenReset() {
        long result = value;
        value = identity;
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public long longValue() {
        return get();
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public float floatValue() {
        return get();
    }

    @Override
    public double doubleValue() {
        return get();
    }
}
