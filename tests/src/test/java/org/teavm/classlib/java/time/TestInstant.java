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

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoUnit.DAYS;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test Instant.
 */
@TeaVMTest
public class TestInstant extends AbstractDateTimeTest {

    private static final long MIN_SECOND = Instant.MIN.getEpochSecond();
    private static final long MAX_SECOND = Instant.MAX.getEpochSecond();

    private final Instant test12345x123456789 = Instant.ofEpochSecond(12345, 123456789);

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test12345x123456789, Instant.MIN, Instant.MAX, Instant.EPOCH };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            NANO_OF_SECOND,
            MICRO_OF_SECOND,
            MILLI_OF_SECOND,
            INSTANT_SECONDS,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<TemporalField>(Arrays.<TemporalField>asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    private void check(Instant instant, long epochSecs, int nos) {
        assertEquals(epochSecs, instant.getEpochSecond());
        assertEquals(nos, instant.getNano());
        assertEquals(instant, instant);
        assertEquals(instant.hashCode(), instant.hashCode());
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_EPOCH() {
        check(Instant.EPOCH, 0, 0);
    }

    @Test
    public void constant_MIN() {
        check(Instant.MIN, -31557014167219200L, 0);
    }

    @Test
    public void constant_MAX() {
        check(Instant.MAX, 31556889864403199L, 999999999);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        Instant expected = Instant.now(Clock.systemUTC());
        Instant test = Instant.now();
        long diff = Math.abs(test.toEpochMilli() - expected.toEpochMilli());
        assertTrue(diff < 100);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> Instant.now(null));
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant expected = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(expected, ZoneOffset.UTC);
            Instant test = Instant.now(clock);
            assertEquals(expected, test);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant expected = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(expected, ZoneOffset.UTC);
            Instant test = Instant.now(clock);
            assertEquals(expected, test);
        }
    }

    //-----------------------------------------------------------------------
    // ofEpochSecond(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_seconds_long() {
        for (long i = -2; i <= 2; i++) {
            Instant t = Instant.ofEpochSecond(i);
            assertEquals(i, t.getEpochSecond());
            assertEquals(0, t.getNano());
        }
    }

    //-----------------------------------------------------------------------
    // ofEpochSecond(long,long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_seconds_long_long() {
        for (long i = -2; i <= 2; i++) {
            for (int j = 0; j < 10; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(i, t.getEpochSecond());
                assertEquals(j, t.getNano());
            }
            for (int j = -10; j < 0; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(i - 1, t.getEpochSecond());
                assertEquals(j + 1000000000, t.getNano());
            }
            for (int j = 999999990; j < 1000000000; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(i, t.getEpochSecond());
                assertEquals(j, t.getNano());
            }
        }
    }

    @Test
    public void factory_seconds_long_long_nanosNegativeAdjusted() {
        Instant test = Instant.ofEpochSecond(2L, -1);
        assertEquals(1, test.getEpochSecond());
        assertEquals(999999999, test.getNano());
    }

    @Test
    public void factory_seconds_long_long_tooBig() {
        assertThrows(DateTimeException.class, () -> Instant.ofEpochSecond(MAX_SECOND, 1000000000));
    }

    @Test
    public void factory_seconds_long_long_tooBigBig() {
        assertThrows(ArithmeticException.class, () -> Instant.ofEpochSecond(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    //-----------------------------------------------------------------------
    // ofEpochMilli(long)
    //-----------------------------------------------------------------------
    static Object[][] provider_factory_millis_long() {
        return new Object[][] {
                {0, 0, 0, 0},
                {0, 999999, 0, 999999},
                {1, 0, 0, 1000000},
                {1, 1, 0, 1000001},
                {2, 0, 0, 2000000},
                {999, 0, 0, 999000000},
                {1000, 0, 1, 0},
                {1001, 0, 1, 1000000},
                {-1, 1, -1, 999000001},
                {-1, 0, -1, 999000000},
                {-2, 999999, -1, 998999999},
                {-2, 0, -1, 998000000},
                {-999, 0, -1, 1000000},
                {-1000, 0, -1, 0},
                {-1001, 0, -2, 999000000},
                {Long.MAX_VALUE, 0, Long.MAX_VALUE / 1000, (int) (Long.MAX_VALUE % 1000) * 1000000},
                {Long.MAX_VALUE - 1, 0, (Long.MAX_VALUE - 1) / 1000, (int) ((Long.MAX_VALUE - 1) % 1000) * 1000000},
                {Long.MIN_VALUE, 0, (Long.MIN_VALUE / 1000) - 1, (int) (Long.MIN_VALUE % 1000) * 1000000 + 1000000000},
                {Long.MIN_VALUE, 1, (Long.MIN_VALUE / 1000) - 1, (int) (Long.MIN_VALUE % 1000) * 1000000
                        + 1000000000 + 1},
                {Long.MIN_VALUE + 1, 0, ((Long.MIN_VALUE + 1) / 1000) - 1, (int) ((Long.MIN_VALUE + 1) % 1000)
                        * 1000000 + 1000000000},
                {Long.MIN_VALUE + 1, 1, ((Long.MIN_VALUE + 1) / 1000) - 1, (int) ((Long.MIN_VALUE + 1) % 1000)
                        * 1000000 + 1000000000 + 1},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_factory_millis_long")
    public void factory_millis_long(long millis, int nanos, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochMilli(millis).plusNanos(nanos);
        assertEquals(expectedSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
        assertEquals(millis, t.toEpochMilli());
    }

    //-----------------------------------------------------------------------
    // parse(String)
    //-----------------------------------------------------------------------
    // see also parse tests under toString()
    static Object[][] provider_factory_parse() {
        return new Object[][] {
                {"1970-01-01T00:00:00Z", 0, 0},
                {"1970-01-01t00:00:00Z", 0, 0},
                {"1970-01-01T00:00:00z", 0, 0},
                {"1970-01-01T00:00:00.0Z", 0, 0},
                {"1970-01-01T00:00:00.000000000Z", 0, 0},

                {"1970-01-01T00:00:00.000000001Z", 0, 1},
                {"1970-01-01T00:00:00.100000000Z", 0, 100000000},
                {"1970-01-01T00:00:01Z", 1, 0},
                {"1970-01-01T00:01:00Z", 60, 0},
                {"1970-01-01T00:01:01Z", 61, 0},
                {"1970-01-01T00:01:01.000000001Z", 61, 1},
                {"1970-01-01T01:00:00.000000000Z", 3600, 0},
                {"1970-01-01T01:01:01.000000001Z", 3661, 1},
                {"1970-01-02T01:01:01.100000000Z", 90061, 100000000},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_factory_parse")
    public void factory_parse(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.parse(text);
        assertEquals(expectedEpochSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }

    @ParameterizedTest
    @MethodSource("provider_factory_parse")
    public void factory_parseLowercase(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.parse(text.toLowerCase(Locale.ENGLISH));
        assertEquals(expectedEpochSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }

// TODO: should comma be accepted?
//    @Test(dataProvider = "Parse")
//    public void factory_parse_comma(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
//        text = text.replace('.', ',');
//        Instant t = Instant.parse(text);
//        assertEquals(t.getEpochSecond(), expectedEpochSeconds);
//        assertEquals(t.getNano(), expectedNanoOfSecond);
//    }

    static Object[][] provider_factory_parseFailures() {
        return new Object[][] {
                {""},
                {"Z"},
                {"1970-01-01T00:00:00"},
                {"1970-01-01T00:00:0Z"},
                {"1970-01-01T00:00:00.0000000000Z"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_factory_parseFailures")
    public void factory_parseFailures(String text) {
        assertThrows(DateTimeParseException.class, () -> Instant.parse(text));
    }

    @ParameterizedTest
    @MethodSource("provider_factory_parseFailures")
    public void factory_parseFailures_comma(String text) {
        var textWithComma = text.replace('.', ',');
        assertThrows(DateTimeParseException.class, () -> Instant.parse(textWithComma));
    }

    @Test
    public void factory_parse_nullText() {
        assertThrows(NullPointerException.class, () -> Instant.parse(null));
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        Instant test = test12345x123456789;
        assertEquals(123456789, test.get(ChronoField.NANO_OF_SECOND));
        assertEquals(123456, test.get(ChronoField.MICRO_OF_SECOND));
        assertEquals(123, test.get(ChronoField.MILLI_OF_SECOND));
    }

    @Test
    public void test_getLong_TemporalField() {
        Instant test = test12345x123456789;
        assertEquals(123456789, test.getLong(ChronoField.NANO_OF_SECOND));
        assertEquals(123456, test.getLong(ChronoField.MICRO_OF_SECOND));
        assertEquals(123, test.getLong(ChronoField.MILLI_OF_SECOND));
        assertEquals(12345, test.getLong(ChronoField.INSTANT_SECONDS));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(null, test12345x123456789.query(TemporalQueries.chronology()));
        assertEquals(null, test12345x123456789.query(TemporalQueries.localDate()));
        assertEquals(null, test12345x123456789.query(TemporalQueries.localTime()));
        assertEquals(null, test12345x123456789.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.NANOS, test12345x123456789.query(TemporalQueries.precision()));
        assertEquals(null, test12345x123456789.query(TemporalQueries.zone()));
        assertEquals(null, test12345x123456789.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test12345x123456789.query(null));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_plus() {
        return new Object[][] {
                {MIN_SECOND, 0, -MIN_SECOND, 0, 0, 0},

                {MIN_SECOND, 0, 1, 0, MIN_SECOND + 1, 0},
                {MIN_SECOND, 0, 0, 500, MIN_SECOND, 500},
                {MIN_SECOND, 0, 0, 1000000000, MIN_SECOND + 1, 0},

                {MIN_SECOND + 1, 0, -1, 0, MIN_SECOND, 0},
                {MIN_SECOND + 1, 0, 0, -500, MIN_SECOND, 999999500},
                {MIN_SECOND + 1, 0, 0, -1000000000, MIN_SECOND, 0},

                {-4, 666666667, -4, 666666667, -7, 333333334},
                {-4, 666666667, -3,         0, -7, 666666667},
                {-4, 666666667, -2,         0, -6, 666666667},
                {-4, 666666667, -1,         0, -5, 666666667},
                {-4, 666666667, -1, 333333334, -4,         1},
                {-4, 666666667, -1, 666666667, -4, 333333334},
                {-4, 666666667, -1, 999999999, -4, 666666666},
                {-4, 666666667,  0,         0, -4, 666666667},
                {-4, 666666667,  0,         1, -4, 666666668},
                {-4, 666666667,  0, 333333333, -3,         0},
                {-4, 666666667,  0, 666666666, -3, 333333333},
                {-4, 666666667,  1,         0, -3, 666666667},
                {-4, 666666667,  2,         0, -2, 666666667},
                {-4, 666666667,  3,         0, -1, 666666667},
                {-4, 666666667,  3, 333333333,  0,         0},

                {-3, 0, -4, 666666667, -7, 666666667},
                {-3, 0, -3,         0, -6,         0},
                {-3, 0, -2,         0, -5,         0},
                {-3, 0, -1,         0, -4,         0},
                {-3, 0, -1, 333333334, -4, 333333334},
                {-3, 0, -1, 666666667, -4, 666666667},
                {-3, 0, -1, 999999999, -4, 999999999},
                {-3, 0,  0,         0, -3,         0},
                {-3, 0,  0,         1, -3,         1},
                {-3, 0,  0, 333333333, -3, 333333333},
                {-3, 0,  0, 666666666, -3, 666666666},
                {-3, 0,  1,         0, -2,         0},
                {-3, 0,  2,         0, -1,         0},
                {-3, 0,  3,         0,  0,         0},
                {-3, 0,  3, 333333333,  0, 333333333},

                {-2, 0, -4, 666666667, -6, 666666667},
                {-2, 0, -3,         0, -5,         0},
                {-2, 0, -2,         0, -4,         0},
                {-2, 0, -1,         0, -3,         0},
                {-2, 0, -1, 333333334, -3, 333333334},
                {-2, 0, -1, 666666667, -3, 666666667},
                {-2, 0, -1, 999999999, -3, 999999999},
                {-2, 0,  0,         0, -2,         0},
                {-2, 0,  0,         1, -2,         1},
                {-2, 0,  0, 333333333, -2, 333333333},
                {-2, 0,  0, 666666666, -2, 666666666},
                {-2, 0,  1,         0, -1,         0},
                {-2, 0,  2,         0,  0,         0},
                {-2, 0,  3,         0,  1,         0},
                {-2, 0,  3, 333333333,  1, 333333333},

                {-1, 0, -4, 666666667, -5, 666666667},
                {-1, 0, -3,         0, -4,         0},
                {-1, 0, -2,         0, -3,         0},
                {-1, 0, -1,         0, -2,         0},
                {-1, 0, -1, 333333334, -2, 333333334},
                {-1, 0, -1, 666666667, -2, 666666667},
                {-1, 0, -1, 999999999, -2, 999999999},
                {-1, 0,  0,         0, -1,         0},
                {-1, 0,  0,         1, -1,         1},
                {-1, 0,  0, 333333333, -1, 333333333},
                {-1, 0,  0, 666666666, -1, 666666666},
                {-1, 0,  1,         0,  0,         0},
                {-1, 0,  2,         0,  1,         0},
                {-1, 0,  3,         0,  2,         0},
                {-1, 0,  3, 333333333,  2, 333333333},

                {-1, 666666667, -4, 666666667, -4, 333333334},
                {-1, 666666667, -3,         0, -4, 666666667},
                {-1, 666666667, -2,         0, -3, 666666667},
                {-1, 666666667, -1,         0, -2, 666666667},
                {-1, 666666667, -1, 333333334, -1,         1},
                {-1, 666666667, -1, 666666667, -1, 333333334},
                {-1, 666666667, -1, 999999999, -1, 666666666},
                {-1, 666666667,  0,         0, -1, 666666667},
                {-1, 666666667,  0,         1, -1, 666666668},
                {-1, 666666667,  0, 333333333,  0,         0},
                {-1, 666666667,  0, 666666666,  0, 333333333},
                {-1, 666666667,  1,         0,  0, 666666667},
                {-1, 666666667,  2,         0,  1, 666666667},
                {-1, 666666667,  3,         0,  2, 666666667},
                {-1, 666666667,  3, 333333333,  3,         0},

                {0, 0, -4, 666666667, -4, 666666667},
                {0, 0, -3,         0, -3,         0},
                {0, 0, -2,         0, -2,         0},
                {0, 0, -1,         0, -1,         0},
                {0, 0, -1, 333333334, -1, 333333334},
                {0, 0, -1, 666666667, -1, 666666667},
                {0, 0, -1, 999999999, -1, 999999999},
                {0, 0,  0,         0,  0,         0},
                {0, 0,  0,         1,  0,         1},
                {0, 0,  0, 333333333,  0, 333333333},
                {0, 0,  0, 666666666,  0, 666666666},
                {0, 0,  1,         0,  1,         0},
                {0, 0,  2,         0,  2,         0},
                {0, 0,  3,         0,  3,         0},
                {0, 0,  3, 333333333,  3, 333333333},

                {0, 333333333, -4, 666666667, -3,         0},
                {0, 333333333, -3,         0, -3, 333333333},
                {0, 333333333, -2,         0, -2, 333333333},
                {0, 333333333, -1,         0, -1, 333333333},
                {0, 333333333, -1, 333333334, -1, 666666667},
                {0, 333333333, -1, 666666667,  0,         0},
                {0, 333333333, -1, 999999999,  0, 333333332},
                {0, 333333333,  0,         0,  0, 333333333},
                {0, 333333333,  0,         1,  0, 333333334},
                {0, 333333333,  0, 333333333,  0, 666666666},
                {0, 333333333,  0, 666666666,  0, 999999999},
                {0, 333333333,  1,         0,  1, 333333333},
                {0, 333333333,  2,         0,  2, 333333333},
                {0, 333333333,  3,         0,  3, 333333333},
                {0, 333333333,  3, 333333333,  3, 666666666},

                {1, 0, -4, 666666667, -3, 666666667},
                {1, 0, -3,         0, -2,         0},
                {1, 0, -2,         0, -1,         0},
                {1, 0, -1,         0,  0,         0},
                {1, 0, -1, 333333334,  0, 333333334},
                {1, 0, -1, 666666667,  0, 666666667},
                {1, 0, -1, 999999999,  0, 999999999},
                {1, 0,  0,         0,  1,         0},
                {1, 0,  0,         1,  1,         1},
                {1, 0,  0, 333333333,  1, 333333333},
                {1, 0,  0, 666666666,  1, 666666666},
                {1, 0,  1,         0,  2,         0},
                {1, 0,  2,         0,  3,         0},
                {1, 0,  3,         0,  4,         0},
                {1, 0,  3, 333333333,  4, 333333333},

                {2, 0, -4, 666666667, -2, 666666667},
                {2, 0, -3,         0, -1,         0},
                {2, 0, -2,         0,  0,         0},
                {2, 0, -1,         0,  1,         0},
                {2, 0, -1, 333333334,  1, 333333334},
                {2, 0, -1, 666666667,  1, 666666667},
                {2, 0, -1, 999999999,  1, 999999999},
                {2, 0,  0,         0,  2,         0},
                {2, 0,  0,         1,  2,         1},
                {2, 0,  0, 333333333,  2, 333333333},
                {2, 0,  0, 666666666,  2, 666666666},
                {2, 0,  1,         0,  3,         0},
                {2, 0,  2,         0,  4,         0},
                {2, 0,  3,         0,  5,         0},
                {2, 0,  3, 333333333,  5, 333333333},

                {3, 0, -4, 666666667, -1, 666666667},
                {3, 0, -3,         0,  0,         0},
                {3, 0, -2,         0,  1,         0},
                {3, 0, -1,         0,  2,         0},
                {3, 0, -1, 333333334,  2, 333333334},
                {3, 0, -1, 666666667,  2, 666666667},
                {3, 0, -1, 999999999,  2, 999999999},
                {3, 0,  0,         0,  3,         0},
                {3, 0,  0,         1,  3,         1},
                {3, 0,  0, 333333333,  3, 333333333},
                {3, 0,  0, 666666666,  3, 666666666},
                {3, 0,  1,         0,  4,         0},
                {3, 0,  2,         0,  5,         0},
                {3, 0,  3,         0,  6,         0},
                {3, 0,  3, 333333333,  6, 333333333},

                {3, 333333333, -4, 666666667,  0,         0},
                {3, 333333333, -3,         0,  0, 333333333},
                {3, 333333333, -2,         0,  1, 333333333},
                {3, 333333333, -1,         0,  2, 333333333},
                {3, 333333333, -1, 333333334,  2, 666666667},
                {3, 333333333, -1, 666666667,  3,         0},
                {3, 333333333, -1, 999999999,  3, 333333332},
                {3, 333333333,  0,         0,  3, 333333333},
                {3, 333333333,  0,         1,  3, 333333334},
                {3, 333333333,  0, 333333333,  3, 666666666},
                {3, 333333333,  0, 666666666,  3, 999999999},
                {3, 333333333,  1,         0,  4, 333333333},
                {3, 333333333,  2,         0,  5, 333333333},
                {3, 333333333,  3,         0,  6, 333333333},
                {3, 333333333,  3, 333333333,  6, 666666666},

                {MAX_SECOND - 1, 0, 1, 0, MAX_SECOND, 0},
                {MAX_SECOND - 1, 0, 0, 500, MAX_SECOND - 1, 500},
                {MAX_SECOND - 1, 0, 0, 1000000000, MAX_SECOND, 0},

                {MAX_SECOND, 0, -1, 0, MAX_SECOND - 1, 0},
                {MAX_SECOND, 0, 0, -500, MAX_SECOND - 1, 999999500},
                {MAX_SECOND, 0, 0, -1000000000, MAX_SECOND - 1, 0},

                {MAX_SECOND, 0, -MAX_SECOND, 0, 0, 0},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_plus")
    public void plus_Duration(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos).plus(Duration.ofSeconds(otherSeconds, otherNanos));
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void plus_Duration_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        assertThrows(DateTimeException.class, () -> i.plus(Duration.ofSeconds(0, 1)));
    }

    @Test
    public void plus_Duration_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND);
        assertThrows(DateTimeException.class, () -> i.plus(Duration.ofSeconds(-1, 999999999)));
    }

    //-----------------------------------------------------------------------a
    @ParameterizedTest
    @MethodSource("provider_plus")
    public void plus_longTemporalUnit(long seconds, int nanos, long otherSeconds, int otherNanos,
            long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos).plus(otherSeconds, SECONDS).plus(otherNanos, NANOS);
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void plus_longTemporalUnit_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        assertThrows(DateTimeException.class, () -> i.plus(1, NANOS));
    }

    @Test
    public void plus_longTemporalUnit_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND);
        i.plus(999999999, NANOS);
        assertThrows(DateTimeException.class, () -> i.plus(-1, SECONDS));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_plusSeconds_long() {
        return new Object[][] {
                {0, 0, 0, 0, 0},
                {0, 0, 1, 1, 0},
                {0, 0, -1, -1, 0},
                {0, 0, MAX_SECOND, MAX_SECOND, 0},
                {0, 0, MIN_SECOND, MIN_SECOND, 0},
                {1, 0, 0, 1, 0},
                {1, 0, 1, 2, 0},
                {1, 0, -1, 0, 0},
                {1, 0, MAX_SECOND - 1, MAX_SECOND, 0},
                {1, 0, MIN_SECOND, MIN_SECOND + 1, 0},
                {1, 1, 0, 1, 1},
                {1, 1, 1, 2, 1},
                {1, 1, -1, 0, 1},
                {1, 1, MAX_SECOND - 1, MAX_SECOND, 1},
                {1, 1, MIN_SECOND, MIN_SECOND + 1, 1},
                {-1, 1, 0, -1, 1},
                {-1, 1, 1, 0, 1},
                {-1, 1, -1, -2, 1},
                {-1, 1, MAX_SECOND, MAX_SECOND - 1, 1},
                {-1, 1, MIN_SECOND + 1, MIN_SECOND, 1},

                {MAX_SECOND, 2, -MAX_SECOND, 0, 2},
                {MIN_SECOND, 2, -MIN_SECOND, 0, 2},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_plusSeconds_long")
    public void plusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusSeconds(amount);
        assertEquals(expectedSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }

    @Test
    public void plusSeconds_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(1, 0);
        assertThrows(ArithmeticException.class, () -> t.plusSeconds(Long.MAX_VALUE));
    }

    @Test
    public void plusSeconds_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(-1, 0);
        assertThrows(ArithmeticException.class, () -> t.plusSeconds(Long.MIN_VALUE));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_plusMillis_long() {
        return new Object[][] {
                {0, 0, 0,       0, 0},
                {0, 0, 1,       0, 1000000},
                {0, 0, 999,     0, 999000000},
                {0, 0, 1000,    1, 0},
                {0, 0, 1001,    1, 1000000},
                {0, 0, 1999,    1, 999000000},
                {0, 0, 2000,    2, 0},
                {0, 0, -1,      -1, 999000000},
                {0, 0, -999,    -1, 1000000},
                {0, 0, -1000,   -1, 0},
                {0, 0, -1001,   -2, 999000000},
                {0, 0, -1999,   -2, 1000000},

                {0, 1, 0,       0, 1},
                {0, 1, 1,       0, 1000001},
                {0, 1, 998,     0, 998000001},
                {0, 1, 999,     0, 999000001},
                {0, 1, 1000,    1, 1},
                {0, 1, 1998,    1, 998000001},
                {0, 1, 1999,    1, 999000001},
                {0, 1, 2000,    2, 1},
                {0, 1, -1,      -1, 999000001},
                {0, 1, -2,      -1, 998000001},
                {0, 1, -1000,   -1, 1},
                {0, 1, -1001,   -2, 999000001},

                {0, 1000000, 0,       0, 1000000},
                {0, 1000000, 1,       0, 2000000},
                {0, 1000000, 998,     0, 999000000},
                {0, 1000000, 999,     1, 0},
                {0, 1000000, 1000,    1, 1000000},
                {0, 1000000, 1998,    1, 999000000},
                {0, 1000000, 1999,    2, 0},
                {0, 1000000, 2000,    2, 1000000},
                {0, 1000000, -1,      0, 0},
                {0, 1000000, -2,      -1, 999000000},
                {0, 1000000, -999,    -1, 2000000},
                {0, 1000000, -1000,   -1, 1000000},
                {0, 1000000, -1001,   -1, 0},
                {0, 1000000, -1002,   -2, 999000000},

                {0, 999999999, 0,     0, 999999999},
                {0, 999999999, 1,     1, 999999},
                {0, 999999999, 999,   1, 998999999},
                {0, 999999999, 1000,  1, 999999999},
                {0, 999999999, 1001,  2, 999999},
                {0, 999999999, -1,    0, 998999999},
                {0, 999999999, -1000, -1, 999999999},
                {0, 999999999, -1001, -1, 998999999},

                {0, 0, Long.MAX_VALUE, Long.MAX_VALUE / 1000, (int) (Long.MAX_VALUE % 1000) * 1000000},
                {0, 0, Long.MIN_VALUE, Long.MIN_VALUE / 1000 - 1, (int) (Long.MIN_VALUE % 1000) * 1000000 + 1000000000},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_plusMillis_long")
    public void plusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusMillis(amount);
        assertEquals(expectedSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }
    @ParameterizedTest
    @MethodSource("provider_plusMillis_long")
    public void plusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds + 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(expectedSeconds + 1, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }
    @ParameterizedTest
    @MethodSource("provider_plusMillis_long")
    public void plusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds - 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(expectedSeconds - 1, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }

    @Test
    public void plusMillis_long_max() {
        Instant t = Instant.ofEpochSecond(MAX_SECOND, 998999999);
        t = t.plusMillis(1);
        assertEquals(MAX_SECOND, t.getEpochSecond());
        assertEquals(999999999, t.getNano());
    }

    @Test
    public void plusMillis_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(MAX_SECOND, 999000000);
        assertThrows(DateTimeException.class, () -> t.plusMillis(1));
    }

    @Test
    public void plusMillis_long_min() {
        Instant t = Instant.ofEpochSecond(MIN_SECOND, 1000000);
        t = t.plusMillis(-1);
        assertEquals(MIN_SECOND, t.getEpochSecond());
        assertEquals(0, t.getNano());
    }

    @Test
    public void plusMillis_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(MIN_SECOND, 0);
        assertThrows(DateTimeException.class, () -> t.plusMillis(-1));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_plusNanos_long() {
        return new Object[][] {
                {0, 0, 0,           0, 0},
                {0, 0, 1,           0, 1},
                {0, 0, 999999999,   0, 999999999},
                {0, 0, 1000000000,  1, 0},
                {0, 0, 1000000001,  1, 1},
                {0, 0, 1999999999,  1, 999999999},
                {0, 0, 2000000000,  2, 0},
                {0, 0, -1,          -1, 999999999},
                {0, 0, -999999999,  -1, 1},
                {0, 0, -1000000000, -1, 0},
                {0, 0, -1000000001, -2, 999999999},
                {0, 0, -1999999999, -2, 1},

                {1, 0, 0,           1, 0},
                {1, 0, 1,           1, 1},
                {1, 0, 999999999,   1, 999999999},
                {1, 0, 1000000000,  2, 0},
                {1, 0, 1000000001,  2, 1},
                {1, 0, 1999999999,  2, 999999999},
                {1, 0, 2000000000,  3, 0},
                {1, 0, -1,          0, 999999999},
                {1, 0, -999999999,  0, 1},
                {1, 0, -1000000000, 0, 0},
                {1, 0, -1000000001, -1, 999999999},
                {1, 0, -1999999999, -1, 1},

                {-1, 0, 0,           -1, 0},
                {-1, 0, 1,           -1, 1},
                {-1, 0, 999999999,   -1, 999999999},
                {-1, 0, 1000000000,  0, 0},
                {-1, 0, 1000000001,  0, 1},
                {-1, 0, 1999999999,  0, 999999999},
                {-1, 0, 2000000000,  1, 0},
                {-1, 0, -1,          -2, 999999999},
                {-1, 0, -999999999,  -2, 1},
                {-1, 0, -1000000000, -2, 0},
                {-1, 0, -1000000001, -3, 999999999},
                {-1, 0, -1999999999, -3, 1},

                {1, 1, 0,           1, 1},
                {1, 1, 1,           1, 2},
                {1, 1, 999999998,   1, 999999999},
                {1, 1, 999999999,   2, 0},
                {1, 1, 1000000000,  2, 1},
                {1, 1, 1999999998,  2, 999999999},
                {1, 1, 1999999999,  3, 0},
                {1, 1, 2000000000,  3, 1},
                {1, 1, -1,          1, 0},
                {1, 1, -2,          0, 999999999},
                {1, 1, -1000000000, 0, 1},
                {1, 1, -1000000001, 0, 0},
                {1, 1, -1000000002, -1, 999999999},
                {1, 1, -2000000000, -1, 1},

                {1, 999999999, 0,           1, 999999999},
                {1, 999999999, 1,           2, 0},
                {1, 999999999, 999999999,   2, 999999998},
                {1, 999999999, 1000000000,  2, 999999999},
                {1, 999999999, 1000000001,  3, 0},
                {1, 999999999, -1,          1, 999999998},
                {1, 999999999, -1000000000, 0, 999999999},
                {1, 999999999, -1000000001, 0, 999999998},
                {1, 999999999, -1999999999, 0, 0},
                {1, 999999999, -2000000000, -1, 999999999},

                {MAX_SECOND, 0, 999999999, MAX_SECOND, 999999999},
                {MAX_SECOND - 1, 0, 1999999999, MAX_SECOND, 999999999},
                {MIN_SECOND, 1, -1, MIN_SECOND, 0},
                {MIN_SECOND + 1, 1, -1000000001, MIN_SECOND, 0},

                {0, 0, MAX_SECOND, MAX_SECOND / 1000000000, (int) (MAX_SECOND % 1000000000)},
                {0, 0, MIN_SECOND, MIN_SECOND / 1000000000 - 1, (int) (MIN_SECOND % 1000000000) + 1000000000},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_plusNanos_long")
    public void plusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusNanos(amount);
        assertEquals(expectedSeconds, t.getEpochSecond());
        assertEquals(expectedNanoOfSecond, t.getNano());
    }

    @Test
    public void plusNanos_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        assertThrows(DateTimeException.class, () -> t.plusNanos(1));
    }

    @Test
    public void plusNanos_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(MIN_SECOND, 0);
        assertThrows(DateTimeException.class, () -> t.plusNanos(-1));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_minus() {
        return new Object[][] {
                {MIN_SECOND, 0, MIN_SECOND, 0, 0, 0},

                {MIN_SECOND, 0, -1, 0, MIN_SECOND + 1, 0},
                {MIN_SECOND, 0, 0, -500, MIN_SECOND, 500},
                {MIN_SECOND, 0, 0, -1000000000, MIN_SECOND + 1, 0},

                {MIN_SECOND + 1, 0, 1, 0, MIN_SECOND, 0},
                {MIN_SECOND + 1, 0, 0, 500, MIN_SECOND, 999999500},
                {MIN_SECOND + 1, 0, 0, 1000000000, MIN_SECOND, 0},

                {-4, 666666667, -4, 666666667,  0,         0},
                {-4, 666666667, -3,         0, -1, 666666667},
                {-4, 666666667, -2,         0, -2, 666666667},
                {-4, 666666667, -1,         0, -3, 666666667},
                {-4, 666666667, -1, 333333334, -3, 333333333},
                {-4, 666666667, -1, 666666667, -3,         0},
                {-4, 666666667, -1, 999999999, -4, 666666668},
                {-4, 666666667,  0,         0, -4, 666666667},
                {-4, 666666667,  0,         1, -4, 666666666},
                {-4, 666666667,  0, 333333333, -4, 333333334},
                {-4, 666666667,  0, 666666666, -4,         1},
                {-4, 666666667,  1,         0, -5, 666666667},
                {-4, 666666667,  2,         0, -6, 666666667},
                {-4, 666666667,  3,         0, -7, 666666667},
                {-4, 666666667,  3, 333333333, -7, 333333334},

                {-3, 0, -4, 666666667,  0, 333333333},
                {-3, 0, -3,         0,  0,         0},
                {-3, 0, -2,         0, -1,         0},
                {-3, 0, -1,         0, -2,         0},
                {-3, 0, -1, 333333334, -3, 666666666},
                {-3, 0, -1, 666666667, -3, 333333333},
                {-3, 0, -1, 999999999, -3,         1},
                {-3, 0,  0,         0, -3,         0},
                {-3, 0,  0,         1, -4, 999999999},
                {-3, 0,  0, 333333333, -4, 666666667},
                {-3, 0,  0, 666666666, -4, 333333334},
                {-3, 0,  1,         0, -4,         0},
                {-3, 0,  2,         0, -5,         0},
                {-3, 0,  3,         0, -6,         0},
                {-3, 0,  3, 333333333, -7, 666666667},

                {-2, 0, -4, 666666667,  1, 333333333},
                {-2, 0, -3,         0,  1,         0},
                {-2, 0, -2,         0,  0,         0},
                {-2, 0, -1,         0, -1,         0},
                {-2, 0, -1, 333333334, -2, 666666666},
                {-2, 0, -1, 666666667, -2, 333333333},
                {-2, 0, -1, 999999999, -2,         1},
                {-2, 0,  0,         0, -2,         0},
                {-2, 0,  0,         1, -3, 999999999},
                {-2, 0,  0, 333333333, -3, 666666667},
                {-2, 0,  0, 666666666, -3, 333333334},
                {-2, 0,  1,         0, -3,         0},
                {-2, 0,  2,         0, -4,         0},
                {-2, 0,  3,         0, -5,         0},
                {-2, 0,  3, 333333333, -6, 666666667},

                {-1, 0, -4, 666666667,  2, 333333333},
                {-1, 0, -3,         0,  2,         0},
                {-1, 0, -2,         0,  1,         0},
                {-1, 0, -1,         0,  0,         0},
                {-1, 0, -1, 333333334, -1, 666666666},
                {-1, 0, -1, 666666667, -1, 333333333},
                {-1, 0, -1, 999999999, -1,         1},
                {-1, 0,  0,         0, -1,         0},
                {-1, 0,  0,         1, -2, 999999999},
                {-1, 0,  0, 333333333, -2, 666666667},
                {-1, 0,  0, 666666666, -2, 333333334},
                {-1, 0,  1,         0, -2,         0},
                {-1, 0,  2,         0, -3,         0},
                {-1, 0,  3,         0, -4,         0},
                {-1, 0,  3, 333333333, -5, 666666667},

                {-1, 666666667, -4, 666666667,  3,         0},
                {-1, 666666667, -3,         0,  2, 666666667},
                {-1, 666666667, -2,         0,  1, 666666667},
                {-1, 666666667, -1,         0,  0, 666666667},
                {-1, 666666667, -1, 333333334,  0, 333333333},
                {-1, 666666667, -1, 666666667,  0,         0},
                {-1, 666666667, -1, 999999999, -1, 666666668},
                {-1, 666666667,  0,         0, -1, 666666667},
                {-1, 666666667,  0,         1, -1, 666666666},
                {-1, 666666667,  0, 333333333, -1, 333333334},
                {-1, 666666667,  0, 666666666, -1,         1},
                {-1, 666666667,  1,         0, -2, 666666667},
                {-1, 666666667,  2,         0, -3, 666666667},
                {-1, 666666667,  3,         0, -4, 666666667},
                {-1, 666666667,  3, 333333333, -4, 333333334},

                {0, 0, -4, 666666667,  3, 333333333},
                {0, 0, -3,         0,  3,         0},
                {0, 0, -2,         0,  2,         0},
                {0, 0, -1,         0,  1,         0},
                {0, 0, -1, 333333334,  0, 666666666},
                {0, 0, -1, 666666667,  0, 333333333},
                {0, 0, -1, 999999999,  0,         1},
                {0, 0,  0,         0,  0,         0},
                {0, 0,  0,         1, -1, 999999999},
                {0, 0,  0, 333333333, -1, 666666667},
                {0, 0,  0, 666666666, -1, 333333334},
                {0, 0,  1,         0, -1,         0},
                {0, 0,  2,         0, -2,         0},
                {0, 0,  3,         0, -3,         0},
                {0, 0,  3, 333333333, -4, 666666667},

                {0, 333333333, -4, 666666667,  3, 666666666},
                {0, 333333333, -3,         0,  3, 333333333},
                {0, 333333333, -2,         0,  2, 333333333},
                {0, 333333333, -1,         0,  1, 333333333},
                {0, 333333333, -1, 333333334,  0, 999999999},
                {0, 333333333, -1, 666666667,  0, 666666666},
                {0, 333333333, -1, 999999999,  0, 333333334},
                {0, 333333333,  0,         0,  0, 333333333},
                {0, 333333333,  0,         1,  0, 333333332},
                {0, 333333333,  0, 333333333,  0,         0},
                {0, 333333333,  0, 666666666, -1, 666666667},
                {0, 333333333,  1,         0, -1, 333333333},
                {0, 333333333,  2,         0, -2, 333333333},
                {0, 333333333,  3,         0, -3, 333333333},
                {0, 333333333,  3, 333333333, -3,         0},

                {1, 0, -4, 666666667,  4, 333333333},
                {1, 0, -3,         0,  4,         0},
                {1, 0, -2,         0,  3,         0},
                {1, 0, -1,         0,  2,         0},
                {1, 0, -1, 333333334,  1, 666666666},
                {1, 0, -1, 666666667,  1, 333333333},
                {1, 0, -1, 999999999,  1,         1},
                {1, 0,  0,         0,  1,         0},
                {1, 0,  0,         1,  0, 999999999},
                {1, 0,  0, 333333333,  0, 666666667},
                {1, 0,  0, 666666666,  0, 333333334},
                {1, 0,  1,         0,  0,         0},
                {1, 0,  2,         0, -1,         0},
                {1, 0,  3,         0, -2,         0},
                {1, 0,  3, 333333333, -3, 666666667},

                {2, 0, -4, 666666667,  5, 333333333},
                {2, 0, -3,         0,  5,         0},
                {2, 0, -2,         0,  4,         0},
                {2, 0, -1,         0,  3,         0},
                {2, 0, -1, 333333334,  2, 666666666},
                {2, 0, -1, 666666667,  2, 333333333},
                {2, 0, -1, 999999999,  2,         1},
                {2, 0,  0,         0,  2,         0},
                {2, 0,  0,         1,  1, 999999999},
                {2, 0,  0, 333333333,  1, 666666667},
                {2, 0,  0, 666666666,  1, 333333334},
                {2, 0,  1,         0,  1,         0},
                {2, 0,  2,         0,  0,         0},
                {2, 0,  3,         0, -1,         0},
                {2, 0,  3, 333333333, -2, 666666667},

                {3, 0, -4, 666666667,  6, 333333333},
                {3, 0, -3,         0,  6,         0},
                {3, 0, -2,         0,  5,         0},
                {3, 0, -1,         0,  4,         0},
                {3, 0, -1, 333333334,  3, 666666666},
                {3, 0, -1, 666666667,  3, 333333333},
                {3, 0, -1, 999999999,  3,         1},
                {3, 0,  0,         0,  3,         0},
                {3, 0,  0,         1,  2, 999999999},
                {3, 0,  0, 333333333,  2, 666666667},
                {3, 0,  0, 666666666,  2, 333333334},
                {3, 0,  1,         0,  2,         0},
                {3, 0,  2,         0,  1,         0},
                {3, 0,  3,         0,  0,         0},
                {3, 0,  3, 333333333, -1, 666666667},

                {3, 333333333, -4, 666666667,  6, 666666666},
                {3, 333333333, -3,         0,  6, 333333333},
                {3, 333333333, -2,         0,  5, 333333333},
                {3, 333333333, -1,         0,  4, 333333333},
                {3, 333333333, -1, 333333334,  3, 999999999},
                {3, 333333333, -1, 666666667,  3, 666666666},
                {3, 333333333, -1, 999999999,  3, 333333334},
                {3, 333333333,  0,         0,  3, 333333333},
                {3, 333333333,  0,         1,  3, 333333332},
                {3, 333333333,  0, 333333333,  3,         0},
                {3, 333333333,  0, 666666666,  2, 666666667},
                {3, 333333333,  1,         0,  2, 333333333},
                {3, 333333333,  2,         0,  1, 333333333},
                {3, 333333333,  3,         0,  0, 333333333},
                {3, 333333333,  3, 333333333,  0,         0},

                {MAX_SECOND - 1, 0, -1, 0, MAX_SECOND, 0},
                {MAX_SECOND - 1, 0, 0, -500, MAX_SECOND - 1, 500},
                {MAX_SECOND - 1, 0, 0, -1000000000, MAX_SECOND, 0},

                {MAX_SECOND, 0, 1, 0, MAX_SECOND - 1, 0},
                {MAX_SECOND, 0, 0, 500, MAX_SECOND - 1, 999999500},
                {MAX_SECOND, 0, 0, 1000000000, MAX_SECOND - 1, 0},

                {MAX_SECOND, 0, MAX_SECOND, 0, 0, 0},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_minus")
    public void minus_Duration(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos).minus(Duration.ofSeconds(otherSeconds, otherNanos));
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void minus_Duration_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND);
        assertThrows(DateTimeException.class, () -> i.minus(Duration.ofSeconds(0, 1)));
    }

    @Test
    public void minus_Duration_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        assertThrows(DateTimeException.class, () -> i.minus(Duration.ofSeconds(-1, 999999999)));
    }

    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_minus")
    public void minus_longTemporalUnit(long seconds, int nanos, long otherSeconds, int otherNanos,
            long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos).minus(otherSeconds, SECONDS).minus(otherNanos, NANOS);
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void minus_longTemporalUnit_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND);
        assertThrows(DateTimeException.class, () -> i.minus(1, NANOS));
    }

    @Test
    public void minus_longTemporalUnit_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        i.minus(999999999, NANOS);
        assertThrows(DateTimeException.class, () -> i.minus(-1, SECONDS));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_minusSeconds_long() {
        return new Object[][] {
                {0, 0, 0, 0, 0},
                {0, 0, 1, -1, 0},
                {0, 0, -1, 1, 0},
                {0, 0, -MIN_SECOND, MIN_SECOND, 0},
                {1, 0, 0, 1, 0},
                {1, 0, 1, 0, 0},
                {1, 0, -1, 2, 0},
                {1, 0, -MIN_SECOND + 1, MIN_SECOND, 0},
                {1, 1, 0, 1, 1},
                {1, 1, 1, 0, 1},
                {1, 1, -1, 2, 1},
                {1, 1, -MIN_SECOND, MIN_SECOND + 1, 1},
                {1, 1, -MIN_SECOND + 1, MIN_SECOND, 1},
                {-1, 1, 0, -1, 1},
                {-1, 1, 1, -2, 1},
                {-1, 1, -1, 0, 1},
                {-1, 1, -MAX_SECOND, MAX_SECOND - 1, 1},
                {-1, 1, -(MAX_SECOND + 1), MAX_SECOND, 1},

                {MIN_SECOND, 2, MIN_SECOND, 0, 2},
                {MIN_SECOND + 1, 2, MIN_SECOND, 1, 2},
                {MAX_SECOND - 1, 2, MAX_SECOND, -1, 2},
                {MAX_SECOND, 2, MAX_SECOND, 0, 2},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_minusSeconds_long")
    public void minusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusSeconds(amount);
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void minusSeconds_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(1, 0);
        assertThrows(ArithmeticException.class, () -> i.minusSeconds(Long.MIN_VALUE + 1));
    }

    @Test
    public void minusSeconds_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(-2, 0);
        assertThrows(ArithmeticException.class, () -> i.minusSeconds(Long.MAX_VALUE));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_minusMillis_long() {
        return new Object[][] {
                {0, 0, 0,       0, 0},
                {0, 0, 1,      -1, 999000000},
                {0, 0, 999,    -1, 1000000},
                {0, 0, 1000,   -1, 0},
                {0, 0, 1001,   -2, 999000000},
                {0, 0, 1999,   -2, 1000000},
                {0, 0, 2000,   -2, 0},
                {0, 0, -1,      0, 1000000},
                {0, 0, -999,    0, 999000000},
                {0, 0, -1000,   1, 0},
                {0, 0, -1001,   1, 1000000},
                {0, 0, -1999,   1, 999000000},

                {0, 1, 0,       0, 1},
                {0, 1, 1,      -1, 999000001},
                {0, 1, 998,    -1, 2000001},
                {0, 1, 999,    -1, 1000001},
                {0, 1, 1000,   -1, 1},
                {0, 1, 1998,   -2, 2000001},
                {0, 1, 1999,   -2, 1000001},
                {0, 1, 2000,   -2, 1},
                {0, 1, -1,      0, 1000001},
                {0, 1, -2,      0, 2000001},
                {0, 1, -1000,   1, 1},
                {0, 1, -1001,   1, 1000001},

                {0, 1000000, 0,       0, 1000000},
                {0, 1000000, 1,       0, 0},
                {0, 1000000, 998,    -1, 3000000},
                {0, 1000000, 999,    -1, 2000000},
                {0, 1000000, 1000,   -1, 1000000},
                {0, 1000000, 1998,   -2, 3000000},
                {0, 1000000, 1999,   -2, 2000000},
                {0, 1000000, 2000,   -2, 1000000},
                {0, 1000000, -1,      0, 2000000},
                {0, 1000000, -2,      0, 3000000},
                {0, 1000000, -999,    1, 0},
                {0, 1000000, -1000,   1, 1000000},
                {0, 1000000, -1001,   1, 2000000},
                {0, 1000000, -1002,   1, 3000000},

                {0, 999999999, 0,     0, 999999999},
                {0, 999999999, 1,     0, 998999999},
                {0, 999999999, 999,   0, 999999},
                {0, 999999999, 1000, -1, 999999999},
                {0, 999999999, 1001, -1, 998999999},
                {0, 999999999, -1,    1, 999999},
                {0, 999999999, -1000, 1, 999999999},
                {0, 999999999, -1001, 2, 999999},

                {0, 0, Long.MAX_VALUE, -(Long.MAX_VALUE / 1000) - 1, (int) -(Long.MAX_VALUE % 1000)
                        * 1000000 + 1000000000},
                {0, 0, Long.MIN_VALUE, -(Long.MIN_VALUE / 1000), (int) -(Long.MIN_VALUE % 1000) * 1000000},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_minusMillis_long")
    public void minusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusMillis(amount);
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @ParameterizedTest
    @MethodSource("provider_minusMillis_long")
    public void minusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds + 1, nanos);
        i = i.minusMillis(amount);
        assertEquals(expectedSeconds + 1, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @ParameterizedTest
    @MethodSource("provider_minusMillis_long")
    public void minusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds - 1, nanos);
        i = i.minusMillis(amount);
        assertEquals(expectedSeconds - 1, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void minusMillis_long_max() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 998999999);
        i = i.minusMillis(-1);
        assertEquals(MAX_SECOND, i.getEpochSecond());
        assertEquals(999999999, i.getNano());
    }

    @Test
    public void minusMillis_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999000000);
        assertThrows(DateTimeException.class, () -> i.minusMillis(-1));
    }

    @Test
    public void minusMillis_long_min() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND, 1000000);
        i = i.minusMillis(1);
        assertEquals(MIN_SECOND, i.getEpochSecond());
        assertEquals(0, i.getNano());
    }

    @Test
    public void minusMillis_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND, 0);
        assertThrows(DateTimeException.class, () -> i.minusMillis(1));
    }

    //-----------------------------------------------------------------------
    static Object[][] provider_minusNanos_long() {
        return new Object[][] {
                {0, 0, 0,           0, 0},
                {0, 0, 1,          -1, 999999999},
                {0, 0, 999999999,  -1, 1},
                {0, 0, 1000000000, -1, 0},
                {0, 0, 1000000001, -2, 999999999},
                {0, 0, 1999999999, -2, 1},
                {0, 0, 2000000000, -2, 0},
                {0, 0, -1,          0, 1},
                {0, 0, -999999999,  0, 999999999},
                {0, 0, -1000000000, 1, 0},
                {0, 0, -1000000001, 1, 1},
                {0, 0, -1999999999, 1, 999999999},

                {1, 0, 0,            1, 0},
                {1, 0, 1,            0, 999999999},
                {1, 0, 999999999,    0, 1},
                {1, 0, 1000000000,   0, 0},
                {1, 0, 1000000001,  -1, 999999999},
                {1, 0, 1999999999,  -1, 1},
                {1, 0, 2000000000,  -1, 0},
                {1, 0, -1,           1, 1},
                {1, 0, -999999999,   1, 999999999},
                {1, 0, -1000000000,  2, 0},
                {1, 0, -1000000001,  2, 1},
                {1, 0, -1999999999,  2, 999999999},

                {-1, 0, 0,           -1, 0},
                {-1, 0, 1,           -2, 999999999},
                {-1, 0, 999999999,   -2, 1},
                {-1, 0, 1000000000,  -2, 0},
                {-1, 0, 1000000001,  -3, 999999999},
                {-1, 0, 1999999999,  -3, 1},
                {-1, 0, 2000000000,  -3, 0},
                {-1, 0, -1,          -1, 1},
                {-1, 0, -999999999,  -1, 999999999},
                {-1, 0, -1000000000,  0, 0},
                {-1, 0, -1000000001,  0, 1},
                {-1, 0, -1999999999,  0, 999999999},

                {1, 1, 0,           1, 1},
                {1, 1, 1,           1, 0},
                {1, 1, 999999998,   0, 3},
                {1, 1, 999999999,   0, 2},
                {1, 1, 1000000000,  0, 1},
                {1, 1, 1999999998, -1, 3},
                {1, 1, 1999999999, -1, 2},
                {1, 1, 2000000000, -1, 1},
                {1, 1, -1,          1, 2},
                {1, 1, -2,          1, 3},
                {1, 1, -1000000000, 2, 1},
                {1, 1, -1000000001, 2, 2},
                {1, 1, -1000000002, 2, 3},
                {1, 1, -2000000000, 3, 1},

                {1, 999999999, 0,           1, 999999999},
                {1, 999999999, 1,           1, 999999998},
                {1, 999999999, 999999999,   1, 0},
                {1, 999999999, 1000000000,  0, 999999999},
                {1, 999999999, 1000000001,  0, 999999998},
                {1, 999999999, -1,          2, 0},
                {1, 999999999, -1000000000, 2, 999999999},
                {1, 999999999, -1000000001, 3, 0},
                {1, 999999999, -1999999999, 3, 999999998},
                {1, 999999999, -2000000000, 3, 999999999},

                {MAX_SECOND, 0, -999999999, MAX_SECOND, 999999999},
                {MAX_SECOND - 1, 0, -1999999999, MAX_SECOND, 999999999},
                {MIN_SECOND, 1, 1, MIN_SECOND, 0},
                {MIN_SECOND + 1, 1, 1000000001, MIN_SECOND, 0},

                {0, 0, Long.MAX_VALUE, -(Long.MAX_VALUE / 1000000000) - 1, (int) -(Long.MAX_VALUE % 1000000000)
                        + 1000000000},
                {0, 0, Long.MIN_VALUE, -(Long.MIN_VALUE / 1000000000), (int) -(Long.MIN_VALUE % 1000000000)},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_minusNanos_long")
    public void minusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusNanos(amount);
        assertEquals(expectedSeconds, i.getEpochSecond());
        assertEquals(expectedNanoOfSecond, i.getNano());
    }

    @Test
    public void minusNanos_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(MAX_SECOND, 999999999);
        assertThrows(DateTimeException.class, () -> i.minusNanos(-1));
    }

    @Test
    public void minusNanos_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(MIN_SECOND, 0);
        assertThrows(DateTimeException.class, () -> i.minusNanos(1));
    }

    //-----------------------------------------------------------------------
    // truncatedTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo() {
        assertEquals(Instant.ofEpochSecond(2L), Instant.ofEpochSecond(2L, 1000000).truncatedTo(ChronoUnit.SECONDS));
        assertEquals(Instant.ofEpochSecond(1L), Instant.ofEpochSecond(2L, -1000000).truncatedTo(ChronoUnit.SECONDS));
        assertEquals(Instant.ofEpochSecond(-1L), Instant.ofEpochSecond(0L, -1000000).truncatedTo(ChronoUnit.SECONDS));
        assertEquals(Instant.ofEpochSecond(-1L), Instant.ofEpochSecond(-1L).truncatedTo(ChronoUnit.SECONDS));
        assertEquals(Instant.ofEpochSecond(-2L), Instant.ofEpochSecond(-1L, -1000000).truncatedTo(ChronoUnit.SECONDS));
        assertEquals(Instant.ofEpochSecond(-2L), Instant.ofEpochSecond(-2L).truncatedTo(ChronoUnit.SECONDS));
    }

    //-----------------------------------------------------------------------
    // toEpochMilli()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochMilli() {
        assertEquals(1001L, Instant.ofEpochSecond(1L, 1000000).toEpochMilli());
        assertEquals(1002L, Instant.ofEpochSecond(1L, 2000000).toEpochMilli());
        assertEquals(1000L, Instant.ofEpochSecond(1L, 567).toEpochMilli());
        assertEquals((Long.MAX_VALUE / 1000) * 1000, Instant.ofEpochSecond(Long.MAX_VALUE / 1000).toEpochMilli());
        assertEquals((Long.MIN_VALUE / 1000) * 1000, Instant.ofEpochSecond(Long.MIN_VALUE / 1000).toEpochMilli());
        assertEquals(-1L, Instant.ofEpochSecond(0L, -1000000).toEpochMilli());
        assertEquals(1, Instant.ofEpochSecond(0L, 1000000).toEpochMilli());
        assertEquals(0, Instant.ofEpochSecond(0L, 999999).toEpochMilli());
        assertEquals(0, Instant.ofEpochSecond(0L, 1).toEpochMilli());
        assertEquals(0, Instant.ofEpochSecond(0L, 0).toEpochMilli());
        assertEquals(-1L, Instant.ofEpochSecond(0L, -1).toEpochMilli());
        assertEquals(-1L, Instant.ofEpochSecond(0L, -999999).toEpochMilli());
        assertEquals(-1L, Instant.ofEpochSecond(0L, -1000000).toEpochMilli());
        assertEquals(-2L, Instant.ofEpochSecond(0L, -1000001).toEpochMilli());
    }

    @Test
    public void test_toEpochMilli_tooBig() {
        assertThrows(ArithmeticException.class, () -> Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1).toEpochMilli());
    }

    @Test
    public void test_toEpochMilli_tooBigDueToNanos() {
        assertThrows(ArithmeticException.class,
                () -> Instant.ofEpochMilli(Long.MAX_VALUE).plusMillis(1).toEpochMilli());
    }

    @Test
    public void test_toEpochMilli_tooSmall() {
        assertThrows(ArithmeticException.class, () -> Instant.ofEpochSecond(Long.MIN_VALUE / 1000 - 1).toEpochMilli());
    }

    @Test
    public void test_toEpochMilli_tooSmallDueToNanos() {
        assertThrows(ArithmeticException.class,
                () -> Instant.ofEpochMilli(Long.MIN_VALUE).minusMillis(1).toEpochMilli());
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_Instant(
                Instant.ofEpochSecond(-2L, 0),
                Instant.ofEpochSecond(-2L, 999999998),
                Instant.ofEpochSecond(-2L, 999999999),
                Instant.ofEpochSecond(-1L, 0),
                Instant.ofEpochSecond(-1L, 1),
                Instant.ofEpochSecond(-1L, 999999998),
                Instant.ofEpochSecond(-1L, 999999999),
                Instant.ofEpochSecond(0L, 0),
                Instant.ofEpochSecond(0L, 1),
                Instant.ofEpochSecond(0L, 2),
                Instant.ofEpochSecond(0L, 999999999),
                Instant.ofEpochSecond(1L, 0),
                Instant.ofEpochSecond(2L, 0)
        );
    }

    void doTest_comparisons_Instant(Instant... instants) {
        for (int i = 0; i < instants.length; i++) {
            Instant a = instants[i];
            for (int j = 0; j < instants.length; j++) {
                Instant b = instants[j];
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0, a + " <=> " + b);
                    assertTrue(a.isBefore(b), a + " <=> " + b);
                    assertFalse(a.isAfter(b), a + " <=> " + b);
                    assertFalse(a.equals(b), a + " <=> " + b);
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0, a + " <=> " + b);
                    assertFalse(a.isBefore(b), a + " <=> " + b);
                    assertTrue(a.isAfter(b), a + " <=> " + b);
                    assertFalse(a.equals(b), a + " <=> " + b);
                } else {
                    assertEquals(0, a.compareTo(b), a + " <=> " + b);
                    assertFalse(a.isBefore(b), a + " <=> " + b);
                    assertFalse(a.isAfter(b), a + " <=> " + b);
                    assertTrue(a.equals(b), a + " <=> " + b);
                }
            }
        }
    }

    @Test
    public void test_compareTo_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    public void test_isBefore_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        assertThrows(NullPointerException.class, () -> a.isBefore(null));
    }

    @Test
    public void test_isAfter_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        assertThrows(NullPointerException.class, () -> a.isAfter(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonInstant() {
        Comparable c = Instant.ofEpochSecond(0L);
        assertThrows(ClassCastException.class, () -> c.compareTo(new Object()));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        Instant test5a = Instant.ofEpochSecond(5L, 20);
        Instant test5b = Instant.ofEpochSecond(5L, 20);
        Instant test5n = Instant.ofEpochSecond(5L, 30);
        Instant test6 = Instant.ofEpochSecond(6L, 20);

        assertTrue(test5a.equals(test5a));
        assertTrue(test5a.equals(test5b));
        assertFalse(test5a.equals(test5n));
        assertFalse(test5a.equals(test6));

        assertTrue(test5b.equals(test5a));
        assertTrue(test5b.equals(test5b));
        assertFalse(test5b.equals(test5n));
        assertFalse(test5b.equals(test6));

        assertFalse(test5n.equals(test5a));
        assertFalse(test5n.equals(test5b));
        assertTrue(test5n.equals(test5n));
        assertFalse(test5n.equals(test6));

        assertFalse(test6.equals(test5a));
        assertFalse(test6.equals(test5b));
        assertFalse(test6.equals(test5n));
        assertTrue(test6.equals(test6));
    }

    @Test
    public void test_equals_null() {
        Instant test5 = Instant.ofEpochSecond(5L, 20);
        assertFalse(test5.equals(null));
    }

    @Test
    public void test_equals_otherClass() {
        Instant test5 = Instant.ofEpochSecond(5L, 20);
        assertFalse(test5.equals(""));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode() {
        Instant test5a = Instant.ofEpochSecond(5L, 20);
        Instant test5b = Instant.ofEpochSecond(5L, 20);
        Instant test5n = Instant.ofEpochSecond(5L, 30);
        Instant test6 = Instant.ofEpochSecond(6L, 20);

        assertTrue(test5a.hashCode() == test5a.hashCode());
        assertTrue(test5a.hashCode() == test5b.hashCode());
        assertTrue(test5b.hashCode() == test5b.hashCode());

        assertFalse(test5a.hashCode() == test5n.hashCode());
        assertFalse(test5a.hashCode() == test6.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
                {Instant.ofEpochSecond(65L, 567), "1970-01-01T00:01:05.000000567Z"},
                {Instant.ofEpochSecond(1, 0), "1970-01-01T00:00:01Z"},
                {Instant.ofEpochSecond(60, 0), "1970-01-01T00:01:00Z"},
                {Instant.ofEpochSecond(3600, 0), "1970-01-01T01:00:00Z"},
                {Instant.ofEpochSecond(-1, 0), "1969-12-31T23:59:59Z"},

                {LocalDateTime.of(0, 1, 2, 0, 0).toInstant(ZoneOffset.UTC), "0000-01-02T00:00:00Z"},
                {LocalDateTime.of(0, 1, 1, 12, 30).toInstant(ZoneOffset.UTC), "0000-01-01T12:30:00Z"},
                {LocalDateTime.of(0, 1, 1, 0, 0, 0, 1).toInstant(ZoneOffset.UTC), "0000-01-01T00:00:00.000000001Z"},
                {LocalDateTime.of(0, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), "0000-01-01T00:00:00Z"},

                {LocalDateTime.of(-1, 12, 31, 23, 59, 59, 999999999).toInstant(ZoneOffset.UTC),
                        "-0001-12-31T23:59:59.999999999Z"},
                {LocalDateTime.of(-1, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "-0001-12-31T12:30:00Z"},
                {LocalDateTime.of(-1, 12, 30, 12, 30).toInstant(ZoneOffset.UTC), "-0001-12-30T12:30:00Z"},

                {LocalDateTime.of(-9999, 1, 2, 12, 30).toInstant(ZoneOffset.UTC), "-9999-01-02T12:30:00Z"},
                {LocalDateTime.of(-9999, 1, 1, 12, 30).toInstant(ZoneOffset.UTC), "-9999-01-01T12:30:00Z"},
                {LocalDateTime.of(-9999, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), "-9999-01-01T00:00:00Z"},

                {LocalDateTime.of(-10000, 12, 31, 23, 59, 59, 999999999).toInstant(ZoneOffset.UTC),
                        "-10000-12-31T23:59:59.999999999Z"},
                {LocalDateTime.of(-10000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "-10000-12-31T12:30:00Z"},
                {LocalDateTime.of(-10000, 12, 30, 12, 30).toInstant(ZoneOffset.UTC), "-10000-12-30T12:30:00Z"},
                {LocalDateTime.of(-15000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "-15000-12-31T12:30:00Z"},

                {LocalDateTime.of(-19999, 1, 2, 12, 30).toInstant(ZoneOffset.UTC), "-19999-01-02T12:30:00Z"},
                {LocalDateTime.of(-19999, 1, 1, 12, 30).toInstant(ZoneOffset.UTC), "-19999-01-01T12:30:00Z"},
                {LocalDateTime.of(-19999, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), "-19999-01-01T00:00:00Z"},

                {LocalDateTime.of(-20000, 12, 31, 23, 59, 59, 999999999).toInstant(ZoneOffset.UTC),
                        "-20000-12-31T23:59:59.999999999Z"},
                {LocalDateTime.of(-20000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "-20000-12-31T12:30:00Z"},
                {LocalDateTime.of(-20000, 12, 30, 12, 30).toInstant(ZoneOffset.UTC), "-20000-12-30T12:30:00Z"},
                {LocalDateTime.of(-25000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "-25000-12-31T12:30:00Z"},

                {LocalDateTime.of(9999, 12, 30, 12, 30).toInstant(ZoneOffset.UTC), "9999-12-30T12:30:00Z"},
                {LocalDateTime.of(9999, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "9999-12-31T12:30:00Z"},
                {LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999999).toInstant(ZoneOffset.UTC),
                        "9999-12-31T23:59:59.999999999Z"},

                {LocalDateTime.of(10000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), "+10000-01-01T00:00:00Z"},
                {LocalDateTime.of(10000, 1, 1, 12, 30).toInstant(ZoneOffset.UTC), "+10000-01-01T12:30:00Z"},
                {LocalDateTime.of(10000, 1, 2, 12, 30).toInstant(ZoneOffset.UTC), "+10000-01-02T12:30:00Z"},
                {LocalDateTime.of(15000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "+15000-12-31T12:30:00Z"},

                {LocalDateTime.of(19999, 12, 30, 12, 30).toInstant(ZoneOffset.UTC), "+19999-12-30T12:30:00Z"},
                {LocalDateTime.of(19999, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "+19999-12-31T12:30:00Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 999999999).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.999999999Z"},

                {LocalDateTime.of(20000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), "+20000-01-01T00:00:00Z"},
                {LocalDateTime.of(20000, 1, 1, 12, 30).toInstant(ZoneOffset.UTC), "+20000-01-01T12:30:00Z"},
                {LocalDateTime.of(20000, 1, 2, 12, 30).toInstant(ZoneOffset.UTC), "+20000-01-02T12:30:00Z"},
                {LocalDateTime.of(25000, 12, 31, 12, 30).toInstant(ZoneOffset.UTC), "+25000-12-31T12:30:00Z"},

                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 9999999).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.009999999Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 999999000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.999999Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 9999000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.009999Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 123000000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.123Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 100000000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.100Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 20000000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.020Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 3000000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.003Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 400000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000400Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 50000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000050Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 6000).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000006Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 700).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000000700Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 80).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000000080Z"},
                {LocalDateTime.of(19999, 12, 31, 23, 59, 59, 9).toInstant(ZoneOffset.UTC),
                        "+19999-12-31T23:59:59.000000009Z"},
                {LocalDateTime.of(-999999999, 1, 1, 12, 30).toInstant(ZoneOffset.UTC).minus(1, DAYS),
                        "-1000000000-12-31T12:30:00Z"},

                {LocalDateTime.of(999999999, 12, 31, 12, 30).toInstant(ZoneOffset.UTC).plus(1, DAYS),
                        "+1000000000-01-01T12:30:00Z"},

                {Instant.MIN, "-1000000000-01-01T00:00:00Z"},
                {Instant.MAX, "+1000000000-12-31T23:59:59.999999999Z"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(Instant instant, String expected) {
        assertEquals(expected, instant.toString());
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_parse(Instant instant, String text) {
        assertEquals(instant, Instant.parse(text));
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_parseLowercase(Instant instant, String text) {
        assertEquals(instant, Instant.parse(text.toLowerCase(Locale.ENGLISH)));
    }

}
