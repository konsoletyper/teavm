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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
}
