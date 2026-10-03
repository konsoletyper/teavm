/*
 *  Copyright 2020 Alexey Andreev.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.temporal.ValueRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.classlib.java.time.AbstractTest;
import org.teavm.junit.TeaVMTest;

/**
 * Test.
 */
@TeaVMTest
public class TestValueRange extends AbstractTest {

    //-----------------------------------------------------------------------
    // of(long,long)
    //-----------------------------------------------------------------------
    @Test
    public void test_of_longlong() {
        ValueRange test = ValueRange.of(1, 12);
        assertEquals(1, test.getMinimum());
        assertEquals(1, test.getLargestMinimum());
        assertEquals(12, test.getSmallestMaximum());
        assertEquals(12, test.getMaximum());
        assertTrue(test.isFixed());
        assertTrue(test.isIntValue());
    }

    @Test
    public void test_of_longlong_big() {
        ValueRange test = ValueRange.of(1, 123456789012345L);
        assertEquals(1, test.getMinimum());
        assertEquals(1, test.getLargestMinimum());
        assertEquals(123456789012345L, test.getSmallestMaximum());
        assertEquals(123456789012345L, test.getMaximum());
        assertTrue(test.isFixed());
        assertFalse(test.isIntValue());
    }

    @Test
    public void test_of_longlong_minGtMax() {
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(12, 1));
    }

    //-----------------------------------------------------------------------
    // of(long,long,long)
    //-----------------------------------------------------------------------
    @Test
    public void test_of_longlonglong() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertEquals(1, test.getMinimum());
        assertEquals(1, test.getLargestMinimum());
        assertEquals(28, test.getSmallestMaximum());
        assertEquals(31, test.getMaximum());
        assertFalse(test.isFixed());
        assertTrue(test.isIntValue());
    }

    @Test
    public void test_of_longlonglong_minGtMax() {
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(12, 1, 2));
    }

    @Test
    public void test_of_longlonglong_smallestmaxminGtMax() {
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(1, 31, 28));
    }

    //-----------------------------------------------------------------------
    // of(long,long,long,long)
    //-----------------------------------------------------------------------
    static Object[][] data_valid() {
        return new Object[][] {
                {1, 1, 1, 1},
                {1, 1, 1, 2},
                {1, 1, 2, 2},
                {1, 2, 3, 4},
                {1, 1, 28, 31},
                {1, 3, 31, 31},
                {-5, -4, -3, -2},
                {-5, -4, 3, 4},
                {1, 20, 10, 31},
        };
    }

    @ParameterizedTest
    @MethodSource("data_valid")
    public void test_of_longlonglonglong(long sMin, long lMin, long sMax, long lMax) {
        ValueRange test = ValueRange.of(sMin, lMin, sMax, lMax);
        assertEquals(sMin, test.getMinimum());
        assertEquals(lMin, test.getLargestMinimum());
        assertEquals(sMax, test.getSmallestMaximum());
        assertEquals(lMax, test.getMaximum());
        assertEquals(sMin == lMin && sMax == lMax, test.isFixed());
        assertTrue(test.isIntValue());
    }

    static Object[][] data_invalid() {
        return new Object[][] {
                {1, 2, 31, 28},
                {1, 31, 2, 28},
                {31, 2, 1, 28},
                {31, 2, 3, 28},

                {2, 1, 28, 31},
                {2, 1, 31, 28},
                {12, 13, 1, 2},
        };
    }

    @ParameterizedTest
    @MethodSource("data_invalid")
    public void test_of_longlonglonglong_invalid(long sMin, long lMin, long sMax, long lMax) {
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(sMin, lMin, sMax, lMax));
    }

    //-----------------------------------------------------------------------
    // isValidValue(long)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidValue_long() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertFalse(test.isValidValue(0));
        assertTrue(test.isValidValue(1));
        assertTrue(test.isValidValue(2));
        assertTrue(test.isValidValue(30));
        assertTrue(test.isValidValue(31));
        assertFalse(test.isValidValue(32));
    }

    //-----------------------------------------------------------------------
    // isValidIntValue(long)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidValue_long_int() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertFalse(test.isValidValue(0));
        assertTrue(test.isValidValue(1));
        assertTrue(test.isValidValue(31));
        assertFalse(test.isValidValue(32));
    }

    @Test
    public void test_isValidValue_long_long() {
        ValueRange test = ValueRange.of(1, 28, Integer.MAX_VALUE + 1L);
        assertFalse(test.isValidIntValue(0));
        assertFalse(test.isValidIntValue(1));
        assertFalse(test.isValidIntValue(31));
        assertFalse(test.isValidIntValue(32));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals1() {
        ValueRange a = ValueRange.of(1, 2, 3, 4);
        ValueRange b = ValueRange.of(1, 2, 3, 4);
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(b.equals(b));
        assertTrue(a.hashCode() == b.hashCode());
    }

    @Test
    public void test_equals2() {
        ValueRange a = ValueRange.of(1, 2, 3, 4);
        assertFalse(a.equals(ValueRange.of(0, 2, 3, 4)));
        assertFalse(a.equals(ValueRange.of(1, 3, 3, 4)));
        assertFalse(a.equals(ValueRange.of(1, 2, 4, 4)));
        assertFalse(a.equals(ValueRange.of(1, 2, 3, 5)));
    }

    @Test
    public void test_equals_otherType() {
        ValueRange a = ValueRange.of(1, 12);
        assertFalse(a.equals("Rubbish"));
    }

    @Test
    public void test_equals_null() {
        ValueRange a = ValueRange.of(1, 12);
        assertFalse(a.equals(null));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals("1 - 4", ValueRange.of(1, 1, 4, 4).toString());
        assertEquals("1 - 3/4", ValueRange.of(1, 1, 3, 4).toString());
        assertEquals("1/2 - 3/4", ValueRange.of(1, 2, 3, 4).toString());
        assertEquals("1/2 - 4", ValueRange.of(1, 2, 4, 4).toString());
    }

}
