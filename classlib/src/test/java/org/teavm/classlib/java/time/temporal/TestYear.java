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
package org.teavm.classlib.java.time.temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.AbstractDateTimeTest;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TMonthDay;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TYearMonth;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;

@Test
public class TestYear extends AbstractDateTimeTest {

    private static final TYear TEST_2008 = TYear.of(2008);

    @Before
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {TEST_2008, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {
        TTemporalField[] array = {
            YEAR_OF_ERA,
            YEAR,
            ERA,
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
    public void test_immutable() {
        assertImmutable(TYear.class);
    }

    @Test
    public void test_serialization() throws ClassNotFoundException, IOException {
        assertSerializable(TYear.of(-1));
    }

    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        assertEqualsSerialisedForm(TYear.of(2012));
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        TYear expected = TYear.now(TClock.systemDefaultZone());
        TYear test = TYear.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TYear.now(TClock.systemDefaultZone());
            test = TYear.now();
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(TZoneId)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_ZoneId_nullZoneId() {
        TYear.now((TZoneId) null);
    }

    @Test
    public void now_ZoneId() {
        TZoneId zone = TZoneId.of("UTC+01:02:03");
        TYear expected = TYear.now(TClock.system(zone));
        TYear test = TYear.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = TYear.now(TClock.system(zone));
            test = TYear.now(zone);
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(TClock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock() {
        TInstant instant = TLocalDateTime.of(2010, 12, 31, 0, 0).toInstant(TZoneOffset.UTC);
        TClock clock = TClock.fixed(instant, TZoneOffset.UTC);
        TYear test = TYear.now(clock);
        assertEquals(test.getValue(), 2010);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        TYear.now((TClock) null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_singleton() {
        for (int i = -4; i <= 2104; i++) {
            TYear test = TYear.of(i);
            assertEquals(test.getValue(), i);
            assertEquals(TYear.of(i), test);
        }
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_tooLow() {
        TYear.of(TYear.MIN_VALUE - 1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_tooHigh() {
        TYear.of(TYear.MAX_VALUE + 1);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(TYear.from(TLocalDate.of(2007, 7, 15)), TYear.of(2007));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        TYear.from(TLocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        TYear.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @DataProvider(name="goodParseData")
    Object[][] provider_goodParseData() {
        return new Object[][] {
                {"0000", TYear.of(0)},
                {"9999", TYear.of(9999)},
                {"2000", TYear.of(2000)},

                {"+12345678", TYear.of(12345678)},
                {"+123456", TYear.of(123456)},
                {"-1234", TYear.of(-1234)},
                {"-12345678", TYear.of(-12345678)},

                {"+" + TYear.MAX_VALUE, TYear.of(TYear.MAX_VALUE)},
                {"" + TYear.MIN_VALUE, TYear.of(TYear.MIN_VALUE)},
        };
    }

    @Test(dataProvider="goodParseData")
    public void factory_parse_success(String text, TYear expected) {
        TYear year = TYear.parse(text);
        assertEquals(year, expected);
    }

    @DataProvider(name="badParseData")
    Object[][] provider_badParseData() {
        return new Object[][] {
                {"", 0},
                {"-00", 1},
                {"--01-0", 1},
                {"A01", 0},
                {"200", 0},
                {"2009/12", 4},

                {"-0000-10", 0},
                {"-12345678901-10", 11},
                {"+1-10", 1},
                {"+12-10", 1},
                {"+123-10", 1},
                {"+1234-10", 0},
                {"12345-10", 0},
                {"+12345678901-10", 11},
        };
    }

    @Test(dataProvider="badParseData", expectedExceptions=TDateTimeParseException.class)
    public void factory_parse_fail(String text, int pos) {
        try {
            TYear.parse(text);
            fail(String.format("Parse should have failed for %s at position %d", text, pos));
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getParsedString(), text);
            assertEquals(ex.getErrorIndex(), pos);
            throw ex;
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        TYear.parse(null);
    }

    //-----------------------------------------------------------------------
    // parse(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u");
        TYear test = TYear.parse("2010", f);
        assertEquals(test, TYear.of(2010));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("u");
        TYear.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        TYear.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // get(DateTimeField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_DateTimeField() {
        assertEquals(TEST_2008.getLong(TChronoField.YEAR), 2008);
        assertEquals(TEST_2008.getLong(TChronoField.YEAR_OF_ERA), 2008);
        assertEquals(TEST_2008.getLong(TChronoField.ERA), 1);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_get_DateTimeField_null() {
        TEST_2008.getLong((TTemporalField) null);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_get_DateTimeField_invalidField() {
        TEST_2008.getLong(MockFieldNoValue.INSTANCE);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_get_DateTimeField_timeField() {
        TEST_2008.getLong(TChronoField.AMPM_OF_DAY);
    }

    //-----------------------------------------------------------------------
    // isLeap()
    //-----------------------------------------------------------------------
    @Test
    public void test_isLeap() {
        assertEquals(TYear.of(1999).isLeap(), false);
        assertEquals(TYear.of(2000).isLeap(), true);
        assertEquals(TYear.of(2001).isLeap(), false);

        assertEquals(TYear.of(2007).isLeap(), false);
        assertEquals(TYear.of(2008).isLeap(), true);
        assertEquals(TYear.of(2009).isLeap(), false);
        assertEquals(TYear.of(2010).isLeap(), false);
        assertEquals(TYear.of(2011).isLeap(), false);
        assertEquals(TYear.of(2012).isLeap(), true);

        assertEquals(TYear.of(2095).isLeap(), false);
        assertEquals(TYear.of(2096).isLeap(), true);
        assertEquals(TYear.of(2097).isLeap(), false);
        assertEquals(TYear.of(2098).isLeap(), false);
        assertEquals(TYear.of(2099).isLeap(), false);
        assertEquals(TYear.of(2100).isLeap(), false);
        assertEquals(TYear.of(2101).isLeap(), false);
        assertEquals(TYear.of(2102).isLeap(), false);
        assertEquals(TYear.of(2103).isLeap(), false);
        assertEquals(TYear.of(2104).isLeap(), true);
        assertEquals(TYear.of(2105).isLeap(), false);

        assertEquals(TYear.of(-500).isLeap(), false);
        assertEquals(TYear.of(-400).isLeap(), true);
        assertEquals(TYear.of(-300).isLeap(), false);
        assertEquals(TYear.of(-200).isLeap(), false);
        assertEquals(TYear.of(-100).isLeap(), false);
        assertEquals(TYear.of(0).isLeap(), true);
        assertEquals(TYear.of(100).isLeap(), false);
        assertEquals(TYear.of(200).isLeap(), false);
        assertEquals(TYear.of(300).isLeap(), false);
        assertEquals(TYear.of(400).isLeap(), true);
        assertEquals(TYear.of(500).isLeap(), false);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        assertEquals(TYear.of(2007).plusYears(-1), TYear.of(2006));
        assertEquals(TYear.of(2007).plusYears(0), TYear.of(2007));
        assertEquals(TYear.of(2007).plusYears(1), TYear.of(2008));
        assertEquals(TYear.of(2007).plusYears(2), TYear.of(2009));

        assertEquals(TYear.of(TYear.MAX_VALUE - 1).plusYears(1), TYear.of(TYear.MAX_VALUE));
        assertEquals(TYear.of(TYear.MAX_VALUE).plusYears(0), TYear.of(TYear.MAX_VALUE));

        assertEquals(TYear.of(TYear.MIN_VALUE + 1).plusYears(-1), TYear.of(TYear.MIN_VALUE));
        assertEquals(TYear.of(TYear.MIN_VALUE).plusYears(0), TYear.of(TYear.MIN_VALUE));
    }

    @Test
    public void test_plusYear_zero_equals() {
        TYear base = TYear.of(2007);
        assertEquals(base.plusYears(0), base);
    }

    @Test
    public void test_plusYears_big() {
        long years = 20L + TYear.MAX_VALUE;
        assertEquals(TYear.of(-40).plusYears(years), TYear.of((int) (-40L + years)));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_plusYears_max() {
        TYear.of(TYear.MAX_VALUE).plusYears(1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_plusYears_maxLots() {
        TYear.of(TYear.MAX_VALUE).plusYears(1000);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_plusYears_min() {
        TYear.of(TYear.MIN_VALUE).plusYears(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_plusYears_minLots() {
        TYear.of(TYear.MIN_VALUE).plusYears(-1000);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        assertEquals(TYear.of(2007).minusYears(-1), TYear.of(2008));
        assertEquals(TYear.of(2007).minusYears(0), TYear.of(2007));
        assertEquals(TYear.of(2007).minusYears(1), TYear.of(2006));
        assertEquals(TYear.of(2007).minusYears(2), TYear.of(2005));

        assertEquals(TYear.of(TYear.MAX_VALUE - 1).minusYears(-1), TYear.of(TYear.MAX_VALUE));
        assertEquals(TYear.of(TYear.MAX_VALUE).minusYears(0), TYear.of(TYear.MAX_VALUE));

        assertEquals(TYear.of(TYear.MIN_VALUE + 1).minusYears(1), TYear.of(TYear.MIN_VALUE));
        assertEquals(TYear.of(TYear.MIN_VALUE).minusYears(0), TYear.of(TYear.MIN_VALUE));
    }

    @Test
    public void test_minusYear_zero_equals() {
        TYear base = TYear.of(2007);
        assertEquals(base.minusYears(0), base);
    }

    @Test
    public void test_minusYears_big() {
        long years = 20L + TYear.MAX_VALUE;
        assertEquals(TYear.of(40).minusYears(years), TYear.of((int) (40L - years)));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_minusYears_max() {
        TYear.of(TYear.MAX_VALUE).minusYears(-1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_minusYears_maxLots() {
        TYear.of(TYear.MAX_VALUE).minusYears(-1000);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_minusYears_min() {
        TYear.of(TYear.MIN_VALUE).minusYears(1);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_minusYears_minLots() {
        TYear.of(TYear.MIN_VALUE).minusYears(1000);
    }

    //-----------------------------------------------------------------------
    // doAdjustment()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustDate() {
        TLocalDate base = TLocalDate.of(2007, 2, 12);
        for (int i = -4; i <= 2104; i++) {
            TTemporal result = TYear.of(i).adjustInto(base);
            assertEquals(result, TLocalDate.of(i, 2, 12));
        }
    }

    @Test
    public void test_adjustDate_resolve() {
        TYear test = TYear.of(2011);
        assertEquals(test.adjustInto(TLocalDate.of(2012, 2, 29)), TLocalDate.of(2011, 2, 28));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_adjustDate_nullLocalDate() {
        TYear test = TYear.of(1);
        test.adjustInto((TLocalDate) null);
    }

    //-----------------------------------------------------------------------
    // length()
    //-----------------------------------------------------------------------
    @Test
    public void test_length() {
        assertEquals(TYear.of(1999).length(), 365);
        assertEquals(TYear.of(2000).length(), 366);
        assertEquals(TYear.of(2001).length(), 365);

        assertEquals(TYear.of(2007).length(), 365);
        assertEquals(TYear.of(2008).length(), 366);
        assertEquals(TYear.of(2009).length(), 365);
        assertEquals(TYear.of(2010).length(), 365);
        assertEquals(TYear.of(2011).length(), 365);
        assertEquals(TYear.of(2012).length(), 366);

        assertEquals(TYear.of(2095).length(), 365);
        assertEquals(TYear.of(2096).length(), 366);
        assertEquals(TYear.of(2097).length(), 365);
        assertEquals(TYear.of(2098).length(), 365);
        assertEquals(TYear.of(2099).length(), 365);
        assertEquals(TYear.of(2100).length(), 365);
        assertEquals(TYear.of(2101).length(), 365);
        assertEquals(TYear.of(2102).length(), 365);
        assertEquals(TYear.of(2103).length(), 365);
        assertEquals(TYear.of(2104).length(), 366);
        assertEquals(TYear.of(2105).length(), 365);

        assertEquals(TYear.of(-500).length(), 365);
        assertEquals(TYear.of(-400).length(), 366);
        assertEquals(TYear.of(-300).length(), 365);
        assertEquals(TYear.of(-200).length(), 365);
        assertEquals(TYear.of(-100).length(), 365);
        assertEquals(TYear.of(0).length(), 366);
        assertEquals(TYear.of(100).length(), 365);
        assertEquals(TYear.of(200).length(), 365);
        assertEquals(TYear.of(300).length(), 365);
        assertEquals(TYear.of(400).length(), 366);
        assertEquals(TYear.of(500).length(), 365);
    }

    //-----------------------------------------------------------------------
    // isValidMonthDay(TMonth)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidMonthDay_june() {
        TYear test = TYear.of(2007);
        TMonthDay monthDay = TMonthDay.of(6, 30);
        assertEquals(test.isValidMonthDay(monthDay), true);
    }

    @Test
    public void test_isValidMonthDay_febNonLeap() {
        TYear test = TYear.of(2007);
        TMonthDay monthDay = TMonthDay.of(2, 29);
        assertEquals(test.isValidMonthDay(monthDay), false);
    }

    @Test
    public void test_isValidMonthDay_febLeap() {
        TYear test = TYear.of(2008);
        TMonthDay monthDay = TMonthDay.of(2, 29);
        assertEquals(test.isValidMonthDay(monthDay), true);
    }

    @Test
    public void test_isValidMonthDay_null() {
        TYear test = TYear.of(2008);
        assertEquals(test.isValidMonthDay(null), false);
    }

    //-----------------------------------------------------------------------
    // atMonth(TMonth)
    //-----------------------------------------------------------------------
    @Test
    public void test_atMonth() {
        TYear test = TYear.of(2008);
        assertEquals(test.atMonth(TMonth.JUNE), TYearMonth.of(2008, 6));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atMonth_nullMonth() {
        TYear test = TYear.of(2008);
        test.atMonth((TMonth) null);
    }

    //-----------------------------------------------------------------------
    // atMonth(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atMonth_int() {
        TYear test = TYear.of(2008);
        assertEquals(test.atMonth(6), TYearMonth.of(2008, 6));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_atMonth_int_invalidMonth() {
        TYear test = TYear.of(2008);
        test.atMonth(13);
    }

    //-----------------------------------------------------------------------
    // atMonthDay(TMonthDay)
    //-----------------------------------------------------------------------
    @DataProvider(name="atMonthDay")
    Object[][] data_atMonthDay() {
        return new Object[][] {
                {TYear.of(2008), TMonthDay.of(6, 30), TLocalDate.of(2008, 6, 30)},
                {TYear.of(2008), TMonthDay.of(2, 29), TLocalDate.of(2008, 2, 29)},
                {TYear.of(2009), TMonthDay.of(2, 29), TLocalDate.of(2009, 2, 28)},
        };
    }

    @Test(dataProvider="atMonthDay")
    public void test_atMonthDay(TYear year, TMonthDay monthDay, TLocalDate expected) {
        assertEquals(year.atMonthDay(monthDay), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_atMonthDay_nullMonthDay() {
        TYear test = TYear.of(2008);
        test.atMonthDay((TMonthDay) null);
    }

    //-----------------------------------------------------------------------
    // atDay(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atDay_notLeapYear() {
        TYear test = TYear.of(2007);
        TLocalDate expected = TLocalDate.of(2007, 1, 1);
        for (int i = 1; i <= 365; i++) {
            assertEquals(test.atDay(i), expected);
            expected = expected.plusDays(1);
        }
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_atDay_notLeapYear_day366() {
        TYear test = TYear.of(2007);
        test.atDay(366);
    }

    @Test
    public void test_atDay_leapYear() {
        TYear test = TYear.of(2008);
        TLocalDate expected = TLocalDate.of(2008, 1, 1);
        for (int i = 1; i <= 366; i++) {
            assertEquals(test.atDay(i), expected);
            expected = expected.plusDays(1);
        }
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_atDay_day0() {
        TYear test = TYear.of(2007);
        test.atDay(0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_atDay_day367() {
        TYear test = TYear.of(2007);
        test.atDay(367);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TEST_2008.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(TEST_2008.query(TTemporalQueries.localDate()), null);
        assertEquals(TEST_2008.query(TTemporalQueries.localTime()), null);
        assertEquals(TEST_2008.query(TTemporalQueries.offset()), null);
        assertEquals(TEST_2008.query(TTemporalQueries.precision()), TChronoUnit.YEARS);
        assertEquals(TEST_2008.query(TTemporalQueries.zone()), null);
        assertEquals(TEST_2008.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_2008.query(null);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo() {
        for (int i = -4; i <= 2104; i++) {
            TYear a = TYear.of(i);
            for (int j = -4; j <= 2104; j++) {
                TYear b = TYear.of(j);
                if (i < j) {
                    assertEquals(a.compareTo(b) < 0, true);
                    assertEquals(b.compareTo(a) > 0, true);
                    assertEquals(a.isAfter(b), false);
                    assertEquals(a.isBefore(b), true);
                    assertEquals(b.isAfter(a), true);
                    assertEquals(b.isBefore(a), false);
                } else if (i > j) {
                    assertEquals(a.compareTo(b) > 0, true);
                    assertEquals(b.compareTo(a) < 0, true);
                    assertEquals(a.isAfter(b), true);
                    assertEquals(a.isBefore(b), false);
                    assertEquals(b.isAfter(a), false);
                    assertEquals(b.isBefore(a), true);
                } else {
                    assertEquals(a.compareTo(b), 0);
                    assertEquals(b.compareTo(a), 0);
                    assertEquals(a.isAfter(b), false);
                    assertEquals(a.isBefore(b), false);
                    assertEquals(b.isAfter(a), false);
                    assertEquals(b.isBefore(a), false);
                }
            }
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_nullYear() {
        TYear doy = null;
        TYear test = TYear.of(1);
        test.compareTo(doy);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        for (int i = -4; i <= 2104; i++) {
            TYear a = TYear.of(i);
            for (int j = -4; j <= 2104; j++) {
                TYear b = TYear.of(j);
                assertEquals(a.equals(b), i == j);
                assertEquals(a.hashCode() == b.hashCode(), i == j);
            }
        }
    }

    @Test
    public void test_equals_same() {
        TYear test = TYear.of(2011);
        assertEquals(test.equals(test), true);
    }

    @Test
    public void test_equals_nullYear() {
        TYear doy = null;
        TYear test = TYear.of(1);
        assertEquals(test.equals(doy), false);
    }

    @Test
    public void test_equals_incorrectType() {
        TYear test = TYear.of(1);
        assertEquals(test.equals("Incorrect type"), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        for (int i = -4; i <= 2104; i++) {
            TYear a = TYear.of(i);
            assertEquals(a.toString(), "" + i);
        }
    }

    //-----------------------------------------------------------------------
    // format(TDateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        TDateTimeFormatter f = TDateTimeFormatter.ofPattern("y");
        String t = TYear.of(2010).format(f);
        assertEquals(t, "2010");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void format_formatter_null() {
        TYear.of(2010).format(null);
    }

}
