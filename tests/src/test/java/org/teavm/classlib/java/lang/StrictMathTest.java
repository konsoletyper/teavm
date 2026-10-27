/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Test;
import org.teavm.jso.JSBody;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

/**
 * The StrictMath Javadoc: "the Java math library is defined with respect to fdlibm version 5.3", and its methods
 * must give fdlibm's results bit for bit. The expected bits and checksums below come from the JVM's StrictMath.log
 * and log10, and the JVM leg of each test checks them again. Each checksum covers a few thousand to a few hundred
 * thousand inputs, generated from a fixed seed.
 *
 * <p>The bit-exact tests are skipped on C. The C backend's generated code lets clang contract {@code a * b + c} into
 * a fused multiply-add (clang's default -ffp-contract=on, even at -O0), which Java forbids. fdlibm's steps then give
 * a result 1 ulp off: for the input 0x3FF50D3B414EDA2E, C gave 0x3FD18FA30D0C6D53 where fdlibm gives
 * 0x3FD18FA30D0C6D54. With -ffp-contract=off (or {@code #pragma STDC FP_CONTRACT OFF}) they pass on C.
 */
@TeaVMTest
public class StrictMathTest {
    @Test
    @SkipPlatform(TestPlatform.C)
    public void logGivesFdlibmBits() {
        // Inputs for which Node 24's Math.log gives another last bit than fdlibm.
        assertLogBits(0x3FF50D3B414EDA2EL, 0x3FD18FA30D0C6D54L);
        assertLogBits(0x3FD9CD0FFC328D70L, 0xBFED122D47E139FFL);
        assertLogBits(0x400B0433CAEEBD13L, 0x3FF378D841B54204L);
        assertLogBits(0x400599AEB56D9852L, 0x3FEFC8D736D54E56L);
    }

    // Javadoc of StrictMath.log: "If the argument is NaN or less than zero, then the result is NaN. If the argument
    // is positive infinity, then the result is positive infinity. If the argument is positive zero or negative zero,
    // then the result is negative infinity. If the argument is 1.0, then the result is +0.0."
    @Test
    public void logSpecialValues() {
        assertLogBits(0L, 0xFFF0000000000000L);
        assertLogBits(0x8000000000000000L, 0xFFF0000000000000L);
        assertLogBits(0x3FF0000000000000L, 0L);
        assertLogBits(0x7FF0000000000000L, 0x7FF0000000000000L);
        assertTrue(Double.isNaN(StrictMath.log(Double.NaN)));
        assertTrue(Double.isNaN(StrictMath.log(Double.NEGATIVE_INFINITY)));
        assertTrue(Double.isNaN(StrictMath.log(-1.0)));
        assertTrue(Double.isNaN(StrictMath.log(-Double.MIN_VALUE)));
        assertTrue(Double.isNaN(StrictMath.log(-Double.MIN_NORMAL)));
        assertTrue(Double.isNaN(StrictMath.log(-Double.MAX_VALUE)));
        // Smallest and largest subnormals, smallest normal, largest finite value, e.
        assertLogBits(0x0000000000000001L, 0xC0874385446D71C3L);
        assertLogBits(0x000FFFFFFFFFFFFFL, 0xC086232BDD7ABCD2L);
        assertLogBits(0x0010000000000000L, 0xC086232BDD7ABCD2L);
        assertLogBits(0x7FEFFFFFFFFFFFFFL, 0x40862E42FEFA39EFL);
        assertLogBits(0x4005BF0A8B145769L, 0x3FF0000000000000L);
    }

