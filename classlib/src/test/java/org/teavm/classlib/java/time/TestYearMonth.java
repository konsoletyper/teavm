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
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;

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
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TYearMonth;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

public class TestYearMonth extends AbstractDateTimeTest {

    private TYearMonth TEST_2008_06;

    @Before
    public void setUp() {

        this.TEST_2008_06 = TYearMonth.of(2008, 6);
    }

    @Override
    protected List<TTemporalAccessor> samples() {

        TTemporalAccessor[] array = { this.TEST_2008_06, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {

        TTemporalField[] array = { MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR_OF_ERA, YEAR, ERA, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {

        List<TTemporalField> list = new ArrayList<TTemporalField>(
                Arrays.<TTemporalField> asList(TChronoField.values()));
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

    void check(TYearMonth test, int y, int m) {

        assertEquals(test.getYear(), y);
        assertEquals(test.getMonth().getValue(), m);
    }

    @Test
    public void now() {

        TYearMonth expected = TYearMonth.now(TClock.systemDefaultZone());
        TYearMonth test = TYearMonth.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TYearMonth.now(TClock.systemDefaultZone());
            test = TYearMonth.now();
        }
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void now_ZoneId_nullZoneId() {

        TYearMonth.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {

        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TYearMonth expected = TYearMonth.now(TClock.system(zone));
        TYearMonth test = TYearMonth.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TYearMonth.now(TClock.system(zone));
            test = TYearMonth.now(zone);
        }
        assertEquals(test, expected);
    }

    @Test
    public void now_Clock() {

        TInstant instant = TLocalDateTime.of(2010, 12, 31, 0, 0).toInstant(TZoneOffset.UTC);
        TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
        TYearMonth test = TYearMonth.now(clock);
        assertEquals(test.getYear(), 2010);
        assertEquals(test.getMonth(), TMonth.DECEMBER);
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullClock() {

        TYearMonth.now((TClock) null);
    }

    @Test
    public void factory_intsMonth() {

        TYearMonth test = TYearMonth.of(2008, TMonth.FEBRUARY);
        check(test, 2008, 2);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_intsMonth_yearTooLow() {

        TYearMonth.of(TYear.MIN_VALUE - 1, TMonth.JANUARY);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_intsMonth_dayTooHigh() {

        TYearMonth.of(TYear.MAX_VALUE + 1, TMonth.JANUARY);
    }

    @Test(expected = NullPointerException.class)
    public void factory_intsMonth_nullMonth() {

        TYearMonth.of(2008, null);
    }

    @Test
    public void factory_ints() {

        TYearMonth test = TYearMonth.of(2008, 2);
        check(test, 2008, 2);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_yearTooLow() {

        TYearMonth.of(TYear.MIN_VALUE - 1, 2);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_dayTooHigh() {

        TYearMonth.of(TYear.MAX_VALUE + 1, 2);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_monthTooLow() {

        TYearMonth.of(2008, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ints_monthTooHigh() {

        TYearMonth.of(2008, 13);
    }

    @Test
    public void test_factory_CalendricalObject() {

        assertEquals(TYearMonth.from(TLocalDate.of(2007, 7, 15)), TYearMonth.of(2007, 7));
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {

        TYearMonth.from(TLocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_CalendricalObject_null() {

        TYearMonth.from((TTemporalAccessor) null);
    }

    Object[][] provider_goodParseData() {

        return new Object[][] { { "0000-01", TYearMonth.of(0, 1) }, { "0000-12", TYearMonth.of(0, 12) },
        { "9999-12", TYearMonth.of(9999, 12) }, { "2000-01", TYearMonth.of(2000, 1) },
        { "2000-02", TYearMonth.of(2000, 2) }, { "2000-03", TYearMonth.of(2000, 3) },
        { "2000-04", TYearMonth.of(2000, 4) }, { "2000-05", TYearMonth.of(2000, 5) },
        { "2000-06", TYearMonth.of(2000, 6) }, { "2000-07", TYearMonth.of(2000, 7) },
        { "2000-08", TYearMonth.of(2000, 8) }, { "2000-09", TYearMonth.of(2000, 9) },
        { "2000-10", TYearMonth.of(2000, 10) }, { "2000-11", TYearMonth.of(2000, 11) },
        { "2000-12", TYearMonth.of(2000, 12) },

        { "+12345678-03", TYearMonth.of(12345678, 3) }, { "+123456-03", TYearMonth.of(123456, 3) },
        { "0000-03", TYearMonth.of(0, 3) }, { "-1234-03", TYearMonth.of(-1234, 3) },
        { "-12345678-03", TYearMonth.of(-12345678, 3) },

        { "+" + TYear.MAX_VALUE + "-03", TYearMonth.of(TYear.MAX_VALUE, 3) },
        { TYear.MIN_VALUE + "-03", TYearMonth.of(TYear.MIN_VALUE, 3) }, };
    }

    @Test
    public void factory_parse_success() {

        for (Object[] data : provider_goodParseData()) {
            String text = (String) data[0];
            TYearMonth expected = (TYearMonth) data[1];

            TYearMonth yearMonth = TYearMonth.parse(text);
            assertEquals(yearMonth, expected);
        }
    }

    Object[][] provider_badParseData() {

        return new Object[][] { { "", 0 }, { "-00", 1 }, { "--01-0", 1 }, { "A01-3", 0 }, { "200-01", 0 },
        { "2009/12", 4 },

        { "-0000-10", 0 }, { "-12345678901-10", 11 }, { "+1-10", 1 }, { "+12-10", 1 }, { "+123-10", 1 },
        { "+1234-10", 0 }, { "12345-10", 0 }, { "+12345678901-10", 11 }, };
    }

    @Test
    public void factory_parse_fail() {

        for (Object[] data : provider_badParseData()) {
            String text = (String) data[0];
            int pos = (int) data[1];

            try {
                TYearMonth.parse(text);
                fail(String.format("Parse should have failed for %s at position %d", text, pos));
            } catch (TDateTimeParseException ex) {
                assertEquals(ex.getParsedString(), text);
                assertEquals(ex.getErrorIndex(), pos);
            }
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_illegalValue_Month() {

        TYearMonth.parse("2008-13");
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_nullText() {

        TYearMonth.parse(null);
    }

    @Test
    public void factory_parse_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M");
        TYearMonth test = TYearMonth.parse("2010 12", f);
        assertEquals(test, TYearMonth.of(2010, 12));
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullText() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M");
        TYearMonth.parse((String) null, f);
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {

        TYearMonth.parse("ANY", null);
    }

    @Test
    public void test_get_TemporalField() {

        assertEquals(this.TEST_2008_06.get(YEAR), 2008);
        assertEquals(this.TEST_2008_06.get(MONTH_OF_YEAR), 6);
        assertEquals(this.TEST_2008_06.get(YEAR_OF_ERA), 2008);
        assertEquals(this.TEST_2008_06.get(ERA), 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_tooBig() {

        this.TEST_2008_06.get(PROLEPTIC_MONTH);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_TemporalField_null() {

        this.TEST_2008_06.get((TTemporalField) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_invalidField() {

        this.TEST_2008_06.get(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_timeField() {

        this.TEST_2008_06.get(TChronoField.AMPM_OF_DAY);
    }

    @Test
    public void test_getLong_TemporalField() {

        assertEquals(this.TEST_2008_06.getLong(YEAR), 2008);
        assertEquals(this.TEST_2008_06.getLong(MONTH_OF_YEAR), 6);
        assertEquals(this.TEST_2008_06.getLong(YEAR_OF_ERA), 2008);
        assertEquals(this.TEST_2008_06.getLong(ERA), 1);
        assertEquals(this.TEST_2008_06.getLong(PROLEPTIC_MONTH), 2008 * 12 + 6 - 1);
    }

    @Test(expected = NullPointerException.class)
    public void test_getLong_TemporalField_null() {

        this.TEST_2008_06.getLong((TTemporalField) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_getLong_TemporalField_invalidField() {

        this.TEST_2008_06.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_getLong_TemporalField_timeField() {

        this.TEST_2008_06.getLong(TChronoField.AMPM_OF_DAY);
    }

    @Test
    public void test_with_Year() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.with(TYear.of(2000)), TYearMonth.of(2000, 6));
    }

    @Test
    public void test_with_Year_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.with(TYear.of(2008)), test);
    }

    @Test(expected = NullPointerException.class)
    public void test_with_Year_null() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.with((TYear) null);
    }

    @Test
    public void test_with_Month() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.with(TMonth.JANUARY), TYearMonth.of(2008, 1));
    }

    @Test
    public void test_with_Month_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.with(TMonth.JUNE), test);
    }

    @Test(expected = NullPointerException.class)
    public void test_with_Month_null() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.with((TMonth) null);
    }

    @Test
    public void test_withYear() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.withYear(1999), TYearMonth.of(1999, 6));
    }

    @Test
    public void test_withYear_int_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.withYear(2008), test);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withYear_tooLow() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.withYear(TYear.MIN_VALUE - 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withYear_tooHigh() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.withYear(TYear.MAX_VALUE + 1);
    }

    @Test
    public void test_withMonth() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.withMonth(1), TYearMonth.of(2008, 1));
    }

    @Test
    public void test_withMonth_int_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.withMonth(6), test);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooLow() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.withMonth(0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooHigh() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.withMonth(13);
    }

    @Test
    public void test_plusYears_long() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusYears(1), TYearMonth.of(2009, 6));
    }

    @Test
    public void test_plusYears_long_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusYears(0), test);
    }

    @Test
    public void test_plusYears_long_negative() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusYears(-1), TYearMonth.of(2007, 6));
    }

    @Test
    public void test_plusYears_long_big() {

        TYearMonth test = TYearMonth.of(-40, 6);
        assertEquals(test.plusYears(20L + TYear.MAX_VALUE), TYearMonth.of((int) (-40L + 20L + TYear.MAX_VALUE), 6));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLarge() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 6);
        test.plusYears(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMax() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.plusYears(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMin() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.plusYears(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooSmall() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 6);
        test.plusYears(-1);
    }

    @Test
    public void test_plusMonths_long() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusMonths(1), TYearMonth.of(2008, 7));
    }

    @Test
    public void test_plusMonths_long_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusMonths(0), test);
    }

    @Test
    public void test_plusMonths_long_overYears() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusMonths(7), TYearMonth.of(2009, 1));
    }

    @Test
    public void test_plusMonths_long_negative() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusMonths(-1), TYearMonth.of(2008, 5));
    }

