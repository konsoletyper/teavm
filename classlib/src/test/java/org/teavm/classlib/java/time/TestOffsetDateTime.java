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
import static org.teavm.classlib.java.time.TMonth.DECEMBER;
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
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
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
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

@Test
public class TestOffsetDateTime extends AbstractDateTimeTest {

    private static final TZoneId ZONE_PARIS = TZoneId.of("Europe/Paris");
    private static final TZoneId ZONE_GAZA = TZoneId.of("Asia/Gaza");
    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);
    private static final TZoneOffset OFFSET_PTWO = TZoneOffset.ofHours(2);
    private static final TZoneOffset OFFSET_MONE = TZoneOffset.ofHours(-1);
    private static final TZoneOffset OFFSET_MTWO = TZoneOffset.ofHours(-2);
    private TOffsetDateTime TEST_2008_6_30_11_30_59_000000500;

    @Before
    public void setUp() {
        TEST_2008_6_30_11_30_59_000000500 = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {TEST_2008_6_30_11_30_59_000000500, TOffsetDateTime.MIN, TOffsetDateTime.MAX};
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {
        TTemporalField[] array = {
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
            TJulianFields.JULIAN_DAY,
            TJulianFields.MODIFIED_JULIAN_DAY,
            TJulianFields.RATA_DIE,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {
        List<TTemporalField> list = new ArrayList<TTemporalField>(Arrays.<TTemporalField>asList(TChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_serialization() throws Exception {
        assertSerializable(TEST_2008_6_30_11_30_59_000000500);
        assertSerializable(TOffsetDateTime.MIN);
        assertSerializable(TOffsetDateTime.MAX);
    }

    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        TLocalDate date = TLocalDate.of(2012, 9, 16);
        TLocalTime time = TLocalTime.of(22, 17, 59, 464 * 1000000);
        TZoneOffset offset = TZoneOffset.of("+01:00");
        assertEqualsSerialisedForm(TOffsetDateTime.of(date, time, offset));
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        TOffsetDateTime expected = TOffsetDateTime.now(TClock.systemDefaultZone());
        TOffsetDateTime test = TOffsetDateTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= 100000000) {
            // may be date change
            expected = TOffsetDateTime.now(TClock.systemDefaultZone());
            test = TOffsetDateTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(TClock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TOffsetDateTime test = TOffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 123456789);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_offset() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TClock clock = TClock.fixed(instant.minusSeconds(OFFSET_PONE.getTotalSeconds()), OFFSET_PONE);
            TOffsetDateTime test = TOffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
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
        TLocalTime expected = TLocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i).plusNanos(123456789L);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TOffsetDateTime test = TOffsetDateTime.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), TMonth.DECEMBER);
            assertEquals(test.getDayOfMonth(), 31);
            expected = expected.minusSeconds(1);
            assertEquals(test.toLocalTime(), expected);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(12, 0), TZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            TZoneOffset offset = TZoneOffset.ofHours(i);
            TClock clock = TClock.fixed(base.toInstant(), offset);
            TOffsetDateTime test = TOffsetDateTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullZoneId() {
        TOffsetDateTime.now((TZoneId) null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        TOffsetDateTime.now((TClock) null);
    }

    //-----------------------------------------------------------------------
    private void check(TOffsetDateTime test, int y, int mo, int d, int h, int m, int s, int n, TZoneOffset offset) {
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
        assertEquals(TOffsetDateTime.of(TLocalDateTime.of(y, mo, d, h, m, s, n), offset), test);
    }

    //-----------------------------------------------------------------------
    // dateTime factories
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHM() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, TMonth.JUNE, 30),
                TLocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHMS() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, TMonth.JUNE, 30),
                TLocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intMonthIntHMSN() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, TMonth.JUNE, 30),
                TLocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHM() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHMS() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsHMSN() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateLocalTimeZoneOffset() {
        TLocalDate date = TLocalDate.of(2008, 6, 30);
        TLocalTime time = TLocalTime.of(11, 30, 10, 500);
        TOffsetDateTime test = TOffsetDateTime.of(date, time, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalDate() {
        TLocalTime time = TLocalTime.of(11, 30, 10, 500);
        TOffsetDateTime.of((TLocalDate) null, time, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullLocalTime() {
        TLocalDate date = TLocalDate.of(2008, 6, 30);
        TOffsetDateTime.of(date, (TLocalTime) null, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTimeZoneOffset_nullOffset() {
        TLocalDate date = TLocalDate.of(2008, 6, 30);
        TLocalTime time = TLocalTime.of(11, 30, 10, 500);
        TOffsetDateTime.of(date, time, (TZoneOffset) null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateTimeZoneOffset() {
        TLocalDateTime dt = TLocalDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 10, 500));
        TOffsetDateTime test = TOffsetDateTime.of(dt, OFFSET_PONE);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateTimeZoneOffset_nullProvider() {
        TOffsetDateTime.of((TLocalDateTime) null, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateTimeZoneOffset_nullOffset() {
        TLocalDateTime dt = TLocalDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 10, 500));
        TOffsetDateTime.of(dt, (TZoneOffset) null);
    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(TOffsetDateTime.from(
                TOffsetDateTime.of(TLocalDate.of(2007, 7, 15), TLocalTime.of(17, 30), OFFSET_PONE)),
                TOffsetDateTime.of(TLocalDate.of(2007, 7, 15), TLocalTime.of(17, 30), OFFSET_PONE));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        TOffsetDateTime.from(TLocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_Calendricals_null() {
        TOffsetDateTime.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleToString")
    public void test_parse(int y, int month, int d, int h, int m, int s, int n, String offsetId, String text) {
        TOffsetDateTime t = TOffsetDateTime.parse(text);
        assertEquals(t.getYear(), y);
        assertEquals(t.getMonth().getValue(), month);
        assertEquals(t.getDayOfMonth(), d);
        assertEquals(t.getHour(), h);
        assertEquals(t.getMinute(), m);
        assertEquals(t.getSecond(), s);
        assertEquals(t.getNano(), n);
        assertEquals(t.getOffset().getId(), offsetId);
    }

    @Test(expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_illegalValue() {
        TOffsetDateTime.parse("2008-06-32T11:15+01:00");
    }

    @Test(expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_invalidValue() {
        TOffsetDateTime.parse("2008-06-31T11:15+01:00");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        TOffsetDateTime.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // parse(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M d H m s XXX");
        TOffsetDateTime test = TOffsetDateTime.parse("2010 12 3 11 30 0 +01:00", f);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2010, 12, 3), TLocalTime.of(11, 30), TZoneOffset.ofHours(1)));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M d H m s");
        TOffsetDateTime.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        TOffsetDateTime.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullTime() throws Throwable  {
        Constructor<TOffsetDateTime> con = TOffsetDateTime.class.getDeclaredConstructor(TLocalDateTime.class, TZoneOffset.class);
        con.setAccessible(true);
        try {
            con.newInstance(null, OFFSET_PONE);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullOffset() throws Throwable  {
        Constructor<TOffsetDateTime> con = TOffsetDateTime.class.getDeclaredConstructor(TLocalDateTime.class, TZoneOffset.class);
        con.setAccessible(true);
        try {
            con.newInstance(TLocalDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30)), null);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleTimes")
    Object[][] provider_sampleTimes() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 20, 500, OFFSET_PONE},
            {2008, 6, 30, 11, 0, 0, 0, OFFSET_PONE},
            {2008, 6, 30, 23, 59, 59, 999999999, OFFSET_PONE},
            {-1, 1, 1, 0, 0, 0, 0, OFFSET_PONE},
        };
    }

    @Test(dataProvider="sampleTimes")
    public void test_get(int y, int o, int d, int h, int m, int s, int n, TZoneOffset offset) {
        TLocalDate localDate = TLocalDate.of(y, o, d);
        TLocalTime localTime = TLocalTime.of(h, m, s, n);
        TLocalDateTime localDateTime = TLocalDateTime.of(localDate, localTime);
        TOffsetDateTime a = TOffsetDateTime.of(localDateTime, offset);

        assertEquals(a.getYear(), localDate.getYear());
        assertEquals(a.getMonth(), localDate.getMonth());
        assertEquals(a.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(a.getDayOfYear(), localDate.getDayOfYear());
        assertEquals(a.getDayOfWeek(), localDate.getDayOfWeek());

        assertEquals(a.getHour(), localDateTime.getHour());
        assertEquals(a.getMinute(), localDateTime.getMinute());
        assertEquals(a.getSecond(), localDateTime.getSecond());
        assertEquals(a.getNano(), localDateTime.getNano());

        assertEquals(a.toOffsetTime(), TOffsetTime.of(localTime, offset));
        assertEquals(a.toString(), localDateTime.toString() + offset.toString());
    }

    //-----------------------------------------------------------------------
    // get(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
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

    @Test
    public void test_getLong_TemporalField() {
        TOffsetDateTime test = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
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

        assertEquals(test.getLong(TChronoField.INSTANT_SECONDS), test.toEpochSecond());
        assertEquals(test.getLong(TChronoField.OFFSET_SECONDS), 3600);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.localDate()), TEST_2008_6_30_11_30_59_000000500.toLocalDate());
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.localTime()), TEST_2008_6_30_11_30_59_000000500.toLocalTime());
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.offset()), TEST_2008_6_30_11_30_59_000000500.getOffset());
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.precision()), TChronoUnit.NANOS);
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.zone()), TEST_2008_6_30_11_30_59_000000500.getOffset());
        assertEquals(TEST_2008_6_30_11_30_59_000000500.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_2008_6_30_11_30_59_000000500.query(null);
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final TOffsetDateTime sample = TOffsetDateTime.of(TLocalDate.of(2012, 3, 4), TLocalTime.of(23, 5), OFFSET_PONE);
        TTemporalAdjuster adjuster = new TTemporalAdjuster() {
            @Override
            public TTemporal adjustInto(TTemporal dateTime) {
                return sample;
            }
        };
        assertEquals(TEST_2008_6_30_11_30_59_000000500.with(adjuster), sample);
    }

    @Test
    public void test_with_adjustment_LocalDate() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(TLocalDate.of(2012, 9, 3));
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2012, 9, 3), TLocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_LocalTime() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(TLocalTime.of(19, 15));
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(19, 15), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_LocalDateTime() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(TLocalDateTime.of(TLocalDate.of(2012, 9, 3), TLocalTime.of(19, 15)));
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2012, 9, 3), TLocalTime.of(19, 15), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_OffsetTime() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(TOffsetTime.of(TLocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(19, 15), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_OffsetDateTime() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(TOffsetDateTime.of(TLocalDate.of(2012, 9, 3), TLocalTime.of(19, 15), OFFSET_PTWO));
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2012, 9, 3), TLocalTime.of(19, 15), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_Month() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(DECEMBER);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 12, 30),TLocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {
        TOffsetDateTime test = TEST_2008_6_30_11_30_59_000000500.with(OFFSET_PTWO);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 500), OFFSET_PTWO));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjustment_null() {
        TEST_2008_6_30_11_30_59_000000500.with((TTemporalAdjuster) null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameLocal_null() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameLocal(null);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameInstant() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        TOffsetDateTime expected = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(12, 30, 59), OFFSET_PTWO);
        assertEquals(test, expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameInstant_null() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameInstant(null);
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withYear(2007);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2007, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withMonth(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 1, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withDayOfMonth(15);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 15), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withDayOfYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfYear_normal() {
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.withDayOfYear(33);
        assertEquals(t, TOffsetDateTime.of(TLocalDate.of(2008, 2, 2), TLocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withDayOfYear_illegal() {
        TEST_2008_6_30_11_30_59_000000500.withDayOfYear(367);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withDayOfYear_invalid() {
        TOffsetDateTime.of(TLocalDate.of(2007, 2, 2), TLocalTime.of(11, 30), OFFSET_PONE).withDayOfYear(366);
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withHour(15);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(15, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withMinute(15);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 15, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.withSecond(15);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 15), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        TOffsetDateTime test = base.withNano(15);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 15), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TTemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(TEST_2008_6_30_11_30_59_000000500.truncatedTo(NANOS), TEST_2008_6_30_11_30_59_000000500);
        assertEquals(TEST_2008_6_30_11_30_59_000000500.truncatedTo(SECONDS), TEST_2008_6_30_11_30_59_000000500.withNano(0));
        assertEquals(TEST_2008_6_30_11_30_59_000000500.truncatedTo(DAYS), TEST_2008_6_30_11_30_59_000000500.with(TLocalTime.MIDNIGHT));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_truncatedTo_null() {
        TEST_2008_6_30_11_30_59_000000500.truncatedTo(null);
    }

    //-----------------------------------------------------------------------
    // plus(TPeriod)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Period() {
        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.plus(period);
        assertEquals(t, TOffsetDateTime.of(TLocalDate.of(2009, 1, 30), TLocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plus(TDuration)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Duration() {
        TDuration dur = TDuration.ofSeconds(62, 3);
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.plus(dur);
        assertEquals(t, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 32, 1, 503), OFFSET_PONE));
    }

    @Test
    public void test_plus_Duration_zero() {
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.plus(TDuration.ZERO);
        assertEquals(t, TEST_2008_6_30_11_30_59_000000500);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_Duration_null() {
        TEST_2008_6_30_11_30_59_000000500.plus((TDuration) null);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusYears(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2009, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusMonths(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 7, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusWeeks() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusWeeks(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 7, 7), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusDays() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusDays(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 7, 1), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusHours(13);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 7, 1), TLocalTime.of(0, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusMinutes(30);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(12, 0, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.plusSeconds(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 31, 0), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        TOffsetDateTime test = base.plusNanos(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 1), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minus(TPeriod)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Period() {
        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.minus(period);
        assertEquals(t, TOffsetDateTime.of(TLocalDate.of(2007, 11, 30), TLocalTime.of(11, 30, 59, 500), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minus(TDuration)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Duration() {
        TDuration dur = TDuration.ofSeconds(62, 3);
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.minus(dur);
        assertEquals(t, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 57, 497), OFFSET_PONE));
    }

    @Test
    public void test_minus_Duration_zero() {
        TOffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.minus(TDuration.ZERO);
        assertEquals(t, TEST_2008_6_30_11_30_59_000000500);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_Duration_null() {
        TEST_2008_6_30_11_30_59_000000500.minus((TDuration) null);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusYears(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2007, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusMonths(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 5, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusWeeks() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusWeeks(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 23), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusDays() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusDays(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 29), TLocalTime.of(11, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusHours(13);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 29), TLocalTime.of(22, 30, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusMinutes(30);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 0, 59), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetDateTime test = base.minusSeconds(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 58), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos() {
        TOffsetDateTime base = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        TOffsetDateTime test = base.minusNanos(1);
        assertEquals(test, TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 58, 999999999), OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // atZoneSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_atZone() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(t.atZoneSameInstant(ZONE_PARIS),
                TZonedDateTime.of(TLocalDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(15, 30)), ZONE_PARIS));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atZone_nullTimeZone() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_PTWO);
        t.atZoneSameInstant((TZoneId) null);
    }

    //-----------------------------------------------------------------------
    // atZoneSimilarLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_atZoneSimilarLocal() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_MTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS),
                TZonedDateTime.of(TLocalDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30)), ZONE_PARIS));
    }

    @Test
    public void test_atZoneSimilarLocal_dstGap() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2007, 4, 1), TLocalTime.of(0, 0), OFFSET_MTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_GAZA),
                TZonedDateTime.of(TLocalDateTime.of(TLocalDate.of(2007, 4, 1), TLocalTime.of(1, 0)), ZONE_GAZA));
    }

    @Test
    public void test_atZone_dstOverlapSummer() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2007, 10, 28), TLocalTime.of(2, 30), OFFSET_PTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime(), t.toLocalDateTime());
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getOffset(), OFFSET_PTWO);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getZone(), ZONE_PARIS);
    }

    @Test
    public void test_atZone_dstOverlapWinter() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2007, 10, 28), TLocalTime.of(2, 30), OFFSET_PONE);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).toLocalDateTime(), t.toLocalDateTime());
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getOffset(), OFFSET_PONE);
        assertEquals(t.atZoneSimilarLocal(ZONE_PARIS).getZone(), ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atZoneSimilarLocal_nullTimeZone() {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_PTWO);
        t.atZoneSimilarLocal((TZoneId) null);
    }

    //-----------------------------------------------------------------------
    // toEpochSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochSecond_afterEpoch() {
        for (int i = 0; i < 100000; i++) {
            TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0), TZoneOffset.UTC).plusSeconds(i);
            assertEquals(a.toEpochSecond(), i);
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {
        for (int i = 0; i < 100000; i++) {
            TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0), TZoneOffset.UTC).minusSeconds(i);
            assertEquals(a.toEpochSecond(), -i);
        }
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_timeMins() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 3), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 2), OFFSET_PONE);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_timeSecs() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 2), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 3), OFFSET_PONE);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_timeNanos() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 40, 4), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 29, 40, 5), OFFSET_PONE);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_offset() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_PTWO);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30), OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_offsetNanos() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 40, 6), OFFSET_PTWO);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 40, 5), OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_both() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 50), OFFSET_PTWO);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 20), OFFSET_PONE);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_bothNanos() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 20, 40, 4), OFFSET_PTWO);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(10, 20, 40, 5), OFFSET_PONE);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) < 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(10, 0), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 0), OFFSET_PTWO);  // a is before b despite being same time-line time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(a.toInstant().compareTo(b.toInstant()) == 0, true);
    }

    @Test
    public void test_compareTo_max() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(TYear.MAX_VALUE, 12, 31), TLocalTime.of(23, 59), OFFSET_MONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(TYear.MAX_VALUE, 12, 31), TLocalTime.of(23, 59), OFFSET_MTWO);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_min() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(TYear.MIN_VALUE, 1, 1), TLocalTime.of(0, 0), OFFSET_PTWO);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(TYear.MIN_VALUE, 1, 1), TLocalTime.of(0, 0), OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_null() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.compareTo(null);
    }

    @Test(expectedExceptions=ClassCastException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonOffsetDateTime() {
       Comparable c = TEST_2008_6_30_11_30_59_000000500;
       c.compareTo(new Object());
    }

    //-----------------------------------------------------------------------
    // isAfter() / isBefore() / isEqual()
    //-----------------------------------------------------------------------
    @Test
    public void test_isBeforeIsAfterIsEqual1() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 58, 3), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 2), OFFSET_PONE);  // a is before b due to time
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
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 2), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59, 3), OFFSET_PONE);  // a is before b due to time
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
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(10, 0), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 0), OFFSET_PTWO);  // a is same instant as b
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

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_null() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isEqual_null() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isEqual(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_null() {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(2008, 6, 30), TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isAfter(null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_equals_true(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_year_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y + 1, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_hour_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        h = (h == 23 ? 22 : h);
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h + 1, m, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_minute_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        m = (m == 59 ? 58 : m);
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m + 1, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_second_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        s = (s == 59 ? 58 : s);
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s + 1, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_nano_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        n = (n == 999999999 ? 999999998 : n);
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n + 1), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_offset_differs(int y, int o, int d, int h, int m, int s, int n, TZoneOffset ignored) {
        TOffsetDateTime a = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetDateTime b = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), OFFSET_PTWO);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_2008_6_30_11_30_59_000000500.equals(TEST_2008_6_30_11_30_59_000000500), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_2008_6_30_11_30_59_000000500.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_2008_6_30_11_30_59_000000500.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
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

    @Test(dataProvider="sampleToString")
    public void test_toString(int y, int o, int d, int h, int m, int s, int n, String offsetId, String expected) {
        TOffsetDateTime t = TOffsetDateTime.of(TLocalDate.of(y, o, d), TLocalTime.of(h, m, s, n), TZoneOffset.of(offsetId));
        String str = t.toString();
        assertEquals(str, expected);
    }

    //-----------------------------------------------------------------------
    // format(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M d H m s");
        String t = TOffsetDateTime.of(TLocalDate.of(2010, 12, 3), TLocalTime.of(11, 30), OFFSET_PONE).format(f);
        assertEquals(t, "2010 12 3 11 30 0");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        TOffsetDateTime.of(TLocalDate.of(2010, 12, 3), TLocalTime.of(11, 30), OFFSET_PONE).format(null);
    }

}
