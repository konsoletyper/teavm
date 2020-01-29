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
package org.threeten.bp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.ERA;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.PROLEPTIC_MONTH;
import static org.threeten.bp.temporal.ChronoField.YEAR;
import static org.threeten.bp.temporal.ChronoField.YEAR_OF_ERA;
import static org.threeten.bp.temporal.ChronoUnit.CENTURIES;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.DECADES;
import static org.threeten.bp.temporal.ChronoUnit.MILLENNIA;
import static org.threeten.bp.temporal.ChronoUnit.MONTHS;
import static org.threeten.bp.temporal.ChronoUnit.WEEKS;
import static org.threeten.bp.temporal.ChronoUnit.YEARS;

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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.JulianFields;
import org.threeten.bp.temporal.MockFieldNoValue;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalUnit;

/**
 * Test LocalDate.
 */
@Test
public class TestLocalDate extends AbstractDateTimeTest {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId ZONE_GAZA = ZoneId.of("Asia/Gaza");

    private LocalDate TEST_2007_07_15;
    private long MAX_VALID_EPOCHDAYS;
    private long MIN_VALID_EPOCHDAYS;
    private LocalDate MAX_DATE;
    private LocalDate MIN_DATE;
    private Instant MAX_INSTANT;
    private Instant MIN_INSTANT;

