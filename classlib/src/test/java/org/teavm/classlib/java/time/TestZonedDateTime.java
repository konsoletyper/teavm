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
import static org.teavm.classlib.java.time.TMonth.JANUARY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HOURS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MINUTES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

public class TestZonedDateTime extends AbstractDateTimeTest {

    private static final TZoneOffset OFFSET_0100 = TZoneOffset.ofHours(1);

    private static final TZoneOffset OFFSET_0200 = TZoneOffset.ofHours(2);

    private static final TZoneOffset OFFSET_0130 = TZoneOffset.of("+01:30");

    private static final TZoneOffset OFFSET_MAX = TZoneOffset.ofHours(18);

    private static final TZoneOffset OFFSET_MIN = TZoneOffset.ofHours(-18);

    private static final TZoneId ZONE_0100 = OFFSET_0100;

    private static final TZoneId ZONE_0200 = OFFSET_0200;

    private static final TZoneId ZONE_M0100 = TZoneOffset.ofHours(-1);

    private static final TZoneId ZONE_PARIS = TZoneId.of("Europe/Paris");

    private TLocalDateTime TEST_PARIS_GAP_2008_03_30_02_30;

    private TLocalDateTime TEST_PARIS_OVERLAP_2008_10_26_02_30;

    private TLocalDateTime TEST_LOCAL_2008_06_30_11_30_59_500;

    private TZonedDateTime TEST_DATE_TIME;

    private TZonedDateTime TEST_DATE_TIME_PARIS;

    @Before
    public void setUp() {

        this.TEST_LOCAL_2008_06_30_11_30_59_500 = TLocalDateTime.of(2008, 6, 30, 11, 30, 59, 500);
        this.TEST_DATE_TIME = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        this.TEST_DATE_TIME_PARIS = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        this.TEST_PARIS_OVERLAP_2008_10_26_02_30 = TLocalDateTime.of(2008, 10, 26, 2, 30);
        this.TEST_PARIS_GAP_2008_03_30_02_30 = TLocalDateTime.of(2008, 3, 30, 2, 30);
    }

