/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestOffsetDateTime extends AbstractDateTimeTest {

    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");

    private static final ZoneId ZONE_GAZA = ZoneId.of("Asia/Gaza");

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);

    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);

    private static final ZoneOffset OFFSET_MONE = ZoneOffset.ofHours(-1);

    private static final ZoneOffset OFFSET_MTWO = ZoneOffset.ofHours(-2);

    private OffsetDateTime TEST_2008_6_30_11_30_59_000000500;

    @Before
    public void setUp() {

        this.TEST_2008_6_30_11_30_59_000000500 = OffsetDateTime.of(LocalDate.of(2008, 6, 30),
                LocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    @Override
    protected List<TemporalAccessor> samples() {

        TemporalAccessor[] array = { this.TEST_2008_6_30_11_30_59_000000500, OffsetDateTime.MIN, OffsetDateTime.MAX };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {

        TemporalField[] array = { NANO_OF_SECOND, NANO_OF_DAY, MICRO_OF_SECOND, MICRO_OF_DAY, MILLI_OF_SECOND,
        MILLI_OF_DAY, SECOND_OF_MINUTE, SECOND_OF_DAY, MINUTE_OF_HOUR, MINUTE_OF_DAY, CLOCK_HOUR_OF_AMPM, HOUR_OF_AMPM,
        CLOCK_HOUR_OF_DAY, HOUR_OF_DAY, AMPM_OF_DAY, DAY_OF_WEEK, ALIGNED_DAY_OF_WEEK_IN_MONTH,
        ALIGNED_DAY_OF_WEEK_IN_YEAR, DAY_OF_MONTH, DAY_OF_YEAR, EPOCH_DAY, ALIGNED_WEEK_OF_MONTH, ALIGNED_WEEK_OF_YEAR,
        MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR_OF_ERA, YEAR, ERA, OFFSET_SECONDS, INSTANT_SECONDS,
        JulianFields.JULIAN_DAY, JulianFields.MODIFIED_JULIAN_DAY, JulianFields.RATA_DIE, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {

        List<TemporalField> list = new ArrayList<>(Arrays.<TemporalField> asList(ChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

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
        assertTrue(diff < 100000000); // less than 0.1 secs
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {

        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 123456789);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_offset() {

        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant.minusSeconds(OFFSET_PONE.getTotalSeconds()), OFFSET_PONE);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60) ? 1 : 2);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 123456789);
            assertEquals(test.getOffset(), OFFSET_PONE);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {

        LocalTime expected = LocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), Month.DECEMBER);
            assertEquals(test.getDayOfMonth(), 31);
            expected = expected.minusSeconds(1);
            assertEquals(test.toLocalTime(), expected);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(12, 0), ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base.toInstant(), offset);
            OffsetDateTime test = OffsetDateTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
        }
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullZoneId() {

        OffsetDateTime.now((ZoneId) null);
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullClock() {

        OffsetDateTime.now((Clock) null);
    }

    private void check(OffsetDateTime test, int y, int mo, int d, int h, int m, int s, int n, ZoneOffset offset) {

        assertEquals(test.getYear(), y);
        assertEquals(test.getMonth().getValue(), mo);
        assertEquals(test.getDayOfMonth(), d);
        assertEquals(test.getHour(), h);
        assertEquals(test.getMinute(), m);
        assertEquals(test.getSecond(), s);
        assertEquals(test.getNano(), n);
        assertEquals(test.getOffset(), offset);
        assertEquals(test, test);
        assertEquals(test.hashCode(), test.hashCode());
        assertEquals(OffsetDateTime.of(LocalDateTime.of(y, mo, d, h, m, s, n), offset), test);
    }

    @Test
    public void factory_of_intMonthIntHM() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30), LocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    @Test
    public void factory_of_intMonthIntHMS() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30), LocalTime.of(11, 30, 10),
                OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    @Test
    public void factory_of_intMonthIntHMSN() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, Month.JUNE, 30), LocalTime.of(11, 30, 10, 500),
                OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test
    public void factory_of_intsHM() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    @Test
    public void factory_of_intsHMS() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    @Test
    public void factory_of_intsHMSN() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset() {

        LocalDate date = LocalDate.of(2008, 6, 30);
        LocalTime time = LocalTime.of(11, 30, 10, 500);
        OffsetDateTime test = OffsetDateTime.of(date, time, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalDate() {

        LocalTime time = LocalTime.of(11, 30, 10, 500);
        OffsetDateTime.of((LocalDate) null, time, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalTime() {

        LocalDate date = LocalDate.of(2008, 6, 30);
        OffsetDateTime.of(date, (LocalTime) null, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullOffset() {

        LocalDate date = LocalDate.of(2008, 6, 30);
        LocalTime time = LocalTime.of(11, 30, 10, 500);
        OffsetDateTime.of(date, time, (ZoneOffset) null);
    }

    @Test
    public void factory_of_LocalDateTimeZoneOffset() {

        LocalDateTime dt = LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500));
        OffsetDateTime test = OffsetDateTime.of(dt, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateTimeZoneOffset_nullProvider() {

        OffsetDateTime.of((LocalDateTime) null, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateTimeZoneOffset_nullOffset() {

        LocalDateTime dt = LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500));
        OffsetDateTime.of(dt, (ZoneOffset) null);
    }

    @Test
    public void test_factory_CalendricalObject() {

        assertEquals(
                OffsetDateTime.from(OffsetDateTime.of(LocalDate.of(2007, 7, 15), LocalTime.of(17, 30), OFFSET_PONE)),
                OffsetDateTime.of(LocalDate.of(2007, 7, 15), LocalTime.of(17, 30), OFFSET_PONE));
    }

    @Test(expected = DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {

        OffsetDateTime.from(LocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_Calendricals_null() {

        OffsetDateTime.from((TemporalAccessor) null);
    }

    @Test
    public void test_parse() {

        for (Object[] data : provider_sampleToString()) {
            int y = (int) data[0];
            int month = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];
            String offsetId = (String) data[7];
            String text = (String) data[8];

            OffsetDateTime t = OffsetDateTime.parse(text);
            assertEquals(t.getYear(), y);
            assertEquals(t.getMonth().getValue(), month);
            assertEquals(t.getDayOfMonth(), d);
            assertEquals(t.getHour(), h);
            assertEquals(t.getMinute(), m);
            assertEquals(t.getSecond(), s);
            assertEquals(t.getNano(), n);
            assertEquals(t.getOffset().getId(), offsetId);
        }
    }

    @Test(expected = DateTimeParseException.class)
    public void factory_parse_illegalValue() {

        OffsetDateTime.parse("2008-06-32T11:15+01:00");
    }

    @Test(expected = DateTimeParseException.class)
    public void factory_parse_invalidValue() {

        OffsetDateTime.parse("2008-06-31T11:15+01:00");
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_nullText() {

        OffsetDateTime.parse((String) null);
    }

    @Test
    public void factory_parse_formatter() {

        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d H m s XXX");
        OffsetDateTime test = OffsetDateTime.parse("2010 12 3 11 30 0 +01:00", f);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), ZoneOffset.ofHours(1)));
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullText() {

        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d H m s");
        OffsetDateTime.parse((String) null, f);
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {

        OffsetDateTime.parse("ANY", null);
    }

    Object[][] provider_sampleTimes() {

        return new Object[][] { { 2008, 6, 30, 11, 30, 20, 500, OFFSET_PONE },
        { 2008, 6, 30, 11, 0, 0, 0, OFFSET_PONE }, { 2008, 6, 30, 23, 59, 59, 999999999, OFFSET_PONE },
        { -1, 1, 1, 0, 0, 0, 0, OFFSET_PONE }, };
    }

    @Test
    public void test_get() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];
            ZoneOffset offset = (ZoneOffset) data[7];

            LocalDate localDate = LocalDate.of(y, o, d);
            LocalTime localTime = LocalTime.of(h, m, s, n);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
            OffsetDateTime a = OffsetDateTime.of(localDateTime, offset);

            assertEquals(a.getYear(), localDate.getYear());
            assertEquals(a.getMonth(), localDate.getMonth());
            assertEquals(a.getDayOfMonth(), localDate.getDayOfMonth());
            assertEquals(a.getDayOfYear(), localDate.getDayOfYear());
            assertEquals(a.getDayOfWeek(), localDate.getDayOfWeek());

            assertEquals(a.getHour(), localDateTime.getHour());
            assertEquals(a.getMinute(), localDateTime.getMinute());
            assertEquals(a.getSecond(), localDateTime.getSecond());
            assertEquals(a.getNano(), localDateTime.getNano());

            assertEquals(a.toOffsetTime(), OffsetTime.of(localTime, offset));
            assertEquals(a.toString(), localDateTime.toString() + offset.toString());
        }
    }

    @Test
    public void test_get_TemporalField() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 40, 987654321),
                OFFSET_PONE);
        assertEquals(test.get(ChronoField.YEAR), 2008);
        assertEquals(test.get(ChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.get(ChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.get(ChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.get(ChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.get(ChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.get(ChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.get(ChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.get(ChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.get(ChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.get(ChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.get(ChronoField.OFFSET_SECONDS), 3600);
    }

    @Test
    public void test_getLong_TemporalField() {

        OffsetDateTime test = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 40, 987654321),
                OFFSET_PONE);
        assertEquals(test.getLong(ChronoField.YEAR), 2008);
        assertEquals(test.getLong(ChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.getLong(ChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.getLong(ChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.getLong(ChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.getLong(ChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(ChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(ChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(ChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.getLong(ChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(ChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.getLong(ChronoField.INSTANT_SECONDS), test.toEpochSecond());
        assertEquals(test.getLong(ChronoField.OFFSET_SECONDS), 3600);
    }

    @Test
    public void test_query() {

        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.chronology()),
                IsoChronology.INSTANCE);
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.localDate()),
                this.TEST_2008_6_30_11_30_59_000000500.toLocalDate());
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.localTime()),
                this.TEST_2008_6_30_11_30_59_000000500.toLocalTime());
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.offset()),
                this.TEST_2008_6_30_11_30_59_000000500.getOffset());
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.precision()), ChronoUnit.NANOS);
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.zone()),
                this.TEST_2008_6_30_11_30_59_000000500.getOffset());
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.query(TemporalQueries.zoneId()), null);
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        this.TEST_2008_6_30_11_30_59_000000500.query(null);
    }

    @Test
    public void test_with_adjustment() {

        final OffsetDateTime sample = OffsetDateTime.of(LocalDate.of(2012, 3, 4), LocalTime.of(23, 5), OFFSET_PONE);
        TemporalAdjuster adjuster = new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {

                return sample;
            }
        };
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.with(adjuster), sample);
    }

    @Test
    public void test_with_adjustment_LocalDate() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500.with(LocalDate.of(2012, 9, 3));
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_LocalTime() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500.with(LocalTime.of(19, 15));
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(19, 15), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_LocalDateTime() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500
                .with(LocalDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15)));
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_OffsetTime() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500
                .with(OffsetTime.of(LocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(19, 15), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_OffsetDateTime() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500
                .with(OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2012, 9, 3), LocalTime.of(19, 15), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_Month() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500.with(DECEMBER);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 12, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {

        OffsetDateTime test = this.TEST_2008_6_30_11_30_59_000000500.with(OFFSET_PTWO);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PTWO));
    }

    @Test(expected = NullPointerException.class)
    public void test_with_adjustment_null() {

        this.TEST_2008_6_30_11_30_59_000000500.with((TemporalAdjuster) null);
    }

    @Test(expected = NullPointerException.class)
    public void test_withOffsetSameLocal_null() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameLocal(null);
    }

    @Test
    public void test_withOffsetSameInstant() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 30, 59), OFFSET_PTWO);
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void test_withOffsetSameInstant_null() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameInstant(null);
    }

    @Test
    public void test_withYear_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withYear(2007);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2007, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_withMonth_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMonth(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 1, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_withDayOfMonth_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withDayOfMonth(15);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 15), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_withDayOfYear_normal() {

        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.withDayOfYear(33);
        assertEquals(t, OffsetDateTime.of(LocalDate.of(2008, 2, 2), LocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test(expected = DateTimeException.class)
    public void test_withDayOfYear_illegal() {

        this.TEST_2008_6_30_11_30_59_000000500.withDayOfYear(367);
    }

    @Test(expected = DateTimeException.class)
    public void test_withDayOfYear_invalid() {

        OffsetDateTime.of(LocalDate.of(2007, 2, 2), LocalTime.of(11, 30), OFFSET_PONE).withDayOfYear(366);
    }

    @Test
    public void test_withHour_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withHour(15);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(15, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_withMinute_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMinute(15);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 15, 59), OFFSET_PONE));
    }

    @Test
    public void test_withSecond_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withSecond(15);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 15), OFFSET_PONE));
    }

    @Test
    public void test_withNanoOfSecond_normal() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        OffsetDateTime test = base.withNano(15);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 15), OFFSET_PONE));
    }

    @Test
    public void test_truncatedTo_normal() {

        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.truncatedTo(NANOS), this.TEST_2008_6_30_11_30_59_000000500);
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.truncatedTo(SECONDS),
                this.TEST_2008_6_30_11_30_59_000000500.withNano(0));
        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.truncatedTo(DAYS),
                this.TEST_2008_6_30_11_30_59_000000500.with(LocalTime.MIDNIGHT));
    }

    @Test(expected = NullPointerException.class)
    public void test_truncatedTo_null() {

        this.TEST_2008_6_30_11_30_59_000000500.truncatedTo(null);
    }

    @Test
    public void test_plus_Period() {

        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.plus(period);
        assertEquals(t, OffsetDateTime.of(LocalDate.of(2009, 1, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_plus_Duration() {

        Duration dur = Duration.ofSeconds(62, 3);
        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.plus(dur);
        assertEquals(t, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 32, 1, 503), OFFSET_PONE));
    }

    @Test
    public void test_plus_Duration_zero() {

        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.plus(Duration.ZERO);
        assertEquals(t, this.TEST_2008_6_30_11_30_59_000000500);
    }

    @Test(expected = NullPointerException.class)
    public void test_plus_Duration_null() {

        this.TEST_2008_6_30_11_30_59_000000500.plus((Duration) null);
    }

    @Test
    public void test_plusYears() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusYears(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2009, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusMonths() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMonths(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 7, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusWeeks() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusWeeks(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 7, 7), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusDays() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusDays(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 7, 1), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusHours() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusHours(13);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 7, 1), LocalTime.of(0, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusMinutes() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMinutes(30);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(12, 0, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusSeconds() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusSeconds(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 31, 0), OFFSET_PONE));
    }

    @Test
    public void test_plusNanos() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetDateTime test = base.plusNanos(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 1), OFFSET_PONE));
    }

    @Test
    public void test_minus_Period() {

        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.minus(period);
        assertEquals(t, OffsetDateTime.of(LocalDate.of(2007, 11, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_minus_Duration() {

        Duration dur = Duration.ofSeconds(62, 3);
        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.minus(dur);
        assertEquals(t, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 57, 497), OFFSET_PONE));
    }

    @Test
    public void test_minus_Duration_zero() {

        OffsetDateTime t = this.TEST_2008_6_30_11_30_59_000000500.minus(Duration.ZERO);
        assertEquals(t, this.TEST_2008_6_30_11_30_59_000000500);
    }

    @Test(expected = NullPointerException.class)
    public void test_minus_Duration_null() {

        this.TEST_2008_6_30_11_30_59_000000500.minus((Duration) null);
    }

    @Test
    public void test_minusYears() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusYears(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2007, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusMonths() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMonths(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 5, 30), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusWeeks() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusWeeks(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 23), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusDays() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusDays(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 29), LocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusHours() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusHours(13);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 29), LocalTime.of(22, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusMinutes() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMinutes(30);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusSeconds() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusSeconds(1);
        assertEquals(test, OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58), OFFSET_PONE));
    }

    @Test
    public void test_minusNanos() {

        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetDateTime test = base.minusNanos(1);
        assertEquals(test,
                OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58, 999999999), OFFSET_PONE));
    }

    @Test
    public void test_atZone() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(t.atZoneSameInstant(ZONE_PARIS),
                ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(15, 30)), ZONE_PARIS));
    }

    @Test(expected = NullPointerException.class)
    public void test_atZone_nullTimeZone() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        t.atZoneSameInstant((ZoneId) null);
    }

    @Test
    public void test_atZoneSimilarLocal() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS),
                ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30)), ZONE_PARIS));
    }

    @Test
    public void test_atZoneSimilarLocal_dstGap() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 4, 1), LocalTime.of(0, 0), OFFSET_MTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_GAZA),
                ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2007, 4, 1), LocalTime.of(1, 0)), ZONE_GAZA));
    }

    @Test
    public void test_atZone_dstOverlapSummer() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 10, 28), LocalTime.of(2, 30), OFFSET_PTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime(), t.toLocalDateTime());
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getOffset(), OFFSET_PTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getZone(), ZONE_PARIS);
    }

    @Test
    public void test_atZone_dstOverlapWinter() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2007, 10, 28), LocalTime.of(2, 30), OFFSET_PONE);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime(), t.toLocalDateTime());
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getOffset(), OFFSET_PONE);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getZone(), ZONE_PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void test_atZoneSimilarLocal_nullTimeZone() {

        OffsetDateTime t = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        t.atZoneSimilarLocal((ZoneId) null);
    }

    @Test
    public void test_toEpochSecond_afterEpoch() {

        for (int i = 0; i < 100000; i++) {
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneOffset.UTC)
                    .plusSeconds(i);
            assertEquals(a.toEpochSecond(), i);
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {

        for (int i = 0; i < 100000; i++) {
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneOffset.UTC)
                    .minusSeconds(i);
            assertEquals(a.toEpochSecond(), -i);
        }
    }

    @Test
    public void test_compareTo_timeMins() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 3), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 2), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_timeSecs() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 2), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 3), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_timeNanos() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 40, 4), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 29, 40, 5), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_offset() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PTWO);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_offsetNanos() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 40, 6), OFFSET_PTWO);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 40, 5), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_both() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 50), OFFSET_PTWO);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 20), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_bothNanos() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 20, 40, 4), OFFSET_PTWO);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 20, 40, 5), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 0), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0), OFFSET_PTWO);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) == 0, true);
    }

    @Test
    public void test_compareTo_max() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(Year.MAX_VALUE, 12, 31), LocalTime.of(23, 59), OFFSET_MONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(Year.MAX_VALUE, 12, 31), LocalTime.of(23, 59), OFFSET_MTWO);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_min() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(Year.MIN_VALUE, 1, 1), LocalTime.of(0, 0), OFFSET_PTWO);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(Year.MIN_VALUE, 1, 1), LocalTime.of(0, 0), OFFSET_PONE);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test(expected = NullPointerException.class)
    public void test_compareTo_null() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        a.compareTo(null);
    }

    @Test(expected = ClassCastException.class)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void compareToNonOffsetDateTime() {

        Comparable c = this.TEST_2008_6_30_11_30_59_000000500;
        c.compareTo(new Object());
    }

    @Test
    public void test_isBeforeIsAfterIsEqual1() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 58, 3), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 2), OFFSET_PONE);
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 2), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 3), OFFSET_PONE);
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual_instantComparison() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(10, 0), OFFSET_PONE);
        OffsetDateTime b = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 0), OFFSET_PTWO);
        assertEquals(a.isBefore(b), false);
        assertEquals(a.isEqual(b), true);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), true);
        assertEquals(b.isAfter(a), false);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test(expected = NullPointerException.class)
    public void test_isBefore_null() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isBefore(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isEqual_null() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isEqual(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isAfter_null() {

        OffsetDateTime a = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isAfter(null);
    }

    @Test
    public void test_equals_true() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            assertEquals(a.equals(b), true);
            assertEquals(a.hashCode() == b.hashCode(), true);
        }
    }

    @Test
    public void test_equals_false_year_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y + 1, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_hour_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            h = (h == 23 ? 22 : h);
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h + 1, m, s, n), OFFSET_PONE);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_minute_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            m = (m == 59 ? 58 : m);
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m + 1, s, n), OFFSET_PONE);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_second_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            s = (s == 59 ? 58 : s);
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s + 1, n), OFFSET_PONE);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_nano_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            n = (n == 999999999 ? 999999998 : n);
            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n + 1), OFFSET_PONE);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_offset_differs() {

        for (Object[] data : provider_sampleTimes()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];

            OffsetDateTime a = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PONE);
            OffsetDateTime b = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n), OFFSET_PTWO);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_itself_true() {

        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.equals(this.TEST_2008_6_30_11_30_59_000000500), true);
    }

    @Test
    public void test_equals_string_false() {

        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {

        assertEquals(this.TEST_2008_6_30_11_30_59_000000500.equals(null), false);
    }

    Object[][] provider_sampleToString() {

        return new Object[][] { { 2008, 6, 30, 11, 30, 59, 0, "Z", "2008-06-30T11:30:59Z" },
        { 2008, 6, 30, 11, 30, 59, 0, "+01:00", "2008-06-30T11:30:59+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999000000, "Z", "2008-06-30T11:30:59.999Z" },
        { 2008, 6, 30, 11, 30, 59, 999000000, "+01:00", "2008-06-30T11:30:59.999+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999000, "Z", "2008-06-30T11:30:59.000999Z" },
        { 2008, 6, 30, 11, 30, 59, 999000, "+01:00", "2008-06-30T11:30:59.000999+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999, "Z", "2008-06-30T11:30:59.000000999Z" },
        { 2008, 6, 30, 11, 30, 59, 999, "+01:00", "2008-06-30T11:30:59.000000999+01:00" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : provider_sampleToString()) {
            int y = (int) data[0];
            int o = (int) data[1];
            int d = (int) data[2];
            int h = (int) data[3];
            int m = (int) data[4];
            int s = (int) data[5];
            int n = (int) data[6];
            String offsetId = (String) data[7];
            String expected = (String) data[8];

            OffsetDateTime t = OffsetDateTime.of(LocalDate.of(y, o, d), LocalTime.of(h, m, s, n),
                    ZoneOffset.of(offsetId));
            String str = t.toString();
            assertEquals(str, expected);
        }
    }

    @Test
    public void test_format_formatter() {

        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        String t = OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), OFFSET_PONE).format(f);
        assertEquals(t, "2010 12 3 11 30 0");
    }

    @Test(expected = NullPointerException.class)
    public void test_format_formatter_null() {

        OffsetDateTime.of(LocalDate.of(2010, 12, 3), LocalTime.of(11, 30), OFFSET_PONE).format(null);
    }

}
