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
package org.teavm.classlib.java.time;

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.junit.TeaVMTest;

/**
 * Test LocalTime.
 */
@TeaVMTest
public class TestLocalTime extends AbstractDateTimeTest {

    private LocalTime test12x30x40x987654321;

    private static final TemporalUnit[] INVALID_UNITS;
    static {
        EnumSet<ChronoUnit> set = EnumSet.range(WEEKS, FOREVER);
        INVALID_UNITS = set.toArray(new TemporalUnit[0]);
    }

    @BeforeEach
    public void setUp() {
        test12x30x40x987654321 = LocalTime.of(12, 30, 40, 987654321);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test12x30x40x987654321, LocalTime.MIN, LocalTime.MAX, LocalTime.MIDNIGHT, 
                LocalTime.NOON};
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            NANO_OF_SECOND,
            NANO_OF_DAY,
            MICRO_OF_SECOND,
            MICRO_OF_DAY,
            MILLI_OF_SECOND,
            MILLI_OF_DAY,
            SECOND_OF_MINUTE,
            SECOND_OF_DAY,
            MINUTE_OF_HOUR,
            MINUTE_OF_DAY,
            CLOCK_HOUR_OF_AMPM,
            HOUR_OF_AMPM,
            CLOCK_HOUR_OF_DAY,
            HOUR_OF_DAY,
            AMPM_OF_DAY,
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
    private void check(LocalTime time, int h, int m, int s, int n) {
        assertEquals(h, time.getHour());
        assertEquals(m, time.getMinute());
        assertEquals(s, time.getSecond());
        assertEquals(n, time.getNano());
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIDNIGHT() {
        check(LocalTime.MIDNIGHT, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIDNIGHT_equal() {
        assertEquals(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
        assertEquals(LocalTime.of(0, 0), LocalTime.MIDNIGHT);
    }

    @Test
    public void constant_MIDDAY() {
        check(LocalTime.NOON, 12, 0, 0, 0);
    }

    @Test
    public void constant_MIDDAY_equal() {
        assertEquals(LocalTime.NOON, LocalTime.NOON);
        assertEquals(LocalTime.of(12, 0), LocalTime.NOON);
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIN_TIME() {
        check(LocalTime.MIN, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIN_TIME_equal() {
        assertEquals(LocalTime.of(0, 0), LocalTime.MIN);
    }

    @Test
    public void constant_MAX_TIME() {
        check(LocalTime.MAX, 23, 59, 59, 999999999);
    }

    @Test
    public void constant_MAX_TIME_equal() {
        assertEquals(LocalTime.NOON, LocalTime.NOON);
        assertEquals(LocalTime.of(12, 0), LocalTime.NOON);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        LocalTime expected = LocalTime.now(Clock.systemDefaultZone());
        LocalTime test = LocalTime.now();
        long diff = Math.abs(test.toNanoOfDay() - expected.toNanoOfDay());
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void now_ZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> LocalTime.now((ZoneId) null));
    }

    @Test
    @Disabled("Flaky: depends on two subsequent clock readings being equal")
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        LocalTime expected = LocalTime.now(Clock.system(zone));
        LocalTime test = LocalTime.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = LocalTime.now(Clock.system(zone));
            test = LocalTime.now(zone);
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> LocalTime.now((Clock) null));
    }

    @Test
    public void now_Clock_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            LocalTime test = LocalTime.now(clock);
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(8, test.getNano());
        }
    }

    @Test
    public void now_Clock_beforeEpoch() {
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            LocalTime test = LocalTime.now(clock);
            assertEquals(((i + 24 * 60 * 60) / (60 * 60)) % 24, test.getHour());
            assertEquals(((i + 24 * 60 * 60) / 60) % 60, test.getMinute());
            assertEquals((i + 24 * 60 * 60) % 60, test.getSecond());
            assertEquals(8, test.getNano());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_max() {
        Clock clock = Clock.fixed(Instant.MAX, ZoneOffset.UTC);
        LocalTime test = LocalTime.now(clock);
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(999999999, test.getNano());
    }

    @Test
    public void now_Clock_min() {
        Clock clock = Clock.fixed(Instant.MIN, ZoneOffset.UTC);
        LocalTime test = LocalTime.now(clock);
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    //-----------------------------------------------------------------------
    // of() factories
    //-----------------------------------------------------------------------
    @Test
    public void factory_time_2ints() {
        LocalTime test = LocalTime.of(12, 30);
        check(test, 12, 30, 0, 0);
    }

    @Test
    public void factory_time_2ints_hourTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(-1, 0));
    }

    @Test
    public void factory_time_2ints_hourTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(24, 0));
    }

