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

import org.teavm.backend.wasm.runtime.WasmSupport;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.util.random.RandUtils;
import org.teavm.classlib.java.util.random.TRandomGenerator;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSBody;

public class TRandom extends TObject implements TRandomGenerator, TSerializable {
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

    @Override
    public int nextInt() {
        return (int) (nextDouble(0, 4294967296.0) + Integer.MIN_VALUE);
    }

    @Override
    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return (int) (nextDouble() * n);
    }

    @Override
    public long nextLong() {
        return ((long) nextInt() << 32) | nextInt();
    }

    @Override
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

    @Override
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

    @Override
    public boolean nextBoolean() {
        return nextInt() % 2 == 0;
    }

    @Override
    public float nextFloat() {
        return (float) nextDouble();
    }

    @Override
    public float nextFloat(float bound) {
        return (float) nextDouble(bound);
    }

    @Override
    public float nextFloat(float origin, float bound) {
        return (float) nextDouble(origin, bound);
    }

    @Override
    public double nextDouble() {
        if (PlatformDetector.isC()) {
            return crand();
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmSupport.random();
        } else {
            return random();
        }
    }

    @Import(name = "teavm_rand")
    @Unmanaged
    private static native double crand();

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

        double[] pair = RandUtils.pairGaussian(this);
        haveStoredGaussian = true;
        storedGaussian = pair[1];

        return pair[0];
    }

    @JSBody(script = "return Math.random();")
    @Import(module = "teavmMath", name = "random")
    @Unmanaged
    private static native double random();
}
