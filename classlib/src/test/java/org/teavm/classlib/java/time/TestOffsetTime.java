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
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
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
public class TestOffsetTime extends AbstractDateTimeTest {

    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);
    private static final TZoneOffset OFFSET_PTWO = TZoneOffset.ofHours(2);
    private static final TLocalDate DATE = TLocalDate.of(2008, 12, 3);
    private TOffsetTime TEST_11_30_59_500_PONE;

    @Before
    public void setUp() {
        TEST_11_30_59_500_PONE = TOffsetTime.of(TLocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {TEST_11_30_59_500_PONE, TOffsetTime.MIN, TOffsetTime.MAX};
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
            OFFSET_SECONDS,
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
    public void test_serialization() throws Exception {
        assertSerializable(TEST_11_30_59_500_PONE);
        assertSerializable(TOffsetTime.MIN);
        assertSerializable(TOffsetTime.MAX);
    }

    @Test
    public void test_serialization_format() throws Exception {
        assertEqualsSerialisedForm(TOffsetTime.of(TLocalTime.of(22, 17, 59, 464000000), TZoneOffset.ofHours(1)));
    }

    //-----------------------------------------------------------------------
    // constants
    //-----------------------------------------------------------------------
    @Test
    public void constant_MIN() {
        check(TOffsetTime.MIN, 0, 0, 0, 0, TZoneOffset.MAX);
    }

    @Test
    public void constant_MAX() {
        check(TOffsetTime.MAX, 23, 59, 59, 999999999, TZoneOffset.MIN);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        TZonedDateTime nowDT = TZonedDateTime.now();

        TOffsetTime expected = TOffsetTime.now(TClock.systemDefaultZone());
        TOffsetTime test = TOffsetTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        assertTrue(diff < 100000000);  // less than 0.1 secs
        assertEquals(test.getOffset(), nowDT.getOffset());
    }

    //-----------------------------------------------------------------------
    // now(TClock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TOffsetTime test = TOffsetTime.now(clock);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 8);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TOffsetTime test = TOffsetTime.now(clock);
            assertEquals(test.getHour(), ((i + 24 * 60 * 60) / (60 * 60)) % 24);
            assertEquals(test.getMinute(), ((i + 24 * 60 * 60) / 60) % 60);
            assertEquals(test.getSecond(), (i + 24 * 60 * 60) % 60);
            assertEquals(test.getNano(), 8);
            assertEquals(test.getOffset(), TZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {
        TInstant base = TLocalDateTime.of(1970, 1, 1, 12, 0).toInstant(TZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            TZoneOffset offset = TZoneOffset.ofHours(i);
            TClock clock = TClock.fixed(base, offset);
            TOffsetTime test = TOffsetTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullZoneId() {
        TOffsetTime.now((TZoneId) null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        TOffsetTime.now((TClock) null);
    }

    //-----------------------------------------------------------------------
    // factories
    //-----------------------------------------------------------------------
    private void check(TOffsetTime test, int h, int m, int s, int n, TZoneOffset offset) {
        assertEquals(test.toLocalTime(), TLocalTime.of(h, m, s, n));
        assertEquals(test.getOffset(), offset);

        assertEquals(test.getHour(), h);
        assertEquals(test.getMinute(), m);
        assertEquals(test.getSecond(), s);
        assertEquals(test.getNano(), n);

        assertEquals(test, test);
        assertEquals(test.hashCode(), test.hashCode());
        assertEquals(TOffsetTime.of(TLocalTime.of(h, m, s, n), offset), test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHM() {
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PONE);
        check(test, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHMS() {
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHMSN() {
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_LocalTimeZoneOffset() {
        TLocalTime localTime = TLocalTime.of(11, 30, 10, 500);
        TOffsetTime test = TOffsetTime.of(localTime, OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_LocalTimeZoneOffset_nullTime() {
        TOffsetTime.of((TLocalTime) null, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_LocalTimeZoneOffset_nullOffset() {
        TLocalTime localTime = TLocalTime.of(11, 30, 10, 500);
        TOffsetTime.of(localTime, (TZoneOffset) null);
    }

    //-----------------------------------------------------------------------
    // ofInstant()
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_nullInstant() {
        TOffsetTime.ofInstant((TInstant) null, TZoneOffset.UTC);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_nullOffset() {
        TInstant instant = TInstant.ofEpochSecond(0L);
        TOffsetTime.ofInstant(instant, (TZoneOffset) null);
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TOffsetTime test = TOffsetTime.ofInstant(instant, TZoneOffset.UTC);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    @Test
    public void factory_ofInstant_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i, 8);
            TOffsetTime test = TOffsetTime.ofInstant(instant, TZoneOffset.UTC);
            assertEquals(test.getHour(), ((i + 24 * 60 * 60) / (60 * 60)) % 24);
            assertEquals(test.getMinute(), ((i + 24 * 60 * 60) / 60) % 60);
            assertEquals(test.getSecond(), (i + 24 * 60 * 60) % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_maxYear() {
        TOffsetTime test = TOffsetTime.ofInstant(TInstant.MAX, TZoneOffset.UTC);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 999999999);
    }

    @Test
    public void factory_ofInstant_minYear() {
        TOffsetTime test = TOffsetTime.ofInstant(TInstant.MIN, TZoneOffset.UTC);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    // from(TTemporalAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_TemporalAccessor_OT() {
        assertEquals(TOffsetTime.from(TOffsetTime.of(TLocalTime.of(17, 30), OFFSET_PONE)), TOffsetTime.of(TLocalTime.of(17, 30), OFFSET_PONE));
    }

    @Test
    public void test_from_TemporalAccessor_ZDT() {
        TZonedDateTime base = TLocalDateTime.of(2007, 7, 15, 11, 30, 59, 500).atZone(OFFSET_PONE);
        assertEquals(TOffsetTime.from(base), TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void factory_from_TemporalAccessor_invalid_noDerive() {
        TOffsetTime.from(TLocalDate.of(2007, 7, 15));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_from_TemporalAccessor_null() {
        TOffsetTime.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider = "sampleToString")
    public void factory_parse_validText(int h, int m, int s, int n, String offsetId, String parsable) {
        TOffsetTime t = TOffsetTime.parse(parsable);
        assertNotNull(t, parsable);
        check(t, h, m, s, n, TZoneOffset.of(offsetId));
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
                {"11:30"},
                {"11:30+01:00[Europe/Paris]"},
        };
    }

    @Test(dataProvider = "sampleBadParse", expectedExceptions={TDateTimeParseException.class})
    public void factory_parse_invalidText(String unparsable) {
        TOffsetTime.parse(unparsable);
    }

    //-----------------------------------------------------------------------s
    @Test(expectedExceptions={TDateTimeParseException.class})
    public void factory_parse_illegalHour() {
        TOffsetTime.parse("25:00+01:00");
    }

    @Test(expectedExceptions={TDateTimeParseException.class})
    public void factory_parse_illegalMinute() {
        TOffsetTime.parse("12:60+01:00");
    }

    @Test(expectedExceptions={TDateTimeParseException.class})
    public void factory_parse_illegalSecond() {
        TOffsetTime.parse("12:12:60+01:00");
    }

    //-----------------------------------------------------------------------
    // parse(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("H m s XXX");
        TOffsetTime test = TOffsetTime.parse("11 30 0 +01:00", f);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30), TZoneOffset.ofHours(1)));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M d H m s");
        TOffsetTime.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        TOffsetTime.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // constructor
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullTime() throws Throwable  {
        Constructor<TOffsetTime> con = TOffsetTime.class.getDeclaredConstructor(TLocalTime.class, TZoneOffset.class);
        con.setAccessible(true);
        try {
            con.newInstance(null, OFFSET_PONE);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullOffset() throws Throwable  {
        Constructor<TOffsetTime> con = TOffsetTime.class.getDeclaredConstructor(TLocalTime.class, TZoneOffset.class);
        con.setAccessible(true);
        try {
            con.newInstance(TLocalTime.of(11, 30), null);
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
            {11, 30, 20, 500, OFFSET_PONE},
            {11, 0, 0, 0, OFFSET_PONE},
            {23, 59, 59, 999999999, OFFSET_PONE},
        };
    }

    @Test(dataProvider="sampleTimes")
    public void test_get(int h, int m, int s, int n, TZoneOffset offset) {
        TLocalTime localTime = TLocalTime.of(h, m, s, n);
        TOffsetTime a = TOffsetTime.of(localTime, offset);

        assertEquals(a.toLocalTime(), localTime);
        assertEquals(a.getOffset(), offset);
        assertEquals(a.toString(), localTime.toString() + offset.toString());
        assertEquals(a.getHour(), localTime.getHour());
        assertEquals(a.getMinute(), localTime.getMinute());
        assertEquals(a.getSecond(), localTime.getSecond());
        assertEquals(a.getNano(), localTime.getNano());
    }

    //-----------------------------------------------------------------------
    // get(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
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
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
        assertEquals(test.getLong(TChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(TChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(TChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(TChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.getLong(TChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(TChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.getLong(TChronoField.OFFSET_SECONDS), 3600);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.chronology()), null);
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.localDate()), null);
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.localTime()), TEST_11_30_59_500_PONE.toLocalTime());
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.offset()), TEST_11_30_59_500_PONE.getOffset());
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.precision()), TChronoUnit.NANOS);
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.zone()), TEST_11_30_59_500_PONE.getOffset());
        assertEquals(TEST_11_30_59_500_PONE.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_11_30_59_500_PONE.query(null);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameLocal() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withOffsetSameLocal(OFFSET_PTWO);
        assertEquals(test.toLocalTime(), base.toLocalTime());
        assertEquals(test.getOffset(), OFFSET_PTWO);
    }

    @Test
    public void test_withOffsetSameLocal_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withOffsetSameLocal(OFFSET_PONE);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameLocal_null() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameLocal(null);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameInstant() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        TOffsetTime expected = TOffsetTime.of(TLocalTime.of(12, 30, 59), OFFSET_PTWO);
        assertEquals(test, expected);
    }

    @Test
    public void test_withOffsetSameInstant_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withOffsetSameInstant(OFFSET_PONE);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameInstant_null() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        base.withOffsetSameInstant(null);
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final TOffsetTime sample = TOffsetTime.of(TLocalTime.of(23, 5), OFFSET_PONE);
        TTemporalAdjuster adjuster = new TTemporalAdjuster() {
            @Override
            public TTemporal adjustInto(TTemporal dateTime) {
                return sample;
            }
        };
        assertEquals(TEST_11_30_59_500_PONE.with(adjuster), sample);
    }

    @Test
    public void test_with_adjustment_LocalTime() {
        TOffsetTime test = TEST_11_30_59_500_PONE.with(TLocalTime.of(13, 30));
        assertEquals(test, TOffsetTime.of(TLocalTime.of(13, 30), OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_OffsetTime() {
        TOffsetTime test = TEST_11_30_59_500_PONE.with(TOffsetTime.of(TLocalTime.of(13, 35), OFFSET_PTWO));
        assertEquals(test, TOffsetTime.of(TLocalTime.of(13, 35), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {
        TOffsetTime test = TEST_11_30_59_500_PONE.with(OFFSET_PTWO);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30, 59, 500), OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_AmPm() {
        TOffsetTime test = TEST_11_30_59_500_PONE.with(new TTemporalAdjuster() {
            @Override
            public TTemporal adjustInto(TTemporal dateTime) {
                return dateTime.with(HOUR_OF_DAY, 23);
            }
        });
        assertEquals(test, TOffsetTime.of(TLocalTime.of(23, 30, 59, 500), OFFSET_PONE));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjustment_null() {
        TEST_11_30_59_500_PONE.with((TTemporalAdjuster) null);
    }

    //-----------------------------------------------------------------------
    // with(TTemporalField, long)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_TemporalField() {
        TOffsetTime test = TOffsetTime.of(TLocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
        assertEquals(test.with(TChronoField.HOUR_OF_DAY, 15), TOffsetTime.of(TLocalTime.of(15, 30, 40, 987654321), OFFSET_PONE));
        assertEquals(test.with(TChronoField.MINUTE_OF_HOUR, 50), TOffsetTime.of(TLocalTime.of(12, 50, 40, 987654321), OFFSET_PONE));
        assertEquals(test.with(TChronoField.SECOND_OF_MINUTE, 50), TOffsetTime.of(TLocalTime.of(12, 30, 50, 987654321), OFFSET_PONE));
        assertEquals(test.with(TChronoField.NANO_OF_SECOND, 12345), TOffsetTime.of(TLocalTime.of(12, 30, 40, 12345), OFFSET_PONE));
        assertEquals(test.with(TChronoField.HOUR_OF_AMPM, 6), TOffsetTime.of(TLocalTime.of(18, 30, 40, 987654321), OFFSET_PONE));
        assertEquals(test.with(TChronoField.AMPM_OF_DAY, 0), TOffsetTime.of(TLocalTime.of(0, 30, 40, 987654321), OFFSET_PONE));

        assertEquals(test.with(TChronoField.OFFSET_SECONDS, 7205), TOffsetTime.of(TLocalTime.of(12, 30, 40, 987654321), TZoneOffset.ofHoursMinutesSeconds(2, 0, 5)));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_TemporalField_null() {
        TEST_11_30_59_500_PONE.with((TTemporalField) null, 0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_with_TemporalField_invalidField() {
        TEST_11_30_59_500_PONE.with(TChronoField.YEAR, 0);
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withHour(15);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(15, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_withHour_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withHour(11);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withMinute(15);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 15, 59), OFFSET_PONE));
    }

    @Test
    public void test_withMinute_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withMinute(30);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withSecond(15);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30, 15), OFFSET_PONE));
    }

    @Test
    public void test_withSecond_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.withSecond(59);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        TOffsetTime test = base.withNano(15);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30, 59, 15), OFFSET_PONE));
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        TOffsetTime test = base.withNano(1);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TTemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(NANOS), TEST_11_30_59_500_PONE);
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(SECONDS), TEST_11_30_59_500_PONE.withNano(0));
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(DAYS), TEST_11_30_59_500_PONE.with(TLocalTime.MIDNIGHT));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_truncatedTo_null() {
        TEST_11_30_59_500_PONE.truncatedTo(null);
    }

    //-----------------------------------------------------------------------
    // plus(PlusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_PlusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MINUTES);
        TOffsetTime t = TEST_11_30_59_500_PONE.plus(period);
        assertEquals(t, TOffsetTime.of(TLocalTime.of(11, 37, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_plus_PlusAdjuster_noChange() {
        TOffsetTime t = TEST_11_30_59_500_PONE.plus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test
    public void test_plus_PlusAdjuster_zero() {
        TOffsetTime t = TEST_11_30_59_500_PONE.plus(TPeriod.ZERO);
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_PlusAdjuster_null() {
        TEST_11_30_59_500_PONE.plus(null);
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusHours(13);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(0, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusHours_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusHours(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusMinutes(30);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(12, 0, 59), OFFSET_PONE));
    }

    @Test
    public void test_plusMinutes_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusMinutes(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusSeconds(1);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 31, 0), OFFSET_PONE));
    }

    @Test
    public void test_plusSeconds_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusSeconds(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        TOffsetTime test = base.plusNanos(1);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30, 59, 1), OFFSET_PONE));
    }

    @Test
    public void test_plusNanos_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.plusNanos(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minus(MinusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_MinusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MINUTES);
        TOffsetTime t = TEST_11_30_59_500_PONE.minus(period);
        assertEquals(t, TOffsetTime.of(TLocalTime.of(11, 23, 59, 500), OFFSET_PONE));
    }

    @Test
    public void test_minus_MinusAdjuster_noChange() {
        TOffsetTime t = TEST_11_30_59_500_PONE.minus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test
    public void test_minus_MinusAdjuster_zero() {
        TOffsetTime t = TEST_11_30_59_500_PONE.minus(TPeriod.ZERO);
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_MinusAdjuster_null() {
        TEST_11_30_59_500_PONE.minus(null);
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusHours(-13);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(0, 30, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusHours_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusHours(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusMinutes(50);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(10, 40, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusMinutes_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusMinutes(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusSeconds(60);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 29, 59), OFFSET_PONE));
    }

    @Test
    public void test_minusSeconds_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusSeconds(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        TOffsetTime test = base.minusNanos(1);
        assertEquals(test, TOffsetTime.of(TLocalTime.of(11, 30, 58, 999999999), OFFSET_PONE));
    }

    @Test
    public void test_minusNanos_zero() {
        TOffsetTime base = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        TOffsetTime test = base.minusNanos(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_time() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 29), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PONE);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_offset() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PTWO);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_both() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 50), OFFSET_PTWO);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 20), OFFSET_PONE);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_bothNearStartOfDay() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(0, 10), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(2, 30), OFFSET_PTWO);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(10, 0), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 0), OFFSET_PTWO);  // a is before b despite being same time-line time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) == 0, true);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_null() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.compareTo(null);
    }

    @Test(expectedExceptions=ClassCastException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonOffsetTime() {
       Comparable c = TEST_11_30_59_500_PONE;
       c.compareTo(new Object());
    }

    private TInstant convertInstant(TOffsetTime ot) {
        return DATE.atTime(ot.toLocalTime()).toInstant(ot.getOffset());
    }

    //-----------------------------------------------------------------------
    // isAfter() / isBefore() / isEqual()
    //-----------------------------------------------------------------------
    @Test
    public void test_isBeforeIsAfterIsEqual1() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 58), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);  // a is before b due to time
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
    public void test_isBeforeIsAfterIsEqual1nanos() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59, 3), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30, 59, 4), OFFSET_PONE);  // a is before b due to time
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
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PTWO);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30, 58), OFFSET_PONE);  // a is before b due to offset
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
    public void test_isBeforeIsAfterIsEqual2nanos() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59, 4), TZoneOffset.ofTotalSeconds(OFFSET_PONE.getTotalSeconds() + 1));
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(11, 30, 59, 3), OFFSET_PONE);  // a is before b due to offset
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
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PTWO);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(10, 30, 59), OFFSET_PONE);  // a is same instant as b
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
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_null() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isAfter(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isEqual_null() {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(11, 30, 59), OFFSET_PONE);
        a.isEqual(null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_equals_true(int h, int m, int s, int n, TZoneOffset ignored) {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_hour_differs(int h, int m, int s, int n, TZoneOffset ignored) {
        h = (h == 23 ? 22 : h);
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h + 1, m, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_minute_differs(int h, int m, int s, int n, TZoneOffset ignored) {
        m = (m == 59 ? 58 : m);
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h, m + 1, s, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_second_differs(int h, int m, int s, int n, TZoneOffset ignored) {
        s = (s == 59 ? 58 : s);
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h, m, s + 1, n), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_nano_differs(int h, int m, int s, int n, TZoneOffset ignored) {
        n = (n == 999999999 ? 999999998 : n);
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h, m, s, n + 1), OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_offset_differs(int h, int m, int s, int n, TZoneOffset ignored) {
        TOffsetTime a = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PONE);
        TOffsetTime b = TOffsetTime.of(TLocalTime.of(h, m, s, n), OFFSET_PTWO);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_11_30_59_500_PONE.equals(TEST_11_30_59_500_PONE), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_11_30_59_500_PONE.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_11_30_59_500_PONE.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
        return new Object[][] {
            {11, 30, 59, 0, "Z", "11:30:59Z"},
            {11, 30, 59, 0, "+01:00", "11:30:59+01:00"},
            {11, 30, 59, 999000000, "Z", "11:30:59.999Z"},
            {11, 30, 59, 999000000, "+01:00", "11:30:59.999+01:00"},
            {11, 30, 59, 999000, "Z", "11:30:59.000999Z"},
            {11, 30, 59, 999000, "+01:00", "11:30:59.000999+01:00"},
            {11, 30, 59, 999, "Z", "11:30:59.000000999Z"},
            {11, 30, 59, 999, "+01:00", "11:30:59.000000999+01:00"},
        };
    }

    @Test(dataProvider="sampleToString")
    public void test_toString(int h, int m, int s, int n, String offsetId, String expected) {
        TOffsetTime t = TOffsetTime.of(TLocalTime.of(h, m, s, n), TZoneOffset.of(offsetId));
        String str = t.toString();
        assertEquals(str, expected);
    }

    //-----------------------------------------------------------------------
    // format(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("H m s");
        String t = TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PONE).format(f);
        assertEquals(t, "11 30 0");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        TOffsetTime.of(TLocalTime.of(11, 30), OFFSET_PONE).format(null);
    }

}
