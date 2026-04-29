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
import java.util.function.DoubleBinaryOperator;

/**
 * One or more variables that together maintain a running {@code double}
 * value updated using a supplied function. When updates (method
 * {@link #accumulate}) are contended across threads, the set of variables
 * may grow dynamically to reduce contention.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, this class
 * provides the same API as the JDK's DoubleAccumulator but without actual
 * striping since there is no contention.</p>
 */
public class TDoubleAccumulator extends TStriped64 implements Serializable {
    private final DoubleBinaryOperator function;
    private final long identity;
    private double value;

    public TDoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        this.function = accumulatorFunction;
        this.identity = Double.doubleToLongBits(identity);
        this.value = identity;
    }

    public void accumulate(double x) {
        value = function.applyAsDouble(value, x);
    }

    public double get() {
        return value;
    }

    public void reset() {
        value = Double.longBitsToDouble(identity);
    }

    public double getThenReset() {
        double result = value;
        value = Double.longBitsToDouble(identity);
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public double doubleValue() {
        return get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }
}
