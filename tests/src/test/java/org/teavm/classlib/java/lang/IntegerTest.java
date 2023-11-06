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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class IntegerTest {
    @Test
    public void testRightUnsignedShift() {
        assertEquals(1 << 31, Integer.MIN_VALUE >>> Integer.parseInt("0"));
        assertEquals(-1, -1 >>> Integer.parseInt("0"));
    }

    @Test
    public void parsesInteger() {
        assertEquals(0, Integer.parseInt("0", 10));
        assertEquals(473, Integer.parseInt("473", 10));
        assertEquals(42, Integer.parseInt("+42", 10));
        assertEquals(0, Integer.parseInt("-0", 10));
        assertEquals(-255, Integer.parseInt("-FF", 16));
        assertEquals(102, Integer.parseInt("1100110", 2));
        assertEquals(2147483647, Integer.parseInt("2147483647", 10));
        assertEquals(411787, Integer.parseInt("Kona", 27));
    }

    @Test
    public void parsesIntegerInSubstring() {
        assertEquals(0, Integer.parseInt("[0]", 1, 2, 10));
        assertEquals(473, Integer.parseInt("[473]", 1, 4, 10));
        assertEquals(42, Integer.parseInt("[+42]", 1, 4, 10));
        assertEquals(-255, Integer.parseInt("[-FF]", 1, 4, 16));
        try {
            Integer.parseInt("[-FF]", 1, 5, 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("[-FF]", 1, 6, 16);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Integer.parseInt("[-FF]", 1, 2, 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("[-FF]", 5, 4, 16);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void parsesCornerCases() {
        assertEquals(-2147483648, Integer.parseInt("-2147483648", 10));
        assertEquals(-2147483648, Integer.parseInt("-80000000", 16));
        try {
            Integer.parseInt("FFFF", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("2147483648", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("-2147483649", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("80000000", 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("-80000001", 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.parseInt("99999999999", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsTooBigInteger() {
        Integer.parseInt("2147483648", 10);
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsIntegerWithDigitsOutOfRadix() {
        Integer.parseInt("99", 8);
    }

    @Test
    public void writesInteger() {
        assertEquals("473", Integer.toString(473, 10));
        assertEquals("-ff", Integer.toString(-255, 16));
        assertEquals("kona", Integer.toString(411787, 27));
    }

    @Test
    public void writesSingleDigitInteger() {
        assertEquals("a", Integer.toString(10, 16));
    }

    @Test
    public void decodes() {
        assertEquals(Integer.valueOf(123), Integer.decode("123"));
        assertEquals(Integer.valueOf(83), Integer.decode("0123"));
        assertEquals(Integer.valueOf(255), Integer.decode("0xFF"));
        assertEquals(Integer.valueOf(65535), Integer.decode("+0xFFFF"));
        assertEquals(Integer.valueOf(-255), Integer.decode("-0xFF"));
        assertEquals(Integer.valueOf(2748), Integer.decode("+#ABC"));
        try {
            Integer.decode(null); // undocumented NPE
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            Integer.decode("2147483648");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.decode("-2147483649");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.decode("0x80000000");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.decode("-0x80000001");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Integer.decode("99999999999");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
    }

    @Test
    public void numberOfLeadingZerosComputed() {
        assertEquals(0, Integer.numberOfLeadingZeros(-1));
        assertEquals(1, Integer.numberOfLeadingZeros(0x40000000));
        assertEquals(1, Integer.numberOfLeadingZeros(0x40000123));
        assertEquals(1, Integer.numberOfLeadingZeros(0x7FFFFFFF));
        assertEquals(31, Integer.numberOfLeadingZeros(1));
        assertEquals(30, Integer.numberOfLeadingZeros(2));
        assertEquals(30, Integer.numberOfLeadingZeros(3));
        assertEquals(0, Integer.numberOfLeadingZeros(0x80000000));
        assertEquals(0, Integer.numberOfLeadingZeros(0x80000123));
        assertEquals(0, Integer.numberOfLeadingZeros(0xFFFFFFFF));
        assertEquals(32, Integer.numberOfLeadingZeros(0));
    }

    @Test
    public void numberOfTrailingZerosComputed() {
        assertEquals(1,  Integer.numberOfTrailingZeros(0xFFFFFFFE));
        assertEquals(1,  Integer.numberOfTrailingZeros(0x40000002));
        assertEquals(1,  Integer.numberOfTrailingZeros(0x00000002));
        assertEquals(31, Integer.numberOfTrailingZeros(0x80000000));
        assertEquals(30, Integer.numberOfTrailingZeros(0x40000000));
        assertEquals(30, Integer.numberOfTrailingZeros(0xC0000000));
        assertEquals(0,  Integer.numberOfTrailingZeros(0x00000001));
        assertEquals(0,  Integer.numberOfTrailingZeros(0x12300003));
        assertEquals(0,  Integer.numberOfTrailingZeros(0xFFFFFFFF));
        assertEquals(32, Integer.numberOfTrailingZeros(0));
    }

    @Test
    public void highestOneBit() {
        assertEquals(1 << 31, Integer.highestOneBit(-1));
        assertEquals(1 << 31, Integer.highestOneBit(Integer.MIN_VALUE));
        assertEquals(0, Integer.highestOneBit(0));
        assertEquals(16, Integer.highestOneBit(31));
    }

    @Test
    public void lowestOneBit() {
        assertEquals(0, Integer.lowestOneBit(0));
        assertEquals(2, Integer.lowestOneBit(50));
        assertEquals(1, Integer.lowestOneBit(-1));
    }

    @Test
    public void bitsCounted() {
        assertEquals(0, Integer.bitCount(0));
        assertEquals(1, Integer.bitCount(1));
        assertEquals(1, Integer.bitCount(0x400));
        assertEquals(1, Integer.bitCount(0x80000000));
        assertEquals(8, Integer.bitCount(0x11111111));
        assertEquals(8, Integer.bitCount(0x30303030));
        assertEquals(8, Integer.bitCount(0xFF000000));
        assertEquals(8, Integer.bitCount(0x000000FF));
        assertEquals(32, Integer.bitCount(0xFFFFFFFF));
        assertEquals(13, Integer.bitCount(0x5DC6F));
    }

    @Test
    public void bitsReversed() {
        assertEquals(0, Integer.reverse(0));
        assertEquals(0x80000000, Integer.reverse(1));
        assertEquals(0x00200000, Integer.reverse(0x400));
        assertEquals(0x00000001, Integer.reverse(0x80000000));
        assertEquals(0x88888888, Integer.reverse(0x11111111));
        assertEquals(0x0C0C0C0C, Integer.reverse(0x30303030));
        assertEquals(0x000000FF, Integer.reverse(0xFF000000));
        assertEquals(0xFF000000, Integer.reverse(0x000000FF));
        assertEquals(0xFFFFFFFF, Integer.reverse(0xFFFFFFFF));
        assertEquals(0xF63BA000, Integer.reverse(0x5DC6F));
    }

    @Test
    public void compares() {
        assertTrue(Integer.compare(10, 5) > 0);
        assertTrue(Integer.compare(5, 10) < 0);
        assertTrue(Integer.compare(5, 5) == 0);
        assertTrue(Integer.compare(Integer.MAX_VALUE, Integer.MIN_VALUE) > 0);
        assertTrue(Integer.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) < 0);
    }

    @Test
    public void getFromSystemProperty() {
        System.setProperty("test.foo", "23");
        System.setProperty("test.bar", "q");

        assertEquals((Object) 23, Integer.getInteger("test.foo"));
        assertNull(Integer.getInteger("test.bar"));
        assertNull(Integer.getInteger("test.baz"));
        assertNull(Integer.getInteger(null));
    }

    @Test
    public void toHex() {
        assertEquals("0", Integer.toHexString(0));
        assertEquals("1", Integer.toHexString(1));
        assertEquals("a", Integer.toHexString(10));
        assertEquals("11", Integer.toHexString(17));
        assertEquals("ff", Integer.toHexString(255));
        assertEquals("ffffffff", Integer.toHexString(-1));
    }

    @Test
    public void toStringRadix16() {
        assertEquals("17", Integer.toString(23, 16));
        assertEquals("1e240", Integer.toString(123456, 16));
        assertEquals("-17", Integer.toString(-23, 16));
        assertEquals("7fffffff", Integer.toString(Integer.MAX_VALUE, 16));
        assertEquals("-80000000", Integer.toString(Integer.MIN_VALUE, 16));
    }

    @Test
    public void toStringRadix2() {
        assertEquals("10111", Integer.toString(23, 2));
        assertEquals("11110001001000000", Integer.toString(123456, 2));
        assertEquals("-10111", Integer.toString(-23, 2));
        assertEquals("1111111111111111111111111111111", Integer.toString(Integer.MAX_VALUE, 2));
        assertEquals("-10000000000000000000000000000000", Integer.toString(Integer.MIN_VALUE, 2));
    }

    @Test
    public void unsignedRightShift() {
        assertEquals(Integer.MIN_VALUE, Integer.MIN_VALUE >>> Integer.parseInt("0"));
    }
}
