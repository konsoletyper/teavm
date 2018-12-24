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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class DoubleTest {
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
    }

    @Test
    public void negativeParsed() {
        assertEquals(-23, Double.parseDouble("-23"), 1E-12);
    }

    @Test
    public void zeroParsed() {
        assertEquals(0, Double.parseDouble("0.0"), 1E-12);
        assertEquals(0, Double.parseDouble("23E-8000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000.0000"), 1E-12);
    }

    @Test
    public void longBitsExtracted() {
        assertEquals(0x41E23456789ABCDEL, Double.doubleToLongBits(0x1.23456789ABCDEP+31));
    }

    @Test
    public void longBitsExtracted2() {
        assertEquals(0x3FE1C28F5C28F5C3L >>> 3, Double.doubleToLongBits(0.555) >>> 3);
    }

    @Test
    public void subNormalLongBitsExtracted() {
        assertEquals(0x00000056789ABCDEL, Double.doubleToLongBits(0x0.00056789ABCDEP-1022));
    }

    @Test
    public void longBitsPacked() {
        assertEquals(0x1.23456789ABCDEP+31, Double.longBitsToDouble(0x41E23456789ABCDEL), 0x1.0P-19);
    }

    @Test
    public void subNormalLongBitsPacked() {
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
    }

    @Test
    public void compares() {
        assertTrue(Double.compare(10, 5) > 0);
        assertTrue(Double.compare(5, 10) < 0);
        assertTrue(Double.compare(5, 5) == 0);
    }
}
