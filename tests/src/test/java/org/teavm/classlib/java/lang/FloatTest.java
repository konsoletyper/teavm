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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class FloatTest {
    private static final float OTHER_NAN = Float.intBitsToFloat(Float.floatToIntBits(Float.NaN) + 1);

    @Test
    public void parsed() {
        assertEquals(23, Float.parseFloat("23"), 1E-12F);
        assertEquals(23, Float.parseFloat("23.0"), 1E-12F);
        assertEquals(23, Float.parseFloat("23E0"), 1E-12F);
        assertEquals(23, Float.parseFloat("2.30000E1"), 1E-12F);
        assertEquals(23, Float.parseFloat("0.23E2"), 1E-12F);
        assertEquals(23, Float.parseFloat("0.000023E6"), 1E-12F);
        assertEquals(23, Float.parseFloat("00230000e-4"), 1E-12F);
        assertEquals(23, Float.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, Float.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, Float.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, Float.parseFloat("23."), 1E-12F);
        assertEquals(0.1F, Float.parseFloat("0.1"), 0.001F);
        assertEquals(0.1F, Float.parseFloat(".1"), 0.001F);
        assertEquals(0.1F, Float.parseFloat(" .1"), 0.001F);
        assertEquals(0.1F, Float.parseFloat(".1 "), 0.001F);
        assertEquals(-23, Float.parseFloat("-23"), 1E-12F);
        assertEquals(0, Float.parseFloat("0.0"), 1E-12F);
        assertEquals(0, Float.parseFloat("0"), 1E-12F);
        assertEquals(0, Float.parseFloat("00"), 1E-12F);
        assertEquals(0, Float.parseFloat(".0"), 1E-12F);
        assertEquals(0, Float.parseFloat("0."), 1E-12F);
        assertEquals(0, Float.parseFloat("23E-8000"), 1E-12F);
        assertEquals(0, Float.parseFloat("00000"), 1E-12F);
        assertEquals(0, Float.parseFloat("00000.0000"), 1E-12F);
        assertEquals(4499999285F, Float.parseFloat("4499999285"), 100F);
        assertEquals(0.4499999285F, Float.parseFloat("0.4499999285"), 1E-9F);
        assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat("1e50"), 1E-9F);
        assertEquals(23, Float.parseFloat("23f"), 0.1f);
        assertEquals(23, Float.parseFloat("23F"), 0.1f);
        assertEquals(23, Float.parseFloat("23d"), 0.1f);
        assertEquals(23, Float.parseFloat("23D"), 0.1f);

        assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat("Infinity"), 1E-12F);
        assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat("+Infinity"), 1E-12F);
        assertEquals(Float.NEGATIVE_INFINITY, Float.parseFloat("-Infinity"), 1E-12F);
        assertEquals(Float.NEGATIVE_INFINITY, Float.parseFloat(" -Infinity "), 1E-12F);
        assertEquals(Float.NaN, Float.parseFloat("NaN"), 1E-12F);
        assertEquals(Float.NaN, Float.parseFloat("+NaN"), 1E-12F);
        assertEquals(Float.NaN, Float.parseFloat("-NaN"), 1E-12F);
        assertEquals(Float.NaN, Float.parseFloat(" -NaN "), 1E-12F);
    }

    @Test
    public void testEquals() {
        assertNotEquals(Float.valueOf(-0.0f), Float.valueOf(0.0f));
        assertEquals(Float.valueOf(5.0f), Float.valueOf(5.0f));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Float.valueOf(Float.POSITIVE_INFINITY));
        assertNotEquals(Float.valueOf(Float.NEGATIVE_INFINITY), Float.valueOf(Float.POSITIVE_INFINITY));
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), Float.valueOf(Float.NEGATIVE_INFINITY));
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
            Float.parseFloat(string);
            fail("Exception expected parsing string: " + string);
        } catch (NumberFormatException e) {
            // It's expected
        }
    }

    @Test
    public void floatBitsExtracted() {
        assertEquals(0x4591A2B4, Float.floatToIntBits(0x1.234567p+12f));
        assertEquals(0x800000, Float.floatToIntBits((float) Math.pow(2, -126)));
        assertEquals(0x000092, Float.floatToIntBits(0x0.000123p-126f));
    }

    @Test
    public void floatBitsPacked() {
        assertEquals(0x1.234567p+12f, Float.intBitsToFloat(0x4591A2B4), 1e7);
        assertEquals(0x0.000123p-126f, Float.intBitsToFloat(0x000092), 0x000008p-126);
    }

    @Test
    public void hexStringBuilt() {
        assertEquals("0x1.23456p17", Float.toHexString(0x1.23456p17f));
        assertEquals("0x1.0p0", Float.toHexString(1));
        assertEquals("-0x1.0p0", Float.toHexString(-1));
        assertEquals("0x1.0p1", Float.toHexString(2));
        assertEquals("0x1.8p1", Float.toHexString(3));
        assertEquals("0x1.0p-1", Float.toHexString(0.5f));
        assertEquals("0x1.0p-2", Float.toHexString(0.25f));
        assertEquals("0x1.0p-126", Float.toHexString((float) Math.pow(2, -126)));
        assertEquals("0x0.001p-126", Float.toHexString(0x0.001p-126f));
        assertEquals("0x0.0p0", Float.toHexString(0.0F));
        assertEquals("-0x0.0p0", Float.toHexString(-0.0F));
    }

    @Test
    public void compares() {
        assertEquals(1, Float.compare(10, 5));
        assertEquals(-1, Float.compare(5, 10));
        assertEquals(0, Float.compare(5, 5));
        assertEquals(0, Float.compare(0.0f, 0.0f));
        assertEquals(0, Float.compare(-0.0f, -0.0f));
        assertEquals(1, Float.compare(Float.NaN, Float.POSITIVE_INFINITY));
        assertEquals(-1, Float.compare(Float.POSITIVE_INFINITY, Float.NaN));
        assertEquals(1, Float.compare(Float.NaN, 0.0f));
        assertEquals(-1, Float.compare(-0.0f, Float.NaN));
        assertEquals(1, Float.compare(0.0f, -0.0f));
        assertEquals(-1, Float.compare(-0.0f, 0.0f));
    }

    @Test
    public void testNaN() {
        assertTrue(Float.isNaN(OTHER_NAN));
        assertTrue(OTHER_NAN != OTHER_NAN);
        assertTrue(OTHER_NAN != Double.NaN);
        assertEquals(Float.valueOf(Float.NaN), Float.valueOf(Float.NaN));
        assertEquals(Float.valueOf(OTHER_NAN), Float.valueOf(Float.NaN));
        assertEquals(Float.valueOf(OTHER_NAN), Float.valueOf(OTHER_NAN));
        assertNotEquals(Float.floatToRawIntBits(OTHER_NAN), Float.floatToRawIntBits(Float.NaN));
        assertEquals(Float.floatToIntBits(OTHER_NAN), Float.floatToIntBits(Float.NaN));
    }

    @Test
    public void testFinity() {
        assertTrue(Float.isFinite(1f));
        assertFalse(Float.isInfinite(1f));
        assertFalse(Float.isFinite(Float.POSITIVE_INFINITY));
        assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));
        assertFalse(Float.isFinite(Float.NEGATIVE_INFINITY));
        assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
        assertFalse(Float.isFinite(Float.NaN));
        assertFalse(Float.isInfinite(Float.NaN));
    }

    // Float.toString: "When p >= 2, let T be the set of all decimals in R with length p. Otherwise, let T be the
    // set of all decimals in R with length 1 or 2." Float.valueOf(String): the exact decimal value "is then
    // rounded to type float by the usual round-to-nearest rule of IEEE 754 floating-point arithmetic".
    @Test
    public void subnormalToStringAndParse() {
        // The rounding interval of a subnormal is half of its own ulp, not of its normalized mantissa.
        assertEquals("1.4E-45", Float.toString(Float.MIN_VALUE));
        assertEquals("-1.4E-45", Float.toString(-Float.MIN_VALUE));
        assertEquals("2.8E-45", Float.toString(2 * Float.MIN_VALUE));
        assertEquals("4.2E-45", Float.toString(3 * Float.MIN_VALUE));
        assertEquals("1.0E-40", Float.toString(1E-40f));
        assertEquals("1.1754942E-38", Float.toString(Math.nextDown(Float.MIN_NORMAL)));
        assertEquals("1.1754944E-38", Float.toString(Float.MIN_NORMAL));

        // Parsing rounds a subnormal at its own last bit.
        assertEquals(Float.MIN_VALUE, Float.parseFloat("1.4E-45"), 0);
        assertEquals(Float.MIN_VALUE, Float.parseFloat("1E-45"), 0);
        assertEquals(0, Float.parseFloat("7E-46"), 0);
        assertEquals(0, Float.parseFloat("1E-50"), 0);
        assertEquals(Math.nextDown(Float.MIN_NORMAL), Float.parseFloat("1.1754942E-38"), 0);
    }

    // Float.toString: "Let R be the set of all decimals that round to m according to the usual round to nearest
    // rounding policy of IEEE 754 floating-point arithmetic." The selected decimal is in R, so it parses back.
    @Test
    public void smallSubnormalsRoundTrip() {
        for (int bits = 1; bits <= 65536; ++bits) {
            float f = Float.intBitsToFloat(bits);
            String s = Float.toString(f);
            if (Float.floatToRawIntBits(Float.parseFloat(s)) != bits) {
                fail("Float with bits " + bits + " is written as " + s + ", which does not parse back");
            }
        }
    }

    // Float.toString: "Let R be the set of all decimals that round to m according to the usual round to nearest
    // rounding policy of IEEE 754 floating-point arithmetic." Round to nearest breaks a tie to the even
    // significand, so a decimal on the edge of the interval is in R exactly when m has an even significand.
    @Test
    public void toStringOnRoundingIntervalEdge() {
        // A decimal exactly on the edge of the rounding interval is used only when the mantissa is even
        assertEquals("5.383987E7", Float.toString(53839872f));
    }

    // Float.toString: "Define d_m as the decimal in T that is closest to m. Or if there are two such decimals in
    // T, select the one with the even significand."
    @Test
    public void toStringHalfwayTakesEvenDigit() {
        // 2831949.25 is exactly halfway between 2831949.2 and 2831949.3
        assertEquals("2831949.2", Float.toString(2831949.25f));
    }

    // Float.toString: R is the set of decimals that round to m, and below a power of two the neighbor is closer
    @Test
    public void toStringBelowPowerOfTwo() {
        // Below a power of two the next float is closer, so the rounding interval is narrower on that side
        assertEquals("8.4703295E-22", Float.toString(Math.scalb(1f, -70)));
        assertEquals("7.1054274E-15", Float.toString(Math.scalb(1f, -47)));
    }

    // Float.valueOf(String): the exact value "is then conceptually converted to an 'infinitely precise' binary
    // value that is then rounded to type float by the usual round-to-nearest rule of IEEE 754 floating-point
    // arithmetic". That rule breaks a tie to the even significand.
    @Test
    public void parseExactMidpointTakesEvenFloat() {
        // Each decimal is exactly halfway between two floats, and the JDK takes the float with the even mantissa
        assertEquals(494621968f, Float.parseFloat("494621968"), 0);
        assertEquals(314576016f, Float.parseFloat("314576016"), 0);
        assertEquals(19379349f, Float.parseFloat("19379349"), 0);
        assertEquals(4.5666056E8f, Float.parseFloat("4.5666056E8"), 0);
    }

    // Known gap: FloatAnalyzer multiplies by truncated 32-bit powers of ten, so a decimal very close to an edge
    // of the rounding interval, or to the midpoint between two candidates, can go to the wrong side. Each value
    // below is written differently from the JDK, and most are written as a decimal that does not parse back.
    // Expected strings are the JDK's.
    @Test
    @Disabled("Known gap: precision of the power-of-ten table in FloatAnalyzer")
    public void toStringNearIntervalEdgeKnownGap() {
        var failures = new StringBuilder();
        checkToString(failures, 6.9255703E-20f, "6.9255703E-20");
        checkToString(failures, 6.1547434E29f, "6.1547434E29");
        checkToString(failures, 5.0565014E-16f, "5.0565014E-16");
        checkToString(failures, 2.3443956E15f, "2.3443956E15");
        checkToString(failures, 1.37940825E32f, "1.37940825E32");
        checkToString(failures, 2.1088571E-8f, "2.1088571E-8");
        checkToString(failures, 4.4242392E28f, "4.4242392E28");
        checkToString(failures, 5.48341E-40f, "5.48341E-40");
        checkToString(failures, 6.10071E-40f, "6.10071E-40");
        checkToString(failures, 7.55331E-40f, "7.55331E-40");
        if (failures.length() > 0) {
            fail(failures.toString());
        }
    }

    private static void checkToString(StringBuilder failures, float f, String expected) {
        var actual = Float.toString(f);
        if (!actual.equals(expected)) {
            failures.append("expected ").append(expected).append(" but was ").append(actual).append("; ");
        }
    }

    // Known gap: FloatSynthesizer has the same table-precision limit, so these decimals, which the JDK writes,
    // parse to a neighbor of the float the JDK gives. Float.valueOf(String): the exact value "is then
    // conceptually converted to an 'infinitely precise' binary value that is then rounded to type float by the
    // usual round-to-nearest rule of IEEE 754 floating-point arithmetic".
    @Test
    @Disabled("Known gap: precision of the power-of-ten table in FloatSynthesizer")
    public void parseNearMidpointKnownGap() {
        var failures = new StringBuilder();
        checkParse(failures, "1.1159609E-10", 1.1159609E-10f);
        checkParse(failures, "4.499237E-34", 4.499237E-34f);
        checkParse(failures, "6.10193E34", 6.10193E34f);
        checkParse(failures, "1.687857E-32", 1.687857E-32f);
        checkParse(failures, "1.3436E-40", 1.3436E-40f);
        checkParse(failures, "4.0308E-40", 4.0308E-40f);
        checkParse(failures, "6.718E-40", 6.718E-40f);
        checkParse(failures, "9.4052E-40", 9.4052E-40f);
        if (failures.length() > 0) {
            fail(failures.toString());
        }
    }

    private static void checkParse(StringBuilder failures, String s, float expected) {
        var actual = Float.parseFloat(s);
        if (actual != expected) {
            failures.append(s).append(" parsed to bits ").append(Integer.toHexString(Float.floatToIntBits(actual)))
                    .append(" instead of ").append(Integer.toHexString(Float.floatToIntBits(expected))).append("; ");
        }
    }
}