    @Test
    public void factory_time_2ints_minuteTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, -1));
    }

    @Test
    public void factory_time_2ints_minuteTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 60));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_time_3ints() {
        LocalTime test = LocalTime.of(12, 30, 40);
        check(test, 12, 30, 40, 0);
    }

    @Test
    public void factory_time_3ints_hourTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(-1, 0, 0));
    }

    @Test
    public void factory_time_3ints_hourTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(24, 0, 0));
    }

    @Test
    public void factory_time_3ints_minuteTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, -1, 0));
    }

    @Test
    public void factory_time_3ints_minuteTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 60, 0));
    }

    @Test
    public void factory_time_3ints_secondTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, -1));
    }

    @Test
    public void factory_time_3ints_secondTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, 60));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_time_4ints() {
        LocalTime test = LocalTime.of(12, 30, 40, 987654321);
        check(test, 12, 30, 40, 987654321);
        test = LocalTime.of(12, 0, 40, 987654321);
        check(test, 12, 0, 40, 987654321);
    }

    @Test
    public void factory_time_4ints_hourTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(-1, 0, 0, 0));
    }

    @Test
    public void factory_time_4ints_hourTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(24, 0, 0, 0));
    }

    @Test
    public void factory_time_4ints_minuteTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, -1, 0, 0));
    }

    @Test
    public void factory_time_4ints_minuteTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 60, 0, 0));
    }

    @Test
    public void factory_time_4ints_secondTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, -1, 0));
    }

    @Test
    public void factory_time_4ints_secondTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, 60, 0));
    }

    @Test
    public void factory_time_4ints_nanoTooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, 0, -1));
    }

    @Test
    public void factory_time_4ints_nanoTooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.of(0, 0, 0, 1000000000));
    }

    //-----------------------------------------------------------------------
    // ofSecondOfDay(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofSecondOfDay() {
        LocalTime localTime = LocalTime.ofSecondOfDay(2 * 60 * 60 + 17 * 60 + 23);
        check(localTime, 2, 17, 23, 0);
    }

    @Test
    public void factory_ofSecondOfDay_tooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.ofSecondOfDay(-1));
    }

    @Test
    public void factory_ofSecondOfDay_tooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.ofSecondOfDay(24 * 60 * 60));
    }

    //-----------------------------------------------------------------------
    // ofNanoOfDay(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofNanoOfDay() {
        LocalTime localTime = LocalTime.ofNanoOfDay(60 * 60 * 1000000000L + 17);
        check(localTime, 1, 0, 0, 17);
    }

    @Test
    public void factory_ofNanoOfDay_tooLow() {
        assertThrows(DateTimeException.class, () -> LocalTime.ofNanoOfDay(-1));
    }

    @Test
    public void factory_ofNanoOfDay_tooHigh() {
        assertThrows(DateTimeException.class, () -> LocalTime.ofNanoOfDay(24 * 60 * 60 * 1000000000L));
    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_DateTimeAccessor() {
        assertEquals(LocalTime.of(17, 30), LocalTime.from(LocalTime.of(17, 30)));
        assertEquals(LocalTime.of(17, 30), LocalTime.from(LocalDateTime.of(2012, 5, 1, 17, 30)));
    }

    @Test
    public void factory_from_DateTimeAccessor_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> LocalTime.from(LocalDate.of(2007, 7, 15)));
    }

    @Test
    public void factory_from_DateTimeAccessor_null() {
        assertThrows(NullPointerException.class, () -> LocalTime.from(null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void factory_parse_validText(int h, int m, int s, int n, String parsable) {
        LocalTime t = LocalTime.parse(parsable);
        assertNotNull(t, parsable);
        assertEquals(h, t.getHour());
        assertEquals(m, t.getMinute());
        assertEquals(s, t.getSecond());
        assertEquals(n, t.getNano());
    }

    static Object[][] provider_sampleBadParse() {
        return new Object[][]{
                {"00;00"},
                {"12-00"},
                {"-01:00"},
                {"00:00:00-09"},
                {"00:00:00,09"},
                {"00:00:abs"},
                {"11"},
                {"11:30+01:00"},
                {"11:30+01:00[Europe/Paris]"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleBadParse")
    public void factory_parse_invalidText(String unparsable) {
        assertThrows(DateTimeParseException.class, () -> LocalTime.parse(unparsable));
    }

    //-----------------------------------------------------------------------s
    @Test
    public void factory_parse_illegalHour() {
        assertThrows(DateTimeParseException.class, () -> LocalTime.parse("25:00"));
    }

    @Test
    public void factory_parse_illegalMinute() {
        assertThrows(DateTimeParseException.class, () -> LocalTime.parse("12:60"));
    }

    @Test
    public void factory_parse_illegalSecond() {
        assertThrows(DateTimeParseException.class, () -> LocalTime.parse("12:12:60"));
    }

    //-----------------------------------------------------------------------s
    @Test
    public void factory_parse_nullTest() {
        assertThrows(NullPointerException.class, () -> LocalTime.parse(null));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s");
        LocalTime test = LocalTime.parse("14 30 40", f);
        assertEquals(LocalTime.of(14, 30, 40), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s");
        assertThrows(NullPointerException.class, () -> LocalTime.parse(null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> LocalTime.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        LocalTime test = test12x30x40x987654321;
        assertEquals(12, test.get(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.get(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.get(ChronoField.NANO_OF_SECOND));

        assertEquals(12 * 3600 + 30 * 60 + 40, test.get(ChronoField.SECOND_OF_DAY));
        assertEquals(12 * 60 + 30, test.get(ChronoField.MINUTE_OF_DAY));
        assertEquals(0, test.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(12, test.get(ChronoField.CLOCK_HOUR_OF_AMPM));
        assertEquals(12, test.get(ChronoField.CLOCK_HOUR_OF_DAY));
        assertEquals(1, test.get(ChronoField.AMPM_OF_DAY));
    }

    @Test
    public void test_get_TemporalField_tooBig() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.get(NANO_OF_DAY));
    }

    @Test
    public void test_get_TemporalField_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.get(null));
    }

    @Test
    public void test_get_TemporalField_invalidField() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.get(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_get_TemporalField_dateField() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.get(ChronoField.DAY_OF_MONTH));
    }

    //-----------------------------------------------------------------------
    // getLong(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong_TemporalField() {
        LocalTime test = test12x30x40x987654321;
        assertEquals(12, test.getLong(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.getLong(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.getLong(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.getLong(ChronoField.NANO_OF_SECOND));

        assertEquals(((12 * 3600 + 30 * 60 + 40) * 1000000000L) + 987654321, test.getLong(ChronoField.NANO_OF_DAY));
        assertEquals(12 * 3600 + 30 * 60 + 40, test.getLong(ChronoField.SECOND_OF_DAY));
        assertEquals(12 * 60 + 30, test.getLong(ChronoField.MINUTE_OF_DAY));
        assertEquals(0, test.getLong(ChronoField.HOUR_OF_AMPM));
        assertEquals(12, test.getLong(ChronoField.CLOCK_HOUR_OF_AMPM));
        assertEquals(12, test.getLong(ChronoField.CLOCK_HOUR_OF_DAY));
        assertEquals(1, test.getLong(ChronoField.AMPM_OF_DAY));
    }

    @Test
    public void test_getLong_TemporalField_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.getLong(null));
    }

    @Test
    public void test_getLong_TemporalField_invalidField() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.getLong(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_getLong_TemporalField_dateField() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.getLong(ChronoField.DAY_OF_MONTH));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(null, test12x30x40x987654321.query(TemporalQueries.chronology()));
        assertEquals(null, test12x30x40x987654321.query(TemporalQueries.localDate()));
        assertEquals(test12x30x40x987654321, test12x30x40x987654321.query(TemporalQueries.localTime()));
        assertEquals(null, test12x30x40x987654321.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.NANOS, test12x30x40x987654321.query(TemporalQueries.precision()));
        assertEquals(null, test12x30x40x987654321.query(TemporalQueries.zone()));
        assertEquals(null, test12x30x40x987654321.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.query(null));
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleTimes() {
        return new Object[][] {
            {0, 0, 0, 0},
            {0, 0, 0, 1},
            {0, 0, 1, 0},
            {0, 0, 1, 1},
            {0, 1, 0, 0},
            {0, 1, 0, 1},
            {0, 1, 1, 0},
            {0, 1, 1, 1},
            {1, 0, 0, 0},
            {1, 0, 0, 1},
            {1, 0, 1, 0},
            {1, 0, 1, 1},
            {1, 1, 0, 0},
            {1, 1, 0, 1},
            {1, 1, 1, 0},
            {1, 1, 1, 1},
        };
    }

    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_get(int h, int m, int s, int ns) {
        LocalTime a = LocalTime.of(h, m, s, ns);
        assertEquals(h, a.getHour());
        assertEquals(m, a.getMinute());
        assertEquals(s, a.getSecond());
        assertEquals(ns, a.getNano());
    }

    //-----------------------------------------------------------------------
    // with()
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final LocalTime sample = LocalTime.of(23, 5);
        TemporalAdjuster adjuster = dateTime -> sample;
        assertEquals(sample, test12x30x40x987654321.with(adjuster));
    }

    @Test
    public void test_with_adjustment_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.with(null));
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        LocalTime t = test12x30x40x987654321;
        for (int i = 0; i < 24; i++) {
            t = t.withHour(i);
            assertEquals(i, t.getHour());
        }
    }

    @Test
    public void test_withHour_noChange_equal() {
        LocalTime t = test12x30x40x987654321.withHour(12);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_withHour_toMidnight_equal() {
        LocalTime t = LocalTime.of(1, 0).withHour(0);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_withHour_toMidday_equal() {
        LocalTime t = LocalTime.of(1, 0).withHour(12);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_withHour_hourTooLow() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withHour(-1));
    }

    @Test
    public void test_withHour_hourTooHigh() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withHour(24));
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        LocalTime t = test12x30x40x987654321;
        for (int i = 0; i < 60; i++) {
            t = t.withMinute(i);
            assertEquals(i, t.getMinute());
        }
    }

    @Test
    public void test_withMinute_noChange_equal() {
        LocalTime t = test12x30x40x987654321.withMinute(30);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_withMinute_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 1).withMinute(0);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_withMinute_toMidday_equals() {
        LocalTime t = LocalTime.of(12, 1).withMinute(0);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_withMinute_minuteTooLow() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withMinute(-1));
    }

    @Test
    public void test_withMinute_minuteTooHigh() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withMinute(60));
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        LocalTime t = test12x30x40x987654321;
        for (int i = 0; i < 60; i++) {
            t = t.withSecond(i);
            assertEquals(i, t.getSecond());
        }
    }

    @Test
    public void test_withSecond_noChange_equal() {
        LocalTime t = test12x30x40x987654321.withSecond(40);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_withSecond_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 0, 1).withSecond(0);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_withSecond_toMidday_equal() {
        LocalTime t = LocalTime.of(12, 0, 1).withSecond(0);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_withSecond_secondTooLow() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withSecond(-1));
    }

    @Test
    public void test_withSecond_secondTooHigh() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withSecond(60));
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        LocalTime t = test12x30x40x987654321;
        t = t.withNano(1);
        assertEquals(1, t.getNano());
        t = t.withNano(10);
        assertEquals(10, t.getNano());
        t = t.withNano(100);
        assertEquals(100, t.getNano());
        t = t.withNano(999999999);
        assertEquals(999999999, t.getNano());
    }

    @Test
    public void test_withNanoOfSecond_noChange_equal() {
        LocalTime t = test12x30x40x987654321.withNano(987654321);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_withNanoOfSecond_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 0, 0, 1).withNano(0);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_withNanoOfSecond_toMidday_equal() {
        LocalTime t = LocalTime.of(12, 0, 0, 1).withNano(0);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_withNanoOfSecond_nanoTooLow() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withNano(-1));
    }

    @Test
    public void test_withNanoOfSecond_nanoTooHigh() {
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.withNano(1000000000));
    }

    //-----------------------------------------------------------------------
    // truncated(TemporalUnit)
    //-----------------------------------------------------------------------
    private static final TemporalUnit NINETY_MINS = new TemporalUnit() {
        @Override
        public String toString() {
            return "NinetyMins";
        }
        @Override
        public Duration getDuration() {
            return Duration.ofMinutes(90);
        }
        @Override
        public boolean isDurationEstimated() {
            return false;
        }
        @Override
        public boolean isDateBased() {
            return false;
        }
        @Override
        public boolean isTimeBased() {
            return true;
        }
        @Override
        public boolean isSupportedBy(Temporal temporal) {
            return false;
        }
        @Override
        public <R extends Temporal> R addTo(R r, long l) {
            throw new UnsupportedOperationException();
        }
        @Override
        public long between(Temporal r, Temporal r2) {
            throw new UnsupportedOperationException();
        }
    };

    private static final TemporalUnit NINETY_FIVE_MINS = new TemporalUnit() {
        @Override
        public String toString() {
            return "NinetyFiveMins";
        }
        @Override
        public Duration getDuration() {
            return Duration.ofMinutes(95);
        }
        @Override
        public boolean isDurationEstimated() {
            return false;
        }
        @Override
        public boolean isDateBased() {
            return false;
        }
        @Override
        public boolean isTimeBased() {
            return true;
        }
        @Override
        public boolean isSupportedBy(Temporal temporal) {
            return false;
        }
        @Override
        public <R extends Temporal> R addTo(R r, long l) {
            throw new UnsupportedOperationException();
        }
        @Override
        public long between(Temporal r, Temporal r2) {
            throw new UnsupportedOperationException();
        }
    };

    static Object[][] data_truncatedToValid() {
        return new Object[][] {
            {LocalTime.of(1, 2, 3, 123456789), NANOS, LocalTime.of(1, 2, 3, 123456789)},
            {LocalTime.of(1, 2, 3, 123456789), MICROS, LocalTime.of(1, 2, 3, 123456000)},
            {LocalTime.of(1, 2, 3, 123456789), MILLIS, LocalTime.of(1, 2, 3, 123000000)},
            {LocalTime.of(1, 2, 3, 123456789), SECONDS, LocalTime.of(1, 2, 3)},
            {LocalTime.of(1, 2, 3, 123456789), MINUTES, LocalTime.of(1, 2)},
            {LocalTime.of(1, 2, 3, 123456789), HOURS, LocalTime.of(1, 0)},
            {LocalTime.of(1, 2, 3, 123456789), DAYS, LocalTime.MIDNIGHT},

            {LocalTime.of(1, 1, 1, 123456789), NINETY_MINS, LocalTime.of(0, 0)},
            {LocalTime.of(2, 1, 1, 123456789), NINETY_MINS, LocalTime.of(1, 30)},
            {LocalTime.of(3, 1, 1, 123456789), NINETY_MINS, LocalTime.of(3, 0)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_truncatedToValid")
    public void test_truncatedTo_valid(LocalTime input, TemporalUnit unit, LocalTime expected) {
        assertEquals(expected, input.truncatedTo(unit));
    }

    static Object[][] data_truncatedToInvalid() {
        return new Object[][] {
            {LocalTime.of(1, 2, 3, 123456789), NINETY_FIVE_MINS},
            {LocalTime.of(1, 2, 3, 123456789), WEEKS},
            {LocalTime.of(1, 2, 3, 123456789), MONTHS},
            {LocalTime.of(1, 2, 3, 123456789), YEARS},
        };
    }

    @ParameterizedTest
    @MethodSource("data_truncatedToInvalid")
    public void test_truncatedTo_invalid(LocalTime input, TemporalUnit unit) {
        assertThrows(DateTimeException.class, () -> input.truncatedTo(unit));
    }

    @Test
    public void test_truncatedTo_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.truncatedTo(null));
    }

    //-----------------------------------------------------------------------
    // plus(PlusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Adjuster_positiveHours() {
        TemporalAmount period = MockSimplePeriod.of(7, ChronoUnit.HOURS);
        LocalTime t = test12x30x40x987654321.plus(period);
        assertEquals(LocalTime.of(19, 30, 40, 987654321), t);
    }

    @Test
    public void test_plus_Adjuster_negativeMinutes() {
        TemporalAmount period = MockSimplePeriod.of(-25, ChronoUnit.MINUTES);
        LocalTime t = test12x30x40x987654321.plus(period);
        assertEquals(LocalTime.of(12, 5, 40, 987654321), t);
    }

    @Test
    public void test_plus_Adjuster_zero() {
        TemporalAmount period = Period.ZERO;
        LocalTime t = test12x30x40x987654321.plus(period);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plus_Adjuster_wrap() {
        TemporalAmount p = Duration.ofHours(1);
        LocalTime t = LocalTime.of(23, 30).plus(p);
        assertEquals(LocalTime.of(0, 30), t);
    }

    @Test
    public void test_plus_Adjuster_dateNotAllowed() {
        TemporalAmount period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.plus(period));
    }

    @Test
    public void test_plus_Adjuster_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.plus(null));
    }

    //-----------------------------------------------------------------------
    // plus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_longPeriodUnit_positiveHours() {
        LocalTime t = test12x30x40x987654321.plus(7, ChronoUnit.HOURS);
        assertEquals(LocalTime.of(19, 30, 40, 987654321), t);
    }

    @Test
    public void test_plus_longPeriodUnit_negativeMinutes() {
        LocalTime t = test12x30x40x987654321.plus(-25, ChronoUnit.MINUTES);
        assertEquals(LocalTime.of(12, 5, 40, 987654321), t);
    }

    @Test
    public void test_plus_longPeriodUnit_zero() {
        LocalTime t = test12x30x40x987654321.plus(0, ChronoUnit.MINUTES);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plus_long_unit_invalidUnit() {
        for (TemporalUnit unit : INVALID_UNITS) {
            try {
                test12x30x40x987654321.plus(1, unit);
                fail("Unit should not be allowed " + unit);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test
    public void test_plus_long_multiples() {
        assertThrows(UnsupportedTemporalTypeException.class, () -> test12x30x40x987654321.plus(0, DAYS));
    }

    @Test
    public void test_plus_longPeriodUnit_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.plus(1, null));
    }

    //-----------------------------------------------------------------------
    // plus(adjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_adjuster() {
        Duration p = Duration.ofSeconds(62, 3);
        LocalTime t = test12x30x40x987654321.plus(p);
        assertEquals(LocalTime.of(12, 31, 42, 987654324), t);
    }

    @Test
    public void test_plus_adjuster_big() {
        Duration p = Duration.ofNanos(Long.MAX_VALUE);
        LocalTime t = test12x30x40x987654321.plus(p);
        assertEquals(test12x30x40x987654321.plusNanos(Long.MAX_VALUE), t);
    }

    @Test
    public void test_plus_adjuster_zero_equal() {
        LocalTime t = test12x30x40x987654321.plus(Period.ZERO);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plus_adjuster_wrap() {
        Duration p = Duration.ofHours(1);
        LocalTime t = LocalTime.of(23, 30).plus(p);
        assertEquals(LocalTime.of(0, 30), t);
    }

    @Test
    public void test_plus_adjuster_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.plus(null));
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        for (int i = 0; i < 50; i++) {
            t = t.plusHours(1);
            assertEquals((i + 1) % 24, t.getHour());
        }
    }

    @Test
    public void test_plusHours_fromZero() {
        LocalTime base = LocalTime.MIDNIGHT;
        for (int i = -50; i < 50; i++) {
            LocalTime t = base.plusHours(i);
            assertEquals((i + 72) % 24, t.getHour());
        }
    }

    @Test
    public void test_plusHours_fromOne() {
        LocalTime base = LocalTime.of(1, 0);
        for (int i = -50; i < 50; i++) {
            LocalTime t = base.plusHours(i);
            assertEquals((1 + i + 72) % 24, t.getHour());
        }
    }

    @Test
    public void test_plusHours_noChange_equal() {
        LocalTime t = test12x30x40x987654321.plusHours(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusHours_toMidnight_equal() {
        LocalTime t = LocalTime.of(23, 0).plusHours(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_plusHours_toMidday_equal() {
        LocalTime t = LocalTime.of(11, 0).plusHours(1);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_plusHours_big() {
        LocalTime t = LocalTime.of(2, 30).plusHours(Long.MAX_VALUE);
        int hours = (int) (Long.MAX_VALUE % 24L);
        assertEquals(LocalTime.of(2, 30).plusHours(hours), t);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        for (int i = 0; i < 70; i++) {
            t = t.plusMinutes(1);
            min++;
            if (min == 60) {
                hour++;
                min = 0;
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
        }
    }

    @Test
    public void test_plusMinutes_fromZero() {
        LocalTime base = LocalTime.MIDNIGHT;
        int hour;
        int min;
        for (int i = -70; i < 70; i++) {
            LocalTime t = base.plusMinutes(i);
            if (i < -60) {
                hour = 22;
                min = i + 120;
            } else if (i < 0) {
                hour = 23;
                min = i + 60;
            } else if (i >= 60) {
                hour = 1;
                min = i - 60;
            } else {
                hour = 0;
                min = i;
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
        }
    }

    @Test
    public void test_plusMinutes_noChange_equal() {
        LocalTime t = test12x30x40x987654321.plusMinutes(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusMinutes_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.plusMinutes(24 * 60);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusMinutes_toMidnight_equal() {
        LocalTime t = LocalTime.of(23, 59).plusMinutes(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_plusMinutes_toMidday_equal() {
        LocalTime t = LocalTime.of(11, 59).plusMinutes(1);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_plusMinutes_big() {
        LocalTime t = LocalTime.of(2, 30).plusMinutes(Long.MAX_VALUE);
        int mins = (int) (Long.MAX_VALUE % (24L * 60L));
        assertEquals(LocalTime.of(2, 30).plusMinutes(mins), t);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        for (int i = 0; i < 3700; i++) {
            t = t.plusSeconds(1);
            sec++;
            if (sec == 60) {
                min++;
                sec = 0;
            }
            if (min == 60) {
                hour++;
                min = 0;
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
            assertEquals(sec, t.getSecond());
        }
    }

    static Iterator<Object[]> plusSeconds_fromZero() {
        return new Iterator<Object[]>() {
            int delta = 30;
            int i = -3660;
            int hour = 22;
            int min = 59;
            int sec;

            @Override
            public boolean hasNext() {
                return i <= 3660;
            }

            @Override
            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec};
                i += delta;
                sec += delta;

                if (sec >= 60) {
                    min++;
                    sec -= 60;

                    if (min == 60) {
                        hour++;
                        min = 0;

                        if (hour == 24) {
                            hour = 0;
                        }
                    }
                }

                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @ParameterizedTest
    @MethodSource("plusSeconds_fromZero")
    public void test_plusSeconds_fromZero(int seconds, int hour, int min, int sec) {
        LocalTime base = LocalTime.MIDNIGHT;
        LocalTime t = base.plusSeconds(seconds);

        assertEquals(t.getHour(), hour);
        assertEquals(t.getMinute(), min);
        assertEquals(t.getSecond(), sec);
    }

    @Test
    public void test_plusSeconds_noChange_equal() {
        LocalTime t = test12x30x40x987654321.plusSeconds(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusSeconds_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.plusSeconds(24 * 60 * 60);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusSeconds_toMidnight_equal() {
        LocalTime t = LocalTime.of(23, 59, 59).plusSeconds(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_plusSeconds_toMidday_equal() {
        LocalTime t = LocalTime.of(11, 59, 59).plusSeconds(1);
        assertEquals(LocalTime.NOON, t);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos_halfABillion() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int nanos = 0;
        for (long i = 0; i < 3700 * 1000000000L; i += 500000000) {
            t = t.plusNanos(500000000);
            nanos += 500000000;
            if (nanos == 1000000000) {
                sec++;
                nanos = 0;
            }
            if (sec == 60) {
                min++;
                sec = 0;
            }
            if (min == 60) {
                hour++;
                min = 0;
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
            assertEquals(sec, t.getSecond());
            assertEquals(nanos, t.getNano());
        }
    }

    static Iterator<Object[]> plusNanos_fromZero() {
        return new Iterator<Object[]>() {
            long delta = 7500000000L;
            long i = -3660 * 1000000000L;
            int hour = 22;
            int min = 59;
            int sec;
            long nanos;

            @Override
            public boolean hasNext() {
                return i <= 3660 * 1000000000L;
            }

            @Override
            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec, (int) nanos};
                i += delta;
                nanos += delta;

                if (nanos >= 1000000000L) {
                    sec += nanos / 1000000000L;
                    nanos %= 1000000000L;

                    if (sec >= 60) {
                        min++;
                        sec %= 60;

                        if (min == 60) {
                            hour++;
                            min = 0;

                            if (hour == 24) {
                                hour = 0;
                            }
                        }
                    }
                }

                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @ParameterizedTest
    @MethodSource("plusNanos_fromZero")
    public void test_plusNanos_fromZero(long nanoseconds, int hour, int min, int sec, int nanos) {
        LocalTime base = LocalTime.MIDNIGHT;
        LocalTime t = base.plusNanos(nanoseconds);

        assertEquals(t.getHour(), hour);
        assertEquals(t.getMinute(), min);
        assertEquals(t.getSecond(), sec);
        assertEquals(t.getNano(), nanos);
    }

    @Test
    public void test_plusNanos_noChange_equal() {
        LocalTime t = test12x30x40x987654321.plusNanos(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusNanos_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.plusNanos(24 * 60 * 60 * 1000000000L);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_plusNanos_toMidnight_equal() {
        LocalTime t = LocalTime.of(23, 59, 59, 999999999).plusNanos(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_plusNanos_toMidday_equal() {
        LocalTime t = LocalTime.of(11, 59, 59, 999999999).plusNanos(1);
        assertEquals(LocalTime.NOON, t);
    }

    //-----------------------------------------------------------------------
    // minus(MinusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Adjuster() {
        TemporalAmount p = Duration.ofSeconds(62, 3);
        LocalTime t = test12x30x40x987654321.minus(p);
        assertEquals(LocalTime.of(12, 29, 38, 987654318), t);
    }

    @Test
    public void test_minus_Adjuster_positiveHours() {
        TemporalAmount period = MockSimplePeriod.of(7, ChronoUnit.HOURS);
        LocalTime t = test12x30x40x987654321.minus(period);
        assertEquals(LocalTime.of(5, 30, 40, 987654321), t);
    }

    @Test
    public void test_minus_Adjuster_negativeMinutes() {
        TemporalAmount period = MockSimplePeriod.of(-25, ChronoUnit.MINUTES);
        LocalTime t = test12x30x40x987654321.minus(period);
        assertEquals(LocalTime.of(12, 55, 40, 987654321), t);
    }

    @Test
    public void test_minus_Adjuster_big1() {
        TemporalAmount p = Duration.ofNanos(Long.MAX_VALUE);
        LocalTime t = test12x30x40x987654321.minus(p);
        assertEquals(test12x30x40x987654321.minusNanos(Long.MAX_VALUE), t);
    }

    @Test
    public void test_minus_Adjuster_zero() {
        TemporalAmount p = Period.ZERO;
        LocalTime t = test12x30x40x987654321.minus(p);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minus_Adjuster_wrap() {
        TemporalAmount p = Duration.ofHours(1);
        LocalTime t = LocalTime.of(0, 30).minus(p);
        assertEquals(LocalTime.of(23, 30), t);
    }

    @Test
    public void test_minus_Adjuster_dateNotAllowed() {
        TemporalAmount period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        assertThrows(DateTimeException.class, () -> test12x30x40x987654321.minus(period));
    }

    @Test
    public void test_minus_Adjuster_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.minus(null));
    }

    //-----------------------------------------------------------------------
    // minus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_longPeriodUnit_positiveHours() {
        LocalTime t = test12x30x40x987654321.minus(7, ChronoUnit.HOURS);
        assertEquals(LocalTime.of(5, 30, 40, 987654321), t);
    }

    @Test
    public void test_minus_longPeriodUnit_negativeMinutes() {
        LocalTime t = test12x30x40x987654321.minus(-25, ChronoUnit.MINUTES);
        assertEquals(LocalTime.of(12, 55, 40, 987654321), t);
    }

    @Test
    public void test_minus_longPeriodUnit_zero() {
        LocalTime t = test12x30x40x987654321.minus(0, ChronoUnit.MINUTES);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minus_long_unit_invalidUnit() {
        for (TemporalUnit unit : INVALID_UNITS) {
            try {
                test12x30x40x987654321.minus(1, unit);
                fail("Unit should not be allowed " + unit);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test
    public void test_minus_long_multiples() {
        assertThrows(UnsupportedTemporalTypeException.class, () -> test12x30x40x987654321.minus(0, DAYS));
    }

    @Test
    public void test_minus_longPeriodUnit_null() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.minus(1, null));
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        for (int i = 0; i < 50; i++) {
            t = t.minusHours(1);
            assertEquals((((-i + 23) % 24) + 24) % 24, t.getHour(), String.valueOf(i));
        }
    }

    @Test
    public void test_minusHours_fromZero() {
        LocalTime base = LocalTime.MIDNIGHT;
        for (int i = -50; i < 50; i++) {
            LocalTime t = base.minusHours(i);
            assertEquals(((-i % 24) + 24) % 24, t.getHour());
        }
    }

    @Test
    public void test_minusHours_fromOne() {
        LocalTime base = LocalTime.of(1, 0);
        for (int i = -50; i < 50; i++) {
            LocalTime t = base.minusHours(i);
            assertEquals((1 + (-i % 24) + 24) % 24, t.getHour());
        }
    }

    @Test
    public void test_minusHours_noChange_equal() {
        LocalTime t = test12x30x40x987654321.minusHours(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusHours_toMidnight_equal() {
        LocalTime t = LocalTime.of(1, 0).minusHours(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_minusHours_toMidday_equal() {
        LocalTime t = LocalTime.of(13, 0).minusHours(1);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_minusHours_big() {
        LocalTime t = LocalTime.of(2, 30).minusHours(Long.MAX_VALUE);
        int hours = (int) (Long.MAX_VALUE % 24L);
        assertEquals(LocalTime.of(2, 30).minusHours(hours), t);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        for (int i = 0; i < 70; i++) {
            t = t.minusMinutes(1);
            min--;
            if (min == -1) {
                hour--;
                min = 59;

                if (hour == -1) {
                    hour = 23;
                }
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
        }
    }

    @Test
    public void test_minusMinutes_fromZero() {
        LocalTime base = LocalTime.MIDNIGHT;
        int hour = 22;
        int min = 49;
        for (int i = 70; i > -70; i--) {
            LocalTime t = base.minusMinutes(i);
            min++;

            if (min == 60) {
                hour++;
                min = 0;

                if (hour == 24) {
                    hour = 0;
                }
            }

            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
        }
    }

    @Test
    public void test_minusMinutes_noChange_equal() {
        LocalTime t = test12x30x40x987654321.minusMinutes(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusMinutes_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.minusMinutes(24 * 60);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusMinutes_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 1).minusMinutes(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_minusMinutes_toMidday_equals() {
        LocalTime t = LocalTime.of(12, 1).minusMinutes(1);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_minusMinutes_big() {
        LocalTime t = LocalTime.of(2, 30).minusMinutes(Long.MAX_VALUE);
        int mins = (int) (Long.MAX_VALUE % (24L * 60L));
        assertEquals(LocalTime.of(2, 30).minusMinutes(mins), t);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds_one() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        for (int i = 0; i < 3700; i++) {
            t = t.minusSeconds(1);
            sec--;
            if (sec == -1) {
                min--;
                sec = 59;

                if (min == -1) {
                    hour--;
                    min = 59;

                    if (hour == -1) {
                        hour = 23;
                    }
                }
            }
            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
            assertEquals(sec, t.getSecond());
        }
    }

    static Iterator<Object[]> minusSeconds_fromZero() {
        return new Iterator<Object[]>() {
            int delta = 30;
            int i = 3660;
            int hour = 22;
            int min = 59;
            int sec;

            @Override
            public boolean hasNext() {
                return i >= -3660;
            }

            @Override
            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec};
                i -= delta;
                sec += delta;

                if (sec >= 60) {
                    min++;
                    sec -= 60;

                    if (min == 60) {
                        hour++;
                        min = 0;

                        if (hour == 24) {
                            hour = 0;
                        }
                    }
                }

                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @ParameterizedTest
    @MethodSource("minusSeconds_fromZero")
    public void test_minusSeconds_fromZero(int seconds, int hour, int min, int sec) {
        LocalTime base = LocalTime.MIDNIGHT;
        LocalTime t = base.minusSeconds(seconds);

        assertEquals(hour, t.getHour());
        assertEquals(min, t.getMinute());
        assertEquals(sec, t.getSecond());
    }

    @Test
    public void test_minusSeconds_noChange_equal() {
        LocalTime t = test12x30x40x987654321.minusSeconds(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusSeconds_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.minusSeconds(24 * 60 * 60);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusSeconds_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 0, 1).minusSeconds(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_minusSeconds_toMidday_equal() {
        LocalTime t = LocalTime.of(12, 0, 1).minusSeconds(1);
        assertEquals(LocalTime.NOON, t);
    }

    @Test
    public void test_minusSeconds_big() {
        LocalTime t = LocalTime.of(2, 30).minusSeconds(Long.MAX_VALUE);
        int secs = (int) (Long.MAX_VALUE % (24L * 60L * 60L));
        assertEquals(LocalTime.of(2, 30).minusSeconds(secs), t);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos_halfABillion() {
        LocalTime t = LocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int nanos = 0;
        for (long i = 0; i < 3700 * 1000000000L; i += 500000000) {
            t = t.minusNanos(500000000);
            nanos -= 500000000;

            if (nanos < 0) {
                sec--;
                nanos += 1000000000;

                if (sec == -1) {
                    min--;
                    sec += 60;

                    if (min == -1) {
                        hour--;
                        min += 60;

                        if (hour == -1) {
                            hour += 24;
                        }
                    }
                }
            }

            assertEquals(hour, t.getHour());
            assertEquals(min, t.getMinute());
            assertEquals(sec, t.getSecond());
            assertEquals(nanos, t.getNano());
        }
    }

    static Iterator<Object[]> minusNanos_fromZero() {
        return new Iterator<Object[]>() {
            long delta = 7500000000L;
            long i = 3660 * 1000000000L;
            int hour = 22;
            int min = 59;
            int sec;
            long nanos;

            @Override
            public boolean hasNext() {
                return i >= -3660 * 1000000000L;
            }

            @Override
            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec, (int) nanos};
                i -= delta;
                nanos += delta;

                if (nanos >= 1000000000L) {
                    sec += nanos / 1000000000L;
                    nanos %= 1000000000L;

                    if (sec >= 60) {
                        min++;
                        sec %= 60;

                        if (min == 60) {
                            hour++;
                            min = 0;

                            if (hour == 24) {
                                hour = 0;
                            }
                        }
                    }
                }

                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @ParameterizedTest
    @MethodSource("minusNanos_fromZero")
    public void test_minusNanos_fromZero(long nanoseconds, int hour, int min, int sec, int nanos) {
        LocalTime base = LocalTime.MIDNIGHT;
        LocalTime t = base.minusNanos(nanoseconds);

        assertEquals(t.getHour(), hour);
        assertEquals(t.getMinute(), min);
        assertEquals(t.getSecond(), sec);
        assertEquals(t.getNano(), nanos);
    }

    @Test
    public void test_minusNanos_noChange_equal() {
        LocalTime t = test12x30x40x987654321.minusNanos(0);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusNanos_noChange_oneDay_equal() {
        LocalTime t = test12x30x40x987654321.minusNanos(24 * 60 * 60 * 1000000000L);
        assertEquals(test12x30x40x987654321, t);
    }

    @Test
    public void test_minusNanos_toMidnight_equal() {
        LocalTime t = LocalTime.of(0, 0, 0, 1).minusNanos(1);
        assertEquals(LocalTime.MIDNIGHT, t);
    }

    @Test
    public void test_minusNanos_toMidday_equal() {
        LocalTime t = LocalTime.of(12, 0, 0, 1).minusNanos(1);
        assertEquals(LocalTime.NOON, t);
    }

    //-----------------------------------------------------------------------
    // until()
    //-----------------------------------------------------------------------
    static Object[][] provider_until() {
        return new Object[][]{
                {"00:00", "00:00", NANOS, 0},
                {"00:00", "00:00", MICROS, 0},
                {"00:00", "00:00", MILLIS, 0},
                {"00:00", "00:00", SECONDS, 0},
                {"00:00", "00:00", MINUTES, 0},
                {"00:00", "00:00", HOURS, 0},
                {"00:00", "00:00", HALF_DAYS, 0},
                
                {"00:00", "00:00:01", NANOS, 1000000000},
                {"00:00", "00:00:01", MICROS, 1000000},
                {"00:00", "00:00:01", MILLIS, 1000},
                {"00:00", "00:00:01", SECONDS, 1},
                {"00:00", "00:00:01", MINUTES, 0},
                {"00:00", "00:00:01", HOURS, 0},
                {"00:00", "00:00:01", HALF_DAYS, 0},
                
                {"00:00", "00:01", NANOS, 60000000000L},
                {"00:00", "00:01", MICROS, 60000000},
                {"00:00", "00:01", MILLIS, 60000},
                {"00:00", "00:01", SECONDS, 60},
                {"00:00", "00:01", MINUTES, 1},
                {"00:00", "00:01", HOURS, 0},
                {"00:00", "00:01", HALF_DAYS, 0},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_until")
    public void test_until(String startStr, String endStr, TemporalUnit unit, long expected) {
        LocalTime start = LocalTime.parse(startStr);
        LocalTime end = LocalTime.parse(endStr);
        assertEquals(expected, start.until(end, unit));
        assertEquals(-expected, end.until(start, unit));
    }

    //-----------------------------------------------------------------------
    // atDate()
    //-----------------------------------------------------------------------
    @Test
    public void test_atDate() {
        LocalTime t = LocalTime.of(11, 30);
        assertEquals(LocalDateTime.of(2012, 6, 30, 11, 30), t.atDate(LocalDate.of(2012, 6, 30)));
    }

    @Test
    public void test_atDate_nullDate() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.atDate(null));
    }

    //-----------------------------------------------------------------------
    // toSecondOfDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_toSecondOfDay() {
        LocalTime t = LocalTime.of(0, 0);
        for (int i = 0; i < 24 * 60 * 60; i++) {
            assertEquals(i, t.toSecondOfDay());
            t = t.plusSeconds(1);
        }
    }

    @Test
    public void test_toSecondOfDay_fromNanoOfDay_symmetry() {
        LocalTime t = LocalTime.of(0, 0);
        for (int i = 0; i < 24 * 60 * 60; i++) {
            assertEquals(t, LocalTime.ofSecondOfDay(t.toSecondOfDay()));
            t = t.plusSeconds(1);
        }
    }

    //-----------------------------------------------------------------------
    // toNanoOfDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_toNanoOfDay() {
        LocalTime t = LocalTime.of(0, 0);
        for (int i = 0; i < 1000000; i++) {
            assertEquals(i, t.toNanoOfDay());
            t = t.plusNanos(1);
        }
        t = LocalTime.of(0, 0);
        for (int i = 1; i <= 1000000; i++) {
            t = t.minusNanos(1);
            assertEquals(24 * 60 * 60 * 1000000000L - i, t.toNanoOfDay());
        }
    }

    @Test
    public void test_toNanoOfDay_fromNanoOfDay_symmetry() {
        LocalTime t = LocalTime.of(0, 0);
        for (int i = 0; i < 1000000; i++) {
            assertEquals(t, LocalTime.ofNanoOfDay(t.toNanoOfDay()));
            t = t.plusNanos(1);
        }
        t = LocalTime.of(0, 0);
        for (int i = 1; i <= 1000000; i++) {
            t = t.minusNanos(1);
            assertEquals(t, LocalTime.ofNanoOfDay(t.toNanoOfDay()));
        }
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_LocalTime(
            LocalTime.MIDNIGHT,
            LocalTime.of(0, 0, 0, 999999999),
            LocalTime.of(0, 0, 59, 0),
            LocalTime.of(0, 0, 59, 999999999),
            LocalTime.of(0, 59, 0, 0),
            LocalTime.of(0, 59, 0, 999999999),
            LocalTime.of(0, 59, 59, 0),
            LocalTime.of(0, 59, 59, 999999999),
            LocalTime.NOON,
            LocalTime.of(12, 0, 0, 999999999),
            LocalTime.of(12, 0, 59, 0),
            LocalTime.of(12, 0, 59, 999999999),
            LocalTime.of(12, 59, 0, 0),
            LocalTime.of(12, 59, 0, 999999999),
            LocalTime.of(12, 59, 59, 0),
            LocalTime.of(12, 59, 59, 999999999),
            LocalTime.of(23, 0, 0, 0),
            LocalTime.of(23, 0, 0, 999999999),
            LocalTime.of(23, 0, 59, 0),
            LocalTime.of(23, 0, 59, 999999999),
            LocalTime.of(23, 59, 0, 0),
            LocalTime.of(23, 59, 0, 999999999),
            LocalTime.of(23, 59, 59, 0),
            LocalTime.of(23, 59, 59, 999999999)
        );
    }

    void doTest_comparisons_LocalTime(LocalTime... localTimes) {
        for (int i = 0; i < localTimes.length; i++) {
            LocalTime a = localTimes[i];
            for (int j = 0; j < localTimes.length; j++) {
                LocalTime b = localTimes[j];
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
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.compareTo(null));
    }

    @Test
    public void test_isBefore_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.isBefore(null));
    }

    @Test
    public void test_isAfter_ObjectNull() {
        assertThrows(NullPointerException.class, () -> test12x30x40x987654321.isAfter(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonLocalTime() {
       Comparable c = test12x30x40x987654321;
       assertThrows(ClassCastException.class, () -> c.compareTo(new Object()));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_true(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s, n);
        assertTrue(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_hour_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h + 1, m, s, n);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_minute_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m + 1, s, n);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_second_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s + 1, n);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_nano_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s, n + 1);
        assertFalse(a.equals(b));
    }

    @Test
    public void test_equals_itself_true() {
        assertTrue(test12x30x40x987654321.equals(test12x30x40x987654321));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(test12x30x40x987654321.equals("2007-07-15"));
    }

    @Test
    public void test_equals_null_false() {
        assertFalse(test12x30x40x987654321.equals(null));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_hashCode_same(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s, n);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_hashCode_hour_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h + 1, m, s, n);
        assertFalse(a.hashCode() == b.hashCode());
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_hashCode_minute_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m + 1, s, n);
        assertFalse(a.hashCode() == b.hashCode());
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_hashCode_second_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s + 1, n);
        assertFalse(a.hashCode() == b.hashCode());
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_hashCode_nano_differs(int h, int m, int s, int n) {
        LocalTime a = LocalTime.of(h, m, s, n);
        LocalTime b = LocalTime.of(h, m, s, n + 1);
        assertFalse(a.hashCode() == b.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleToString() {
        return new Object[][] {
            {0, 0, 0, 0, "00:00"},
            {1, 0, 0, 0, "01:00"},
            {23, 0, 0, 0, "23:00"},
            {0, 1, 0, 0, "00:01"},
            {12, 30, 0, 0, "12:30"},
            {23, 59, 0, 0, "23:59"},
            {0, 0, 1, 0, "00:00:01"},
            {0, 0, 59, 0, "00:00:59"},
            {0, 0, 0, 100000000, "00:00:00.100"},
            {0, 0, 0, 10000000, "00:00:00.010"},
            {0, 0, 0, 1000000, "00:00:00.001"},
            {0, 0, 0, 100000, "00:00:00.000100"},
            {0, 0, 0, 10000, "00:00:00.000010"},
            {0, 0, 0, 1000, "00:00:00.000001"},
            {0, 0, 0, 100, "00:00:00.000000100"},
            {0, 0, 0, 10, "00:00:00.000000010"},
            {0, 0, 0, 1, "00:00:00.000000001"},
            {0, 0, 0, 999999999, "00:00:00.999999999"},
            {0, 0, 0, 99999999, "00:00:00.099999999"},
            {0, 0, 0, 9999999, "00:00:00.009999999"},
            {0, 0, 0, 999999, "00:00:00.000999999"},
            {0, 0, 0, 99999, "00:00:00.000099999"},
            {0, 0, 0, 9999, "00:00:00.000009999"},
            {0, 0, 0, 999, "00:00:00.000000999"},
            {0, 0, 0, 99, "00:00:00.000000099"},
            {0, 0, 0, 9, "00:00:00.000000009"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int h, int m, int s, int n, String expected) {
        LocalTime t = LocalTime.of(h, m, s, n);
        String str = t.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s");
        String t = LocalTime.of(11, 30, 45).format(f);
        assertEquals("11 30 45", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class, () -> LocalTime.of(11, 30, 45).format(null));
    }

}
