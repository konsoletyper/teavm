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

import org.teavm.classlib.impl.RandomUtils;
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
        RandomUtils.checkRange(origin, bound);
        return TDoubleStream.generate(() -> nextDouble(origin, bound));
    }

    default TDoubleStream doubles(long streamSize) {
        RandomUtils.checkStreamSize(streamSize);
        return doubles().limit(streamSize);
    }

    default TDoubleStream doubles(long streamSize, double origin,
            double bound) {
        RandomUtils.checkStreamSize(streamSize);
        RandomUtils.checkRange(origin, bound);
        return doubles(origin, bound).limit(streamSize);
    }

    default TIntStream ints() {
        return TIntStream.generate(this::nextInt);
    }

    default TIntStream ints(int origin, int bound) {
        RandomUtils.checkRange(origin, bound);
        return TIntStream.generate(() -> nextInt(origin, bound));
    }

    default TIntStream ints(long streamSize) {
        RandomUtils.checkStreamSize(streamSize);
        return ints().limit(streamSize);
    }

    default TIntStream ints(long streamSize, int origin,
            int bound) {
        RandomUtils.checkStreamSize(streamSize);
        RandomUtils.checkRange(origin, bound);
        return ints(origin, bound).limit(streamSize);
    }

    default TLongStream longs() {
        return TLongStream.generate(this::nextLong);
    }

    default TLongStream longs(long origin, long bound) {
        RandomUtils.checkRange(origin, bound);
        return TLongStream.generate(() -> nextLong(origin, bound));
    }

    default TLongStream longs(long streamSize) {
        RandomUtils.checkStreamSize(streamSize);
        return longs().limit(streamSize);
    }

    default TLongStream longs(long streamSize, long origin,
            long bound) {
        RandomUtils.checkStreamSize(streamSize);
        RandomUtils.checkRange(origin, bound);
        return longs(origin, bound).limit(streamSize);
    }

    default boolean nextBoolean() {
        return nextInt() < 0;
    }

    default void nextBytes(byte[] bytes) {
        // The JDK's algorithm: the bytes of each nextLong(), low byte first
        for (int i = 0, len = bytes.length; i < len;) {
            long rnd = nextLong();
            for (int n = Math.min(len - i, Long.BYTES); n-- > 0; rnd >>>= Byte.SIZE) {
                bytes[i++] = (byte) rnd;
            }
        }
    }

    default float nextFloat() {
        return (nextInt() >>> 8) * 0x1.0p-24f;
    }

    default float nextFloat(float bound) {
        RandomUtils.checkBound(bound);
        float res = nextFloat() * bound;
        if (res >= bound) {
            res = Math.nextDown(bound);
        }
        return res;
    }

    default float nextFloat(float origin, float bound) {
        RandomUtils.checkRange(origin, bound);
        float res = nextFloat() * (bound - origin) + origin;
        if (res >= bound) {
            res = Math.nextAfter(bound, origin);
        }
        return res;
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default double nextDouble(double bound) {
        RandomUtils.checkBound(bound);
        double res = nextDouble() * bound;
        if (res >= bound) {
            res = Math.nextDown(bound);
        }
        return res;
    }

    default double nextDouble(double origin, double bound) {
        RandomUtils.checkRange(origin, bound);
        double res = nextDouble() * (bound - origin) + origin;
        if (res >= bound) {
            res = Math.nextAfter(bound, origin);
        }
        return res;
    }

    default int nextInt() {
        return (int) (nextLong() >>> 32);
    }

    default int nextInt(int bound) {
        RandomUtils.checkBound(bound);
        return boundedNextInt(this, 0, bound);
    }

    default int nextInt(int origin, int bound) {
        RandomUtils.checkRange(origin, bound);
        return boundedNextInt(this, origin, bound);
    }

    long nextLong();

    default long nextLong(long bound) {
        RandomUtils.checkBound(bound);
        return boundedNextLong(this, 0, bound);
    }

    default long nextLong(long origin, long bound) {
        RandomUtils.checkRange(origin, bound);
        return boundedNextLong(this, origin, bound);
    }

    default double nextGaussian() {
        return RandomUtils.pairGaussian(this::nextDouble)[0];
    }

    default double nextGaussian(double mean, double stddev) {
        if (stddev < 0.0) {
            throw new IllegalArgumentException();
        }
        return mean + stddev * nextGaussian();
    }

    /**
     * The JDK's RandomSupport.boundedNextInt, which its nextInt(bound) and nextInt(origin, bound) use.
     * Requires {@code origin < bound}.
     */
    private static int boundedNextInt(TRandomGenerator rng, int origin, int bound) {
        int r = rng.nextInt();
        int n = bound - origin;
        int m = n - 1;
        if ((n & m) == 0) {
            // the range is a power of two
            return (r & m) + origin;
        }
        if (n > 0) {
            // reject the candidates from the incomplete last block of n values
            int u = r >>> 1;
            r = u % n;
            while (u + m - r < 0) {
                u = rng.nextInt() >>> 1;
                r = u % n;
            }
            return r + origin;
        }
        // the range does not fit in an int
        while (r < origin || r >= bound) {
            r = rng.nextInt();
        }
        return r;
    }

    /** {@link #boundedNextInt} for longs, as the JDK's RandomSupport.boundedNextLong. */
    private static long boundedNextLong(TRandomGenerator rng, long origin, long bound) {
        long r = rng.nextLong();
        long n = bound - origin;
        long m = n - 1;
        if ((n & m) == 0) {
            return (r & m) + origin;
        }
        if (n > 0) {
            long u = r >>> 1;
            r = u % n;
            while (u + m - r < 0) {
                u = rng.nextLong() >>> 1;
                r = u % n;
            }
            return r + origin;
        }
        while (r < origin || r >= bound) {
            r = rng.nextLong();
        }
        return r;
    }
}