    @Test
    public void test_plusMonths_long_negativeOverYear() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.plusMonths(-6), TYearMonth.of(2007, 12));
    }

    @Test
    public void test_plusMonths_long_big() {

        TYearMonth test = TYearMonth.of(-40, 6);
        long months = 20L + Integer.MAX_VALUE;
        assertEquals(test.plusMonths(months), TYearMonth.of((int) (-40L + months / 12), 6 + (int) (months % 12)));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLarge() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.plusMonths(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMax() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.plusMonths(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMin() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.plusMonths(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooSmall() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 1);
        test.plusMonths(-1);
    }

    @Test
    public void test_minusYears_long() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusYears(1), TYearMonth.of(2007, 6));
    }

    @Test
    public void test_minusYears_long_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusYears(0), test);
    }

    @Test
    public void test_minusYears_long_negative() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusYears(-1), TYearMonth.of(2009, 6));
    }

    @Test
    public void test_minusYears_long_big() {

        TYearMonth test = TYearMonth.of(40, 6);
        assertEquals(test.minusYears(20L + TYear.MAX_VALUE), TYearMonth.of((int) (40L - 20L - TYear.MAX_VALUE), 6));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLarge() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 6);
        test.minusYears(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxSubtractMax() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 12);
        test.minusYears(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxSubtractMin() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 12);
        test.minusYears(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooSmall() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 6);
        test.minusYears(1);
    }

    @Test
    public void test_minusMonths_long() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusMonths(1), TYearMonth.of(2008, 5));
    }

    @Test
    public void test_minusMonths_long_noChange_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusMonths(0), test);
    }

    @Test
    public void test_minusMonths_long_overYears() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusMonths(6), TYearMonth.of(2007, 12));
    }

    @Test
    public void test_minusMonths_long_negative() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusMonths(-1), TYearMonth.of(2008, 7));
    }

    @Test
    public void test_minusMonths_long_negativeOverYear() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.minusMonths(-7), TYearMonth.of(2009, 1));
    }

    @Test
    public void test_minusMonths_long_big() {

        TYearMonth test = TYearMonth.of(40, 6);
        long months = 20L + Integer.MAX_VALUE;
        assertEquals(test.minusMonths(months), TYearMonth.of((int) (40L - months / 12), 6 - (int) (months % 12)));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLarge() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.minusMonths(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxSubtractMax() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.minusMonths(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxSubtractMin() {

        TYearMonth test = TYearMonth.of(TYear.MAX_VALUE, 12);
        test.minusMonths(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooSmall() {

        TYearMonth test = TYearMonth.of(TYear.MIN_VALUE, 1);
        test.minusMonths(1);
    }

    @Test
    public void test_adjustDate() {

        TYearMonth test = TYearMonth.of(2008, 6);
        TLocalDate date = TLocalDate.of(2007, 1, 1);
        assertEquals(test.adjustInto(date), TLocalDate.of(2008, 6, 1));
    }

    @Test
    public void test_adjustDate_preserveDoM() {

        TYearMonth test = TYearMonth.of(2011, 3);
        TLocalDate date = TLocalDate.of(2008, 2, 29);
        assertEquals(test.adjustInto(date), TLocalDate.of(2011, 3, 29));
    }

    @Test
    public void test_adjustDate_resolve() {

        TYearMonth test = TYearMonth.of(2007, 2);
        TLocalDate date = TLocalDate.of(2008, 3, 31);
        assertEquals(test.adjustInto(date), TLocalDate.of(2007, 2, 28));
    }

    @Test
    public void test_adjustDate_equal() {

        TYearMonth test = TYearMonth.of(2008, 6);
        TLocalDate date = TLocalDate.of(2008, 6, 30);
        assertEquals(test.adjustInto(date), date);
    }

    @Test(expected = NullPointerException.class)
    public void test_adjustDate_null() {

        this.TEST_2008_06.adjustInto((TLocalDate) null);
    }

    @Test
    public void test_isLeapYear() {

        assertEquals(TYearMonth.of(2007, 6).isLeapYear(), false);
        assertEquals(TYearMonth.of(2008, 6).isLeapYear(), true);
    }

    @Test
    public void test_lengthOfMonth_june() {

        TYearMonth test = TYearMonth.of(2007, 6);
        assertEquals(test.lengthOfMonth(), 30);
    }

    @Test
    public void test_lengthOfMonth_febNonLeap() {

        TYearMonth test = TYearMonth.of(2007, 2);
        assertEquals(test.lengthOfMonth(), 28);
    }

    @Test
    public void test_lengthOfMonth_febLeap() {

        TYearMonth test = TYearMonth.of(2008, 2);
        assertEquals(test.lengthOfMonth(), 29);
    }

    @Test
    public void test_lengthOfYear() {

        assertEquals(TYearMonth.of(2007, 6).lengthOfYear(), 365);
        assertEquals(TYearMonth.of(2008, 6).lengthOfYear(), 366);
    }

    @Test
    public void test_isValidDay_int_june() {

        TYearMonth test = TYearMonth.of(2007, 6);
        assertEquals(test.isValidDay(1), true);
        assertEquals(test.isValidDay(30), true);

        assertEquals(test.isValidDay(-1), false);
        assertEquals(test.isValidDay(0), false);
        assertEquals(test.isValidDay(31), false);
        assertEquals(test.isValidDay(32), false);
    }

    @Test
    public void test_isValidDay_int_febNonLeap() {

        TYearMonth test = TYearMonth.of(2007, 2);
        assertEquals(test.isValidDay(1), true);
        assertEquals(test.isValidDay(28), true);

        assertEquals(test.isValidDay(-1), false);
        assertEquals(test.isValidDay(0), false);
        assertEquals(test.isValidDay(29), false);
        assertEquals(test.isValidDay(32), false);
    }

    @Test
    public void test_isValidDay_int_febLeap() {

        TYearMonth test = TYearMonth.of(2008, 2);
        assertEquals(test.isValidDay(1), true);
        assertEquals(test.isValidDay(29), true);

        assertEquals(test.isValidDay(-1), false);
        assertEquals(test.isValidDay(0), false);
        assertEquals(test.isValidDay(30), false);
        assertEquals(test.isValidDay(32), false);
    }

    @Test
    public void test_atDay_int() {

        TYearMonth test = TYearMonth.of(2008, 6);
        assertEquals(test.atDay(30), TLocalDate.of(2008, 6, 30));
    }

    @Test(expected = TDateTimeException.class)
    public void test_atDay_int_invalidDay() {

        TYearMonth test = TYearMonth.of(2008, 6);
        test.atDay(31);
    }

    @Test
    public void test_query() {

        assertEquals(this.TEST_2008_06.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.localDate()), null);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.localTime()), null);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.offset()), null);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.precision()), TChronoUnit.MONTHS);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.zone()), null);
        assertEquals(this.TEST_2008_06.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        this.TEST_2008_06.query(null);
    }

    @Test
    public void test_comparisons() {

        doTest_comparisons_YearMonth(TYearMonth.of(-1, 1), TYearMonth.of(0, 1), TYearMonth.of(0, 12),
                TYearMonth.of(1, 1), TYearMonth.of(1, 2), TYearMonth.of(1, 12), TYearMonth.of(2008, 1),
                TYearMonth.of(2008, 6), TYearMonth.of(2008, 12));
    }

    void doTest_comparisons_YearMonth(TYearMonth... localDates) {

        for (int i = 0; i < localDates.length; i++) {
            TYearMonth a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                TYearMonth b = localDates[j];
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

        this.TEST_2008_06.compareTo(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isBefore_ObjectNull() {

        this.TEST_2008_06.isBefore(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isAfter_ObjectNull() {

        this.TEST_2008_06.isAfter(null);
    }

    @Test
    public void test_equals() {

        TYearMonth a = TYearMonth.of(2008, 6);
        TYearMonth b = TYearMonth.of(2008, 6);
        TYearMonth c = TYearMonth.of(2007, 6);
        TYearMonth d = TYearMonth.of(2008, 5);

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

        assertEquals(this.TEST_2008_06.equals(this.TEST_2008_06), true);
    }

    @Test
    public void test_equals_string_false() {

        assertEquals(this.TEST_2008_06.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {

        assertEquals(this.TEST_2008_06.equals(null), false);
    }

    Object[][] provider_sampleDates() {

        return new Object[][] { { 2008, 1 }, { 2008, 2 }, { -1, 3 }, { 0, 12 }, };
    }

    @Test
    public void test_hashCode() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];

            TYearMonth a = TYearMonth.of(y, m);
            assertEquals(a.hashCode(), a.hashCode());
            TYearMonth b = TYearMonth.of(y, m);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    @Test
    public void test_hashCode_unique() {

        Set<Integer> uniques = new HashSet<Integer>(201 * 12);
        for (int i = 1900; i <= 2100; i++) {
            for (int j = 1; j <= 12; j++) {
                assertTrue(uniques.add(TYearMonth.of(i, j).hashCode()));
            }
        }
    }

    Object[][] provider_sampleToString() {

        return new Object[][] { { 2008, 1, "2008-01" }, { 2008, 12, "2008-12" }, { 7, 5, "0007-05" },
        { 0, 5, "0000-05" }, { -1, 1, "-0001-01" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : provider_sampleToString()) {
            int y = (int) data[0];
            int m = (int) data[1];
            String expected = (String) data[2];

            TYearMonth test = TYearMonth.of(y, m);
            String str = test.toString();
            assertEquals(str, expected);
        }
    }

    @Test
    public void test_format_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M");
        String t = TYearMonth.of(2010, 12).format(f);
        assertEquals(t, "2010 12");
    }

    @Test(expected = NullPointerException.class)
    public void test_format_formatter_null() {

        TYearMonth.of(2010, 12).format(null);
    }

}
