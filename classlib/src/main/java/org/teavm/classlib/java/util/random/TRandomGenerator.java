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
        int mask = (Integer.highestOneBit(bound) << 1) - 1;
        while (true) {
            int res = nextInt() & mask;
            if (res < bound) {
                return res;
            }
        }
    }

    default int nextInt(int origin, int bound) {
        RandomUtils.checkRange(origin, bound);
        int range = bound - origin;
        if (range > 0) {
            return nextInt(range) + origin;
        } else {
            while (true) {
                int res = nextInt();
                if (res >= origin && res < bound) {
                    return res;
                }
            }
        }
    }

    long nextLong();

    default long nextLong(long bound) {
        RandomUtils.checkBound(bound);
        long mask = (Long.highestOneBit(bound) << 1) - 1;
        while (true) {
            long res = nextLong() & mask;
            if (res < bound) {
                return res;
            }
        }
    }

    default long nextLong(long origin, long bound) {
        RandomUtils.checkRange(origin, bound);
        long range = bound - origin;
        if (range > 0) {
            return nextLong(range) + origin;
        } else {
            while (true) {
                long res = nextLong();
                if (res >= origin && res < bound) {
                    return res;
                }
            }
        }
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
}
