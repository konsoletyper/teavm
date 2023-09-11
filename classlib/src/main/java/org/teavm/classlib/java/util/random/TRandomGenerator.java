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

    default TDoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return TDoubleStream.generate(() -> nextDouble(randomNumberOrigin, randomNumberBound));
    }

    default TDoubleStream doubles(long streamSize) {
        RandUtils.checkStreamSize(streamSize);

        return doubles().limit(streamSize);
    }

    default TDoubleStream doubles(long streamSize, double randomNumberOrigin,
            double randomNumberBound) {
        RandUtils.checkStreamSize(streamSize);
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return doubles(randomNumberOrigin, randomNumberBound).limit(streamSize);
    }

    default TIntStream ints() {
        return TIntStream.generate(this::nextInt);
    }

    default TIntStream ints(int randomNumberOrigin, int randomNumberBound) {
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return TIntStream.generate(() -> nextInt(randomNumberOrigin, randomNumberBound));
    }

    default TIntStream ints(long streamSize) {
        RandUtils.checkStreamSize(streamSize);

        return ints().limit(streamSize);
    }

    default TIntStream ints(long streamSize, int randomNumberOrigin,
            int randomNumberBound) {
        RandUtils.checkStreamSize(streamSize);
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return ints(randomNumberOrigin, randomNumberBound).limit(streamSize);
    }

    default TLongStream longs() {
        return TLongStream.generate(this::nextLong);
    }

    default TLongStream longs(long randomNumberOrigin, long randomNumberBound) {
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return TLongStream.generate(() -> nextLong(randomNumberOrigin, randomNumberBound));
    }

    default TLongStream longs(long streamSize) {
        RandUtils.checkStreamSize(streamSize);

        return longs().limit(streamSize);
    }

    default TLongStream longs(long streamSize, long randomNumberOrigin,
            long randomNumberBound) {
        RandUtils.checkStreamSize(streamSize);
        RandUtils.checkRange(randomNumberOrigin, randomNumberBound);

        return longs(randomNumberOrigin, randomNumberBound).limit(streamSize);
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
        RandUtils.checkBound(bound);

        return RandUtils.boundedNextFloat(this, bound);
    }

    default float nextFloat(float origin, float bound) {
        RandUtils.checkRange(origin, bound);

        return RandUtils.boundedNextFloat(this, origin, bound);
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default double nextDouble(double bound) {
        RandUtils.checkBound(bound);

        return RandUtils.boundedNextDouble(this, bound);
    }

    default double nextDouble(double origin, double bound) {
        RandUtils.checkRange(origin, bound);

        return RandUtils.boundedNextDouble(this, origin, bound);
    }

    default int nextInt() {
        return (int)(nextLong() >>> 32);
    }

    default int nextInt(int bound) {
        RandUtils.checkBound(bound);

        return RandUtils.boundedNextInt(this, bound);
    }

    default int nextInt(int origin, int bound) {
        RandUtils.checkRange(origin, bound);

        return RandUtils.boundedNextInt(this, origin, bound);
    }

    long nextLong();

    default long nextLong(long bound) {
        RandUtils.checkBound(bound);

        return RandUtils.boundedNextLong(this, bound);
    }

    default long nextLong(long origin, long bound) {
        RandUtils.checkRange(origin, bound);

        return RandUtils.boundedNextLong(this, origin, bound);
    }

    default double nextGaussian() {
        return RandUtils.pairGaussian(this)[0];
    }

    default double nextGaussian(double mean, double stddev) {
        if (stddev < 0.0) {
            throw new IllegalArgumentException();
        }
        return mean + stddev * nextGaussian();
    }
}
