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
 * {@code long} sum. When updates (method {@link #add}) are contended
 * across threads, the set of variables may grow dynamically to reduce
 * contention.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, this class
 * provides the same API as the JDK's LongAdder but without actual
 * striping since there is no contention. All operations are backed
 * by a single long value.</p>
 */
public class TLongAdder extends TStriped64 implements Serializable {
    public TLongAdder() {
    }

    public void add(long x) {
        value += x;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public long sum() {
        return value;
    }

    public void reset() {
        value = 0;
    }

    public long sumThenReset() {
        long result = value;
        value = 0;
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
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
