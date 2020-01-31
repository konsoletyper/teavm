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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HALF_DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HOURS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MICROS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MILLIS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MINUTES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

@Test
public class TestLocalTime extends AbstractDateTimeTest {

    private TLocalTime TEST_12_30_40_987654321;

    private static final TTemporalUnit[] INVALID_UNITS;
    static {
        EnumSet<TChronoUnit> set = EnumSet.range(WEEKS, FOREVER);
        INVALID_UNITS = (TTemporalUnit[]) set.toArray(new TTemporalUnit[set.size()]);
    }

    @Before
    public void setUp() {
        TEST_12_30_40_987654321 = TLocalTime.of(12, 30, 40, 987654321);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {TEST_12_30_40_987654321, TLocalTime.MIN, TLocalTime.MAX, TLocalTime.MIDNIGHT, TLocalTime.NOON};
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
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {
        List<TTemporalField> list = new ArrayList<TTemporalField>(Arrays.<TTemporalField>asList(TChronoField.values()));
        list.removeAll(validFields());
        list.add(TJulianFields.JULIAN_DAY);
        list.add(TJulianFields.MODIFIED_JULIAN_DAY);
        list.add(TJulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        assertEqualsSerialisedForm(TLocalTime.of(22, 17, 59, 460 * 1000000));
    }

    @Test
    public void test_serialization() throws IOException, ClassNotFoundException {
        assertSerializable(TEST_12_30_40_987654321);
    }

    //-----------------------------------------------------------------------
    private void check(TLocalTime time, int h, int m, int s, int n) {
        assertEquals(time.getHour(), h);
        assertEquals(time.getMinute(), m);
        assertEquals(time.getSecond(), s);
        assertEquals(time.getNano(), n);
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIDNIGHT() {
        check(TLocalTime.MIDNIGHT, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIDNIGHT_equal() {
        assertEquals(TLocalTime.MIDNIGHT, TLocalTime.MIDNIGHT);
        assertEquals(TLocalTime.MIDNIGHT, TLocalTime.of(0, 0));
    }

    @Test
    public void constant_MIDDAY() {
        check(TLocalTime.NOON, 12, 0, 0, 0);
    }

    @Test
    public void constant_MIDDAY_equal() {
        assertEquals(TLocalTime.NOON, TLocalTime.NOON);
        assertEquals(TLocalTime.NOON, TLocalTime.of(12, 0));
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIN_TIME() {
        check(TLocalTime.MIN, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIN_TIME_equal() {
        assertEquals(TLocalTime.MIN, TLocalTime.of(0, 0));
    }

    @Test
    public void constant_MAX_TIME() {
        check(TLocalTime.MAX, 23, 59, 59, 999999999);
    }

    @Test
    public void constant_MAX_TIME_equal() {
        assertEquals(TLocalTime.NOON, TLocalTime.NOON);
        assertEquals(TLocalTime.NOON, TLocalTime.of(12, 0));
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        TLocalTime expected = TLocalTime.now(TClock.systemDefaultZone());
        TLocalTime test = TLocalTime.now();
        long diff = Math.abs(test.toNanoOfDay() - expected.toNanoOfDay());
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(TZoneId)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_ZoneId_nullZoneId() {
        TLocalTime.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {
        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TLocalTime expected = TLocalTime.now(TClock.system(zone));
        TLocalTime test = TLocalTime.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TLocalTime.now(TClock.system(zone));
            test = TLocalTime.now(zone);
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(TClock)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        TLocalTime.now((TClock) null);
    }

    @Test
    public void now_Clock_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TLocalTime test = TLocalTime.now(clock);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    @Test
    public void now_Clock_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TLocalTime test = TLocalTime.now(clock);
            assertEquals(test.getHour(), ((i + 24 * 60 * 60) / (60 * 60)) % 24);
            assertEquals(test.getMinute(), ((i + 24 * 60 * 60) / 60) % 60);
            assertEquals(test.getSecond(), (i + 24 * 60 * 60) % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_max() {
        TClock clock = TClock.fixed(TInstant.MAX, TZoneOffset.UTC);
        TLocalTime test = TLocalTime.now(clock);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 999999999);
    }

    @Test
    public void now_Clock_min() {
        TClock clock = TClock.fixed(TInstant.MIN, TZoneOffset.UTC);
        TLocalTime test = TLocalTime.now(clock);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    // of() factories
    //-----------------------------------------------------------------------
    @Test
    public void factory_time_2ints() {
        TLocalTime test = TLocalTime.of(12, 30);
        check(test, 12, 30, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_2ints_hourTooLow() {
        TLocalTime.of(-1, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_2ints_hourTooHigh() {
        TLocalTime.of(24, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_2ints_minuteTooLow() {
        TLocalTime.of(0, -1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_2ints_minuteTooHigh() {
        TLocalTime.of(0, 60);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_time_3ints() {
        TLocalTime test = TLocalTime.of(12, 30, 40);
        check(test, 12, 30, 40, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_hourTooLow() {
        TLocalTime.of(-1, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_hourTooHigh() {
        TLocalTime.of(24, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_minuteTooLow() {
        TLocalTime.of(0, -1, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_minuteTooHigh() {
        TLocalTime.of(0, 60, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_secondTooLow() {
        TLocalTime.of(0, 0, -1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_3ints_secondTooHigh() {
        TLocalTime.of(0, 0, 60);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_time_4ints() {
        TLocalTime test = TLocalTime.of(12, 30, 40, 987654321);
        check(test, 12, 30, 40, 987654321);
        test = TLocalTime.of(12, 0, 40, 987654321);
        check(test, 12, 0, 40, 987654321);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_hourTooLow() {
        TLocalTime.of(-1, 0, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_hourTooHigh() {
        TLocalTime.of(24, 0, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_minuteTooLow() {
        TLocalTime.of(0, -1, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_minuteTooHigh() {
        TLocalTime.of(0, 60, 0, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_secondTooLow() {
        TLocalTime.of(0, 0, -1, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_secondTooHigh() {
        TLocalTime.of(0, 0, 60, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_nanoTooLow() {
        TLocalTime.of(0, 0, 0, -1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_time_4ints_nanoTooHigh() {
        TLocalTime.of(0, 0, 0, 1000000000);
    }

    //-----------------------------------------------------------------------
    // ofSecondOfDay(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofSecondOfDay() {
        TLocalTime localTime = TLocalTime.ofSecondOfDay(2 * 60 * 60 + 17 * 60 + 23);
        check(localTime, 2, 17, 23, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_tooLow() {
        TLocalTime.ofSecondOfDay(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_tooHigh() {
        TLocalTime.ofSecondOfDay(24 * 60 * 60);
    }

    //-----------------------------------------------------------------------
    // ofSecondOfDay(long, int)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofSecondOfDay_long_int() {
        TLocalTime localTime = TLocalTime.ofSecondOfDay(2 * 60 * 60 + 17 * 60 + 23, 987);
        check(localTime, 2, 17, 23, 987);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_long_int_tooLowSecs() {
        TLocalTime.ofSecondOfDay(-1, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_long_int_tooHighSecs() {
        TLocalTime.ofSecondOfDay(24 * 60 * 60, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_long_int_tooLowNanos() {
        TLocalTime.ofSecondOfDay(0, -1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofSecondOfDay_long_int_tooHighNanos() {
        TLocalTime.ofSecondOfDay(0, 1000000000);
    }

    //-----------------------------------------------------------------------
    // ofNanoOfDay(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofNanoOfDay() {
        TLocalTime localTime = TLocalTime.ofNanoOfDay(60 * 60 * 1000000000L + 17);
        check(localTime, 1, 0, 0, 17);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofNanoOfDay_tooLow() {
        TLocalTime.ofNanoOfDay(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_ofNanoOfDay_tooHigh() {
        TLocalTime.ofNanoOfDay(24 * 60 * 60 * 1000000000L);
    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_DateTimeAccessor() {
        assertEquals(TLocalTime.from(TLocalTime.of(17, 30)), TLocalTime.of(17, 30));
        assertEquals(TLocalTime.from(TLocalDateTime.of(2012, 5, 1, 17, 30)), TLocalTime.of(17, 30));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_from_DateTimeAccessor_invalid_noDerive() {
        TLocalTime.from(TLocalDate.of(2007, 7, 15));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_from_DateTimeAccessor_null() {
        TLocalTime.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider = "sampleToString")
    public void factory_parse_validText(int h, int m, int s, int n, String parsable) {
        TLocalTime t = TLocalTime.parse(parsable);
        assertNotNull(t, parsable);
        assertEquals(t.getHour(), h);
        assertEquals(t.getMinute(), m);
        assertEquals(t.getSecond(), s);
        assertEquals(t.getNano(), n);
    }

    @DataProvider(name="sampleBadParse")
    Object[][] provider_sampleBadParse() {
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

    @Test(dataProvider = "sampleBadParse", expectedExceptions={TDateTimeParseException.class})
    public void factory_parse_invalidText(String unparsable) {
        TLocalTime.parse(unparsable);
    }

    //-----------------------------------------------------------------------s
    @Test(expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_illegalHour() {
        TLocalTime.parse("25:00");
    }

    @Test(expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_illegalMinute() {
        TLocalTime.parse("12:60");
    }

    @Test(expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_illegalSecond() {
        TLocalTime.parse("12:12:60");
    }

    //-----------------------------------------------------------------------s
    @Test(expectedExceptions = {NullPointerException.class})
    public void factory_parse_nullTest() {
        TLocalTime.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // parse(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("H m s");
        TLocalTime test = TLocalTime.parse("14 30 40", f);
        assertEquals(test, TLocalTime.of(14, 30, 40));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("H m s");
        TLocalTime.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        TLocalTime.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // get(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        TLocalTime test = TEST_12_30_40_987654321;
        assertEquals(test.get(TChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.get(TChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.get(TChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.get(TChronoField.NANO_OF_SECOND), 987654321);

        assertEquals(test.get(TChronoField.SECOND_OF_DAY), 12 * 3600 + 30 * 60 + 40);
        assertEquals(test.get(TChronoField.MINUTE_OF_DAY), 12 * 60 + 30);
        assertEquals(test.get(TChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.get(TChronoField.CLOCK_HOUR_OF_AMPM), 12);
        assertEquals(test.get(TChronoField.CLOCK_HOUR_OF_DAY), 12);
        assertEquals(test.get(TChronoField.AMPM_OF_DAY), 1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_get_TemporalField_tooBig() {
        TEST_12_30_40_987654321.get(NANO_OF_DAY);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_get_TemporalField_null() {
        TEST_12_30_40_987654321.get((TTemporalField) null);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_get_TemporalField_invalidField() {
        TEST_12_30_40_987654321.get(MockFieldNoValue.INSTANCE);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_get_TemporalField_dateField() {
        TEST_12_30_40_987654321.get(TChronoField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------
    // getLong(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong_TemporalField() {
        TLocalTime test = TEST_12_30_40_987654321;
        assertEquals(test.getLong(TChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(TChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(TChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(TChronoField.NANO_OF_SECOND), 987654321);

        assertEquals(test.getLong(TChronoField.NANO_OF_DAY), ((12 * 3600 + 30 * 60 + 40) * 1000000000L) + 987654321);
        assertEquals(test.getLong(TChronoField.SECOND_OF_DAY), 12 * 3600 + 30 * 60 + 40);
        assertEquals(test.getLong(TChronoField.MINUTE_OF_DAY), 12 * 60 + 30);
        assertEquals(test.getLong(TChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(TChronoField.CLOCK_HOUR_OF_AMPM), 12);
        assertEquals(test.getLong(TChronoField.CLOCK_HOUR_OF_DAY), 12);
        assertEquals(test.getLong(TChronoField.AMPM_OF_DAY), 1);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_getLong_TemporalField_null() {
        TEST_12_30_40_987654321.getLong((TTemporalField) null);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_getLong_TemporalField_invalidField() {
        TEST_12_30_40_987654321.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_getLong_TemporalField_dateField() {
        TEST_12_30_40_987654321.getLong(TChronoField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.chronology()), null);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.localDate()), null);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.localTime()), TEST_12_30_40_987654321);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.offset()), null);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.precision()), TChronoUnit.NANOS);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.zone()), null);
        assertEquals(TEST_12_30_40_987654321.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_12_30_40_987654321.query(null);
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleTimes")
    Object[][] provider_sampleTimes() {
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
    @Test(dataProvider="sampleTimes")
    public void test_get(int h, int m, int s, int ns) {
        TLocalTime a = TLocalTime.of(h, m, s, ns);
        assertEquals(a.getHour(), h);
        assertEquals(a.getMinute(), m);
        assertEquals(a.getSecond(), s);
        assertEquals(a.getNano(), ns);
    }

    //-----------------------------------------------------------------------
    // with()
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final TLocalTime sample = TLocalTime.of(23, 5);
        TTemporalAdjuster adjuster = new TTemporalAdjuster() {
            @Override
            public TTemporal adjustInto(TTemporal dateTime) {
                return sample;
            }
        };
        assertEquals(TEST_12_30_40_987654321.with(adjuster), sample);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjustment_null() {
        TEST_12_30_40_987654321.with((TTemporalAdjuster) null);
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        TLocalTime t = TEST_12_30_40_987654321;
        for (int i = 0; i < 24; i++) {
            t = t.withHour(i);
            assertEquals(t.getHour(), i);
        }
    }

    @Test
    public void test_withHour_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.withHour(12);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_withHour_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(1, 0).withHour(0);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_withHour_toMidday_equal() {
        TLocalTime t = TLocalTime.of(1, 0).withHour(12);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withHour_hourTooLow() {
        TEST_12_30_40_987654321.withHour(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withHour_hourTooHigh() {
        TEST_12_30_40_987654321.withHour(24);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        TLocalTime t = TEST_12_30_40_987654321;
        for (int i = 0; i < 60; i++) {
            t = t.withMinute(i);
            assertEquals(t.getMinute(), i);
        }
    }

    @Test
    public void test_withMinute_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.withMinute(30);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_withMinute_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 1).withMinute(0);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_withMinute_toMidday_equals() {
        TLocalTime t = TLocalTime.of(12, 1).withMinute(0);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withMinute_minuteTooLow() {
        TEST_12_30_40_987654321.withMinute(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withMinute_minuteTooHigh() {
        TEST_12_30_40_987654321.withMinute(60);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        TLocalTime t = TEST_12_30_40_987654321;
        for (int i = 0; i < 60; i++) {
            t = t.withSecond(i);
            assertEquals(t.getSecond(), i);
        }
    }

    @Test
    public void test_withSecond_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.withSecond(40);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_withSecond_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 0, 1).withSecond(0);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_withSecond_toMidday_equal() {
        TLocalTime t = TLocalTime.of(12, 0, 1).withSecond(0);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withSecond_secondTooLow() {
        TEST_12_30_40_987654321.withSecond(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withSecond_secondTooHigh() {
        TEST_12_30_40_987654321.withSecond(60);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        TLocalTime t = TEST_12_30_40_987654321;
        t = t.withNano(1);
        assertEquals(t.getNano(), 1);
        t = t.withNano(10);
        assertEquals(t.getNano(), 10);
        t = t.withNano(100);
        assertEquals(t.getNano(), 100);
        t = t.withNano(999999999);
        assertEquals(t.getNano(), 999999999);
    }

    @Test
    public void test_withNanoOfSecond_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.withNano(987654321);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_withNanoOfSecond_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 0, 0, 1).withNano(0);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_withNanoOfSecond_toMidday_equal() {
        TLocalTime t = TLocalTime.of(12, 0, 0, 1).withNano(0);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withNanoOfSecond_nanoTooLow() {
        TEST_12_30_40_987654321.withNano(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_withNanoOfSecond_nanoTooHigh() {
        TEST_12_30_40_987654321.withNano(1000000000);
    }

    //-----------------------------------------------------------------------
    // truncated(TTemporalUnit)
    //-----------------------------------------------------------------------
    TTemporalUnit NINETY_MINS = new TTemporalUnit() {
        @Override
        public String toString() {
            return "NinetyMins";
        }
        @Override
        public TDuration getDuration() {
            return TDuration.ofMinutes(90);
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
        public boolean isSupportedBy(TTemporal temporal) {
            return false;
        }
        @Override
        public <R extends TTemporal> R addTo(R r, long l) {
            throw new UnsupportedOperationException();
        }
        @Override
        public long between(TTemporal r, TTemporal r2) {
            throw new UnsupportedOperationException();
        }
    };

    TTemporalUnit NINETY_FIVE_MINS = new TTemporalUnit() {
        @Override
        public String toString() {
            return "NinetyFiveMins";
        }
        @Override
        public TDuration getDuration() {
            return TDuration.ofMinutes(95);
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
        public boolean isSupportedBy(TTemporal temporal) {
            return false;
        }
        @Override
        public <R extends TTemporal> R addTo(R r, long l) {
            throw new UnsupportedOperationException();
        }
        @Override
        public long between(TTemporal r, TTemporal r2) {
            throw new UnsupportedOperationException();
        }
    };

    @DataProvider(name="truncatedToValid")
    Object[][] data_truncatedToValid() {
        return new Object[][] {
            {TLocalTime.of(1, 2, 3, 123456789), NANOS, TLocalTime.of(1, 2, 3, 123456789)},
            {TLocalTime.of(1, 2, 3, 123456789), MICROS, TLocalTime.of(1, 2, 3, 123456000)},
            {TLocalTime.of(1, 2, 3, 123456789), MILLIS, TLocalTime.of(1, 2, 3, 123000000)},
            {TLocalTime.of(1, 2, 3, 123456789), SECONDS, TLocalTime.of(1, 2, 3)},
            {TLocalTime.of(1, 2, 3, 123456789), MINUTES, TLocalTime.of(1, 2)},
            {TLocalTime.of(1, 2, 3, 123456789), HOURS, TLocalTime.of(1, 0)},
            {TLocalTime.of(1, 2, 3, 123456789), DAYS, TLocalTime.MIDNIGHT},

            {TLocalTime.of(1, 1, 1, 123456789), NINETY_MINS, TLocalTime.of(0, 0)},
            {TLocalTime.of(2, 1, 1, 123456789), NINETY_MINS, TLocalTime.of(1, 30)},
            {TLocalTime.of(3, 1, 1, 123456789), NINETY_MINS, TLocalTime.of(3, 0)},
        };
    }

    @Test(groups={"tck"}, dataProvider="truncatedToValid")
    public void test_truncatedTo_valid(TLocalTime input, TTemporalUnit unit, TLocalTime expected) {
        assertEquals(input.truncatedTo(unit), expected);
    }

    @DataProvider(name="truncatedToInvalid")
    Object[][] data_truncatedToInvalid() {
        return new Object[][] {
            {TLocalTime.of(1, 2, 3, 123456789), NINETY_FIVE_MINS},
            {TLocalTime.of(1, 2, 3, 123456789), WEEKS},
            {TLocalTime.of(1, 2, 3, 123456789), MONTHS},
            {TLocalTime.of(1, 2, 3, 123456789), YEARS},
        };
    }

    @Test(groups={"tck"}, dataProvider="truncatedToInvalid", expectedExceptions=TDateTimeException.class)
    public void test_truncatedTo_invalid(TLocalTime input, TTemporalUnit unit) {
        input.truncatedTo(unit);
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void test_truncatedTo_null() {
        TEST_12_30_40_987654321.truncatedTo(null);
    }

    //-----------------------------------------------------------------------
    // plus(PlusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Adjuster_positiveHours() {
        TTemporalAmount period = MockSimplePeriod.of(7, TChronoUnit.HOURS);
        TLocalTime t = TEST_12_30_40_987654321.plus(period);
        assertEquals(t, TLocalTime.of(19, 30, 40, 987654321));
    }

    @Test
    public void test_plus_Adjuster_negativeMinutes() {
        TTemporalAmount period = MockSimplePeriod.of(-25, TChronoUnit.MINUTES);
        TLocalTime t = TEST_12_30_40_987654321.plus(period);
        assertEquals(t, TLocalTime.of(12, 5, 40, 987654321));
    }

    @Test
    public void test_plus_Adjuster_zero() {
        TTemporalAmount period = TPeriod.ZERO;
        TLocalTime t = TEST_12_30_40_987654321.plus(period);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plus_Adjuster_wrap() {
        TTemporalAmount p = TDuration.ofHours(1);
        TLocalTime t = TLocalTime.of(23, 30).plus(p);
        assertEquals(t, TLocalTime.of(0, 30));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_plus_Adjuster_dateNotAllowed() {
        TTemporalAmount period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TEST_12_30_40_987654321.plus(period);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_Adjuster_null() {
        TEST_12_30_40_987654321.plus((TTemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // plus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_longPeriodUnit_positiveHours() {
        TLocalTime t = TEST_12_30_40_987654321.plus(7, TChronoUnit.HOURS);
        assertEquals(t, TLocalTime.of(19, 30, 40, 987654321));
    }

    @Test
    public void test_plus_longPeriodUnit_negativeMinutes() {
        TLocalTime t = TEST_12_30_40_987654321.plus(-25, TChronoUnit.MINUTES);
        assertEquals(t, TLocalTime.of(12, 5, 40, 987654321));
    }

    @Test
    public void test_plus_longPeriodUnit_zero() {
        TLocalTime t = TEST_12_30_40_987654321.plus(0, TChronoUnit.MINUTES);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plus_long_unit_invalidUnit() {
        for (TTemporalUnit unit : INVALID_UNITS) {
            try {
                TEST_12_30_40_987654321.plus(1, unit);
                fail("Unit should not be allowed " + unit);
            } catch (TDateTimeException ex) {
                // expected
            }
        }
    }

    @Test(expectedExceptions=TUnsupportedTemporalTypeException.class)
    public void test_plus_long_multiples() {
        TEST_12_30_40_987654321.plus(0, DAYS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_longPeriodUnit_null() {
        TEST_12_30_40_987654321.plus(1, (TTemporalUnit) null);
    }

    //-----------------------------------------------------------------------
    // plus(adjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_adjuster() {
        TDuration p = TDuration.ofSeconds(62, 3);
        TLocalTime t = TEST_12_30_40_987654321.plus(p);
        assertEquals(t, TLocalTime.of(12, 31, 42, 987654324));
    }

    @Test
    public void test_plus_adjuster_big() {
        TDuration p = TDuration.ofNanos(Long.MAX_VALUE);
        TLocalTime t = TEST_12_30_40_987654321.plus(p);
        assertEquals(t, TEST_12_30_40_987654321.plusNanos(Long.MAX_VALUE));
    }

    @Test
    public void test_plus_adjuster_zero_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plus(TPeriod.ZERO);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plus_adjuster_wrap() {
        TDuration p = TDuration.ofHours(1);
        TLocalTime t = TLocalTime.of(23, 30).plus(p);
        assertEquals(t, TLocalTime.of(0, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_adjuster_null() {
        TEST_12_30_40_987654321.plus((TTemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
        for (int i = 0; i < 50; i++) {
            t = t.plusHours(1);
            assertEquals(t.getHour(), (i + 1) % 24);
        }
    }

    @Test
    public void test_plusHours_fromZero() {
        TLocalTime base = TLocalTime.MIDNIGHT;
        for (int i = -50; i < 50; i++) {
            TLocalTime t = base.plusHours(i);
            assertEquals(t.getHour(), (i + 72) % 24);
        }
    }

    @Test
    public void test_plusHours_fromOne() {
        TLocalTime base = TLocalTime.of(1, 0);
        for (int i = -50; i < 50; i++) {
            TLocalTime t = base.plusHours(i);
            assertEquals(t.getHour(), (1 + i + 72) % 24);
        }
    }

    @Test
    public void test_plusHours_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusHours(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusHours_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(23, 0).plusHours(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_plusHours_toMidday_equal() {
        TLocalTime t = TLocalTime.of(11, 0).plusHours(1);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test
    public void test_plusHours_big() {
        TLocalTime t = TLocalTime.of(2, 30).plusHours(Long.MAX_VALUE);
        int hours = (int) (Long.MAX_VALUE % 24L);
        assertEquals(t, TLocalTime.of(2, 30).plusHours(hours));
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        for (int i = 0; i < 70; i++) {
            t = t.plusMinutes(1);
            min++;
            if (min == 60) {
                hour++;
                min = 0;
            }
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
        }
    }

    @Test
    public void test_plusMinutes_fromZero() {
        TLocalTime base = TLocalTime.MIDNIGHT;
        int hour;
        int min;
        for (int i = -70; i < 70; i++) {
            TLocalTime t = base.plusMinutes(i);
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
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
        }
    }

    @Test
    public void test_plusMinutes_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusMinutes(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusMinutes_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusMinutes(24 * 60);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusMinutes_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(23, 59).plusMinutes(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_plusMinutes_toMidday_equal() {
        TLocalTime t = TLocalTime.of(11, 59).plusMinutes(1);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test
    public void test_plusMinutes_big() {
        TLocalTime t = TLocalTime.of(2, 30).plusMinutes(Long.MAX_VALUE);
        int mins = (int) (Long.MAX_VALUE % (24L * 60L));
        assertEquals(t, TLocalTime.of(2, 30).plusMinutes(mins));
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
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
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
            assertEquals(t.getSecond(), sec);
        }
    }

    @DataProvider(name="plusSeconds_fromZero")
    Iterator<Object[]> plusSeconds_fromZero() {
        return new Iterator<Object[]>() {
            int delta = 30;
            int i = -3660;
            int hour = 22;
            int min = 59;
            int sec = 0;

            public boolean hasNext() {
                return i <= 3660;
            }

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

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test(dataProvider="plusSeconds_fromZero")
    public void test_plusSeconds_fromZero(int seconds, int hour, int min, int sec) {
        TLocalTime base = TLocalTime.MIDNIGHT;
        TLocalTime t = base.plusSeconds(seconds);

        assertEquals(hour, t.getHour());
        assertEquals(min, t.getMinute());
        assertEquals(sec, t.getSecond());
    }

    @Test
    public void test_plusSeconds_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusSeconds(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusSeconds_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusSeconds(24 * 60 * 60);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusSeconds_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(23, 59, 59).plusSeconds(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_plusSeconds_toMidday_equal() {
        TLocalTime t = TLocalTime.of(11, 59, 59).plusSeconds(1);
        assertEquals(t, TLocalTime.NOON);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos_halfABillion() {
        TLocalTime t = TLocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int nanos = 0;
        for (long i = 0; i < 3700 * 1000000000L; i+= 500000000) {
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
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
            assertEquals(t.getSecond(), sec);
            assertEquals(t.getNano(), nanos);
        }
    }

    @DataProvider(name="plusNanos_fromZero")
    Iterator<Object[]> plusNanos_fromZero() {
        return new Iterator<Object[]>() {
            long delta = 7500000000L;
            long i = -3660 * 1000000000L;
            int hour = 22;
            int min = 59;
            int sec = 0;
            long nanos = 0;

            public boolean hasNext() {
                return i <= 3660 * 1000000000L;
            }

            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec, (int)nanos};
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

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test(dataProvider="plusNanos_fromZero")
    public void test_plusNanos_fromZero(long nanoseconds, int hour, int min, int sec, int nanos) {
        TLocalTime base = TLocalTime.MIDNIGHT;
        TLocalTime t = base.plusNanos(nanoseconds);

        assertEquals(hour, t.getHour());
        assertEquals(min, t.getMinute());
        assertEquals(sec, t.getSecond());
        assertEquals(nanos, t.getNano());
    }

    @Test
    public void test_plusNanos_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusNanos(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusNanos_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.plusNanos(24 * 60 * 60 * 1000000000L);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_plusNanos_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(23, 59, 59, 999999999).plusNanos(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_plusNanos_toMidday_equal() {
        TLocalTime t = TLocalTime.of(11, 59, 59, 999999999).plusNanos(1);
        assertEquals(t, TLocalTime.NOON);
    }

    //-----------------------------------------------------------------------
    // minus(MinusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Adjuster() {
        TTemporalAmount p = TDuration.ofSeconds(62, 3);
        TLocalTime t = TEST_12_30_40_987654321.minus(p);
        assertEquals(t, TLocalTime.of(12, 29, 38, 987654318));
    }

    @Test
    public void test_minus_Adjuster_positiveHours() {
        TTemporalAmount period = MockSimplePeriod.of(7, TChronoUnit.HOURS);
        TLocalTime t = TEST_12_30_40_987654321.minus(period);
        assertEquals(t, TLocalTime.of(5, 30, 40, 987654321));
    }

    @Test
    public void test_minus_Adjuster_negativeMinutes() {
        TTemporalAmount period = MockSimplePeriod.of(-25, TChronoUnit.MINUTES);
        TLocalTime t = TEST_12_30_40_987654321.minus(period);
        assertEquals(t, TLocalTime.of(12, 55, 40, 987654321));
    }

    @Test
    public void test_minus_Adjuster_big1() {
        TTemporalAmount p = TDuration.ofNanos(Long.MAX_VALUE);
        TLocalTime t = TEST_12_30_40_987654321.minus(p);
        assertEquals(t, TEST_12_30_40_987654321.minusNanos(Long.MAX_VALUE));
    }

    @Test
    public void test_minus_Adjuster_zero() {
        TTemporalAmount p = TPeriod.ZERO;
        TLocalTime t = TEST_12_30_40_987654321.minus(p);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minus_Adjuster_wrap() {
        TTemporalAmount p = TDuration.ofHours(1);
        TLocalTime t = TLocalTime.of(0, 30).minus(p);
        assertEquals(t, TLocalTime.of(23, 30));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_minus_Adjuster_dateNotAllowed() {
        TTemporalAmount period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TEST_12_30_40_987654321.minus(period);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_Adjuster_null() {
        TEST_12_30_40_987654321.minus((TTemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // minus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_longPeriodUnit_positiveHours() {
        TLocalTime t = TEST_12_30_40_987654321.minus(7, TChronoUnit.HOURS);
        assertEquals(t, TLocalTime.of(5, 30, 40, 987654321));
    }

    @Test
    public void test_minus_longPeriodUnit_negativeMinutes() {
        TLocalTime t = TEST_12_30_40_987654321.minus(-25, TChronoUnit.MINUTES);
        assertEquals(t, TLocalTime.of(12, 55, 40, 987654321));
    }

    @Test
    public void test_minus_longPeriodUnit_zero() {
        TLocalTime t = TEST_12_30_40_987654321.minus(0, TChronoUnit.MINUTES);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minus_long_unit_invalidUnit() {
        for (TTemporalUnit unit : INVALID_UNITS) {
            try {
                TEST_12_30_40_987654321.minus(1, unit);
                fail("Unit should not be allowed " + unit);
            } catch (TDateTimeException ex) {
                // expected
            }
        }
    }

    @Test(expectedExceptions=TUnsupportedTemporalTypeException.class)
    public void test_minus_long_multiples() {
        TEST_12_30_40_987654321.minus(0, DAYS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_longPeriodUnit_null() {
        TEST_12_30_40_987654321.minus(1, (TTemporalUnit) null);
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
        for (int i = 0; i < 50; i++) {
            t = t.minusHours(1);
            assertEquals(t.getHour(), (((-i + 23) % 24) + 24) % 24, String.valueOf(i));
        }
    }

    @Test
    public void test_minusHours_fromZero() {
        TLocalTime base = TLocalTime.MIDNIGHT;
        for (int i = -50; i < 50; i++) {
            TLocalTime t = base.minusHours(i);
            assertEquals(t.getHour(), ((-i % 24) + 24) % 24);
        }
    }

    @Test
    public void test_minusHours_fromOne() {
        TLocalTime base = TLocalTime.of(1, 0);
        for (int i = -50; i < 50; i++) {
            TLocalTime t = base.minusHours(i);
            assertEquals(t.getHour(), (1 + (-i % 24) + 24) % 24);
        }
    }

    @Test
    public void test_minusHours_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusHours(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusHours_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(1, 0).minusHours(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_minusHours_toMidday_equal() {
        TLocalTime t = TLocalTime.of(13, 0).minusHours(1);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test
    public void test_minusHours_big() {
        TLocalTime t = TLocalTime.of(2, 30).minusHours(Long.MAX_VALUE);
        int hours = (int) (Long.MAX_VALUE % 24L);
        assertEquals(t, TLocalTime.of(2, 30).minusHours(hours));
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
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
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
        }
    }

    @Test
    public void test_minusMinutes_fromZero() {
        TLocalTime base = TLocalTime.MIDNIGHT;
        int hour = 22;
        int min = 49;
        for (int i = 70; i > -70; i--) {
            TLocalTime t = base.minusMinutes(i);
            min++;

            if (min == 60) {
                hour++;
                min = 0;

                if (hour == 24) {
                    hour = 0;
                }
            }

            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
        }
    }

    @Test
    public void test_minusMinutes_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusMinutes(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusMinutes_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusMinutes(24 * 60);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusMinutes_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 1).minusMinutes(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_minusMinutes_toMidday_equals() {
        TLocalTime t = TLocalTime.of(12, 1).minusMinutes(1);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test
    public void test_minusMinutes_big() {
        TLocalTime t = TLocalTime.of(2, 30).minusMinutes(Long.MAX_VALUE);
        int mins = (int) (Long.MAX_VALUE % (24L * 60L));
        assertEquals(t, TLocalTime.of(2, 30).minusMinutes(mins));
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds_one() {
        TLocalTime t = TLocalTime.MIDNIGHT;
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
            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
            assertEquals(t.getSecond(), sec);
        }
    }

    @DataProvider(name="minusSeconds_fromZero")
    Iterator<Object[]> minusSeconds_fromZero() {
        return new Iterator<Object[]>() {
            int delta = 30;
            int i = 3660;
            int hour = 22;
            int min = 59;
            int sec = 0;

            public boolean hasNext() {
                return i >= -3660;
            }

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

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test(dataProvider="minusSeconds_fromZero")
    public void test_minusSeconds_fromZero(int seconds, int hour, int min, int sec) {
        TLocalTime base = TLocalTime.MIDNIGHT;
        TLocalTime t = base.minusSeconds(seconds);

        assertEquals(t.getHour(), hour);
        assertEquals(t.getMinute(), min);
        assertEquals(t.getSecond(), sec);
    }

    @Test
    public void test_minusSeconds_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusSeconds(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusSeconds_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusSeconds(24 * 60 * 60);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusSeconds_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 0, 1).minusSeconds(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_minusSeconds_toMidday_equal() {
        TLocalTime t = TLocalTime.of(12, 0, 1).minusSeconds(1);
        assertEquals(t, TLocalTime.NOON);
    }

    @Test
    public void test_minusSeconds_big() {
        TLocalTime t = TLocalTime.of(2, 30).minusSeconds(Long.MAX_VALUE);
        int secs = (int) (Long.MAX_VALUE % (24L * 60L * 60L));
        assertEquals(t, TLocalTime.of(2, 30).minusSeconds(secs));
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos_halfABillion() {
        TLocalTime t = TLocalTime.MIDNIGHT;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int nanos = 0;
        for (long i = 0; i < 3700 * 1000000000L; i+= 500000000) {
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

            assertEquals(t.getHour(), hour);
            assertEquals(t.getMinute(), min);
            assertEquals(t.getSecond(), sec);
            assertEquals(t.getNano(), nanos);
        }
    }

    @DataProvider(name="minusNanos_fromZero")
    Iterator<Object[]> minusNanos_fromZero() {
        return new Iterator<Object[]>() {
            long delta = 7500000000L;
            long i = 3660 * 1000000000L;
            int hour = 22;
            int min = 59;
            int sec = 0;
            long nanos = 0;

            public boolean hasNext() {
                return i >= -3660 * 1000000000L;
            }

            public Object[] next() {
                final Object[] ret = new Object[] {i, hour, min, sec, (int)nanos};
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

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test(dataProvider="minusNanos_fromZero")
    public void test_minusNanos_fromZero(long nanoseconds, int hour, int min, int sec, int nanos) {
        TLocalTime base = TLocalTime.MIDNIGHT;
        TLocalTime t = base.minusNanos(nanoseconds);

        assertEquals(hour, t.getHour());
        assertEquals(min, t.getMinute());
        assertEquals(sec, t.getSecond());
        assertEquals(nanos, t.getNano());
    }

    @Test
    public void test_minusNanos_noChange_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusNanos(0);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusNanos_noChange_oneDay_equal() {
        TLocalTime t = TEST_12_30_40_987654321.minusNanos(24 * 60 * 60 * 1000000000L);
        assertEquals(t, TEST_12_30_40_987654321);
    }

    @Test
    public void test_minusNanos_toMidnight_equal() {
        TLocalTime t = TLocalTime.of(0, 0, 0, 1).minusNanos(1);
        assertEquals(t, TLocalTime.MIDNIGHT);
    }

    @Test
    public void test_minusNanos_toMidday_equal() {
        TLocalTime t = TLocalTime.of(12, 0, 0, 1).minusNanos(1);
        assertEquals(t, TLocalTime.NOON);
    }

    //-----------------------------------------------------------------------
    // until()
    //-----------------------------------------------------------------------
    @DataProvider(name="until")
    Object[][] provider_until() {
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

    @Test(dataProvider = "until")
    public void test_until(String startStr, String endStr, TTemporalUnit unit, long expected) {
        TLocalTime start = TLocalTime.parse(startStr);
        TLocalTime end = TLocalTime.parse(endStr);
        assertEquals(start.until(end, unit), expected);
        assertEquals(end.until(start, unit), -expected);
    }

    //-----------------------------------------------------------------------
    // atDate()
    //-----------------------------------------------------------------------
    @Test
    public void test_atDate() {
        TLocalTime t = TLocalTime.of(11, 30);
        assertEquals(t.atDate(TLocalDate.of(2012, 6, 30)), TLocalDateTime.of(2012, 6, 30, 11, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atDate_nullDate() {
        TEST_12_30_40_987654321.atDate((TLocalDate) null);
    }

    //-----------------------------------------------------------------------
    // toSecondOfDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_toSecondOfDay() {
        TLocalTime t = TLocalTime.of(0, 0);
        for (int i = 0; i < 24 * 60 * 60; i++) {
            assertEquals(t.toSecondOfDay(), i);
            t = t.plusSeconds(1);
        }
    }

    @Test
    public void test_toSecondOfDay_fromNanoOfDay_symmetry() {
        TLocalTime t = TLocalTime.of(0, 0);
        for (int i = 0; i < 24 * 60 * 60; i++) {
            assertEquals(TLocalTime.ofSecondOfDay(t.toSecondOfDay()), t);
            t = t.plusSeconds(1);
        }
    }

    //-----------------------------------------------------------------------
    // toNanoOfDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_toNanoOfDay() {
        TLocalTime t = TLocalTime.of(0, 0);
        for (int i = 0; i < 1000000; i++) {
            assertEquals(t.toNanoOfDay(), i);
            t = t.plusNanos(1);
        }
        t = TLocalTime.of(0, 0);
        for (int i = 1; i <= 1000000; i++) {
            t = t.minusNanos(1);
            assertEquals(t.toNanoOfDay(), 24 * 60 * 60 * 1000000000L - i);
        }
    }

    @Test
    public void test_toNanoOfDay_fromNanoOfDay_symmetry() {
        TLocalTime t = TLocalTime.of(0, 0);
        for (int i = 0; i < 1000000; i++) {
            assertEquals(TLocalTime.ofNanoOfDay(t.toNanoOfDay()), t);
            t = t.plusNanos(1);
        }
        t = TLocalTime.of(0, 0);
        for (int i = 1; i <= 1000000; i++) {
            t = t.minusNanos(1);
            assertEquals(TLocalTime.ofNanoOfDay(t.toNanoOfDay()), t);
        }
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_LocalTime(
            TLocalTime.MIDNIGHT,
            TLocalTime.of(0, 0, 0, 999999999),
            TLocalTime.of(0, 0, 59, 0),
            TLocalTime.of(0, 0, 59, 999999999),
            TLocalTime.of(0, 59, 0, 0),
            TLocalTime.of(0, 59, 0, 999999999),
            TLocalTime.of(0, 59, 59, 0),
            TLocalTime.of(0, 59, 59, 999999999),
            TLocalTime.NOON,
            TLocalTime.of(12, 0, 0, 999999999),
            TLocalTime.of(12, 0, 59, 0),
            TLocalTime.of(12, 0, 59, 999999999),
            TLocalTime.of(12, 59, 0, 0),
            TLocalTime.of(12, 59, 0, 999999999),
            TLocalTime.of(12, 59, 59, 0),
            TLocalTime.of(12, 59, 59, 999999999),
            TLocalTime.of(23, 0, 0, 0),
            TLocalTime.of(23, 0, 0, 999999999),
            TLocalTime.of(23, 0, 59, 0),
            TLocalTime.of(23, 0, 59, 999999999),
            TLocalTime.of(23, 59, 0, 0),
            TLocalTime.of(23, 59, 0, 999999999),
            TLocalTime.of(23, 59, 59, 0),
            TLocalTime.of(23, 59, 59, 999999999)
        );
    }

    void doTest_comparisons_LocalTime(TLocalTime... localTimes) {
        for (int i = 0; i < localTimes.length; i++) {
            TLocalTime a = localTimes[i];
            for (int j = 0; j < localTimes.length; j++) {
                TLocalTime b = localTimes[j];
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), true, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_ObjectNull() {
        TEST_12_30_40_987654321.compareTo(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_ObjectNull() {
        TEST_12_30_40_987654321.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_ObjectNull() {
        TEST_12_30_40_987654321.isAfter(null);
    }

    @Test(expectedExceptions=ClassCastException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonLocalTime() {
       Comparable c = TEST_12_30_40_987654321;
       c.compareTo(new Object());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_equals_true(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s, n);
        assertEquals(a.equals(b), true);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_hour_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h + 1, m, s, n);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_minute_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m + 1, s, n);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_second_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s + 1, n);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_nano_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s, n + 1);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_12_30_40_987654321.equals(TEST_12_30_40_987654321), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_12_30_40_987654321.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_12_30_40_987654321.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_hashCode_same(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s, n);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test(dataProvider="sampleTimes")
    public void test_hashCode_hour_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h + 1, m, s, n);
        assertEquals(a.hashCode() == b.hashCode(), false);
    }

    @Test(dataProvider="sampleTimes")
    public void test_hashCode_minute_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m + 1, s, n);
        assertEquals(a.hashCode() == b.hashCode(), false);
    }

    @Test(dataProvider="sampleTimes")
    public void test_hashCode_second_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s + 1, n);
        assertEquals(a.hashCode() == b.hashCode(), false);
    }

    @Test(dataProvider="sampleTimes")
    public void test_hashCode_nano_differs(int h, int m, int s, int n) {
        TLocalTime a = TLocalTime.of(h, m, s, n);
        TLocalTime b = TLocalTime.of(h, m, s, n + 1);
        assertEquals(a.hashCode() == b.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
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

    @Test(dataProvider="sampleToString")
    public void test_toString(int h, int m, int s, int n, String expected) {
        TLocalTime t = TLocalTime.of(h, m, s, n);
        String str = t.toString();
        assertEquals(str, expected);
    }

    //-----------------------------------------------------------------------
    // format(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("H m s");
        String t = TLocalTime.of(11, 30, 45).format(f);
        assertEquals(t, "11 30 45");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        TLocalTime.of(11, 30, 45).format(null);
    }

}
