/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util.random;

import org.teavm.classlib.java.util.stream.TDoubleStream;
import org.teavm.classlib.java.util.stream.TIntStream;
import org.teavm.classlib.java.util.stream.TLongStream;

public interface TRandomGenerator {
    default boolean isDeprecated() {
        return false;
    }

    default TDoubleStream doubles() {
        return TDoubleStream.generate(this::nextDouble);
    }

    default TDoubleStream doubles(double origin, double bound) {
        checkRange(origin, bound);
        return TDoubleStream.generate(() -> nextDouble(origin, bound));
    }

    default TDoubleStream doubles(long streamSize) {
        checkStreamSize(streamSize);
        return doubles().limit(streamSize);
    }

    default TDoubleStream doubles(long streamSize, double origin,
            double bound) {
        checkStreamSize(streamSize);
        checkRange(origin, bound);
        return doubles(origin, bound).limit(streamSize);
    }

    default TIntStream ints() {
        return TIntStream.generate(this::nextInt);
    }

    default TIntStream ints(int origin, int bound) {
        checkRange(origin, bound);
        return TIntStream.generate(() -> nextInt(origin, bound));
    }

    default TIntStream ints(long streamSize) {
        checkStreamSize(streamSize);
        return ints().limit(streamSize);
    }

    default TIntStream ints(long streamSize, int origin,
            int bound) {
        checkStreamSize(streamSize);
        checkRange(origin, bound);
        return ints(origin, bound).limit(streamSize);
    }

    default TLongStream longs() {
        return TLongStream.generate(this::nextLong);
    }

    default TLongStream longs(long origin, long bound) {
        checkRange(origin, bound);
        return TLongStream.generate(() -> nextLong(origin, bound));
    }

    default TLongStream longs(long streamSize) {
        checkStreamSize(streamSize);
        return longs().limit(streamSize);
    }

    default TLongStream longs(long streamSize, long origin,
            long bound) {
        checkStreamSize(streamSize);
        checkRange(origin, bound);
        return longs(origin, bound).limit(streamSize);
    }

    default boolean nextBoolean() {
        return nextInt() < 0;
    }

    default void nextBytes(byte[] bytes) {
        if (bytes.length == 0) {
            return;
        }
        int len = (bytes.length - 1) / Integer.BYTES + 1;
        for (int i = 0; i < len; i++) {
            int rnd = nextInt();
            for (int j = 0; j < Integer.BYTES; j++) {
                int idx = Integer.BYTES * i + j;
                if (idx < bytes.length) {
                    bytes[idx] = (byte) (0xFF & (rnd >> (i * Byte.SIZE)));
                }
            }
        }
    }

    default float nextFloat() {
        return (nextInt() >>> 8) * 0x1.0p-24f;
    }

    default float nextFloat(float bound) {
        checkBound(bound);
        float r = nextFloat() * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default float nextFloat(float origin, float bound) {
        checkRange(origin, bound);
        float r = nextFloat() * (bound - origin) + origin;
        if (r >= bound) {
            r = Math.nextAfter(bound, origin);
        }
        return r;
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default double nextDouble(double bound) {
        checkBound(bound);
        double r = nextDouble() * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default double nextDouble(double origin, double bound) {
        checkRange(origin, bound);
        double r = nextDouble() * (bound - origin) + origin;
        if (r >= bound) {
            r = Math.nextAfter(bound, origin);
        }
        return r;
    }

    default int nextInt() {
        return (int)(nextLong() >>> 32);
    }

    default int nextInt(int bound) {
        checkBound(bound);
        int mask = (Integer.highestOneBit(bound) << 1) - 1;
        while (true) {
            int r = nextInt() & mask;
            if (r < bound) {
                return r;
            }
        }
    }

    default int nextInt(int origin, int bound) {
        checkRange(origin, bound);
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

    long nextLong();

    default long nextLong(long bound) {
        checkBound(bound);
        long mask = (Long.highestOneBit(bound) << 1) - 1;
        while (true) {
            long r = nextLong() & mask;
            if (r < bound) {
                return r;
            }
        }
    }

    default long nextLong(long origin, long bound) {
        checkRange(origin, bound);
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

    default double nextGaussian() {
        return pairGaussian(this)[0];
    }

    default double nextGaussian(double mean, double stddev) {
        if (stddev < 0.0) {
            throw new IllegalArgumentException();
        }
        return mean + stddev * nextGaussian();
    }

    //********************************** UTILITY *************************************************//

    private static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkBound(float bound) {
        if (!(bound > 0.0 && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkBound(double bound) {
        if (!(bound > 0.0 && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkBound(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkRange(float origin, float bound) {
        if (!(origin < bound && bound - origin < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkRange(double origin, double bound) {
        if (!(origin < bound && bound - origin < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    private static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    static double[] pairGaussian(TRandomGenerator rng) {
        /*
         * This implementation uses the polar method to generate two gaussian
         * values at a time. One is returned, and the other is stored to be returned
         * next time.
         */
        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * rng.nextDouble() - 1;
            v2 = 2 * rng.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);

        double m = StrictMath.sqrt(-2 * StrictMath.log(s) / s);

        return new double[] { v1 * m, v2 * m };
    }
}
