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
package org.teavm.classlib.java.time.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Iterator;
import org.teavm.classlib.java.util.TLocale;
import java.util.Map;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TYearMonth;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TIsoFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

@Test
public class TestDateTimeFormatters {

    @Before
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_print_nullCalendrical() {
        TDateTimeFormatter.ISO_DATE.format((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_pattern_String() {
        TDateTimeFormatter test = TDateTimeFormatter.ofPattern("d MMM uuuu");
        assertEquals(test.toString(), "Value(DayOfMonth)' 'Text(MonthOfYear,SHORT)' 'Value(TYear,4,19,EXCEEDS_PAD)");
        assertEquals(test.getLocale(), TLocale.getDefault());
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_pattern_String_invalid() {
        TDateTimeFormatter.ofPattern("p");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_pattern_String_null() {
        TDateTimeFormatter.ofPattern(null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_pattern_StringLocale() {
        TDateTimeFormatter test = TDateTimeFormatter.ofPattern("d MMM uuuu", TLocale.UK);
        assertEquals(test.toString(), "Value(DayOfMonth)' 'Text(MonthOfYear,SHORT)' 'Value(TYear,4,19,EXCEEDS_PAD)");
        assertEquals(test.getLocale(), TLocale.UK);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_pattern_StringLocale_invalid() {
        TDateTimeFormatter.ofPattern("p", TLocale.UK);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_pattern_StringLocale_nullPattern() {
        TDateTimeFormatter.ofPattern(null, TLocale.UK);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_pattern_StringLocale_nullLocale() {
        TDateTimeFormatter.ofPattern("yyyy", null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoLocalDate")
    Object[][] provider_sample_isoLocalDate() {
        return new Object[][]{
                {2008, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, TDateTimeException.class},
                {null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, null, null, null, TDateTimeException.class},

                {2008, 6, 30, null, null,                   "2008-06-30", null},
                {2008, 6, 30, "+01:00", null,               "2008-06-30", null},
                {2008, 6, 30, "+01:00", "Europe/Paris",     "2008-06-30", null},
                {2008, 6, 30, null, "Europe/Paris",         "2008-06-30", null},

                {123456, 6, 30, null, null,                 "+123456-06-30", null},
        };
    }

    @Test(dataProvider="sample_isoLocalDate")
    public void test_print_isoLocalDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, null, null, null, null, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_LOCAL_DATE.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_LOCAL_DATE.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoLocalDate")
    public void test_parse_isoLocalDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDate(year, month, day);
            // offset/zone not expected to be parsed
            assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    @Test
    public void test_parse_isoLocalDate_999999999() {
        Expected expected = createDate(999999999, 8, 6);
        assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("+999999999-08-06", new ParsePosition(0)), expected);
        assertEquals(TLocalDate.parse("+999999999-08-06"), TLocalDate.of(999999999, 8, 6));
    }

    @Test
    public void test_parse_isoLocalDate_1000000000() {
        Expected expected = createDate(1000000000, 8, 6);
        assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("+1000000000-08-06", new ParsePosition(0)), expected);
    }

    @Test(expectedExceptions = TDateTimeException.class)
    public void test_parse_isoLocalDate_1000000000_failedCreate() {
        TLocalDate.parse("+1000000000-08-06");
    }

    @Test
    public void test_parse_isoLocalDate_M999999999() {
        Expected expected = createDate(-999999999, 8, 6);
        assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("-999999999-08-06", new ParsePosition(0)), expected);
        assertEquals(TLocalDate.parse("-999999999-08-06"), TLocalDate.of(-999999999, 8, 6));
    }

    @Test
    public void test_parse_isoLocalDate_M1000000000() {
        Expected expected = createDate(-1000000000, 8, 6);
        assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("-1000000000-08-06", new ParsePosition(0)), expected);
    }

    @Test(expectedExceptions = TDateTimeException.class)
    public void test_parse_isoLocalDate_M1000000000_failedCreate() {
        TLocalDate.parse("-1000000000-08-06");
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoOffsetDate")
    Object[][] provider_sample_isoOffsetDate() {
        return new Object[][]{
                {2008, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, TDateTimeException.class},
                {null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, null, null, null, TDateTimeException.class},

                {2008, 6, 30, null, null,                   null, TDateTimeException.class},
                {2008, 6, 30, "+01:00", null,               "2008-06-30+01:00", null},
                {2008, 6, 30, "+01:00", "Europe/Paris",     "2008-06-30+01:00", null},
                {2008, 6, 30, null, "Europe/Paris",         null, TDateTimeException.class},

                {123456, 6, 30, "+01:00", null,             "+123456-06-30+01:00", null},
        };
    }

    @Test(dataProvider="sample_isoOffsetDate")
    public void test_print_isoOffsetDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, null, null, null, null, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_OFFSET_DATE.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_OFFSET_DATE.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoOffsetDate")
    public void test_parse_isoOffsetDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDate(year, month, day);
            buildCalendrical(expected, offsetId, null);  // zone not expected to be parsed
            assertParseMatch(TDateTimeFormatter.ISO_OFFSET_DATE.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoDate")
    Object[][] provider_sample_isoDate() {
        return new Object[][]{
                {2008, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, TDateTimeException.class},
                {null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, null, null, null, TDateTimeException.class},

                {2008, 6, 30, null, null,                   "2008-06-30", null},
                {2008, 6, 30, "+01:00", null,               "2008-06-30+01:00", null},
                {2008, 6, 30, "+01:00", "Europe/Paris",     "2008-06-30+01:00", null},
                {2008, 6, 30, null, "Europe/Paris",         "2008-06-30", null},

                {123456, 6, 30, "+01:00", "Europe/Paris",   "+123456-06-30+01:00", null},
        };
    }

    @Test(dataProvider="sample_isoDate")
    public void test_print_isoDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, null, null, null, null, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_DATE.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_DATE.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoDate")
    public void test_parse_isoDate(
            Integer year, Integer month, Integer day, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDate(year, month, day);
            if (offsetId != null) {
                expected.fieldValues.put(OFFSET_SECONDS, (long) TZoneOffset.of(offsetId).getTotalSeconds());
            }
            assertParseMatch(TDateTimeFormatter.ISO_DATE.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoLocalTime")
    Object[][] provider_sample_isoLocalTime() {
        return new Object[][]{
                {11, null, null, null, null, null, null, TDateTimeException.class},
                {null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, TDateTimeException.class},
                {null, null, null, 1, null, null, null, TDateTimeException.class},
                {null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},

                {11, 5, null, null, null, null,     "11:05", null},
                {11, 5, 30, null, null, null,       "11:05:30", null},
                {11, 5, 30, 500000000, null, null,  "11:05:30.5", null},
                {11, 5, 30, 1, null, null,          "11:05:30.000000001", null},

                {11, 5, null, null, "+01:00", null,     "11:05", null},
                {11, 5, 30, null, "+01:00", null,       "11:05:30", null},
                {11, 5, 30, 500000000, "+01:00", null,  "11:05:30.5", null},
                {11, 5, 30, 1, "+01:00", null,          "11:05:30.000000001", null},

                {11, 5, null, null, "+01:00", "Europe/Paris",       "11:05", null},
                {11, 5, 30, null, "+01:00", "Europe/Paris",         "11:05:30", null},
                {11, 5, 30, 500000000, "+01:00", "Europe/Paris",    "11:05:30.5", null},
                {11, 5, 30, 1, "+01:00", "Europe/Paris",            "11:05:30.000000001", null},

                {11, 5, null, null, null, "Europe/Paris",       "11:05", null},
                {11, 5, 30, null, null, "Europe/Paris",         "11:05:30", null},
                {11, 5, 30, 500000000, null, "Europe/Paris",    "11:05:30.5", null},
                {11, 5, 30, 1, null, "Europe/Paris",            "11:05:30.000000001", null},
        };
    }

    @Test(dataProvider="sample_isoLocalTime")
    public void test_print_isoLocalTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(null, null, null, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_LOCAL_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_LOCAL_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoLocalTime")
    public void test_parse_isoLocalTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createTime(hour, min, sec, nano);
            // offset/zone not expected to be parsed
            assertParseMatch(TDateTimeFormatter.ISO_LOCAL_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoOffsetTime")
    Object[][] provider_sample_isoOffsetTime() {
        return new Object[][]{
                {11, null, null, null, null, null, null, TDateTimeException.class},
                {null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, TDateTimeException.class},
                {null, null, null, 1, null, null, null, TDateTimeException.class},
                {null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},

                {11, 5, null, null, null, null,     null, TDateTimeException.class},
                {11, 5, 30, null, null, null,       null, TDateTimeException.class},
                {11, 5, 30, 500000000, null, null,  null, TDateTimeException.class},
                {11, 5, 30, 1, null, null,          null, TDateTimeException.class},

                {11, 5, null, null, "+01:00", null,     "11:05+01:00", null},
                {11, 5, 30, null, "+01:00", null,       "11:05:30+01:00", null},
                {11, 5, 30, 500000000, "+01:00", null,  "11:05:30.5+01:00", null},
                {11, 5, 30, 1, "+01:00", null,          "11:05:30.000000001+01:00", null},

                {11, 5, null, null, "+01:00", "Europe/Paris",       "11:05+01:00", null},
                {11, 5, 30, null, "+01:00", "Europe/Paris",         "11:05:30+01:00", null},
                {11, 5, 30, 500000000, "+01:00", "Europe/Paris",    "11:05:30.5+01:00", null},
                {11, 5, 30, 1, "+01:00", "Europe/Paris",            "11:05:30.000000001+01:00", null},

                {11, 5, null, null, null, "Europe/Paris",       null, TDateTimeException.class},
                {11, 5, 30, null, null, "Europe/Paris",         null, TDateTimeException.class},
                {11, 5, 30, 500000000, null, "Europe/Paris",    null, TDateTimeException.class},
                {11, 5, 30, 1, null, "Europe/Paris",            null, TDateTimeException.class},
        };
    }

    @Test(dataProvider="sample_isoOffsetTime")
    public void test_print_isoOffsetTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(null, null, null, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_OFFSET_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_OFFSET_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoOffsetTime")
    public void test_parse_isoOffsetTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createTime(hour, min, sec, nano);
            buildCalendrical(expected, offsetId, null);  // zoneId is not expected from parse
            assertParseMatch(TDateTimeFormatter.ISO_OFFSET_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoTime")
    Object[][] provider_sample_isoTime() {
        return new Object[][]{
                {11, null, null, null, null, null, null, TDateTimeException.class},
                {null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, TDateTimeException.class},
                {null, null, null, 1, null, null, null, TDateTimeException.class},
                {null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},

                {11, 5, null, null, null, null,     "11:05", null},
                {11, 5, 30, null, null, null,       "11:05:30", null},
                {11, 5, 30, 500000000, null, null,  "11:05:30.5", null},
                {11, 5, 30, 1, null, null,          "11:05:30.000000001", null},

                {11, 5, null, null, "+01:00", null,     "11:05+01:00", null},
                {11, 5, 30, null, "+01:00", null,       "11:05:30+01:00", null},
                {11, 5, 30, 500000000, "+01:00", null,  "11:05:30.5+01:00", null},
                {11, 5, 30, 1, "+01:00", null,          "11:05:30.000000001+01:00", null},

                {11, 5, null, null, "+01:00", "Europe/Paris",       "11:05+01:00", null},
                {11, 5, 30, null, "+01:00", "Europe/Paris",         "11:05:30+01:00", null},
                {11, 5, 30, 500000000, "+01:00", "Europe/Paris",    "11:05:30.5+01:00", null},
                {11, 5, 30, 1, "+01:00", "Europe/Paris",            "11:05:30.000000001+01:00", null},

                {11, 5, null, null, null, "Europe/Paris",       "11:05", null},
                {11, 5, 30, null, null, "Europe/Paris",         "11:05:30", null},
                {11, 5, 30, 500000000, null, "Europe/Paris",    "11:05:30.5", null},
                {11, 5, 30, 1, null, "Europe/Paris",            "11:05:30.000000001", null},
        };
    }

    @Test(dataProvider="sample_isoTime")
    public void test_print_isoTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(null, null, null, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoTime")
    public void test_parse_isoTime(
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createTime(hour, min, sec, nano);
            if (offsetId != null) {
                expected.fieldValues.put(OFFSET_SECONDS, (long) TZoneOffset.of(offsetId).getTotalSeconds());
            }
            assertParseMatch(TDateTimeFormatter.ISO_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoLocalDateTime")
    Object[][] provider_sample_isoLocalDateTime() {
        return new Object[][]{
                {2008, null, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, 11, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, 30, 11, null, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, 30, null, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, null, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, null, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},

                {2008, 6, 30, 11, 5, null, null, null, null,                    "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, null, null,                      "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, null, null,                 "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, null, null,                         "2008-06-30T11:05:30.000000001", null},

                {2008, 6, 30, 11, 5, null, null, "+01:00", null,                "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", null,                  "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", null,             "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", null,                     "2008-06-30T11:05:30.000000001", null},

                {2008, 6, 30, 11, 5, null, null, "+01:00", "Europe/Paris",      "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", "Europe/Paris",        "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", "Europe/Paris",   "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", "Europe/Paris",           "2008-06-30T11:05:30.000000001", null},

                {2008, 6, 30, 11, 5, null, null, null, "Europe/Paris",          "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, null, "Europe/Paris",            "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, null, "Europe/Paris",       "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, null, "Europe/Paris",               "2008-06-30T11:05:30.000000001", null},

                {123456, 6, 30, 11, 5, null, null, null, null,                  "+123456-06-30T11:05", null},
        };
    }

    @Test(dataProvider="sample_isoLocalDateTime")
    public void test_print_isoLocalDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_LOCAL_DATE_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_LOCAL_DATE_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoLocalDateTime")
    public void test_parse_isoLocalDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDateTime(year, month, day, hour, min, sec, nano);
            assertParseMatch(TDateTimeFormatter.ISO_LOCAL_DATE_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoOffsetDateTime")
    Object[][] provider_sample_isoOffsetDateTime() {
        return new Object[][]{
                {2008, null, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, 11, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, 30, 11, null, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, 30, null, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, null, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, null, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},

                {2008, 6, 30, 11, 5, null, null, null, null,                    null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, null, null, null,                      null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 500000000, null, null,                 null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 1, null, null,                         null, TDateTimeException.class},

                {2008, 6, 30, 11, 5, null, null, "+01:00", null,                "2008-06-30T11:05+01:00", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", null,                  "2008-06-30T11:05:30+01:00", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", null,             "2008-06-30T11:05:30.5+01:00", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", null,                     "2008-06-30T11:05:30.000000001+01:00", null},

                {2008, 6, 30, 11, 5, null, null, "+01:00", "Europe/Paris",      "2008-06-30T11:05+01:00", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", "Europe/Paris",        "2008-06-30T11:05:30+01:00", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", "Europe/Paris",   "2008-06-30T11:05:30.5+01:00", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", "Europe/Paris",           "2008-06-30T11:05:30.000000001+01:00", null},

                {2008, 6, 30, 11, 5, null, null, null, "Europe/Paris",          null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, null, null, "Europe/Paris",            null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 500000000, null, "Europe/Paris",       null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 1, null, "Europe/Paris",               null, TDateTimeException.class},

                {123456, 6, 30, 11, 5, null, null, "+01:00", null,              "+123456-06-30T11:05+01:00", null},
        };
    }

    @Test(dataProvider="sample_isoOffsetDateTime")
    public void test_print_isoOffsetDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_OFFSET_DATE_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_OFFSET_DATE_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoOffsetDateTime")
    public void test_parse_isoOffsetDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDateTime(year, month, day, hour, min, sec, nano);
            buildCalendrical(expected, offsetId, null);  // zone not expected to be parsed
            assertParseMatch(TDateTimeFormatter.ISO_OFFSET_DATE_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoZonedDateTime")
    Object[][] provider_sample_isoZonedDateTime() {
        return new Object[][]{
                {2008, null, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, 11, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, 30, 11, null, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, 30, null, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, null, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, null, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},

                {2008, 6, 30, 11, 5, null, null, null, null,                    null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, null, null, null,                      null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 500000000, null, null,                 null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 1, null, null,                         null, TDateTimeException.class},

                // allow TOffsetDateTime (no harm comes of this AFAICT)
                {2008, 6, 30, 11, 5, null, null, "+01:00", null,                "2008-06-30T11:05+01:00", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", null,                  "2008-06-30T11:05:30+01:00", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", null,             "2008-06-30T11:05:30.5+01:00", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", null,                     "2008-06-30T11:05:30.000000001+01:00", null},

                // TZonedDateTime with TZoneId of TZoneOffset
                {2008, 6, 30, 11, 5, null, null, "+01:00", "+01:00",            "2008-06-30T11:05+01:00", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", "+01:00",              "2008-06-30T11:05:30+01:00", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", "+01:00",         "2008-06-30T11:05:30.5+01:00", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", "+01:00",                 "2008-06-30T11:05:30.000000001+01:00", null},

                // TZonedDateTime with TZoneId of TZoneRegion
                {2008, 6, 30, 11, 5, null, null, "+01:00", "Europe/Paris",      "2008-06-30T11:05+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", "Europe/Paris",        "2008-06-30T11:05:30+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", "Europe/Paris",   "2008-06-30T11:05:30.5+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", "Europe/Paris",           "2008-06-30T11:05:30.000000001+01:00[Europe/Paris]", null},

                // offset required
                {2008, 6, 30, 11, 5, null, null, null, "Europe/Paris",          null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, null, null, "Europe/Paris",            null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 500000000, null, "Europe/Paris",       null, TDateTimeException.class},
                {2008, 6, 30, 11, 5, 30, 1, null, "Europe/Paris",               null, TDateTimeException.class},

                {123456, 6, 30, 11, 5, null, null, "+01:00", "Europe/Paris",    "+123456-06-30T11:05+01:00[Europe/Paris]", null},
        };
    }

    @Test(dataProvider="sample_isoZonedDateTime")
    public void test_print_isoZonedDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_ZONED_DATE_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_ZONED_DATE_TIME.format(test);
                fail(test.toString());
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoZonedDateTime")
    public void test_parse_isoZonedDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDateTime(year, month, day, hour, min, sec, nano);
            if (offsetId.equals(zoneId)) {
                buildCalendrical(expected, offsetId, null);
            } else {
                buildCalendrical(expected, offsetId, zoneId);
            }
            assertParseMatch(TDateTimeFormatter.ISO_ZONED_DATE_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="sample_isoDateTime")
    Object[][] provider_sample_isoDateTime() {
        return new Object[][]{
                {2008, null, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, 6, null, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, 30, null, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, 11, null, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, 5, null, null, null, null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, "+01:00", null, null, TDateTimeException.class},
                {null, null, null, null, null, null, null, null, "Europe/Paris", null, TDateTimeException.class},
                {2008, 6, 30, 11, null, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, 30, null, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, 6, null, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {2008, null, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},
                {null, 6, 30, 11, 5, null, null, null, null, null, TDateTimeException.class},

                {2008, 6, 30, 11, 5, null, null, null, null,                    "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, null, null,                      "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, null, null,                 "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, null, null,                         "2008-06-30T11:05:30.000000001", null},

                {2008, 6, 30, 11, 5, null, null, "+01:00", null,                "2008-06-30T11:05+01:00", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", null,                  "2008-06-30T11:05:30+01:00", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", null,             "2008-06-30T11:05:30.5+01:00", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", null,                     "2008-06-30T11:05:30.000000001+01:00", null},

                {2008, 6, 30, 11, 5, null, null, "+01:00", "Europe/Paris",      "2008-06-30T11:05+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, null, "+01:00", "Europe/Paris",        "2008-06-30T11:05:30+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, 500000000, "+01:00", "Europe/Paris",   "2008-06-30T11:05:30.5+01:00[Europe/Paris]", null},
                {2008, 6, 30, 11, 5, 30, 1, "+01:00", "Europe/Paris",           "2008-06-30T11:05:30.000000001+01:00[Europe/Paris]", null},

                {2008, 6, 30, 11, 5, null, null, null, "Europe/Paris",          "2008-06-30T11:05", null},
                {2008, 6, 30, 11, 5, 30, null, null, "Europe/Paris",            "2008-06-30T11:05:30", null},
                {2008, 6, 30, 11, 5, 30, 500000000, null, "Europe/Paris",       "2008-06-30T11:05:30.5", null},
                {2008, 6, 30, 11, 5, 30, 1, null, "Europe/Paris",               "2008-06-30T11:05:30.000000001", null},

                {123456, 6, 30, 11, 5, null, null, null, null,                  "+123456-06-30T11:05", null},
        };
    }

    @Test(dataProvider="sample_isoDateTime")
    public void test_print_isoDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String expected, Class<?> expectedEx) {
        TTemporalAccessor test = buildAccessor(year, month, day, hour, min, sec, nano, offsetId, zoneId);
        if (expectedEx == null) {
            assertEquals(TDateTimeFormatter.ISO_DATE_TIME.format(test), expected);
        } else {
            try {
                TDateTimeFormatter.ISO_DATE_TIME.format(test);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    @Test(dataProvider="sample_isoDateTime")
    public void test_parse_isoDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano, String offsetId, String zoneId,
            String input, Class<?> invalid) {
        if (input != null) {
            Expected expected = createDateTime(year, month, day, hour, min, sec, nano);
            if (offsetId != null) {
                expected.fieldValues.put(OFFSET_SECONDS, (long) TZoneOffset.of(offsetId).getTotalSeconds());
                if (zoneId != null) {
                    expected.zone = TZoneId.of(zoneId);
                }
            }
            assertParseMatch(TDateTimeFormatter.ISO_DATE_TIME.parseUnresolved(input, new ParsePosition(0)), expected);
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_print_isoOrdinalDate() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), null, null);
        assertEquals(TDateTimeFormatter.ISO_ORDINAL_DATE.format(test), "2008-155");
    }

    @Test
    public void test_print_isoOrdinalDate_offset() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "Z", null);
        assertEquals(TDateTimeFormatter.ISO_ORDINAL_DATE.format(test), "2008-155Z");
    }

    @Test
    public void test_print_isoOrdinalDate_zoned() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "+02:00", "Europe/Paris");
        assertEquals(TDateTimeFormatter.ISO_ORDINAL_DATE.format(test), "2008-155+02:00");
    }

    @Test
    public void test_print_isoOrdinalDate_zoned_largeYear() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(123456, 6, 3, 11, 5, 30), "Z", null);
        assertEquals(TDateTimeFormatter.ISO_ORDINAL_DATE.format(test), "+123456-155Z");
    }

    @Test
    public void test_print_isoOrdinalDate_fields() {
        TTemporalAccessor test = new TDefaultInterfaceTemporalAccessor() {
            @Override
            public boolean isSupported(TTemporalField field) {
                return field == YEAR || field == DAY_OF_YEAR;
            }
            @Override
            public long getLong(TTemporalField field) {
                if (field == YEAR) {
                    return 2008;
                }
                if (field == DAY_OF_YEAR) {
                    return 231;
                }
                throw new TDateTimeException("Unsupported");
            }
        };
        assertEquals(TDateTimeFormatter.ISO_ORDINAL_DATE.format(test), "2008-231");
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_isoOrdinalDate_missingField() {
        TTemporalAccessor test = TYear.of(2008);
        TDateTimeFormatter.ISO_ORDINAL_DATE.format(test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_isoOrdinalDate() {
        Expected expected = new Expected(YEAR, 2008, DAY_OF_YEAR, 123);
        assertParseMatch(TDateTimeFormatter.ISO_ORDINAL_DATE.parseUnresolved("2008-123", new ParsePosition(0)), expected);
    }

    @Test
    public void test_parse_isoOrdinalDate_largeYear() {
        Expected expected = new Expected(YEAR, 123456, DAY_OF_YEAR, 123);
        assertParseMatch(TDateTimeFormatter.ISO_ORDINAL_DATE.parseUnresolved("+123456-123", new ParsePosition(0)), expected);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_print_basicIsoDate() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), null, null);
        assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.format(test), "20080603");
    }

    @Test
    public void test_print_basicIsoDate_offset() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "Z", null);
        assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.format(test), "20080603Z");
    }

    @Test
    public void test_print_basicIsoDate_zoned() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "+02:00", "Europe/Paris");
        assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.format(test), "20080603+0200");
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_basicIsoDate_largeYear() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(123456, 6, 3, 11, 5, 30), "Z", null);
        TDateTimeFormatter.BASIC_ISO_DATE.format(test);
    }

    @Test
    public void test_print_basicIsoDate_fields() {
        TTemporalAccessor test = buildAccessor(TLocalDate.of(2008, 6, 3), null, null);
        assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.format(test), "20080603");
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_basicIsoDate_missingField() {
        TTemporalAccessor test = TYearMonth.of(2008, 6);
        TDateTimeFormatter.BASIC_ISO_DATE.format(test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_basicIsoDate() {
        TLocalDate expected = TLocalDate.of(2008, 6, 3);
        assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.parse("20080603", TLocalDate.FROM), expected);
    }

    @Test(expectedExceptions=TDateTimeParseException.class)
    public void test_parse_basicIsoDate_largeYear() {
        try {
            TLocalDate expected = TLocalDate.of(123456, 6, 3);
            assertEquals(TDateTimeFormatter.BASIC_ISO_DATE.parse("+1234560603", TLocalDate.FROM), expected);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getErrorIndex(), 0);
            assertEquals(ex.getParsedString(), "+1234560603");
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="weekDate")
    Iterator<Object[]> weekDate() {
        return new Iterator<Object[]>() {
            private TZonedDateTime date = TZonedDateTime.of(TLocalDateTime.of(2003, 12, 29, 11, 5, 30), TZoneId.of("Europe/Paris"));
            private TZonedDateTime endDate = date.withYear(2005).withMonth(1).withDayOfMonth(2);
            private int week = 1;
            private int day = 1;

            public boolean hasNext() {
                return !date.isAfter(endDate);
            }
            public Object[] next() {
                StringBuilder sb = new StringBuilder("2004-W");
                if (week < 10) {
                    sb.append('0');
                }
                sb.append(week).append('-').append(day).append(date.getOffset());
                Object[] ret = new Object[] {date, sb.toString()};
                date = date.plusDays(1);
                day += 1;
                if (day == 8) {
                    day = 1;
                    week++;
                }
                return ret;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test(dataProvider="weekDate")
    public void test_print_isoWeekDate(TTemporalAccessor test, String expected) {
        assertEquals(TDateTimeFormatter.ISO_WEEK_DATE.format(test), expected);
    }

    @Test
    public void test_print_isoWeekDate_zoned_largeYear() {
        TTemporalAccessor test = buildAccessor(TLocalDateTime.of(123456, 6, 3, 11, 5, 30), "Z", null);
        assertEquals(TDateTimeFormatter.ISO_WEEK_DATE.format(test), "+123456-W23-2Z");
    }

    @Test
    public void test_print_isoWeekDate_fields() {
        TTemporalAccessor test = buildAccessor(TLocalDate.of(2004, 1, 27), null, null);
        assertEquals(TDateTimeFormatter.ISO_WEEK_DATE.format(test), "2004-W05-2");
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_isoWeekDate_missingField() {
        TTemporalAccessor test = TYearMonth.of(2008, 6);
        TDateTimeFormatter.ISO_WEEK_DATE.format(test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_weekDate() {
        TLocalDate expected = TLocalDate.of(2004, 1, 28);
        assertEquals(TDateTimeFormatter.ISO_WEEK_DATE.parse("2004-W05-3", TLocalDate.FROM), expected);
    }

    @Test
    public void test_parse_weekDate_largeYear() {
        TTemporalAccessor parsed = TDateTimeFormatter.ISO_WEEK_DATE.parseUnresolved("+123456-W04-5", new ParsePosition(0));
        assertEquals(parsed.get(TIsoFields.WEEK_BASED_YEAR), 123456);
        assertEquals(parsed.get(TIsoFields.WEEK_OF_WEEK_BASED_YEAR), 4);
        assertEquals(parsed.get(DAY_OF_WEEK), 5);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="rfc")
    Object[][] data_rfc() {
        return new Object[][] {
            {TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "Z", "Tue, 3 Jun 2008 11:05:30 GMT"},
            {TLocalDateTime.of(2008, 6, 30, 11, 5, 30), "Z", "Mon, 30 Jun 2008 11:05:30 GMT"},
            {TLocalDateTime.of(2008, 6, 3, 11, 5, 30), "+02:00", "Tue, 3 Jun 2008 11:05:30 +0200"},
            {TLocalDateTime.of(2008, 6, 30, 11, 5, 30), "-03:00", "Mon, 30 Jun 2008 11:05:30 -0300"},
        };
    }

    @Test(dataProvider="rfc")
    public void test_print_rfc1123(TLocalDateTime base, String offsetId, String expected) {
        TTemporalAccessor test = buildAccessor(base, offsetId, null);
        assertEquals(TDateTimeFormatter.RFC_1123_DATE_TIME.format(test), expected);
    }

    @Test(dataProvider="rfc")
    public void test_print_rfc1123_french(TLocalDateTime base, String offsetId, String expected) {
        TTemporalAccessor test = buildAccessor(base, offsetId, null);
        assertEquals(TDateTimeFormatter.RFC_1123_DATE_TIME.withLocale(TLocale.FRENCH).format(test), expected);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_rfc1123_missingField() {
        TTemporalAccessor test = TYearMonth.of(2008, 6);
        TDateTimeFormatter.RFC_1123_DATE_TIME.format(test);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private Expected createDate(Integer year, Integer month, Integer day) {
        Expected test = new Expected();
        if (year != null) {
            test.fieldValues.put(YEAR, (long) year);
        }
        if (month != null) {
            test.fieldValues.put(MONTH_OF_YEAR, (long) month);
        }
        if (day != null) {
            test.fieldValues.put(DAY_OF_MONTH, (long) day);
        }
        return test;
    }

    private Expected createTime(Integer hour, Integer min, Integer sec, Integer nano) {
        Expected test = new Expected();
        if (hour != null) {
            test.fieldValues.put(HOUR_OF_DAY, (long) hour);
        }
        if (min != null) {
            test.fieldValues.put(MINUTE_OF_HOUR, (long) min);
        }
        if (sec != null) {
            test.fieldValues.put(SECOND_OF_MINUTE, (long) sec);
        }
        if (nano != null) {
            test.fieldValues.put(NANO_OF_SECOND, (long) nano);
        }
        return test;
    }

    private Expected createDateTime(
            Integer year, Integer month, Integer day,
            Integer hour, Integer min, Integer sec, Integer nano) {
        Expected test = new Expected();
        if (year != null) {
            test.fieldValues.put(YEAR, (long) year);
        }
        if (month != null) {
            test.fieldValues.put(MONTH_OF_YEAR, (long) month);
        }
        if (day != null) {
            test.fieldValues.put(DAY_OF_MONTH, (long) day);
        }
        if (hour != null) {
            test.fieldValues.put(HOUR_OF_DAY, (long) hour);
        }
        if (min != null) {
            test.fieldValues.put(MINUTE_OF_HOUR, (long) min);
        }
        if (sec != null) {
            test.fieldValues.put(SECOND_OF_MINUTE, (long) sec);
        }
        if (nano != null) {
            test.fieldValues.put(NANO_OF_SECOND, (long) nano);
        }
        return test;
    }

    private TTemporalAccessor buildAccessor(
                    Integer year, Integer month, Integer day,
                    Integer hour, Integer min, Integer sec, Integer nano,
                    String offsetId, String zoneId) {
        MockAccessor mock = new MockAccessor();
        if (year != null) {
            mock.fields.put(YEAR, (long) year);
        }
        if (month != null) {
            mock.fields.put(MONTH_OF_YEAR, (long) month);
        }
        if (day != null) {
            mock.fields.put(DAY_OF_MONTH, (long) day);
        }
        if (hour != null) {
            mock.fields.put(HOUR_OF_DAY, (long) hour);
        }
        if (min != null) {
            mock.fields.put(MINUTE_OF_HOUR, (long) min);
        }
        if (sec != null) {
            mock.fields.put(SECOND_OF_MINUTE, (long) sec);
        }
        if (nano != null) {
            mock.fields.put(NANO_OF_SECOND, (long) nano);
        }
        mock.setOffset(offsetId);
        mock.setZone(zoneId);
        return mock;
    }

    private TTemporalAccessor buildAccessor(TLocalDateTime base, String offsetId, String zoneId) {
        MockAccessor mock = new MockAccessor();
        mock.setFields(base);
        mock.setOffset(offsetId);
        mock.setZone(zoneId);
        return mock;
    }

    private TTemporalAccessor buildAccessor(TLocalDate base, String offsetId, String zoneId) {
        MockAccessor mock = new MockAccessor();
        mock.setFields(base);
        mock.setOffset(offsetId);
        mock.setZone(zoneId);
        return mock;
    }

    private void buildCalendrical(Expected expected, String offsetId, String zoneId) {
        if (offsetId != null) {
            expected.add(TZoneOffset.of(offsetId));
        }
        if (zoneId != null) {
            expected.zone = TZoneId.of(zoneId);
        }
    }

    private void assertParseMatch(TTemporalAccessor parsed, Expected expected) {
        for (TTemporalField field : expected.fieldValues.keySet()) {
            assertEquals(parsed.isSupported(field), true);
            parsed.getLong(field);
        }
        assertEquals(parsed.query(TTemporalQueries.chronology()), expected.chrono);
        assertEquals(parsed.query(TTemporalQueries.zoneId()), expected.zone);
    }

    //-------------------------------------------------------------------------
    static class MockAccessor extends TDefaultInterfaceTemporalAccessor {
        Map<TTemporalField, Long> fields = new HashMap<TTemporalField, Long>();
        TZoneId zoneId;

        void setFields(TLocalDate dt) {
            if (dt != null) {
                fields.put(YEAR, (long) dt.getYear());
                fields.put(MONTH_OF_YEAR, (long) dt.getMonthValue());
                fields.put(DAY_OF_MONTH, (long) dt.getDayOfMonth());
                fields.put(DAY_OF_YEAR, (long) dt.getDayOfYear());
                fields.put(DAY_OF_WEEK, (long) dt.getDayOfWeek().getValue());
                fields.put(TIsoFields.WEEK_BASED_YEAR, dt.getLong(TIsoFields.WEEK_BASED_YEAR));
                fields.put(TIsoFields.WEEK_OF_WEEK_BASED_YEAR, dt.getLong(TIsoFields.WEEK_OF_WEEK_BASED_YEAR));
            }
        }

        void setFields(TLocalDateTime dt) {
            if (dt != null) {
                fields.put(YEAR, (long) dt.getYear());
                fields.put(MONTH_OF_YEAR, (long) dt.getMonthValue());
                fields.put(DAY_OF_MONTH, (long) dt.getDayOfMonth());
                fields.put(DAY_OF_YEAR, (long) dt.getDayOfYear());
                fields.put(DAY_OF_WEEK, (long) dt.getDayOfWeek().getValue());
                fields.put(TIsoFields.WEEK_BASED_YEAR, dt.getLong(TIsoFields.WEEK_BASED_YEAR));
                fields.put(TIsoFields.WEEK_OF_WEEK_BASED_YEAR, dt.getLong(TIsoFields.WEEK_OF_WEEK_BASED_YEAR));
                fields.put(HOUR_OF_DAY, (long) dt.getHour());
                fields.put(MINUTE_OF_HOUR, (long) dt.getMinute());
                fields.put(SECOND_OF_MINUTE, (long) dt.getSecond());
                fields.put(NANO_OF_SECOND, (long) dt.getNano());
            }
        }

        void setOffset(String offsetId) {
            if (offsetId != null) {
                this.fields.put(OFFSET_SECONDS, (long) TZoneOffset.of(offsetId).getTotalSeconds());
            }
        }

        void setZone(String zoneId) {
            if (zoneId != null) {
                this.zoneId = TZoneId.of(zoneId);
            }
        }

        @Override
        public boolean isSupported(TTemporalField field) {
            return fields.containsKey(field);
        }

        @Override
        public long getLong(TTemporalField field) {
            try {
                return fields.get(field);
            } catch (NullPointerException ex) {
                throw new TDateTimeException("Field missing: " + field);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R query(TTemporalQuery<R> query) {
            if (query == TTemporalQueries.zoneId()) {
                return (R) zoneId;
            }
            return super.query(query);
        }

        @Override
        public String toString() {
            return fields + (zoneId != null ? " " + zoneId : "");
        }
    }

    //-----------------------------------------------------------------------
    static class Expected {
        Map<TTemporalField, Long> fieldValues = new HashMap<TTemporalField, Long>();
        TZoneId zone;
        TChronology chrono;

        Expected() {
        }

        Expected(TTemporalField field1, long value1, TTemporalField field2, long value2) {
            fieldValues.put(field1, value1);
            fieldValues.put(field2, value2);
        }

        void add(TZoneOffset offset) {
            fieldValues.put(OFFSET_SECONDS, (long) offset.getTotalSeconds());
        }
    }

}
