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

import static java.time.Month.DECEMBER;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test OffsetDateTime.
 */
@TeaVMTest
public class TestOffsetDateTime extends AbstractDateTimeTest {

    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId ZONE_GAZA = ZoneId.of("Asia/Gaza");
    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_MONE = ZoneOffset.ofHours(-1);
    private static final ZoneOffset OFFSET_MTWO = ZoneOffset.ofHours(-2);
    private OffsetDateTime test2008x6x30x11x30x59x000000500;

    @BeforeEach
    public void setUp() {
        test2008x6x30x11x30x59x000000500 = OffsetDateTime.of(LocalDate.of(2008, 6, 30),
                LocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test2008x6x30x11x30x59x000000500, OffsetDateTime.MIN, OffsetDateTime.MAX};
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
            DAY_OF_WEEK,
            ALIGNED_DAY_OF_WEEK_IN_MONTH,
            ALIGNED_DAY_OF_WEEK_IN_YEAR,
            DAY_OF_MONTH,
            DAY_OF_YEAR,
            EPOCH_DAY,
            ALIGNED_WEEK_OF_MONTH,
            ALIGNED_WEEK_OF_YEAR,
            MONTH_OF_YEAR,
            PROLEPTIC_MONTH,
            YEAR_OF_ERA,
            YEAR,
            ERA,
            OFFSET_SECONDS,
            INSTANT_SECONDS,
            JulianFields.JULIAN_DAY,
            JulianFields.MODIFIED_JULIAN_DAY,
            JulianFields.RATA_DIE,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<TemporalField>(Arrays.<TemporalField>asList(ChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        OffsetDateTime expected = OffsetDateTime.now(Clock.systemDefaultZone());
        OffsetDateTime test = OffsetDateTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= 100000000) {
            // may be date change
            expected = OffsetDateTime.now(Clock.systemDefaultZone());
            test = OffsetDateTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(1970, test.getYear());
            assertEquals(Month.JANUARY, test.getMonth());
            assertEquals(i < 24 * 60 * 60 ? 1 : 2, test.getDayOfMonth());
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(123456789, test.getNano());
            assertEquals(ZoneOffset.UTC, test.getOffset());
        }
    }

    @Test
    public void now_Clock_allSecsInDay_offset() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant.minusSeconds(OFFSET_PONE.getTotalSeconds()), OFFSET_PONE);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(1970, test.getYear());
            assertEquals(Month.JANUARY, test.getMonth());
            assertEquals((i < 24 * 60 * 60) ? 1 : 2, test.getDayOfMonth());
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(123456789, test.getNano());
            assertEquals(OFFSET_PONE, test.getOffset());
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {
        LocalTime expected = LocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(1969, test.getYear());
            assertEquals(Month.DECEMBER, test.getMonth());
            assertEquals(31, test.getDayOfMonth());
            expected = expected.minusSeconds(1);
            assertEquals(expected, test.toLocalTime());
            assertEquals(ZoneOffset.UTC, test.getOffset());
        }
    }

    @Test
    public void now_Clock_offsets() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(12, 0), ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base.toInstant(), offset);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals((12 + i) % 24, test.getHour());
            assertEquals(0, test.getMinute());
            assertEquals(0, test.getSecond());
            assertEquals(0, test.getNano());
            assertEquals(offset, test.getOffset());
        }
    }

    @Test
    public void now_Clock_nullZoneId() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.now((ZoneId) null));
    }

    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.now((Clock) null));
    }

    //-----------------------------------------------------------------------
    private void check(OffsetDateTime test, int y, int mo, int d, int h, int m, int s, int n, ZoneOffset offset) {
        assertEquals(y, test.getYear());
        assertEquals(mo, test.getMonth().getValue());
        assertEquals(d, test.getDayOfMonth());
        assertEquals(h, test.getHour());
        assertEquals(m, test.getMinute());
        assertEquals(s, test.getSecond());
        assertEquals(n, test.getNano());
        assertEquals(offset, test.getOffset());
        assertEquals(test, test);
        assertEquals(test.hashCode(), test.hashCode());
        assertEquals(test, OffsetDateTime.of(LocalDateTime.of(y, mo, d, h, m, s, n), offset));
    }

    //-----------------------------------------------------------------------
    // dateTime factories
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHM() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30),
                LocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHMS() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30),
                LocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHMSN() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30),
                LocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHM() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHMS() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHMSN() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset() {
        LocalDate date = LocalDate.of(2008, 6, 30);
        LocalTime time = LocalTime.of(11, 30, 10, 500);
        OffsetDateTime test = OffsetDateTime.of(date, time, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalDate() {
        LocalTime time = LocalTime.of(11, 30, 10, 500);
        assertThrows(NullPointerException.class, () -> OffsetDateTime.of((LocalDate) null, time, OFFSET_PONE));
    }

    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalTime() {
        LocalDate date = LocalDate.of(2008, 6, 30);
        assertThrows(NullPointerException.class, () -> OffsetDateTime.of(date, (LocalTime) null, OFFSET_PONE));
    }

    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset_nullOffset() {
        LocalDate date = LocalDate.of(2008, 6, 30);
        LocalTime time = LocalTime.of(11, 30, 10, 500);
        assertThrows(NullPointerException.class, () -> OffsetDateTime.of(date, time, (ZoneOffset) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateTimeZoneOffset() {
        LocalDateTime dt = LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500));
        OffsetDateTime test = OffsetDateTime.of(dt, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test
    public void factory_of_LocalDateTimeZoneOffset_nullProvider() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.of((LocalDateTime) null, OFFSET_PONE));
    }

    @Test
    public void factory_of_LocalDateTimeZoneOffset_nullOffset() {
        LocalDateTime dt = LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500));
        assertThrows(NullPointerException.class, () -> OffsetDateTime.of(dt, (ZoneOffset) null));
    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(OffsetDateTime.of(LocalDate.of(2007, 7, 15), LocalTime.of(17, 30), OFFSET_PONE),
                OffsetDateTime.from(OffsetDateTime.of(LocalDate.of(2007, 7, 15), LocalTime.of(17, 30), OFFSET_PONE)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> OffsetDateTime.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_Calendricals_null() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_parse(int y, int month, int d, int h, int m, int s, int n, String offsetId, String text) {
        OffsetDateTime t = OffsetDateTime.parse(text);
        assertEquals(y, t.getYear());
        assertEquals(month, t.getMonth().getValue());
        assertEquals(d, t.getDayOfMonth());
        assertEquals(h, t.getHour());
        assertEquals(m, t.getMinute());
        assertEquals(s, t.getSecond());
        assertEquals(n, t.getNano());
        assertEquals(offsetId, t.getOffset().getId());
    }

    @Test
    public void factory_parse_illegalValue() {
        assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("2008-06-32T11:15+01:00"));
    }

    @Test
    public void factory_parse_invalidValue() {
        assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("2008-06-31T11:15+01:00"));
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.parse((String) null));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d H m s XXX");
        OffsetDateTime test = OffsetDateTime.parse("2010 12 3 11 30 0 +01:00", f);
        assertEquals(OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), ZoneOffset.ofHours(1)), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d H m s");
        assertThrows(NullPointerException.class, () -> OffsetDateTime.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleTimes() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 20, 500, OFFSET_PONE},
            {2008, 6, 30, 11, 0, 0, 0, OFFSET_PONE},
            {2008, 6, 30, 23, 59, 59, 999999999, OFFSET_PONE},
            {-1, 1, 1, 0, 0, 0, 0, OFFSET_PONE},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_get(int y, int o, int d, int h, int m, int s, int n, ZoneOffset offset) {
        LocalDate localDate = LocalDate.of(y, o, d);
        LocalTime localTime = LocalTime.of(h, m, s, n);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        OffsetDateTime a = OffsetDateTime.of(localDateTime, offset);

        assertEquals(localDate.getYear(), a.getYear());
        assertEquals(localDate.getMonth(), a.getMonth());
        assertEquals(localDate.getDayOfMonth(), a.getDayOfMonth());
        assertEquals(localDate.getDayOfYear(), a.getDayOfYear());
        assertEquals(localDate.getDayOfWeek(), a.getDayOfWeek());

        assertEquals(localDateTime.getHour(), a.getHour());
        assertEquals(localDateTime.getMinute(), a.getMinute());
        assertEquals(localDateTime.getSecond(), a.getSecond());
        assertEquals(localDateTime.getNano(), a.getNano());

        assertEquals(OffsetTime.of(localTime, offset), a.toOffsetTime());
        assertEquals(localDateTime.toString() + offset.toString(), a.toString());
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 40, 987654321),
                OFFSET_PONE);
        assertEquals(2008, test.get(ChronoField.YEAR));
        assertEquals(6, test.get(ChronoField.MONTH_OF_YEAR));
        assertEquals(30, test.get(ChronoField.DAY_OF_MONTH));
        assertEquals(1, test.get(ChronoField.DAY_OF_WEEK));
        assertEquals(182, test.get(ChronoField.DAY_OF_YEAR));

        assertEquals(12, test.get(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.get(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.get(ChronoField.NANO_OF_SECOND));
        assertEquals(0, test.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, test.get(ChronoField.AMPM_OF_DAY));

        assertEquals(3600, test.get(ChronoField.OFFSET_SECONDS));
    }

    @Test
    public void test_getLong_TemporalField() {
        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 40, 987654321),
                OFFSET_PONE);
        assertEquals(2008, test.getLong(ChronoField.YEAR));
        assertEquals(6, test.getLong(ChronoField.MONTH_OF_YEAR));
        assertEquals(30, test.getLong(ChronoField.DAY_OF_MONTH));
        assertEquals(1, test.getLong(ChronoField.DAY_OF_WEEK));
        assertEquals(182, test.getLong(ChronoField.DAY_OF_YEAR));

        assertEquals(12, test.getLong(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.getLong(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.getLong(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.getLong(ChronoField.NANO_OF_SECOND));
        assertEquals(0, test.getLong(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, test.getLong(ChronoField.AMPM_OF_DAY));

        assertEquals(test.toEpochSecond(), test.getLong(ChronoField.INSTANT_SECONDS));
        assertEquals(3600, test.getLong(ChronoField.OFFSET_SECONDS));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, test2008x6x30x11x30x59x000000500.query(TemporalQueries.chronology()));
        assertEquals(test2008x6x30x11x30x59x000000500.toLocalDate(),
                test2008x6x30x11x30x59x000000500.query(TemporalQueries.localDate()));
        assertEquals(test2008x6x30x11x30x59x000000500.toLocalTime(),
                test2008x6x30x11x30x59x000000500.query(TemporalQueries.localTime()));
        assertEquals(test2008x6x30x11x30x59x000000500.getOffset(),
                test2008x6x30x11x30x59x000000500.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.NANOS, test2008x6x30x11x30x59x000000500.query(TemporalQueries.precision()));
        assertEquals(test2008x6x30x11x30x59x000000500.getOffset(),
                test2008x6x30x11x30x59x000000500.query(TemporalQueries.zone()));
        assertEquals(null, test2008x6x30x11x30x59x000000500.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test2008x6x30x11x30x59x000000500.query(null));
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final OffsetDateTime sample = OffsetDateTime.of(LocalDate.of(2012, 3, 4), LocalTime.of(23, 5), OFFSET_PONE);
        TemporalAdjuster adjuster = dateTime -> sample;
        assertEquals(sample, test2008x6x30x11x30x59x000000500.with(adjuster));
    }

    @Test
    public void test_with_adjustment_LocalDate() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500.with(LocalDate.of(2012, 9, 3));
        assertEquals(OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(11, 30, 59, 500), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_LocalTime() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500.with(LocalTime.of(19, 15));
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(19, 15), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_LocalDateTime() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500
                .with(LocalDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15)));
        assertEquals(OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_OffsetTime() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500.with(OffsetTime.of(LocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(19, 15), OFFSET_PTWO), test);
    }

    @Test
    public void test_with_adjustment_OffsetDateTime() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500
                .with(OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PTWO), test);
    }

    @Test
    public void test_with_adjustment_Month() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500.with(DECEMBER);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 12, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {
        OffsetDateTime test = test2008x6x30x11x30x59x000000500.with(OFFSET_PTWO);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PTWO), test);
    }

    @Test
    public void test_with_adjustment_null() {
        assertThrows(NullPointerException.class, () -> test2008x6x30x11x30x59x000000500.with((TemporalAdjuster) null));
    }

    @Test
    public void test_withOffsetSameLocal_null() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> base.withOffsetSameLocal(null));
    }

    //-----------------------------------------------------------------------
    // withOffsetSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameInstant() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 59), OFFSET_PTWO);
        assertEquals(expected, test);
    }

    @Test
    public void test_withOffsetSameInstant_null() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> base.withOffsetSameInstant(null));
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withYear(2007);
        assertEquals(OffsetDateTime.of(LocalDate.of(2007, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMonth(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 1, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withDayOfMonth(15);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 15), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withDayOfYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfYear_normal() {
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.withDayOfYear(33);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 2, 2), LocalTime.of(11, 30, 59, 500), OFFSET_PONE), t);
    }

    @Test
    public void test_withDayOfYear_illegal() {
        assertThrows(DateTimeException.class, () -> test2008x6x30x11x30x59x000000500.withDayOfYear(367));
    }

    @Test
    public void test_withDayOfYear_invalid() {
        var date = OffsetDateTime.of(LocalDate.of(2007, 2, 2), LocalTime.of(11, 30), OFFSET_PONE);
        assertThrows(DateTimeException.class, () -> date.withDayOfYear(366));
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withHour(15);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(15, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMinute(15);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 15, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withSecond(15);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 15), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        OffsetDateTime test = base.withNano(15);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 15), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(test2008x6x30x11x30x59x000000500, test2008x6x30x11x30x59x000000500.truncatedTo(NANOS));
        assertEquals(test2008x6x30x11x30x59x000000500.withNano(0),
                test2008x6x30x11x30x59x000000500.truncatedTo(SECONDS));
        assertEquals(test2008x6x30x11x30x59x000000500.with(LocalTime.MIDNIGHT),
                test2008x6x30x11x30x59x000000500.truncatedTo(DAYS));
    }

    @Test
    public void test_truncatedTo_null() {
        assertThrows(NullPointerException.class, () -> test2008x6x30x11x30x59x000000500.truncatedTo(null));
    }

    //-----------------------------------------------------------------------
    // plus(Period)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Period() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.plus(period);
        assertEquals(OffsetDateTime.of(LocalDate.of(2009, 1, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE), t);
    }

    //-----------------------------------------------------------------------
    // plus(Duration)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Duration() {
        Duration dur = Duration.ofSeconds(62, 3);
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.plus(dur);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 32, 1, 503), OFFSET_PONE), t);
    }

    @Test
    public void test_plus_Duration_zero() {
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.plus(Duration.ZERO);
        assertEquals(test2008x6x30x11x30x59x000000500, t);
    }

    @Test
    public void test_plus_Duration_null() {
        assertThrows(NullPointerException.class, () -> test2008x6x30x11x30x59x000000500.plus((Duration) null));
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusYears(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2009, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMonths(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 7, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusWeeks() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusWeeks(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 7, 7), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusDays() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusDays(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 7, 1), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusHours(13);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 7, 1), LocalTime.of(0, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMinutes(30);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 0, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusSeconds(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 31, 0), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetDateTime test = base.plusNanos(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 1), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minus(Period)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Period() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.minus(period);
        assertEquals(OffsetDateTime.of(LocalDate.of(2007, 11, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE), t);
    }

    //-----------------------------------------------------------------------
    // minus(Duration)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Duration() {
        Duration dur = Duration.ofSeconds(62, 3);
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.minus(dur);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 57, 497), OFFSET_PONE), t);
    }

    @Test
    public void test_minus_Duration_zero() {
        OffsetDateTime t = test2008x6x30x11x30x59x000000500.minus(Duration.ZERO);
        assertEquals(test2008x6x30x11x30x59x000000500, t);
    }

    @Test
    public void test_minus_Duration_null() {
        assertThrows(NullPointerException.class, () -> test2008x6x30x11x30x59x000000500.minus((Duration) null));
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusYears(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2007, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMonths(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 5, 30), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusWeeks() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusWeeks(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 23), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusDays() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusDays(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 29), LocalTime.of(11, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusHours(13);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 29), LocalTime.of(22, 30, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMinutes(30);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0, 59), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusSeconds(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58), OFFSET_PONE), test);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetDateTime test = base.minusNanos(1);
        assertEquals(OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58, 999999999), OFFSET_PONE),
                test);
    }

    //-----------------------------------------------------------------------
    // atZoneSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_atZone() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(15, 30)), ZONE_PARIS),
                t.atZoneSameInstant(ZONE_PARIS));
    }

    @Test
    public void test_atZone_nullTimeZone() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        assertThrows(NullPointerException.class, () -> t.atZoneSameInstant((ZoneId) null));
    }

    //-----------------------------------------------------------------------
    // atZoneSimilarLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_atZoneSimilarLocal() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30)), ZONE_PARIS),
                t.atZoneSimilarLocal(ZONE_PARIS));
    }

    @Test
    public void test_atZoneSimilarLocal_dstGap() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 4, 1), LocalTime.of(0, 0), OFFSET_MTWO);
        assertEquals(ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2007, 4, 1), LocalTime.of(1, 0)), ZONE_GAZA),
                t.atZoneSimilarLocal(ZONE_GAZA));
    }

    @Test
    public void test_atZone_dstOverlapSummer() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 10, 28), LocalTime.of(2, 30), OFFSET_PTWO);
        assertEquals(t.toLocalDateTime(), t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime());
        assertEquals(OFFSET_PTWO, t.atZoneSimilarLocal(ZONE_PARIS).getOffset());
        assertEquals(ZONE_PARIS, t.atZoneSimilarLocal(ZONE_PARIS).getZone());
    }

    @Test
    public void test_atZone_dstOverlapWinter() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 10, 28), LocalTime.of(2, 30), OFFSET_PONE);
        assertEquals(t.toLocalDateTime(), t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime());
        assertEquals(OFFSET_PONE, t.atZoneSimilarLocal(ZONE_PARIS).getOffset());
        assertEquals(ZONE_PARIS, t.atZoneSimilarLocal(ZONE_PARIS).getZone());
    }

    @Test
    public void test_atZoneSimilarLocal_nullTimeZone() {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        assertThrows(NullPointerException.class, () -> t.atZoneSimilarLocal((ZoneId) null));
    }

    //-----------------------------------------------------------------------
    // toEpochSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochSecond_afterEpoch() {
        for (int i = 0; i < 100000; i++) {
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneOffset.UTC)
                    .plusSeconds(i);
            assertEquals(i, a.toEpochSecond());
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {
        for (int i = 0; i < 100000; i++) {
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneOffset.UTC)
                    .minusSeconds(i);
            assertEquals(-i, a.toEpochSecond());
        }
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_timeMins() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 3), OFFSET_PONE);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 2), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_timeSecs() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 2), OFFSET_PONE);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 3), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_timeNanos() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 40, 4), OFFSET_PONE);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 40, 5), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_offset() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_offsetNanos() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 40, 6), OFFSET_PTWO);
        // a is before b due to offset
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 40, 5), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_both() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 50), OFFSET_PTWO);
        // a is before b on instant scale
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 20), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_bothNanos() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 20, 40, 4), OFFSET_PTWO);
        // a is before b on instant scale
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 20, 40, 5), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) < 0);
    }

    @Test
    public void test_compareTo_hourDifference() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 0), OFFSET_PONE);
        // a is before b on instant scale
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0), OFFSET_PTWO);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.toInstant().compareTo(b.toInstant()) == 0);
    }

    @Test
    public void test_compareTo_max() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(Year.MAX_VALUE, 12, 31), LocalTime.of(23, 59), OFFSET_MONE);
        // a is before b on instant scale
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(Year.MAX_VALUE, 12, 31), LocalTime.of(23, 59), OFFSET_MTWO);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_min() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(Year.MIN_VALUE, 1, 1), LocalTime.of(0, 0), OFFSET_PTWO);
        // a is before b on instant scale
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(Year.MIN_VALUE, 1, 1), LocalTime.of(0, 0), OFFSET_PONE);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_null() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonOffsetDateTime() {
       Comparable c = test2008x6x30x11x30x59x000000500;
       assertThrows(ClassCastException.class, () -> c.compareTo(new Object()));
    }

    //-----------------------------------------------------------------------
    // isAfter() / isBefore() / isEqual()
    //-----------------------------------------------------------------------
    @Test
    public void test_isBeforeIsAfterIsEqual1() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58, 3), OFFSET_PONE);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 2), OFFSET_PONE);
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 2), OFFSET_PONE);
        // a is before b due to time
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 3), OFFSET_PONE);
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual_instantComparison() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 0), OFFSET_PONE);
        // a is same instant as b
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0), OFFSET_PTWO);
        assertFalse(a.isBefore(b));
        assertTrue(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertTrue(b.isEqual(a));
        assertFalse(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBefore_null() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isBefore(null));
    }

    @Test
    public void test_isEqual_null() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isEqual(null));
    }

    @Test
    public void test_isAfter_null() {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isAfter(null));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_true(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        assertTrue(a.equals(b));
        assertTrue(a.hashCode() == b.hashCode());
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_year_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y + 1, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_hour_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        h = h == 23 ? 22 : h;
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h + 1, m, s, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_minute_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        m = m == 59 ? 58 : m;
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m + 1, s, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_second_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        s = s == 59 ? 58 : s;
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s + 1, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_nano_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        n = n == 999999999 ? 999999998 : n;
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n + 1), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_offset_differs(int y, int o, int d, int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PTWO);
        assertFalse(a.equals(b));
    }

    @Test
    public void test_equals_itself_true() {
        assertTrue(test2008x6x30x11x30x59x000000500.equals(test2008x6x30x11x30x59x000000500));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(test2008x6x30x11x30x59x000000500.equals("2007-07-15"));
    }

    @Test
    public void test_equals_null_false() {
        assertFalse(test2008x6x30x11x30x59x000000500.equals(null));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleToString() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 59, 0, "Z", "2008-06-30T11:30:59Z"},
            {2008, 6, 30, 11, 30, 59, 0, "+01:00", "2008-06-30T11:30:59+01:00"},
            {2008, 6, 30, 11, 30, 59, 999000000, "Z", "2008-06-30T11:30:59.999Z"},
            {2008, 6, 30, 11, 30, 59, 999000000, "+01:00", "2008-06-30T11:30:59.999+01:00"},
            {2008, 6, 30, 11, 30, 59, 999000, "Z", "2008-06-30T11:30:59.000999Z"},
            {2008, 6, 30, 11, 30, 59, 999000, "+01:00", "2008-06-30T11:30:59.000999+01:00"},
            {2008, 6, 30, 11, 30, 59, 999, "Z", "2008-06-30T11:30:59.000000999Z"},
            {2008, 6, 30, 11, 30, 59, 999, "+01:00", "2008-06-30T11:30:59.000000999+01:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int y, int o, int d, int h, int m, int s, int n, String offsetId, String expected) {
        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), ZoneOffset.of(offsetId));
        String str = t.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        String t = OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), OFFSET_PONE).format(f);
        assertEquals("2010 12 3 11 30 0", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class,
                () -> OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), OFFSET_PONE).format(null));
    }

}
