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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.classlib.java.time.AbstractDateTimeTest;
import org.teavm.junit.TeaVMTest;

/**
 * Test MonthDay.
 */
@TeaVMTest
public class TestMonthDay extends AbstractDateTimeTest {

    private MonthDay test07x15;

    @BeforeEach
    public void setUp() {
        test07x15 = MonthDay.of(7, 15);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test07x15, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            DAY_OF_MONTH,
            MONTH_OF_YEAR,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<>(Arrays.asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    void check(MonthDay test, int m, int d) {
        assertEquals(m, test.getMonth().getValue());
        assertEquals(d, test.getDayOfMonth());
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        MonthDay expected = MonthDay.now(Clock.systemDefaultZone());
        MonthDay test = MonthDay.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = MonthDay.now(Clock.systemDefaultZone());
            test = MonthDay.now();
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void now_ZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> MonthDay.now((ZoneId) null));
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        MonthDay expected = MonthDay.now(Clock.system(zone));
        MonthDay test = MonthDay.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = MonthDay.now(Clock.system(zone));
            test = MonthDay.now(zone);
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock() {
        Instant instant = LocalDateTime.of(2010, 12, 31, 0, 0).toInstant(ZoneOffset.UTC);
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        MonthDay test = MonthDay.now(clock);
        assertEquals(Month.DECEMBER, test.getMonth());
        assertEquals(31, test.getDayOfMonth());
    }

    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> MonthDay.now((Clock) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intMonth() {
        assertEquals(MonthDay.of(Month.JULY, 15), test07x15);
    }

    @Test
    public void test_factory_intMonth_dayTooLow() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(Month.JANUARY, 0));
    }

    @Test
    public void test_factory_intMonth_dayTooHigh() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(Month.JANUARY, 32));
    }

    @Test
    public void factory_intMonth_nullMonth() {
        assertThrows(NullPointerException.class, () -> MonthDay.of(null, 15));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ints() {
        check(test07x15, 7, 15);
    }

    @Test
    public void test_factory_ints_dayTooLow() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(1, 0));
    }

    @Test
    public void test_factory_ints_dayTooHigh() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(1, 32));
    }


    @Test
    public void test_factory_ints_monthTooLow() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(0, 1));
    }

    @Test
    public void test_factory_ints_monthTooHigh() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(13, 1));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(test07x15, MonthDay.from(LocalDate.of(2007, 7, 15)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> MonthDay.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> MonthDay.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    static Object[][] provider_goodParseData() {
        return new Object[][] {
                {"--01-01", MonthDay.of(1, 1)},
                {"--01-31", MonthDay.of(1, 31)},
                {"--02-01", MonthDay.of(2, 1)},
                {"--02-29", MonthDay.of(2, 29)},
                {"--03-01", MonthDay.of(3, 1)},
                {"--03-31", MonthDay.of(3, 31)},
                {"--04-01", MonthDay.of(4, 1)},
                {"--04-30", MonthDay.of(4, 30)},
                {"--05-01", MonthDay.of(5, 1)},
                {"--05-31", MonthDay.of(5, 31)},
                {"--06-01", MonthDay.of(6, 1)},
                {"--06-30", MonthDay.of(6, 30)},
                {"--07-01", MonthDay.of(7, 1)},
                {"--07-31", MonthDay.of(7, 31)},
                {"--08-01", MonthDay.of(8, 1)},
                {"--08-31", MonthDay.of(8, 31)},
                {"--09-01", MonthDay.of(9, 1)},
                {"--09-30", MonthDay.of(9, 30)},
                {"--10-01", MonthDay.of(10, 1)},
                {"--10-31", MonthDay.of(10, 31)},
                {"--11-01", MonthDay.of(11, 1)},
                {"--11-30", MonthDay.of(11, 30)},
                {"--12-01", MonthDay.of(12, 1)},
                {"--12-31", MonthDay.of(12, 31)},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_goodParseData")
    public void factory_parse_success(String text, MonthDay expected) {
        MonthDay monthDay = MonthDay.parse(text);
        assertEquals(expected, monthDay);
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_badParseData() {
        return new Object[][] {
                {"", 0},
                {"-00", 0},
                {"--FEB-23", 2},
                {"--01-0", 5},
                {"--01-3A", 5},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_badParseData")
    public void factory_parse_fail(String text, int pos) {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                MonthDay.parse(text);
                fail(String.format("Parse should have failed for %s at position %d", text, pos));
            } catch (DateTimeParseException ex) {
                assertEquals(text, ex.getParsedString());
                assertEquals(pos, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_illegalValue_Day() {
        assertThrows(DateTimeParseException.class, () -> MonthDay.parse("--06-32"));
    }

    @Test
    public void factory_parse_invalidValue_Day() {
        assertThrows(DateTimeParseException.class, () -> MonthDay.parse("--06-31"));
    }

    @Test
    public void factory_parse_illegalValue_Month() {
        assertThrows(DateTimeParseException.class, () -> MonthDay.parse("--13-25"));
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> MonthDay.parse(null));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        MonthDay test = MonthDay.parse("12 3", f);
        assertEquals(MonthDay.of(12, 3), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        assertThrows(NullPointerException.class, () -> MonthDay.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> MonthDay.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // get(DateTimeField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_DateTimeField() {
        assertEquals(15, test07x15.getLong(ChronoField.DAY_OF_MONTH));
        assertEquals(7, test07x15.getLong(ChronoField.MONTH_OF_YEAR));
    }

    @Test
    public void test_get_DateTimeField_null() {
        assertThrows(NullPointerException.class, () -> test07x15.getLong((TemporalField) null));
    }

    @Test
    public void test_get_DateTimeField_invalidField() {
        assertThrows(DateTimeException.class, () -> test07x15.getLong(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_get_DateTimeField_timeField() {
        assertThrows(DateTimeException.class, () -> test07x15.getLong(ChronoField.AMPM_OF_DAY));
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleDates() {
        return new Object[][] {
            {1, 1},
            {1, 31},
            {2, 1},
            {2, 28},
            {2, 29},
            {7, 4},
            {7, 5},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleDates")
    public void test_get(int m, int d) {
        MonthDay a = MonthDay.of(m, d);
        assertEquals(Month.of(m), a.getMonth());
        assertEquals(d, a.getDayOfMonth());
    }

    //-----------------------------------------------------------------------
    // with(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_Month() {
        assertEquals(MonthDay.of(1, 30), MonthDay.of(6, 30).with(Month.JANUARY));
    }

    @Test
    public void test_with_Month_adjustToValid() {
        assertEquals(MonthDay.of(6, 30), MonthDay.of(7, 31).with(Month.JUNE));
    }

    @Test
    public void test_with_Month_adjustToValidFeb() {
        assertEquals(MonthDay.of(2, 29), MonthDay.of(7, 31).with(Month.FEBRUARY));
    }

    @Test
    public void test_with_Month_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test, test.with(Month.JUNE));
    }

    @Test
    public void test_with_Month_null() {
        assertThrows(NullPointerException.class, () -> MonthDay.of(6, 30).with((Month) null));
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth() {
        assertEquals(MonthDay.of(1, 30), MonthDay.of(6, 30).withMonth(1));
    }

    @Test
    public void test_withMonth_adjustToValid() {
        assertEquals(MonthDay.of(6, 30), MonthDay.of(7, 31).withMonth(6));
    }

    @Test
    public void test_withMonth_adjustToValidFeb() {
        assertEquals(MonthDay.of(2, 29), MonthDay.of(7, 31).withMonth(2));
    }

    @Test
    public void test_withMonth_int_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test, test.withMonth(6));
    }

    @Test
    public void test_withMonth_tooLow() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(6, 30).withMonth(0));
    }

    @Test
    public void test_withMonth_tooHigh() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(6, 30).withMonth(13));
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth() {
        assertEquals(MonthDay.of(6, 1), MonthDay.of(6, 30).withDayOfMonth(1));
    }

    @Test
    public void test_withDayOfMonth_invalid() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(6, 30).withDayOfMonth(31));
    }

    @Test
    public void test_withDayOfMonth_adjustToValidFeb() {
        assertEquals(MonthDay.of(2, 29), MonthDay.of(2, 1).withDayOfMonth(29));
    }

    @Test
    public void test_withDayOfMonth_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test, test.withDayOfMonth(30));
    }

    @Test
    public void test_withDayOfMonth_tooLow() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(6, 30).withDayOfMonth(0));
    }

    @Test
    public void test_withDayOfMonth_tooHigh() {
        assertThrows(DateTimeException.class, () -> MonthDay.of(6, 30).withDayOfMonth(32));
    }

    //-----------------------------------------------------------------------
    // adjust()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustDate() {
        MonthDay test = MonthDay.of(6, 30);
        LocalDate date = LocalDate.of(2007, 1, 1);
        assertEquals(LocalDate.of(2007, 6, 30), test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_resolve() {
        MonthDay test = MonthDay.of(2, 29);
        LocalDate date = LocalDate.of(2007, 6, 30);
        assertEquals(LocalDate.of(2007, 2, 28), test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_equal() {
        MonthDay test = MonthDay.of(6, 30);
        LocalDate date = LocalDate.of(2007, 6, 30);
        assertEquals(date, test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_null() {
        assertThrows(NullPointerException.class, () -> test07x15.adjustInto((LocalDate) null));
    }

    //-----------------------------------------------------------------------
    // isValidYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidYear_june() {
        MonthDay test = MonthDay.of(6, 30);
        assertTrue(test.isValidYear(2007));
    }

    @Test
    public void test_isValidYear_febNonLeap() {
        MonthDay test = MonthDay.of(2, 29);
        assertFalse(test.isValidYear(2007));
    }

    @Test
    public void test_isValidYear_febLeap() {
        MonthDay test = MonthDay.of(2, 29);
        assertTrue(test.isValidYear(2008));
    }

    //-----------------------------------------------------------------------
    // atYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atYear_int() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(LocalDate.of(2008, 6, 30), test.atYear(2008));
    }

    @Test
    public void test_atYear_int_leapYearAdjust() {
        MonthDay test = MonthDay.of(2, 29);
        assertEquals(LocalDate.of(2005, 2, 28), test.atYear(2005));
    }

    @Test
    public void test_atYear_int_invalidYear() {
        MonthDay test = MonthDay.of(6, 30);
        assertThrows(DateTimeException.class, () -> test.atYear(Integer.MIN_VALUE));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, test07x15.query(TemporalQueries.chronology()));
        assertEquals(null, test07x15.query(TemporalQueries.localDate()));
        assertEquals(null, test07x15.query(TemporalQueries.localTime()));
        assertEquals(null, test07x15.query(TemporalQueries.offset()));
        assertEquals(null, test07x15.query(TemporalQueries.precision()));
        assertEquals(null, test07x15.query(TemporalQueries.zone()));
        assertEquals(null, test07x15.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test07x15.query(null));
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_MonthDay(
            MonthDay.of(1, 1),
            MonthDay.of(1, 31),
            MonthDay.of(2, 1),
            MonthDay.of(2, 29),
            MonthDay.of(3, 1),
            MonthDay.of(12, 31)
        );
    }

    void doTest_comparisons_MonthDay(MonthDay... localDates) {
        for (int i = 0; i < localDates.length; i++) {
            MonthDay a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                MonthDay b = localDates[j];
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0, a + " <=> " + b);
                    assertTrue(a.isBefore(b), a + " <=> " + b);
                    assertFalse(a.isAfter(b), a + " <=> " + b);
                    assertFalse(a.equals(b), a + " <=> " + b);
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0, a + " <=> " + b);
                    assertFalse(a.isBefore(b), a + " <=> " + b);
                    assertTrue(a.isAfter(b), a + " <=> " + b);
                    assertFalse(a.equals(b), a + " <=> " + b);
                } else {
                    assertEquals(0, a.compareTo(b), a + " <=> " + b);
                    assertFalse(a.isBefore(b), a + " <=> " + b);
                    assertFalse(a.isAfter(b), a + " <=> " + b);
                    assertTrue(a.equals(b), a + " <=> " + b);
                }
            }
        }
    }

    @Test
    public void test_compareTo_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test07x15.compareTo(null));
    }

    @Test
    public void test_isBefore_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test07x15.isBefore(null));
    }

    @Test
    public void test_isAfter_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test07x15.isAfter(null));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        MonthDay a = MonthDay.of(1, 1);
        MonthDay b = MonthDay.of(1, 1);
        MonthDay c = MonthDay.of(2, 1);
        MonthDay d = MonthDay.of(1, 2);

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));

        assertTrue(b.equals(a));
        assertTrue(b.equals(b));
        assertFalse(b.equals(c));
        assertFalse(b.equals(d));

        assertFalse(c.equals(a));
        assertFalse(c.equals(b));
        assertTrue(c.equals(c));
        assertFalse(c.equals(d));

        assertFalse(d.equals(a));
        assertFalse(d.equals(b));
        assertFalse(d.equals(c));
        assertTrue(d.equals(d));
    }

    @Test
    public void test_equals_itself_true() {
        assertTrue(test07x15.equals(test07x15));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(test07x15.equals("2007-07-15"));
    }

    @Test
    public void test_equals_null_false() {
        assertFalse(test07x15.equals(null));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleDates")
    public void test_hashCode(int m, int d) {
        MonthDay a = MonthDay.of(m, d);
        assertEquals(a.hashCode(), a.hashCode());
        MonthDay b = MonthDay.of(m, d);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_hashCode_unique() {
        int leapYear = 2008;
        Set<Integer> uniques = new HashSet<Integer>(366);
        for (int i = 1; i <= 12; i++) {
            for (int j = 1; j <= 31; j++) {
                if (YearMonth.of(leapYear, i).isValidDay(j)) {
                    assertTrue(uniques.add(MonthDay.of(i, j).hashCode()));
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleToString() {
        return new Object[][] {
            {7, 5, "--07-05"},
            {12, 31, "--12-31"},
            {1, 2, "--01-02"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int m, int d, String expected) {
        MonthDay test = MonthDay.of(m, d);
        String str = test.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        String t = MonthDay.of(12, 3).format(f);
        assertEquals("12 3", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class, () -> MonthDay.of(12, 3).format(null));
    }

}
