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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.CENTURIES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DECADES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MILLENNIA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.MockFieldNoValue;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public class TestLocalDate extends AbstractDateTimeTest {

    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);

    private static final TZoneId ZONE_PARIS = TZoneId.of("Europe/Paris");

    private static final TZoneId ZONE_GAZA = TZoneId.of("Asia/Gaza");

    private TLocalDate TEST_2007_07_15;

    private long MAX_VALID_EPOCHDAYS;

    private long MIN_VALID_EPOCHDAYS;

    private TLocalDate MAX_DATE;

    private TLocalDate MIN_DATE;

    private TInstant MAX_INSTANT;

    private TInstant MIN_INSTANT;

    @Before
    public void setUp() {

        this.TEST_2007_07_15 = TLocalDate.of(2007, 7, 15);

        TLocalDate max = TLocalDate.MAX;
        TLocalDate min = TLocalDate.MIN;
        this.MAX_VALID_EPOCHDAYS = max.toEpochDay();
        this.MIN_VALID_EPOCHDAYS = min.toEpochDay();
        this.MAX_DATE = max;
        this.MIN_DATE = min;
        this.MAX_INSTANT = max.atStartOfDay(TZoneOffset.UTC).toInstant();
        this.MIN_INSTANT = min.atStartOfDay(TZoneOffset.UTC).toInstant();
    }

    @Override
    protected List<TTemporalAccessor> samples() {

        TTemporalAccessor[] array = { this.TEST_2007_07_15, TLocalDate.MAX, TLocalDate.MIN, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {

        TTemporalField[] array = { DAY_OF_WEEK, ALIGNED_DAY_OF_WEEK_IN_MONTH, ALIGNED_DAY_OF_WEEK_IN_YEAR, DAY_OF_MONTH,
        DAY_OF_YEAR, EPOCH_DAY, ALIGNED_WEEK_OF_MONTH, ALIGNED_WEEK_OF_YEAR, MONTH_OF_YEAR, PROLEPTIC_MONTH,
        YEAR_OF_ERA, YEAR, ERA, TJulianFields.JULIAN_DAY, TJulianFields.MODIFIED_JULIAN_DAY, TJulianFields.RATA_DIE, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {

        List<TTemporalField> list = new ArrayList<>(Arrays.<TTemporalField> asList(TChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    @Test
    public void test_serialization() throws IOException, ClassNotFoundException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this.TEST_2007_07_15);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(ois.readObject(), this.TEST_2007_07_15);
    }

    @Test
    public void test_immutable() {

        Class<TLocalDate> cls = TLocalDate.class;
        assertTrue(Modifier.isPublic(cls.getModifiers()));
        assertTrue(Modifier.isFinal(cls.getModifiers()));
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().contains("$") == false) {
                if (Modifier.isStatic(field.getModifiers())) {
                    assertTrue("Field:" + field.getName(), Modifier.isFinal(field.getModifiers()));
                } else {
                    assertTrue("Field:" + field.getName(), Modifier.isPrivate(field.getModifiers()));
                    assertTrue("Field:" + field.getName(), Modifier.isFinal(field.getModifiers()));
                }
            }
        }
    }

    private void check(TLocalDate test_2008_02_29, int y, int m, int d) {

        assertEquals(test_2008_02_29.getYear(), y);
        assertEquals(test_2008_02_29.getMonth().getValue(), m);
        assertEquals(test_2008_02_29.getDayOfMonth(), d);
    }

    @Test
    public void now() {

        TLocalDate expected = TLocalDate.now(TClock.systemDefaultZone());
        TLocalDate test = TLocalDate.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TLocalDate.now(TClock.systemDefaultZone());
            test = TLocalDate.now();
        }
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void now_ZoneId_nullZoneId() {

        TLocalDate.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {

        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TLocalDate expected = TLocalDate.now(TClock.system(zone));
        TLocalDate test = TLocalDate.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TLocalDate.now(TClock.system(zone));
            test = TLocalDate.now(zone);
        }
        assertEquals(test, expected);
    }

    @Test(expected = NullPointerException.class)
    public void now_Clock_nullClock() {

        TLocalDate.now((TClock) null);
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {

        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TLocalDate test = TLocalDate.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
        }
    }

    @Test
    public void now_Clock_allSecsInDay_offset() {

        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i);
            TClock clock = TClock.fixed(instant.minusSeconds(OFFSET_PONE.getTotalSeconds()), OFFSET_PONE);
            TLocalDate test = TLocalDate.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60) ? 1 : 2);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {

        for (int i = -1; i >= -(2 * 24 * 60 * 60); i--) {
            TInstant instant = TInstant.ofEpochSecond(i);
            TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
            TLocalDate test = TLocalDate.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), TMonth.DECEMBER);
            assertEquals(test.getDayOfMonth(), (i >= -24 * 60 * 60 ? 31 : 30));
        }
    }

    @Test
    public void now_Clock_maxYear() {

        TClock clock = TClock.fixed(this.MAX_INSTANT, TZoneOffset.UTC);
        TLocalDate test = TLocalDate.now(clock);
        assertEquals(test, this.MAX_DATE);
    }

    @Test(expected = TDateTimeException.class)
    public void now_Clock_tooBig() {

        TClock clock = TClock.fixed(this.MAX_INSTANT.plusSeconds(24 * 60 * 60), TZoneOffset.UTC);
        TLocalDate.now(clock);
    }

    @Test
    public void now_Clock_minYear() {

        TClock clock = TClock.fixed(this.MIN_INSTANT, TZoneOffset.UTC);
        TLocalDate test = TLocalDate.now(clock);
        assertEquals(test, this.MIN_DATE);
    }

    @Test(expected = TDateTimeException.class)
    public void now_Clock_tooLow() {

        TClock clock = TClock.fixed(this.MIN_INSTANT.minusNanos(1), TZoneOffset.UTC);
        TLocalDate.now(clock);
    }

    @Test
    public void factory_of_intsMonth() {

        assertEquals(this.TEST_2007_07_15, TLocalDate.of(2007, TMonth.JULY, 15));
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_intsMonth_29febNonLeap() {

        TLocalDate.of(2007, TMonth.FEBRUARY, 29);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_intsMonth_31apr() {

        TLocalDate.of(2007, TMonth.APRIL, 31);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_intsMonth_dayTooLow() {

        TLocalDate.of(2007, TMonth.JANUARY, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_intsMonth_dayTooHigh() {

        TLocalDate.of(2007, TMonth.JANUARY, 32);
    }

    @Test(expected = NullPointerException.class)
    public void factory_of_intsMonth_nullMonth() {

        TLocalDate.of(2007, null, 30);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_intsMonth_yearTooLow() {

        TLocalDate.of(Integer.MIN_VALUE, TMonth.JANUARY, 1);
    }

    @Test
    public void factory_of_ints() {

        check(this.TEST_2007_07_15, 2007, 7, 15);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_29febNonLeap() {

        TLocalDate.of(2007, 2, 29);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_31apr() {

        TLocalDate.of(2007, 4, 31);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_dayTooLow() {

        TLocalDate.of(2007, 1, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_dayTooHigh() {

        TLocalDate.of(2007, 1, 32);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_monthTooLow() {

        TLocalDate.of(2007, 0, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_monthTooHigh() {

        TLocalDate.of(2007, 13, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_of_ints_yearTooLow() {

        TLocalDate.of(Integer.MIN_VALUE, 1, 1);
    }

    @Test
    public void factory_ofYearDay_ints_nonLeap() {

        TLocalDate date = TLocalDate.of(2007, 1, 1);
        for (int i = 1; i <= 365; i++) {
            assertEquals(TLocalDate.ofYearDay(2007, i), date);
            date = next(date);
        }
    }

    @Test
    public void factory_ofYearDay_ints_leap() {

        TLocalDate date = TLocalDate.of(2008, 1, 1);
        for (int i = 1; i <= 366; i++) {
            assertEquals(TLocalDate.ofYearDay(2008, i), date);
            date = next(date);
        }
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofYearDay_ints_366nonLeap() {

        TLocalDate.ofYearDay(2007, 366);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofYearDay_ints_dayTooLow() {

        TLocalDate.ofYearDay(2007, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofYearDay_ints_dayTooHigh() {

        TLocalDate.ofYearDay(2007, 367);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofYearDay_ints_yearTooLow() {

        TLocalDate.ofYearDay(Integer.MIN_VALUE, 1);
    }

    private TLocalDate next(TLocalDate date) {

        int newDayOfMonth = date.getDayOfMonth() + 1;
        if (newDayOfMonth <= date.getMonth().length(isIsoLeap(date.getYear()))) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.withDayOfMonth(1);
        if (date.getMonth() == TMonth.DECEMBER) {
            date = date.withYear(date.getYear() + 1);
        }
        return date.with(date.getMonth().plus(1));
    }

    private TLocalDate previous(TLocalDate date) {

        int newDayOfMonth = date.getDayOfMonth() - 1;
        if (newDayOfMonth > 0) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.with(date.getMonth().minus(1));
        if (date.getMonth() == TMonth.DECEMBER) {
            date = date.withYear(date.getYear() - 1);
        }
        return date.withDayOfMonth(date.getMonth().length(isIsoLeap(date.getYear())));
    }

    @Test
    public void factory_ofEpochDay() {

        long date_0000_01_01 = -678941 - 40587;
        assertEquals(TLocalDate.ofEpochDay(0), TLocalDate.of(1970, 1, 1));
        assertEquals(TLocalDate.ofEpochDay(date_0000_01_01), TLocalDate.of(0, 1, 1));
        assertEquals(TLocalDate.ofEpochDay(date_0000_01_01 - 1), TLocalDate.of(-1, 12, 31));
        assertEquals(TLocalDate.ofEpochDay(this.MAX_VALID_EPOCHDAYS), TLocalDate.of(TYear.MAX_VALUE, 12, 31));
        assertEquals(TLocalDate.ofEpochDay(this.MIN_VALID_EPOCHDAYS), TLocalDate.of(TYear.MIN_VALUE, 1, 1));

        TLocalDate test = TLocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i < 700000; i++) {
            assertEquals(TLocalDate.ofEpochDay(i), test);
            test = next(test);
        }
        test = TLocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i > -2000000; i--) {
            assertEquals(TLocalDate.ofEpochDay(i), test);
            test = previous(test);
        }
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofEpochDay_aboveMax() {

        TLocalDate.ofEpochDay(this.MAX_VALID_EPOCHDAYS + 1);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofEpochDay_belowMin() {

        TLocalDate.ofEpochDay(this.MIN_VALID_EPOCHDAYS - 1);
    }

    @Test
    public void test_factory_CalendricalObject() {

        assertEquals(TLocalDate.from(TLocalDate.of(2007, 7, 15)), TLocalDate.of(2007, 7, 15));
        assertEquals(TLocalDate.from(TLocalDateTime.of(2007, 7, 15, 12, 30)), TLocalDate.of(2007, 7, 15));
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {

        TLocalDate.from(TLocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_CalendricalObject_null() {

        TLocalDate.from((TTemporalAccessor) null);
    }

    Object[][] provider_sampleBadParse() {

        return new Object[][] { { "2008/07/05" }, { "10000-01-01" }, { "2008-1-1" }, { "2008--01" }, { "ABCD-02-01" },
        { "2008-AB-01" }, { "2008-02-AB" }, { "-0000-02-01" }, { "2008-02-01Z" }, { "2008-02-01+01:00" },
        { "2008-02-01+01:00[Europe/Paris]" }, };
    }

    @Test
    public void factory_parse_invalidText() {

        for (Object[] data : provider_sampleBadParse()) {
            String unparsable = (String) data[0];

            try {
                TLocalDate.parse(unparsable);
                fail("Expected TDateTimeParseException");
            } catch (TDateTimeParseException e) {
                // expected
            }
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_illegalValue() {

        TLocalDate.parse("2008-06-32");
    }

    @Test(expected = TDateTimeParseException.class)
    public void factory_parse_invalidValue() {

        TLocalDate.parse("2008-06-31");
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_nullText() {

        TLocalDate.parse((String) null);
    }

    @Test
    public void factory_parse_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M d");
        TLocalDate test = TLocalDate.parse("2010 12 3", f);
        assertEquals(test, TLocalDate.of(2010, 12, 3));
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullText() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u M d");
        TLocalDate.parse((String) null, f);
    }

    @Test(expected = NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {

        TLocalDate.parse("ANY", null);
    }

    @Test
    public void test_get_TemporalField() {

        TLocalDate test = TLocalDate.of(2008, 6, 30);
        assertEquals(test.get(YEAR), 2008);
        assertEquals(test.get(MONTH_OF_YEAR), 6);
        assertEquals(test.get(DAY_OF_MONTH), 30);
        assertEquals(test.get(DAY_OF_WEEK), 1);
        assertEquals(test.get(DAY_OF_YEAR), 182);
        assertEquals(test.get(YEAR_OF_ERA), 2008);
        assertEquals(test.get(ERA), 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_tooBig() {

        this.TEST_2007_07_15.get(EPOCH_DAY);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_TemporalField_null() {

        this.TEST_2007_07_15.get((TTemporalField) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_invalidField() {

        this.TEST_2007_07_15.get(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_get_TemporalField_timeField() {

        this.TEST_2007_07_15.get(TChronoField.AMPM_OF_DAY);
    }

    @Test
    public void test_getLong_TemporalField() {

        TLocalDate test = TLocalDate.of(2008, 6, 30);
        assertEquals(test.getLong(YEAR), 2008);
        assertEquals(test.getLong(MONTH_OF_YEAR), 6);
        assertEquals(test.getLong(DAY_OF_MONTH), 30);
        assertEquals(test.getLong(DAY_OF_WEEK), 1);
        assertEquals(test.getLong(DAY_OF_YEAR), 182);
        assertEquals(test.getLong(YEAR_OF_ERA), 2008);
        assertEquals(test.getLong(ERA), 1);
        assertEquals(test.getLong(PROLEPTIC_MONTH), 2008 * 12 + 6 - 1);
    }

    @Test(expected = NullPointerException.class)
    public void test_getLong_TemporalField_null() {

        this.TEST_2007_07_15.getLong((TTemporalField) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_getLong_TemporalField_invalidField() {

        this.TEST_2007_07_15.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_getLong_TemporalField_timeField() {

        this.TEST_2007_07_15.getLong(TChronoField.AMPM_OF_DAY);
    }

    @Test
    public void test_query() {

        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.localDate()), this.TEST_2007_07_15);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.localTime()), null);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.offset()), null);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.precision()), TChronoUnit.DAYS);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.zone()), null);
        assertEquals(this.TEST_2007_07_15.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        this.TEST_2007_07_15.query(null);
    }

    Object[][] provider_sampleDates() {

        return new Object[][] { { 2008, 7, 5 }, { 2007, 7, 5 }, { 2006, 7, 5 }, { 2005, 7, 5 }, { 2004, 1, 1 },
        { -1, 1, 2 }, };
    }

    @Test
    public void test_get() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            assertEquals(a.getYear(), y);
            assertEquals(a.getMonth(), TMonth.of(m));
            assertEquals(a.getDayOfMonth(), d);
        }
    }

    @Test
    public void test_getDOY() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            int total = 0;
            for (int i = 1; i < m; i++) {
                total += TMonth.of(i).length(isIsoLeap(y));
            }
            int doy = total + d;
            assertEquals(a.getDayOfYear(), doy);
        }
    }

    @Test
    public void test_getDayOfWeek() {

        TDayOfWeek dow = TDayOfWeek.MONDAY;
        for (TMonth month : TMonth.values()) {
            int length = month.length(false);
            for (int i = 1; i <= length; i++) {
                TLocalDate d = TLocalDate.of(2007, month, i);
                assertSame(d.getDayOfWeek(), dow);
                dow = dow.plus(1);
            }
        }
    }

    @Test
    public void test_isLeapYear() {

        assertEquals(TLocalDate.of(1999, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(2000, 1, 1).isLeapYear(), true);
        assertEquals(TLocalDate.of(2001, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(2002, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(2003, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(2004, 1, 1).isLeapYear(), true);
        assertEquals(TLocalDate.of(2005, 1, 1).isLeapYear(), false);

        assertEquals(TLocalDate.of(1500, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(1600, 1, 1).isLeapYear(), true);
        assertEquals(TLocalDate.of(1700, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(1800, 1, 1).isLeapYear(), false);
        assertEquals(TLocalDate.of(1900, 1, 1).isLeapYear(), false);
    }

    @Test
    public void test_lengthOfMonth_notLeapYear() {

        assertEquals(TLocalDate.of(2007, 1, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 2, 1).lengthOfMonth(), 28);
        assertEquals(TLocalDate.of(2007, 3, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 4, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2007, 5, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 6, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2007, 7, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 8, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 9, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2007, 10, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2007, 11, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2007, 12, 1).lengthOfMonth(), 31);
    }

    @Test
    public void test_lengthOfMonth_leapYear() {

        assertEquals(TLocalDate.of(2008, 1, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 2, 1).lengthOfMonth(), 29);
        assertEquals(TLocalDate.of(2008, 3, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 4, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2008, 5, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 6, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2008, 7, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 8, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 9, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2008, 10, 1).lengthOfMonth(), 31);
        assertEquals(TLocalDate.of(2008, 11, 1).lengthOfMonth(), 30);
        assertEquals(TLocalDate.of(2008, 12, 1).lengthOfMonth(), 31);
    }

    @Test
    public void test_lengthOfYear() {

        assertEquals(TLocalDate.of(2007, 1, 1).lengthOfYear(), 365);
        assertEquals(TLocalDate.of(2008, 1, 1).lengthOfYear(), 366);
    }

    @Test
    public void test_with_adjustment() {

        final TLocalDate sample = TLocalDate.of(2012, 3, 4);
        TTemporalAdjuster adjuster = new TTemporalAdjuster() {
            @Override
            public TTemporal adjustInto(TTemporal dateTime) {

                return sample;
            }
        };
        assertEquals(this.TEST_2007_07_15.with(adjuster), sample);
    }

    @Test(expected = NullPointerException.class)
    public void test_with_adjustment_null() {

        this.TEST_2007_07_15.with((TTemporalAdjuster) null);
    }

    @Test
    public void test_with_DateTimeField_long_normal() {

        TLocalDate t = this.TEST_2007_07_15.with(YEAR, 2008);
        assertEquals(t, TLocalDate.of(2008, 7, 15));
    }

    @Test(expected = NullPointerException.class)
    public void test_with_DateTimeField_long_null() {

        this.TEST_2007_07_15.with((TTemporalField) null, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_with_DateTimeField_long_invalidField() {

        this.TEST_2007_07_15.with(MockFieldNoValue.INSTANCE, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_with_DateTimeField_long_timeField() {

        this.TEST_2007_07_15.with(TChronoField.AMPM_OF_DAY, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_with_DateTimeField_long_invalidValue() {

        this.TEST_2007_07_15.with(TChronoField.DAY_OF_WEEK, -1);
    }

    @Test
    public void test_withYear_int_normal() {

        TLocalDate t = this.TEST_2007_07_15.withYear(2008);
        assertEquals(t, TLocalDate.of(2008, 7, 15));
    }

    @Test(expected = TDateTimeException.class)
    public void test_withYear_int_invalid() {

        this.TEST_2007_07_15.withYear(TYear.MIN_VALUE - 1);
    }

    @Test
    public void test_withYear_int_adjustDay() {

        TLocalDate t = TLocalDate.of(2008, 2, 29).withYear(2007);
        TLocalDate expected = TLocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_withMonth_int_normal() {

        TLocalDate t = this.TEST_2007_07_15.withMonth(1);
        assertEquals(t, TLocalDate.of(2007, 1, 15));
    }

    @Test(expected = TDateTimeException.class)
    public void test_withMonth_int_invalid() {

        this.TEST_2007_07_15.withMonth(13);
    }

    @Test
    public void test_withMonth_int_adjustDay() {

        TLocalDate t = TLocalDate.of(2007, 12, 31).withMonth(11);
        TLocalDate expected = TLocalDate.of(2007, 11, 30);
        assertEquals(t, expected);
    }

    @Test
    public void test_withDayOfMonth_normal() {

        TLocalDate t = this.TEST_2007_07_15.withDayOfMonth(1);
        assertEquals(t, TLocalDate.of(2007, 7, 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_illegal() {

        this.TEST_2007_07_15.withDayOfMonth(32);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfMonth_invalid() {

        TLocalDate.of(2007, 11, 30).withDayOfMonth(31);
    }

    @Test
    public void test_withDayOfYear_normal() {

        TLocalDate t = this.TEST_2007_07_15.withDayOfYear(33);
        assertEquals(t, TLocalDate.of(2007, 2, 2));
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfYear_illegal() {

        this.TEST_2007_07_15.withDayOfYear(367);
    }

    @Test(expected = TDateTimeException.class)
    public void test_withDayOfYear_invalid() {

        this.TEST_2007_07_15.withDayOfYear(366);
    }

    @Test
    public void test_plus_Period_positiveMonths() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TLocalDate t = this.TEST_2007_07_15.plus(period);
        assertEquals(t, TLocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_plus_Period_negativeDays() {

        MockSimplePeriod period = MockSimplePeriod.of(-25, TChronoUnit.DAYS);
        TLocalDate t = this.TEST_2007_07_15.plus(period);
        assertEquals(t, TLocalDate.of(2007, 6, 20));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_Period_timeNotAllowed() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.HOURS);
        this.TEST_2007_07_15.plus(period);
    }

    @Test(expected = NullPointerException.class)
    public void test_plus_Period_null() {

        this.TEST_2007_07_15.plus((MockSimplePeriod) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_Period_invalidTooLarge() {

        MockSimplePeriod period = MockSimplePeriod.of(1, TChronoUnit.YEARS);
        TLocalDate.of(TYear.MAX_VALUE, 1, 1).plus(period);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_Period_invalidTooSmall() {

        MockSimplePeriod period = MockSimplePeriod.of(-1, TChronoUnit.YEARS);
        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plus(period);
    }

    @Test
    public void test_plus_longPeriodUnit_positiveMonths() {

        TLocalDate t = this.TEST_2007_07_15.plus(7, TChronoUnit.MONTHS);
        assertEquals(t, TLocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_plus_longPeriodUnit_negativeDays() {

        TLocalDate t = this.TEST_2007_07_15.plus(-25, TChronoUnit.DAYS);
        assertEquals(t, TLocalDate.of(2007, 6, 20));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_longPeriodUnit_timeNotAllowed() {

        this.TEST_2007_07_15.plus(7, TChronoUnit.HOURS);
    }

    @Test(expected = NullPointerException.class)
    public void test_plus_longPeriodUnit_null() {

        this.TEST_2007_07_15.plus(1, (TTemporalUnit) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_longPeriodUnit_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 1, 1).plus(1, TChronoUnit.YEARS);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plus_longPeriodUnit_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plus(-1, TChronoUnit.YEARS);
    }

    @Test
    public void test_plusYears_long_normal() {

        TLocalDate t = this.TEST_2007_07_15.plusYears(1);
        assertEquals(t, TLocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_plusYears_long_negative() {

        TLocalDate t = this.TEST_2007_07_15.plusYears(-1);
        assertEquals(t, TLocalDate.of(2006, 7, 15));
    }

    @Test
    public void test_plusYears_long_adjustDay() {

        TLocalDate t = TLocalDate.of(2008, 2, 29).plusYears(1);
        TLocalDate expected = TLocalDate.of(2009, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusYears_long_big() {

        long years = 20L + TYear.MAX_VALUE;
        TLocalDate test = TLocalDate.of(-40, 6, 1).plusYears(years);
        assertEquals(test, TLocalDate.of((int) (-40L + years), 6, 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLarge() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 6, 1);
        test.plusYears(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMax() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.plusYears(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMin() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.plusYears(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooSmall_validInt() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plusYears(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusYears_long_invalidTooSmall_invalidInt() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plusYears(-10);
    }

    @Test
    public void test_plusMonths_long_normal() {

        TLocalDate t = this.TEST_2007_07_15.plusMonths(1);
        assertEquals(t, TLocalDate.of(2007, 8, 15));
    }

    @Test
    public void test_plusMonths_long_overYears() {

        TLocalDate t = this.TEST_2007_07_15.plusMonths(25);
        assertEquals(t, TLocalDate.of(2009, 8, 15));
    }

    @Test
    public void test_plusMonths_long_negative() {

        TLocalDate t = this.TEST_2007_07_15.plusMonths(-1);
        assertEquals(t, TLocalDate.of(2007, 6, 15));
    }

    @Test
    public void test_plusMonths_long_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.plusMonths(-7);
        assertEquals(t, TLocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_plusMonths_long_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.plusMonths(-31);
        assertEquals(t, TLocalDate.of(2004, 12, 15));
    }

    @Test
    public void test_plusMonths_long_adjustDayFromLeapYear() {

        TLocalDate t = TLocalDate.of(2008, 2, 29).plusMonths(12);
        TLocalDate expected = TLocalDate.of(2009, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusMonths_long_adjustDayFromMonthLength() {

        TLocalDate t = TLocalDate.of(2007, 3, 31).plusMonths(1);
        TLocalDate expected = TLocalDate.of(2007, 4, 30);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusMonths_long_big() {

        long months = 20L + Integer.MAX_VALUE;
        TLocalDate test = TLocalDate.of(-40, 6, 1).plusMonths(months);
        assertEquals(test, TLocalDate.of((int) (-40L + months / 12), 6 + (int) (months % 12), 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 1).plusMonths(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMax() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.plusMonths(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMin() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.plusMonths(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusMonths_long_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plusMonths(-1);
    }

    @Test
    public void test_plusWeeks_normal() {

        TLocalDate t = this.TEST_2007_07_15.plusWeeks(1);
        assertEquals(t, TLocalDate.of(2007, 7, 22));
    }

    @Test
    public void test_plusWeeks_overMonths() {

        TLocalDate t = this.TEST_2007_07_15.plusWeeks(9);
        assertEquals(t, TLocalDate.of(2007, 9, 16));
    }

    @Test
    public void test_plusWeeks_overYears() {

        TLocalDate t = TLocalDate.of(2006, 7, 16).plusWeeks(52);
        assertEquals(t, this.TEST_2007_07_15);
    }

    @Test
    public void test_plusWeeks_overLeapYears() {

        TLocalDate t = this.TEST_2007_07_15.plusYears(-1).plusWeeks(104);
        assertEquals(t, TLocalDate.of(2008, 7, 12));
    }

    @Test
    public void test_plusWeeks_negative() {

        TLocalDate t = this.TEST_2007_07_15.plusWeeks(-1);
        assertEquals(t, TLocalDate.of(2007, 7, 8));
    }

    @Test
    public void test_plusWeeks_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.plusWeeks(-28);
        assertEquals(t, TLocalDate.of(2006, 12, 31));
    }

    @Test
    public void test_plusWeeks_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.plusWeeks(-104);
        assertEquals(t, TLocalDate.of(2005, 7, 17));
    }

    @Test
    public void test_plusWeeks_maximum() {

        TLocalDate t = TLocalDate.of(TYear.MAX_VALUE, 12, 24).plusWeeks(1);
        TLocalDate expected = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusWeeks_minimum() {

        TLocalDate t = TLocalDate.of(TYear.MIN_VALUE, 1, 8).plusWeeks(-1);
        TLocalDate expected = TLocalDate.of(TYear.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusWeeks_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).plusWeeks(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusWeeks_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 7).plusWeeks(-1);
    }

    @Test(expected = ArithmeticException.class)
    public void test_plusWeeks_invalidMaxMinusMax() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).plusWeeks(Long.MAX_VALUE);
    }

    @Test(expected = ArithmeticException.class)
    public void test_plusWeeks_invalidMaxMinusMin() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).plusWeeks(Long.MIN_VALUE);
    }

    @Test
    public void test_plusDays_normal() {

        TLocalDate t = this.TEST_2007_07_15.plusDays(1);
        assertEquals(t, TLocalDate.of(2007, 7, 16));
    }

    @Test
    public void test_plusDays_overMonths() {

        TLocalDate t = this.TEST_2007_07_15.plusDays(62);
        assertEquals(t, TLocalDate.of(2007, 9, 15));
    }

    @Test
    public void test_plusDays_overYears() {

        TLocalDate t = TLocalDate.of(2006, 7, 14).plusDays(366);
        assertEquals(t, this.TEST_2007_07_15);
    }

    @Test
    public void test_plusDays_overLeapYears() {

        TLocalDate t = this.TEST_2007_07_15.plusYears(-1).plusDays(365 + 366);
        assertEquals(t, TLocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_plusDays_negative() {

        TLocalDate t = this.TEST_2007_07_15.plusDays(-1);
        assertEquals(t, TLocalDate.of(2007, 7, 14));
    }

    @Test
    public void test_plusDays_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.plusDays(-196);
        assertEquals(t, TLocalDate.of(2006, 12, 31));
    }

    @Test
    public void test_plusDays_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.plusDays(-730);
        assertEquals(t, TLocalDate.of(2005, 7, 15));
    }

    @Test
    public void test_plusDays_maximum() {

        TLocalDate t = TLocalDate.of(TYear.MAX_VALUE, 12, 30).plusDays(1);
        TLocalDate expected = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusDays_minimum() {

        TLocalDate t = TLocalDate.of(TYear.MIN_VALUE, 1, 2).plusDays(-1);
        TLocalDate expected = TLocalDate.of(TYear.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusDays_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 31).plusDays(1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_plusDays_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plusDays(-1);
    }

    @Test(expected = ArithmeticException.class)
    public void test_plusDays_overflowTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 31).plusDays(Long.MAX_VALUE);
    }

    @Test(expected = ArithmeticException.class)
    public void test_plusDays_overflowTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).plusDays(Long.MIN_VALUE);
    }

    @Test
    public void test_minus_Period_positiveMonths() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.MONTHS);
        TLocalDate t = this.TEST_2007_07_15.minus(period);
        assertEquals(t, TLocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_minus_Period_negativeDays() {

        MockSimplePeriod period = MockSimplePeriod.of(-25, TChronoUnit.DAYS);
        TLocalDate t = this.TEST_2007_07_15.minus(period);
        assertEquals(t, TLocalDate.of(2007, 8, 9));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_Period_timeNotAllowed() {

        MockSimplePeriod period = MockSimplePeriod.of(7, TChronoUnit.HOURS);
        this.TEST_2007_07_15.minus(period);
    }

    @Test(expected = NullPointerException.class)
    public void test_minus_Period_null() {

        this.TEST_2007_07_15.minus((MockSimplePeriod) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_Period_invalidTooLarge() {

        MockSimplePeriod period = MockSimplePeriod.of(-1, TChronoUnit.YEARS);
        TLocalDate.of(TYear.MAX_VALUE, 1, 1).minus(period);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_Period_invalidTooSmall() {

        MockSimplePeriod period = MockSimplePeriod.of(1, TChronoUnit.YEARS);
        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minus(period);
    }

    @Test
    public void test_minus_longPeriodUnit_positiveMonths() {

        TLocalDate t = this.TEST_2007_07_15.minus(7, TChronoUnit.MONTHS);
        assertEquals(t, TLocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_minus_longPeriodUnit_negativeDays() {

        TLocalDate t = this.TEST_2007_07_15.minus(-25, TChronoUnit.DAYS);
        assertEquals(t, TLocalDate.of(2007, 8, 9));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_longPeriodUnit_timeNotAllowed() {

        this.TEST_2007_07_15.minus(7, TChronoUnit.HOURS);
    }

    @Test(expected = NullPointerException.class)
    public void test_minus_longPeriodUnit_null() {

        this.TEST_2007_07_15.minus(1, (TTemporalUnit) null);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_longPeriodUnit_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 1, 1).minus(-1, TChronoUnit.YEARS);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minus_longPeriodUnit_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minus(1, TChronoUnit.YEARS);
    }

    @Test
    public void test_minusYears_long_normal() {

        TLocalDate t = this.TEST_2007_07_15.minusYears(1);
        assertEquals(t, TLocalDate.of(2006, 7, 15));
    }

    @Test
    public void test_minusYears_long_negative() {

        TLocalDate t = this.TEST_2007_07_15.minusYears(-1);
        assertEquals(t, TLocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_minusYears_long_adjustDay() {

        TLocalDate t = TLocalDate.of(2008, 2, 29).minusYears(1);
        TLocalDate expected = TLocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusYears_long_big() {

        long years = 20L + TYear.MAX_VALUE;
        TLocalDate test = TLocalDate.of(40, 6, 1).minusYears(years);
        assertEquals(test, TLocalDate.of((int) (40L - years), 6, 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLarge() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 6, 1);
        test.minusYears(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxAddMax() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.minusYears(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxAddMin() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.minusYears(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusYears_long_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minusYears(1);
    }

    @Test
    public void test_minusMonths_long_normal() {

        TLocalDate t = this.TEST_2007_07_15.minusMonths(1);
        assertEquals(t, TLocalDate.of(2007, 6, 15));
    }

    @Test
    public void test_minusMonths_long_overYears() {

        TLocalDate t = this.TEST_2007_07_15.minusMonths(25);
        assertEquals(t, TLocalDate.of(2005, 6, 15));
    }

    @Test
    public void test_minusMonths_long_negative() {

        TLocalDate t = this.TEST_2007_07_15.minusMonths(-1);
        assertEquals(t, TLocalDate.of(2007, 8, 15));
    }

    @Test
    public void test_minusMonths_long_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.minusMonths(-7);
        assertEquals(t, TLocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_minusMonths_long_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.minusMonths(-31);
        assertEquals(t, TLocalDate.of(2010, 2, 15));
    }

    @Test
    public void test_minusMonths_long_adjustDayFromLeapYear() {

        TLocalDate t = TLocalDate.of(2008, 2, 29).minusMonths(12);
        TLocalDate expected = TLocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusMonths_long_adjustDayFromMonthLength() {

        TLocalDate t = TLocalDate.of(2007, 3, 31).minusMonths(1);
        TLocalDate expected = TLocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusMonths_long_big() {

        long months = 20L + Integer.MAX_VALUE;
        TLocalDate test = TLocalDate.of(40, 6, 1).minusMonths(months);
        assertEquals(test, TLocalDate.of((int) (40L - months / 12), 6 - (int) (months % 12), 1));
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 1).minusMonths(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxAddMax() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.minusMonths(Long.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxAddMin() {

        TLocalDate test = TLocalDate.of(TYear.MAX_VALUE, 12, 1);
        test.minusMonths(Long.MIN_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusMonths_long_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minusMonths(1);
    }

    @Test
    public void test_minusWeeks_normal() {

        TLocalDate t = this.TEST_2007_07_15.minusWeeks(1);
        assertEquals(t, TLocalDate.of(2007, 7, 8));
    }

    @Test
    public void test_minusWeeks_overMonths() {

        TLocalDate t = this.TEST_2007_07_15.minusWeeks(9);
        assertEquals(t, TLocalDate.of(2007, 5, 13));
    }

    @Test
    public void test_minusWeeks_overYears() {

        TLocalDate t = TLocalDate.of(2008, 7, 13).minusWeeks(52);
        assertEquals(t, this.TEST_2007_07_15);
    }

    @Test
    public void test_minusWeeks_overLeapYears() {

        TLocalDate t = this.TEST_2007_07_15.minusYears(-1).minusWeeks(104);
        assertEquals(t, TLocalDate.of(2006, 7, 18));
    }

    @Test
    public void test_minusWeeks_negative() {

        TLocalDate t = this.TEST_2007_07_15.minusWeeks(-1);
        assertEquals(t, TLocalDate.of(2007, 7, 22));
    }

    @Test
    public void test_minusWeeks_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.minusWeeks(-28);
        assertEquals(t, TLocalDate.of(2008, 1, 27));
    }

    @Test
    public void test_minusWeeks_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.minusWeeks(-104);
        assertEquals(t, TLocalDate.of(2009, 7, 12));
    }

    @Test
    public void test_minusWeeks_maximum() {

        TLocalDate t = TLocalDate.of(TYear.MAX_VALUE, 12, 24).minusWeeks(-1);
        TLocalDate expected = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusWeeks_minimum() {

        TLocalDate t = TLocalDate.of(TYear.MIN_VALUE, 1, 8).minusWeeks(1);
        TLocalDate expected = TLocalDate.of(TYear.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusWeeks_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).minusWeeks(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusWeeks_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 7).minusWeeks(1);
    }

    @Test(expected = ArithmeticException.class)
    public void test_minusWeeks_invalidMaxMinusMax() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).minusWeeks(Long.MAX_VALUE);
    }

    @Test(expected = ArithmeticException.class)
    public void test_minusWeeks_invalidMaxMinusMin() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 25).minusWeeks(Long.MIN_VALUE);
    }

    @Test
    public void test_minusDays_normal() {

        TLocalDate t = this.TEST_2007_07_15.minusDays(1);
        assertEquals(t, TLocalDate.of(2007, 7, 14));
    }

    @Test
    public void test_minusDays_overMonths() {

        TLocalDate t = this.TEST_2007_07_15.minusDays(62);
        assertEquals(t, TLocalDate.of(2007, 5, 14));
    }

    @Test
    public void test_minusDays_overYears() {

        TLocalDate t = TLocalDate.of(2008, 7, 16).minusDays(367);
        assertEquals(t, this.TEST_2007_07_15);
    }

    @Test
    public void test_minusDays_overLeapYears() {

        TLocalDate t = this.TEST_2007_07_15.plusYears(2).minusDays(365 + 366);
        assertEquals(t, this.TEST_2007_07_15);
    }

    @Test
    public void test_minusDays_negative() {

        TLocalDate t = this.TEST_2007_07_15.minusDays(-1);
        assertEquals(t, TLocalDate.of(2007, 7, 16));
    }

    @Test
    public void test_minusDays_negativeAcrossYear() {

        TLocalDate t = this.TEST_2007_07_15.minusDays(-169);
        assertEquals(t, TLocalDate.of(2007, 12, 31));
    }

    @Test
    public void test_minusDays_negativeOverYears() {

        TLocalDate t = this.TEST_2007_07_15.minusDays(-731);
        assertEquals(t, TLocalDate.of(2009, 7, 15));
    }

    @Test
    public void test_minusDays_maximum() {

        TLocalDate t = TLocalDate.of(TYear.MAX_VALUE, 12, 30).minusDays(-1);
        TLocalDate expected = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusDays_minimum() {

        TLocalDate t = TLocalDate.of(TYear.MIN_VALUE, 1, 2).minusDays(1);
        TLocalDate expected = TLocalDate.of(TYear.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusDays_invalidTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 31).minusDays(-1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_minusDays_invalidTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minusDays(1);
    }

    @Test(expected = ArithmeticException.class)
    public void test_minusDays_overflowTooLarge() {

        TLocalDate.of(TYear.MAX_VALUE, 12, 31).minusDays(Long.MIN_VALUE);
    }

    @Test(expected = ArithmeticException.class)
    public void test_minusDays_overflowTooSmall() {

        TLocalDate.of(TYear.MIN_VALUE, 1, 1).minusDays(Long.MAX_VALUE);
    }

    Object[][] provider_until() {

        return new Object[][] { { "2012-06-30", "2012-06-30", DAYS, 0 }, { "2012-06-30", "2012-06-30", WEEKS, 0 },
        { "2012-06-30", "2012-06-30", MONTHS, 0 }, { "2012-06-30", "2012-06-30", YEARS, 0 },
        { "2012-06-30", "2012-06-30", DECADES, 0 }, { "2012-06-30", "2012-06-30", CENTURIES, 0 },
        { "2012-06-30", "2012-06-30", MILLENNIA, 0 },

        { "2012-06-30", "2012-07-01", DAYS, 1 }, { "2012-06-30", "2012-07-01", WEEKS, 0 },
        { "2012-06-30", "2012-07-01", MONTHS, 0 }, { "2012-06-30", "2012-07-01", YEARS, 0 },
        { "2012-06-30", "2012-07-01", DECADES, 0 }, { "2012-06-30", "2012-07-01", CENTURIES, 0 },
        { "2012-06-30", "2012-07-01", MILLENNIA, 0 },

        { "2012-06-30", "2012-07-07", DAYS, 7 }, { "2012-06-30", "2012-07-07", WEEKS, 1 },
        { "2012-06-30", "2012-07-07", MONTHS, 0 }, { "2012-06-30", "2012-07-07", YEARS, 0 },
        { "2012-06-30", "2012-07-07", DECADES, 0 }, { "2012-06-30", "2012-07-07", CENTURIES, 0 },
        { "2012-06-30", "2012-07-07", MILLENNIA, 0 },

        { "2012-06-30", "2012-07-29", MONTHS, 0 }, { "2012-06-30", "2012-07-30", MONTHS, 1 },
        { "2012-06-30", "2012-07-31", MONTHS, 1 }, };
    }

    @Test
    public void test_until() {

        for (Object[] data : provider_until()) {
            String startStr = (String) data[0];
            String endStr = (String) data[1];
            TTemporalUnit unit = (TTemporalUnit) data[2];
            long expected = ((Number) data[3]).longValue();

            TLocalDate start = TLocalDate.parse(startStr);
            TLocalDate end = TLocalDate.parse(endStr);
            assertEquals(start.until(end, unit), expected);
            assertEquals(end.until(start, unit), -expected);
        }
    }

    @Test
    public void test_atTime_LocalTime() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(TLocalTime.of(11, 30)), TLocalDateTime.of(2008, 6, 30, 11, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_atTime_LocalTime_null() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime((TLocalTime) null);
    }

    @Test
    public void test_atTime_int_int() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30), TLocalDateTime.of(2008, 6, 30, 11, 30));
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_hourTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(-1, 30);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_hourTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(24, 30);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_minuteTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, -1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_minuteTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 60);
    }

    @Test
    public void test_atTime_int_int_int() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30, 40), TLocalDateTime.of(2008, 6, 30, 11, 30, 40));
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_hourTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(-1, 30, 40);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_hourTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(24, 30, 40);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_minuteTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, -1, 40);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_minuteTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 60, 40);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_secondTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, -1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_secondTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 60);
    }

    @Test
    public void test_atTime_int_int_int_int() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30, 40, 50), TLocalDateTime.of(2008, 6, 30, 11, 30, 40, 50));
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_hourTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(-1, 30, 40, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_hourTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(24, 30, 40, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_minuteTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, -1, 40, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_minuteTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 60, 40, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_secondTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, -1, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_secondTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 60, 50);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_nanoTooSmall() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 40, -1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_atTime_int_int_int_int_nanoTooBig() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 40, 1000000000);
    }

    @Test
    public void test_atStartOfDay() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        assertEquals(t.atStartOfDay(ZONE_PARIS), TZonedDateTime.of(TLocalDateTime.of(2008, 6, 30, 0, 0), ZONE_PARIS));
    }

    @Test
    public void test_atStartOfDay_dstGap() {

        TLocalDate t = TLocalDate.of(2007, 4, 1);
        assertEquals(t.atStartOfDay(ZONE_GAZA), TZonedDateTime.of(TLocalDateTime.of(2007, 4, 1, 1, 0), ZONE_GAZA));
    }

    @Test(expected = NullPointerException.class)
    public void test_atStartOfDay_nullTimeZone() {

        TLocalDate t = TLocalDate.of(2008, 6, 30);
        t.atStartOfDay((TZoneId) null);
    }

    @Test
    public void test_toEpochDay() {

        long date_0000_01_01 = -678941 - 40587;

        TLocalDate test = TLocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i < 700000; i++) {
            assertEquals(test.toEpochDay(), i);
            test = next(test);
        }
        test = TLocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i > -2000000; i--) {
            assertEquals(test.toEpochDay(), i);
            test = previous(test);
        }

        assertEquals(TLocalDate.of(1858, 11, 17).toEpochDay(), -40587);
        assertEquals(TLocalDate.of(1, 1, 1).toEpochDay(), -678575 - 40587);
        assertEquals(TLocalDate.of(1995, 9, 27).toEpochDay(), 49987 - 40587);
        assertEquals(TLocalDate.of(1970, 1, 1).toEpochDay(), 0);
        assertEquals(TLocalDate.of(-1, 12, 31).toEpochDay(), -678942 - 40587);
    }

    @Test
    public void test_comparisons() {

        doTest_comparisons_LocalDate(TLocalDate.of(TYear.MIN_VALUE, 1, 1), TLocalDate.of(TYear.MIN_VALUE, 12, 31),
                TLocalDate.of(-1, 1, 1), TLocalDate.of(-1, 12, 31), TLocalDate.of(0, 1, 1), TLocalDate.of(0, 12, 31),
                TLocalDate.of(1, 1, 1), TLocalDate.of(1, 12, 31), TLocalDate.of(2006, 1, 1),
                TLocalDate.of(2006, 12, 31), TLocalDate.of(2007, 1, 1), TLocalDate.of(2007, 12, 31),
                TLocalDate.of(2008, 1, 1), TLocalDate.of(2008, 2, 29), TLocalDate.of(2008, 12, 31),
                TLocalDate.of(TYear.MAX_VALUE, 1, 1), TLocalDate.of(TYear.MAX_VALUE, 12, 31));
    }

    void doTest_comparisons_LocalDate(TLocalDate... localDates) {

        for (int i = 0; i < localDates.length; i++) {
            TLocalDate a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                TLocalDate b = localDates[j];
                if (i < j) {
                    assertTrue(a + " <=> " + b, a.compareTo(b) < 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), true);
                    assertEquals(a + " <=> " + b, a.isAfter(b), false);
                    assertEquals(a + " <=> " + b, a.equals(b), false);
                } else if (i > j) {
                    assertTrue(a + " <=> " + b, a.compareTo(b) > 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), false);
                    assertEquals(a + " <=> " + b, a.isAfter(b), true);
                    assertEquals(a + " <=> " + b, a.equals(b), false);
                } else {
                    assertEquals(a + " <=> " + b, a.compareTo(b), 0);
                    assertEquals(a + " <=> " + b, a.isBefore(b), false);
                    assertEquals(a + " <=> " + b, a.isAfter(b), false);
                    assertEquals(a + " <=> " + b, a.equals(b), true);
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_compareTo_ObjectNull() {

        this.TEST_2007_07_15.compareTo(null);
    }

    @Test
    public void test_isBefore() {

        assertTrue(this.TEST_2007_07_15.isBefore(TLocalDate.of(2007, 07, 16)));
        assertFalse(this.TEST_2007_07_15.isBefore(TLocalDate.of(2007, 07, 14)));
        assertFalse(this.TEST_2007_07_15.isBefore(this.TEST_2007_07_15));
    }

    @Test(expected = NullPointerException.class)
    public void test_isBefore_ObjectNull() {

        this.TEST_2007_07_15.isBefore(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_isAfter_ObjectNull() {

        this.TEST_2007_07_15.isAfter(null);
    }

    @Test
    public void test_isAfter() {

        assertTrue(this.TEST_2007_07_15.isAfter(TLocalDate.of(2007, 07, 14)));
        assertFalse(this.TEST_2007_07_15.isAfter(TLocalDate.of(2007, 07, 16)));
        assertFalse(this.TEST_2007_07_15.isAfter(this.TEST_2007_07_15));
    }

    @Test(expected = ClassCastException.class)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void compareToNonLocalDate() {

        Comparable c = this.TEST_2007_07_15;
        c.compareTo(new Object());
    }

    @Test
    public void test_equals_true() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            TLocalDate b = TLocalDate.of(y, m, d);
            assertEquals(a.equals(b), true);
        }
    }

    @Test
    public void test_equals_false_year_differs() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            TLocalDate b = TLocalDate.of(y + 1, m, d);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_month_differs() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            TLocalDate b = TLocalDate.of(y, m + 1, d);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_false_day_differs() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            TLocalDate b = TLocalDate.of(y, m, d + 1);
            assertEquals(a.equals(b), false);
        }
    }

    @Test
    public void test_equals_itself_true() {

        assertEquals(this.TEST_2007_07_15.equals(this.TEST_2007_07_15), true);
    }

    @Test
    public void test_equals_string_false() {

        assertEquals(this.TEST_2007_07_15.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {

        assertEquals(this.TEST_2007_07_15.equals(null), false);
    }

    @Test
    public void test_hashCode() {

        for (Object[] data : provider_sampleDates()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];

            TLocalDate a = TLocalDate.of(y, m, d);
            assertEquals(a.hashCode(), a.hashCode());
            TLocalDate b = TLocalDate.of(y, m, d);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    Object[][] provider_sampleToString() {

        return new Object[][] { { 2008, 7, 5, "2008-07-05" }, { 2007, 12, 31, "2007-12-31" },
        { 999, 12, 31, "0999-12-31" }, { -1, 1, 2, "-0001-01-02" }, { 9999, 12, 31, "9999-12-31" },
        { -9999, 12, 31, "-9999-12-31" }, { 10000, 1, 1, "+10000-01-01" }, { -10000, 1, 1, "-10000-01-01" },
        { 12345678, 1, 1, "+12345678-01-01" }, { -12345678, 1, 1, "-12345678-01-01" }, };
    }

    @Test
    public void factory_parse_validText() {

        for (Object[] data : provider_sampleToString()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];
            String parsable = (String) data[3];

            TLocalDate t = TLocalDate.parse(parsable);
            assertNotNull(parsable, t);
            assertEquals(parsable, t.getYear(), y);
            assertEquals(parsable, t.getMonth().getValue(), m);
            assertEquals(parsable, t.getDayOfMonth(), d);
        }
    }

    @Test
    public void test_toString() {

        for (Object[] data : provider_sampleToString()) {
            int y = (int) data[0];
            int m = (int) data[1];
            int d = (int) data[2];
            String expected = (String) data[3];

            TLocalDate t = TLocalDate.of(y, m, d);
            String str = t.toString();
            assertEquals(str, expected);
        }
    }

    @Test
    public void test_format_formatter() {

        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y M d");
        String t = TLocalDate.of(2010, 12, 3).format(f);
        assertEquals(t, "2010 12 3");
    }

    @Test(expected = NullPointerException.class)
    public void test_format_formatter_null() {

        TLocalDate.of(2010, 12, 3).format(null);
    }

}
