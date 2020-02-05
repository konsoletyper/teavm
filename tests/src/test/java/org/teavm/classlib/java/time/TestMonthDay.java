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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TMonthDay;
import org.teavm.classlib.java.time.TYearMonth;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

public class TestMonthDay extends AbstractDateTimeTest {

    private TMonthDay TEST_07_15;

    @Before
    public void setUp() {

        this.TEST_07_15 = TMonthDay.of(7, 15);
    }

    @Override
    protected List<TTemporalAccessor> samples() {

        TTemporalAccessor[] array = { this.TEST_07_15, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {

        TTemporalField[] array = { DAY_OF_MONTH, MONTH_OF_YEAR, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {

        List<TTemporalField> list = new ArrayList<>(Arrays.asList(TChronoField.values()));
        list.removeAll(validFields());
        list.add(TJulianFields.JULIAN_DAY);
        list.add(TJulianFields.MODIFIED_JULIAN_DAY);
        list.add(TJulianFields.RATA_DIE);
        return list;
    }

    @Test
    public void test_immutable() {

        assertImmutable(TYearMonth.class);
    }

    void check(TMonthDay test, int m, int d) {

        assertEquals(test.getMonth().getValue(), m);
        assertEquals(test.getDayOfMonth(), d);
    }

    @Test
    public void now() {

        TMonthDay expected = TMonthDay.now(TClock.systemDefaultZone());
        TMonthDay test = TMonthDay.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TMonthDay.now(TClock.systemDefaultZone());
            test = TMonthDay.now();
        }
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void now_ZoneId_nullZoneId() {

        TMonthDay.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {

        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TMonthDay expected = TMonthDay.now(TClock.system(zone));
        TMonthDay test = TMonthDay.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TMonthDay.now(TClock.system(zone));
            test = TMonthDay.now(zone);
        }
        assertEquals(test, expected);
    }

    @Test
    public void now_Clock() {

        TInstant instant = TLocalDateTime.of(2010, 12, 31, 0, 0).toInstant(TZoneOffset.UTC);
        TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
        TMonthDay test = TMonthDay.now(clock);
        assertEquals(test.getMonth(), TMonth.DECEMBER);
        assertEquals(test.getDayOfMonth(), 31);
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullClock() {

        TMonthDay.now((TClock) null);
    }

    @Test
    public void factory_intMonth() {

        assertEquals(this.TEST_07_15, TMonthDay.of(TMonth.JULY, 15));
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_intMonth_dayTooLow() {

        TMonthDay.of(TMonth.JANUARY, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_intMonth_dayTooHigh() {

        TMonthDay.of(TMonth.JANUARY, 32);
    }

    @Test(expected = NullPointerException.class)
    public void factory_intMonth_nullMonth() {

        TMonthDay.of(null, 15);
    }

    @Test
    public void factory_ints() {

        check(this.TEST_07_15, 7, 15);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_dayTooLow() {

        TMonthDay.of(1, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_dayTooHigh() {

        TMonthDay.of(1, 32);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_monthTooLow() {

        TMonthDay.of(0, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_monthTooHigh() {

        TMonthDay.of(13, 1);
    }

    @Test
    public void test_factory_CalendricalObject() {

        assertEquals(TMonthDay.from(TLocalDate.of(2007, 7, 15)), this.TEST_07_15);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {

        TMonthDay.from(TLocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_CalendricalObject_null() {

        TMonthDay.from((TTemporalAccessor) null);
    }

    Object[][] provider_goodParseData() {

        return new Object[][] { { "--01-01", TMonthDay.of(1, 1) }, { "--01-31", TMonthDay.of(1, 31) },
        { "--02-01", TMonthDay.of(2, 1) }, { "--02-29", TMonthDay.of(2, 29) }, { "--03-01", TMonthDay.of(3, 1) },
        { "--03-31", TMonthDay.of(3, 31) }, { "--04-01", TMonthDay.of(4, 1) }, { "--04-30", TMonthDay.of(4, 30) },
        { "--05-01", TMonthDay.of(5, 1) }, { "--05-31", TMonthDay.of(5, 31) }, { "--06-01", TMonthDay.of(6, 1) },
        { "--06-30", TMonthDay.of(6, 30) }, { "--07-01", TMonthDay.of(7, 1) }, { "--07-31", TMonthDay.of(7, 31) },
        { "--08-01", TMonthDay.of(8, 1) }, { "--08-31", TMonthDay.of(8, 31) }, { "--09-01", TMonthDay.of(9, 1) },
        { "--09-30", TMonthDay.of(9, 30) }, { "--10-01", TMonthDay.of(10, 1) }, { "--10-31", TMonthDay.of(10, 31) },
        { "--11-01", TMonthDay.of(11, 1) }, { "--11-30", TMonthDay.of(11, 30) }, { "--12-01", TMonthDay.of(12, 1) },
        { "--12-31", TMonthDay.of(12, 31) }, };
    }

    @Test
    public void factory_parse_success() {

        for (Object[] data : provider_goodParseData()) {
            String text = (String) data[0];
            TMonthDay expected = (TMonthDay) data[1];

            TMonthDay monthDay = TMonthDay.parse(text);
            assertEquals(monthDay, expected);
        }
    }

    Object[][] provider_badParseData() {

        return new Object[][] { { "", 0 }, { "-00", 0 }, { "--FEB-23", 2 }, { "--01-0", 5 }, { "--01-3A", 5 }, };
    }

    @Test
    public void factory_parse_fail() {

        for (Object[] data : provider_badParseData()) {
            String text = (String) data[0];
            int pos = (int) data[1];

            try {
                TMonthDay.parse(text);
                fail(String.format("Parse should have failed for %s at position %d", text, pos));
            } catch (TDateTimeParseException ex) {
                assertEquals(ex.getParsedString(), text);
                assertEquals(ex.getErrorIndex(), pos);
            }
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_illegalValue_Day() {

        TMonthDay.parse("--06-32");
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_invalidValue_Day() {

        TMonthDay.parse("--06-31");
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_illegalValue_Month() {

        TMonthDay.parse("--13-25");
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_nullText() {

        TMonthDay.parse(null);
    }

    @Test
    public void factory_parse_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("M d");
        TMonthDay test = TMonthDay.parse("12 3", f);
        assertEquals(test, TMonthDay.of(12, 3));
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullText() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("M d");
        TMonthDay.parse((String) null, f);
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {

        TMonthDay.parse("ANY", null);
    }

    @Test
    public void test_get_DateTimeField() {

        assertEquals(this.TEST_07_15.getLong(TChronoField.DAY_OF_MONTH), 15);
        assertEquals(this.TEST_07_15.getLong(TChronoField.MONTH_OF_YEAR), 7);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_DateTimeField_null() {

        this.TEST_07_15.getLong((TTemporalField) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_DateTimeField_invalidField() {

        this.TEST_07_15.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_DateTimeField_timeField() {

        this.TEST_07_15.getLong(TChronoField.AMPM_OF_DAY);
    }

    Object[][] provider_sampleDates() {

        return new Object[][] { { 1, 1 }, { 1, 31 }, { 2, 1 }, { 2, 28 }, { 2, 29 }, { 7, 4 }, { 7, 5 }, };
    }

    @Test
    public void test_get() {

        for (Object[] data : provider_sampleDates()) {
            int m = (int) data[0];
            int d = (int) data[1];

            TMonthDay a = TMonthDay.of(m, d);
            assertEquals(a.getMonth(), TMonth.of(m));
            assertEquals(a.getDayOfMonth(), d);
        }
    }

    @Test
    public void test_with_Month() {

        assertEquals(TMonthDay.of(6, 30).with(TMonth.JANUARY), TMonthDay.of(1, 30));
    }

    @Test
    public void test_with_Month_adjustToValid() {

        assertEquals(TMonthDay.of(7, 31).with(TMonth.JUNE), TMonthDay.of(6, 30));
    }

    @Test
    public void test_with_Month_adjustToValidFeb() {

        assertEquals(TMonthDay.of(7, 31).with(TMonth.FEBRUARY), TMonthDay.of(2, 29));
    }

    @Test
    public void test_with_Month_noChangeEqual() {

        TMonthDay test = TMonthDay.of(6, 30);
        assertEquals(test.with(TMonth.JUNE), test);
    }

    @Test(expected = NullPointerException.class)
    public void test_with_Month_null() {

        TMonthDay.of(6, 30).with((TMonth) null);
    }

    @Test
    public void test_withMonth() {

        assertEquals(TMonthDay.of(6, 30).withMonth(1), TMonthDay.of(1, 30));
    }

    @Test
    public void test_withMonth_adjustToValid() {

        assertEquals(TMonthDay.of(7, 31).withMonth(6), TMonthDay.of(6, 30));
    }

    @Test
    public void test_withMonth_adjustToValidFeb() {

        assertEquals(TMonthDay.of(7, 31).withMonth(2), TMonthDay.of(2, 29));
    }

    @Test
    public void test_withMonth_int_noChangeEqual() {

        TMonthDay test = TMonthDay.of(6, 30);
        assertEquals(test.withMonth(6), test);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooLow() {

        TMonthDay.of(6, 30).withMonth(0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooHigh() {

        TMonthDay.of(6, 30).withMonth(13);
    }

    @Test
    public void test_withDayOfMonth() {

        assertEquals(TMonthDay.of(6, 30).withDayOfMonth(1), TMonthDay.of(6, 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_invalid() {

        TMonthDay.of(6, 30).withDayOfMonth(31);
    }

    @Test
    public void test_withDayOfMonth_adjustToValidFeb() {

        assertEquals(TMonthDay.of(2, 1).withDayOfMonth(29), TMonthDay.of(2, 29));
    }

    @Test
    public void test_withDayOfMonth_noChangeEqual() {

        TMonthDay test = TMonthDay.of(6, 30);
        assertEquals(test.withDayOfMonth(30), test);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_tooLow() {

        TMonthDay.of(6, 30).withDayOfMonth(0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_tooHigh() {

        TMonthDay.of(6, 30).withDayOfMonth(32);
    }

    @Test
    public void test_adjustDate() {

        TMonthDay test = TMonthDay.of(6, 30);
        TLocalDate date = TLocalDate.of(2007, 1, 1);
        assertEquals(test.adjustInto(date), TLocalDate.of(2007, 6, 30));
    }

    @Test
    public void test_adjustDate_resolve() {

        TMonthDay test = TMonthDay.of(2, 29);
        TLocalDate date = TLocalDate.of(2007, 6, 30);
        assertEquals(test.adjustInto(date), TLocalDate.of(2007, 2, 28));
    }

    @Test
    public void test_adjustDate_equal() {

        TMonthDay test = TMonthDay.of(6, 30);
        TLocalDate date = TLocalDate.of(2007, 6, 30);
        assertEquals(test.adjustInto(date), date);
    }

    @Test(expected = NullPointerException.class)
    public void test_adjustDate_null() {

        this.TEST_07_15.adjustInto((TLocalDate) null);
    }

    @Test
    public void test_isValidYear_june() {

        TMonthDay test = TMonthDay.of(6, 30);
        assertEquals(test.isValidYear(2007), true);
    }

    @Test
    public void test_isValidYear_febNonLeap() {

        TMonthDay test = TMonthDay.of(2, 29);
        assertEquals(test.isValidYear(2007), false);
    }

    @Test
    public void test_isValidYear_febLeap() {

        TMonthDay test = TMonthDay.of(2, 29);
        assertEquals(test.isValidYear(2008), true);
    }

    @Test
    public void test_atYear_int() {

        TMonthDay test = TMonthDay.of(6, 30);
        assertEquals(test.atYear(2008), TLocalDate.of(2008, 6, 30));
    }

    @Test
    public void test_atYear_int_leapYearAdjust() {

        TMonthDay test = TMonthDay.of(2, 29);
        assertEquals(test.atYear(2005), TLocalDate.of(2005, 2, 28));
    }

    @Test(expected = TDateTimeException.class)
    public void test_atYear_int_invalidYear() {

        TMonthDay test = TMonthDay.of(6, 30);
        test.atYear(Integer.MIN_VALUE);
    }

    @Test
    public void test_query() {

        assertEquals(this.TEST_07_15.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.localDate()), null);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.localTime()), null);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.offset()), null);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.precision()), null);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.zone()), null);
        assertEquals(this.TEST_07_15.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        this.TEST_07_15.query(null);
    }

    @Test
    public void test_comparisons() {

        doTest_comparisons_MonthDay(TMonthDay.of(1, 1), TMonthDay.of(1, 31), TMonthDay.of(2, 1), TMonthDay.of(2, 29),
                TMonthDay.of(3, 1), TMonthDay.of(12, 31));
    }

    void doTest_comparisons_MonthDay(TMonthDay... localDates) {

        for (int i = 0; i < localDates.length; i++) {
            TMonthDay a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                TMonthDay b = localDates[j];
                if (i < j) {
                    assertTrue(a + " <=> " + b, a.compareTo(b) < 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), true);
                    assertEquals(a + " <=> " + b, a.isAfter(b), false);
                    assertEquals(a + " <=> " + b, a.equals(b), false);
                } else if (i > j) {
                    assertTrue(a + " <=> " + b, a.compareTo(b) > 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), false);
                    assertEquals(a + " <=> " + b, a.isAfter(b), true);
                    assertEquals(a + " <=> " + b, a.equals(b), false);
                } else {
                    assertEquals(a + " <=> " + b, a.compareTo(b), 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), false);
                    assertEquals(a + " <=> " + b, a.isAfter(b), false);
                    assertEquals(a + " <=> " + b, a.equals(b), true);
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_compareTo_ObjectNull() {

        this.TEST_07_15.compareTo(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isBefore_ObjectNull() {

        this.TEST_07_15.isBefore(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isAfter_ObjectNull() {

        this.TEST_07_15.isAfter(null);
    }

    @Test
    public void test_equals() {

        TMonthDay a = TMonthDay.of(1, 1);
        TMonthDay b = TMonthDay.of(1, 1);
        TMonthDay c = TMonthDay.of(2, 1);
        TMonthDay d = TMonthDay.of(1, 2);

        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(a.equals(c), false);
        assertEquals(a.equals(d), false);

        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);
        assertEquals(b.equals(c), false);
        assertEquals(b.equals(d), false);

        assertEquals(c.equals(a), false);
        assertEquals(c.equals(b), false);
        assertEquals(c.equals(c), true);
        assertEquals(c.equals(d), false);

        assertEquals(d.equals(a), false);
        assertEquals(d.equals(b), false);
        assertEquals(d.equals(c), false);
        assertEquals(d.equals(d), true);
    }

    @Test
    public void test_equals_itself_true() {

        assertEquals(this.TEST_07_15.equals(this.TEST_07_15), true);
    }

    @Test
    public void test_equals_string_false() {

        assertEquals(this.TEST_07_15.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {

        assertEquals(this.TEST_07_15.equals(null), false);
    }

    @Test
    public void test_hashCode() {

        for (Object[] data : provider_sampleDates()) {
            int m = (int) data[0];
            int d = (int) data[1];

            TMonthDay a = TMonthDay.of(m, d);
            assertEquals(a.hashCode(), a.hashCode());
            TMonthDay b = TMonthDay.of(m, d);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    @Test
    public void test_hashCode_unique() {

        int leapYear = 2008;
        Set<Integer> uniques = new HashSet<>(366);
        for (int i = 1; i <= 12; i++) {
            for (int j = 1; j <= 31; j++) {
                if (TYearMonth.of(leapYear, i).isValidDay(j)) {
                    assertTrue(uniques.add(TMonthDay.of(i, j).hashCode()));
                }
            }
        }
    }

    Object[][] provider_sampleToString() {

        return new Object[][] { { 7, 5, "--07-05" }, { 12, 31, "--12-31" }, { 1, 2, "--01-02" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : provider_sampleToString()) {
            int m = (int) data[0];
            int d = (int) data[1];
            String expected = (String) data[2];

            TMonthDay test = TMonthDay.of(m, d);
            String str = test.toString();
            assertEquals(str, expected);
        }
    }

    @Test
    public void test_format_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("M d");
        String t = TMonthDay.of(12, 3).format(f);
        assertEquals(t, "12 3");
    }

    @Test(expected = NullPointerException.class)
    public void test_format_formatter_null() {

        TMonthDay.of(12, 3).format(null);
    }

}
