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
package org.teavm.classlib.impl;

import java.util.function.DoubleSupplier;

public class RandomUtils {
    private RandomUtils() {
    }

    public static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkBound(float bound) {
        if (!(bound > 0.0 && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkBound(double bound) {
        if (!(bound > 0.0 && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkBound(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkRange(float origin, float bound) {
        if (!(origin < bound && bound - origin < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkRange(double origin, double bound) {
        if (!(origin < bound && bound - origin < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    public static double[] pairGaussian(DoubleSupplier rng) {
        /*
         * This implementation uses the polar method to generate two gaussian
         * values at a time. One is returned, and the other is stored to be returned
         * next time.
         */
        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * rng.getAsDouble() - 1;
            v2 = 2 * rng.getAsDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);

        double m = StrictMath.sqrt(-2 * StrictMath.log(s) / s);

        return new double[] { v1 * m, v2 * m };
    }
}
