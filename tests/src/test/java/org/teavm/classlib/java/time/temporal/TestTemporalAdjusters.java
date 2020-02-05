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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.time.TDayOfWeek.MONDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.TUESDAY;
import static org.teavm.classlib.java.time.TMonth.DECEMBER;
import static org.teavm.classlib.java.time.TMonth.JANUARY;

import org.junit.Test;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TMonth;

public class TestTemporalAdjusters {

    @Test
    public void factory_firstDayOfMonth() {

        assertNotNull(TTemporalAdjusters.firstDayOfMonth());
    }

    @Test
    public void test_firstDayOfMonth_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void test_firstDayOfMonth_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void factory_lastDayOfMonth() {

        assertNotNull(TTemporalAdjusters.lastDayOfMonth());
    }

    @Test
    public void test_lastDayOfMonth_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.lastDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), month.length(false));
            }
        }
    }

    @Test
    public void test_lastDayOfMonth_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.lastDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), month.length(true));
            }
        }
    }

    @Test
    public void factory_firstDayOfNextMonth() {

        assertNotNull(TTemporalAdjusters.firstDayOfNextMonth());
    }

    @Test
    public void test_firstDayOfNextMonth_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfNextMonth().adjustInto(date);
                assertEquals(test.getYear(), month == DECEMBER ? 2008 : 2007);
                assertEquals(test.getMonth(), month.plus(1));
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void test_firstDayOfNextMonth_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfNextMonth().adjustInto(date);
                assertEquals(test.getYear(), month == DECEMBER ? 2009 : 2008);
                assertEquals(test.getMonth(), month.plus(1));
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void factory_firstDayOfYear() {

        assertNotNull(TTemporalAdjusters.firstDayOfYear());
    }

    @Test
    public void test_firstDayOfYear_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), TMonth.JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void test_firstDayOfYear_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), TMonth.JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void factory_lastDayOfYear() {

        assertNotNull(TTemporalAdjusters.lastDayOfYear());
    }

    @Test
    public void test_lastDayOfYear_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.lastDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), TMonth.DECEMBER);
                assertEquals(test.getDayOfMonth(), 31);
            }
        }
    }

    @Test
    public void test_lastDayOfYear_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.lastDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), TMonth.DECEMBER);
                assertEquals(test.getDayOfMonth(), 31);
            }
        }
    }

    @Test
    public void factory_firstDayOfNextYear() {

        assertNotNull(TTemporalAdjusters.firstDayOfNextYear());
    }

    @Test
    public void test_firstDayOfNextYear_nonLeap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfNextYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void test_firstDayOfNextYear_leap() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                TLocalDate date = date(2008, month, i);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstDayOfNextYear().adjustInto(date);
                assertEquals(test.getYear(), 2009);
                assertEquals(test.getMonth(), JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test
    public void factory_dayOfWeekInMonth() {

        assertNotNull(TTemporalAdjusters.dayOfWeekInMonth(1, MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_dayOfWeekInMonth_nullDayOfWeek() {

        TTemporalAdjusters.dayOfWeekInMonth(1, null);
    }

    Object[][] data_dayOfWeekInMonth_positive() {

        return new Object[][] { { 2011, 1, TUESDAY, date(2011, 1, 4) }, { 2011, 2, TUESDAY, date(2011, 2, 1) },
        { 2011, 3, TUESDAY, date(2011, 3, 1) }, { 2011, 4, TUESDAY, date(2011, 4, 5) },
        { 2011, 5, TUESDAY, date(2011, 5, 3) }, { 2011, 6, TUESDAY, date(2011, 6, 7) },
        { 2011, 7, TUESDAY, date(2011, 7, 5) }, { 2011, 8, TUESDAY, date(2011, 8, 2) },
        { 2011, 9, TUESDAY, date(2011, 9, 6) }, { 2011, 10, TUESDAY, date(2011, 10, 4) },
        { 2011, 11, TUESDAY, date(2011, 11, 1) }, { 2011, 12, TUESDAY, date(2011, 12, 6) }, };
    }

    @Test
    public void test_dayOfWeekInMonth_positive() {

        for (Object[] data : data_dayOfWeekInMonth_positive()) {
            int year = (int) data[0];
            int month = (int) data[1];
            TDayOfWeek dow = (TDayOfWeek) data[2];
            TLocalDate expected = (TLocalDate) data[3];

            for (int ordinal = 1; ordinal <= 5; ordinal++) {
                for (int day = 1; day <= TMonth.of(month).length(false); day++) {
                    TLocalDate date = date(year, month, day);
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.dayOfWeekInMonth(ordinal, dow).adjustInto(date);
                    assertEquals(test, expected.plusWeeks(ordinal - 1));
                }
            }
        }
    }

    Object[][] data_dayOfWeekInMonth_zero() {

        return new Object[][] { { 2011, 1, TUESDAY, date(2010, 12, 28) }, { 2011, 2, TUESDAY, date(2011, 1, 25) },
        { 2011, 3, TUESDAY, date(2011, 2, 22) }, { 2011, 4, TUESDAY, date(2011, 3, 29) },
        { 2011, 5, TUESDAY, date(2011, 4, 26) }, { 2011, 6, TUESDAY, date(2011, 5, 31) },
        { 2011, 7, TUESDAY, date(2011, 6, 28) }, { 2011, 8, TUESDAY, date(2011, 7, 26) },
        { 2011, 9, TUESDAY, date(2011, 8, 30) }, { 2011, 10, TUESDAY, date(2011, 9, 27) },
        { 2011, 11, TUESDAY, date(2011, 10, 25) }, { 2011, 12, TUESDAY, date(2011, 11, 29) }, };
    }

    @Test
    public void test_dayOfWeekInMonth_zero() {

        for (Object[] data : data_dayOfWeekInMonth_zero()) {
            int year = (int) data[0];
            int month = (int) data[1];
            TDayOfWeek dow = (TDayOfWeek) data[2];
            TLocalDate expected = (TLocalDate) data[3];

            for (int day = 1; day <= TMonth.of(month).length(false); day++) {
                TLocalDate date = date(year, month, day);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.dayOfWeekInMonth(0, dow).adjustInto(date);
                assertEquals(test, expected);
            }
        }
    }

    Object[][] data_dayOfWeekInMonth_negative() {

        return new Object[][] { { 2011, 1, TUESDAY, date(2011, 1, 25) }, { 2011, 2, TUESDAY, date(2011, 2, 22) },
        { 2011, 3, TUESDAY, date(2011, 3, 29) }, { 2011, 4, TUESDAY, date(2011, 4, 26) },
        { 2011, 5, TUESDAY, date(2011, 5, 31) }, { 2011, 6, TUESDAY, date(2011, 6, 28) },
        { 2011, 7, TUESDAY, date(2011, 7, 26) }, { 2011, 8, TUESDAY, date(2011, 8, 30) },
        { 2011, 9, TUESDAY, date(2011, 9, 27) }, { 2011, 10, TUESDAY, date(2011, 10, 25) },
        { 2011, 11, TUESDAY, date(2011, 11, 29) }, { 2011, 12, TUESDAY, date(2011, 12, 27) }, };
    }

    @Test
    public void test_dayOfWeekInMonth_negative() {

        for (Object[] data : data_dayOfWeekInMonth_negative()) {
            int year = (int) data[0];
            int month = (int) data[1];
            TDayOfWeek dow = (TDayOfWeek) data[2];
            TLocalDate expected = (TLocalDate) data[3];

            for (int ordinal = 0; ordinal < 5; ordinal++) {
                for (int day = 1; day <= TMonth.of(month).length(false); day++) {
                    TLocalDate date = date(year, month, day);
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.dayOfWeekInMonth(-1 - ordinal, dow)
                            .adjustInto(date);
                    assertEquals(test, expected.minusWeeks(ordinal));
                }
            }
        }
    }

    @Test
    public void factory_firstInMonth() {

        assertNotNull(TTemporalAdjusters.firstInMonth(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_firstInMonth_nullDayOfWeek() {

        TTemporalAdjusters.firstInMonth(null);
    }

    @Test
    public void test_firstInMonth() {

        for (Object[] data : data_dayOfWeekInMonth_positive()) {
            int year = (int) data[0];
            int month = (int) data[1];
            TDayOfWeek dow = (TDayOfWeek) data[2];
            TLocalDate expected = (TLocalDate) data[3];

            for (int day = 1; day <= TMonth.of(month).length(false); day++) {
                TLocalDate date = date(year, month, day);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.firstInMonth(dow).adjustInto(date);
                assertEquals("day-of-month=" + day, test, expected);
            }
        }
    }

    @Test
    public void factory_lastInMonth() {

        assertNotNull(TTemporalAdjusters.lastInMonth(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_lastInMonth_nullDayOfWeek() {

        TTemporalAdjusters.lastInMonth(null);
    }

    @Test
    public void test_lastInMonth() {

        for (Object[] data : data_dayOfWeekInMonth_negative()) {
            int year = (int) data[0];
            int month = (int) data[1];
            TDayOfWeek dow = (TDayOfWeek) data[2];
            TLocalDate expected = (TLocalDate) data[3];

            for (int day = 1; day <= TMonth.of(month).length(false); day++) {
                TLocalDate date = date(year, month, day);
                TLocalDate test = (TLocalDate) TTemporalAdjusters.lastInMonth(dow).adjustInto(date);
                assertEquals("day-of-month=" + day, test, expected);
            }
        }
    }

    @Test
    public void factory_next() {

        assertNotNull(TTemporalAdjusters.next(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_next_nullDayOfWeek() {

        TTemporalAdjusters.next(null);
    }

    @Test
    public void test_next() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);

                for (TDayOfWeek dow : TDayOfWeek.values()) {
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.next(dow).adjustInto(date);

                    assertSame(date + " " + test, test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff > 0 && dayDiff < 8);
                    } else {
                        assertSame(month, TMonth.DECEMBER);
                        assertTrue(date.getDayOfMonth() > 24);
                        assertEquals(test.getYear(), 2008);
                        assertSame(test.getMonth(), TMonth.JANUARY);
                        assertTrue(test.getDayOfMonth() < 8);
                    }
                }
            }
        }
    }

    @Test
    public void factory_nextOrSame() {

        assertNotNull(TTemporalAdjusters.nextOrSame(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_nextOrSame_nullDayOfWeek() {

        TTemporalAdjusters.nextOrSame(null);
    }

    @Test
    public void test_nextOrSame() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);

                for (TDayOfWeek dow : TDayOfWeek.values()) {
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.nextOrSame(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff < 8);
                        assertEquals(date.equals(test), date.getDayOfWeek() == dow);
                    } else {
                        assertFalse(date.getDayOfWeek() == dow);
                        assertSame(month, TMonth.DECEMBER);
                        assertTrue(date.getDayOfMonth() > 24);
                        assertEquals(test.getYear(), 2008);
                        assertSame(test.getMonth(), TMonth.JANUARY);
                        assertTrue(test.getDayOfMonth() < 8);
                    }
                }
            }
        }
    }

    @Test
    public void factory_previous() {

        assertNotNull(TTemporalAdjusters.previous(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_previous_nullDayOfWeek() {

        TTemporalAdjusters.previous(null);
    }

    @Test
    public void test_previous() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);

                for (TDayOfWeek dow : TDayOfWeek.values()) {
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.previous(dow).adjustInto(date);

                    assertSame(date + " " + test, test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff + " " + test, dayDiff < 0 && dayDiff > -8);
                    } else {
                        assertSame(month, TMonth.JANUARY);
                        assertTrue(date.getDayOfMonth() < 8);
                        assertEquals(test.getYear(), 2006);
                        assertSame(test.getMonth(), TMonth.DECEMBER);
                        assertTrue(test.getDayOfMonth() > 24);
                    }
                }
            }
        }
    }

    @Test
    public void factory_previousOrSame() {

        assertNotNull(TTemporalAdjusters.previousOrSame(MONDAY));
    }

    @Test(expected = NullPointerException.class)
    public void factory_previousOrSame_nullDayOfWeek() {

        TTemporalAdjusters.previousOrSame(null);
    }

    @Test
    public void test_previousOrSame() {

        for (TMonth month : TMonth.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                TLocalDate date = date(2007, month, i);

                for (TDayOfWeek dow : TDayOfWeek.values()) {
                    TLocalDate test = (TLocalDate) TTemporalAdjusters.previousOrSame(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff <= 0 && dayDiff > -7);
                        assertEquals(date.equals(test), date.getDayOfWeek() == dow);
                    } else {
                        assertFalse(date.getDayOfWeek() == dow);
                        assertSame(month, TMonth.JANUARY);
                        assertTrue(date.getDayOfMonth() < 7);
                        assertEquals(test.getYear(), 2006);
                        assertSame(test.getMonth(), TMonth.DECEMBER);
                        assertTrue(test.getDayOfMonth() > 25);
                    }
                }
            }
        }
    }

    private TLocalDate date(int year, TMonth month, int day) {

        return TLocalDate.of(year, month, day);
    }

    private TLocalDate date(int year, int month, int day) {

        return TLocalDate.of(year, month, day);
    }

}