    // Javadoc of StrictMath.log10: "If the argument is NaN or less than zero, then the result is NaN. If the argument
    // is positive infinity, then the result is positive infinity. If the argument is positive zero or negative zero,
    // then the result is negative infinity. If the argument is equal to 10^n for integer n, then the result is n. In
    // particular, if the argument is 1.0 (10^0), then the result is +0.0."
    @Test
    public void log10SpecialValues() {
        assertEquals(Double.NEGATIVE_INFINITY, StrictMath.log10(0.0), 0);
        assertEquals(Double.NEGATIVE_INFINITY, StrictMath.log10(-0.0), 0);
        assertEquals(0L, Double.doubleToRawLongBits(StrictMath.log10(1.0)));
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.log10(Double.POSITIVE_INFINITY), 0);
        assertTrue(Double.isNaN(StrictMath.log10(Double.NaN)));
        assertTrue(Double.isNaN(StrictMath.log10(Double.NEGATIVE_INFINITY)));
        assertTrue(Double.isNaN(StrictMath.log10(-1.0)));
        assertTrue(Double.isNaN(StrictMath.log10(-Double.MIN_VALUE)));
        assertEquals(-323.3062153431158, StrictMath.log10(Double.MIN_VALUE), 0);
        assertEquals(308.25471555991675, StrictMath.log10(Double.MAX_VALUE), 0);
        // Every power of ten up to 10^22 is exact as a double.
        double p = 1;
        for (int n = 0; n <= 22; ++n) {
            assertEquals(n, StrictMath.log10(p), 0, "log10(1e" + n + ")");
            p *= 10;
        }
    }

    @Test
    public void logOfNegativeIsNaN() {
        long state = 1;
        for (int n = 0; n < 4096; ++n) {
            state += 0x9E3779B97F4A7C15L;
            double x = Double.longBitsToDouble(mix(state) | 0x8000000000000000L);
            if (x != 0) {
                assertTrue(Double.isNaN(StrictMath.log(x)), "log(" + x + ")");
                assertTrue(Double.isNaN(StrictMath.log10(x)), "log10(" + x + ")");
            }
        }
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void logOfPowersOfTwo() {
        assertChecksum(0xF66640C133F0DCEFL, powersOfTwo(StrictMath::log));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void log10OfPowersOfTwo() {
        assertChecksum(0x5F3D3CC33690EF6CL, powersOfTwo(StrictMath::log10));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void logNearOne() {
        assertChecksum(0x55113B9AA4159DAFL, nearOne(StrictMath::log));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void log10NearOne() {
        assertChecksum(0xC72BC23BAD96C364L, nearOne(StrictMath::log10));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void logOfSubnormals() {
        assertChecksum(0xC02EA725644C35DAL, subnormals(StrictMath::log));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void log10OfSubnormals() {
        assertChecksum(0xADCE6677EFBFDCCFL, subnormals(StrictMath::log10));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void logAtBranchThresholds() {
        assertChecksum(0x98B52647D60A4A13L, branchThresholds(StrictMath::log));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void log10AtBranchThresholds() {
        assertChecksum(0x6E778649C0DAF841L, branchThresholds(StrictMath::log10));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void logOfRandomValues() {
        assertChecksum(0x76266F79B722C90AL, randomValues(StrictMath::log));
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void log10OfRandomValues() {
        assertChecksum(0xB5FE974954FC2434L, randomValues(StrictMath::log10));
    }

    @Test
    @SkipJVM
    @OnlyPlatform(TestPlatform.JAVASCRIPT)
    public void logDoesNotUseHostMathLog() {
        // A host Math.log need not be fdlibm's: Node 24's differs in the last bit for the inputs above, Chromium
        // 138's does not. Replace it with a wrong function, so that a StrictMath.log that calls it fails.
        breakHostLog();
        try {
            assertLogBits(0x3FF50D3B414EDA2EL, 0x3FD18FA30D0C6D54L);
            assertEquals(3, StrictMath.log10(1000), 0);
        } finally {
            restoreHostLog();
        }
    }

    private static long powersOfTwo(DoubleUnaryOperator f) {
        long h = 0;
        for (int e = -1074; e <= 1023; ++e) {
            long bits = e >= -1022 ? (long) (e + 1023) << 52 : 1L << (e + 1074);
            h = hash(h, f.applyAsDouble(Double.longBitsToDouble(bits)));
        }
        return h;
    }

    private static long nearOne(DoubleUnaryOperator f) {
        long h = 0;
        long one = 0x3FF0000000000000L;
        // Steps of one ulp on both sides of 1, covering f == 0 and the |f| < 2**-20 branch.
        for (int n = 1; n <= 4096; ++n) {
            h = hash(h, f.applyAsDouble(Double.longBitsToDouble(one + n)));
            h = hash(h, f.applyAsDouble(Double.longBitsToDouble(one - n)));
        }
        // Random offsets of up to 2**-20, 2**-10 and 2**-2 on both sides of 1.
        long state = 2;
        for (double scale : new double[] { 0x1.0p-73, 0x1.0p-63, 0x1.0p-55 }) {
            for (int n = 0; n < 8192; ++n) {
                state += 0x9E3779B97F4A7C15L;
                double offset = (mix(state) >>> 11) * scale;
                h = hash(hash(h, f.applyAsDouble(1 + offset)), f.applyAsDouble(1 - offset));
            }
        }
        return h;
    }

    private static long subnormals(DoubleUnaryOperator f) {
        long h = 0;
        for (long bits = 1; bits <= 2048; ++bits) {
            h = hash(h, f.applyAsDouble(Double.longBitsToDouble(bits)));
            h = hash(h, f.applyAsDouble(Double.longBitsToDouble(0x0010000000000000L - bits)));
        }
        long state = 3;
        for (int n = 0; n < 16384; ++n) {
            state += 0x9E3779B97F4A7C15L;
            long bits = (mix(state) & 0x000FFFFFFFFFFFFFL) >>> (n % 52);
            if (bits != 0) {
                h = hash(h, f.applyAsDouble(Double.longBitsToDouble(bits)));
            }
        }
        return h;
    }

    private static long branchThresholds(DoubleUnaryOperator f) {
        // fdlibm switches between x and x/2 at a significand of 0x6a09e (about sqrt(2)), and between two
        // formulas at 0x6147a and 0x6b851. Cover each threshold of the high word, with random low words, at
        // several exponents.
        long h = 0;
        long state = 4;
        for (int threshold : new int[] { 0x6a09e, 0x6a09f, 0x6147a, 0x6b851 }) {
            for (int exponent : new int[] { 1, 1022, 1023, 1024, 2046 }) {
                for (int d = -3; d <= 3; ++d) {
                    for (int n = 0; n < 64; ++n) {
                        state += 0x9E3779B97F4A7C15L;
                        long high = ((long) exponent << 20) | (threshold + d);
                        long bits = (high << 32) | (mix(state) & 0xFFFFFFFFL);
                        h = hash(h, f.applyAsDouble(Double.longBitsToDouble(bits)));
                    }
                }
            }
        }
        return h;
    }

    private static long randomValues(DoubleUnaryOperator f) {
        long h = 0;
        long state = 5;
        // Random bit patterns over all positive finite doubles.
        for (int n = 0; n < 1 << 17; ++n) {
            state += 0x9E3779B97F4A7C15L;
            long bits = mix(state) & 0x7FFFFFFFFFFFFFFFL;
            if (bits < 0x7FF0000000000000L) {
                h = hash(h, f.applyAsDouble(Double.longBitsToDouble(bits)));
            }
        }
        // Random values in (0, 1), as Random.nextGaussian passes.
        for (int n = 0; n < 1 << 16; ++n) {
            state += 0x9E3779B97F4A7C15L;
            h = hash(h, f.applyAsDouble(((mix(state) >>> 11) + 1) * 0x1.0p-53));
        }
        return h;
    }

    private static void assertLogBits(long input, long expected) {
        double x = Double.longBitsToDouble(input);
        assertEquals(Long.toHexString(expected), Long.toHexString(Double.doubleToRawLongBits(StrictMath.log(x))),
                "log(" + Long.toHexString(input) + ")");
    }

    private static void assertChecksum(long expected, long actual) {
        assertEquals(Long.toHexString(expected), Long.toHexString(actual));
    }

    // The finalizer of SplitMix64.
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long hash(long h, double value) {
        return mix(h ^ Double.doubleToRawLongBits(value)) + 1;
    }

    @JSBody(script = "globalThis.teavmTestSavedLog = Math.log; Math.log = function() { return 0; };")
    private static native void breakHostLog();

    @JSBody(script = "Math.log = globalThis.teavmTestSavedLog;")
    private static native void restoreHostLog();
}
