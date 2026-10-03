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

import static java.time.Month.JANUARY;
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
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
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
import java.time.Period;
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
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZonedDateTime.
 */
@TeaVMTest
public class TestZonedDateTime extends AbstractDateTimeTest {

    private static final ZoneOffset OFFSET_0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_0200 = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_0130 = ZoneOffset.of("+01:30");
    private static final ZoneOffset OFFSET_MAX = ZoneOffset.ofHours(18);
    private static final ZoneOffset OFFSET_MIN = ZoneOffset.ofHours(-18);

    private static final ZoneId ZONE_0100 = OFFSET_0100;
    private static final ZoneId ZONE_0200 = OFFSET_0200;
    private static final ZoneId ZONE_M0100 = ZoneOffset.ofHours(-1);
    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private LocalDateTime testParisGap2008x03x30x02x30;
    private LocalDateTime testParisOverlap2008x10x26x02x30;
    private LocalDateTime testLocal2008x06x30x11x30x59x500;
    private ZonedDateTime testDateTime;
    private ZonedDateTime testDateTimeParis;

    @BeforeEach
    public void setUp() {
        testLocal2008x06x30x11x30x59x500 = LocalDateTime.of(2008, 6, 30, 11, 30, 59, 500);
        testDateTime = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        testDateTimeParis = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_PARIS);
        testParisOverlap2008x10x26x02x30 = LocalDateTime.of(2008, 10, 26, 2, 30);
        testParisGap2008x03x30x02x30 = LocalDateTime.of(2008, 3, 30, 2, 30);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { testDateTime, };
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
        List<TemporalField> list = new ArrayList<>(Arrays.asList(ChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        ZonedDateTime expected = ZonedDateTime.now(Clock.systemDefaultZone());
        ZonedDateTime test = ZonedDateTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= 100000000) {
            // may be date change
            expected = ZonedDateTime.now(Clock.systemDefaultZone());
            test = ZonedDateTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void now_ZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.now((ZoneId) null));
    }

