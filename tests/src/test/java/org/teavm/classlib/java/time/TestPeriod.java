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
package org.teavm.classlib.java.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.time.Period;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test.
 */
@TeaVMTest
public class TestPeriod extends AbstractTest {
    //-----------------------------------------------------------------------
    // factories
    //-----------------------------------------------------------------------
    public void factory_zeroSingleton() {
        assertSame(Period.ZERO, Period.ZERO);
        assertSame(Period.ZERO, Period.of(0, 0, 0));
        assertSame(Period.ZERO, Period.ofYears(0));
        assertSame(Period.ZERO, Period.ofMonths(0));
        assertSame(Period.ZERO, Period.ofDays(0));
    }

    //-----------------------------------------------------------------------
    // of
    //-----------------------------------------------------------------------
    public void factory_of_ints() {
        assertPeriod(Period.of(1, 2, 3), 1, 2, 3);
        assertPeriod(Period.of(0, 2, 3), 0, 2, 3);
        assertPeriod(Period.of(1, 0, 0), 1, 0, 0);
        assertPeriod(Period.of(0, 0, 0), 0, 0, 0);
        assertPeriod(Period.of(-1, -2, -3), -1, -2, -3);
    }

    //-----------------------------------------------------------------------
    public void factory_ofYears() {
        assertPeriod(Period.ofYears(1), 1, 0, 0);
        assertPeriod(Period.ofYears(0), 0, 0, 0);
        assertPeriod(Period.ofYears(-1), -1, 0, 0);
        assertPeriod(Period.ofYears(Integer.MAX_VALUE), Integer.MAX_VALUE, 0, 0);
        assertPeriod(Period.ofYears(Integer.MIN_VALUE), Integer.MIN_VALUE, 0, 0);
    }

    public void factory_ofMonths() {
        assertPeriod(Period.ofMonths(1), 0, 1, 0);
        assertPeriod(Period.ofMonths(0), 0, 0, 0);
        assertPeriod(Period.ofMonths(-1), 0, -1, 0);
        assertPeriod(Period.ofMonths(Integer.MAX_VALUE), 0, Integer.MAX_VALUE, 0);
        assertPeriod(Period.ofMonths(Integer.MIN_VALUE), 0, Integer.MIN_VALUE, 0);
    }

