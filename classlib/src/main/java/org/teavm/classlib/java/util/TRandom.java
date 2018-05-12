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

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.interop.Import;
import org.teavm.jso.JSBody;

public class TRandom extends TObject implements TSerializable {

    /** A stored gaussian value for nextGaussian() */
    private double storedGaussian;
    
    /** Whether storedGuassian value is valid */
    private boolean haveStoredGaussian;
    
    public TRandom() {
    }

    public TRandom(@SuppressWarnings("unused") long seed) {
    }

    public void setSeed(@SuppressWarnings("unused") long seed) {
    }

    protected int next(int bits) {
        if (bits == 32) {
            return (int) (nextDouble() * ((1L << 32) - 1) + Integer.MIN_VALUE);
        } else {
            return (int) (nextDouble() * (1L << TMath.min(32, bits)));
        }
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) next(8);
        }
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        return (int) (nextDouble() * n);
    }

    public long nextLong() {
        return ((long) nextInt() << 32) | nextInt();
    }

    public boolean nextBoolean() {
        return nextInt() % 2 == 0;
    }

    public float nextFloat() {
        return (float) nextDouble();
    }

    public double nextDouble() {
        if (PlatformDetector.isC()) {
            return crand();
        } else {
            return random();
        }
    }

    @Import(name = "TeaVM_rand")
    private static native double crand();

    /**
     * Generate a random number with Gaussian distribution:
     * centered around 0 with a standard deviation of 1.0.
     */
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

        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);

        double m = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        storedGaussian = v2 * m;
        haveStoredGaussian = true;

        return v1 * m;
    }

    @JSBody(script = "return Math.random();")
    @Import(module = "teavmMath", name = "random")
    private static native double random();
}
