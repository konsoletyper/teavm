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

/**
 * One or more variables that together maintain an initially zero
 * {@code double} sum. When updates (method {@link #add}) are contended
 * across threads, the set of variables may grow dynamically to reduce
 * contention.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, this class
 * provides the same API as the JDK's DoubleAdder but without actual
 * striping since there is no contention. All operations are backed
 * by a single double value.</p>
 */
public class TDoubleAdder extends TStriped64 implements Serializable {
    private double doubleValue;

    public TDoubleAdder() {
    }

    public void add(double x) {
        doubleValue += x;
    }

    public double sum() {
        return doubleValue;
    }

    public void reset() {
        doubleValue = 0;
    }

    public double sumThenReset() {
        double result = doubleValue;
        doubleValue = 0;
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(doubleValue);
    }

    @Override
    public double doubleValue() {
        return doubleValue;
    }

    @Override
    public long longValue() {
        return (long) doubleValue;
    }

    @Override
    public int intValue() {
        return (int) doubleValue;
    }

    @Override
    public float floatValue() {
        return (float) doubleValue;
    }
}