    @BeforeMethod
    public void setUp() {
        TEST_2007_07_15 = LocalDate.of(2007, 7, 15);

        LocalDate max = LocalDate.MAX;
        LocalDate min = LocalDate.MIN;
        MAX_VALID_EPOCHDAYS = max.toEpochDay();
        MIN_VALID_EPOCHDAYS = min.toEpochDay();
        MAX_DATE = max;
        MIN_DATE = min;
        MAX_INSTANT = max.atStartOfDay(ZoneOffset.UTC).toInstant();
        MIN_INSTANT = min.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {TEST_2007_07_15, LocalDate.MAX, LocalDate.MIN, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
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

    @Test
    public void test_serialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(TEST_2007_07_15);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        assertEquals(ois.readObject(), TEST_2007_07_15);
    }

    @Test
    public void test_immutable() {
        Class<LocalDate> cls = LocalDate.class;
        assertTrue(Modifier.isPublic(cls.getModifiers()));
        assertTrue(Modifier.isFinal(cls.getModifiers()));
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().contains("$") == false) {
                if (Modifier.isStatic(field.getModifiers())) {
                    assertTrue(Modifier.isFinal(field.getModifiers()), "Field:" + field.getName());
                } else {
                    assertTrue(Modifier.isPrivate(field.getModifiers()), "Field:" + field.getName());
                    assertTrue(Modifier.isFinal(field.getModifiers()), "Field:" + field.getName());
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    private void check(LocalDate test_2008_02_29, int y, int m, int d) {
        assertEquals(test_2008_02_29.getYear(), y);
        assertEquals(test_2008_02_29.getMonth().getValue(), m);
        assertEquals(test_2008_02_29.getDayOfMonth(), d);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        LocalDate expected = LocalDate.now(Clock.systemDefaultZone());
        LocalDate test = LocalDate.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = LocalDate.now(Clock.systemDefaultZone());
            test = LocalDate.now();
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_ZoneId_nullZoneId() {
        LocalDate.now((ZoneId) null);
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        LocalDate expected = LocalDate.now(Clock.system(zone));
        LocalDate test = LocalDate.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = LocalDate.now(Clock.system(zone));
            test = LocalDate.now(zone);
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        LocalDate.now((Clock) null);
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            LocalDate test = LocalDate.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
        }
    }

    @Test
    public void now_Clock_allSecsInDay_offset() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            Clock clock = Clock.fixed(instant.minusSeconds(OFFSET_PONE.getTotalSeconds()), OFFSET_PONE);
            LocalDate test = LocalDate.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60) ? 1 : 2);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {
        for (int i =-1; i >= -(2 * 24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            LocalDate test = LocalDate.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), Month.DECEMBER);
            assertEquals(test.getDayOfMonth(), (i >= -24 * 60 * 60 ? 31 : 30));
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_maxYear() {
        Clock clock = Clock.fixed(MAX_INSTANT, ZoneOffset.UTC);
        LocalDate test = LocalDate.now(clock);
        assertEquals(test, MAX_DATE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void now_Clock_tooBig() {
        Clock clock = Clock.fixed(MAX_INSTANT.plusSeconds(24 * 60 * 60), ZoneOffset.UTC);
        LocalDate.now(clock);
    }

    @Test
    public void now_Clock_minYear() {
        Clock clock = Clock.fixed(MIN_INSTANT, ZoneOffset.UTC);
        LocalDate test = LocalDate.now(clock);
        assertEquals(test, MIN_DATE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void now_Clock_tooLow() {
        Clock clock = Clock.fixed(MIN_INSTANT.minusNanos(1), ZoneOffset.UTC);
        LocalDate.now(clock);
    }

    //-----------------------------------------------------------------------
    // of() factories
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_intsMonth() {
        assertEquals(TEST_2007_07_15, LocalDate.of(2007, Month.JULY, 15));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_intsMonth_29febNonLeap() {
        LocalDate.of(2007, Month.FEBRUARY, 29);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_intsMonth_31apr() {
        LocalDate.of(2007, Month.APRIL, 31);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_intsMonth_dayTooLow() {
        LocalDate.of(2007, Month.JANUARY, 0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_intsMonth_dayTooHigh() {
        LocalDate.of(2007, Month.JANUARY, 32);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_intsMonth_nullMonth() {
        LocalDate.of(2007, null, 30);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_intsMonth_yearTooLow() {
        LocalDate.of(Integer.MIN_VALUE, Month.JANUARY, 1);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_of_ints() {
        check(TEST_2007_07_15, 2007, 7, 15);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_29febNonLeap() {
        LocalDate.of(2007, 2, 29);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_31apr() {
        LocalDate.of(2007, 4, 31);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_dayTooLow() {
        LocalDate.of(2007, 1, 0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_dayTooHigh() {
        LocalDate.of(2007, 1, 32);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_monthTooLow() {
        LocalDate.of(2007, 0, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_monthTooHigh() {
        LocalDate.of(2007, 13, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_of_ints_yearTooLow() {
        LocalDate.of(Integer.MIN_VALUE, 1, 1);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofYearDay_ints_nonLeap() {
        LocalDate date = LocalDate.of(2007, 1, 1);
        for (int i = 1; i <= 365; i++) {
            assertEquals(LocalDate.ofYearDay(2007, i), date);
            date = next(date);
        }
    }

    @Test
    public void factory_ofYearDay_ints_leap() {
        LocalDate date = LocalDate.of(2008, 1, 1);
        for (int i = 1; i <= 366; i++) {
            assertEquals(LocalDate.ofYearDay(2008, i), date);
            date = next(date);
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofYearDay_ints_366nonLeap() {
        LocalDate.ofYearDay(2007, 366);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofYearDay_ints_dayTooLow() {
        LocalDate.ofYearDay(2007, 0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofYearDay_ints_dayTooHigh() {
        LocalDate.ofYearDay(2007, 367);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofYearDay_ints_yearTooLow() {
        LocalDate.ofYearDay(Integer.MIN_VALUE, 1);
    }

    //-----------------------------------------------------------------------
    // Since plusDays/minusDays actually depends on MJDays, it cannot be used for testing
    private LocalDate next(LocalDate date) {
        int newDayOfMonth = date.getDayOfMonth() + 1;
        if (newDayOfMonth <= date.getMonth().length(isIsoLeap(date.getYear()))) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.withDayOfMonth(1);
        if (date.getMonth() == Month.DECEMBER) {
            date = date.withYear(date.getYear() + 1);
        }
        return date.with(date.getMonth().plus(1));
    }

    private LocalDate previous(LocalDate date) {
        int newDayOfMonth = date.getDayOfMonth() - 1;
        if (newDayOfMonth > 0) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.with(date.getMonth().minus(1));
        if (date.getMonth() == Month.DECEMBER) {
            date = date.withYear(date.getYear() - 1);
        }
        return date.withDayOfMonth(date.getMonth().length(isIsoLeap(date.getYear())));
    }

    //-----------------------------------------------------------------------
    // ofEpochDay()
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofEpochDay() {
        long date_0000_01_01 = -678941 - 40587;
        assertEquals(LocalDate.ofEpochDay(0), LocalDate.of(1970, 1, 1));
        assertEquals(LocalDate.ofEpochDay(date_0000_01_01), LocalDate.of(0, 1, 1));
        assertEquals(LocalDate.ofEpochDay(date_0000_01_01 - 1), LocalDate.of(-1, 12, 31));
        assertEquals(LocalDate.ofEpochDay(MAX_VALID_EPOCHDAYS), LocalDate.of(Year.MAX_VALUE, 12, 31));
        assertEquals(LocalDate.ofEpochDay(MIN_VALID_EPOCHDAYS), LocalDate.of(Year.MIN_VALUE, 1, 1));

        LocalDate test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i < 700000; i++) {
            assertEquals(LocalDate.ofEpochDay(i), test);
            test = next(test);
        }
        test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i > -2000000; i--) {
            assertEquals(LocalDate.ofEpochDay(i), test);
            test = previous(test);
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofEpochDay_aboveMax() {
        LocalDate.ofEpochDay(MAX_VALID_EPOCHDAYS + 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofEpochDay_belowMin() {
        LocalDate.ofEpochDay(MIN_VALID_EPOCHDAYS - 1);
    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(LocalDate.from(LocalDate.of(2007, 7, 15)), LocalDate.of(2007, 7, 15));
        assertEquals(LocalDate.from(LocalDateTime.of(2007, 7, 15, 12, 30)), LocalDate.of(2007, 7, 15));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        LocalDate.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        LocalDate.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleToString")
    public void factory_parse_validText(int y, int m, int d, String parsable) {
        LocalDate t = LocalDate.parse(parsable);
        assertNotNull(t, parsable);
        assertEquals(t.getYear(), y, parsable);
        assertEquals(t.getMonth().getValue(), m, parsable);
        assertEquals(t.getDayOfMonth(), d, parsable);
    }

    @DataProvider(name="sampleBadParse")
    Object[][] provider_sampleBadParse() {
        return new Object[][]{
                {"2008/07/05"},
                {"10000-01-01"},
                {"2008-1-1"},
                {"2008--01"},
                {"ABCD-02-01"},
                {"2008-AB-01"},
                {"2008-02-AB"},
                {"-0000-02-01"},
                {"2008-02-01Z"},
                {"2008-02-01+01:00"},
                {"2008-02-01+01:00[Europe/Paris]"},
        };
    }

    @Test(dataProvider="sampleBadParse", expectedExceptions={DateTimeParseException.class})
    public void factory_parse_invalidText(String unparsable) {
        LocalDate.parse(unparsable);
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_illegalValue() {
        LocalDate.parse("2008-06-32");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_invalidValue() {
        LocalDate.parse("2008-06-31");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        LocalDate.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d");
        LocalDate test = LocalDate.parse("2010 12 3", f);
        assertEquals(test, LocalDate.of(2010, 12, 3));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("u M d");
        LocalDate.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        LocalDate.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        LocalDate test = LocalDate.of(2008, 6, 30);
        assertEquals(test.get(YEAR), 2008);
        assertEquals(test.get(MONTH_OF_YEAR), 6);
        assertEquals(test.get(DAY_OF_MONTH), 30);
        assertEquals(test.get(DAY_OF_WEEK), 1);
        assertEquals(test.get(DAY_OF_YEAR), 182);
        assertEquals(test.get(YEAR_OF_ERA), 2008);
        assertEquals(test.get(ERA), 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_get_TemporalField_tooBig() {
        TEST_2007_07_15.get(EPOCH_DAY);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_get_TemporalField_null() {
        TEST_2007_07_15.get((TemporalField) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_get_TemporalField_invalidField() {
        TEST_2007_07_15.get(MockFieldNoValue.INSTANCE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_get_TemporalField_timeField() {
        TEST_2007_07_15.get(ChronoField.AMPM_OF_DAY);
    }

    //-----------------------------------------------------------------------
    // getLong(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong_TemporalField() {
        LocalDate test = LocalDate.of(2008, 6, 30);
        assertEquals(test.getLong(YEAR), 2008);
        assertEquals(test.getLong(MONTH_OF_YEAR), 6);
        assertEquals(test.getLong(DAY_OF_MONTH), 30);
        assertEquals(test.getLong(DAY_OF_WEEK), 1);
        assertEquals(test.getLong(DAY_OF_YEAR), 182);
        assertEquals(test.getLong(YEAR_OF_ERA), 2008);
        assertEquals(test.getLong(ERA), 1);
        assertEquals(test.getLong(PROLEPTIC_MONTH), 2008 * 12 + 6 - 1);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_getLong_TemporalField_null() {
        TEST_2007_07_15.getLong((TemporalField) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_getLong_TemporalField_invalidField() {
        TEST_2007_07_15.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_getLong_TemporalField_timeField() {
        TEST_2007_07_15.getLong(ChronoField.AMPM_OF_DAY);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TEST_2007_07_15.query(TemporalQueries.chronology()), IsoChronology.INSTANCE);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.localDate()), TEST_2007_07_15);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.localTime()), null);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.offset()), null);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.precision()), ChronoUnit.DAYS);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.zone()), null);
        assertEquals(TEST_2007_07_15.query(TemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_2007_07_15.query(null);
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleDates")
    Object[][] provider_sampleDates() {
        return new Object[][] {
            {2008, 7, 5},
            {2007, 7, 5},
            {2006, 7, 5},
            {2005, 7, 5},
            {2004, 1, 1},
            {-1, 1, 2},
        };
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleDates")
    public void test_get(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        assertEquals(a.getYear(), y);
        assertEquals(a.getMonth(), Month.of(m));
        assertEquals(a.getDayOfMonth(), d);
    }

    @Test(dataProvider="sampleDates")
    public void test_getDOY(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        int total = 0;
        for (int i = 1; i < m; i++) {
            total += Month.of(i).length(isIsoLeap(y));
        }
        int doy = total + d;
        assertEquals(a.getDayOfYear(), doy);
    }

    @Test
    public void test_getDayOfWeek() {
        DayOfWeek dow = DayOfWeek.MONDAY;
        for (Month month : Month.values()) {
            int length = month.length(false);
            for (int i = 1; i <= length; i++) {
                LocalDate d = LocalDate.of(2007, month, i);
                assertSame(d.getDayOfWeek(), dow);
                dow = dow.plus(1);
            }
        }
    }

    //-----------------------------------------------------------------------
    // isLeapYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_isLeapYear() {
        assertEquals(LocalDate.of(1999, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(2000, 1, 1).isLeapYear(), true);
        assertEquals(LocalDate.of(2001, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(2002, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(2003, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(2004, 1, 1).isLeapYear(), true);
        assertEquals(LocalDate.of(2005, 1, 1).isLeapYear(), false);

        assertEquals(LocalDate.of(1500, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(1600, 1, 1).isLeapYear(), true);
        assertEquals(LocalDate.of(1700, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(1800, 1, 1).isLeapYear(), false);
        assertEquals(LocalDate.of(1900, 1, 1).isLeapYear(), false);
    }

    //-----------------------------------------------------------------------
    // lengthOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_lengthOfMonth_notLeapYear() {
        assertEquals(LocalDate.of(2007, 1, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 2, 1).lengthOfMonth(), 28);
        assertEquals(LocalDate.of(2007, 3, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 4, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2007, 5, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 6, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2007, 7, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 8, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 9, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2007, 10, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2007, 11, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2007, 12, 1).lengthOfMonth(), 31);
    }

    @Test
    public void test_lengthOfMonth_leapYear() {
        assertEquals(LocalDate.of(2008, 1, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 2, 1).lengthOfMonth(), 29);
        assertEquals(LocalDate.of(2008, 3, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 4, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2008, 5, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 6, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2008, 7, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 8, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 9, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2008, 10, 1).lengthOfMonth(), 31);
        assertEquals(LocalDate.of(2008, 11, 1).lengthOfMonth(), 30);
        assertEquals(LocalDate.of(2008, 12, 1).lengthOfMonth(), 31);
    }

    //-----------------------------------------------------------------------
    // lengthOfYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_lengthOfYear() {
        assertEquals(LocalDate.of(2007, 1, 1).lengthOfYear(), 365);
        assertEquals(LocalDate.of(2008, 1, 1).lengthOfYear(), 366);
    }

    //-----------------------------------------------------------------------
    // with()
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final LocalDate sample = LocalDate.of(2012, 3, 4);
        TemporalAdjuster adjuster = new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {
                return sample;
            }
        };
        assertEquals(TEST_2007_07_15.with(adjuster), sample);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjustment_null() {
        TEST_2007_07_15.with((TemporalAdjuster) null);
    }

    //-----------------------------------------------------------------------
    // with(DateTimeField,long)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_DateTimeField_long_normal() {
        LocalDate t = TEST_2007_07_15.with(YEAR, 2008);
        assertEquals(t, LocalDate.of(2008, 7, 15));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_DateTimeField_long_null() {
        TEST_2007_07_15.with((TemporalField) null, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_with_DateTimeField_long_invalidField() {
        TEST_2007_07_15.with(MockFieldNoValue.INSTANCE, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_with_DateTimeField_long_timeField() {
        TEST_2007_07_15.with(ChronoField.AMPM_OF_DAY, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_with_DateTimeField_long_invalidValue() {
        TEST_2007_07_15.with(ChronoField.DAY_OF_WEEK, -1);
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_int_normal() {
        LocalDate t = TEST_2007_07_15.withYear(2008);
        assertEquals(t, LocalDate.of(2008, 7, 15));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withYear_int_invalid() {
        TEST_2007_07_15.withYear(Year.MIN_VALUE - 1);
    }

    @Test
    public void test_withYear_int_adjustDay() {
        LocalDate t = LocalDate.of(2008, 2, 29).withYear(2007);
        LocalDate expected = LocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_int_normal() {
        LocalDate t = TEST_2007_07_15.withMonth(1);
        assertEquals(t, LocalDate.of(2007, 1, 15));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withMonth_int_invalid() {
        TEST_2007_07_15.withMonth(13);
    }

    @Test
    public void test_withMonth_int_adjustDay() {
        LocalDate t = LocalDate.of(2007, 12, 31).withMonth(11);
        LocalDate expected = LocalDate.of(2007, 11, 30);
        assertEquals(t, expected);
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth_normal() {
        LocalDate t = TEST_2007_07_15.withDayOfMonth(1);
        assertEquals(t, LocalDate.of(2007, 7, 1));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_illegal() {
        TEST_2007_07_15.withDayOfMonth(32);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_invalid() {
        LocalDate.of(2007, 11, 30).withDayOfMonth(31);
    }

    //-----------------------------------------------------------------------
    // withDayOfYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfYear_normal() {
        LocalDate t = TEST_2007_07_15.withDayOfYear(33);
        assertEquals(t, LocalDate.of(2007, 2, 2));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfYear_illegal() {
        TEST_2007_07_15.withDayOfYear(367);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfYear_invalid() {
        TEST_2007_07_15.withDayOfYear(366);
    }

    //-----------------------------------------------------------------------
    // plus(Period)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_Period_positiveMonths() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        LocalDate t = TEST_2007_07_15.plus(period);
        assertEquals(t, LocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_plus_Period_negativeDays() {
        MockSimplePeriod period = MockSimplePeriod.of(-25, ChronoUnit.DAYS);
        LocalDate t = TEST_2007_07_15.plus(period);
        assertEquals(t, LocalDate.of(2007, 6, 20));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_Period_timeNotAllowed() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.HOURS);
        TEST_2007_07_15.plus(period);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_Period_null() {
        TEST_2007_07_15.plus((MockSimplePeriod) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_Period_invalidTooLarge() {
        MockSimplePeriod period = MockSimplePeriod.of(1, ChronoUnit.YEARS);
        LocalDate.of(Year.MAX_VALUE, 1, 1).plus(period);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_Period_invalidTooSmall() {
        MockSimplePeriod period = MockSimplePeriod.of(-1, ChronoUnit.YEARS);
        LocalDate.of(Year.MIN_VALUE, 1, 1).plus(period);
    }

    //-----------------------------------------------------------------------
    // plus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_longPeriodUnit_positiveMonths() {
        LocalDate t = TEST_2007_07_15.plus(7, ChronoUnit.MONTHS);
        assertEquals(t, LocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_plus_longPeriodUnit_negativeDays() {
        LocalDate t = TEST_2007_07_15.plus(-25, ChronoUnit.DAYS);
        assertEquals(t, LocalDate.of(2007, 6, 20));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_longPeriodUnit_timeNotAllowed() {
        TEST_2007_07_15.plus(7, ChronoUnit.HOURS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_longPeriodUnit_null() {
        TEST_2007_07_15.plus(1, (TemporalUnit) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_longPeriodUnit_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 1, 1).plus(1, ChronoUnit.YEARS);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plus_longPeriodUnit_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plus(-1, ChronoUnit.YEARS);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears_long_normal() {
        LocalDate t = TEST_2007_07_15.plusYears(1);
        assertEquals(t, LocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_plusYears_long_negative() {
        LocalDate t = TEST_2007_07_15.plusYears(-1);
        assertEquals(t, LocalDate.of(2006, 7, 15));
    }

    @Test
    public void test_plusYears_long_adjustDay() {
        LocalDate t = LocalDate.of(2008, 2, 29).plusYears(1);
        LocalDate expected = LocalDate.of(2009, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusYears_long_big() {
        long years = 20L + Year.MAX_VALUE;
        LocalDate test = LocalDate.of(-40, 6, 1).plusYears(years);
        assertEquals(test, LocalDate.of((int) (-40L + years), 6, 1));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusYears_long_invalidTooLarge() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 6, 1);
        test.plusYears(1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMax() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.plusYears(Long.MAX_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusYears_long_invalidTooLargeMaxAddMin() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.plusYears(Long.MIN_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusYears_long_invalidTooSmall_validInt() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plusYears(-1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusYears_long_invalidTooSmall_invalidInt() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plusYears(-10);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths_long_normal() {
        LocalDate t = TEST_2007_07_15.plusMonths(1);
        assertEquals(t, LocalDate.of(2007, 8, 15));
    }

    @Test
    public void test_plusMonths_long_overYears() {
        LocalDate t = TEST_2007_07_15.plusMonths(25);
        assertEquals(t, LocalDate.of(2009, 8, 15));
    }

    @Test
    public void test_plusMonths_long_negative() {
        LocalDate t = TEST_2007_07_15.plusMonths(-1);
        assertEquals(t, LocalDate.of(2007, 6, 15));
    }

    @Test
    public void test_plusMonths_long_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.plusMonths(-7);
        assertEquals(t, LocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_plusMonths_long_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.plusMonths(-31);
        assertEquals(t, LocalDate.of(2004, 12, 15));
    }

    @Test
    public void test_plusMonths_long_adjustDayFromLeapYear() {
        LocalDate t = LocalDate.of(2008, 2, 29).plusMonths(12);
        LocalDate expected = LocalDate.of(2009, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusMonths_long_adjustDayFromMonthLength() {
        LocalDate t = LocalDate.of(2007, 3, 31).plusMonths(1);
        LocalDate expected = LocalDate.of(2007, 4, 30);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusMonths_long_big() {
        long months = 20L + Integer.MAX_VALUE;
        LocalDate test = LocalDate.of(-40, 6, 1).plusMonths(months);
        assertEquals(test, LocalDate.of((int) (-40L + months / 12), 6 + (int) (months % 12), 1));
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusMonths_long_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 1).plusMonths(1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMax() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.plusMonths(Long.MAX_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_plusMonths_long_invalidTooLargeMaxAddMin() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.plusMonths(Long.MIN_VALUE);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusMonths_long_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plusMonths(-1);
    }

    @Test
    public void test_plusWeeks_normal() {
        LocalDate t = TEST_2007_07_15.plusWeeks(1);
        assertEquals(t, LocalDate.of(2007, 7, 22));
    }

    @Test
    public void test_plusWeeks_overMonths() {
        LocalDate t = TEST_2007_07_15.plusWeeks(9);
        assertEquals(t, LocalDate.of(2007, 9, 16));
    }

    @Test
    public void test_plusWeeks_overYears() {
        LocalDate t = LocalDate.of(2006, 7, 16).plusWeeks(52);
        assertEquals(t, TEST_2007_07_15);
    }

    @Test
    public void test_plusWeeks_overLeapYears() {
        LocalDate t = TEST_2007_07_15.plusYears(-1).plusWeeks(104);
        assertEquals(t, LocalDate.of(2008, 7, 12));
    }

    @Test
    public void test_plusWeeks_negative() {
        LocalDate t = TEST_2007_07_15.plusWeeks(-1);
        assertEquals(t, LocalDate.of(2007, 7, 8));
    }

    @Test
    public void test_plusWeeks_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.plusWeeks(-28);
        assertEquals(t, LocalDate.of(2006, 12, 31));
    }

    @Test
    public void test_plusWeeks_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.plusWeeks(-104);
        assertEquals(t, LocalDate.of(2005, 7, 17));
    }

    @Test
    public void test_plusWeeks_maximum() {
        LocalDate t = LocalDate.of(Year.MAX_VALUE, 12, 24).plusWeeks(1);
        LocalDate expected = LocalDate.of(Year.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusWeeks_minimum() {
        LocalDate t = LocalDate.of(Year.MIN_VALUE, 1, 8).plusWeeks(-1);
        LocalDate expected = LocalDate.of(Year.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusWeeks_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).plusWeeks(1);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusWeeks_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 7).plusWeeks(-1);
    }

    @Test(expectedExceptions={ArithmeticException.class})
    public void test_plusWeeks_invalidMaxMinusMax() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).plusWeeks(Long.MAX_VALUE);
    }

    @Test(expectedExceptions={ArithmeticException.class})
    public void test_plusWeeks_invalidMaxMinusMin() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).plusWeeks(Long.MIN_VALUE);
    }

    @Test
    public void test_plusDays_normal() {
        LocalDate t = TEST_2007_07_15.plusDays(1);
        assertEquals(t, LocalDate.of(2007, 7, 16));
    }

    @Test
    public void test_plusDays_overMonths() {
        LocalDate t = TEST_2007_07_15.plusDays(62);
        assertEquals(t, LocalDate.of(2007, 9, 15));
    }

    @Test
    public void test_plusDays_overYears() {
        LocalDate t = LocalDate.of(2006, 7, 14).plusDays(366);
        assertEquals(t, TEST_2007_07_15);
    }

    @Test
    public void test_plusDays_overLeapYears() {
        LocalDate t = TEST_2007_07_15.plusYears(-1).plusDays(365 + 366);
        assertEquals(t, LocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_plusDays_negative() {
        LocalDate t = TEST_2007_07_15.plusDays(-1);
        assertEquals(t, LocalDate.of(2007, 7, 14));
    }

    @Test
    public void test_plusDays_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.plusDays(-196);
        assertEquals(t, LocalDate.of(2006, 12, 31));
    }

    @Test
    public void test_plusDays_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.plusDays(-730);
        assertEquals(t, LocalDate.of(2005, 7, 15));
    }

    @Test
    public void test_plusDays_maximum() {
        LocalDate t = LocalDate.of(Year.MAX_VALUE, 12, 30).plusDays(1);
        LocalDate expected = LocalDate.of(Year.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_plusDays_minimum() {
        LocalDate t = LocalDate.of(Year.MIN_VALUE, 1, 2).plusDays(-1);
        LocalDate expected = LocalDate.of(Year.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusDays_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 31).plusDays(1);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_plusDays_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plusDays(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 31).plusDays(Long.MAX_VALUE);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).plusDays(Long.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // minus(Period)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_Period_positiveMonths() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        LocalDate t = TEST_2007_07_15.minus(period);
        assertEquals(t, LocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_minus_Period_negativeDays() {
        MockSimplePeriod period = MockSimplePeriod.of(-25, ChronoUnit.DAYS);
        LocalDate t = TEST_2007_07_15.minus(period);
        assertEquals(t, LocalDate.of(2007, 8, 9));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_Period_timeNotAllowed() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.HOURS);
        TEST_2007_07_15.minus(period);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_Period_null() {
        TEST_2007_07_15.minus((MockSimplePeriod) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_Period_invalidTooLarge() {
        MockSimplePeriod period = MockSimplePeriod.of(-1, ChronoUnit.YEARS);
        LocalDate.of(Year.MAX_VALUE, 1, 1).minus(period);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_Period_invalidTooSmall() {
        MockSimplePeriod period = MockSimplePeriod.of(1, ChronoUnit.YEARS);
        LocalDate.of(Year.MIN_VALUE, 1, 1).minus(period);
    }

    //-----------------------------------------------------------------------
    // minus(long,PeriodUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_longPeriodUnit_positiveMonths() {
        LocalDate t = TEST_2007_07_15.minus(7, ChronoUnit.MONTHS);
        assertEquals(t, LocalDate.of(2006, 12, 15));
    }

    @Test
    public void test_minus_longPeriodUnit_negativeDays() {
        LocalDate t = TEST_2007_07_15.minus(-25, ChronoUnit.DAYS);
        assertEquals(t, LocalDate.of(2007, 8, 9));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_longPeriodUnit_timeNotAllowed() {
        TEST_2007_07_15.minus(7, ChronoUnit.HOURS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_longPeriodUnit_null() {
        TEST_2007_07_15.minus(1, (TemporalUnit) null);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_longPeriodUnit_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 1, 1).minus(-1, ChronoUnit.YEARS);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minus_longPeriodUnit_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).minus(1, ChronoUnit.YEARS);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears_long_normal() {
        LocalDate t = TEST_2007_07_15.minusYears(1);
        assertEquals(t, LocalDate.of(2006, 7, 15));
    }

    @Test
    public void test_minusYears_long_negative() {
        LocalDate t = TEST_2007_07_15.minusYears(-1);
        assertEquals(t, LocalDate.of(2008, 7, 15));
    }

    @Test
    public void test_minusYears_long_adjustDay() {
        LocalDate t = LocalDate.of(2008, 2, 29).minusYears(1);
        LocalDate expected = LocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusYears_long_big() {
        long years = 20L + Year.MAX_VALUE;
        LocalDate test = LocalDate.of(40, 6, 1).minusYears(years);
        assertEquals(test, LocalDate.of((int) (40L - years), 6, 1));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusYears_long_invalidTooLarge() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 6, 1);
        test.minusYears(-1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxAddMax() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.minusYears(Long.MAX_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusYears_long_invalidTooLargeMaxAddMin() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.minusYears(Long.MIN_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusYears_long_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).minusYears(1);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths_long_normal() {
        LocalDate t = TEST_2007_07_15.minusMonths(1);
        assertEquals(t, LocalDate.of(2007, 6, 15));
    }

    @Test
    public void test_minusMonths_long_overYears() {
        LocalDate t = TEST_2007_07_15.minusMonths(25);
        assertEquals(t, LocalDate.of(2005, 6, 15));
    }

    @Test
    public void test_minusMonths_long_negative() {
        LocalDate t = TEST_2007_07_15.minusMonths(-1);
        assertEquals(t, LocalDate.of(2007, 8, 15));
    }

    @Test
    public void test_minusMonths_long_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.minusMonths(-7);
        assertEquals(t, LocalDate.of(2008, 2, 15));
    }

    @Test
    public void test_minusMonths_long_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.minusMonths(-31);
        assertEquals(t, LocalDate.of(2010, 2, 15));
    }

    @Test
    public void test_minusMonths_long_adjustDayFromLeapYear() {
        LocalDate t = LocalDate.of(2008, 2, 29).minusMonths(12);
        LocalDate expected = LocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusMonths_long_adjustDayFromMonthLength() {
        LocalDate t = LocalDate.of(2007, 3, 31).minusMonths(1);
        LocalDate expected = LocalDate.of(2007, 2, 28);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusMonths_long_big() {
        long months = 20L + Integer.MAX_VALUE;
        LocalDate test = LocalDate.of(40, 6, 1).minusMonths(months);
        assertEquals(test, LocalDate.of((int) (40L - months / 12), 6 - (int) (months % 12), 1));
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusMonths_long_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 1).minusMonths(-1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxAddMax() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.minusMonths(Long.MAX_VALUE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_minusMonths_long_invalidTooLargeMaxAddMin() {
        LocalDate test = LocalDate.of(Year.MAX_VALUE, 12, 1);
        test.minusMonths(Long.MIN_VALUE);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusMonths_long_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).minusMonths(1);
    }

    @Test
    public void test_minusWeeks_normal() {
        LocalDate t = TEST_2007_07_15.minusWeeks(1);
        assertEquals(t, LocalDate.of(2007, 7, 8));
    }

    @Test
    public void test_minusWeeks_overMonths() {
        LocalDate t = TEST_2007_07_15.minusWeeks(9);
        assertEquals(t, LocalDate.of(2007, 5, 13));
    }

    @Test
    public void test_minusWeeks_overYears() {
        LocalDate t = LocalDate.of(2008, 7, 13).minusWeeks(52);
        assertEquals(t, TEST_2007_07_15);
    }

    @Test
    public void test_minusWeeks_overLeapYears() {
        LocalDate t = TEST_2007_07_15.minusYears(-1).minusWeeks(104);
        assertEquals(t, LocalDate.of(2006, 7, 18));
    }

    @Test
    public void test_minusWeeks_negative() {
        LocalDate t = TEST_2007_07_15.minusWeeks(-1);
        assertEquals(t, LocalDate.of(2007, 7, 22));
    }

    @Test
    public void test_minusWeeks_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.minusWeeks(-28);
        assertEquals(t, LocalDate.of(2008, 1, 27));
    }

    @Test
    public void test_minusWeeks_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.minusWeeks(-104);
        assertEquals(t, LocalDate.of(2009, 7, 12));
    }

    @Test
    public void test_minusWeeks_maximum() {
        LocalDate t = LocalDate.of(Year.MAX_VALUE, 12, 24).minusWeeks(-1);
        LocalDate expected = LocalDate.of(Year.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusWeeks_minimum() {
        LocalDate t = LocalDate.of(Year.MIN_VALUE, 1, 8).minusWeeks(1);
        LocalDate expected = LocalDate.of(Year.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusWeeks_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).minusWeeks(-1);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusWeeks_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 7).minusWeeks(1);
    }

    @Test(expectedExceptions={ArithmeticException.class})
    public void test_minusWeeks_invalidMaxMinusMax() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).minusWeeks(Long.MAX_VALUE);
    }

    @Test(expectedExceptions={ArithmeticException.class})
    public void test_minusWeeks_invalidMaxMinusMin() {
        LocalDate.of(Year.MAX_VALUE, 12, 25).minusWeeks(Long.MIN_VALUE);
    }

    @Test
    public void test_minusDays_normal() {
        LocalDate t = TEST_2007_07_15.minusDays(1);
        assertEquals(t, LocalDate.of(2007, 7, 14));
    }

    @Test
    public void test_minusDays_overMonths() {
        LocalDate t = TEST_2007_07_15.minusDays(62);
        assertEquals(t, LocalDate.of(2007, 5, 14));
    }

    @Test
    public void test_minusDays_overYears() {
        LocalDate t = LocalDate.of(2008, 7, 16).minusDays(367);
        assertEquals(t, TEST_2007_07_15);
    }

    @Test
    public void test_minusDays_overLeapYears() {
        LocalDate t = TEST_2007_07_15.plusYears(2).minusDays(365 + 366);
        assertEquals(t, TEST_2007_07_15);
    }

    @Test
    public void test_minusDays_negative() {
        LocalDate t = TEST_2007_07_15.minusDays(-1);
        assertEquals(t, LocalDate.of(2007, 7, 16));
    }

    @Test
    public void test_minusDays_negativeAcrossYear() {
        LocalDate t = TEST_2007_07_15.minusDays(-169);
        assertEquals(t, LocalDate.of(2007, 12, 31));
    }

    @Test
    public void test_minusDays_negativeOverYears() {
        LocalDate t = TEST_2007_07_15.minusDays(-731);
        assertEquals(t, LocalDate.of(2009, 7, 15));
    }

    @Test
    public void test_minusDays_maximum() {
        LocalDate t = LocalDate.of(Year.MAX_VALUE, 12, 30).minusDays(-1);
        LocalDate expected = LocalDate.of(Year.MAX_VALUE, 12, 31);
        assertEquals(t, expected);
    }

    @Test
    public void test_minusDays_minimum() {
        LocalDate t = LocalDate.of(Year.MIN_VALUE, 1, 2).minusDays(1);
        LocalDate expected = LocalDate.of(Year.MIN_VALUE, 1, 1);
        assertEquals(t, expected);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusDays_invalidTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 31).minusDays(-1);
    }

    @Test(expectedExceptions={DateTimeException.class})
    public void test_minusDays_invalidTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).minusDays(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooLarge() {
        LocalDate.of(Year.MAX_VALUE, 12, 31).minusDays(Long.MIN_VALUE);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooSmall() {
        LocalDate.of(Year.MIN_VALUE, 1, 1).minusDays(Long.MAX_VALUE);
    }

    //-----------------------------------------------------------------------
    // until()
    //-----------------------------------------------------------------------
    @DataProvider(name="until")
    Object[][] provider_until() {
        return new Object[][]{
                {"2012-06-30", "2012-06-30", DAYS, 0},
                {"2012-06-30", "2012-06-30", WEEKS, 0},
                {"2012-06-30", "2012-06-30", MONTHS, 0},
                {"2012-06-30", "2012-06-30", YEARS, 0},
                {"2012-06-30", "2012-06-30", DECADES, 0},
                {"2012-06-30", "2012-06-30", CENTURIES, 0},
                {"2012-06-30", "2012-06-30", MILLENNIA, 0},
                
                {"2012-06-30", "2012-07-01", DAYS, 1},
                {"2012-06-30", "2012-07-01", WEEKS, 0},
                {"2012-06-30", "2012-07-01", MONTHS, 0},
                {"2012-06-30", "2012-07-01", YEARS, 0},
                {"2012-06-30", "2012-07-01", DECADES, 0},
                {"2012-06-30", "2012-07-01", CENTURIES, 0},
                {"2012-06-30", "2012-07-01", MILLENNIA, 0},
                
                {"2012-06-30", "2012-07-07", DAYS, 7},
                {"2012-06-30", "2012-07-07", WEEKS, 1},
                {"2012-06-30", "2012-07-07", MONTHS, 0},
                {"2012-06-30", "2012-07-07", YEARS, 0},
                {"2012-06-30", "2012-07-07", DECADES, 0},
                {"2012-06-30", "2012-07-07", CENTURIES, 0},
                {"2012-06-30", "2012-07-07", MILLENNIA, 0},
                
                {"2012-06-30", "2012-07-29", MONTHS, 0},
                {"2012-06-30", "2012-07-30", MONTHS, 1},
                {"2012-06-30", "2012-07-31", MONTHS, 1},
        };
    }

    @Test(dataProvider = "until")
    public void test_until(String startStr, String endStr, TemporalUnit unit, long expected) {
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        assertEquals(start.until(end, unit), expected);
        assertEquals(end.until(start, unit), -expected);
    }

    //-----------------------------------------------------------------------
    // atTime()
    //-----------------------------------------------------------------------
    @Test
    public void test_atTime_LocalTime() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(LocalTime.of(11, 30)), LocalDateTime.of(2008, 6, 30, 11, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atTime_LocalTime_null() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime((LocalTime) null);
    }

    //-------------------------------------------------------------------------
    @Test
    public void test_atTime_int_int() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30), LocalDateTime.of(2008, 6, 30, 11, 30));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_hourTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(-1, 30);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_hourTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(24, 30);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_minuteTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, -1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_minuteTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 60);
    }

    @Test
    public void test_atTime_int_int_int() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30, 40), LocalDateTime.of(2008, 6, 30, 11, 30, 40));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_hourTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(-1, 30, 40);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_hourTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(24, 30, 40);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_minuteTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, -1, 40);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_minuteTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 60, 40);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_secondTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, -1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_secondTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 60);
    }

    @Test
    public void test_atTime_int_int_int_int() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        assertEquals(t.atTime(11, 30, 40, 50), LocalDateTime.of(2008, 6, 30, 11, 30, 40, 50));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_hourTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(-1, 30, 40, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_hourTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(24, 30, 40, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_minuteTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, -1, 40, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_minuteTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 60, 40, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_secondTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, -1, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_secondTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 60, 50);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_nanoTooSmall() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 40, -1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atTime_int_int_int_int_nanoTooBig() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atTime(11, 30, 40, 1000000000);
    }

    //-----------------------------------------------------------------------
    // atStartOfDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_atStartOfDay() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        assertEquals(t.atStartOfDay(ZONE_PARIS),
                ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 0, 0), ZONE_PARIS));
    }

    @Test
    public void test_atStartOfDay_dstGap() {
        LocalDate t = LocalDate.of(2007, 4, 1);
        assertEquals(t.atStartOfDay(ZONE_GAZA),
                ZonedDateTime.of(LocalDateTime.of(2007, 4, 1, 1, 0), ZONE_GAZA));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atStartOfDay_nullTimeZone() {
        LocalDate t = LocalDate.of(2008, 6, 30);
        t.atStartOfDay((ZoneId) null);
    }

    //-----------------------------------------------------------------------
    // toEpochDay()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochDay() {
        long date_0000_01_01 = -678941 - 40587;

        LocalDate test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i < 700000; i++) {
            assertEquals(test.toEpochDay(), i);
            test = next(test);
        }
        test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i > -2000000; i--) {
            assertEquals(test.toEpochDay(), i);
            test = previous(test);
        }

        assertEquals(LocalDate.of(1858, 11, 17).toEpochDay(), -40587);
        assertEquals(LocalDate.of(1, 1, 1).toEpochDay(), -678575 - 40587);
        assertEquals(LocalDate.of(1995, 9, 27).toEpochDay(), 49987 - 40587);
        assertEquals(LocalDate.of(1970, 1, 1).toEpochDay(), 0);
        assertEquals(LocalDate.of(-1, 12, 31).toEpochDay(), -678942 - 40587);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_LocalDate(
            LocalDate.of(Year.MIN_VALUE, 1, 1),
            LocalDate.of(Year.MIN_VALUE, 12, 31),
            LocalDate.of(-1, 1, 1),
            LocalDate.of(-1, 12, 31),
            LocalDate.of(0, 1, 1),
            LocalDate.of(0, 12, 31),
            LocalDate.of(1, 1, 1),
            LocalDate.of(1, 12, 31),
            LocalDate.of(2006, 1, 1),
            LocalDate.of(2006, 12, 31),
            LocalDate.of(2007, 1, 1),
            LocalDate.of(2007, 12, 31),
            LocalDate.of(2008, 1, 1),
            LocalDate.of(2008, 2, 29),
            LocalDate.of(2008, 12, 31),
            LocalDate.of(Year.MAX_VALUE, 1, 1),
            LocalDate.of(Year.MAX_VALUE, 12, 31)
        );
    }

    void doTest_comparisons_LocalDate(LocalDate... localDates) {
        for (int i = 0; i < localDates.length; i++) {
            LocalDate a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                LocalDate b = localDates[j];
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
        TEST_2007_07_15.compareTo(null);
    }

    @Test
    public void test_isBefore() {
        assertTrue(TEST_2007_07_15.isBefore(LocalDate.of(2007, 07, 16)));
        assertFalse(TEST_2007_07_15.isBefore(LocalDate.of(2007, 07, 14)));
        assertFalse(TEST_2007_07_15.isBefore(TEST_2007_07_15));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_ObjectNull() {
        TEST_2007_07_15.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_ObjectNull() {
        TEST_2007_07_15.isAfter(null);
    }

    @Test
    public void test_isAfter() {
        assertTrue(TEST_2007_07_15.isAfter(LocalDate.of(2007, 07, 14)));
        assertFalse(TEST_2007_07_15.isAfter(LocalDate.of(2007, 07, 16)));
        assertFalse(TEST_2007_07_15.isAfter(TEST_2007_07_15));
    }

    @Test(expectedExceptions=ClassCastException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonLocalDate() {
       Comparable c = TEST_2007_07_15;
       c.compareTo(new Object());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleDates" )
    public void test_equals_true(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        LocalDate b = LocalDate.of(y, m, d);
        assertEquals(a.equals(b), true);
    }
    @Test(dataProvider="sampleDates")
    public void test_equals_false_year_differs(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        LocalDate b = LocalDate.of(y + 1, m, d);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleDates")
    public void test_equals_false_month_differs(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        LocalDate b = LocalDate.of(y, m + 1, d);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleDates")
    public void test_equals_false_day_differs(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        LocalDate b = LocalDate.of(y, m, d + 1);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_2007_07_15.equals(TEST_2007_07_15), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_2007_07_15.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_2007_07_15.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleDates")
    public void test_hashCode(int y, int m, int d) {
        LocalDate a = LocalDate.of(y, m, d);
        assertEquals(a.hashCode(), a.hashCode());
        LocalDate b = LocalDate.of(y, m, d);
        assertEquals(a.hashCode(), b.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
        return new Object[][] {
            {2008, 7, 5, "2008-07-05"},
            {2007, 12, 31, "2007-12-31"},
            {999, 12, 31, "0999-12-31"},
            {-1, 1, 2, "-0001-01-02"},
            {9999, 12, 31, "9999-12-31"},
            {-9999, 12, 31, "-9999-12-31"},
            {10000, 1, 1, "+10000-01-01"},
            {-10000, 1, 1, "-10000-01-01"},
            {12345678, 1, 1, "+12345678-01-01"},
            {-12345678, 1, 1, "-12345678-01-01"},
        };
    }

    @Test(dataProvider="sampleToString")
    public void test_toString(int y, int m, int d, String expected) {
        LocalDate t = LocalDate.of(y, m, d);
        String str = t.toString();
        assertEquals(str, expected);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d");
        String t = LocalDate.of(2010, 12, 3).format(f);
        assertEquals(t, "2010 12 3");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        LocalDate.of(2010, 12, 3).format(null);
    }

}