    public void factory_ofDays() {
        assertPeriod(Period.ofDays(1), 0, 0, 1);
        assertPeriod(Period.ofDays(0), 0, 0, 0);
        assertPeriod(Period.ofDays(-1), 0, 0, -1);
        assertPeriod(Period.ofDays(Integer.MAX_VALUE), 0, 0, Integer.MAX_VALUE);
        assertPeriod(Period.ofDays(Integer.MIN_VALUE), 0, 0, Integer.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // between
    //-----------------------------------------------------------------------
    static Object[][] data_between() {
        return new Object[][] {
            {2010, 1, 1, 2010, 1, 1, 0, 0, 0},
            {2010, 1, 1, 2010, 1, 2, 0, 0, 1},
            {2010, 1, 1, 2010, 1, 31, 0, 0, 30},
            {2010, 1, 1, 2010, 2, 1, 0, 1, 0},
            {2010, 1, 1, 2010, 2, 28, 0, 1, 27},
            {2010, 1, 1, 2010, 3, 1, 0, 2, 0},
            {2010, 1, 1, 2010, 12, 31, 0, 11, 30},
            {2010, 1, 1, 2011, 1, 1, 1, 0, 0},
            {2010, 1, 1, 2011, 12, 31, 1, 11, 30},
            {2010, 1, 1, 2012, 1, 1, 2, 0, 0},

            {2010, 1, 10, 2010, 1, 1, 0, 0, -9},
            {2010, 1, 10, 2010, 1, 2, 0, 0, -8},
            {2010, 1, 10, 2010, 1, 9, 0, 0, -1},
            {2010, 1, 10, 2010, 1, 10, 0, 0, 0},
            {2010, 1, 10, 2010, 1, 11, 0, 0, 1},
            {2010, 1, 10, 2010, 1, 31, 0, 0, 21},
            {2010, 1, 10, 2010, 2, 1, 0, 0, 22},
            {2010, 1, 10, 2010, 2, 9, 0, 0, 30},
            {2010, 1, 10, 2010, 2, 10, 0, 1, 0},
            {2010, 1, 10, 2010, 2, 28, 0, 1, 18},
            {2010, 1, 10, 2010, 3, 1, 0, 1, 19},
            {2010, 1, 10, 2010, 3, 9, 0, 1, 27},
            {2010, 1, 10, 2010, 3, 10, 0, 2, 0},
            {2010, 1, 10, 2010, 12, 31, 0, 11, 21},
            {2010, 1, 10, 2011, 1, 1, 0, 11, 22},
            {2010, 1, 10, 2011, 1, 9, 0, 11, 30},
            {2010, 1, 10, 2011, 1, 10, 1, 0, 0},

            {2010, 3, 30, 2011, 5, 1, 1, 1, 1},
            {2010, 4, 30, 2011, 5, 1, 1, 0, 1},

            {2010, 2, 28, 2012, 2, 27, 1, 11, 30},
            {2010, 2, 28, 2012, 2, 28, 2, 0, 0},
            {2010, 2, 28, 2012, 2, 29, 2, 0, 1},

            {2012, 2, 28, 2014, 2, 27, 1, 11, 30},
            {2012, 2, 28, 2014, 2, 28, 2, 0, 0},
            {2012, 2, 28, 2014, 3, 1, 2, 0, 1},

            {2012, 2, 29, 2014, 2, 28, 1, 11, 30},
            {2012, 2, 29, 2014, 3, 1, 2, 0, 1},
            {2012, 2, 29, 2014, 3, 2, 2, 0, 2},

            {2012, 2, 29, 2016, 2, 28, 3, 11, 30},
            {2012, 2, 29, 2016, 2, 29, 4, 0, 0},
            {2012, 2, 29, 2016, 3, 1, 4, 0, 1},

            {2010, 1, 1, 2009, 12, 31, 0, 0, -1},
            {2010, 1, 1, 2009, 12, 30, 0, 0, -2},
            {2010, 1, 1, 2009, 12, 2, 0, 0, -30},
            {2010, 1, 1, 2009, 12, 1, 0, -1, 0},
            {2010, 1, 1, 2009, 11, 30, 0, -1, -1},
            {2010, 1, 1, 2009, 11, 2, 0, -1, -29},
            {2010, 1, 1, 2009, 11, 1, 0, -2, 0},
            {2010, 1, 1, 2009, 1, 2, 0, -11, -30},
            {2010, 1, 1, 2009, 1, 1, -1, 0, 0},

            {2010, 1, 15, 2010, 1, 15, 0, 0, 0},
            {2010, 1, 15, 2010, 1, 14, 0, 0, -1},
            {2010, 1, 15, 2010, 1, 1, 0, 0, -14},
            {2010, 1, 15, 2009, 12, 31, 0, 0, -15},
            {2010, 1, 15, 2009, 12, 16, 0, 0, -30},
            {2010, 1, 15, 2009, 12, 15, 0, -1, 0},
            {2010, 1, 15, 2009, 12, 14, 0, -1, -1},

            {2010, 2, 28, 2009, 3, 1, 0, -11, -27},
            {2010, 2, 28, 2009, 2, 28, -1, 0, 0},
            {2010, 2, 28, 2009, 2, 27, -1, 0, -1},

            {2010, 2, 28, 2008, 2, 29, -1, -11, -28},
            {2010, 2, 28, 2008, 2, 28, -2, 0, 0},
            {2010, 2, 28, 2008, 2, 27, -2, 0, -1},

            {2012, 2, 29, 2009, 3, 1, -2, -11, -28},
            {2012, 2, 29, 2009, 2, 28, -3, 0, -1},
            {2012, 2, 29, 2009, 2, 27, -3, 0, -2},

            {2012, 2, 29, 2008, 3, 1, -3, -11, -28},
            {2012, 2, 29, 2008, 2, 29, -4, 0, 0},
            {2012, 2, 29, 2008, 2, 28, -4, 0, -1},
        };
    }

    @ParameterizedTest
    @MethodSource("data_between")
    public void factory_between_LocalDate(int y1, int m1, int d1, int y2, int m2, int d2, int ye, int me, int de) {
        LocalDate start = LocalDate.of(y1, m1, d1);
        LocalDate end = LocalDate.of(y2, m2, d2);
        Period test = Period.between(start, end);
        assertPeriod(test, ye, me, de);
        //assertEquals(start.plus(test), end);
    }

    @Test
    public void factory_between_LocalDate_nullFirst() {
        assertThrows(NullPointerException.class, () -> Period.between((LocalDate) null, LocalDate.of(2010, 1, 1)));
    }

    @Test
    public void factory_between_LocalDate_nullSecond() {
        assertThrows(NullPointerException.class, () -> Period.between(LocalDate.of(2010, 1, 1), (LocalDate) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    static Object[][] data_parse() {
        return new Object[][] {
            {"P0D", Period.ZERO},
            {"P0W", Period.ZERO},
            {"P0M", Period.ZERO},
            {"P0Y", Period.ZERO},
            
            {"P0Y0D", Period.ZERO},
            {"P0Y0W", Period.ZERO},
            {"P0Y0M", Period.ZERO},
            {"P0M0D", Period.ZERO},
            {"P0M0W", Period.ZERO},
            {"P0W0D", Period.ZERO},
            
            {"P1D", Period.ofDays(1)},
            {"P2D", Period.ofDays(2)},
            {"P-2D", Period.ofDays(-2)},
            {"-P2D", Period.ofDays(-2)},
            {"-P-2D", Period.ofDays(2)},
            {"P" + Integer.MAX_VALUE + "D", Period.ofDays(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "D", Period.ofDays(Integer.MIN_VALUE)},
            
            {"P1W", Period.ofDays(7)},
            {"P2W", Period.ofDays(14)},
            {"P-2W", Period.ofDays(-14)},
            {"-P2W", Period.ofDays(-14)},
            {"-P-2W", Period.ofDays(14)},
            
            {"P1M", Period.ofMonths(1)},
            {"P2M", Period.ofMonths(2)},
            {"P-2M", Period.ofMonths(-2)},
            {"-P2M", Period.ofMonths(-2)},
            {"-P-2M", Period.ofMonths(2)},
            {"P" + Integer.MAX_VALUE + "M", Period.ofMonths(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "M", Period.ofMonths(Integer.MIN_VALUE)},
            
            {"P1Y", Period.ofYears(1)},
            {"P2Y", Period.ofYears(2)},
            {"P-2Y", Period.ofYears(-2)},
            {"-P2Y", Period.ofYears(-2)},
            {"-P-2Y", Period.ofYears(2)},
            {"P" + Integer.MAX_VALUE + "Y", Period.ofYears(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "Y", Period.ofYears(Integer.MIN_VALUE)},
            
            {"P1Y2M3W4D", Period.of(1, 2, 3 * 7 + 4)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_parse")
    public void test_parse(String text, Period expected) {
        assertEquals(expected, Period.parse(text));
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_parse_toString(Period test, String expected) {
        assertEquals(Period.parse(expected), test);
    }

    @Test
    public void test_parse_nullText() {
        assertThrows(NullPointerException.class, () -> Period.parse((String) null));
    }

    //-----------------------------------------------------------------------
    // isZero()
    //-----------------------------------------------------------------------
    @Test
    public void test_isZero() {
        assertFalse(Period.of(1, 2, 3).isZero());
        assertFalse(Period.of(1, 0, 0).isZero());
        assertFalse(Period.of(0, 2, 0).isZero());
        assertFalse(Period.of(0, 0, 3).isZero());
        assertTrue(Period.of(0, 0, 0).isZero());
    }

    //-----------------------------------------------------------------------
    // isNegative()
    //-----------------------------------------------------------------------
    @Test
    @Disabled
    // TODO: it's a bug in optimizer, find and fix
    public void test_isNegative() {
        assertFalse(Period.of(0, 0, 0).isNegative());
        
        assertFalse(Period.of(1, 2, 3).isNegative());
        assertFalse(Period.of(1, 0, 0).isNegative());
        assertFalse(Period.of(0, 2, 0).isNegative());
        assertFalse(Period.of(0, 0, 3).isNegative());
        
        assertTrue(Period.of(-1, -2, -3).isNegative());
        assertTrue(Period.of(-1, 0, 0).isNegative());
        assertTrue(Period.of(0, -2, 0).isNegative());
        assertTrue(Period.of(0, 0, -3).isNegative());
        assertTrue(Period.of(-1, 2, 3).isNegative());
        assertTrue(Period.of(1, -2, 3).isNegative());
        assertTrue(Period.of(1, 2, -3).isNegative());
    }

    //-----------------------------------------------------------------------
    // withYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYears() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.withYears(10), 10, 2, 3);
    }

    @Test
    public void test_withYears_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.withYears(1));
    }

    @Test
    public void test_withYears_toZero() {
        Period test = Period.ofYears(1);
        assertSame(Period.ZERO, test.withYears(0));
    }

    //-----------------------------------------------------------------------
    // withMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonths() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.withMonths(10), 1, 10, 3);
    }

    @Test
    public void test_withMonths_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.withMonths(2));
    }

    @Test
    public void test_withMonths_toZero() {
        Period test = Period.ofMonths(1);
        assertSame(Period.ZERO, test.withMonths(0));
    }

    //-----------------------------------------------------------------------
    // withDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDays() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.withDays(10), 1, 2, 10);
    }

    @Test
    public void test_withDays_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.withDays(3));
    }

    @Test
    public void test_withDays_toZero() {
        Period test = Period.ofDays(1);
        assertSame(Period.ZERO, test.withDays(0));
    }

    //-----------------------------------------------------------------------
    // plus(Period)
    //-----------------------------------------------------------------------
    static Object[][] data_plus() {
        return new Object[][] {
            {pymd(0, 0, 0), pymd(0, 0, 0), pymd(0, 0, 0)},
            {pymd(0, 0, 0), pymd(5, 0, 0), pymd(5, 0, 0)},
            {pymd(0, 0, 0), pymd(-5, 0, 0), pymd(-5, 0, 0)},
            {pymd(0, 0, 0), pymd(0, 5, 0), pymd(0, 5, 0)},
            {pymd(0, 0, 0), pymd(0, -5, 0), pymd(0, -5, 0)},
            {pymd(0, 0, 0), pymd(0, 0, 5), pymd(0, 0, 5)},
            {pymd(0, 0, 0), pymd(0, 0, -5), pymd(0, 0, -5)},
            {pymd(0, 0, 0), pymd(2, 3, 4), pymd(2, 3, 4)},
            {pymd(0, 0, 0), pymd(-2, -3, -4), pymd(-2, -3, -4)},

            {pymd(4, 5, 6), pymd(2, 3, 4), pymd(6, 8, 10)},
            {pymd(4, 5, 6), pymd(-2, -3, -4), pymd(2, 2, 2)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_plus")
    public void test_plus(Period base, Period add, Period expected) {
        assertEquals(expected, base.plus(add));
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.plusYears(10), 11, 2, 3);
        assertPeriod(test.plus(Period.ofYears(10)), 11, 2, 3);
    }

    @Test
    public void test_plusYears_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.plusYears(0));
        assertPeriod(test.plus(Period.ofYears(0)), 1, 2, 3);
    }

    @Test
    public void test_plusYears_toZero() {
        Period test = Period.ofYears(-1);
        assertSame(Period.ZERO, test.plusYears(1));
        assertSame(Period.ZERO, test.plus(Period.ofYears(1)));
    }

    @Test
    public void test_plusYears_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusYears(1));
    }

    @Test
    public void test_plusYears_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusYears(-1));
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.plusMonths(10), 1, 12, 3);
        assertPeriod(test.plus(Period.ofMonths(10)), 1, 12, 3);
    }

    @Test
    public void test_plusMonths_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.plusMonths(0));
        assertEquals(test, test.plus(Period.ofMonths(0)));
    }

    @Test
    public void test_plusMonths_toZero() {
        Period test = Period.ofMonths(-1);
        assertSame(Period.ZERO, test.plusMonths(1));
        assertSame(Period.ZERO, test.plus(Period.ofMonths(1)));
    }

    @Test
    public void test_plusMonths_overflowTooBig() {
        Period test = Period.ofMonths(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusMonths(1));
    }

    @Test
    public void test_plusMonths_overflowTooSmall() {
        Period test = Period.ofMonths(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusMonths(-1));
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusDays() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.plusDays(10), 1, 2, 13);
    }

    @Test
    public void test_plusDays_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.plusDays(0));
    }

    @Test
    public void test_plusDays_toZero() {
        Period test = Period.ofDays(-1);
        assertSame(Period.ZERO, test.plusDays(1));
    }

    @Test
    public void test_plusDays_overflowTooBig() {
        Period test = Period.ofDays(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusDays(1));
    }

    @Test
    public void test_plusDays_overflowTooSmall() {
        Period test = Period.ofDays(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.plusDays(-1));
    }

    //-----------------------------------------------------------------------
    // minus(Period)
    //-----------------------------------------------------------------------
    static Object[][] data_minus() {
        return new Object[][] {
            {pymd(0, 0, 0), pymd(0, 0, 0), pymd(0, 0, 0)},
            {pymd(0, 0, 0), pymd(5, 0, 0), pymd(-5, 0, 0)},
            {pymd(0, 0, 0), pymd(-5, 0, 0), pymd(5, 0, 0)},
            {pymd(0, 0, 0), pymd(0, 5, 0), pymd(0, -5, 0)},
            {pymd(0, 0, 0), pymd(0, -5, 0), pymd(0, 5, 0)},
            {pymd(0, 0, 0), pymd(0, 0, 5), pymd(0, 0, -5)},
            {pymd(0, 0, 0), pymd(0, 0, -5), pymd(0, 0, 5)},
            {pymd(0, 0, 0), pymd(2, 3, 4), pymd(-2, -3, -4)},
            {pymd(0, 0, 0), pymd(-2, -3, -4), pymd(2, 3, 4)},

            {pymd(4, 5, 6), pymd(2, 3, 4), pymd(2, 2, 2)},
            {pymd(4, 5, 6), pymd(-2, -3, -4), pymd(6, 8, 10)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_minus")
    public void test_minus(Period base, Period subtract, Period expected) {
        assertEquals(expected, base.minus(subtract));
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.minusYears(10), -9, 2, 3);
    }

    @Test
    public void test_minusYears_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.minusYears(0));
    }

    @Test
    public void test_minusYears_toZero() {
        Period test = Period.ofYears(1);
        assertSame(Period.ZERO, test.minusYears(1));
    }

    @Test
    public void test_minusYears_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusYears(-1));
    }

    @Test
    public void test_minusYears_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusYears(1));
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.minusMonths(10), 1, -8, 3);
    }

    @Test
    public void test_minusMonths_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.minusMonths(0));
    }

    @Test
    public void test_minusMonths_toZero() {
        Period test = Period.ofMonths(1);
        assertSame(Period.ZERO, test.minusMonths(1));
    }

    @Test
    public void test_minusMonths_overflowTooBig() {
        Period test = Period.ofMonths(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusMonths(-1));
    }

    @Test
    public void test_minusMonths_overflowTooSmall() {
        Period test = Period.ofMonths(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusMonths(1));
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusDays() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.minusDays(10), 1, 2, -7);
    }

    @Test
    public void test_minusDays_noChange() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.minusDays(0));
    }

    @Test
    public void test_minusDays_toZero() {
        Period test = Period.ofDays(1);
        assertSame(Period.ZERO, test.minusDays(1));
    }

    @Test
    public void test_minusDays_overflowTooBig() {
        Period test = Period.ofDays(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusDays(-1));
    }

    @Test
    public void test_minusDays_overflowTooSmall() {
        Period test = Period.ofDays(Integer.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> test.minusDays(1));
    }

    //-----------------------------------------------------------------------
    // multipliedBy()
    //-----------------------------------------------------------------------
    @Test
    public void test_multipliedBy() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.multipliedBy(2), 2, 4, 6);
        assertPeriod(test.multipliedBy(-3), -3, -6, -9);
    }

    @Test
    public void test_multipliedBy_zeroBase() {
        assertSame(Period.ZERO, Period.ZERO.multipliedBy(2));
    }

    @Test
    public void test_multipliedBy_zero() {
        Period test = Period.of(1, 2, 3);
        assertSame(Period.ZERO, test.multipliedBy(0));
    }

    @Test
    public void test_multipliedBy_one() {
        Period test = Period.of(1, 2, 3);
        assertSame(test, test.multipliedBy(1));
    }

    @Test
    public void test_multipliedBy_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE / 2 + 1);
        assertThrows(ArithmeticException.class, () -> test.multipliedBy(2));
    }

    @Test
    public void test_multipliedBy_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE / 2 - 1);
        assertThrows(ArithmeticException.class, () -> test.multipliedBy(2));
    }

    //-----------------------------------------------------------------------
    // negated()
    //-----------------------------------------------------------------------
    @Test
    public void test_negated() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.negated(), -1, -2, -3);
    }

    @Test
    public void test_negated_zero() {
        assertSame(Period.ZERO, Period.ZERO.negated());
    }

    @Test
    public void test_negated_max() {
        assertPeriod(Period.ofYears(Integer.MAX_VALUE).negated(), -Integer.MAX_VALUE, 0, 0);
    }

    @Test
    public void test_negated_overflow() {
        assertThrows(ArithmeticException.class, () -> Period.ofYears(Integer.MIN_VALUE).negated());
    }

    //-----------------------------------------------------------------------
    // normalized()
    //-----------------------------------------------------------------------
    static Object[][] data_normalized() {
        return new Object[][] {
            {0, 0,  0, 0},
            {1, 0,  1, 0},
            {-1, 0,  -1, 0},

            {1, 1,  1, 1},
            {1, 2,  1, 2},
            {1, 11,  1, 11},
            {1, 12,  2, 0},
            {1, 13,  2, 1},
            {1, 23,  2, 11},
            {1, 24,  3, 0},
            {1, 25,  3, 1},

            {1, -1,  0, 11},
            {1, -2,  0, 10},
            {1, -11,  0, 1},
            {1, -12,  0, 0},
            {1, -13,  0, -1},
            {1, -23,  0, -11},
            {1, -24,  -1, 0},
            {1, -25,  -1, -1},
            {1, -35,  -1, -11},
            {1, -36,  -2, 0},
            {1, -37,  -2, -1},

            {-1, 1,  0, -11},
            {-1, 11,  0, -1},
            {-1, 12,  0, 0},
            {-1, 13,  0, 1},
            {-1, 23,  0, 11},
            {-1, 24,  1, 0},
            {-1, 25,  1, 1},

            {-1, -1,  -1, -1},
            {-1, -11,  -1, -11},
            {-1, -12,  -2, 0},
            {-1, -13,  -2, -1},
        };
    }

    @ParameterizedTest
    @MethodSource("data_normalized")
    public void test_normalized(int inputYears, int inputMonths, int expectedYears, int expectedMonths) {
        assertPeriod(Period.of(inputYears, inputMonths, 0).normalized(), expectedYears, expectedMonths, 0);
    }

    @Test
    public void test_normalizedMonthsISO_min() {
        Period base = Period.of(Integer.MIN_VALUE, -12, 0);
        assertThrows(ArithmeticException.class, () -> base.normalized());
    }

    @Test
    public void test_normalizedMonthsISO_max() {
        Period base = Period.of(Integer.MAX_VALUE, 12, 0);
        assertThrows(ArithmeticException.class, () -> base.normalized());
    }

    //-----------------------------------------------------------------------
    // addTo()
    //-----------------------------------------------------------------------
    static Object[][] data_addTo() {
        return new Object[][] {
            {pymd(0, 0, 0),  date(2012, 6, 30), date(2012, 6, 30)},

            {pymd(1, 0, 0),  date(2012, 6, 10), date(2013, 6, 10)},
            {pymd(0, 1, 0),  date(2012, 6, 10), date(2012, 7, 10)},
            {pymd(0, 0, 1),  date(2012, 6, 10), date(2012, 6, 11)},

            {pymd(-1, 0, 0),  date(2012, 6, 10), date(2011, 6, 10)},
            {pymd(0, -1, 0),  date(2012, 6, 10), date(2012, 5, 10)},
            {pymd(0, 0, -1),  date(2012, 6, 10), date(2012, 6, 9)},

            {pymd(1, 2, 3),  date(2012, 6, 27), date(2013, 8, 30)},
            {pymd(1, 2, 3),  date(2012, 6, 28), date(2013, 8, 31)},
            {pymd(1, 2, 3),  date(2012, 6, 29), date(2013, 9, 1)},
            {pymd(1, 2, 3),  date(2012, 6, 30), date(2013, 9, 2)},
            {pymd(1, 2, 3),  date(2012, 7, 1), date(2013, 9, 4)},

            {pymd(1, 0, 0),  date(2011, 2, 28), date(2012, 2, 28)},
            {pymd(4, 0, 0),  date(2011, 2, 28), date(2015, 2, 28)},
            {pymd(1, 0, 0),  date(2012, 2, 29), date(2013, 2, 28)},
            {pymd(4, 0, 0),  date(2012, 2, 29), date(2016, 2, 29)},

            {pymd(1, 1, 0),  date(2011, 1, 29), date(2012, 2, 29)},
            {pymd(1, 2, 0),  date(2012, 2, 29), date(2013, 4, 29)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_addTo")
    public void test_addTo(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(expected, period.addTo(baseDate));
    }

    @ParameterizedTest
    @MethodSource("data_addTo")
    public void test_addTo_usingLocalDatePlus(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(expected, baseDate.plus(period));
    }

    @Test
    public void test_addTo_nullZero() {
        assertThrows(NullPointerException.class, () -> Period.ZERO.addTo(null));
    }

    @Test
    public void test_addTo_nullNonZero() {
        assertThrows(NullPointerException.class, () -> Period.ofDays(2).addTo(null));
    }

    //-----------------------------------------------------------------------
    // subtractFrom()
    //-----------------------------------------------------------------------
    static Object[][] data_subtractFrom() {
        return new Object[][] {
            {pymd(0, 0, 0),  date(2012, 6, 30), date(2012, 6, 30)},

            {pymd(1, 0, 0),  date(2012, 6, 10), date(2011, 6, 10)},
            {pymd(0, 1, 0),  date(2012, 6, 10), date(2012, 5, 10)},
            {pymd(0, 0, 1),  date(2012, 6, 10), date(2012, 6, 9)},

            {pymd(-1, 0, 0),  date(2012, 6, 10), date(2013, 6, 10)},
            {pymd(0, -1, 0),  date(2012, 6, 10), date(2012, 7, 10)},
            {pymd(0, 0, -1),  date(2012, 6, 10), date(2012, 6, 11)},

            {pymd(1, 2, 3),  date(2012, 8, 30), date(2011, 6, 27)},
            {pymd(1, 2, 3),  date(2012, 8, 31), date(2011, 6, 27)},
            {pymd(1, 2, 3),  date(2012, 9, 1), date(2011, 6, 28)},
            {pymd(1, 2, 3),  date(2012, 9, 2), date(2011, 6, 29)},
            {pymd(1, 2, 3),  date(2012, 9, 3), date(2011, 6, 30)},
            {pymd(1, 2, 3),  date(2012, 9, 4), date(2011, 7, 1)},

            {pymd(1, 0, 0),  date(2011, 2, 28), date(2010, 2, 28)},
            {pymd(4, 0, 0),  date(2011, 2, 28), date(2007, 2, 28)},
            {pymd(1, 0, 0),  date(2012, 2, 29), date(2011, 2, 28)},
            {pymd(4, 0, 0),  date(2012, 2, 29), date(2008, 2, 29)},

            {pymd(1, 1, 0),  date(2013, 3, 29), date(2012, 2, 29)},
            {pymd(1, 2, 0),  date(2012, 2, 29), date(2010, 12, 29)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_subtractFrom")
    public void test_subtractFrom(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(expected, period.subtractFrom(baseDate));
    }

    @ParameterizedTest
    @MethodSource("data_subtractFrom")
    public void test_subtractFrom_usingLocalDateMinus(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(expected, baseDate.minus(period));
    }

    @Test
    public void test_subtractFrom_nullZero() {
        assertThrows(NullPointerException.class, () -> Period.ZERO.subtractFrom(null));
    }

    @Test
    public void test_subtractFrom_nullNonZero() {
        assertThrows(NullPointerException.class, () -> Period.ofDays(2).subtractFrom(null));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        assertTrue(Period.of(1, 0, 0).equals(Period.ofYears(1)));
        assertTrue(Period.of(0, 1, 0).equals(Period.ofMonths(1)));
        assertTrue(Period.of(0, 0, 1).equals(Period.ofDays(1)));
        assertTrue(Period.of(1, 2, 3).equals(Period.of(1, 2, 3)));

        assertTrue(Period.ofYears(1).equals(Period.ofYears(1)));
        assertFalse(Period.ofYears(1).equals(Period.ofYears(2)));

        assertTrue(Period.ofMonths(1).equals(Period.ofMonths(1)));
        assertFalse(Period.ofMonths(1).equals(Period.ofMonths(2)));

        assertTrue(Period.ofDays(1).equals(Period.ofDays(1)));
        assertFalse(Period.ofDays(1).equals(Period.ofDays(2)));

        assertTrue(Period.of(1, 2, 3).equals(Period.of(1, 2, 3)));
        assertFalse(Period.of(1, 2, 3).equals(Period.of(0, 2, 3)));
        assertFalse(Period.of(1, 2, 3).equals(Period.of(1, 0, 3)));
        assertFalse(Period.of(1, 2, 3).equals(Period.of(1, 2, 0)));
    }

    @Test
    public void test_equals_self() {
        Period test = Period.of(1, 2, 3);
        assertTrue(test.equals(test));
    }

    @Test
    public void test_equals_null() {
        Period test = Period.of(1, 2, 3);
        assertFalse(test.equals(null));
    }

    @Test
    public void test_equals_otherClass() {
        Period test = Period.of(1, 2, 3);
        assertFalse(test.equals(""));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode() {
        Period test5 = Period.ofDays(5);
        Period test6 = Period.ofDays(6);
        Period test5M = Period.ofMonths(5);
        Period test5Y = Period.ofYears(5);
        assertTrue(test5.hashCode() == test5.hashCode());
        assertFalse(test5.hashCode() == test6.hashCode());
        assertFalse(test5.hashCode() == test5M.hashCode());
        assertFalse(test5.hashCode() == test5Y.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
            {Period.ZERO, "P0D"},
            {Period.ofDays(0), "P0D"},
            {Period.ofYears(1), "P1Y"},
            {Period.ofMonths(1), "P1M"},
            {Period.ofDays(1), "P1D"},
            {Period.of(1, 2, 3), "P1Y2M3D"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(Period input, String expected) {
        assertEquals(expected, input.toString());
    }

    //-----------------------------------------------------------------------
    private void assertPeriod(Period test, int y, int mo, int d) {
        assertEquals(y, test.getYears(), "years");
        assertEquals(mo, test.getMonths(), "months");
        assertEquals(d, test.getDays(), "days");
    }

    private static Period pymd(int y, int m, int d) {
        return Period.of(y, m, d);
    }

    private static LocalDate date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }

}