    @Test
    @Disabled("Flaky: depends on two subsequent clock readings being equal")
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        ZonedDateTime expected = ZonedDateTime.now(Clock.system(zone));
        ZonedDateTime test = ZonedDateTime.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = ZonedDateTime.now(Clock.system(zone));
            test = ZonedDateTime.now(zone);
        }
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.now((Clock) null));
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(1970, test.getYear());
            assertEquals(Month.JANUARY, test.getMonth());
            assertEquals(i < 24 * 60 * 60 ? 1 : 2, test.getDayOfMonth());
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(123456789, test.getNano());
            assertEquals(ZoneOffset.UTC, test.getOffset());
            assertEquals(ZoneOffset.UTC, test.getZone());
        }
    }

    @Test
    public void now_Clock_allSecsInDay_zone() {
        ZoneId zone = ZoneId.of("Europe/London");
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            ZonedDateTime expected = ZonedDateTime.ofInstant(instant, zone);
            Clock clock = Clock.fixed(expected.toInstant(), zone);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(expected, test);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {
        LocalTime expected = LocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(1969, test.getYear());
            assertEquals(Month.DECEMBER, test.getMonth());
            assertEquals(31, test.getDayOfMonth());
            expected = expected.minusSeconds(1);
            assertEquals(expected, test.toLocalTime());
            assertEquals(ZoneOffset.UTC, test.getOffset());
            assertEquals(ZoneOffset.UTC, test.getZone());
        }
    }

    @Test
    public void now_Clock_offsets() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(1970, 1, 1, 12, 0), ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base.toInstant(), offset);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals((12 + i) % 24, test.getHour());
            assertEquals(0, test.getMinute());
            assertEquals(0, test.getSecond());
            assertEquals(0, test.getNano());
            assertEquals(offset, test.getOffset());
            assertEquals(offset, test.getZone());
        }
    }

    //-----------------------------------------------------------------------
    // dateTime factories
    //-----------------------------------------------------------------------
    void check(ZonedDateTime test, int y, int m, int d, int h, int min, int s, int n, ZoneOffset offset, ZoneId zone) {
        assertEquals(y, test.getYear());
        assertEquals(m, test.getMonth().getValue());
        assertEquals(d, test.getDayOfMonth());
        assertEquals(h, test.getHour());
        assertEquals(min, test.getMinute());
        assertEquals(s, test.getSecond());
        assertEquals(n, test.getNano());
        assertEquals(offset, test.getOffset());
        assertEquals(zone, test.getZone());
    }

    //-----------------------------------------------------------------------
    // of(LocalDateTime, ZoneId)
    //-----------------------------------------------------------------------
    // TODO: tests of overlap/gap

    @Test
    public void factory_of_LocalDateTime() {
        LocalDateTime base = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        ZonedDateTime test = ZonedDateTime.of(base, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_of_LocalDateTime_nullDateTime() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.of((LocalDateTime) null, ZONE_PARIS));
    }

    @Test
    public void factory_of_LocalDateTime_nullZone() {
        LocalDateTime base = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        assertThrows(NullPointerException.class, () -> ZonedDateTime.of(base, null));
    }

    //-----------------------------------------------------------------------
    // ofInstant(Instant, ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_Instant_ZR() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 35).toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 35, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_ofInstant_Instant_ZO() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 45).toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_0200);
        check(test, 2008, 6, 30, 11, 30, 10, 45, OFFSET_0200, OFFSET_0200);
    }

    @Test
    public void factory_ofInstant_Instant_inGap() {
        Instant instant = testParisGap2008x03x30x02x30.toInstant(OFFSET_0100);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // one hour later in summer offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_earlier() {
        Instant instant = testParisOverlap2008x10x26x02x30.toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_later() {
        Instant instant = testParisOverlap2008x10x26x02x30.toInstant(OFFSET_0100);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS);  // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_invalidOffset() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500).toInstant(OFFSET_0130);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 12, 0, 10, 500, OFFSET_0200, ZONE_PARIS);  // corrected offset, thus altered time
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_0100);
            assertEquals(1970, test.getYear());
            assertEquals(Month.JANUARY, test.getMonth());
            assertEquals(1 + (i >= 23 * 60 * 60 ? 1 : 0), test.getDayOfMonth());
            assertEquals(((i / (60 * 60)) + 1) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
        }
    }

    @Test
    public void factory_ofInstant_allDaysInCycle() {
        // sanity check using different algorithm
        ZonedDateTime expected = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0).atZone(ZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            Instant instant = Instant.ofEpochSecond(i * 24L * 60L * 60L);
            ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(expected, test);
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void factory_ofInstant_minWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(Year.MIN_VALUE, test.getYear());
        assertEquals(1, test.getMonth().getValue());
        assertEquals(1, test.getDayOfMonth());
        assertEquals(OFFSET_MIN, test.getOffset());
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    @Test
    public void factory_ofInstant_minWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(Year.MIN_VALUE, test.getYear());
        assertEquals(1, test.getMonth().getValue());
        assertEquals(1, test.getDayOfMonth());
        assertEquals(OFFSET_MAX, test.getOffset());
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    @Test
    public void factory_ofInstant_maxWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(Year.MAX_VALUE, test.getYear());
        assertEquals(12, test.getMonth().getValue());
        assertEquals(31, test.getDayOfMonth());
        assertEquals(OFFSET_MIN, test.getOffset());
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(0, test.getNano());
    }

    @Test
    public void factory_ofInstant_maxWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(Year.MAX_VALUE, test.getYear());
        assertEquals(12, test.getMonth().getValue());
        assertEquals(31, test.getDayOfMonth());
        assertEquals(OFFSET_MAX, test.getOffset());
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(0, test.getNano());
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_maxInstantWithMaxOffset() {
        assertThrows(DateTimeException.class, () -> {
            Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
            ZonedDateTime.ofInstant(instant, OFFSET_MAX);
        });
    }

    @Test
    public void factory_ofInstant_maxInstantWithMinOffset() {
        assertThrows(DateTimeException.class, () -> {
            Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
            ZonedDateTime.ofInstant(instant, OFFSET_MIN);
        });
    }

    @Test
    public void factory_ofInstant_tooBig() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        long year = Year.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        assertThrows(DateTimeException.class, () -> ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    @Test
    public void factory_ofInstant_tooLow() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        assertThrows(DateTimeException.class, () -> ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    @Test
    public void factory_ofInstant_Instant_nullInstant() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.ofInstant((Instant) null, ZONE_0100));
    }

    @Test
    public void factory_ofInstant_Instant_nullZone() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.ofInstant(Instant.EPOCH, null));
    }

    //-----------------------------------------------------------------------
    // ofStrict(LocalDateTime, ZoneId, ZoneOffset)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofStrict_LDT_ZI_ZO() {
        LocalDateTime normal = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        ZonedDateTime test = ZonedDateTime.ofStrict(normal, OFFSET_0200, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_inGap() {
        assertThrows(DateTimeException.class, () -> {
            try {
                ZonedDateTime.ofStrict(testParisGap2008x03x30x02x30, OFFSET_0100, ZONE_PARIS);
            } catch (DateTimeException ex) {
                assertTrue(ex.getMessage().contains(" gap"));
                throw ex;
            }
        });
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_inOverlap_invalidOfset() {
        assertThrows(DateTimeException.class, () -> {
            try {
                ZonedDateTime.ofStrict(testParisOverlap2008x10x26x02x30, OFFSET_0130, ZONE_PARIS);
            } catch (DateTimeException ex) {
                assertTrue(ex.getMessage().contains(" is not valid for "));
                throw ex;
            }
        });
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_invalidOffset() {
        assertThrows(DateTimeException.class, () -> {
            try {
                ZonedDateTime.ofStrict(testLocal2008x06x30x11x30x59x500, OFFSET_0130, ZONE_PARIS);
            } catch (DateTimeException ex) {
                assertTrue(ex.getMessage().contains(" is not valid for "));
                throw ex;
            }
        });
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_nullLDT() {
        assertThrows(NullPointerException.class,
                () -> ZonedDateTime.ofStrict((LocalDateTime) null, OFFSET_0100, ZONE_PARIS));
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_nullZO() {
        assertThrows(NullPointerException.class,
                () -> ZonedDateTime.ofStrict(testLocal2008x06x30x11x30x59x500, null, ZONE_PARIS));
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO_nullZI() {
        assertThrows(NullPointerException.class,
                () -> ZonedDateTime.ofStrict(testLocal2008x06x30x11x30x59x500, OFFSET_0100, null));
    }

    //-----------------------------------------------------------------------
    // from(DateTimeAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_DateTimeAccessor_ZDT() {
        assertEquals(testDateTimeParis, ZonedDateTime.from(testDateTimeParis));
    }

    @Test
    public void factory_from_DateTimeAccessor_LDT_ZoneId() {
        assertEquals(testDateTimeParis, ZonedDateTime.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return testDateTimeParis.toLocalDateTime().isSupported(field);
            }
            @Override
            public long getLong(TemporalField field) {
                return testDateTimeParis.toLocalDateTime().getLong(field);
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.zoneId()) {
                    return (R) testDateTimeParis.getZone();
                }
                return TemporalAccessor.super.query(query);
            }
        }));
    }

    @Test
    public void factory_from_DateTimeAccessor_Instant_ZoneId() {
        assertEquals(testDateTimeParis, ZonedDateTime.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return field == INSTANT_SECONDS || field == NANO_OF_SECOND;
            }
            @Override
            public long getLong(TemporalField field) {
                return testDateTimeParis.toInstant().getLong(field);
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.zoneId()) {
                    return (R) testDateTimeParis.getZone();
                }
                return TemporalAccessor.super.query(query);
            }
        }));
    }

    @Test
    public void factory_from_DateTimeAccessor_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> ZonedDateTime.from(LocalTime.of(12, 30)));
    }

    @Test
    public void factory_from_DateTimeAccessor_null() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_parse(int y, int month, int d, int h, int m, int s, int n, String zoneId, String text) {
        ZonedDateTime t = ZonedDateTime.parse(text);
        assertEquals(y, t.getYear());
        assertEquals(month, t.getMonth().getValue());
        assertEquals(d, t.getDayOfMonth());
        assertEquals(h, t.getHour());
        assertEquals(m, t.getMinute());
        assertEquals(s, t.getSecond());
        assertEquals(n, t.getNano());
        assertEquals(zoneId, t.getZone().getId());
    }

    static Object[][] data_parseAdditional() {
        return new Object[][] {
                {"2012-06-30T12:30:40Z[GMT]", 2012, 6, 30, 12, 30, 40, 0, "GMT"},
                {"2012-06-30T12:30:40Z[UT]", 2012, 6, 30, 12, 30, 40, 0, "UT"},
                {"2012-06-30T12:30:40Z[UTC]", 2012, 6, 30, 12, 30, 40, 0, "UTC"},
                {"2012-06-30T12:30:40+01:00[+01:00]", 2012, 6, 30, 12, 30, 40, 0, "+01:00"},
                {"2012-06-30T12:30:40+01:00[GMT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT+01:00"},
                {"2012-06-30T12:30:40+01:00[UT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT+01:00"},
                {"2012-06-30T12:30:40+01:00[UTC+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC+01:00"},
                {"2012-06-30T12:30:40-01:00[-01:00]", 2012, 6, 30, 12, 30, 40, 0, "-01:00"},
                {"2012-06-30T12:30:40-01:00[GMT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT-01:00"},
                {"2012-06-30T12:30:40-01:00[UT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT-01:00"},
                {"2012-06-30T12:30:40-01:00[UTC-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC-01:00"},
                {"2012-06-30T12:30:40+01:00[Europe/London]", 2012, 6, 30, 12, 30, 40, 0, "Europe/London"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_parseAdditional")
    public void test_parseAdditional(String text, int y, int month, int d, int h, int m, int s, int n, String zoneId) {
        ZonedDateTime t = ZonedDateTime.parse(text);
        assertEquals(y, t.getYear());
        assertEquals(month, t.getMonth().getValue());
        assertEquals(d, t.getDayOfMonth());
        assertEquals(h, t.getHour());
        assertEquals(m, t.getMinute());
        assertEquals(s, t.getSecond());
        assertEquals(n, t.getNano());
        assertEquals(zoneId, t.getZone().getId());
    }

    @Test
    public void factory_parse_illegalValue() {
        assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("2008-06-32T11:15+01:00[Europe/Paris]"));
    }

    @Test
    public void factory_parse_invalidValue() {
        assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("2008-06-31T11:15+01:00[Europe/Paris]"));
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.parse((String) null));
    }

    static Object[][] data_parseOverlapRoundtrip() {
        return new Object[][] {
                {"2016-11-06T01:00-04:00[America/New_York]"},
                {"2016-10-30T02:00+02:00[Europe/Berlin]"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_parseOverlapRoundtrip")
    public void test_parseFormatRoundtripWithZoneAndOffset(String text) {
        ZonedDateTime start = ZonedDateTime.parse(text);
        for (int min = 0; min <= 60; min += 15) {
            ZonedDateTime  t = start.plusMinutes(min);
            assertEquals(ZonedDateTime.parse(t.toString()), t);
        }
    }

    static Object[][] data_parseOverlapToInstant() {
        return new Object[][] {
            {"2016-11-06T01:00-04:00[America/New_York]", "2016-11-06T05:00:00Z"},
            {"2016-11-06T01:30-04:00[America/New_York]", "2016-11-06T05:30:00Z"},
            {"2016-11-06T01:00-05:00[America/New_York]", "2016-11-06T06:00:00Z"},
            {"2016-11-06T01:30-05:00[America/New_York]", "2016-11-06T06:30:00Z"},
            {"2016-11-06T02:00-05:00[America/New_York]", "2016-11-06T07:00:00Z"},

            {"2016-10-30T02:00+02:00[Europe/Berlin]", "2016-10-30T00:00:00Z"},
            {"2016-10-30T02:30+02:00[Europe/Berlin]", "2016-10-30T00:30:00Z"},
            {"2016-10-30T02:00+01:00[Europe/Berlin]", "2016-10-30T01:00:00Z"},
            {"2016-10-30T02:30+01:00[Europe/Berlin]", "2016-10-30T01:30:00Z"},
            {"2016-10-30T03:00+01:00[Europe/Berlin]", "2016-10-30T02:00:00Z"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_parseOverlapToInstant")
    public void test_parseWithZoneAndOffsetToInstant(String z, String i) {
        ZonedDateTime zdt = ZonedDateTime.parse(z);
        Instant instant = Instant.parse(i);
        assertEquals(instant, zdt.toInstant());
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d H m s VV");
        ZonedDateTime test = ZonedDateTime.parse("2010 12 3 11 30 0 Europe/London", f);
        assertEquals(ZonedDateTime.of(LocalDateTime.of(2010, 12, 3, 11, 30), ZoneId.of("Europe/London")), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        assertThrows(NullPointerException.class, () -> ZonedDateTime.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> ZonedDateTime.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleTimes() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 20, 500, ZONE_0100},
            {2008, 6, 30, 11, 0, 0, 0, ZONE_0100},
            {2008, 6, 30, 11, 30, 20, 500, ZONE_PARIS},
            {2008, 6, 30, 11, 0, 0, 0, ZONE_PARIS},
            {2008, 6, 30, 23, 59, 59, 999999999, ZONE_0100},
            {-1, 1, 1, 0, 0, 0, 0, ZONE_0100},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_get(int y, int o, int d, int h, int m, int s, int n, ZoneId zone) {
        LocalDate localDate = LocalDate.of(y, o, d);
        LocalTime localTime = LocalTime.of(h, m, s, n);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        ZoneOffset offset = zone.getRules().getOffset(localDateTime);
        ZonedDateTime a = ZonedDateTime.of(localDateTime, zone);

        assertEquals(localDate.getYear(), a.getYear());
        assertEquals(localDate.getMonth(), a.getMonth());
        assertEquals(localDate.getDayOfMonth(), a.getDayOfMonth());
        assertEquals(localDate.getDayOfYear(), a.getDayOfYear());
        assertEquals(localDate.getDayOfWeek(), a.getDayOfWeek());

        assertEquals(localTime.getHour(), a.getHour());
        assertEquals(localTime.getMinute(), a.getMinute());
        assertEquals(localTime.getSecond(), a.getSecond());
        assertEquals(localTime.getNano(), a.getNano());

        assertEquals(localDate, a.toLocalDate());
        assertEquals(localTime, a.toLocalTime());
        assertEquals(localDateTime, a.toLocalDateTime());
        if (zone instanceof ZoneOffset) {
            assertEquals(localDateTime.toString() + offset.toString(), a.toString());
        } else {
            assertEquals(localDateTime.toString() + offset.toString() + "[" + zone.toString() + "]", a.toString());
        }
    }

    //-----------------------------------------------------------------------
    // get(DateTimeField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_DateTimeField() {
        ZonedDateTime test = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
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
    public void test_get_DateTimeField_long() {
        assertThrows(DateTimeException.class, () -> testDateTime.get(ChronoField.INSTANT_SECONDS));
    }

    @Test
    public void test_get_DateTimeField_invalidField() {
        assertThrows(DateTimeException.class, () -> testDateTime.get(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_get_DateTimeField_null() {
        assertThrows(NullPointerException.class, () -> testDateTime.get((TemporalField) null));
    }

    //-----------------------------------------------------------------------
    // getLong(DateTimeField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong_DateTimeField() {
        ZonedDateTime test = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
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

        assertEquals(3600, test.getLong(ChronoField.OFFSET_SECONDS));
        assertEquals(test.toEpochSecond(), test.getLong(ChronoField.INSTANT_SECONDS));
    }

    @Test
    public void test_getLong_DateTimeField_invalidField() {
        assertThrows(DateTimeException.class, () -> testDateTime.getLong(MockFieldNoValue.INSTANCE));
    }

    @Test
    public void test_getLong_DateTimeField_null() {
        assertThrows(NullPointerException.class, () -> testDateTime.getLong((TemporalField) null));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, testDateTime.query(TemporalQueries.chronology()));
        assertEquals(testDateTime.toLocalDate(), testDateTime.query(TemporalQueries.localDate()));
        assertEquals(testDateTime.toLocalTime(), testDateTime.query(TemporalQueries.localTime()));
        assertEquals(testDateTime.getOffset(), testDateTime.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.NANOS, testDateTime.query(TemporalQueries.precision()));
        assertEquals(testDateTime.getZone(), testDateTime.query(TemporalQueries.zone()));
        assertEquals(testDateTime.getZone(), testDateTime.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> testDateTime.query(null));
    }

    //-----------------------------------------------------------------------
    // withEarlierOffsetAtOverlap()
    //-----------------------------------------------------------------------
    @Test
    public void test_withEarlierOffsetAtOverlap_notAtOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testLocal2008x06x30x11x30x59x500, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(base, test);  // not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testParisOverlap2008x10x26x02x30, OFFSET_0100, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(OFFSET_0200, test.getOffset());  // offset changed to earlier
        assertEquals(base.toLocalDateTime(), test.toLocalDateTime());  // date-time not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap_noChange() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testParisOverlap2008x10x26x02x30, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(base, test);  // not changed
    }

    //-----------------------------------------------------------------------
    // withLaterOffsetAtOverlap()
    //-----------------------------------------------------------------------
    @Test
    public void test_withLaterOffsetAtOverlap_notAtOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testLocal2008x06x30x11x30x59x500, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(base, test);  // not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testParisOverlap2008x10x26x02x30, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(OFFSET_0100, test.getOffset());  // offset changed to later
        assertEquals(base.toLocalDateTime(), test.toLocalDateTime());  // date-time not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap_noChange() {
        ZonedDateTime base = ZonedDateTime.ofStrict(testParisOverlap2008x10x26x02x30, OFFSET_0100, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(base, test);  // not changed
    }

    //-----------------------------------------------------------------------
    // withZoneSameLocal(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneSameLocal() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withZoneSameLocal(ZONE_0200);
        assertEquals(base.toLocalDateTime(), test.toLocalDateTime());
    }

    @Test
    public void test_withZoneSameLocal_noChange() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withZoneSameLocal(ZONE_0100);
        assertEquals(base, test);
    }

    @Test
    public void test_withZoneSameLocal_retainOffset1() {
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 2, 1, 30, 59, 0);  // overlap
        ZonedDateTime base = ZonedDateTime.of(ldt, ZoneId.of("UTC-04:00"));
        ZonedDateTime test = base.withZoneSameLocal(ZoneId.of("America/New_York"));
        assertEquals(ZoneOffset.ofHours(-4), base.getOffset());
        assertEquals(ZoneOffset.ofHours(-4), test.getOffset());
    }

    @Test
    public void test_withZoneSameLocal_retainOffset2() {
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 2, 1, 30, 59, 0);  // overlap
        ZonedDateTime base = ZonedDateTime.of(ldt, ZoneId.of("UTC-05:00"));
        ZonedDateTime test = base.withZoneSameLocal(ZoneId.of("America/New_York"));
        assertEquals(ZoneOffset.ofHours(-5), base.getOffset());
        assertEquals(ZoneOffset.ofHours(-5), test.getOffset());
    }

    @Test
    public void test_withZoneSameLocal_null() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        assertThrows(NullPointerException.class, () -> base.withZoneSameLocal(null));
    }

    //-----------------------------------------------------------------------
    // withZoneSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneSameInstant() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withZoneSameInstant(ZONE_0200);
        ZonedDateTime expected = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.plusHours(1), ZONE_0200);
        assertEquals(expected, test);
    }

    @Test
    public void test_withZoneSameInstant_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withZoneSameInstant(ZONE_0100);
        assertEquals(base, test);
    }

    @Test
    public void test_withZoneSameInstant_null() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        assertThrows(NullPointerException.class, () -> base.withZoneSameInstant(null));
    }

    //-----------------------------------------------------------------------
    // withFixedOffsetZone()
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneLocked() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_PARIS);
        ZonedDateTime test = base.withFixedOffsetZone();
        ZonedDateTime expected = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0200);
        assertEquals(expected, test);
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_WithAdjuster_LocalDateTime_sameOffset() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDateTime.of(2012, 7, 15, 14, 30));
        check(test, 2012, 7, 15, 14, 30, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalDateTime_adjustedOffset() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDateTime.of(2012, 1, 15, 14, 30));
        check(test, 2012, 1, 15, 14, 30, 0, 0, OFFSET_0100, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalDate() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDate.of(2012, 7, 28));
        check(test, 2012, 7, 28, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalTime() {
        ZonedDateTime base = ZonedDateTime.of(testParisOverlap2008x10x26x02x30, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalTime.of(2, 29));
        check(test, 2008, 10, 26, 2, 29, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Year() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.with(Year.of(2007));
        assertEquals(ZonedDateTime.of(ldt.withYear(2007), ZONE_0100), test);
    }

    @Test
    public void test_with_WithAdjuster_Month_adjustedDayOfMonth() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(Month.JUNE);
        check(test, 2012, 6, 30, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Offset_same() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(ZoneOffset.ofHours(2));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Offset_ignored() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(ZoneOffset.ofHours(1));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);  // offset ignored
    }

    @Test
    public void test_with_WithAdjuster_LocalDate_retainOffset1() {
        ZoneId newYork = ZoneId.of("America/New_York");
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 1, 1, 30);
        ZonedDateTime base = ZonedDateTime.of(ldt, newYork);
        assertEquals(ZoneOffset.ofHours(-4), base.getOffset());
        ZonedDateTime test = base.with(LocalDate.of(2008, 11, 2));
        assertEquals(ZoneOffset.ofHours(-4), test.getOffset());
    }

    @Test
    public void test_with_WithAdjuster_LocalDate_retainOffset2() {
        ZoneId newYork = ZoneId.of("America/New_York");
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 3, 1, 30);
        ZonedDateTime base = ZonedDateTime.of(ldt, newYork);
        assertEquals(ZoneOffset.ofHours(-5), base.getOffset());
        ZonedDateTime test = base.with(LocalDate.of(2008, 11, 2));
        assertEquals(ZoneOffset.ofHours(-5), test.getOffset());
    }

    @Test
    public void test_with_WithAdjuster_null() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        assertThrows(NullPointerException.class, () -> base.with((TemporalAdjuster) null));
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withYear(2007);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withYear(2007), ZONE_0100), test);
    }

    @Test
    public void test_withYear_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withYear(2008);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // with(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_Month_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.with(JANUARY);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withMonth(1), ZONE_0100), test);
    }

    @Test
    public void test_withMonth_Month_null() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        assertThrows(NullPointerException.class, () -> base.with((Month) null));
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withMonth(1);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withMonth(1), ZONE_0100), test);
    }

    @Test
    public void test_withMonth_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withMonth(6);
        assertEquals(base, test);
    }

    @Test
    public void test_withMonth_tooBig() {
        assertThrows(DateTimeException.class, () -> testDateTime.withMonth(13));
    }

    @Test
    public void test_withMonth_tooSmall() {
        assertThrows(DateTimeException.class, () -> testDateTime.withMonth(0));
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withDayOfMonth(15);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withDayOfMonth(15), ZONE_0100), test);
    }

    @Test
    public void test_withDayOfMonth_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withDayOfMonth(30);
        assertEquals(base, test);
    }

    @Test
    public void test_withDayOfMonth_tooBig() {
        assertThrows(DateTimeException.class,
                () -> LocalDateTime.of(2007, 7, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(32));
    }

    @Test
    public void test_withDayOfMonth_tooSmall() {
        assertThrows(DateTimeException.class, () -> testDateTime.withDayOfMonth(0));
    }

    @Test
    public void test_withDayOfMonth_invalid31() {
        assertThrows(DateTimeException.class,
                () -> LocalDateTime.of(2007, 6, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(31));
    }

    //-----------------------------------------------------------------------
    // withDayOfYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfYear_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withDayOfYear(33);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withDayOfYear(33), ZONE_0100), test);
    }

    @Test
    public void test_withDayOfYear_noChange() {
        LocalDateTime ldt = LocalDateTime.of(2008, 2, 5, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withDayOfYear(36);
        assertEquals(base, test);
    }

    @Test
    public void test_withDayOfYear_tooBig() {
        assertThrows(DateTimeException.class, () -> testDateTime.withDayOfYear(367));
    }

    @Test
    public void test_withDayOfYear_tooSmall() {
        assertThrows(DateTimeException.class, () -> testDateTime.withDayOfYear(0));
    }

    @Test
    public void test_withDayOfYear_invalid366() {
        assertThrows(DateTimeException.class,
                () -> LocalDateTime.of(2007, 2, 2, 11, 30).atZone(ZONE_PARIS).withDayOfYear(366));
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withHour(15);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withHour(15), ZONE_0100), test);
    }

    @Test
    public void test_withHour_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withHour(11);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withMinute(15);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withMinute(15), ZONE_0100), test);
    }

    @Test
    public void test_withMinute_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withMinute(30);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withSecond(12);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withSecond(12), ZONE_0100), test);
    }

    @Test
    public void test_withSecond_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withSecond(59);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withNano(15);
        assertEquals(ZonedDateTime.of(testLocal2008x06x30x11x30x59x500.withNano(15), ZONE_0100), test);
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        ZonedDateTime base = ZonedDateTime.of(testLocal2008x06x30x11x30x59x500, ZONE_0100);
        ZonedDateTime test = base.withNano(500);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plus/minus
    //-----------------------------------------------------------------------
    static Object[][] data_plusDays() {
        return new Object[][] {
            // normal
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0,
                    dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1,
                    dateTime(2008, 7, 1, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1,
                    dateTime(2008, 6, 29, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            // skip over gap
            {dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
                    dateTime(2008, 3, 31, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
                    dateTime(2008, 3, 29, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // land in gap
            {dateTime(2008, 3, 29, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
                    dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 31, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
                    dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            // skip over overlap
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
                    dateTime(2008, 10, 27, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 25, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
                    dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // land in overlap
            {dateTime(2008, 10, 25, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
                    dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 10, 27, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), -1,
                    dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
        };
    }

    static Object[][] data_plusTime() {
        return new Object[][] {
            // normal
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0,
                    dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1,
                    dateTime(2008, 7, 1, 0, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1,
                    dateTime(2008, 6, 30, 22, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            // gap
            {dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
                    dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
                    dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // overlap
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
                    dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2,
                    dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 3,
                    dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
                    dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2,
                    dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
        };
    }

    //-----------------------------------------------------------------------
    // plus(adjuster)
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusDays")
    public void test_plus_adjuster_Period_days(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(Period.ofDays((int) amount)));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_adjuster_Period_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(Duration.ofHours(amount)));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_adjuster_Duration_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(Duration.ofHours(amount)));
    }

    @Test
    public void test_plus_adjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2009, 1, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(expected, t.plus(period));
    }

    @Test
    public void test_plus_adjuster_Duration() {
        Duration duration = Duration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 16, 36, 5, 500), ZONE_0100);
        assertEquals(expected, t.plus(duration));
    }

    @Test
    public void test_plus_adjuster_Period_zero() {
        ZonedDateTime t = testDateTime.plus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(testDateTime, t);
    }

    @Test
    public void test_plus_adjuster_Duration_zero() {
        ZonedDateTime t = testDateTime.plus(Duration.ZERO);
        assertEquals(testDateTime, t);
    }

    @Test
    public void test_plus_adjuster_null() {
        assertThrows(NullPointerException.class, () -> testDateTime.plus(null));
    }

    //-----------------------------------------------------------------------
    // plus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_longUnit_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(amount, HOURS));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_longUnit_minutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(amount * 60, MINUTES));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_longUnit_seconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(amount * 3600, SECONDS));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plus_longUnit_nanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plus(amount * 3600000000000L, NANOS));
    }

    @Test
    public void test_plus_longUnit_null() {
        assertThrows(NullPointerException.class, () -> testDateTimeParis.plus(0, null));
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusYears(1);
        assertEquals(ZonedDateTime.of(ldt.plusYears(1), ZONE_0100), test);
    }

    @Test
    public void test_plusYears_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusYears(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMonths(1);
        assertEquals(ZonedDateTime.of(ldt.plusMonths(1), ZONE_0100), test);
    }

    @Test
    public void test_plusMonths_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMonths(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusWeeks() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusWeeks(1);
        assertEquals(ZonedDateTime.of(ldt.plusWeeks(1), ZONE_0100), test);
    }

    @Test
    public void test_plusWeeks_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusWeeks(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusDays")
    public void test_plusDays(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plusDays(amount));
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plusHours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plusHours(amount));
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plusMinutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plusMinutes(amount * 60));
    }

    @Test
    public void test_plusMinutes_minutes() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMinutes(30);
        assertEquals(ZonedDateTime.of(ldt.plusMinutes(30), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plusSeconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plusSeconds(amount * 3600));
    }

    @Test
    public void test_plusSeconds_seconds() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusSeconds(1);
        assertEquals(ZonedDateTime.of(ldt.plusSeconds(1), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_plusNanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.plusNanos(amount * 3600000000000L));
    }

    @Test
    public void test_plusNanos_nanos() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusNanos(1);
        assertEquals(ZonedDateTime.of(ldt.plusNanos(1), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // minus(adjuster)
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusDays")
    public void test_minus_adjuster_Period_days(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minus(Period.ofDays((int) -amount)));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minus_adjuster_Period_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minus(Duration.ofHours(-amount)));
    }

    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minus_adjuster_Duration_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minus(Duration.ofHours(-amount)));
    }

    @Test
    public void test_minus_adjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2007, 11, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(expected, t.minus(period));
    }

    @Test
    public void test_minus_adjuster_Duration() {
        Duration duration = Duration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 8, 25, 53, 500), ZONE_0100);
        assertEquals(expected, t.minus(duration));
    }

    @Test
    public void test_minus_adjuster_Period_zero() {
        ZonedDateTime t = testDateTime.minus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(testDateTime, t);
    }

    @Test
    public void test_minus_adjuster_Duration_zero() {
        ZonedDateTime t = testDateTime.minus(Duration.ZERO);
        assertEquals(testDateTime, t);
    }

    @Test
    public void test_minus_adjuster_null() {
        assertThrows(NullPointerException.class, () -> testDateTime.minus(null));
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusYears(1);
        assertEquals(ZonedDateTime.of(ldt.minusYears(1), ZONE_0100), test);
    }

    @Test
    public void test_minusYears_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusYears(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMonths(1);
        assertEquals(ZonedDateTime.of(ldt.minusMonths(1), ZONE_0100), test);
    }

    @Test
    public void test_minusMonths_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMonths(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusWeeks() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusWeeks(1);
        assertEquals(ZonedDateTime.of(ldt.minusWeeks(1), ZONE_0100), test);
    }

    @Test
    public void test_minusWeeks_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusWeeks(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusDays")
    public void test_minusDays(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minusDays(-amount));
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minusHours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minusHours(-amount));
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minusMinutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minusMinutes(-amount * 60));
    }

    @Test
    public void test_minusMinutes_minutes() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMinutes(30);
        assertEquals(ZonedDateTime.of(ldt.minusMinutes(30), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minusSeconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minusSeconds(-amount * 3600));
    }

    @Test
    public void test_minusSeconds_seconds() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusSeconds(1);
        assertEquals(ZonedDateTime.of(ldt.minusSeconds(1), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_plusTime")
    public void test_minusNanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(expected, base.minusNanos(-amount * 3600000000000L));
    }

    @Test
    public void test_minusNanos_nanos() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusNanos(1);
        assertEquals(ZonedDateTime.of(ldt.minusNanos(1), ZONE_0100), test);
    }

    //-----------------------------------------------------------------------
    // toInstant()
    //-----------------------------------------------------------------------
    static Object[][] data_toInstant() {
        return new Object[][] {
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 0L, 0},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1), 0L, 1},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 999999999), 0L, 999999999},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 1, 0), 1L, 0},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 1, 1), 1L, 1},
            {LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999999999), -1L, 999999999},
            {LocalDateTime.of(1970, 1, 2, 0, 0), 24L * 60L * 60L, 0},
            {LocalDateTime.of(1969, 12, 31, 0, 0), -24L * 60L * 60L, 0},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toInstant_UTC(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(expectedEpSec, test.getEpochSecond());
        assertEquals(expectedNos, test.getNano());
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toInstant_P0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_0100);
        Instant test = dt.toInstant();
        assertEquals(expectedEpSec - 3600, test.getEpochSecond());
        assertEquals(expectedNos, test.getNano());
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toInstant_M0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_M0100);
        Instant test = dt.toInstant();
        assertEquals(expectedEpSec + 3600, test.getEpochSecond());
        assertEquals(expectedNos, test.getNano());
    }

    //-----------------------------------------------------------------------
    // toEpochSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochSecond_afterEpoch() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(i, a.toEpochSecond());
            ldt = ldt.plusSeconds(1);
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(-i, a.toEpochSecond());
            ldt = ldt.minusSeconds(1);
        }
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toEpochSecond_UTC(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZoneOffset.UTC);
        assertEquals(expectedEpSec, dt.toEpochSecond());
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toEpochSecond_P0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_0100);
        assertEquals(expectedEpSec - 3600, dt.toEpochSecond());
    }

    @ParameterizedTest
    @MethodSource("data_toInstant")
    public void test_toEpochSecond_M0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_M0100);
        assertEquals(expectedEpSec + 3600, dt.toEpochSecond());
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_time1() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 39), ZONE_0100);
        // a is before b due to time
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 41), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_time2() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 40, 4), ZONE_0100);
        // a is before b due to time
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 40, 5), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_offset1() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 41), ZONE_0200);
        // a is before b due to offset
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 39), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_offset2() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 40, 5), ZoneId.of("UTC+01:01"));
        // a is before b due to offset
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 30, 40, 4), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_both() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 50), ZONE_0200);
        // a is before b on instant scale
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 20), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_bothNanos() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 20, 40, 5), ZONE_0200);
        // a is before b on instant scale
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 10, 20, 40, 6), ZONE_0100);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_hourDifference() {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 10, 0), ZONE_0100);
        // a is before b despite being same time-line time
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 11, 0), ZONE_0200);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    public void test_compareTo_null() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_0100);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    //-----------------------------------------------------------------------
    // isBefore()
    //-----------------------------------------------------------------------
    static Object[][] data_isBefore() {
        return new Object[][] {
            {11, 30, ZONE_0100, 11, 31, ZONE_0100, true}, // a is before b due to time
            {11, 30, ZONE_0200, 11, 30, ZONE_0100, true}, // a is before b due to offset
            {11, 30, ZONE_0200, 10, 30, ZONE_0100, false}, // a is equal b due to same instant
        };
    }

    @ParameterizedTest
    @MethodSource("data_isBefore")
    public void test_isBefore(int hour1, int minute1, ZoneId zone1, int hour2, int minute2, ZoneId zone2,
            boolean expected) {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, hour1, minute1), zone1);
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, hour2, minute2), zone2);
        assertEquals(expected, a.isBefore(b));
        assertFalse(b.isBefore(a));
        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));
    }

    @Test
    public void test_isBefore_null() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_0100);
        assertThrows(NullPointerException.class, () -> a.isBefore(null));
    }

    //-----------------------------------------------------------------------
    // isAfter()
    //-----------------------------------------------------------------------
    static Object[][] data_isAfter() {
        return new Object[][] {
            {11, 31, ZONE_0100, 11, 30, ZONE_0100, true}, // a is after b due to time
            {11, 30, ZONE_0100, 11, 30, ZONE_0200, true}, // a is after b due to offset
            {11, 30, ZONE_0200, 10, 30, ZONE_0100, false}, // a is equal b due to same instant
        };
    }

    @ParameterizedTest
    @MethodSource("data_isAfter")
    public void test_isAfter(int hour1, int minute1, ZoneId zone1, int hour2, int minute2, ZoneId zone2,
            boolean expected) {
        ZonedDateTime a = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, hour1, minute1), zone1);
        ZonedDateTime b = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, hour2, minute2), zone2);
        assertEquals(expected, a.isAfter(b));
        assertFalse(b.isAfter(a));
        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isAfter_null() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_0100);
        assertThrows(NullPointerException.class, () -> a.isAfter(null));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_true(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        assertTrue(a.equals(b));
        assertTrue(a.hashCode() == b.hashCode());
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_year_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y + 1, o, d, h, m, s, n), ZONE_0100);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_hour_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        h = h == 23 ? 22 : h;
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h + 1, m, s, n), ZONE_0100);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_minute_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        m = m == 59 ? 58 : m;
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m + 1, s, n), ZONE_0100);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_second_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        s = s == 59 ? 58 : s;
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s + 1, n), ZONE_0100);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_nano_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        n = n == 999999999 ? 999999998 : n;
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n + 1), ZONE_0100);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_offset_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0200);
        assertFalse(a.equals(b));
    }

    @Test
    public void test_equals_itself_true() {
        assertTrue(testDateTime.equals(testDateTime));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(testDateTime.equals("2007-07-15"));
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

            {2008, 6, 30, 11, 30, 59, 999, "Europe/London", "2008-06-30T11:30:59.000000999+01:00[Europe/London]"},
            {2008, 6, 30, 11, 30, 59, 999, "Europe/Paris", "2008-06-30T11:30:59.000000999+02:00[Europe/Paris]"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int y, int o, int d, int h, int m, int s, int n, String zoneId, String expected) {
        ZonedDateTime t = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZoneId.of(zoneId));
        String str = t.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        String t = ZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(f);
        assertEquals("2010 12 3 11 30 0", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class,
                () -> ZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(null));
    }

    //-------------------------------------------------------------------------
    private static LocalDateTime dateTime(
            int year, int month, int dayOfMonth,
            int hour, int minute) {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    private static LocalDateTime dateTime(
                    int year, int month, int dayOfMonth,
                    int hour, int minute, int second, int nanoOfSecond) {
                return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
            }

    private static ZonedDateTime dateTime(
            int year, int month, int dayOfMonth,
            int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset, ZoneId zoneId) {
        return ZonedDateTime.ofStrict(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
                offset, zoneId);
    }

}
