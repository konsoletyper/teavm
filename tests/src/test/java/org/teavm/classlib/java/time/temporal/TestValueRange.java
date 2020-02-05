/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time.temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.teavm.classlib.java.time.AbstractTest;

public class TestValueRange extends AbstractTest {

    @Test
    public void test_immutable() {

        assertImmutable(TValueRange.class);
    }

    @Test
    public void test_of_longlong() {

        TValueRange test = TValueRange.of(1, 12);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 12);
        assertEquals(test.getMaximum(), 12);
        assertEquals(test.isFixed(), true);
        assertEquals(test.isIntValue(), true);
    }

    @Test
    public void test_of_longlong_big() {

        TValueRange test = TValueRange.of(1, 123456789012345L);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 123456789012345L);
        assertEquals(test.getMaximum(), 123456789012345L);
        assertEquals(test.isFixed(), true);
        assertEquals(test.isIntValue(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_of_longlong_minGtMax() {

        TValueRange.of(12, 1);
    }

    @Test
    public void test_of_longlonglong() {

        TValueRange test = TValueRange.of(1, 28, 31);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 28);
        assertEquals(test.getMaximum(), 31);
        assertEquals(test.isFixed(), false);
        assertEquals(test.isIntValue(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_of_longlonglong_minGtMax() {

        TValueRange.of(12, 1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_of_longlonglong_smallestmaxminGtMax() {

        TValueRange.of(1, 31, 28);
    }

    Object[][] data_valid() {

        return new Object[][] { { 1, 1, 1, 1 }, { 1, 1, 1, 2 }, { 1, 1, 2, 2 }, { 1, 2, 3, 4 }, { 1, 1, 28, 31 },
        { 1, 3, 31, 31 }, { -5, -4, -3, -2 }, { -5, -4, 3, 4 }, { 1, 20, 10, 31 }, };
    }

    @Test
    public void test_of_longlonglonglong() {

        for (Object[] data : data_valid()) {
            long sMin = ((Number) data[0]).longValue();
            long lMin = ((Number) data[1]).longValue();
            long sMax = ((Number) data[2]).longValue();
            long lMax = ((Number) data[3]).longValue();

            TValueRange test = TValueRange.of(sMin, lMin, sMax, lMax);
            assertEquals(test.getMinimum(), sMin);
            assertEquals(test.getLargestMinimum(), lMin);
            assertEquals(test.getSmallestMaximum(), sMax);
            assertEquals(test.getMaximum(), lMax);
            assertEquals(test.isFixed(), sMin == lMin && sMax == lMax);
            assertEquals(test.isIntValue(), true);
        }
    }

    Object[][] data_invalid() {

        return new Object[][] { { 1, 2, 31, 28 }, { 1, 31, 2, 28 }, { 31, 2, 1, 28 }, { 31, 2, 3, 28 },

        { 2, 1, 28, 31 }, { 2, 1, 31, 28 }, { 12, 13, 1, 2 }, };
    }

    @Test
    public void test_of_longlonglonglong_invalid() {

        for (Object[] data : data_invalid()) {
            long sMin = ((Number) data[0]).longValue();
            long lMin = ((Number) data[1]).longValue();
            long sMax = ((Number) data[2]).longValue();
            long lMax = ((Number) data[3]).longValue();
            try {
                TValueRange.of(sMin, lMin, sMax, lMax);
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    @Test
    public void test_isValidValue_long() {

        TValueRange test = TValueRange.of(1, 28, 31);
        assertEquals(test.isValidValue(0), false);
        assertEquals(test.isValidValue(1), true);
        assertEquals(test.isValidValue(2), true);
        assertEquals(test.isValidValue(30), true);
        assertEquals(test.isValidValue(31), true);
        assertEquals(test.isValidValue(32), false);
    }

    @Test
    public void test_isValidValue_long_int() {

        TValueRange test = TValueRange.of(1, 28, 31);
        assertEquals(test.isValidValue(0), false);
        assertEquals(test.isValidValue(1), true);
        assertEquals(test.isValidValue(31), true);
        assertEquals(test.isValidValue(32), false);
    }

    @Test
    public void test_isValidValue_long_long() {

        TValueRange test = TValueRange.of(1, 28, Integer.MAX_VALUE + 1L);
        assertEquals(test.isValidIntValue(0), false);
        assertEquals(test.isValidIntValue(1), false);
        assertEquals(test.isValidIntValue(31), false);
        assertEquals(test.isValidIntValue(32), false);
    }

    @Test
    public void test_equals1() {

        TValueRange a = TValueRange.of(1, 2, 3, 4);
        TValueRange b = TValueRange.of(1, 2, 3, 4);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }

    @Test
    public void test_equals2() {

        TValueRange a = TValueRange.of(1, 2, 3, 4);
        assertEquals(a.equals(TValueRange.of(0, 2, 3, 4)), false);
        assertEquals(a.equals(TValueRange.of(1, 3, 3, 4)), false);
        assertEquals(a.equals(TValueRange.of(1, 2, 4, 4)), false);
        assertEquals(a.equals(TValueRange.of(1, 2, 3, 5)), false);
    }

    @Test
    public void test_equals_otherType() {

        TValueRange a = TValueRange.of(1, 12);
        assertEquals(a.equals("Rubbish"), false);
    }

    @Test
    public void test_equals_null() {

        TValueRange a = TValueRange.of(1, 12);
        assertEquals(a.equals(null), false);
    }

    @Test
    public void test_toString() {

        assertEquals(TValueRange.of(1, 1, 4, 4).toString(), "1 - 4");
        assertEquals(TValueRange.of(1, 1, 3, 4).toString(), "1 - 3/4");
        assertEquals(TValueRange.of(1, 2, 3, 4).toString(), "1/2 - 3/4");
        assertEquals(TValueRange.of(1, 2, 4, 4).toString(), "1/2 - 4");
    }

}