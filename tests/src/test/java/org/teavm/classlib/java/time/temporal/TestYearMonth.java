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

import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
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
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
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
 * Test YearMonth.
 */
@TeaVMTest
public class TestYearMonth extends AbstractDateTimeTest {

    private YearMonth test2008x06;

    @BeforeEach
    public void setUp() {
        test2008x06 = YearMonth.of(2008, 6);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test2008x06, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            MONTH_OF_YEAR,
            PROLEPTIC_MONTH,
            YEAR_OF_ERA,
            YEAR,
            ERA,
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
    void check(YearMonth test, int y, int m) {
        assertEquals(y, test.getYear());
        assertEquals(m, test.getMonth().getValue());
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        YearMonth expected = YearMonth.now(Clock.systemDefaultZone());
        YearMonth test = YearMonth.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = YearMonth.now(Clock.systemDefaultZone());
            test = YearMonth.now();
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void now_ZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> YearMonth.now((ZoneId) null));
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        YearMonth expected = YearMonth.now(Clock.system(zone));
        YearMonth test = YearMonth.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = YearMonth.now(Clock.system(zone));
            test = YearMonth.now(zone);
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
        YearMonth test = YearMonth.now(clock);
        assertEquals(2010, test.getYear());
        assertEquals(Month.DECEMBER, test.getMonth());
    }

    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> YearMonth.now((Clock) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsMonth() {
        YearMonth test = YearMonth.of(2008, Month.FEBRUARY);
        check(test, 2008, 2);
    }

    @Test
    public void test_factory_intsMonth_yearTooLow() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(Year.MIN_VALUE - 1, Month.JANUARY));
    }

    @Test
    public void test_factory_intsMonth_dayTooHigh() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(Year.MAX_VALUE + 1, Month.JANUARY));
    }

    @Test
    public void factory_intsMonth_nullMonth() {
        assertThrows(NullPointerException.class, () -> YearMonth.of(2008, null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ints() {
        YearMonth test = YearMonth.of(2008, 2);
        check(test, 2008, 2);
    }

    @Test
    public void test_factory_ints_yearTooLow() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(Year.MIN_VALUE - 1, 2));
    }

    @Test
    public void test_factory_ints_dayTooHigh() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(Year.MAX_VALUE + 1, 2));
    }

    @Test
    public void test_factory_ints_monthTooLow() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(2008, 0));
    }

    @Test
    public void test_factory_ints_monthTooHigh() {
        assertThrows(DateTimeException.class, () -> YearMonth.of(2008, 13));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(YearMonth.of(2007, 7), YearMonth.from(LocalDate.of(2007, 7, 15)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> YearMonth.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> YearMonth.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    static Object[][] provider_goodParseData() {
        return new Object[][] {
                {"0000-01", YearMonth.of(0, 1)},
                {"0000-12", YearMonth.of(0, 12)},
                {"9999-12", YearMonth.of(9999, 12)},
                {"2000-01", YearMonth.of(2000, 1)},
                {"2000-02", YearMonth.of(2000, 2)},
                {"2000-03", YearMonth.of(2000, 3)},
                {"2000-04", YearMonth.of(2000, 4)},
                {"2000-05", YearMonth.of(2000, 5)},
                {"2000-06", YearMonth.of(2000, 6)},
                {"2000-07", YearMonth.of(2000, 7)},
                {"2000-08", YearMonth.of(2000, 8)},
                {"2000-09", YearMonth.of(2000, 9)},
                {"2000-10", YearMonth.of(2000, 10)},
                {"2000-11", YearMonth.of(2000, 11)},
                {"2000-12", YearMonth.of(2000, 12)},

                {"+12345678-03", YearMonth.of(12345678, 3)},
                {"+123456-03", YearMonth.of(123456, 3)},
                {"0000-03", YearMonth.of(0, 3)},
                {"-1234-03", YearMonth.of(-1234, 3)},
                {"-12345678-03", YearMonth.of(-12345678, 3)},

                {"+" + Year.MAX_VALUE + "-03", YearMonth.of(Year.MAX_VALUE, 3)},
                {Year.MIN_VALUE + "-03", YearMonth.of(Year.MIN_VALUE, 3)},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_goodParseData")
    public void factory_parse_success(String text, YearMonth expected) {
        YearMonth yearMonth = YearMonth.parse(text);
        assertEquals(expected, yearMonth);
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_badParseData() {
        return new Object[][] {
                {"", 0},
                {"-00", 1},
                {"--01-0", 1},
                {"A01-3", 0},
                {"200-01", 0},
                {"2009/12", 4},

                {"-0000-10", 0},
                {"-12345678901-10", 11},
                {"+1-10", 1},
                {"+12-10", 1},
                {"+123-10", 1},
                {"+1234-10", 0},
                {"12345-10", 0},
                {"+12345678901-10", 11},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_badParseData")
    public void factory_parse_fail(String text, int pos) {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                YearMonth.parse(text);
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
    public void factory_parse_illegalValue_Month() {
        assertThrows(DateTimeParseException.class, () -> YearMonth.parse("2008-13"));
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> YearMonth.parse(null));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M");
        YearMonth test = YearMonth.parse("2010 12", f);
        assertEquals(YearMonth.of(2010, 12), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M");
        assertThrows(NullPointerException.class, () -> YearMonth.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> YearMonth.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(2008, test2008x06.get(YEAR));
        assertEquals(6, test2008x06.get(MONTH_OF_YEAR));
        assertEquals(2008, test2008x06.get(YEAR_OF_ERA));
        assertEquals(1, test2008x06.get(ERA));
    }

    @Test
    public void test_get_TemporalField_tooBig() {
        assertThrows(DateTimeException.class, () -> test2008x06.get(PROLEPTIC_MONTH));
    }

    @Test
    public void test_get_TemporalField_null() {
        assertThrows(NullPointerException.class, () -> test2008x06.get((TemporalField) null));
    }

    @Test
    public void test_get_TemporalField_invalidField() {
        assertThrows(DateTimeException.class, () -> test2008x06.get(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_get_TemporalField_timeField() {
        assertThrows(DateTimeException.class, () -> test2008x06.get(ChronoField.AMPM_OF_DAY));
    }

    //-----------------------------------------------------------------------
    // getLong(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong_TemporalField() {
        assertEquals(2008, test2008x06.getLong(YEAR));
        assertEquals(6, test2008x06.getLong(MONTH_OF_YEAR));
        assertEquals(2008, test2008x06.getLong(YEAR_OF_ERA));
        assertEquals(1, test2008x06.getLong(ERA));
        assertEquals(2008 * 12 + 6 - 1, test2008x06.getLong(PROLEPTIC_MONTH));
    }

    @Test
    public void test_getLong_TemporalField_null() {
        assertThrows(NullPointerException.class, () -> test2008x06.getLong((TemporalField) null));
    }

    @Test
    public void test_getLong_TemporalField_invalidField() {
        assertThrows(DateTimeException.class, () -> test2008x06.getLong(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_getLong_TemporalField_timeField() {
        assertThrows(DateTimeException.class, () -> test2008x06.getLong(ChronoField.AMPM_OF_DAY));
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleDates() {
        return new Object[][] {
            {2008, 1},
            {2008, 2},
            {-1, 3},
            {0, 12},
        };
    }

    //-----------------------------------------------------------------------
    // with(Year)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_Year() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2000, 6), test.with(Year.of(2000)));
    }

    @Test
    public void test_with_Year_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.with(Year.of(2008)));
    }

    @Test
    public void test_with_Year_null() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(NullPointerException.class, () -> test.with((Year) null));
    }

    //-----------------------------------------------------------------------
    // with(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_Month() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 1), test.with(Month.JANUARY));
    }

    @Test
    public void test_with_Month_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.with(Month.JUNE));
    }

    @Test
    public void test_with_Month_null() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(NullPointerException.class, () -> test.with((Month) null));
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(1999, 6), test.withYear(1999));
    }

    @Test
    public void test_withYear_int_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.withYear(2008));
    }

    @Test
    public void test_withYear_tooLow() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(DateTimeException.class, () -> test.withYear(Year.MIN_VALUE - 1));
    }

    @Test
    public void test_withYear_tooHigh() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(DateTimeException.class, () -> test.withYear(Year.MAX_VALUE + 1));
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 1), test.withMonth(1));
    }

    @Test
    public void test_withMonth_int_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.withMonth(6));
    }

    @Test
    public void test_withMonth_tooLow() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(DateTimeException.class, () -> test.withMonth(0));
    }

    @Test
    public void test_withMonth_tooHigh() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(DateTimeException.class, () -> test.withMonth(13));
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears_long() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2009, 6), test.plusYears(1));
    }

    @Test
    public void test_plusYears_long_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.plusYears(0));
    }

    @Test
    public void test_plusYears_long_negative() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2007, 6), test.plusYears(-1));
    }

    @Test
    public void test_plusYears_long_big() {
        YearMonth test = YearMonth.of(-40, 6);
        assertEquals(YearMonth.of((int) (-40L + 20L + Year.MAX_VALUE), 6), test.plusYears(20L + Year.MAX_VALUE));
    }

    @Test
    public void test_plusYears_long_invalidTooLarge() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 6);
        assertThrows(DateTimeException.class, () -> test.plusYears(1));
    }

    @Test
    public void test_plusYears_long_invalidTooLargeMaxAddMax() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.plusYears(Long.MAX_VALUE));
    }

    @Test
    public void test_plusYears_long_invalidTooLargeMaxAddMin() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.plusYears(Long.MIN_VALUE));
    }

    @Test
    public void test_plusYears_long_invalidTooSmall() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 6);
        assertThrows(DateTimeException.class, () -> test.plusYears(-1));
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths_long() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 7), test.plusMonths(1));
    }

    @Test
    public void test_plusMonths_long_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.plusMonths(0));
    }

    @Test
    public void test_plusMonths_long_overYears() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2009, 1), test.plusMonths(7));
    }

    @Test
    public void test_plusMonths_long_negative() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 5), test.plusMonths(-1));
    }

    @Test
    public void test_plusMonths_long_negativeOverYear() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2007, 12), test.plusMonths(-6));
    }

    @Test
    public void test_plusMonths_long_big() {
        YearMonth test = YearMonth.of(-40, 6);
        long months = 20L + Integer.MAX_VALUE;
        assertEquals(YearMonth.of((int) (-40L + months / 12), 6 + (int) (months % 12)), test.plusMonths(months));
    }

    @Test
    public void test_plusMonths_long_invalidTooLarge() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.plusMonths(1));
    }

    @Test
    public void test_plusMonths_long_invalidTooLargeMaxAddMax() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.plusMonths(Long.MAX_VALUE));
    }

    @Test
    public void test_plusMonths_long_invalidTooLargeMaxAddMin() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.plusMonths(Long.MIN_VALUE));
    }

    @Test
    public void test_plusMonths_long_invalidTooSmall() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 1);
        assertThrows(DateTimeException.class, () -> test.plusMonths(-1));
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears_long() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2007, 6), test.minusYears(1));
    }

    @Test
    public void test_minusYears_long_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.minusYears(0));
    }

    @Test
    public void test_minusYears_long_negative() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2009, 6), test.minusYears(-1));
    }

    @Test
    public void test_minusYears_long_big() {
        YearMonth test = YearMonth.of(40, 6);
        assertEquals(YearMonth.of((int) (40L - 20L - Year.MAX_VALUE), 6), test.minusYears(20L + Year.MAX_VALUE));
    }

    @Test
    public void test_minusYears_long_invalidTooLarge() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 6);
        assertThrows(DateTimeException.class, () -> test.minusYears(-1));
    }

    @Test
    public void test_minusYears_long_invalidTooLargeMaxSubtractMax() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.minusYears(Long.MAX_VALUE));
    }

    @Test
    public void test_minusYears_long_invalidTooLargeMaxSubtractMin() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.minusYears(Long.MIN_VALUE));
    }

    @Test
    public void test_minusYears_long_invalidTooSmall() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 6);
        assertThrows(DateTimeException.class, () -> test.minusYears(1));
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths_long() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 5), test.minusMonths(1));
    }

    @Test
    public void test_minusMonths_long_noChange_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(test, test.minusMonths(0));
    }

    @Test
    public void test_minusMonths_long_overYears() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2007, 12), test.minusMonths(6));
    }

    @Test
    public void test_minusMonths_long_negative() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2008, 7), test.minusMonths(-1));
    }

    @Test
    public void test_minusMonths_long_negativeOverYear() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(YearMonth.of(2009, 1), test.minusMonths(-7));
    }

    @Test
    public void test_minusMonths_long_big() {
        YearMonth test = YearMonth.of(40, 6);
        long months = 20L + Integer.MAX_VALUE;
        assertEquals(YearMonth.of((int) (40L - months / 12), 6 - (int) (months % 12)), test.minusMonths(months));
    }

    @Test
    public void test_minusMonths_long_invalidTooLarge() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.minusMonths(-1));
    }

    @Test
    public void test_minusMonths_long_invalidTooLargeMaxSubtractMax() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.minusMonths(Long.MAX_VALUE));
    }

    @Test
    public void test_minusMonths_long_invalidTooLargeMaxSubtractMin() {
        YearMonth test = YearMonth.of(Year.MAX_VALUE, 12);
        assertThrows(DateTimeException.class, () -> test.minusMonths(Long.MIN_VALUE));
    }

    @Test
    public void test_minusMonths_long_invalidTooSmall() {
        YearMonth test = YearMonth.of(Year.MIN_VALUE, 1);
        assertThrows(DateTimeException.class, () -> test.minusMonths(1));
    }

    //-----------------------------------------------------------------------
    // doAdjustment()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustDate() {
        YearMonth test = YearMonth.of(2008, 6);
        LocalDate date = LocalDate.of(2007, 1, 1);
        assertEquals(LocalDate.of(2008, 6, 1), test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_preserveDoM() {
        YearMonth test = YearMonth.of(2011, 3);
        LocalDate date = LocalDate.of(2008, 2, 29);
        assertEquals(LocalDate.of(2011, 3, 29), test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_resolve() {
        YearMonth test = YearMonth.of(2007, 2);
        LocalDate date = LocalDate.of(2008, 3, 31);
        assertEquals(LocalDate.of(2007, 2, 28), test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_equal() {
        YearMonth test = YearMonth.of(2008, 6);
        LocalDate date = LocalDate.of(2008, 6, 30);
        assertEquals(date, test.adjustInto(date));
    }

    @Test
    public void test_adjustDate_null() {
        assertThrows(NullPointerException.class, () -> test2008x06.adjustInto((LocalDate) null));
    }

    //-----------------------------------------------------------------------
    // isLeapYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_isLeapYear() {
        assertFalse(YearMonth.of(2007, 6).isLeapYear());
        assertTrue(YearMonth.of(2008, 6).isLeapYear());
    }

    //-----------------------------------------------------------------------
    // lengthOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_lengthOfMonth_june() {
        YearMonth test = YearMonth.of(2007, 6);
        assertEquals(30, test.lengthOfMonth());
    }

    @Test
    public void test_lengthOfMonth_febNonLeap() {
        YearMonth test = YearMonth.of(2007, 2);
        assertEquals(28, test.lengthOfMonth());
    }

    @Test
    public void test_lengthOfMonth_febLeap() {
        YearMonth test = YearMonth.of(2008, 2);
        assertEquals(29, test.lengthOfMonth());
    }

    //-----------------------------------------------------------------------
    // lengthOfYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_lengthOfYear() {
        assertEquals(365, YearMonth.of(2007, 6).lengthOfYear());
        assertEquals(366, YearMonth.of(2008, 6).lengthOfYear());
    }

    //-----------------------------------------------------------------------
    // isValidDay(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidDay_int_june() {
        YearMonth test = YearMonth.of(2007, 6);
        assertTrue(test.isValidDay(1));
        assertTrue(test.isValidDay(30));

        assertFalse(test.isValidDay(-1));
        assertFalse(test.isValidDay(0));
        assertFalse(test.isValidDay(31));
        assertFalse(test.isValidDay(32));
    }

    @Test
    public void test_isValidDay_int_febNonLeap() {
        YearMonth test = YearMonth.of(2007, 2);
        assertTrue(test.isValidDay(1));
        assertTrue(test.isValidDay(28));

        assertFalse(test.isValidDay(-1));
        assertFalse(test.isValidDay(0));
        assertFalse(test.isValidDay(29));
        assertFalse(test.isValidDay(32));
    }

    @Test
    public void test_isValidDay_int_febLeap() {
        YearMonth test = YearMonth.of(2008, 2);
        assertTrue(test.isValidDay(1));
        assertTrue(test.isValidDay(29));

        assertFalse(test.isValidDay(-1));
        assertFalse(test.isValidDay(0));
        assertFalse(test.isValidDay(30));
        assertFalse(test.isValidDay(32));
    }

    //-----------------------------------------------------------------------
    // atDay(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atDay_int() {
        YearMonth test = YearMonth.of(2008, 6);
        assertEquals(LocalDate.of(2008, 6, 30), test.atDay(30));
    }

    @Test
    public void test_atDay_int_invalidDay() {
        YearMonth test = YearMonth.of(2008, 6);
        assertThrows(DateTimeException.class, () -> test.atDay(31));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, test2008x06.query(TemporalQueries.chronology()));
        assertEquals(null, test2008x06.query(TemporalQueries.localDate()));
        assertEquals(null, test2008x06.query(TemporalQueries.localTime()));
        assertEquals(null, test2008x06.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.MONTHS, test2008x06.query(TemporalQueries.precision()));
        assertEquals(null, test2008x06.query(TemporalQueries.zone()));
        assertEquals(null, test2008x06.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test2008x06.query(null));
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_YearMonth(
            YearMonth.of(-1, 1),
            YearMonth.of(0, 1),
            YearMonth.of(0, 12),
            YearMonth.of(1, 1),
            YearMonth.of(1, 2),
            YearMonth.of(1, 12),
            YearMonth.of(2008, 1),
            YearMonth.of(2008, 6),
            YearMonth.of(2008, 12)
        );
    }

    void doTest_comparisons_YearMonth(YearMonth... localDates) {
        for (int i = 0; i < localDates.length; i++) {
            YearMonth a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                YearMonth b = localDates[j];
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
        assertThrows(NullPointerException.class, () -> test2008x06.compareTo(null));
    }

    @Test
    public void test_isBefore_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test2008x06.isBefore(null));
    }

    @Test
    public void test_isAfter_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test2008x06.isAfter(null));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        YearMonth a = YearMonth.of(2008, 6);
        YearMonth b = YearMonth.of(2008, 6);
        YearMonth c = YearMonth.of(2007, 6);
        YearMonth d = YearMonth.of(2008, 5);

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
        assertTrue(test2008x06.equals(test2008x06));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(test2008x06.equals("2007-07-15"));
    }

    @Test
    public void test_equals_null_false() {
        assertFalse(test2008x06.equals(null));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleDates")
    public void test_hashCode(int y, int m) {
        YearMonth a = YearMonth.of(y, m);
        assertEquals(a.hashCode(), a.hashCode());
        YearMonth b = YearMonth.of(y, m);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_hashCode_unique() {
        Set<Integer> uniques = new HashSet<Integer>(201 * 12);
        for (int i = 1900; i <= 2100; i++) {
            for (int j = 1; j <= 12; j++) {
                assertTrue(uniques.add(YearMonth.of(i, j).hashCode()));
            }
        }
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleToString() {
        return new Object[][] {
            {2008, 1, "2008-01"},
            {2008, 12, "2008-12"},
            {7, 5, "0007-05"},
            {0, 5, "0000-05"},
            {-1, 1, "-0001-01"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int y, int m, String expected) {
        YearMonth test = YearMonth.of(y, m);
        String str = test.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M");
        String t = YearMonth.of(2010, 12).format(f);
        assertEquals("2010 12", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class, () -> YearMonth.of(2010, 12).format(null));
    }

}
