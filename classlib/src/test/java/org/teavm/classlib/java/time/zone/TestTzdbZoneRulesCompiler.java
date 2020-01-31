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
package org.teavm.classlib.java.time.zone;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.junit.Test;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.zone.TTzdbZoneRulesCompiler.LeapSecondRule;
import org.teavm.classlib.java.time.zone.TTzdbZoneRulesCompiler.TZDBMonthDayTime;
import org.teavm.classlib.java.time.zone.TTzdbZoneRulesCompiler.TZDBRule;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransitionRule.TimeDefinition;

@Test
public class TestTzdbZoneRulesCompiler {

    //-----------------------------------------------------------------------
    // parseYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_parseYear_specific() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "2010", 2000), 2010);
    }

    @Test
    public void test_parseYear_min() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "min", 2000), TYear.MIN_VALUE);
    }

    @Test
    public void test_parseYear_mini() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "mini", 2000), TYear.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minim() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minim", 2000), TYear.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minimu() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minimu", 2000), TYear.MIN_VALUE);
    }

    @Test
    public void test_parseYear_minimum() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "minimum", 2000), TYear.MIN_VALUE);
    }


    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_minTooShort() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "mi", 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_minTooLong() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "minimuma", 2000);
    }

    @Test
    public void test_parseYear_max() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "max", 2000), TYear.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maxi() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maxi", 2000), TYear.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maxim() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maxim", 2000), TYear.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maximu() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maximu", 2000), TYear.MAX_VALUE);
    }

    @Test
    public void test_parseYear_maximum() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "maximum", 2000), TYear.MAX_VALUE);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_maxTooShort() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "ma", 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_maxTooLong() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "maximuma", 2000);
    }

    @Test
    public void test_parseYear_only() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "only", 2000), 2000);
    }

    @Test
    public void test_parseYear_only_uppercase() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseYear(test, "ONLY", 2000), 2000);
    }

    @Test(expectedExceptions=NumberFormatException.class)
    public void test_parseYear_invalidYear() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseYear(test, "ABC", 2000);
    }

    static final Method PARSE_YEAR;
    static {
        try {
            PARSE_YEAR = TTzdbZoneRulesCompiler.class.getDeclaredMethod("parseYear", String.class, Integer.TYPE);
            PARSE_YEAR.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private int parseYear(TTzdbZoneRulesCompiler test, String str, int year) throws Exception {
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
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseMonth(test, "Jan"), TMonth.JANUARY);
        assertEquals(parseMonth(test, "Feb"), TMonth.FEBRUARY);
        assertEquals(parseMonth(test, "Mar"), TMonth.MARCH);
        assertEquals(parseMonth(test, "Apr"), TMonth.APRIL);
        assertEquals(parseMonth(test, "May"), TMonth.MAY);
        assertEquals(parseMonth(test, "Jun"), TMonth.JUNE);
        assertEquals(parseMonth(test, "Jul"), TMonth.JULY);
        assertEquals(parseMonth(test, "Aug"), TMonth.AUGUST);
        assertEquals(parseMonth(test, "Sep"), TMonth.SEPTEMBER);
        assertEquals(parseMonth(test, "Oct"), TMonth.OCTOBER);
        assertEquals(parseMonth(test, "Nov"), TMonth.NOVEMBER);
        assertEquals(parseMonth(test, "Dec"), TMonth.DECEMBER);
        assertEquals(parseMonth(test, "January"), TMonth.JANUARY);
        assertEquals(parseMonth(test, "February"), TMonth.FEBRUARY);
        assertEquals(parseMonth(test, "March"), TMonth.MARCH);
        assertEquals(parseMonth(test, "April"), TMonth.APRIL);
        assertEquals(parseMonth(test, "May"), TMonth.MAY);
        assertEquals(parseMonth(test, "June"), TMonth.JUNE);
        assertEquals(parseMonth(test, "July"), TMonth.JULY);
        assertEquals(parseMonth(test, "August"), TMonth.AUGUST);
        assertEquals(parseMonth(test, "September"), TMonth.SEPTEMBER);
        assertEquals(parseMonth(test, "October"), TMonth.OCTOBER);
        assertEquals(parseMonth(test, "November"), TMonth.NOVEMBER);
        assertEquals(parseMonth(test, "December"), TMonth.DECEMBER);
        assertEquals(parseMonth(test, "Janu"), TMonth.JANUARY);
        assertEquals(parseMonth(test, "Janua"), TMonth.JANUARY);
        assertEquals(parseMonth(test, "Januar"), TMonth.JANUARY);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseMonth_invalidMonth() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseMonth(test, "ABC");
    }

    static final Method PARSE_MONTH;
    static {
        try {
            PARSE_MONTH = TTzdbZoneRulesCompiler.class.getDeclaredMethod("parseMonth", String.class);
            PARSE_MONTH.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private TMonth parseMonth(TTzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            return (TMonth) PARSE_MONTH.invoke(test, str);
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
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        assertEquals(parseDayOfWeek(test, "Mon"), TDayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Tue"), TDayOfWeek.TUESDAY);
        assertEquals(parseDayOfWeek(test, "Wed"), TDayOfWeek.WEDNESDAY);
        assertEquals(parseDayOfWeek(test, "Thu"), TDayOfWeek.THURSDAY);
        assertEquals(parseDayOfWeek(test, "Fri"), TDayOfWeek.FRIDAY);
        assertEquals(parseDayOfWeek(test, "Sat"), TDayOfWeek.SATURDAY);
        assertEquals(parseDayOfWeek(test, "Sun"), TDayOfWeek.SUNDAY);
        assertEquals(parseDayOfWeek(test, "Monday"), TDayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Tuesday"), TDayOfWeek.TUESDAY);
        assertEquals(parseDayOfWeek(test, "Wednesday"), TDayOfWeek.WEDNESDAY);
        assertEquals(parseDayOfWeek(test, "Thursday"), TDayOfWeek.THURSDAY);
        assertEquals(parseDayOfWeek(test, "Friday"), TDayOfWeek.FRIDAY);
        assertEquals(parseDayOfWeek(test, "Saturday"), TDayOfWeek.SATURDAY);
        assertEquals(parseDayOfWeek(test, "Sunday"), TDayOfWeek.SUNDAY);
        assertEquals(parseDayOfWeek(test, "Mond"), TDayOfWeek.MONDAY);
        assertEquals(parseDayOfWeek(test, "Monda"), TDayOfWeek.MONDAY);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseDayOfWeek_invalidMonth() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseMonth(test, "ABC");
    }

    static final Method PARSE_DOW;
    static {
        try {
            PARSE_DOW = TTzdbZoneRulesCompiler.class.getDeclaredMethod("parseDayOfWeek", String.class);
            PARSE_DOW.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private TDayOfWeek parseDayOfWeek(TTzdbZoneRulesCompiler test, String str) throws Exception {
        try {
            return (TDayOfWeek) PARSE_DOW.invoke(test, str);
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
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Mar lastSun 2:20");
        assertEquals(mdt.month, TMonth.MARCH);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SUNDAY);
        assertEquals(mdt.dayOfMonth, -1);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, TLocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    @Test
    public void test_parseMonthDayTime_jun50220s() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Jun 5 2:20s");
        assertEquals(mdt.month, TMonth.JUNE);
        assertEquals(mdt.dayOfWeek, null);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, TLocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.STANDARD);
    }

    @Test
    public void test_parseMonthDayTime_maySatAfter50220u() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat>=5 2:20u");
        assertEquals(mdt.month, TMonth.MAY);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, TLocalTime.of(2, 20));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_maySatBefore50220u() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat<=5 24:00g");
        assertEquals(mdt.month, TMonth.MAY);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 5);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, TLocalTime.of(0, 0));
        assertEquals(mdt.adjustDays, 1);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_maySatBefore15Dash() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May Sat<=15 -");
        assertEquals(mdt.month, TMonth.MAY);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 15);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, TLocalTime.of(0, 0));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    @Test
    public void test_parseMonthDayTime_maylastSunShortTime() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "May lastSun 3z");
        assertEquals(mdt.month, TMonth.MAY);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SUNDAY);
        assertEquals(mdt.dayOfMonth, -1);
        assertEquals(mdt.adjustForwards, false);
        assertEquals(mdt.time, TLocalTime.of(3, 0));
        assertEquals(mdt.adjustDays, 0);
        assertEquals(mdt.timeDefinition, TimeDefinition.UTC);
    }

    @Test
    public void test_parseMonthDayTime_sepSatAfter82500() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2018f", new ArrayList<File>(), null, false);
        TZDBRule mdt = parseMonthDayTime(test, "Sep Sat>=8 25:00");
        assertEquals(mdt.month, TMonth.SEPTEMBER);
        assertEquals(mdt.dayOfWeek, TDayOfWeek.SATURDAY);
        assertEquals(mdt.dayOfMonth, 8);
        assertEquals(mdt.adjustForwards, true);
        assertEquals(mdt.time, TLocalTime.of(1, 0));
        assertEquals(mdt.adjustDays, 1);
        assertEquals(mdt.timeDefinition, TimeDefinition.WALL);
    }

    static final Method PARSE_MDT;
    static {
        try {
            PARSE_MDT = TTzdbZoneRulesCompiler.class.getDeclaredMethod("parseMonthDayTime", StringTokenizer.class, TZDBMonthDayTime.class);
            PARSE_MDT.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private TZDBRule parseMonthDayTime(TTzdbZoneRulesCompiler test, String str) throws Exception {
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
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        LeapSecondRule lsr = parseLeapSecondRule(test, "Leap\t1972 Jun\t30   23:59:60 +   S");
        assertEquals(lsr.leapDate, TLocalDate.of(1972, TMonth.JUNE, 30));
        assertEquals(lsr.secondAdjustment, +1);
    }

    @Test
    public void test_parseLeapSecondRule_just_before_midnight() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        LeapSecondRule lsr = parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:59 - S");
        assertEquals(lsr.leapDate, TLocalDate.of(2009, TMonth.MAY, 1));
        assertEquals(lsr.secondAdjustment, -1);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_too_short() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1  23:59:60 S");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_bad_adjustment() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:60 % S");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parseLeapSecondRule_rolling() throws Exception {
        TTzdbZoneRulesCompiler test = new TTzdbZoneRulesCompiler("2010c", new ArrayList<File>(), null, false);
        parseLeapSecondRule(test, "Leap\t2009 May\t1   23:59:60 - R");
    }

    static final Method PARSE_LSR;
    static {
        try {
            PARSE_LSR = TTzdbZoneRulesCompiler.class.getDeclaredMethod("parseLeapSecondRule", String.class);
            PARSE_LSR.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private LeapSecondRule parseLeapSecondRule(TTzdbZoneRulesCompiler test, String str) throws Exception {
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
