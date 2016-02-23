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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class IntegerTest {
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
    public void parsesMinInteger() {
        assertEquals(-2147483648, Integer.parseInt("-2147483648", 10));
        assertEquals(-2147483648, Integer.parseInt("-80000000", 16));
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
    }

    @Test
    public void numberOfLeadingZerosComputed() {
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
}
