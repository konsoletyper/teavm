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
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
}
