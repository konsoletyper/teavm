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

public interface RandUtils {
    static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException();
        }
    }

    static void checkBound(float bound) {
        if (!(bound > 0.0 && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    static void checkBound(double bound) {
        if (!(bound > 0.0 && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    static void checkBound(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
    }

    static void checkRange(float origin, float bound) {
        if (!(origin < bound && (bound - origin) < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    static void checkRange(double origin, double bound) {
        if (!(origin < bound && (bound - origin) < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException();
        }
    }

    static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException();
        }
    }

    static long boundedNextLong(TRandomGenerator rng, long origin, long bound) {
        long range = bound - origin;
        if (range > 0) {
            return rng.nextLong(range) + origin;
        } else {
            while (true) {
                long value = rng.nextLong();
                if (value >= origin && value < bound) {
                    return value;
                }
            }
        }
    }

    static long boundedNextLong(TRandomGenerator rng, long bound) {
        long mask = (Long.highestOneBit(bound) << 1) - 1;
        while (true) {
            long r = rng.nextLong() & mask;
            if (r < bound) {
                return r;
            }
        }
    }

    static int boundedNextInt(TRandomGenerator rng, int origin, int bound) {
        int range = bound - origin;
        if (range > 0) {
            return rng.nextInt(range) + origin;
        } else {
            while (true) {
                int value = rng.nextInt();
                if (value >= origin && value < bound) {
                    return value;
                }
            }
        }
    }

    static int boundedNextInt(TRandomGenerator rng, int bound) {
        int mask = (Integer.highestOneBit(bound) << 1) - 1;
        while (true) {
            int r = rng.nextInt() & mask;
            if (r < bound) {
                return r;
            }
        }
    }

    static double boundedNextDouble(TRandomGenerator rng, double origin, double bound) {
        double r = rng.nextDouble();
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) {
                r = Math.nextAfter(bound, origin);
            }
        }
        return r;
    }

    static double boundedNextDouble(TRandomGenerator rng, double bound) {
        // Specialize boundedNextDouble for origin == 0, bound > 0
        double r = rng.nextDouble();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    static float boundedNextFloat(TRandomGenerator rng, float origin, float bound) {
        float r = rng.nextFloat();
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) {
                r = Math.nextAfter(bound, origin);
            }
        }
        return r;
    }

    static float boundedNextFloat(TRandomGenerator rng, float bound) {
        // Specialize boundedNextFloat for origin == 0, bound > 0
        float r = rng.nextFloat();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
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
