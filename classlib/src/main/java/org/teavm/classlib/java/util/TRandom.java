/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import org.teavm.classlib.impl.RandomUtils;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.util.random.TRandomGenerator;

public class TRandom extends TObject implements TRandomGenerator, TSerializable {
    /** A stored gaussian value for nextGaussian() */
    private double storedGaussian;

    /** Whether storedGuassian value is valid */
    private boolean haveStoredGaussian;

    // Could be improved more efficiently for JS backend
    private long seed;

    public TRandom() {
        this((long) (Math.random() * ((1L << 48)) - 1));
    }

    public TRandom(@SuppressWarnings("unused") long seed) {
        setSeed(seed);
    }

    public void setSeed(@SuppressWarnings("unused") long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
    }

    @Override
    public int nextInt() {
        return next(32);
    }

    protected int next(int bits) {
        var newSeed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        seed = newSeed;
        return (int) (newSeed >> (48 - bits));
    }

    @Override
    public long nextLong() {
        return ((long) nextInt() << 32) + nextInt();
    }

    @Override
    public float nextFloat() {
        return next(24) * 0x1p-24f;
    }

    @Override
    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * 0x1p-53;
    }

    /**
     * Generate a random number with Gaussian distribution:
     * centered around 0 with a standard deviation of 1.0.
     */
    @Override
    public double nextGaussian() {
        /*
         * This implementation uses the polar method to generate two gaussian
         * values at a time. One is returned, and the other is stored to be returned
         * next time.
         */
        if (haveStoredGaussian) {
            haveStoredGaussian = false;
            return storedGaussian;
        }

        double[] pair = RandomUtils.pairGaussian(this::nextDouble);
        haveStoredGaussian = true;
        storedGaussian = pair[1];

        return pair[0];
    }
}
