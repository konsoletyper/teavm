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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.testng.annotations.DataProvider;
import org.junit.Test;

@Test
public class TestPeriod extends AbstractTest {

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    public void test_interfaces() {
        assertTrue(Serializable.class.isAssignableFrom(TPeriod.class));
    }

    @DataProvider(name="serialization")
    Object[][] data_serialization() {
        return new Object[][] {
            {TPeriod.ZERO},
            {TPeriod.ofDays(1)},
            {TPeriod.of(1, 2, 3)},
        };
    }

    @Test(dataProvider="serialization")
    public void test_serialization(TPeriod period) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(period);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        if (period.isZero()) {
            assertSame(ois.readObject(), period);
        } else {
            assertEquals(ois.readObject(), period);
        }
    }

    @Test
    public void test_immutable() {
        assertImmutable(TPeriod.class);
    }

    //-----------------------------------------------------------------------
    // factories
    //-----------------------------------------------------------------------
    public void factory_zeroSingleton() {
        assertSame(TPeriod.ZERO, TPeriod.ZERO);
        assertSame(TPeriod.of(0, 0, 0), TPeriod.ZERO);
        assertSame(TPeriod.ofYears(0), TPeriod.ZERO);
        assertSame(TPeriod.ofMonths(0), TPeriod.ZERO);
        assertSame(TPeriod.ofDays(0), TPeriod.ZERO);
    }

    //-----------------------------------------------------------------------
    // of
    //-----------------------------------------------------------------------
    public void factory_of_ints() {
        assertPeriod(TPeriod.of(1, 2, 3), 1, 2, 3);
        assertPeriod(TPeriod.of(0, 2, 3), 0, 2, 3);
        assertPeriod(TPeriod.of(1, 0, 0), 1, 0, 0);
        assertPeriod(TPeriod.of(0, 0, 0), 0, 0, 0);
        assertPeriod(TPeriod.of(-1, -2, -3), -1, -2, -3);
    }

    //-----------------------------------------------------------------------
    public void factory_ofYears() {
        assertPeriod(TPeriod.ofYears(1), 1, 0, 0);
        assertPeriod(TPeriod.ofYears(0), 0, 0, 0);
        assertPeriod(TPeriod.ofYears(-1), -1, 0, 0);
        assertPeriod(TPeriod.ofYears(Integer.MAX_VALUE), Integer.MAX_VALUE, 0, 0);
        assertPeriod(TPeriod.ofYears(Integer.MIN_VALUE), Integer.MIN_VALUE, 0, 0);
    }

    public void factory_ofMonths() {
        assertPeriod(TPeriod.ofMonths(1), 0, 1, 0);
        assertPeriod(TPeriod.ofMonths(0), 0, 0, 0);
        assertPeriod(TPeriod.ofMonths(-1), 0, -1, 0);
        assertPeriod(TPeriod.ofMonths(Integer.MAX_VALUE), 0, Integer.MAX_VALUE, 0);
        assertPeriod(TPeriod.ofMonths(Integer.MIN_VALUE), 0, Integer.MIN_VALUE, 0);
    }

    public void factory_ofDays() {
        assertPeriod(TPeriod.ofDays(1), 0, 0, 1);
        assertPeriod(TPeriod.ofDays(0), 0, 0, 0);
        assertPeriod(TPeriod.ofDays(-1), 0, 0, -1);
        assertPeriod(TPeriod.ofDays(Integer.MAX_VALUE), 0, 0, Integer.MAX_VALUE);
        assertPeriod(TPeriod.ofDays(Integer.MIN_VALUE), 0, 0, Integer.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // between
    //-----------------------------------------------------------------------
    @DataProvider(name="between")
    Object[][] data_between() {
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

    @Test(dataProvider="between")
    public void factory_between_LocalDate(int y1, int m1, int d1, int y2, int m2, int d2, int ye, int me, int de) {
        TLocalDate start = TLocalDate.of(y1, m1, d1);
        TLocalDate end = TLocalDate.of(y2, m2, d2);
        TPeriod test = TPeriod.between(start, end);
        assertPeriod(test, ye, me, de);
        //assertEquals(start.plus(test), end);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_between_LocalDate_nullFirst() {
        TPeriod.between((TLocalDate) null, TLocalDate.of(2010, 1, 1));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_between_LocalDate_nullSecond() {
        TPeriod.between(TLocalDate.of(2010, 1, 1), (TLocalDate) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @DataProvider(name="parse")
    Object[][] data_parse() {
        return new Object[][] {
            {"P0D", TPeriod.ZERO},
            {"P0W", TPeriod.ZERO},
            {"P0M", TPeriod.ZERO},
            {"P0Y", TPeriod.ZERO},
            
            {"P0Y0D", TPeriod.ZERO},
            {"P0Y0W", TPeriod.ZERO},
            {"P0Y0M", TPeriod.ZERO},
            {"P0M0D", TPeriod.ZERO},
            {"P0M0W", TPeriod.ZERO},
            {"P0W0D", TPeriod.ZERO},
            
            {"P1D", TPeriod.ofDays(1)},
            {"P2D", TPeriod.ofDays(2)},
            {"P-2D", TPeriod.ofDays(-2)},
            {"-P2D", TPeriod.ofDays(-2)},
            {"-P-2D", TPeriod.ofDays(2)},
            {"P" + Integer.MAX_VALUE + "D", TPeriod.ofDays(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "D", TPeriod.ofDays(Integer.MIN_VALUE)},
            
            {"P1W", TPeriod.ofDays(7)},
            {"P2W", TPeriod.ofDays(14)},
            {"P-2W", TPeriod.ofDays(-14)},
            {"-P2W", TPeriod.ofDays(-14)},
            {"-P-2W", TPeriod.ofDays(14)},
            
            {"P1M", TPeriod.ofMonths(1)},
            {"P2M", TPeriod.ofMonths(2)},
            {"P-2M", TPeriod.ofMonths(-2)},
            {"-P2M", TPeriod.ofMonths(-2)},
            {"-P-2M", TPeriod.ofMonths(2)},
            {"P" + Integer.MAX_VALUE + "M", TPeriod.ofMonths(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "M", TPeriod.ofMonths(Integer.MIN_VALUE)},
            
            {"P1Y", TPeriod.ofYears(1)},
            {"P2Y", TPeriod.ofYears(2)},
            {"P-2Y", TPeriod.ofYears(-2)},
            {"-P2Y", TPeriod.ofYears(-2)},
            {"-P-2Y", TPeriod.ofYears(2)},
            {"P" + Integer.MAX_VALUE + "Y", TPeriod.ofYears(Integer.MAX_VALUE)},
            {"P" + Integer.MIN_VALUE + "Y", TPeriod.ofYears(Integer.MIN_VALUE)},
            
            {"P1Y2M3W4D", TPeriod.of(1, 2, 3 * 7 + 4)},
        };
    }

    @Test(dataProvider="parse")
    public void test_parse(String text, TPeriod expected) {
        assertEquals(TPeriod.parse(text), expected);
    }

    @Test(dataProvider="toStringAndParse")
    public void test_parse_toString(TPeriod test, String expected) {
        assertEquals(test, TPeriod.parse(expected));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_parse_nullText() {
        TPeriod.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // isZero()
    //-----------------------------------------------------------------------
    public void test_isZero() {
        assertEquals(TPeriod.of(1, 2, 3).isZero(), false);
        assertEquals(TPeriod.of(1, 0, 0).isZero(), false);
        assertEquals(TPeriod.of(0, 2, 0).isZero(), false);
        assertEquals(TPeriod.of(0, 0, 3).isZero(), false);
        assertEquals(TPeriod.of(0, 0, 0).isZero(), true);
    }

    //-----------------------------------------------------------------------
    // isNegative()
    //-----------------------------------------------------------------------
    public void test_isNegative() {
        assertEquals(TPeriod.of(0, 0, 0).isNegative(), false);
        
        assertEquals(TPeriod.of(1, 2, 3).isNegative(), false);
        assertEquals(TPeriod.of(1, 0, 0).isNegative(), false);
        assertEquals(TPeriod.of(0, 2, 0).isNegative(), false);
        assertEquals(TPeriod.of(0, 0, 3).isNegative(), false);
        
        assertEquals(TPeriod.of(-1, -2, -3).isNegative(), true);
        assertEquals(TPeriod.of(-1, 0, 0).isNegative(), true);
        assertEquals(TPeriod.of(0, -2, 0).isNegative(), true);
        assertEquals(TPeriod.of(0, 0, -3).isNegative(), true);
        assertEquals(TPeriod.of(-1, 2, 3).isNegative(), true);
        assertEquals(TPeriod.of(1, -2, 3).isNegative(), true);
        assertEquals(TPeriod.of(1, 2, -3).isNegative(), true);
    }

    //-----------------------------------------------------------------------
    // withYears()
    //-----------------------------------------------------------------------
    public void test_withYears() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.withYears(10), 10, 2, 3);
    }

    public void test_withYears_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.withYears(1), test);
    }

    public void test_withYears_toZero() {
        TPeriod test = TPeriod.ofYears(1);
        assertSame(test.withYears(0), TPeriod.ZERO);
    }

    //-----------------------------------------------------------------------
    // withMonths()
    //-----------------------------------------------------------------------
    public void test_withMonths() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.withMonths(10), 1, 10, 3);
    }

    public void test_withMonths_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.withMonths(2), test);
    }

    public void test_withMonths_toZero() {
        TPeriod test = TPeriod.ofMonths(1);
        assertSame(test.withMonths(0), TPeriod.ZERO);
    }

    //-----------------------------------------------------------------------
    // withDays()
    //-----------------------------------------------------------------------
    public void test_withDays() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.withDays(10), 1, 2, 10);
    }

    public void test_withDays_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.withDays(3), test);
    }

    public void test_withDays_toZero() {
        TPeriod test = TPeriod.ofDays(1);
        assertSame(test.withDays(0), TPeriod.ZERO);
    }

    //-----------------------------------------------------------------------
    // plus(TPeriod)
    //-----------------------------------------------------------------------
    @DataProvider(name="plus")
    Object[][] data_plus() {
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

    @Test(dataProvider="plus")
    public void test_plus(TPeriod base, TPeriod add, TPeriod expected) {
        assertEquals(base.plus(add), expected);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    public void test_plusYears() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.plusYears(10), 11, 2, 3);
        assertPeriod(test.plus(TPeriod.ofYears(10)), 11, 2, 3);
    }

    public void test_plusYears_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.plusYears(0), test);
        assertPeriod(test.plus(TPeriod.ofYears(0)), 1, 2, 3);
    }

    public void test_plusYears_toZero() {
        TPeriod test = TPeriod.ofYears(-1);
        assertSame(test.plusYears(1), TPeriod.ZERO);
        assertSame(test.plus(TPeriod.ofYears(1)), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusYears_overflowTooBig() {
        TPeriod test = TPeriod.ofYears(Integer.MAX_VALUE);
        test.plusYears(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusYears_overflowTooSmall() {
        TPeriod test = TPeriod.ofYears(Integer.MIN_VALUE);
        test.plusYears(-1);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    public void test_plusMonths() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.plusMonths(10), 1, 12, 3);
        assertPeriod(test.plus(TPeriod.ofMonths(10)), 1, 12, 3);
    }

    public void test_plusMonths_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.plusMonths(0), test);
        assertEquals(test.plus(TPeriod.ofMonths(0)), test);
    }

    public void test_plusMonths_toZero() {
        TPeriod test = TPeriod.ofMonths(-1);
        assertSame(test.plusMonths(1), TPeriod.ZERO);
        assertSame(test.plus(TPeriod.ofMonths(1)), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusMonths_overflowTooBig() {
        TPeriod test = TPeriod.ofMonths(Integer.MAX_VALUE);
        test.plusMonths(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusMonths_overflowTooSmall() {
        TPeriod test = TPeriod.ofMonths(Integer.MIN_VALUE);
        test.plusMonths(-1);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    public void test_plusDays() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.plusDays(10), 1, 2, 13);
    }

    public void test_plusDays_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.plusDays(0), test);
    }

    public void test_plusDays_toZero() {
        TPeriod test = TPeriod.ofDays(-1);
        assertSame(test.plusDays(1), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooBig() {
        TPeriod test = TPeriod.ofDays(Integer.MAX_VALUE);
        test.plusDays(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooSmall() {
        TPeriod test = TPeriod.ofDays(Integer.MIN_VALUE);
        test.plusDays(-1);
    }

    //-----------------------------------------------------------------------
    // minus(TPeriod)
    //-----------------------------------------------------------------------
    @DataProvider(name="minus")
    Object[][] data_minus() {
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

    @Test(dataProvider="minus")
    public void test_minus(TPeriod base, TPeriod subtract, TPeriod expected) {
        assertEquals(base.minus(subtract), expected);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    public void test_minusYears() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.minusYears(10), -9, 2, 3);
    }

    public void test_minusYears_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.minusYears(0), test);
    }

    public void test_minusYears_toZero() {
        TPeriod test = TPeriod.ofYears(1);
        assertSame(test.minusYears(1), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusYears_overflowTooBig() {
        TPeriod test = TPeriod.ofYears(Integer.MAX_VALUE);
        test.minusYears(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusYears_overflowTooSmall() {
        TPeriod test = TPeriod.ofYears(Integer.MIN_VALUE);
        test.minusYears(1);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    public void test_minusMonths() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.minusMonths(10), 1, -8, 3);
    }

    public void test_minusMonths_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.minusMonths(0), test);
    }

    public void test_minusMonths_toZero() {
        TPeriod test = TPeriod.ofMonths(1);
        assertSame(test.minusMonths(1), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusMonths_overflowTooBig() {
        TPeriod test = TPeriod.ofMonths(Integer.MAX_VALUE);
        test.minusMonths(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusMonths_overflowTooSmall() {
        TPeriod test = TPeriod.ofMonths(Integer.MIN_VALUE);
        test.minusMonths(1);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    public void test_minusDays() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.minusDays(10), 1, 2, -7);
    }

    public void test_minusDays_noChange() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.minusDays(0), test);
    }

    public void test_minusDays_toZero() {
        TPeriod test = TPeriod.ofDays(1);
        assertSame(test.minusDays(1), TPeriod.ZERO);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooBig() {
        TPeriod test = TPeriod.ofDays(Integer.MAX_VALUE);
        test.minusDays(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooSmall() {
        TPeriod test = TPeriod.ofDays(Integer.MIN_VALUE);
        test.minusDays(1);
    }

    //-----------------------------------------------------------------------
    // multipliedBy()
    //-----------------------------------------------------------------------
    public void test_multipliedBy() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.multipliedBy(2), 2, 4, 6);
        assertPeriod(test.multipliedBy(-3), -3, -6, -9);
    }

    public void test_multipliedBy_zeroBase() {
        assertSame(TPeriod.ZERO.multipliedBy(2), TPeriod.ZERO);
    }

    public void test_multipliedBy_zero() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.multipliedBy(0), TPeriod.ZERO);
    }

    public void test_multipliedBy_one() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertSame(test.multipliedBy(1), test);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_multipliedBy_overflowTooBig() {
        TPeriod test = TPeriod.ofYears(Integer.MAX_VALUE / 2 + 1);
        test.multipliedBy(2);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_multipliedBy_overflowTooSmall() {
        TPeriod test = TPeriod.ofYears(Integer.MIN_VALUE / 2 - 1);
        test.multipliedBy(2);
    }

    //-----------------------------------------------------------------------
    // negated()
    //-----------------------------------------------------------------------
    public void test_negated() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertPeriod(test.negated(), -1, -2, -3);
    }

    public void test_negated_zero() {
        assertSame(TPeriod.ZERO.negated(), TPeriod.ZERO);
    }

    public void test_negated_max() {
        assertPeriod(TPeriod.ofYears(Integer.MAX_VALUE).negated(), -Integer.MAX_VALUE, 0, 0);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_negated_overflow() {
        TPeriod.ofYears(Integer.MIN_VALUE).negated();
    }

    //-----------------------------------------------------------------------
    // normalized()
    //-----------------------------------------------------------------------
    @DataProvider(name="normalized")
    Object[][] data_normalized() {
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

    @Test(dataProvider="normalized")
    public void test_normalized(int inputYears, int inputMonths, int expectedYears, int expectedMonths) {
        assertPeriod(TPeriod.of(inputYears, inputMonths, 0).normalized(), expectedYears, expectedMonths, 0);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_normalizedMonthsISO_min() {
        TPeriod base = TPeriod.of(Integer.MIN_VALUE, -12, 0);
        base.normalized();
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_normalizedMonthsISO_max() {
        TPeriod base = TPeriod.of(Integer.MAX_VALUE, 12, 0);
        base.normalized();
    }

    //-----------------------------------------------------------------------
    // addTo()
    //-----------------------------------------------------------------------
    @DataProvider(name="addTo")
    Object[][] data_addTo() {
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

    @Test(dataProvider="addTo")
    public void test_addTo(TPeriod period, TLocalDate baseDate, TLocalDate expected) {
        assertEquals(period.addTo(baseDate), expected);
    }

    @Test(dataProvider="addTo")
    public void test_addTo_usingLocalDatePlus(TPeriod period, TLocalDate baseDate, TLocalDate expected) {
        assertEquals(baseDate.plus(period), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_addTo_nullZero() {
        TPeriod.ZERO.addTo(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_addTo_nullNonZero() {
        TPeriod.ofDays(2).addTo(null);
    }

    //-----------------------------------------------------------------------
    // subtractFrom()
    //-----------------------------------------------------------------------
    @DataProvider(name="subtractFrom")
    Object[][] data_subtractFrom() {
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

    @Test(dataProvider="subtractFrom")
    public void test_subtractFrom(TPeriod period, TLocalDate baseDate, TLocalDate expected) {
        assertEquals(period.subtractFrom(baseDate), expected);
    }

    @Test(dataProvider="subtractFrom")
    public void test_subtractFrom_usingLocalDateMinus(TPeriod period, TLocalDate baseDate, TLocalDate expected) {
        assertEquals(baseDate.minus(period), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_subtractFrom_nullZero() {
        TPeriod.ZERO.subtractFrom(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_subtractFrom_nullNonZero() {
        TPeriod.ofDays(2).subtractFrom(null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    public void test_equals() {
        assertEquals(TPeriod.of(1, 0, 0).equals(TPeriod.ofYears(1)), true);
        assertEquals(TPeriod.of(0, 1, 0).equals(TPeriod.ofMonths(1)), true);
        assertEquals(TPeriod.of(0, 0, 1).equals(TPeriod.ofDays(1)), true);
        assertEquals(TPeriod.of(1, 2, 3).equals(TPeriod.of(1, 2, 3)), true);

        assertEquals(TPeriod.ofYears(1).equals(TPeriod.ofYears(1)), true);
        assertEquals(TPeriod.ofYears(1).equals(TPeriod.ofYears(2)), false);

        assertEquals(TPeriod.ofMonths(1).equals(TPeriod.ofMonths(1)), true);
        assertEquals(TPeriod.ofMonths(1).equals(TPeriod.ofMonths(2)), false);

        assertEquals(TPeriod.ofDays(1).equals(TPeriod.ofDays(1)), true);
        assertEquals(TPeriod.ofDays(1).equals(TPeriod.ofDays(2)), false);

        assertEquals(TPeriod.of(1, 2, 3).equals(TPeriod.of(1, 2, 3)), true);
        assertEquals(TPeriod.of(1, 2, 3).equals(TPeriod.of(0, 2, 3)), false);
        assertEquals(TPeriod.of(1, 2, 3).equals(TPeriod.of(1, 0, 3)), false);
        assertEquals(TPeriod.of(1, 2, 3).equals(TPeriod.of(1, 2, 0)), false);
    }

    public void test_equals_self() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertEquals(test.equals(test), true);
    }

    public void test_equals_null() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertEquals(test.equals(null), false);
    }

    public void test_equals_otherClass() {
        TPeriod test = TPeriod.of(1, 2, 3);
        assertEquals(test.equals(""), false);
    }

    //-----------------------------------------------------------------------
    public void test_hashCode() {
        TPeriod test5 = TPeriod.ofDays(5);
        TPeriod test6 = TPeriod.ofDays(6);
        TPeriod test5M = TPeriod.ofMonths(5);
        TPeriod test5Y = TPeriod.ofYears(5);
        assertEquals(test5.hashCode() == test5.hashCode(), true);
        assertEquals(test5.hashCode() == test6.hashCode(), false);
        assertEquals(test5.hashCode() == test5M.hashCode(), false);
        assertEquals(test5.hashCode() == test5Y.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="toStringAndParse")
    Object[][] data_toString() {
        return new Object[][] {
            {TPeriod.ZERO, "P0D"},
            {TPeriod.ofDays(0), "P0D"},
            {TPeriod.ofYears(1), "P1Y"},
            {TPeriod.ofMonths(1), "P1M"},
            {TPeriod.ofDays(1), "P1D"},
            {TPeriod.of(1, 2, 3), "P1Y2M3D"},
        };
    }

    @Test(dataProvider="toStringAndParse")
    public void test_toString(TPeriod input, String expected) {
        assertEquals(input.toString(), expected);
    }

    //-----------------------------------------------------------------------
    private void assertPeriod(TPeriod test, int y, int mo, int d) {
        assertEquals(test.getYears(), y, "years");
        assertEquals(test.getMonths(), mo, "months");
        assertEquals(test.getDays(), d, "days");
    }

    private static TPeriod pymd(int y, int m, int d) {
        return TPeriod.of(y, m, d);
    }

    private static TLocalDate date(int y, int m, int d) {
        return TLocalDate.of(y, m, d);
    }

}
