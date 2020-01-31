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
package org.threeten.bp.zone;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.testng.annotations.Test;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Month;
import org.threeten.bp.Year;
import org.threeten.bp.zone.TzdbZoneRulesCompiler.LeapSecondRule;
import org.threeten.bp.zone.TzdbZoneRulesCompiler.TZDBMonthDayTime;
import org.threeten.bp.zone.TzdbZoneRulesCompiler.TZDBRule;
import org.threeten.bp.zone.ZoneOffsetTransitionRule.TimeDefinition;

/**
 * Test TzdbZoneRulesCompiler.
 */
@Test
public class TestTzdbZoneRulesCompiler {

    //-----------------------------------------------------------------------
    // parseYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_parseYear_specific() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "2010", 2000), 2010);
    }

    @Test
    public void test_parseYear_min() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "min", 2000), Year.MIN_VALUE);
    }

    @Test
    public void test_parseYear_mini() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "mini", 2000), Year.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minim() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minim", 2000), Year.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minimu() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minimu", 2000), Year.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minimum() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minimum", 2000), Year.MIN_VALUE);
    }


    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_minTooShort() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "mi", 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_minTooLong() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "minimuma", 2000);
    }

    @Test
    public void test_parseYear_max() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "max", 2000), Year.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maxi() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maxi", 2000), Year.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maxim() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maxim", 2000), Year.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maximu() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maximu", 2000), Year.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maximum() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maximum", 2000), Year.MAX_VALUE);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_maxTooShort() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "ma", 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_maxTooLong() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "maximuma", 2000);
    }

    @Test
    public void test_parseYear_only() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "only", 2000), 2000);
    }

    @Test
    public void test_parseYear_only_uppercase() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "ONLY", 2000), 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_invalidYear() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "ABC", 2000);
    }

    static final Method PARSE_YEAR;
    static {
        try {
            PARSE_YEAR = TzdbZoneRulesCompiler.class.getDeclaredMethod("parseYear", String.class, Integer.TYPE);
            PARSE_YEAR.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private int parseYear(TzdbZoneRulesCompiler test, String str, int year) throws Exception {
        try {
            return (Integer) PARSE_YEAR.invoke(test, str, year);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    // parseMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_parseMonth() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseMonth(test, "Jan"), Month.JANUARY);
        assertEquals(parseMonth(test, "Feb"), Month.FEBRUARY);
        assertEquals(parseMonth(test, "Mar"), Month.MARCH);
        assertEquals(parseMonth(test, "Apr"), Month.APRIL);
        assertEquals(parseMonth(test, "May"), Month.MAY);
        assertEquals(parseMonth(test, "Jun"), Month.JUNE);
        assertEquals(parseMonth(test, "Jul"), Month.JULY);
        assertEquals(parseMonth(test, "Aug"), Month.AUGUST);
        assertEquals(parseMonth(test, "Sep"), Month.SEPTEMBER);
        assertEquals(parseMonth(test, "Oct"), Month.OCTOBER);
        assertEquals(parseMonth(test, "Nov"), Month.NOVEMBER);
        assertEquals(parseMonth(test, "Dec"), Month.DECEMBER);
        assertEquals(parseMonth(test, "January"), Month.JANUARY);
        assertEquals(parseMonth(test, "February"), Month.FEBRUARY);
        assertEquals(parseMonth(test, "March"), Month.MARCH);
        assertEquals(parseMonth(test, "April"), Month.APRIL);
        assertEquals(parseMonth(test, "May"), Month.MAY);
        assertEquals(parseMonth(test, "June"), Month.JUNE);
        assertEquals(parseMonth(test, "July"), Month.JULY);
        assertEquals(parseMonth(test, "August"), Month.AUGUST);
        assertEquals(parseMonth(test, "September"), Month.SEPTEMBER);
        assertEquals(parseMonth(test, "October"), Month.OCTOBER);
        assertEquals(parseMonth(test, "November"), Month.NOVEMBER);
        assertEquals(parseMonth(test, "December"), Month.DECEMBER);
        assertEquals(parseMonth(test, "Janu"), Month.JANUARY);
        assertEquals(parseMonth(test, "Janua"), Month.JANUARY);
        assertEquals(parseMonth(test, "Januar"), Month.JANUARY);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseMonth_invalidMonth() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseMonth(test, "ABC");
    }

    static final Method PARSE_MONTH;
    static {
        try {
            PARSE_MONTH = TzdbZoneRulesCompiler.class.getDeclaredMethod("parseMonth", String.class);
            PARSE_MONTH.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private Month parseMonth(TzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            return (Month) PARSE_MONTH.invoke(test, str);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    // parseDayOfWeek()
    //-----------------------------------------------------------------------
    @Test
    public void test_parseDayOfWeek() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseDayOfWeek(test, "Mon"), DayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Tue"), DayOfWeek.TUESDAY);
        assertEquals(parseDayOfWeek(test, "Wed"), DayOfWeek.WEDNESDAY);
        assertEquals(parseDayOfWeek(test, "Thu"), DayOfWeek.THURSDAY);
        assertEquals(parseDayOfWeek(test, "Fri"), DayOfWeek.FRIDAY);
        assertEquals(parseDayOfWeek(test, "Sat"), DayOfWeek.SATURDAY);
        assertEquals(parseDayOfWeek(test, "Sun"), DayOfWeek.SUNDAY);
        assertEquals(parseDayOfWeek(test, "Monday"), DayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Tuesday"), DayOfWeek.TUESDAY);
        assertEquals(parseDayOfWeek(test, "Wednesday"), DayOfWeek.WEDNESDAY);
        assertEquals(parseDayOfWeek(test, "Thursday"), DayOfWeek.THURSDAY);
        assertEquals(parseDayOfWeek(test, "Friday"), DayOfWeek.FRIDAY);
        assertEquals(parseDayOfWeek(test, "Saturday"), DayOfWeek.SATURDAY);
        assertEquals(parseDayOfWeek(test, "Sunday"), DayOfWeek.SUNDAY);
        assertEquals(parseDayOfWeek(test, "Mond"), DayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Monda"), DayOfWeek.MONDAY);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseDayOfWeek_invalidMonth() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseMonth(test, "ABC");
    }

    static final Method PARSE_DOW;
    static {
        try {
            PARSE_DOW = TzdbZoneRulesCompiler.class.getDeclaredMethod("parseDayOfWeek", String.class);
            PARSE_DOW.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private DayOfWeek parseDayOfWeek(TzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            return (DayOfWeek) PARSE_DOW.invoke(test, str);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    // parseMonthDayTime()
    //-----------------------------------------------------------------------
    @Test
    public void test_parseMonthDayTime_marLastSun0220() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Mar lastSun 2:20");
        assertEquals(mdt.month, Month.MARCH);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SUNDAY);
        assertEquals(mdt.dayOfMonth, -1);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, LocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    @Test
    public void test_parseMonthDayTime_jun50220s() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Jun 5 2:20s");
        assertEquals(mdt.month, Month.JUNE);
        assertEquals(mdt.dayOfWeek, null);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, LocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.STANDARD);
    }

    @Test
    public void test_parseMonthDayTime_maySatAfter50220u() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat>=5 2:20u");
        assertEquals(mdt.month, Month.MAY);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, LocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_maySatBefore50220u() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat<=5 24:00g");
        assertEquals(mdt.month, Month.MAY);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, LocalTime.of(0, 0));
        assertEquals(mdt.adjustDays, 1);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_maySatBefore15Dash() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat<=15 -");
        assertEquals(mdt.month, Month.MAY);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 15);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, LocalTime.of(0, 0));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    @Test
    public void test_parseMonthDayTime_maylastSunShortTime() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May lastSun 3z");
        assertEquals(mdt.month, Month.MAY);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SUNDAY);
        assertEquals(mdt.dayOfMonth, -1);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, LocalTime.of(3, 0));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_sepSatAfter82500() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2018f", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Sep Sat>=8 25:00");
        assertEquals(mdt.month, Month.SEPTEMBER);
        assertEquals(mdt.dayOfWeek, DayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 8);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, LocalTime.of(1, 0));
        assertEquals(mdt.adjustDays, 1);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    static final Method PARSE_MDT;
    static {
        try {
            PARSE_MDT = TzdbZoneRulesCompiler.class.getDeclaredMethod("parseMonthDayTime", StringTokenizer.class, TZDBMonthDayTime.class);
            PARSE_MDT.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private TZDBRule parseMonthDayTime(TzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            TZDBRule mdt = test.new TZDBRule();  // create a bound inner class
            PARSE_MDT.invoke(test, new StringTokenizer(str), mdt);
            return mdt;
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseLeapSecondRule_at_midnight() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        LeapSecondRule lsr = parseLeapSecondRule(test, "Leap\t1972 Jun\t30   23:59:60 +   S");
        assertEquals(lsr.leapDate, LocalDate.of(1972, Month.JUNE, 30));
        assertEquals(lsr.secondAdjustment, +1);
    }

    @Test
    public void test_parseLeapSecondRule_just_before_midnight() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        LeapSecondRule lsr = parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:59 - S");
        assertEquals(lsr.leapDate, LocalDate.of(2009, Month.MAY, 1));
        assertEquals(lsr.secondAdjustment, -1);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_too_short() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1  23:59:60 S");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_bad_adjustment() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:60 % S");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_rolling() throws Exception {
        TzdbZoneRulesCompiler test = new TzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:60 - R");
    }

    static final Method PARSE_LSR;
    static {
        try {
            PARSE_LSR = TzdbZoneRulesCompiler.class.getDeclaredMethod("parseLeapSecondRule", String.class);
            PARSE_LSR.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private LeapSecondRule parseLeapSecondRule(TzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            return (LeapSecondRule)PARSE_LSR.invoke(test, str);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

}