    @Override
    protected List<TTemporalAccessor> samples() {

        TTemporalAccessor[] array = { this.TEST_DATE_TIME, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {

        TTemporalField[] array = { NANO_OF_SECOND, NANO_OF_DAY, MICRO_OF_SECOND, MICRO_OF_DAY, MILLI_OF_SECOND,
        MILLI_OF_DAY, SECOND_OF_MINUTE, SECOND_OF_DAY, MINUTE_OF_HOUR, MINUTE_OF_DAY, CLOCK_HOUR_OF_AMPM, HOUR_OF_AMPM,
        CLOCK_HOUR_OF_DAY, HOUR_OF_DAY, AMPM_OF_DAY, DAY_OF_WEEK, ALIGNED_DAY_OF_WEEK_IN_MONTH,
        ALIGNED_DAY_OF_WEEK_IN_YEAR, DAY_OF_MONTH, DAY_OF_YEAR, EPOCH_DAY, ALIGNED_WEEK_OF_MONTH, ALIGNED_WEEK_OF_YEAR,
        MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR_OF_ERA, YEAR, ERA, OFFSET_SECONDS, INSTANT_SECONDS,
        TJulianFields.JULIAN_DAY, TJulianFields.MODIFIED_JULIAN_DAY, TJulianFields.RATA_DIE, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {

        List<TTemporalField> list = new ArrayList<>(Arrays.asList(TChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    @Test
    public void now() {

        TZonedDateTime expected = TZonedDateTime.now(TClock.systemDefaultZone());
        TZonedDateTime test = TZonedDateTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= 100000000) {
            // may be date change
            expected = TZonedDateTime.now(TClock.systemDefaultZone());
            test = TZonedDateTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < 100000000); // less than 0.1 secs
    }

    @Test(expected = NullPointerException.class)
    public void now_ZoneId_nullZoneId() {

        TZonedDateTime.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {

        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TZonedDateTime expected = TZonedDateTime.now(TClock.system(zone));
        TZonedDateTime test = TZonedDateTime.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TZonedDateTime.now(TClock.system(zone));
            test = TZonedDateTime.now(zone);
        }
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullClock() {

        TZonedDateTime.now((TClock) null);
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {

        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TZonedDateTime test = TZonedDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 123456789);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
            assertEquals(test.getZone(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_zone() {

        TZoneId zone = TZoneId.of("Europe/London");
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TZonedDateTime expected = TZonedDateTime.ofInstant(instant, zone);
            TClock clock = TClock.fixed(expected.toInstant(), zone);
            TZonedDateTime test = TZonedDateTime.now(clock);
            assertEquals(test, expected);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {

        TLocalTime expected = TLocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TZonedDateTime test = TZonedDateTime.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), TMonth.DECEMBER);
            assertEquals(test.getDayOfMonth(), 31);
            expected = expected.minusSeconds(1);
            assertEquals(test.toLocalTime(), expected);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
            assertEquals(test.getZone(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {

        TZonedDateTime base = TZonedDateTime.of(TLocalDateTime.of(1970, 1, 1, 12, 0), TZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            TZoneOffset offset = TZoneOffset.ofHours(i);
            TClock clock = TClock.fixed(base.toInstant(), offset);
            TZonedDateTime test = TZonedDateTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
            assertEquals(test.getZone(), offset);
        }
    }

    void check(TZonedDateTime test, int y, int m, int d, int h, int min, int s, int n, TZoneOffset offset,
            TZoneId zone) {

        assertEquals(test.getYear(), y);
        assertEquals(test.getMonth().getValue(), m);
        assertEquals(test.getDayOfMonth(), d);
        assertEquals(test.getHour(), h);
        assertEquals(test.getMinute(), min);
        assertEquals(test.getSecond(), s);
        assertEquals(test.getNano(), n);
        assertEquals(test.getOffset(), offset);
        assertEquals(test.getZone(), zone);
    }

    @Test
    public void factory_of_LocalDateTime() {

        TLocalDateTime base = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        TZonedDateTime test = TZonedDateTime.of(base, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateTime_nullDateTime() {

        TZonedDateTime.of((TLocalDateTime) null, ZONE_PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_LocalDateTime_nullZone() {

        TLocalDateTime base = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        TZonedDateTime.of(base, null);
    }

    @Test
    public void factory_ofInstant_Instant_ZR() {

        TInstant instant = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 35).toInstant(OFFSET_0200);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 35, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_ofInstant_Instant_ZO() {

        TInstant instant = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 45).toInstant(OFFSET_0200);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_0200);
        check(test, 2008, 6, 30, 11, 30, 10, 45, OFFSET_0200, OFFSET_0200);
    }

    @Test
    public void factory_ofInstant_Instant_inGap() {

        TInstant instant = this.TEST_PARIS_GAP_2008_03_30_02_30.toInstant(OFFSET_0100);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS); // one hour later in summer offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_earlier() {

        TInstant instant = this.TEST_PARIS_OVERLAP_2008_10_26_02_30.toInstant(OFFSET_0200);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS); // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_later() {

        TInstant instant = this.TEST_PARIS_OVERLAP_2008_10_26_02_30.toInstant(OFFSET_0100);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS); // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_invalidOffset() {

        TInstant instant = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500).toInstant(OFFSET_0130);
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 12, 0, 10, 500, OFFSET_0200, ZONE_PARIS); // corrected offset, thus altered time
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {

        for (int i = 0; i < (24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i);
            TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_0100);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), 1 + (i >= 23 * 60 * 60 ? 1 : 0));
            assertEquals(test.getHour(), ((i / (60 * 60)) + 1) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
        }
    }

    @Test
    public void factory_ofInstant_allDaysInCycle() {

        // sanity check using different algorithm
        TZonedDateTime expected = TLocalDateTime.of(1970, 1, 1, 0, 0, 0, 0).atZone(TZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            TInstant instant = TInstant.ofEpochSecond(i * 24L * 60L * 60L);
            TZonedDateTime test = TZonedDateTime.ofInstant(instant, TZoneOffset.UTC);
            assertEquals(test, expected);
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void factory_ofInstant_minWithMinOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), TYear.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_minWithMaxOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), TYear.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMinOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), TYear.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMaxOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        TZonedDateTime test = TZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), TYear.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_maxInstantWithMaxOffset() {

        TInstant instant = TInstant.ofEpochSecond(Long.MAX_VALUE);
        TZonedDateTime.ofInstant(instant, OFFSET_MAX);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_maxInstantWithMinOffset() {

        TInstant instant = TInstant.ofEpochSecond(Long.MAX_VALUE);
        TZonedDateTime.ofInstant(instant, OFFSET_MIN);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_tooBig() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        long year = TYear.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L);
        TZonedDateTime.ofInstant(instant, TZoneOffset.UTC);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_tooLow() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L);
        TZonedDateTime.ofInstant(instant, TZoneOffset.UTC);
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofInstant_Instant_nullInstant() {

        TZonedDateTime.ofInstant((TInstant) null, ZONE_0100);
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofInstant_Instant_nullZone() {

        TZonedDateTime.ofInstant(TInstant.EPOCH, null);
    }

    @Test
    public void factory_ofStrict_LDT_ZI_ZO() {

        TLocalDateTime normal = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        TZonedDateTime test = TZonedDateTime.ofStrict(normal, OFFSET_0200, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_inGap() {

        try {
            TZonedDateTime.ofStrict(this.TEST_PARIS_GAP_2008_03_30_02_30, OFFSET_0100, ZONE_PARIS);
        } catch (TDateTimeException ex) {
            assertEquals(ex.getMessage().contains(" gap"), true);
            throw ex;
        }
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_inOverlap_invalidOfset() {

        try {
            TZonedDateTime.ofStrict(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0130, ZONE_PARIS);
        } catch (TDateTimeException ex) {
            assertEquals(ex.getMessage().contains(" is not valid for "), true);
            throw ex;
        }
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_invalidOffset() {

        try {
            TZonedDateTime.ofStrict(this.TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0130, ZONE_PARIS);
        } catch (TDateTimeException ex) {
            assertEquals(ex.getMessage().contains(" is not valid for "), true);
            throw ex;
        }
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullLDT() {

        TZonedDateTime.ofStrict((TLocalDateTime) null, OFFSET_0100, ZONE_PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullZO() {

        TZonedDateTime.ofStrict(this.TEST_LOCAL_2008_06_30_11_30_59_500, null, ZONE_PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullZI() {

        TZonedDateTime.ofStrict(this.TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0100, null);
    }

    @Test
    public void factory_from_DateTimeAccessor_ZDT() {

        assertEquals(TZonedDateTime.from(this.TEST_DATE_TIME_PARIS), this.TEST_DATE_TIME_PARIS);
    }

    @Test
    public void factory_from_DateTimeAccessor_LDT_ZoneId() {

        assertEquals(TZonedDateTime.from(new TDefaultInterfaceTemporalAccessor() {
            @Override
            public boolean isSupported(TTemporalField field) {

                return TestZonedDateTime.this.TEST_DATE_TIME_PARIS.toLocalDateTime().isSupported(field);
            }

            @Override
            public long getLong(TTemporalField field) {

                return TestZonedDateTime.this.TEST_DATE_TIME_PARIS.toLocalDateTime().getLong(field);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TTemporalQuery<R> query) {

                if (query == TTemporalQueries.zoneId()) {
                    return (R) TestZonedDateTime.this.TEST_DATE_TIME_PARIS.getZone();
                }
                return super.query(query);
            }
        }), this.TEST_DATE_TIME_PARIS);
    }

    @Test
    public void factory_from_DateTimeAccessor_Instant_ZoneId() {

        assertEquals(TZonedDateTime.from(new TDefaultInterfaceTemporalAccessor() {
            @Override
            public boolean isSupported(TTemporalField field) {

                return field == INSTANT_SECONDS || field == NANO_OF_SECOND;
            }

            @Override
            public long getLong(TTemporalField field) {

                return TestZonedDateTime.this.TEST_DATE_TIME_PARIS.toInstant().getLong(field);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TTemporalQuery<R> query) {

                if (query == TTemporalQueries.zoneId()) {
                    return (R) TestZonedDateTime.this.TEST_DATE_TIME_PARIS.getZone();
                }
                return super.query(query);
            }
        }), this.TEST_DATE_TIME_PARIS);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_from_DateTimeAccessor_invalid_noDerive() {

        TZonedDateTime.from(TLocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void factory_from_DateTimeAccessor_null() {

        TZonedDateTime.from((TTemporalAccessor) null);
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
            String zoneId = (String) data[7];
            String text = (String) data[8];

            TZonedDateTime t = TZonedDateTime.parse(text);
            assertEquals(t.getYear(), y);
            assertEquals(t.getMonth().getValue(), month);
            assertEquals(t.getDayOfMonth(), d);
            assertEquals(t.getHour(), h);
            assertEquals(t.getMinute(), m);
            assertEquals(t.getSecond(), s);
            assertEquals(t.getNano(), n);
            assertEquals(t.getZone().getId(), zoneId);
        }
    }

    Object[][] data_parseAdditional() {

        return new Object[][] { { "2012-06-30T12:30:40Z[GMT]", 2012, 6, 30, 12, 30, 40, 0, "GMT" },
        { "2012-06-30T12:30:40Z[UT]", 2012, 6, 30, 12, 30, 40, 0, "UT" },
        { "2012-06-30T12:30:40Z[UTC]", 2012, 6, 30, 12, 30, 40, 0, "UTC" },
        { "2012-06-30T12:30:40+01:00[+01:00]", 2012, 6, 30, 12, 30, 40, 0, "+01:00" },
        { "2012-06-30T12:30:40+01:00[GMT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT+01:00" },
        { "2012-06-30T12:30:40+01:00[UT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT+01:00" },
        { "2012-06-30T12:30:40+01:00[UTC+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC+01:00" },
        { "2012-06-30T12:30:40-01:00[-01:00]", 2012, 6, 30, 12, 30, 40, 0, "-01:00" },
        { "2012-06-30T12:30:40-01:00[GMT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT-01:00" },
        { "2012-06-30T12:30:40-01:00[UT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT-01:00" },
        { "2012-06-30T12:30:40-01:00[UTC-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC-01:00" },
        { "2012-06-30T12:30:40+01:00[Europe/London]", 2012, 6, 30, 12, 30, 40, 0, "Europe/London" }, };
    }

    @Test
    public void test_parseAdditional() {

        for (Object[] data : data_parseAdditional()) {
            String text = (String) data[0];
            int y = (int) data[1];
            int month = (int) data[2];
            int d = (int) data[3];
            int h = (int) data[4];
            int m = (int) data[5];
            int s = (int) data[6];
            int n = (int) data[7];
            String zoneId = (String) data[8];

            TZonedDateTime t = TZonedDateTime.parse(text);
            assertEquals(t.getYear(), y);
            assertEquals(t.getMonth().getValue(), month);
            assertEquals(t.getDayOfMonth(), d);
            assertEquals(t.getHour(), h);
            assertEquals(t.getMinute(), m);
            assertEquals(t.getSecond(), s);
            assertEquals(t.getNano(), n);
            assertEquals(t.getZone().getId(), zoneId);
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_illegalValue() {

        TZonedDateTime.parse("2008-06-32T11:15+01:00[Europe/Paris]");
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_invalidValue() {

        TZonedDateTime.parse("2008-06-31T11:15+01:00[Europe/Paris]");
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_nullText() {

        TZonedDateTime.parse((String) null);
    }

    Object[][] data_parseOverlapRoundtrip() {

        return new Object[][] { { "2016-11-06T01:00-04:00[America/New_York]" },
        { "2016-10-30T02:00+02:00[Europe/Berlin]" }, };
    }

    @Test
    public void test_parseFormatRoundtripWithZoneAndOffset() {

        for (Object[] data : data_parseOverlapRoundtrip()) {
            String text = (String) data[0];

            TZonedDateTime start = TZonedDateTime.parse(text);
            for (int min = 0; min <= 60; min += 15) {
                TZonedDateTime t = start.plusMinutes(min);
                assertEquals(t, TZonedDateTime.parse(t.toString()));
            }
        }
    }

    Object[][] data_parseOverlapToInstant() {

        return new Object[][] { { "2016-11-06T01:00-04:00[America/New_York]", "2016-11-06T05:00:00Z" },
        { "2016-11-06T01:30-04:00[America/New_York]", "2016-11-06T05:30:00Z" },
        { "2016-11-06T01:00-05:00[America/New_York]", "2016-11-06T06:00:00Z" },
        { "2016-11-06T01:30-05:00[America/New_York]", "2016-11-06T06:30:00Z" },
        { "2016-11-06T02:00-05:00[America/New_York]", "2016-11-06T07:00:00Z" },

        { "2016-10-30T02:00+02:00[Europe/Berlin]", "2016-10-30T00:00:00Z" },
        { "2016-10-30T02:30+02:00[Europe/Berlin]", "2016-10-30T00:30:00Z" },
        { "2016-10-30T02:00+01:00[Europe/Berlin]", "2016-10-30T01:00:00Z" },
        { "2016-10-30T02:30+01:00[Europe/Berlin]", "2016-10-30T01:30:00Z" },
        { "2016-10-30T03:00+01:00[Europe/Berlin]", "2016-10-30T02:00:00Z" }, };
    }

    @Test
    public void test_parseWithZoneAndOffsetToInstant() {

        for (Object[] data : data_parseOverlapToInstant()) {
            String z = (String) data[0];
            String i = (String) data[1];

            TZonedDateTime zdt = TZonedDateTime.parse(z);
            TInstant instant = TInstant.parse(i);
            assertEquals(zdt.toInstant(), instant);
        }
    }

    @Test
    public void factory_parse_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M d H m s VV");
        TZonedDateTime test = TZonedDateTime.parse("2010 12 3 11 30 0 Europe/London", f);
        assertEquals(test, TZonedDateTime.of(TLocalDateTime.of(2010, 12, 3, 11, 30), TZoneId.of("Europe/London")));
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullText() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M d H m s");
        TZonedDateTime.parse((String) null, f);
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {

        TZonedDateTime.parse("ANY", null);
    }

    Object[][] provider_sampleTimes() {

        return new Object[][] { { 2008, 6, 30, 11, 30, 20, 500, ZONE_0100 }, { 2008, 6, 30, 11, 0, 0, 0, ZONE_0100 },
        { 2008, 6, 30, 11, 30, 20, 500, ZONE_PARIS }, { 2008, 6, 30, 11, 0, 0, 0, ZONE_PARIS },
        { 2008, 6, 30, 23, 59, 59, 999999999, ZONE_0100 }, { -1, 1, 1, 0, 0, 0, 0, ZONE_0100 }, };
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
            TZoneId zone = (TZoneId) data[7];

            TLocalDate localDate = TLocalDate.of(y, o, d);
            TLocalTime localTime = TLocalTime.of(h, m, s, n);
            TLocalDateTime localDateTime = TLocalDateTime.of(localDate, localTime);
            TZoneOffset offset = zone.getRules().getOffset(localDateTime);
            TZonedDateTime a = TZonedDateTime.of(localDateTime, zone);

            assertEquals(a.getYear(), localDate.getYear());
            assertEquals(a.getMonth(), localDate.getMonth());
            assertEquals(a.getDayOfMonth(), localDate.getDayOfMonth());
            assertEquals(a.getDayOfYear(), localDate.getDayOfYear());
            assertEquals(a.getDayOfWeek(), localDate.getDayOfWeek());

            assertEquals(a.getHour(), localTime.getHour());
            assertEquals(a.getMinute(), localTime.getMinute());
            assertEquals(a.getSecond(), localTime.getSecond());
            assertEquals(a.getNano(), localTime.getNano());

            assertEquals(a.toLocalDate(), localDate);
            assertEquals(a.toLocalTime(), localTime);
            assertEquals(a.toLocalDateTime(), localDateTime);
            if (zone instanceof TZoneOffset) {
                assertEquals(a.toString(), localDateTime.toString() + offset.toString());
            } else {
                assertEquals(a.toString(), localDateTime.toString() + offset.toString() + "[" + zone.toString() + "]");
            }
        }
    }

    @Test
    public void test_get_DateTimeField() {

        TZonedDateTime test = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
        assertEquals(test.get(TChronoField.YEAR), 2008);
        assertEquals(test.get(TChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.get(TChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.get(TChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.get(TChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.get(TChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.get(TChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.get(TChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.get(TChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.get(TChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.get(TChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.get(TChronoField.OFFSET_SECONDS), 3600);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_DateTimeField_long() {

        this.TEST_DATE_TIME.get(TChronoField.INSTANT_SECONDS);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_DateTimeField_invalidField() {

        this.TEST_DATE_TIME.get(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_DateTimeField_null() {

        this.TEST_DATE_TIME.get((TTemporalField) null);
    }

    @Test
    public void test_getLong_DateTimeField() {

        TZonedDateTime test = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
        assertEquals(test.getLong(TChronoField.YEAR), 2008);
        assertEquals(test.getLong(TChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.getLong(TChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.getLong(TChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.getLong(TChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.getLong(TChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(TChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(TChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(TChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.getLong(TChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(TChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.getLong(TChronoField.OFFSET_SECONDS), 3600);
        assertEquals(test.getLong(TChronoField.INSTANT_SECONDS), test.toEpochSecond());
    }

    @Test(expected = TDateTimeException.class)
    public void test_getLong_DateTimeField_invalidField() {

        this.TEST_DATE_TIME.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = NullPointerException.class)
    public void test_getLong_DateTimeField_null() {

        this.TEST_DATE_TIME.getLong((TTemporalField) null);
    }

    @Test
    public void test_query() {

        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.localDate()), this.TEST_DATE_TIME.toLocalDate());
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.localTime()), this.TEST_DATE_TIME.toLocalTime());
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.offset()), this.TEST_DATE_TIME.getOffset());
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.precision()), TChronoUnit.NANOS);
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.zone()), this.TEST_DATE_TIME.getZone());
        assertEquals(this.TEST_DATE_TIME.query(TTemporalQueries.zoneId()), this.TEST_DATE_TIME.getZone());
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        this.TEST_DATE_TIME.query(null);
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_notAtOverlap() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0200, ZONE_PARIS);
        TZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test, base); // not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0100,
                ZONE_PARIS);
        TZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test.getOffset(), OFFSET_0200); // offset changed to earlier
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime()); // date-time not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap_noChange() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0200,
                ZONE_PARIS);
        TZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test, base); // not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_notAtOverlap() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0200, ZONE_PARIS);
        TZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test, base); // not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0200,
                ZONE_PARIS);
        TZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test.getOffset(), OFFSET_0100); // offset changed to later
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime()); // date-time not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap_noChange() {

        TZonedDateTime base = TZonedDateTime.ofStrict(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0100,
                ZONE_PARIS);
        TZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test, base); // not changed
    }

    @Test
    public void test_withZoneSameLocal() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.withZoneSameLocal(ZONE_0200);
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime());
    }

    @Test
    public void test_withZoneSameLocal_noChange() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.withZoneSameLocal(ZONE_0100);
        assertEquals(test, base);
    }

    @Test
    public void test_withZoneSameLocal_retainOffset1() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 11, 2, 1, 30, 59, 0); // overlap
        TZonedDateTime base = TZonedDateTime.of(ldt, TZoneId.of("UTC-04:00"));
        TZonedDateTime test = base.withZoneSameLocal(TZoneId.of("America/New_York"));
        assertEquals(base.getOffset(), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(), TZoneOffset.ofHours(-4));
    }

    @Test
    public void test_withZoneSameLocal_retainOffset2() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 11, 2, 1, 30, 59, 0); // overlap
        TZonedDateTime base = TZonedDateTime.of(ldt, TZoneId.of("UTC-05:00"));
        TZonedDateTime test = base.withZoneSameLocal(TZoneId.of("America/New_York"));
        assertEquals(base.getOffset(), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(), TZoneOffset.ofHours(-5));
    }

    @Test(expected = NullPointerException.class)
    public void test_withZoneSameLocal_null() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        base.withZoneSameLocal(null);
    }

    @Test
    public void test_withZoneSameInstant() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withZoneSameInstant(ZONE_0200);
        TZonedDateTime expected = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.plusHours(1), ZONE_0200);
        assertEquals(test, expected);
    }

    @Test
    public void test_withZoneSameInstant_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withZoneSameInstant(ZONE_0100);
        assertEquals(test, base);
    }

    @Test(expected = NullPointerException.class)
    public void test_withZoneSameInstant_null() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.withZoneSameInstant(null);
    }

    @Test
    public void test_withZoneLocked() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        TZonedDateTime test = base.withFixedOffsetZone();
        TZonedDateTime expected = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0200);
        assertEquals(test, expected);
    }

    @Test
    public void test_with_WithAdjuster_LocalDateTime_sameOffset() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        TZonedDateTime test = base.with(TLocalDateTime.of(2012, 7, 15, 14, 30));
        check(test, 2012, 7, 15, 14, 30, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalDateTime_adjustedOffset() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        TZonedDateTime test = base.with(TLocalDateTime.of(2012, 1, 15, 14, 30));
        check(test, 2012, 1, 15, 14, 30, 0, 0, OFFSET_0100, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalDate() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        TZonedDateTime test = base.with(TLocalDate.of(2012, 7, 28));
        check(test, 2012, 7, 28, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_LocalTime() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_PARIS_OVERLAP_2008_10_26_02_30, ZONE_PARIS);
        TZonedDateTime test = base.with(TLocalTime.of(2, 29));
        check(test, 2008, 10, 26, 2, 29, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Year() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.with(TYear.of(2007));
        assertEquals(test, TZonedDateTime.of(ldt.withYear(2007), ZONE_0100));
    }

    @Test
    public void test_with_WithAdjuster_Month_adjustedDayOfMonth() {

        TZonedDateTime base = TZonedDateTime.of(TLocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        TZonedDateTime test = base.with(TMonth.JUNE);
        check(test, 2012, 6, 30, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Offset_same() {

        TZonedDateTime base = TZonedDateTime.of(TLocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        TZonedDateTime test = base.with(TZoneOffset.ofHours(2));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_WithAdjuster_Offset_ignored() {

        TZonedDateTime base = TZonedDateTime.of(TLocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        TZonedDateTime test = base.with(TZoneOffset.ofHours(1));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS); // offset ignored
    }

    @Test
    public void test_with_WithAdjuster_LocalDate_retainOffset1() {

        TZoneId newYork = TZoneId.of("America/New_York");
        TLocalDateTime ldt = TLocalDateTime.of(2008, 11, 1, 1, 30);
        TZonedDateTime base = TZonedDateTime.of(ldt, newYork);
        assertEquals(base.getOffset(), TZoneOffset.ofHours(-4));
        TZonedDateTime test = base.with(TLocalDate.of(2008, 11, 2));
        assertEquals(test.getOffset(), TZoneOffset.ofHours(-4));
    }

    @Test
    public void test_with_WithAdjuster_LocalDate_retainOffset2() {

        TZoneId newYork = TZoneId.of("America/New_York");
        TLocalDateTime ldt = TLocalDateTime.of(2008, 11, 3, 1, 30);
        TZonedDateTime base = TZonedDateTime.of(ldt, newYork);
        assertEquals(base.getOffset(), TZoneOffset.ofHours(-5));
        TZonedDateTime test = base.with(TLocalDate.of(2008, 11, 2));
        assertEquals(test.getOffset(), TZoneOffset.ofHours(-5));
    }

    @Test(expected = NullPointerException.class)
    public void test_with_WithAdjuster_null() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.with((TTemporalAdjuster) null);
    }

    @Test
    public void test_withYear_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withYear(2007);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withYear(2007), ZONE_0100));
    }

    @Test
    public void test_withYear_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withYear(2008);
        assertEquals(test, base);
    }

    @Test
    public void test_withMonth_Month_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.with(JANUARY);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withMonth(1), ZONE_0100));
    }

    @Test(expected = NullPointerException.class)
    public void test_withMonth_Month_null() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.with((TMonth) null);
    }

    @Test
    public void test_withMonth_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withMonth(1);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withMonth(1), ZONE_0100));
    }

    @Test
    public void test_withMonth_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withMonth(6);
        assertEquals(test, base);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooBig() {

        this.TEST_DATE_TIME.withMonth(13);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_tooSmall() {

        this.TEST_DATE_TIME.withMonth(0);
    }

    @Test
    public void test_withDayOfMonth_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withDayOfMonth(15);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withDayOfMonth(15), ZONE_0100));
    }

    @Test
    public void test_withDayOfMonth_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withDayOfMonth(30);
        assertEquals(test, base);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_tooBig() {

        TLocalDateTime.of(2007, 7, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(32);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_tooSmall() {

        this.TEST_DATE_TIME.withDayOfMonth(0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_invalid31() {

        TLocalDateTime.of(2007, 6, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(31);
    }

    @Test
    public void test_withDayOfYear_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withDayOfYear(33);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withDayOfYear(33), ZONE_0100));
    }

    @Test
    public void test_withDayOfYear_noChange() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 2, 5, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.withDayOfYear(36);
        assertEquals(test, base);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfYear_tooBig() {

        this.TEST_DATE_TIME.withDayOfYear(367);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfYear_tooSmall() {

        this.TEST_DATE_TIME.withDayOfYear(0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfYear_invalid366() {

        TLocalDateTime.of(2007, 2, 2, 11, 30).atZone(ZONE_PARIS).withDayOfYear(366);
    }

    @Test
    public void test_withHour_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withHour(15);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withHour(15), ZONE_0100));
    }

    @Test
    public void test_withHour_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withHour(11);
        assertEquals(test, base);
    }

    @Test
    public void test_withMinute_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withMinute(15);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withMinute(15), ZONE_0100));
    }

