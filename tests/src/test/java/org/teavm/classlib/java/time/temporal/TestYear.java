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
import java.time.MonthDay;
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
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.classlib.java.time.AbstractDateTimeTest;
import org.teavm.junit.TeaVMTest;

/**
 * Test Year.
 */
@TeaVMTest
public class TestYear extends AbstractDateTimeTest {

    private static final Year TEST_2008 = Year.of(2008);

    @BeforeEach
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {TEST_2008, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
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
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        Year expected = Year.now(Clock.systemDefaultZone());
        Year test = Year.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = Year.now(Clock.systemDefaultZone());
            test = Year.now();
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void now_ZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> Year.now((ZoneId) null));
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        Year expected = Year.now(Clock.system(zone));
        Year test = Year.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = Year.now(Clock.system(zone));
            test = Year.now(zone);
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
        Year test = Year.now(clock);
        assertEquals(2010, test.getValue());
    }

    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> Year.now((Clock) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_singleton() {
        for (int i = -4; i <= 2104; i++) {
            Year test = Year.of(i);
            assertEquals(i, test.getValue());
            assertEquals(test, Year.of(i));
        }
    }

    @Test
    public void test_factory_int_tooLow() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MIN_VALUE - 1));
    }

    @Test
    public void test_factory_int_tooHigh() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MAX_VALUE + 1));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(Year.of(2007), Year.from(LocalDate.of(2007, 7, 15)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> Year.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> Year.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    static Object[][] provider_goodParseData() {
        return new Object[][] {
                {"0000", Year.of(0)},
                {"9999", Year.of(9999)},
                {"2000", Year.of(2000)},

                {"+12345678", Year.of(12345678)},
                {"+123456", Year.of(123456)},
                {"-1234", Year.of(-1234)},
                {"-12345678", Year.of(-12345678)},

                {"+" + Year.MAX_VALUE, Year.of(Year.MAX_VALUE)},
                {"" + Year.MIN_VALUE, Year.of(Year.MIN_VALUE)},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_goodParseData")
    public void factory_parse_success(String text, Year expected) {
        Year year = Year.parse(text);
        assertEquals(expected, year);
    }

    static Object[][] provider_badParseData() {
        return new Object[][] {
                {"", 0},
                {"-00", 1},
                {"--01-0", 1},
                {"A01", 0},
                {"200", 0},
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
    @Disabled("Error indexes differ from those reported by JDK")
    public void factory_parse_fail(String text, int pos) {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                Year.parse(text);
                fail(String.format("Parse should have failed for %s at position %d", text, pos));
            } catch (DateTimeParseException ex) {
                assertEquals(text, ex.getParsedString());
                assertEquals(pos, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> Year.parse(null));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u");
        Year test = Year.parse("2010", f);
        assertEquals(Year.of(2010), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u");
        assertThrows(NullPointerException.class, () -> Year.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> Year.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // get(DateTimeField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_DateTimeField() {
        assertEquals(2008, TEST_2008.getLong(ChronoField.YEAR));
        assertEquals(2008, TEST_2008.getLong(ChronoField.YEAR_OF_ERA));
        assertEquals(1, TEST_2008.getLong(ChronoField.ERA));
    }

    @Test
    public void test_get_DateTimeField_null() {
        assertThrows(NullPointerException.class, () -> TEST_2008.getLong((TemporalField) null));
    }

    @Test
    public void test_get_DateTimeField_invalidField() {
        assertThrows(DateTimeException.class, () -> TEST_2008.getLong(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_get_DateTimeField_timeField() {
        assertThrows(DateTimeException.class, () -> TEST_2008.getLong(ChronoField.AMPM_OF_DAY));
    }

    //-----------------------------------------------------------------------
    // isLeap()
    //-----------------------------------------------------------------------
    @Test
    public void test_isLeap() {
        assertFalse(Year.of(1999).isLeap());
        assertTrue(Year.of(2000).isLeap());
        assertFalse(Year.of(2001).isLeap());

        assertFalse(Year.of(2007).isLeap());
        assertTrue(Year.of(2008).isLeap());
        assertFalse(Year.of(2009).isLeap());
        assertFalse(Year.of(2010).isLeap());
        assertFalse(Year.of(2011).isLeap());
        assertTrue(Year.of(2012).isLeap());

        assertFalse(Year.of(2095).isLeap());
        assertTrue(Year.of(2096).isLeap());
        assertFalse(Year.of(2097).isLeap());
        assertFalse(Year.of(2098).isLeap());
        assertFalse(Year.of(2099).isLeap());
        assertFalse(Year.of(2100).isLeap());
        assertFalse(Year.of(2101).isLeap());
        assertFalse(Year.of(2102).isLeap());
        assertFalse(Year.of(2103).isLeap());
        assertTrue(Year.of(2104).isLeap());
        assertFalse(Year.of(2105).isLeap());

        assertFalse(Year.of(-500).isLeap());
        assertTrue(Year.of(-400).isLeap());
        assertFalse(Year.of(-300).isLeap());
        assertFalse(Year.of(-200).isLeap());
        assertFalse(Year.of(-100).isLeap());
        assertTrue(Year.of(0).isLeap());
        assertFalse(Year.of(100).isLeap());
        assertFalse(Year.of(200).isLeap());
        assertFalse(Year.of(300).isLeap());
        assertTrue(Year.of(400).isLeap());
        assertFalse(Year.of(500).isLeap());
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        assertEquals(Year.of(2006), Year.of(2007).plusYears(-1));
        assertEquals(Year.of(2007), Year.of(2007).plusYears(0));
        assertEquals(Year.of(2008), Year.of(2007).plusYears(1));
        assertEquals(Year.of(2009), Year.of(2007).plusYears(2));

        assertEquals(Year.of(Year.MAX_VALUE), Year.of(Year.MAX_VALUE - 1).plusYears(1));
        assertEquals(Year.of(Year.MAX_VALUE), Year.of(Year.MAX_VALUE).plusYears(0));

        assertEquals(Year.of(Year.MIN_VALUE), Year.of(Year.MIN_VALUE + 1).plusYears(-1));
        assertEquals(Year.of(Year.MIN_VALUE), Year.of(Year.MIN_VALUE).plusYears(0));
    }

    @Test
    public void test_plusYear_zero_equals() {
        Year base = Year.of(2007);
        assertEquals(base, base.plusYears(0));
    }

    @Test
    public void test_plusYears_big() {
        long years = 20L + Year.MAX_VALUE;
        assertEquals(Year.of((int) (-40L + years)), Year.of(-40).plusYears(years));
    }

    @Test
    public void test_plusYears_max() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MAX_VALUE).plusYears(1));
    }

    @Test
    public void test_plusYears_maxLots() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MAX_VALUE).plusYears(1000));
    }

    @Test
    public void test_plusYears_min() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MIN_VALUE).plusYears(-1));
    }

    @Test
    public void test_plusYears_minLots() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MIN_VALUE).plusYears(-1000));
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        assertEquals(Year.of(2008), Year.of(2007).minusYears(-1));
        assertEquals(Year.of(2007), Year.of(2007).minusYears(0));
        assertEquals(Year.of(2006), Year.of(2007).minusYears(1));
        assertEquals(Year.of(2005), Year.of(2007).minusYears(2));

        assertEquals(Year.of(Year.MAX_VALUE), Year.of(Year.MAX_VALUE - 1).minusYears(-1));
        assertEquals(Year.of(Year.MAX_VALUE), Year.of(Year.MAX_VALUE).minusYears(0));

        assertEquals(Year.of(Year.MIN_VALUE), Year.of(Year.MIN_VALUE + 1).minusYears(1));
        assertEquals(Year.of(Year.MIN_VALUE), Year.of(Year.MIN_VALUE).minusYears(0));
    }

    @Test
    public void test_minusYear_zero_equals() {
        Year base = Year.of(2007);
        assertEquals(base, base.minusYears(0));
    }

    @Test
    public void test_minusYears_big() {
        long years = 20L + Year.MAX_VALUE;
        assertEquals(Year.of((int) (40L - years)), Year.of(40).minusYears(years));
    }

    @Test
    public void test_minusYears_max() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MAX_VALUE).minusYears(-1));
    }

    @Test
    public void test_minusYears_maxLots() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MAX_VALUE).minusYears(-1000));
    }

    @Test
    public void test_minusYears_min() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MIN_VALUE).minusYears(1));
    }

    @Test
    public void test_minusYears_minLots() {
        assertThrows(DateTimeException.class, () -> Year.of(Year.MIN_VALUE).minusYears(1000));
    }

    //-----------------------------------------------------------------------
    // doAdjustment()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustDate() {
        LocalDate base = LocalDate.of(2007, 2, 12);
        for (int i = -4; i <= 2104; i++) {
            Temporal result = Year.of(i).adjustInto(base);
            assertEquals(LocalDate.of(i, 2, 12), result);
        }
    }

    @Test
    public void test_adjustDate_resolve() {
        Year test = Year.of(2011);
        assertEquals(LocalDate.of(2011, 2, 28), test.adjustInto(LocalDate.of(2012, 2, 29)));
    }

    @Test
    public void test_adjustDate_nullLocalDate() {
        Year test = Year.of(1);
        assertThrows(NullPointerException.class, () -> test.adjustInto((LocalDate) null));
    }

    //-----------------------------------------------------------------------
    // length()
    //-----------------------------------------------------------------------
    @Test
    public void test_length() {
        assertEquals(365, Year.of(1999).length());
        assertEquals(366, Year.of(2000).length());
        assertEquals(365, Year.of(2001).length());

        assertEquals(365, Year.of(2007).length());
        assertEquals(366, Year.of(2008).length());
        assertEquals(365, Year.of(2009).length());
        assertEquals(365, Year.of(2010).length());
        assertEquals(365, Year.of(2011).length());
        assertEquals(366, Year.of(2012).length());

        assertEquals(365, Year.of(2095).length());
        assertEquals(366, Year.of(2096).length());
        assertEquals(365, Year.of(2097).length());
        assertEquals(365, Year.of(2098).length());
        assertEquals(365, Year.of(2099).length());
        assertEquals(365, Year.of(2100).length());
        assertEquals(365, Year.of(2101).length());
        assertEquals(365, Year.of(2102).length());
        assertEquals(365, Year.of(2103).length());
        assertEquals(366, Year.of(2104).length());
        assertEquals(365, Year.of(2105).length());

        assertEquals(365, Year.of(-500).length());
        assertEquals(366, Year.of(-400).length());
        assertEquals(365, Year.of(-300).length());
        assertEquals(365, Year.of(-200).length());
        assertEquals(365, Year.of(-100).length());
        assertEquals(366, Year.of(0).length());
        assertEquals(365, Year.of(100).length());
        assertEquals(365, Year.of(200).length());
        assertEquals(365, Year.of(300).length());
        assertEquals(366, Year.of(400).length());
        assertEquals(365, Year.of(500).length());
    }

    //-----------------------------------------------------------------------
    // isValidMonthDay(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidMonthDay_june() {
        Year test = Year.of(2007);
        MonthDay monthDay = MonthDay.of(6, 30);
        assertTrue(test.isValidMonthDay(monthDay));
    }

    @Test
    public void test_isValidMonthDay_febNonLeap() {
        Year test = Year.of(2007);
        MonthDay monthDay = MonthDay.of(2, 29);
        assertFalse(test.isValidMonthDay(monthDay));
    }

    @Test
    public void test_isValidMonthDay_febLeap() {
        Year test = Year.of(2008);
        MonthDay monthDay = MonthDay.of(2, 29);
        assertTrue(test.isValidMonthDay(monthDay));
    }

    @Test
    public void test_isValidMonthDay_null() {
        Year test = Year.of(2008);
        assertFalse(test.isValidMonthDay(null));
    }

    //-----------------------------------------------------------------------
    // atMonth(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_atMonth() {
        Year test = Year.of(2008);
        assertEquals(YearMonth.of(2008, 6), test.atMonth(Month.JUNE));
    }

    @Test
    public void test_atMonth_nullMonth() {
        Year test = Year.of(2008);
        assertThrows(NullPointerException.class, () -> test.atMonth((Month) null));
    }

    //-----------------------------------------------------------------------
    // atMonth(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atMonth_int() {
        Year test = Year.of(2008);
        assertEquals(YearMonth.of(2008, 6), test.atMonth(6));
    }

    @Test
    public void test_atMonth_int_invalidMonth() {
        Year test = Year.of(2008);
        assertThrows(DateTimeException.class, () -> test.atMonth(13));
    }

    //-----------------------------------------------------------------------
    // atMonthDay(MonthDay)
    //-----------------------------------------------------------------------
    static Object[][] data_atMonthDay() {
        return new Object[][] {
                {Year.of(2008), MonthDay.of(6, 30), LocalDate.of(2008, 6, 30)},
                {Year.of(2008), MonthDay.of(2, 29), LocalDate.of(2008, 2, 29)},
                {Year.of(2009), MonthDay.of(2, 29), LocalDate.of(2009, 2, 28)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_atMonthDay")
    public void test_atMonthDay(Year year, MonthDay monthDay, LocalDate expected) {
        assertEquals(expected, year.atMonthDay(monthDay));
    }

    @Test
    public void test_atMonthDay_nullMonthDay() {
        Year test = Year.of(2008);
        assertThrows(NullPointerException.class, () -> test.atMonthDay((MonthDay) null));
    }

    //-----------------------------------------------------------------------
    // atDay(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atDay_notLeapYear() {
        Year test = Year.of(2007);
        LocalDate expected = LocalDate.of(2007, 1, 1);
        for (int i = 1; i <= 365; i++) {
            assertEquals(expected, test.atDay(i));
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void test_atDay_notLeapYear_day366() {
        Year test = Year.of(2007);
        assertThrows(DateTimeException.class, () -> test.atDay(366));
    }

    @Test
    public void test_atDay_leapYear() {
        Year test = Year.of(2008);
        LocalDate expected = LocalDate.of(2008, 1, 1);
        for (int i = 1; i <= 366; i++) {
            assertEquals(expected, test.atDay(i));
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void test_atDay_day0() {
        Year test = Year.of(2007);
        assertThrows(DateTimeException.class, () -> test.atDay(0));
    }

    @Test
    public void test_atDay_day367() {
        Year test = Year.of(2007);
        assertThrows(DateTimeException.class, () -> test.atDay(367));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, TEST_2008.query(TemporalQueries.chronology()));
        assertEquals(null, TEST_2008.query(TemporalQueries.localDate()));
        assertEquals(null, TEST_2008.query(TemporalQueries.localTime()));
        assertEquals(null, TEST_2008.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.YEARS, TEST_2008.query(TemporalQueries.precision()));
        assertEquals(null, TEST_2008.query(TemporalQueries.zone()));
        assertEquals(null, TEST_2008.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> TEST_2008.query(null));
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo() {
        for (int i = -4; i <= 2104; i++) {
            Year a = Year.of(i);
            for (int j = -4; j <= 2104; j++) {
                Year b = Year.of(j);
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0);
                    assertTrue(b.compareTo(a) > 0);
                    assertFalse(a.isAfter(b));
                    assertTrue(a.isBefore(b));
                    assertTrue(b.isAfter(a));
                    assertFalse(b.isBefore(a));
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0);
                    assertTrue(b.compareTo(a) < 0);
                    assertTrue(a.isAfter(b));
                    assertFalse(a.isBefore(b));
                    assertFalse(b.isAfter(a));
                    assertTrue(b.isBefore(a));
                } else {
                    assertEquals(0, a.compareTo(b));
                    assertEquals(0, b.compareTo(a));
                    assertFalse(a.isAfter(b));
                    assertFalse(a.isBefore(b));
                    assertFalse(b.isAfter(a));
                    assertFalse(b.isBefore(a));
                }
            }
        }
    }

    @Test
    public void test_compareTo_nullYear() {
        Year doy = null;
        Year test = Year.of(1);
        assertThrows(NullPointerException.class, () -> test.compareTo(doy));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        for (int i = -4; i <= 2104; i++) {
            Year a = Year.of(i);
            for (int j = -4; j <= 2104; j++) {
                Year b = Year.of(j);
                assertEquals(i == j, a.equals(b));
                assertEquals(i == j, a.hashCode() == b.hashCode());
            }
        }
    }

    @Test
    public void test_equals_same() {
        Year test = Year.of(2011);
        assertTrue(test.equals(test));
    }

    @Test
    public void test_equals_nullYear() {
        Year doy = null;
        Year test = Year.of(1);
        assertFalse(test.equals(doy));
    }

    @Test
    public void test_equals_incorrectType() {
        Year test = Year.of(1);
        assertFalse(test.equals("Incorrect type"));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        for (int i = -4; i <= 2104; i++) {
            Year a = Year.of(i);
            assertEquals("" + i, a.toString());
        }
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y");
        String t = Year.of(2010).format(f);
        assertEquals("2010", t);
    }

    @Test
    public void format_formatter_null() {
        assertThrows(NullPointerException.class, () -> Year.of(2010).format(null));
    }

}
