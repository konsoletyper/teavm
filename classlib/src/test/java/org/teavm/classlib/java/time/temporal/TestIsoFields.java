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
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.time.TDayOfWeek.FRIDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.MONDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.SATURDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.SUNDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.THURSDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.TUESDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.WEDNESDAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;

@Test
public class TestIsoFields {

    public void test_enum() {
        assertTrue(TIsoFields.WEEK_OF_WEEK_BASED_YEAR instanceof Enum);
        assertTrue(TIsoFields.WEEK_BASED_YEAR instanceof Enum);
        assertTrue(TIsoFields.WEEK_BASED_YEARS instanceof Enum);
    }

    @DataProvider(name="week")
    Object[][] data_week() {
        return new Object[][] {
                {TLocalDate.of(1969, 12, 29), MONDAY, 1, 1970},
                {TLocalDate.of(2012, 12, 23), SUNDAY, 51, 2012},
                {TLocalDate.of(2012, 12, 24), MONDAY, 52, 2012},
                {TLocalDate.of(2012, 12, 27), THURSDAY, 52, 2012},
                {TLocalDate.of(2012, 12, 28), FRIDAY, 52, 2012},
                {TLocalDate.of(2012, 12, 29), SATURDAY, 52, 2012},
                {TLocalDate.of(2012, 12, 30), SUNDAY, 52, 2012},
                {TLocalDate.of(2012, 12, 31), MONDAY, 1, 2013},
                {TLocalDate.of(2013, 1, 1), TUESDAY, 1, 2013},
                {TLocalDate.of(2013, 1, 2), WEDNESDAY, 1, 2013},
                {TLocalDate.of(2013, 1, 6), SUNDAY, 1, 2013},
                {TLocalDate.of(2013, 1, 7), MONDAY, 2, 2013},
        };
    }

    //-----------------------------------------------------------------------
    // WEEK_OF_WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_WOWBY(TLocalDate date, TDayOfWeek dow, int week, int wby) {
        assertEquals(date.getDayOfWeek(), dow);
        assertEquals(TIsoFields.WEEK_OF_WEEK_BASED_YEAR.getFrom(date), week);
        assertEquals(date.get(TIsoFields.WEEK_OF_WEEK_BASED_YEAR), week);
    }

    //-----------------------------------------------------------------------
    // WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_WBY(TLocalDate date, TDayOfWeek dow, int week, int wby) {
        assertEquals(date.getDayOfWeek(), dow);
        assertEquals(TIsoFields.WEEK_BASED_YEAR.getFrom(date), wby);
        assertEquals(date.get(TIsoFields.WEEK_BASED_YEAR), wby);
    }

    //-----------------------------------------------------------------------
    // parse weeks
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_parse_weeks(TLocalDate date, TDayOfWeek dow, int week, int wby) {
        TDateTimeFormatter f = new TDateTimeFormatterBuilder()
                .appendValue(TIsoFields.WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(TIsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(DAY_OF_WEEK).toFormatter();
        TLocalDate parsed = TLocalDate.parse(wby + "-" + week + "-" + dow.getValue(), f);
        assertEquals(parsed, date);
    }

    //-----------------------------------------------------------------------
    public void test_loop() {
        // loop round at least one 400 year cycle, including before 1970
        TLocalDate date = TLocalDate.of(1960, 1, 5);  // Tuseday of week 1 1960
        int year = 1960;
        int wby = 1960;
        int weekLen = 52;
        int week = 1;
        while (date.getYear() < 2400) {
            TDayOfWeek loopDow = date.getDayOfWeek();
            if (date.getYear() != year) {
                year = date.getYear();
            }
            if (loopDow == MONDAY) {
                week++;
                if ((week == 53 && weekLen == 52) || week == 54) {
                    week = 1;
                    TLocalDate firstDayOfWeekBasedYear = date.plusDays(14).withDayOfYear(1);
                    TDayOfWeek firstDay = firstDayOfWeekBasedYear.getDayOfWeek();
                    weekLen = (firstDay == THURSDAY || (firstDay == WEDNESDAY && firstDayOfWeekBasedYear.isLeapYear()) ? 53 : 52);
                    wby++;
                }
            }
            assertEquals(TIsoFields.WEEK_OF_WEEK_BASED_YEAR.rangeRefinedBy(date), TValueRange.of(1, weekLen), "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(TIsoFields.WEEK_OF_WEEK_BASED_YEAR.getFrom(date), week, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(date.get(TIsoFields.WEEK_OF_WEEK_BASED_YEAR), week, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(TIsoFields.WEEK_BASED_YEAR.getFrom(date), wby, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(date.get(TIsoFields.WEEK_BASED_YEAR), wby, "Failed on " + date + " " + date.getDayOfWeek());
            date = date.plusDays(1);
        }
    }

    //-----------------------------------------------------------------------
     // quarters between
     //-----------------------------------------------------------------------
     @DataProvider(name="quartersBetween")
     Object[][] data_quartersBetween() {
         return new Object[][] {
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 1, 1), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 1, 2), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 2, 1), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 3, 1), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 3, 31), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 4, 1), 1},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 4, 2), 1},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 6, 30), 1},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 7, 1), 2},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 10, 1), 3},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2000, 12, 31), 3},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2001, 1, 1), 4},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(2002, 1, 1), 8},

                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 12, 31), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 10, 2), 0},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 10, 1), -1},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 7, 2), -1},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 7, 1), -2},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 4, 2), -2},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 4, 1), -3},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 1, 2), -3},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1999, 1, 1), -4},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1998, 12, 31), -4},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1998, 10, 2), -4},
                 {TLocalDate.of(2000, 1, 1), TLocalDate.of(1998, 10, 1), -5},
         };
     }

     @Test(dataProvider="quartersBetween")
     public void test_quarters_between(TLocalDate start, TLocalDate end, long expected) {
         assertEquals(TIsoFields.QUARTER_YEARS.between(start, end), expected);
     }

    // TODO: more tests
}