    @Test
    public void test_withMinute_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withMinute(30);
        assertEquals(test, base);
    }

    @Test
    public void test_withSecond_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withSecond(12);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withSecond(12), ZONE_0100));
    }

    @Test
    public void test_withSecond_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withSecond(59);
        assertEquals(test, base);
    }

    @Test
    public void test_withNanoOfSecond_normal() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withNano(15);
        assertEquals(test, TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500.withNano(15), ZONE_0100));
    }

    @Test
    public void test_withNanoOfSecond_noChange() {

        TZonedDateTime base = TZonedDateTime.of(this.TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TZonedDateTime test = base.withNano(500);
        assertEquals(test, base);
    }

    Object[][] data_plusDays() {

        return new Object[][] {
        // normal
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0,
        dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1,
        dateTime(2008, 7, 1, 23, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1,
        dateTime(2008, 6, 29, 23, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        // skip over gap
        { dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
        dateTime(2008, 3, 31, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        { dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
        dateTime(2008, 3, 29, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        // land in gap
        { dateTime(2008, 3, 29, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
        dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        { dateTime(2008, 3, 31, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
        dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        // skip over overlap
        { dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
        dateTime(2008, 10, 27, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        { dateTime(2008, 10, 25, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
        dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        // land in overlap
        { dateTime(2008, 10, 25, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        { dateTime(2008, 10, 27, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), -1,
        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS) }, };
    }

    @Test
    public void test_plus_adjuster_Period_days() {

        for (Object[] data : data_plusDays()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(TPeriod.ofDays((int) amount)), expected);
        }
    }

    Object[][] data_plusTime() {

        return new Object[][] {
        // normal
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0,
        dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1,
        dateTime(2008, 7, 1, 0, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        { dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1,
        dateTime(2008, 6, 30, 22, 30, 59, 0, OFFSET_0100, ZONE_0100) },
        // gap
        { dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1,
        dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        { dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1,
        dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        // overlap
        { dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS) },
        { dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2,
        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        { dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 3,
        dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        { dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1,
        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS) },
        { dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2,
        dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS) }, };
    }

    @Test
    public void test_plus_adjuster_Period_hours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(TDuration.ofHours(amount)), expected);
        }
    }

    @Test
    public void test_plus_adjuster_Duration_hours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(TDuration.ofHours(amount)), expected);
        }
    }

    @Test
    public void test_plus_adjuster() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TZonedDateTime t = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        TZonedDateTime expected = TZonedDateTime.of(TLocalDateTime.of(2009, 1, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(t.plus(period), expected);
    }

    @Test
    public void test_plus_adjuster_Duration() {

        TDuration duration = TDuration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        TZonedDateTime t = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        TZonedDateTime expected = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 16, 36, 5, 500), ZONE_0100);
        assertEquals(t.plus(duration), expected);
    }

    @Test
    public void test_plus_adjuster_Period_zero() {

        TZonedDateTime t = this.TEST_DATE_TIME.plus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(t, this.TEST_DATE_TIME);
    }

    @Test
    public void test_plus_adjuster_Duration_zero() {

        TZonedDateTime t = this.TEST_DATE_TIME.plus(TDuration.ZERO);
        assertEquals(t, this.TEST_DATE_TIME);
    }

    @Test(expected = NullPointerException.class)
    public void test_plus_adjuster_null() {

        this.TEST_DATE_TIME.plus(null);
    }

    @Test
    public void test_plus_longUnit_hours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(amount, HOURS), expected);
        }
    }

    @Test
    public void test_plus_longUnit_minutes() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(amount * 60, MINUTES), expected);
        }
    }

    @Test
    public void test_plus_longUnit_seconds() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(amount * 3600, SECONDS), expected);
        }
    }

    @Test
    public void test_plus_longUnit_nanos() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plus(amount * 3600000000000L, NANOS), expected);
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_plus_longUnit_null() {

        this.TEST_DATE_TIME_PARIS.plus(0, null);
    }

    @Test
    public void test_plusYears() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusYears(1);
        assertEquals(test, TZonedDateTime.of(ldt.plusYears(1), ZONE_0100));
    }

    @Test
    public void test_plusYears_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusYears(0);
        assertEquals(test, base);
    }

    @Test
    public void test_plusMonths() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusMonths(1);
        assertEquals(test, TZonedDateTime.of(ldt.plusMonths(1), ZONE_0100));
    }

    @Test
    public void test_plusMonths_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusMonths(0);
        assertEquals(test, base);
    }

    @Test
    public void test_plusWeeks() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusWeeks(1);
        assertEquals(test, TZonedDateTime.of(ldt.plusWeeks(1), ZONE_0100));
    }

    @Test
    public void test_plusWeeks_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusWeeks(0);
        assertEquals(test, base);
    }

    @Test
    public void test_plusDays() {

        for (Object[] data : data_plusDays()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plusDays(amount), expected);
        }
    }

    @Test
    public void test_plusHours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plusHours(amount), expected);
        }
    }

    @Test
    public void test_plusMinutes() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plusMinutes(amount * 60), expected);
        }
    }

    @Test
    public void test_plusMinutes_minutes() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusMinutes(30);
        assertEquals(test, TZonedDateTime.of(ldt.plusMinutes(30), ZONE_0100));
    }

    @Test
    public void test_plusSeconds() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plusSeconds(amount * 3600), expected);
        }
    }

    @Test
    public void test_plusSeconds_seconds() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusSeconds(1);
        assertEquals(test, TZonedDateTime.of(ldt.plusSeconds(1), ZONE_0100));
    }

    @Test
    public void test_plusNanos() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.plusNanos(amount * 3600000000000L), expected);
        }
    }

    @Test
    public void test_plusNanos_nanos() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.plusNanos(1);
        assertEquals(test, TZonedDateTime.of(ldt.plusNanos(1), ZONE_0100));
    }

    @Test
    public void test_minus_adjuster_Period_days() {

        for (Object[] data : data_plusDays()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minus(TPeriod.ofDays((int) -amount)), expected);
        }
    }

    @Test
    public void test_minus_adjuster_Period_hours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minus(TDuration.ofHours(-amount)), expected);
        }
    }

    @Test
    public void test_minus_adjuster_Duration_hours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minus(TDuration.ofHours(-amount)), expected);
        }
    }

    @Test
    public void test_minus_adjuster() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TZonedDateTime t = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        TZonedDateTime expected = TZonedDateTime.of(TLocalDateTime.of(2007, 11, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(t.minus(period), expected);
    }

    @Test
    public void test_minus_adjuster_Duration() {

        TDuration duration = TDuration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        TZonedDateTime t = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        TZonedDateTime expected = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 1, 8, 25, 53, 500), ZONE_0100);
        assertEquals(t.minus(duration), expected);
    }

    @Test
    public void test_minus_adjuster_Period_zero() {

        TZonedDateTime t = this.TEST_DATE_TIME.minus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(t, this.TEST_DATE_TIME);
    }

    @Test
    public void test_minus_adjuster_Duration_zero() {

        TZonedDateTime t = this.TEST_DATE_TIME.minus(TDuration.ZERO);
        assertEquals(t, this.TEST_DATE_TIME);
    }

    @Test(expected = NullPointerException.class)
    public void test_minus_adjuster_null() {

        this.TEST_DATE_TIME.minus(null);
    }

    @Test
    public void test_minusYears() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusYears(1);
        assertEquals(test, TZonedDateTime.of(ldt.minusYears(1), ZONE_0100));
    }

    @Test
    public void test_minusYears_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusYears(0);
        assertEquals(test, base);
    }

    @Test
    public void test_minusMonths() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusMonths(1);
        assertEquals(test, TZonedDateTime.of(ldt.minusMonths(1), ZONE_0100));
    }

    @Test
    public void test_minusMonths_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusMonths(0);
        assertEquals(test, base);
    }

    @Test
    public void test_minusWeeks() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusWeeks(1);
        assertEquals(test, TZonedDateTime.of(ldt.minusWeeks(1), ZONE_0100));
    }

    @Test
    public void test_minusWeeks_zero() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusWeeks(0);
        assertEquals(test, base);
    }

    @Test
    public void test_minusDays() {

        for (Object[] data : data_plusDays()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minusDays(-amount), expected);
        }
    }

    @Test
    public void test_minusHours() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minusHours(-amount), expected);
        }
    }

    @Test
    public void test_minusMinutes() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minusMinutes(-amount * 60), expected);
        }
    }

    @Test
    public void test_minusMinutes_minutes() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusMinutes(30);
        assertEquals(test, TZonedDateTime.of(ldt.minusMinutes(30), ZONE_0100));
    }

    @Test
    public void test_minusSeconds() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minusSeconds(-amount * 3600), expected);
        }
    }

    @Test
    public void test_minusSeconds_seconds() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusSeconds(1);
        assertEquals(test, TZonedDateTime.of(ldt.minusSeconds(1), ZONE_0100));
    }

    @Test
    public void test_minusNanos() {

        for (Object[] data : data_plusTime()) {
            TZonedDateTime base = (TZonedDateTime) data[0];
            long amount = ((Number) data[1]).longValue();
            TZonedDateTime expected = (TZonedDateTime) data[2];

            assertEquals(base.minusNanos(-amount * 3600000000000L), expected);
        }
    }

    @Test
    public void test_minusNanos_nanos() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime base = TZonedDateTime.of(ldt, ZONE_0100);
        TZonedDateTime test = base.minusNanos(1);
        assertEquals(test, TZonedDateTime.of(ldt.minusNanos(1), ZONE_0100));
    }

    Object[][] data_toInstant() {

        return new Object[][] { { TLocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 0L, 0 },
        { TLocalDateTime.of(1970, 1, 1, 0, 0, 0, 1), 0L, 1 },
        { TLocalDateTime.of(1970, 1, 1, 0, 0, 0, 999999999), 0L, 999999999 },
        { TLocalDateTime.of(1970, 1, 1, 0, 0, 1, 0), 1L, 0 }, { TLocalDateTime.of(1970, 1, 1, 0, 0, 1, 1), 1L, 1 },
        { TLocalDateTime.of(1969, 12, 31, 23, 59, 59, 999999999), -1L, 999999999 },
        { TLocalDateTime.of(1970, 1, 2, 0, 0), 24L * 60L * 60L, 0 },
        { TLocalDateTime.of(1969, 12, 31, 0, 0), -24L * 60L * 60L, 0 }, };
    }

    @Test
    public void test_toInstant_UTC() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(TZoneOffset.UTC);
            TInstant test = dt.toInstant();
            assertEquals(test.getEpochSecond(), expectedEpSec);
            assertEquals(test.getNano(), expectedNos);
        }
    }

    @Test
    public void test_toInstant_P0100() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(ZONE_0100);
            TInstant test = dt.toInstant();
            assertEquals(test.getEpochSecond(), expectedEpSec - 3600);
            assertEquals(test.getNano(), expectedNos);
        }
    }

    @Test
    public void test_toInstant_M0100() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(ZONE_M0100);
            TInstant test = dt.toInstant();
            assertEquals(test.getEpochSecond(), expectedEpSec + 3600);
            assertEquals(test.getNano(), expectedNos);
        }
    }

    @Test
    public void test_toEpochSecond_afterEpoch() {

        TLocalDateTime ldt = TLocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            TZonedDateTime a = TZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(a.toEpochSecond(), i);
            ldt = ldt.plusSeconds(1);
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {

        TLocalDateTime ldt = TLocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            TZonedDateTime a = TZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(a.toEpochSecond(), -i);
            ldt = ldt.minusSeconds(1);
        }
    }

    @Test
    public void test_toEpochSecond_UTC() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(TZoneOffset.UTC);
            assertEquals(dt.toEpochSecond(), expectedEpSec);
        }
    }

    @Test
    public void test_toEpochSecond_P0100() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(ZONE_0100);
            assertEquals(dt.toEpochSecond(), expectedEpSec - 3600);
        }
    }

    @Test
    public void test_toEpochSecond_M0100() {

        for (Object[] data : data_toInstant()) {
            TLocalDateTime ldt = (TLocalDateTime) data[0];
            long expectedEpSec = ((Number) data[1]).longValue();
            int expectedNos = (int) data[2];

            TZonedDateTime dt = ldt.atZone(ZONE_M0100);
            assertEquals(dt.toEpochSecond(), expectedEpSec + 3600);
        }
    }

    @Test
    public void test_compareTo_time1() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 39), ZONE_0100);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 41), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_time2() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 40, 4), ZONE_0100);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 40, 5), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_offset1() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 41), ZONE_0200);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 39), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_offset2() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 40, 5), TZoneId.of("UTC+01:01"));
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 30, 40, 4), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_both() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 50), ZONE_0200);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 20), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_bothNanos() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 20, 40, 5), ZONE_0200);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 10, 20, 40, 6), ZONE_0100);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {

        TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 10, 0), ZONE_0100);
        TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 11, 0), ZONE_0200);
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test(expected = NullPointerException.class)
    public void test_compareTo_null() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime a = TZonedDateTime.of(ldt, ZONE_0100);
        a.compareTo(null);
    }

    Object[][] data_isBefore() {

        return new Object[][] { { 11, 30, ZONE_0100, 11, 31, ZONE_0100, true }, // a is before b due to time
        { 11, 30, ZONE_0200, 11, 30, ZONE_0100, true }, // a is before b due to offset
        { 11, 30, ZONE_0200, 10, 30, ZONE_0100, false }, // a is equal b due to same instant
        };
    }

    @Test
    public void test_isBefore() {

        for (Object[] data : data_isBefore()) {
            int hour1 = (int) data[0];
            int minute1 = (int) data[1];
            TZoneId zone1 = (TZoneId) data[2];
            int hour2 = (int) data[3];
            int minute2 = (int) data[4];
            TZoneId zone2 = (TZoneId) data[5];
            boolean expected = (boolean) data[6];

            TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, hour1, minute1), zone1);
            TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, hour2, minute2), zone2);
            assertEquals(a.isBefore(b), expected);
            assertEquals(b.isBefore(a), false);
            assertEquals(a.isBefore(a), false);
            assertEquals(b.isBefore(b), false);
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_isBefore_null() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime a = TZonedDateTime.of(ldt, ZONE_0100);
        a.isBefore(null);
    }

    Object[][] data_isAfter() {

        return new Object[][] { { 11, 31, ZONE_0100, 11, 30, ZONE_0100, true }, // a is after b due to time
        { 11, 30, ZONE_0100, 11, 30, ZONE_0200, true }, // a is after b due to offset
        { 11, 30, ZONE_0200, 10, 30, ZONE_0100, false }, // a is equal b due to same instant
        };
    }

    @Test
    public void test_isAfter() {

        for (Object[] data : data_isAfter()) {
            int hour1 = (int) data[0];
            int minute1 = (int) data[1];
            TZoneId zone1 = (TZoneId) data[2];
            int hour2 = (int) data[3];
            int minute2 = (int) data[4];
            TZoneId zone2 = (TZoneId) data[5];
            boolean expected = (boolean) data[6];

            TZonedDateTime a = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, hour1, minute1), zone1);
            TZonedDateTime b = TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, hour2, minute2), zone2);
            assertEquals(a.isAfter(b), expected);
            assertEquals(b.isAfter(a), false);
            assertEquals(a.isAfter(a), false);
            assertEquals(b.isAfter(b), false);
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_isAfter_null() {

        TLocalDateTime ldt = TLocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        TZonedDateTime a = TZonedDateTime.of(ldt, ZONE_0100);
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

            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
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

            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y + 1, o, d, h, m, s, n), ZONE_0100);
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
            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h + 1, m, s, n), ZONE_0100);
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
            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h, m + 1, s, n), ZONE_0100);
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
            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h, m, s + 1, n), ZONE_0100);
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
            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n + 1), ZONE_0100);
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

            TZonedDateTime a = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
            TZonedDateTime b = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0200);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_itself_true() {

        assertEquals(this.TEST_DATE_TIME.equals(this.TEST_DATE_TIME), true);
    }

    @Test
    public void test_equals_string_false() {

        assertEquals(this.TEST_DATE_TIME.equals("2007-07-15"), false);
    }

    Object[][] provider_sampleToString() {

        return new Object[][] { { 2008, 6, 30, 11, 30, 59, 0, "Z", "2008-06-30T11:30:59Z" },
        { 2008, 6, 30, 11, 30, 59, 0, "+01:00", "2008-06-30T11:30:59+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999000000, "Z", "2008-06-30T11:30:59.999Z" },
        { 2008, 6, 30, 11, 30, 59, 999000000, "+01:00", "2008-06-30T11:30:59.999+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999000, "Z", "2008-06-30T11:30:59.000999Z" },
        { 2008, 6, 30, 11, 30, 59, 999000, "+01:00", "2008-06-30T11:30:59.000999+01:00" },
        { 2008, 6, 30, 11, 30, 59, 999, "Z", "2008-06-30T11:30:59.000000999Z" },
        { 2008, 6, 30, 11, 30, 59, 999, "+01:00", "2008-06-30T11:30:59.000000999+01:00" },

        { 2008, 6, 30, 11, 30, 59, 999, "Europe/London", "2008-06-30T11:30:59.000000999+01:00[Europe/London]" },
        { 2008, 6, 30, 11, 30, 59, 999, "Europe/Paris", "2008-06-30T11:30:59.000000999+02:00[Europe/Paris]" }, };
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
            String zoneId = (String) data[7];
            String expected = (String) data[8];

            TZonedDateTime t = TZonedDateTime.of(dateTime(y, o, d, h, m, s, n), TZoneId.of(zoneId));
            String str = t.toString();
            assertEquals(str, expected);
        }
    }

    @Test
    public void test_format_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M d H m s");
        String t = TZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(f);
        assertEquals(t, "2010 12 3 11 30 0");
    }

    @Test(expected = NullPointerException.class)
    public void test_format_formatter_null() {

        TZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(null);
    }

    private static TLocalDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {

        return TLocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    private static TLocalDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond) {

        return TLocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
    }

    private static TZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond, TZoneOffset offset, TZoneId zoneId) {

        return TZonedDateTime.ofStrict(TLocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
                offset, zoneId);
    }

}
