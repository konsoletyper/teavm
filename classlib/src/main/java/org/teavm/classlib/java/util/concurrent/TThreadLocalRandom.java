/*
 *  Copyright 2020 konsoletyper.
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
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.TRandom;

public class TThreadLocalRandom extends TRandom {
    private static final TThreadLocalRandom current = new TThreadLocalRandom();

    private TThreadLocalRandom() {
    }

    public static TThreadLocalRandom current() {
        return current;
    }

    @Override
    public void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    public int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
        int range = bound - origin;
        if (range > 0) {
            return nextInt(range) + origin;
        } else {
            while (true) {
                int value = nextInt();
                if (value >= origin && value < bound) {
                    return value;
                }
            }
        }
    }

    public long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
        while (true) {
            long value = nextLong();
            long result = value % bound;
            if (value - result + (bound - 1) < 0) {
                return result;
            }
        }
    }

    public long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
        long range = bound - origin;
        if (range > 0) {
            return nextLong(range) + origin;
        } else {
            while (true) {
                long value = nextLong();
                if (value >= origin && value < bound) {
                    return value;
                }
            }
        }
    }

    public double nextDouble(double bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
        double value = nextDouble() * bound;
        if (value == bound) {
            value = Math.nextDown(value);
        }
        return value;
    }

    public double nextDouble(double origin, double bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
        return origin + nextDouble(bound - origin);
    }
}
