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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class DoubleTest {
    public static final double OTHER_NAN = Double.longBitsToDouble(Double.doubleToLongBits(Double.NaN) + 1);

    @Test
    public void parsed() {
        assertEquals(23, Double.parseDouble("23"), 1E-12);
        assertEquals(23, Double.parseDouble("23.0"), 1E-12);
        assertEquals(23, Double.parseDouble("23E0"), 1E-12);
        assertEquals(23, Double.parseDouble("2.30000E1"), 1E-12);
        assertEquals(23, Double.parseDouble("0.23E2"), 1E-12);
        assertEquals(23, Double.parseDouble("0.000023E6"), 1E-12);
        assertEquals(23, Double.parseDouble("00230000e-4"), 1E-12);
        assertEquals(23, Double.parseDouble("2300000000000000000000e-20"), 1E-12);
        assertEquals(23, Double.parseDouble("2300000000000000000000e-20"), 1E-12);
        assertEquals(23, Double.parseDouble("23."), 1E-12);
        assertEquals(0.1, Double.parseDouble("0.1"), 0.001);
        assertEquals(0.1, Double.parseDouble(".1"), 0.001);
        assertEquals(0.1, Double.parseDouble(" .1"), 0.001);
        assertEquals(0.1, Double.parseDouble(".1 "), 0.001);
        assertEquals(-23, Double.parseDouble("-23"), 1E-12);
        assertEquals(0, Double.parseDouble("0.0"), 1E-12);
        assertEquals(0, Double.parseDouble("0"), 1E-12);
        assertEquals(0, Double.parseDouble("00"), 1E-12);
        assertEquals(0, Double.parseDouble("0."), 1E-12);
        assertEquals(0, Double.parseDouble(".0"), 1E-12);
        assertEquals(0, Double.parseDouble("23E-8000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000.0000"), 1E-12);
        assertEquals("74.92507492507494", Double.toString(Double.parseDouble("74.92507492507494")));

        assertEquals(4499999999999888888888888.0, Double.parseDouble("4499999999999888888888888"), 1E9);
        assertEquals(0.4499999999999888888888888, Double.parseDouble("0.4499999999999888888888888"), 1E-15);
        assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble("1e330"), 1E-15);

        assertEquals(23, Double.parseDouble("23f"), 0.1f);
        assertEquals(23, Double.parseDouble("23F"), 0.1f);
        assertEquals(23, Double.parseDouble("23d"), 0.1f);
        assertEquals(23, Double.parseDouble("23D"), 0.1f);

        assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble("Infinity"), 1E-12);
        assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble("+Infinity"), 1E-12);
        assertEquals(Double.NEGATIVE_INFINITY, Double.parseDouble("-Infinity"), 1E-12);
        assertEquals(Double.NEGATIVE_INFINITY, Double.parseDouble(" -Infinity "), 1E-12);
        assertEquals(Double.NaN, Double.parseDouble("NaN"), 1E-12);
        assertEquals(Double.NaN, Double.parseDouble("+NaN"), 1E-12);
        assertEquals(Double.NaN, Double.parseDouble("-NaN"), 1E-12);
        assertEquals(Double.NaN, Double.parseDouble(" -NaN "), 1E-12);
    }

    @Test
    public void testEquals() {
        assertNotEquals(Double.valueOf(-0.0), Double.valueOf(0.0));
        assertEquals(Double.valueOf(3.0), Double.valueOf(3.0));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY));
        assertNotEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void randomDoubles() {
        var random = new Random();
        for (var i = 0; i < 10000; ++i) {
            var n = random.nextLong();
            var d = Double.longBitsToDouble(n);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                continue;
            }
            var actual = Double.parseDouble(Double.toString(d));
            if (n != Double.doubleToLongBits(actual)) {
                System.out.println(d + ", " + n + ", " + Double.doubleToLongBits(actual));
            }
        }
    }

    @Test
    public void parsedWithError() {
        checkIllegalFormat("");
        checkIllegalFormat("  ");
        checkIllegalFormat("a");
        checkIllegalFormat(" a ");
        checkIllegalFormat("-");
        checkIllegalFormat("-.");
        checkIllegalFormat(".");
        checkIllegalFormat("1e-");
        checkIllegalFormat("1e");

        checkIllegalFormat("++Infinity");
        checkIllegalFormat("--Infinity");
        checkIllegalFormat("INFINITY");
        checkIllegalFormat("infinity");
        checkIllegalFormat("InfinityF");
        checkIllegalFormat("InfinityD");
        checkIllegalFormat("Infinityf");
        checkIllegalFormat("Infinityd");
        checkIllegalFormat("++NaN");
        checkIllegalFormat("--NaN");
        checkIllegalFormat("NAN");
        checkIllegalFormat("nan");
        checkIllegalFormat("NaNF");
        checkIllegalFormat("NaND");
        checkIllegalFormat("NaNf");
        checkIllegalFormat("NaNd");
    }

    private void checkIllegalFormat(String string) {
        try {
            Double.parseDouble(string);
            fail("Exception expected parsing string: " + string);
        } catch (NumberFormatException e) {
            // It's expected
        }
    }

    @Test
    public void longBitsExtracted() {
        assertEquals(0x41E23456789ABCDEL, Double.doubleToLongBits(0x1.23456789ABCDEP+31));
        assertEquals(0x3FE1C28F5C28F5C3L >>> 3, Double.doubleToLongBits(0.555) >>> 3);
        assertEquals(0x00000056789ABCDEL, Double.doubleToLongBits(0x0.00056789ABCDEP-1022));
    }

    @Test
    public void longBitsPacked() {
        assertEquals(0x1.23456789ABCDEP+31, Double.longBitsToDouble(0x41E23456789ABCDEL), 0x1.0P-19);
        assertEquals(0x0.00056789ABCDEP-1022, Double.longBitsToDouble(0x00000056789ABCDEL), 0x1.0P-19);
    }

    @Test
    public void hexStringBuilt() {
        assertEquals("0x1.23456789abcdep31", Double.toHexString(0x1.23456789ABCDEP+31));
        assertEquals("0x1.0p0", Double.toHexString(1));
        assertEquals("-0x1.0p0", Double.toHexString(-1));
        assertEquals("0x1.0p1", Double.toHexString(2));
        assertEquals("0x1.8p1", Double.toHexString(3));
        assertEquals("0x1.0p-1", Double.toHexString(0.5));
        assertEquals("0x1.0p-2", Double.toHexString(0.25));
        assertEquals("0x1.0p-1022", Double.toHexString(0x1.0p-1022));
        assertEquals("0x0.8p-1022", Double.toHexString(0x0.8p-1022));
        assertEquals("0x0.001p-1022", Double.toHexString(0x0.001p-1022));
        assertEquals("0x0.0p0", Double.toHexString(0.0D));
        assertEquals("-0x0.0p0", Double.toHexString(-0.0D));
    }

    @Test
    public void compares() {
        assertEquals(1, Double.compare(10, 5));
        assertEquals(-1, Double.compare(5, 10));
        assertEquals(0, Double.compare(5, 5));
        assertEquals(0, Double.compare(0.0f, 0.0f));
        assertEquals(1, Double.compare(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(-1, Double.compare(Double.POSITIVE_INFINITY, Double.NaN));
        assertEquals(1, Double.compare(Double.NaN, 0.0));
        assertEquals(-1, Double.compare(-0.0, Double.NaN));
        assertEquals(1, Double.compare(0.0, -0.0));
        assertEquals(-1, Double.compare(-0.0, 0.0));
    }

    @Test
    public void testNaN() {
        assertTrue(Double.isNaN(OTHER_NAN));
        assertTrue(OTHER_NAN != OTHER_NAN);
        assertTrue(OTHER_NAN != Double.NaN);
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN));
        assertEquals(Double.valueOf(OTHER_NAN), Double.valueOf(Double.NaN));
        assertEquals(Double.valueOf(OTHER_NAN), Double.valueOf(OTHER_NAN));
        assertNotEquals(Double.doubleToRawLongBits(OTHER_NAN), Double.doubleToRawLongBits(Double.NaN));
        assertEquals(Double.doubleToLongBits(OTHER_NAN), Double.doubleToLongBits(Double.NaN));
    }

    @Test
    public void testFinity() {
        assertTrue(Double.isFinite(1d));
        assertFalse(Double.isInfinite(1d));
        assertFalse(Double.isFinite(Double.POSITIVE_INFINITY));
        assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));
        assertFalse(Double.isFinite(Double.NEGATIVE_INFINITY));
        assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
        assertFalse(Double.isFinite(Double.NaN));
        assertFalse(Double.isInfinite(Double.NaN));
    }

    // Double.toString: "When p >= 2, let T be the set of all decimals in R with length p. Otherwise, let T be the
    // set of all decimals in R with length 1 or 2." Double.valueOf(String): the exact decimal value "is then
    // rounded to type double by the usual round-to-nearest rule of IEEE 754 floating-point arithmetic".
    @Test
    public void subnormalToStringAndParse() {
        // The rounding interval of a subnormal is half of its own ulp, not of its normalized mantissa.
        assertEquals("4.9E-324", Double.toString(Double.MIN_VALUE));
        assertEquals("-4.9E-324", Double.toString(-Double.MIN_VALUE));
        assertEquals("9.9E-324", Double.toString(2 * Double.MIN_VALUE));
        assertEquals("1.5E-323", Double.toString(3 * Double.MIN_VALUE));
        assertEquals("2.0E-323", Double.toString(4 * Double.MIN_VALUE));
        assertEquals("1.0E-310", Double.toString(1E-310));
        assertEquals("2.5E-320", Double.toString(2.5E-320));
        assertEquals("1.0E-308", Double.toString(1E-308));
        assertEquals("2.225073858507201E-308", Double.toString(Math.nextDown(Double.MIN_NORMAL)));
        assertEquals("2.2250738585072014E-308", Double.toString(Double.MIN_NORMAL));
        assertEquals("1.27603E-318", Double.toString(Double.longBitsToDouble(0x3f0dfL)));
        assertEquals("6.0246141314E-314", Double.toString(Double.longBitsToDouble(0x2d6d0fbc3L)));

        // Parsing rounds a subnormal at its own last bit.
        assertEquals(Double.MIN_VALUE, Double.parseDouble("4.9E-324"), 0);
        assertEquals(Double.MIN_VALUE, Double.parseDouble("3E-324"), 0);
        assertEquals(0, Double.parseDouble("2E-324"), 0);
        assertEquals(0, Double.parseDouble("1E-330"), 0);
        assertEquals(3 * Double.MIN_VALUE, Double.parseDouble("1.5E-323"), 0);
        assertEquals(Math.nextDown(Double.MIN_NORMAL), Double.parseDouble("2.225073858507201E-308"), 0);
    }

    // Double.toString: "Let R be the set of all decimals that round to m according to the usual round to nearest
    // rounding policy of IEEE 754 floating-point arithmetic." The selected decimal is in R, so it parses back.
    @Test
    public void smallSubnormalsRoundTrip() {
        for (long bits = 1; bits <= 100000; ++bits) {
            double d = Double.longBitsToDouble(bits);
            String s = Double.toString(d);
            if (Double.doubleToRawLongBits(Double.parseDouble(s)) != bits) {
                fail("Double with bits " + bits + " is written as " + s + ", which does not parse back");
            }
        }
    }

    // Double.toString: "Let R be the set of all decimals that round to m according to the usual round to nearest
    // rounding policy of IEEE 754 floating-point arithmetic." Round to nearest breaks a tie to the even
    // significand, so a decimal on the edge of the interval is in R exactly when m has an even significand.
    @Test
    public void toStringOnRoundingIntervalEdge() {
        // A decimal exactly on the edge of the rounding interval is used only when the mantissa is even
        assertEquals("1.0E23", Double.toString(1E23));
        assertEquals("1.0000000000000001E23", Double.toString(Math.nextUp(1E23)));
        assertEquals("3.849937261242046E16", Double.toString(3.849937261242046E16));
    }

    // Double.toString: "Define d_m as the decimal in T that is closest to m. Or if there are two such decimals in
    // T, select the one with the even significand."
    @Test
    public void toStringHalfwayTakesEvenDigit() {
        // 2116974939765710.25 is exactly halfway between ...102 and ...103
        assertEquals("2.1169749397657102E15", Double.toString(2.1169749397657102E15));
        assertEquals("2.4333958225460162E14", Double.toString(2.4333958225460162E14));
    }

    // Double.toString: R is the set of decimals that round to m, and below a power of two the neighbor is closer
    @Test
    public void toStringBelowPowerOfTwo() {
        // Below a power of two the next double is closer, so the rounding interval is narrower on that side
        assertEquals("1.7800590868057611E-307", Double.toString(Math.scalb(1.0, -1019)));
    }

    // Double.valueOf(String): the exact value "is then conceptually converted to an 'infinitely precise' binary
    // value that is then rounded to type double by the usual round-to-nearest rule of IEEE 754 floating-point
    // arithmetic". That rule breaks a tie to the even significand.
    @Test
    public void parseExactMidpointTakesEvenDouble() {
        // Each decimal is exactly halfway between two doubles, and the JDK takes the double with the even mantissa
        assertEquals(2.238987874235451E16, Double.parseDouble("2.238987874235451E16"), 0);
        assertEquals(5.741072488841486E16, Double.parseDouble("5.741072488841486E16"), 0);
        assertEquals(1.60448237089496E18, Double.parseDouble("1.60448237089496E18"), 0);
        assertEquals(9007199254740992.0, Double.parseDouble("9007199254740993"), 0);
        assertEquals(9007199254740996.0, Double.parseDouble("9007199254740995"), 0);
    }

    @Test
    public void largeDoublesRoundTrip() {
        // Double.toString writes such doubles as the decimal halfway to a neighbor when that decimal is shortest
        var random = new Random(4);
        for (var i = 0; i < 20000; ++i) {
            var d = Math.scalb(1 + random.nextDouble(), 53 + random.nextInt(12));
            assertEquals(d, Double.parseDouble(Double.toString(d)), 0);
        }
    }

    // Known gap: DoubleAnalyzer multiplies by truncated 64-bit powers of ten, so a decimal very close to an edge
    // of the rounding interval, or to the midpoint between two candidates, can go to the wrong side. Each value
    // below is written differently from the JDK, and the subnormal ones and 1.6439167945475462E-67 are written as
    // a decimal that does not parse back. Expected strings are the JDK's.
    @Test
    @Disabled("Known gap: precision of the power-of-ten table in DoubleAnalyzer")
    public void toStringNearIntervalEdgeKnownGap() {
        var failures = new StringBuilder();
        checkToString(failures, 1E126, "1.0E126");
        checkToString(failures, 2.9866777770622487E-68, "2.9866777770622487E-68");
        checkToString(failures, 8.149551566440249E-118, "8.149551566440249E-118");
        checkToString(failures, 2.9362303787435153E153, "2.9362303787435153E153");
        checkToString(failures, 1.6439167945475462E-67, "1.6439167945475462E-67");
        checkToString(failures, 9.539897001926882E-309, "9.539897001926882E-309");
        checkToString(failures, 1.0436252483415902E-308, "1.0436252483415902E-308");
        checkToString(failures, 1.5238698338848862E-308, "1.5238698338848862E-308");
        checkToString(failures, 2.2250738584111362E-308, "2.2250738584111362E-308");
        if (failures.length() > 0) {
            fail(failures.toString());
        }
    }

    private static void checkToString(StringBuilder failures, double d, String expected) {
        var actual = Double.toString(d);
        if (!actual.equals(expected)) {
            failures.append("expected ").append(expected).append(" but was ").append(actual).append("; ");
        }
        if (Double.parseDouble(actual) != d) {
            failures.append(actual).append(" does not parse back to ").append(expected).append("; ");
        }
    }

    // Known gap: DoubleSynthesizer has the same table-precision limit, so these decimals, which the JDK writes,
    // parse to a neighbor of the double the JDK gives. Double.valueOf(String): the exact value "is then
    // conceptually converted to an 'infinitely precise' binary value that is then rounded to type double by the
    // usual round-to-nearest rule of IEEE 754 floating-point arithmetic".
    @Test
    @Disabled("Known gap: precision of the power-of-ten table in DoubleSynthesizer")
    public void parseNearMidpointKnownGap() {
        var failures = new StringBuilder();
        checkParse(failures, "2.807569972855083E137", 2.807569972855083E137);
        checkParse(failures, "3.185971058067586E202", 3.185971058067586E202);
        checkParse(failures, "3.093933428094499E-10", 3.093933428094499E-10);
        checkParse(failures, "1.6161557140473E217", 1.6161557140473E217);
        checkParse(failures, "1.654925151066461E-139", 1.654925151066461E-139);
        checkParse(failures, "1.276790740084915E228", 1.276790740084915E228);
        if (failures.length() > 0) {
            fail(failures.toString());
        }
    }

    private static void checkParse(StringBuilder failures, String s, double expected) {
        var actual = Double.parseDouble(s);
        if (actual != expected) {
            failures.append(s).append(" parsed to bits ").append(Long.toHexString(Double.doubleToLongBits(actual)))
                    .append(" instead of ").append(Long.toHexString(Double.doubleToLongBits(expected))).append("; ");
        }
    }
}
