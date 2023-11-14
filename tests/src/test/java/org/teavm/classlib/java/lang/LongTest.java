/*
 *  Copyright 2017 konsoletyper.
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
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class LongTest {
    @Test
    public void parsesLongInSubstring() {
        assertEquals(0, Long.parseLong("[0]", 1, 2, 10));
        assertEquals(473, Long.parseLong("[473]", 1, 4, 10));
        assertEquals(42, Long.parseLong("[+42]", 1, 4, 10));
        assertEquals(-255, Long.parseLong("[-FF]", 1, 4, 16));
        try {
            Long.parseLong("[-FF]", 1, 5, 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("[-FF]", 1, 6, 16);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Long.parseLong("[-FF]", 1, 2, 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("[-FF]", 5, 4, 16);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void compares() {
        assertTrue(Long.compare(10, 5) > 0);
        assertTrue(Long.compare(5, 10) < 0);
        assertTrue(Long.compare(5, 5) == 0);
        assertTrue(Long.compare(Long.MAX_VALUE, Long.MIN_VALUE) > 0);
        assertTrue(Long.compare(Long.MIN_VALUE, Long.MAX_VALUE) < 0);
    }

    @Test
    @SkipJVM
    public void calculatesHashCode() {
        assertEquals(23 ^ 42, Long.hashCode((23L << 32) | 42));
    }

    @Test
    public void bitsReversed() {
        assertEquals(0, Long.reverse(0));
        assertEquals(0x8000000000000000L, Long.reverse(1));
        assertEquals(0x0020000000000000L, Long.reverse(0x400));
        assertEquals(0x0000000000000001L, Long.reverse(0x8000000000000000L));
        assertEquals(0x8888888888888888L, Long.reverse(0x1111111111111111L));
        assertEquals(0x0C0C0C0C0C0C0C0CL, Long.reverse(0x3030303030303030L));
        assertEquals(0x00000000000000FFL, Long.reverse(0xFF00000000000000L));
        assertEquals(0xFF00000000000000L, Long.reverse(0x00000000000000FFL));
        assertEquals(0xFFFFFFFFFFFFFFFFL, Long.reverse(0xFFFFFFFFFFFFFFFFL));
        assertEquals(0xF63BA00000000000L, Long.reverse(0x5DC6F));
    }

    @Test
    public void highestOneBit() {
        assertEquals(1L << 63, Long.highestOneBit(-1L));
        assertEquals(1L << 63, Long.highestOneBit(Long.MIN_VALUE));
        assertEquals(0, Long.highestOneBit(0L));
        assertEquals(16L, Long.highestOneBit(31L));
    }

    @Test
    public void lowestOneBit() {
        assertEquals(0L, Long.lowestOneBit(0L));
        assertEquals(2L, Long.lowestOneBit(50L));
        assertEquals(1L, Long.lowestOneBit(-1L));
    }

    @Test
    public void bitsCounted() {
        assertEquals(39, Long.bitCount(2587208649207147453L));
        assertEquals(0, Long.bitCount(0));
        assertEquals(64, Long.bitCount(-1));
        assertEquals(6, Long.bitCount(12345));
        assertEquals(59, Long.bitCount(-12345));

        for (int i = 0; i < 64; ++i) {
            assertEquals(1, Long.bitCount(1L << i));
        }
    }

    @Test
    public void toStringRadix16() {
        assertEquals("17", Long.toString(23, 16));
        assertEquals("1e240", Long.toString(123456, 16));
        assertEquals("-17", Long.toString(-23, 16));
        assertEquals("7fffffffffffffff", Long.toString(Long.MAX_VALUE, 16));
        assertEquals("-8000000000000000", Long.toString(Long.MIN_VALUE, 16));
    }

    @Test
    public void toStringRadix2() {
        assertEquals("10111", Long.toString(23, 2));
        assertEquals("11110001001000000", Long.toString(123456, 2));
        assertEquals("-10111", Long.toString(-23, 2));
        assertEquals("111111111111111111111111111111111111111111111111111111111111111",
                Long.toString(Long.MAX_VALUE, 2));
        assertEquals("-1000000000000000000000000000000000000000000000000000000000000000",
                Long.toString(Long.MIN_VALUE, 2));
    }

    @Test
    public void reverseBytes() {
        assertEquals(0xAABBCCDD00112233L, Long.reverseBytes(0x33221100DDCCBBAAL));
        assertEquals(0x1122334455667788L, Long.reverseBytes(0x8877665544332211L));
        assertEquals(0x0011223344556677L, Long.reverseBytes(0x7766554433221100L));
        assertEquals(0x2000000000000002L, Long.reverseBytes(0x0200000000000020L));
    }

    @Test
    public void decode() {
        assertEquals("Returned incorrect value for hex string", 255L,
                Long.decode("0xFF").longValue());
        assertEquals("Returned incorrect value for dec string", -89000L,
                Long.decode("-89000").longValue());
        assertEquals("Returned incorrect value for 0 decimal", 0,
                Long.decode("0").longValue());
        assertEquals("Returned incorrect value for 0 hex", 0,
                Long.decode("0x0").longValue());
        assertEquals("Returned incorrect value for most negative value decimal", 0x8000000000000000L,
                Long.decode("-9223372036854775808").longValue());
        assertEquals("Returned incorrect value for most negative value hex", 0x8000000000000000L,
                Long.decode("-0x8000000000000000").longValue());
        assertEquals("Returned incorrect value for most positive value decimal", 0x7fffffffffffffffL,
                Long.decode("9223372036854775807").longValue());
        assertEquals("Returned incorrect value for most positive value hex", 0x7fffffffffffffffL,
                Long.decode("0x7fffffffffffffff").longValue());
        assertEquals("Failed for 07654321765432", 07654321765432L,
                Long.decode("07654321765432").longValue());
        try {
            Long.decode(null); // undocumented NPE
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            Long.decode("999999999999999999999999999999999999999999999999999999");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.decode("9223372036854775808");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.decode("-9223372036854775809");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.decode("0x8000000000000000");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.decode("-0x8000000000000001");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.decode("42325917317067571199");
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
    }

    @Test
    public void test_parseLong() {
        assertEquals("Returned incorrect value",
                100000000L, Long.parseLong("100000000", 10));
        assertEquals("Returned incorrect value from hex string", 68719476735L,
                Long.parseLong("FFFFFFFFF", 16));
        assertEquals("Returned incorrect value from octal string: " + Long.parseLong("77777777777"),
                8589934591L, Long.parseLong("77777777777", 8));
        assertEquals("Returned incorrect value for 0 hex", 0, Long.parseLong("0", 16));
        assertEquals("Returned incorrect value for most negative value hex", 0x8000000000000000L,
                Long.parseLong("-8000000000000000", 16));
        assertEquals("Returned incorrect value for most positive value hex", 0x7fffffffffffffffL,
                Long.parseLong("7fffffffffffffff", 16));
        assertEquals("Returned incorrect value for 0 decimal", 0,
                Long.parseLong("0", 10));
        assertEquals("Returned incorrect value for most negative value decimal", 0x8000000000000000L,
                Long.parseLong("-9223372036854775808", 10));
        assertEquals("Returned incorrect value for most positive value decimal", 0x7fffffffffffffffL,
                Long.parseLong("9223372036854775807", 10));
        try {
            Long.parseLong("999999999999", 8);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("9223372036854775808", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("-9223372036854775809", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("8000000000000000", 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("-8000000000000001", 16);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
        try {
            Long.parseLong("42325917317067571199", 10);
            fail();
        } catch (NumberFormatException e) {
            // ok
        }
    }
}
