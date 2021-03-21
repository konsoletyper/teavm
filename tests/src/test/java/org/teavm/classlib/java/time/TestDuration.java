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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Duration.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestDuration extends AbstractTest {
    
    //-----------------------------------------------------------------------
    // constants
    //-----------------------------------------------------------------------
    @Test
    public void test_zero() {
        assertEquals(Duration.ZERO.getSeconds(), 0L);
        assertEquals(Duration.ZERO.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    // ofSeconds(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_seconds_long() {
        for (long i = -2; i <= 2; i++) {
            Duration t = Duration.ofSeconds(i);
            assertEquals(t.getSeconds(), i);
            assertEquals(t.getNano(), 0);
        }
    }

    //-----------------------------------------------------------------------
    // ofSeconds(long,long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_seconds_long_long() {
        for (long i = -2; i <= 2; i++) {
            for (int j = 0; j < 10; j++) {
                Duration t = Duration.ofSeconds(i, j);
                assertEquals(t.getSeconds(), i);
                assertEquals(t.getNano(), j);
            }
            for (int j = -10; j < 0; j++) {
                Duration t = Duration.ofSeconds(i, j);
                assertEquals(t.getSeconds(), i - 1);
                assertEquals(t.getNano(), j + 1000000000);
            }
            for (int j = 999999990; j < 1000000000; j++) {
                Duration t = Duration.ofSeconds(i, j);
                assertEquals(t.getSeconds(), i);
                assertEquals(t.getNano(), j);
            }
        }
    }

    @Test
    public void factory_seconds_long_long_nanosNegativeAdjusted() {
        Duration test = Duration.ofSeconds(2L, -1);
        assertEquals(test.getSeconds(), 1);
        assertEquals(test.getNano(), 999999999);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_seconds_long_long_tooBig() {
        Duration.ofSeconds(Long.MAX_VALUE, 1000000000);
    }

    //-----------------------------------------------------------------------
    // ofMillis(long)
    //-----------------------------------------------------------------------
    @DataProvider(name = "MillisDurationNoNanos")
    Object[][] provider_factory_millis_long() {
        return new Object[][] {
            {0, 0, 0},
            {1, 0, 1000000},
            {2, 0, 2000000},
            {999, 0, 999000000},
            {1000, 1, 0},
            {1001, 1, 1000000},
            {-1, -1, 999000000},
            {-2, -1, 998000000},
            {-999, -1, 1000000},
            {-1000, -1, 0},
            {-1001, -2, 999000000},
        };
    }

    @Test(dataProvider = "MillisDurationNoNanos")
    public void factory_millis_long(long millis, long expectedSeconds, int expectedNanoOfSecond) {
        Duration test = Duration.ofMillis(millis);
        assertEquals(test.getSeconds(), expectedSeconds);
        assertEquals(test.getNano(), expectedNanoOfSecond);
    }

    //-----------------------------------------------------------------------
    // ofNanos(long)
    //-----------------------------------------------------------------------
    @Test
    public void factory_nanos_nanos() {
        Duration test = Duration.ofNanos(1);
        assertEquals(test.getSeconds(), 0);
        assertEquals(test.getNano(), 1);
    }

    @Test
    public void factory_nanos_nanosSecs() {
        Duration test = Duration.ofNanos(1000000002);
        assertEquals(test.getSeconds(), 1);
        assertEquals(test.getNano(), 2);
    }

    @Test
    public void factory_nanos_negative() {
        Duration test = Duration.ofNanos(-2000000001);
        assertEquals(test.getSeconds(), -3);
        assertEquals(test.getNano(), 999999999);
    }

    @Test
    public void factory_nanos_max() {
        Duration test = Duration.ofNanos(Long.MAX_VALUE);
        assertEquals(test.getSeconds(), Long.MAX_VALUE / 1000000000);
        assertEquals(test.getNano(), Long.MAX_VALUE % 1000000000);
    }

    @Test
    public void factory_nanos_min() {
        Duration test = Duration.ofNanos(Long.MIN_VALUE);
        assertEquals(test.getSeconds(), Long.MIN_VALUE / 1000000000 - 1);
        assertEquals(test.getNano(), Long.MIN_VALUE % 1000000000 + 1000000000);
    }

    //-----------------------------------------------------------------------
    // ofMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void factory_minutes() {
        Duration test = Duration.ofMinutes(2);
        assertEquals(test.getSeconds(), 120);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_minutes_max() {
        Duration test = Duration.ofMinutes(Long.MAX_VALUE / 60);
        assertEquals(test.getSeconds(), (Long.MAX_VALUE / 60) * 60);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_minutes_min() {
        Duration test = Duration.ofMinutes(Long.MIN_VALUE / 60);
        assertEquals(test.getSeconds(), (Long.MIN_VALUE / 60) * 60);
        assertEquals(test.getNano(), 0);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_minutes_tooBig() {
        Duration.ofMinutes(Long.MAX_VALUE / 60 + 1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_minutes_tooSmall() {
        Duration.ofMinutes(Long.MIN_VALUE / 60 - 1);
    }

    //-----------------------------------------------------------------------
    // ofHours()
    //-----------------------------------------------------------------------
    @Test
    public void factory_hours() {
        Duration test = Duration.ofHours(2);
        assertEquals(test.getSeconds(), 2 * 3600);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_hours_max() {
        Duration test = Duration.ofHours(Long.MAX_VALUE / 3600);
        assertEquals(test.getSeconds(), (Long.MAX_VALUE / 3600) * 3600);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_hours_min() {
        Duration test = Duration.ofHours(Long.MIN_VALUE / 3600);
        assertEquals(test.getSeconds(), (Long.MIN_VALUE / 3600) * 3600);
        assertEquals(test.getNano(), 0);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_hours_tooBig() {
        Duration.ofHours(Long.MAX_VALUE / 3600 + 1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_hours_tooSmall() {
        Duration.ofHours(Long.MIN_VALUE / 3600 - 1);
    }

    //-----------------------------------------------------------------------
    // ofDays()
    //-----------------------------------------------------------------------
    @Test
    public void factory_days() {
        Duration test = Duration.ofDays(2);
        assertEquals(test.getSeconds(), 2 * 86400);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_days_max() {
        Duration test = Duration.ofDays(Long.MAX_VALUE / 86400);
        assertEquals(test.getSeconds(), (Long.MAX_VALUE / 86400) * 86400);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_days_min() {
        Duration test = Duration.ofDays(Long.MIN_VALUE / 86400);
        assertEquals(test.getSeconds(), (Long.MIN_VALUE / 86400) * 86400);
        assertEquals(test.getNano(), 0);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_days_tooBig() {
        Duration.ofDays(Long.MAX_VALUE / 86400 + 1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_days_tooSmall() {
        Duration.ofDays(Long.MIN_VALUE / 86400 - 1);
    }

    //-----------------------------------------------------------------------
    // of(long,TemporalUnit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "OfTemporalUnit")
    Object[][] provider_factory_of_longTemporalUnit() {
        return new Object[][] {
            {0, NANOS, 0, 0},
            {0, MICROS, 0, 0},
            {0, MILLIS, 0, 0},
            {0, SECONDS, 0, 0},
            {0, MINUTES, 0, 0},
            {0, HOURS, 0, 0},
            {0, HALF_DAYS, 0, 0},
            {0, DAYS, 0, 0},
            {1, NANOS, 0, 1},
            {1, MICROS, 0, 1000},
            {1, MILLIS, 0, 1000000},
            {1, SECONDS, 1, 0},
            {1, MINUTES, 60, 0},
            {1, HOURS, 3600, 0},
            {1, HALF_DAYS, 43200, 0},
            {1, DAYS, 86400, 0},
            {3, NANOS, 0, 3},
            {3, MICROS, 0, 3000},
            {3, MILLIS, 0, 3000000},
            {3, SECONDS, 3, 0},
            {3, MINUTES, 3 * 60, 0},
            {3, HOURS, 3 * 3600, 0},
            {3, HALF_DAYS, 3 * 43200, 0},
            {3, DAYS, 3 * 86400, 0},
            {-1, NANOS, -1, 999999999},
            {-1, MICROS, -1, 999999000},
            {-1, MILLIS, -1, 999000000},
            {-1, SECONDS, -1, 0},
            {-1, MINUTES, -60, 0},
            {-1, HOURS, -3600, 0},
            {-1, HALF_DAYS, -43200, 0},
            {-1, DAYS, -86400, 0},
            {-3, NANOS, -1, 999999997},
            {-3, MICROS, -1, 999997000},
            {-3, MILLIS, -1, 997000000},
            {-3, SECONDS, -3, 0},
            {-3, MINUTES, -3 * 60, 0},
            {-3, HOURS, -3 * 3600, 0},
            {-3, HALF_DAYS, -3 * 43200, 0},
            {-3, DAYS, -3 * 86400, 0},
            {Long.MAX_VALUE, NANOS, Long.MAX_VALUE / 1000000000, (int) (Long.MAX_VALUE % 1000000000)},
            {Long.MIN_VALUE, NANOS, Long.MIN_VALUE / 1000000000 - 1, (int) (Long.MIN_VALUE % 1000000000 + 1000000000)},
            {Long.MAX_VALUE, MICROS, Long.MAX_VALUE / 1000000, (int) ((Long.MAX_VALUE % 1000000) * 1000)},
            {Long.MIN_VALUE, MICROS, Long.MIN_VALUE / 1000000 - 1, (int) ((Long.MIN_VALUE % 1000000 + 1000000) * 1000)},
            {Long.MAX_VALUE, MILLIS, Long.MAX_VALUE / 1000, (int) ((Long.MAX_VALUE % 1000) * 1000000)},
            {Long.MIN_VALUE, MILLIS, Long.MIN_VALUE / 1000 - 1, (int) ((Long.MIN_VALUE % 1000 + 1000) * 1000000)},
            {Long.MAX_VALUE, SECONDS, Long.MAX_VALUE, 0},
            {Long.MIN_VALUE, SECONDS, Long.MIN_VALUE, 0},
            {Long.MAX_VALUE / 60, MINUTES, (Long.MAX_VALUE / 60) * 60, 0},
            {Long.MIN_VALUE / 60, MINUTES, (Long.MIN_VALUE / 60) * 60, 0},
            {Long.MAX_VALUE / 3600, HOURS, (Long.MAX_VALUE / 3600) * 3600, 0},
            {Long.MIN_VALUE / 3600, HOURS, (Long.MIN_VALUE / 3600) * 3600, 0},
            {Long.MAX_VALUE / 43200, HALF_DAYS, (Long.MAX_VALUE / 43200) * 43200, 0},
            {Long.MIN_VALUE / 43200, HALF_DAYS, (Long.MIN_VALUE / 43200) * 43200, 0},
        };
    }

    @Test(dataProvider = "OfTemporalUnit")
    public void factory_of_longTemporalUnit(long amount, TemporalUnit unit, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.of(amount, unit);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @DataProvider(name = "OfTemporalUnitOutOfRange")
    Object[][] provider_factory_of_longTemporalUnit_outOfRange() {
        return new Object[][] {
            {Long.MAX_VALUE / 60 + 1, MINUTES},
            {Long.MIN_VALUE / 60 - 1, MINUTES},
            {Long.MAX_VALUE / 3600 + 1, HOURS},
            {Long.MIN_VALUE / 3600 - 1, HOURS},
            {Long.MAX_VALUE / 43200 + 1, HALF_DAYS},
            {Long.MIN_VALUE / 43200 - 1, HALF_DAYS},
        };
    }

    @Test(dataProvider = "OfTemporalUnitOutOfRange", expectedExceptions = ArithmeticException.class)
    public void factory_of_longTemporalUnit_outOfRange(long amount, TemporalUnit unit) {
        Duration.of(amount, unit);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_of_longTemporalUnit_estimatedUnit() {
        Duration.of(2, WEEKS);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_of_longTemporalUnit_null() {
        Duration.of(1, (TemporalUnit) null);
    }

    //-----------------------------------------------------------------------
    // between()
    //-----------------------------------------------------------------------
    @DataProvider(name = "DurationBetween")
    Object[][] provider_factory_between_Instant_Instant() {
        return new Object[][] {
            {0, 0, 0, 0, 0, 0},
            {3, 0, 7, 0, 4, 0},
            {3, 20, 7, 50, 4, 30},
            {3, 80, 7, 50, 3, 999999970},
            {7, 0, 3, 0, -4, 0},
        };
    }

    @Test(dataProvider = "DurationBetween")
    public void factory_between_Instant_Instant(long secs1, int nanos1, long secs2, int nanos2, long expectedSeconds,
            int expectedNanoOfSecond) {
        Instant start = Instant.ofEpochSecond(secs1, nanos1);
        Instant end = Instant.ofEpochSecond(secs2, nanos2);
        Duration t = Duration.between(start, end);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_between_Instant_Instant_startNull() {
        Instant end = Instant.ofEpochSecond(1);
        Duration.between(null, end);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_between_Instant_Instant_endNull() {
        Instant start = Instant.ofEpochSecond(1);
        Duration.between(start, null);
    }

    //-----------------------------------------------------------------------
    // parse(String)
    //-----------------------------------------------------------------------
    @DataProvider(name = "Parse")
    Object[][] provider_factory_parse() {
        return new Object[][] {
            {"PT0S", 0, 0},

            {"PT1S", 1, 0},
            {"PT12S", 12, 0},
            {"PT123456789S", 123456789, 0},
            {"PT" + Long.MAX_VALUE + "S", Long.MAX_VALUE, 0},

            {"PT+1S", 1, 0},
            {"PT+12S", 12, 0},
            {"PT-1S", -1, 0},
            {"PT-12S", -12, 0},
            {"PT-123456789S", -123456789, 0},
            {"PT" + Long.MIN_VALUE + "S", Long.MIN_VALUE, 0},

            {"PT0.1S", 0, 100000000},
            {"PT1.1S", 1, 100000000},
            {"PT1.12S", 1, 120000000},
            {"PT1.123S", 1, 123000000},
            {"PT1.1234S", 1, 123400000},
            {"PT1.12345S", 1, 123450000},
            {"PT1.123456S", 1, 123456000},
            {"PT1.1234567S", 1, 123456700},
            {"PT1.12345678S", 1, 123456780},
            {"PT1.123456789S", 1, 123456789},

            {"PT-0.1S", -1, 1000000000 - 100000000},
            {"PT-1.1S", -2, 1000000000 - 100000000},
            {"PT-1.12S", -2, 1000000000 - 120000000},
            {"PT-1.123S", -2, 1000000000 - 123000000},
            {"PT-1.1234S", -2, 1000000000 - 123400000},
            {"PT-1.12345S", -2, 1000000000 - 123450000},
            {"PT-1.123456S", -2, 1000000000 - 123456000},
            {"PT-1.1234567S", -2, 1000000000 - 123456700},
            {"PT-1.12345678S", -2, 1000000000 - 123456780},
            {"PT-1.123456789S", -2, 1000000000 - 123456789},

            {"PT" + Long.MAX_VALUE + ".123456789S", Long.MAX_VALUE, 123456789},
            {"PT" + Long.MIN_VALUE + ".000000000S", Long.MIN_VALUE, 0},
            
            {"PT12M", 12 * 60, 0},
            {"PT12M0.35S", 12 * 60, 350000000},
            {"PT12M1.35S", 12 * 60 + 1, 350000000},
            {"PT12M-0.35S", 12 * 60 - 1, 1000000000 - 350000000},
            {"PT12M-1.35S", 12 * 60 - 2, 1000000000 - 350000000},
            
            {"PT12H", 12 * 3600, 0},
            {"PT12H0.35S", 12 * 3600, 350000000},
            {"PT12H1.35S", 12 * 3600 + 1, 350000000},
            {"PT12H-0.35S", 12 * 3600 - 1, 1000000000 - 350000000},
            {"PT12H-1.35S", 12 * 3600 - 2, 1000000000 - 350000000},
            
            {"P12D", 12 * 24 * 3600, 0},
            {"P12DT0.35S", 12 * 24 * 3600, 350000000},
            {"P12DT1.35S", 12 * 24 * 3600 + 1, 350000000},
            {"P12DT-0.35S", 12 * 24 * 3600 - 1, 1000000000 - 350000000},
            {"P12DT-1.35S", 12 * 24 * 3600 - 2, 1000000000 - 350000000},
        };
    }

    @Test(dataProvider = "Parse")
    public void factory_parse(String text, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.parse(text);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "Parse")
    public void factory_parse_ignoreCase(String text, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.parse(text.toLowerCase(Locale.ENGLISH));
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "Parse")
    public void factory_parse_comma(String text, long expectedSeconds, int expectedNanoOfSecond) {
        text = text.replace('.', ',');
        Duration t = Duration.parse(text);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @DataProvider(name = "ParseFailures")
    Object[][] provider_factory_parseFailures() {
        return new Object[][] {
            {""},
            {"PTS"},
            {"AT0S"},
            {"PA0S"},
            {"PT0A"},

            {"PT+S"},
            {"PT-S"},
            {"PT.S"},
            {"PTAS"},

            {"PT-.S"},
            {"PT+.S"},

            {"PT1ABC2S"},
            {"PT1.1ABC2S"},

            {"PT123456789123456789123456789S"},
            {"PT0.1234567891S"},
            {"PT.1S"},

            {"PT2.-3"},
            {"PT-2.-3"},
            {"PT2.+3"},
            {"PT-2.+3"},
        };
    }

    @Test(dataProvider = "ParseFailures", expectedExceptions = DateTimeParseException.class)
    public void factory_parseFailures(String text) {
        Duration.parse(text);
    }

    @Test(dataProvider = "ParseFailures", expectedExceptions = DateTimeParseException.class)
    public void factory_parseFailures_comma(String text) {
        text = text.replace('.', ',');
        Duration.parse(text);
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void factory_parse_tooBig() {
        Duration.parse("PT" + Long.MAX_VALUE + "1S");
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void factory_parse_tooBig_decimal() {
        Duration.parse("PT" + Long.MAX_VALUE + "1.1S");
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void factory_parse_tooSmall() {
        Duration.parse("PT" + Long.MIN_VALUE + "1S");
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void factory_parse_tooSmall_decimal() {
        Duration.parse("PT" + Long.MIN_VALUE + ".1S");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_parse_nullText() {
        Duration.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // isZero(), isPositive(), isPositiveOrZero(), isNegative(), isNegativeOrZero()
    //-----------------------------------------------------------------------
    @Test
    public void test_isZero() {
        assertEquals(Duration.ofNanos(0).isZero(), true);
        assertEquals(Duration.ofSeconds(0).isZero(), true);
        assertEquals(Duration.ofNanos(1).isZero(), false);
        assertEquals(Duration.ofSeconds(1).isZero(), false);
        assertEquals(Duration.ofSeconds(1, 1).isZero(), false);
        assertEquals(Duration.ofNanos(-1).isZero(), false);
        assertEquals(Duration.ofSeconds(-1).isZero(), false);
        assertEquals(Duration.ofSeconds(-1, -1).isZero(), false);
    }

    @Test
    public void test_isNegative() {
        assertEquals(Duration.ofNanos(0).isNegative(), false);
        assertEquals(Duration.ofSeconds(0).isNegative(), false);
        assertEquals(Duration.ofNanos(1).isNegative(), false);
        assertEquals(Duration.ofSeconds(1).isNegative(), false);
        assertEquals(Duration.ofSeconds(1, 1).isNegative(), false);
        assertEquals(Duration.ofNanos(-1).isNegative(), true);
        assertEquals(Duration.ofSeconds(-1).isNegative(), true);
        assertEquals(Duration.ofSeconds(-1, -1).isNegative(), true);
    }

    //-----------------------------------------------------------------------
    // plus()
    //-----------------------------------------------------------------------
    @DataProvider(name = "Plus")
    Object[][] provider_plus() {
        return new Object[][] {
            {Long.MIN_VALUE, 0, Long.MAX_VALUE, 0, -1, 0},

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

            {Long.MAX_VALUE, 0, Long.MIN_VALUE, 0, -1, 0},
       };
    }

    @Test(dataProvider = "Plus")
    public void plus(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds,
            int expectedNanoOfSecond) {
       Duration t = Duration.ofSeconds(seconds, nanos).plus(Duration.ofSeconds(otherSeconds, otherNanos));
       assertEquals(t.getSeconds(), expectedSeconds);
       assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusOverflowTooBig() {
       Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
       t.plus(Duration.ofSeconds(0, 1));
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusOverflowTooSmall() {
       Duration t = Duration.ofSeconds(Long.MIN_VALUE);
       t.plus(Duration.ofSeconds(-1, 999999999));
    }

    //-----------------------------------------------------------------------
    @Test
    public void plus_longTemporalUnit_seconds() {
        Duration t = Duration.ofSeconds(1);
        t = t.plus(1, SECONDS);
        assertEquals(2, t.getSeconds());
        assertEquals(0, t.getNano());
     }

    @Test
    public void plus_longTemporalUnit_millis() {
        Duration t = Duration.ofSeconds(1);
        t = t.plus(1, MILLIS);
        assertEquals(1, t.getSeconds());
        assertEquals(1000000, t.getNano());
     }

    @Test
    public void plus_longTemporalUnit_micros() {
        Duration t = Duration.ofSeconds(1);
        t = t.plus(1, MICROS);
        assertEquals(1, t.getSeconds());
        assertEquals(1000, t.getNano());
     }

    @Test
    public void plus_longTemporalUnit_nanos() {
        Duration t = Duration.ofSeconds(1);
        t = t.plus(1, NANOS);
        assertEquals(1, t.getSeconds());
        assertEquals(1, t.getNano());
     }

    @Test(expectedExceptions = NullPointerException.class)
    public void plus_longTemporalUnit_null() {
       Duration t = Duration.ofSeconds(1);
       t.plus(1, (TemporalUnit) null);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "PlusSeconds")
    Object[][] provider_plusSeconds_long() {
        return new Object[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0},
            {0, 0, -1, -1, 0},
            {0, 0, Long.MAX_VALUE, Long.MAX_VALUE, 0},
            {0, 0, Long.MIN_VALUE, Long.MIN_VALUE, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 1, 2, 0},
            {1, 0, -1, 0, 0},
            {1, 0, Long.MAX_VALUE - 1, Long.MAX_VALUE, 0},
            {1, 0, Long.MIN_VALUE, Long.MIN_VALUE + 1, 0},
            {1, 1, 0, 1, 1},
            {1, 1, 1, 2, 1},
            {1, 1, -1, 0, 1},
            {1, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE, 1},
            {1, 1, Long.MIN_VALUE, Long.MIN_VALUE + 1, 1},
            {-1, 1, 0, -1, 1},
            {-1, 1, 1, 0, 1},
            {-1, 1, -1, -2, 1},
            {-1, 1, Long.MAX_VALUE, Long.MAX_VALUE - 1, 1},
            {-1, 1, Long.MIN_VALUE + 1, Long.MIN_VALUE, 1},
        };
    }

    @Test(dataProvider = "PlusSeconds")
    public void plusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.plusSeconds(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusSeconds_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(1, 0);
        t.plusSeconds(Long.MAX_VALUE);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusSeconds_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(-1, 0);
        t.plusSeconds(Long.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "PlusMillis")
    Object[][] provider_plusMillis_long() {
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
        };
    }

    @Test(dataProvider = "PlusMillis")
    public void plusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "PlusMillis")
    public void plusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds + 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds + 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "PlusMillis")
    public void plusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds - 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds - 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test
    public void plusMillis_long_max() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 998999999);
        t = t.plusMillis(1);
        assertEquals(t.getSeconds(), Long.MAX_VALUE);
        assertEquals(t.getNano(), 999999999);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusMillis_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999000000);
        t.plusMillis(1);
    }

    @Test
    public void plusMillis_long_min() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 1000000);
        t = t.plusMillis(-1);
        assertEquals(t.getSeconds(), Long.MIN_VALUE);
        assertEquals(t.getNano(), 0);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusMillis_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 0);
        t.plusMillis(-1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "PlusNanos")
    Object[][] provider_plusNanos_long() {
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

            {Long.MAX_VALUE, 0, 999999999, Long.MAX_VALUE, 999999999},
            {Long.MAX_VALUE - 1, 0, 1999999999, Long.MAX_VALUE, 999999999},
            {Long.MIN_VALUE, 1, -1, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 1, -1000000001, Long.MIN_VALUE, 0},
        };
    }

    @Test(dataProvider = "PlusNanos")
    public void plusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.plusNanos(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusNanos_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
        t.plusNanos(1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void plusNanos_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 0);
        t.plusNanos(-1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "Minus")
    Object[][] provider_minus() {
        return new Object[][] {
            {Long.MIN_VALUE, 0, Long.MIN_VALUE + 1, 0, -1, 0},

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

            {Long.MAX_VALUE, 0, Long.MAX_VALUE, 0, 0, 0},
       };
    }

    @Test(dataProvider = "Minus")
    public void minus(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds,
            int expectedNanoOfSecond) {
       Duration t = Duration.ofSeconds(seconds, nanos).minus(Duration.ofSeconds(otherSeconds, otherNanos));
       assertEquals(t.getSeconds(), expectedSeconds);
       assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusOverflowTooSmall() {
       Duration t = Duration.ofSeconds(Long.MIN_VALUE);
       t.minus(Duration.ofSeconds(0, 1));
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusOverflowTooBig() {
       Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
       t.minus(Duration.ofSeconds(-1, 999999999));
    }

    //-----------------------------------------------------------------------
    @Test
    public void minus_longTemporalUnit_seconds() {
        Duration t = Duration.ofSeconds(1);
        t = t.minus(1, SECONDS);
        assertEquals(0, t.getSeconds());
        assertEquals(0, t.getNano());
     }

    @Test
    public void minus_longTemporalUnit_millis() {
        Duration t = Duration.ofSeconds(1);
        t = t.minus(1, MILLIS);
        assertEquals(0, t.getSeconds());
        assertEquals(999000000, t.getNano());
     }

    @Test
    public void minus_longTemporalUnit_micros() {
        Duration t = Duration.ofSeconds(1);
        t = t.minus(1, MICROS);
        assertEquals(0, t.getSeconds());
        assertEquals(999999000, t.getNano());
     }

    @Test
    public void minus_longTemporalUnit_nanos() {
        Duration t = Duration.ofSeconds(1);
        t = t.minus(1, NANOS);
        assertEquals(0, t.getSeconds());
        assertEquals(999999999, t.getNano());
     }

    @Test(expectedExceptions = NullPointerException.class)
    public void minus_longTemporalUnit_null() {
       Duration t = Duration.ofSeconds(1);
       t.minus(1, (TemporalUnit) null);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "MinusSeconds")
    Object[][] provider_minusSeconds_long() {
        return new Object[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 1, -1, 0},
            {0, 0, -1, 1, 0},
            {0, 0, Long.MAX_VALUE, -Long.MAX_VALUE, 0},
            {0, 0, Long.MIN_VALUE + 1, Long.MAX_VALUE, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 0, -1, 2, 0},
            {1, 0, Long.MAX_VALUE - 1, -Long.MAX_VALUE + 2, 0},
            {1, 0, Long.MIN_VALUE + 2, Long.MAX_VALUE, 0},
            {1, 1, 0, 1, 1},
            {1, 1, 1, 0, 1},
            {1, 1, -1, 2, 1},
            {1, 1, Long.MAX_VALUE, -Long.MAX_VALUE + 1, 1},
            {1, 1, Long.MIN_VALUE + 2, Long.MAX_VALUE, 1},
            {-1, 1, 0, -1, 1},
            {-1, 1, 1, -2, 1},
            {-1, 1, -1, 0, 1},
            {-1, 1, Long.MAX_VALUE, Long.MIN_VALUE, 1},
            {-1, 1, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, 1},
        };
    }

    @Test(dataProvider = "MinusSeconds")
    public void minusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.minusSeconds(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusSeconds_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(1, 0);
        t.minusSeconds(Long.MIN_VALUE + 1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusSeconds_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(-2, 0);
        t.minusSeconds(Long.MAX_VALUE);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "MinusMillis")
    Object[][] provider_minusMillis_long() {
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
        };
    }

    @Test(dataProvider = "MinusMillis")
    public void minusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.minusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "MinusMillis")
    public void minusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds + 1, nanos);
        t = t.minusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds + 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider = "MinusMillis")
    public void minusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds,
            int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds - 1, nanos);
        t = t.minusMillis(amount);
        assertEquals(t.getSeconds(), expectedSeconds - 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test
    public void minusMillis_long_max() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 998999999);
        t = t.minusMillis(-1);
        assertEquals(t.getSeconds(), Long.MAX_VALUE);
        assertEquals(t.getNano(), 999999999);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusMillis_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999000000);
        t.minusMillis(-1);
    }

    @Test
    public void minusMillis_long_min() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 1000000);
        t = t.minusMillis(1);
        assertEquals(t.getSeconds(), Long.MIN_VALUE);
        assertEquals(t.getNano(), 0);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusMillis_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 0);
        t.minusMillis(1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "MinusNanos")
    Object[][] provider_minusNanos_long() {
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

            {Long.MAX_VALUE, 0, -999999999, Long.MAX_VALUE, 999999999},
            {Long.MAX_VALUE - 1, 0, -1999999999, Long.MAX_VALUE, 999999999},
            {Long.MIN_VALUE, 1, 1, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 1, 1000000001, Long.MIN_VALUE, 0},
        };
    }

    @Test(dataProvider = "MinusNanos")
    public void minusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.minusNanos(amount);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusNanos_long_overflowTooBig() {
        Duration t = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
        t.minusNanos(-1);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void minusNanos_long_overflowTooSmall() {
        Duration t = Duration.ofSeconds(Long.MIN_VALUE, 0);
        t.minusNanos(1);
    }

    //-----------------------------------------------------------------------
    // multipliedBy()
    //-----------------------------------------------------------------------
    @DataProvider(name = "MultipliedBy")
    Object[][] provider_multipliedBy() {
       return new Object[][] {
          {-4, 666666667, -3,   9, 999999999},
          {-4, 666666667, -2,   6, 666666666},
          {-4, 666666667, -1,   3, 333333333},
          {-4, 666666667,  0,   0,         0},
          {-4, 666666667,  1,  -4, 666666667},
          {-4, 666666667,  2,  -7, 333333334},
          {-4, 666666667,  3, -10, 000000001},

          {-3, 0, -3,  9, 0},
          {-3, 0, -2,  6, 0},
          {-3, 0, -1,  3, 0},
          {-3, 0,  0,  0, 0},
          {-3, 0,  1, -3, 0},
          {-3, 0,  2, -6, 0},
          {-3, 0,  3, -9, 0},

          {-2, 0, -3,  6, 0},
          {-2, 0, -2,  4, 0},
          {-2, 0, -1,  2, 0},
          {-2, 0,  0,  0, 0},
          {-2, 0,  1, -2, 0},
          {-2, 0,  2, -4, 0},
          {-2, 0,  3, -6, 0},

          {-1, 0, -3,  3, 0},
          {-1, 0, -2,  2, 0},
          {-1, 0, -1,  1, 0},
          {-1, 0,  0,  0, 0},
          {-1, 0,  1, -1, 0},
          {-1, 0,  2, -2, 0},
          {-1, 0,  3, -3, 0},

          {-1, 500000000, -3,  1, 500000000},
          {-1, 500000000, -2,  1,         0},
          {-1, 500000000, -1,  0, 500000000},
          {-1, 500000000,  0,  0,         0},
          {-1, 500000000,  1, -1, 500000000},
          {-1, 500000000,  2, -1,         0},
          {-1, 500000000,  3, -2, 500000000},

          {0, 0, -3, 0, 0},
          {0, 0, -2, 0, 0},
          {0, 0, -1, 0, 0},
          {0, 0,  0, 0, 0},
          {0, 0,  1, 0, 0},
          {0, 0,  2, 0, 0},
          {0, 0,  3, 0, 0},

          {0, 500000000, -3, -2, 500000000},
          {0, 500000000, -2, -1,         0},
          {0, 500000000, -1, -1, 500000000},
          {0, 500000000,  0,  0,         0},
          {0, 500000000,  1,  0, 500000000},
          {0, 500000000,  2,  1,         0},
          {0, 500000000,  3,  1, 500000000},

          {1, 0, -3, -3, 0},
          {1, 0, -2, -2, 0},
          {1, 0, -1, -1, 0},
          {1, 0,  0,  0, 0},
          {1, 0,  1,  1, 0},
          {1, 0,  2,  2, 0},
          {1, 0,  3,  3, 0},

          {2, 0, -3, -6, 0},
          {2, 0, -2, -4, 0},
          {2, 0, -1, -2, 0},
          {2, 0,  0,  0, 0},
          {2, 0,  1,  2, 0},
          {2, 0,  2,  4, 0},
          {2, 0,  3,  6, 0},

          {3, 0, -3, -9, 0},
          {3, 0, -2, -6, 0},
          {3, 0, -1, -3, 0},
          {3, 0,  0,  0, 0},
          {3, 0,  1,  3, 0},
          {3, 0,  2,  6, 0},
          {3, 0,  3,  9, 0},

          {3, 333333333, -3, -10, 000000001},
          {3, 333333333, -2,  -7, 333333334},
          {3, 333333333, -1,  -4, 666666667},
          {3, 333333333,  0,   0,         0},
          {3, 333333333,  1,   3, 333333333},
          {3, 333333333,  2,   6, 666666666},
          {3, 333333333,  3,   9, 999999999},
       };
    }

    @Test(dataProvider = "MultipliedBy")
    public void multipliedBy(long seconds, int nanos, int multiplicand, long expectedSeconds, int expectedNanos) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.multipliedBy(multiplicand);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanos);
    }

    @Test
    public void multipliedBy_max() {
        Duration test = Duration.ofSeconds(1);
        assertEquals(test.multipliedBy(Long.MAX_VALUE), Duration.ofSeconds(Long.MAX_VALUE));
    }

    @Test
    public void multipliedBy_min() {
        Duration test = Duration.ofSeconds(1);
        assertEquals(test.multipliedBy(Long.MIN_VALUE), Duration.ofSeconds(Long.MIN_VALUE));
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void multipliedBy_tooBig() {
        Duration test = Duration.ofSeconds(1, 1);
        test.multipliedBy(Long.MAX_VALUE);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void multipliedBy_tooBig_negative() {
        Duration test = Duration.ofSeconds(1, 1);
        test.multipliedBy(Long.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // dividedBy()
    //-----------------------------------------------------------------------
    @DataProvider(name = "DividedBy")
    Object[][] provider_dividedBy() {
       return new Object[][] {
          {-4, 666666667, -3,  1, 111111111},
          {-4, 666666667, -2,  1, 666666666},
          {-4, 666666667, -1,  3, 333333333},
          {-4, 666666667,  1, -4, 666666667},
          {-4, 666666667,  2, -2, 333333334},
          {-4, 666666667,  3, -2, 888888889},

          {-3, 0, -3,  1, 0},
          {-3, 0, -2,  1, 500000000},
          {-3, 0, -1,  3, 0},
          {-3, 0,  1, -3, 0},
          {-3, 0,  2, -2, 500000000},
          {-3, 0,  3, -1, 0},

          {-2, 0, -3,  0, 666666666},
          {-2, 0, -2,  1,         0},
          {-2, 0, -1,  2,         0},
          {-2, 0,  1, -2,         0},
          {-2, 0,  2, -1,         0},
          {-2, 0,  3, -1, 333333334},

          {-1, 0, -3,  0, 333333333},
          {-1, 0, -2,  0, 500000000},
          {-1, 0, -1,  1,         0},
          {-1, 0,  1, -1,         0},
          {-1, 0,  2, -1, 500000000},
          {-1, 0,  3, -1, 666666667},

          {-1, 500000000, -3,  0, 166666666},
          {-1, 500000000, -2,  0, 250000000},
          {-1, 500000000, -1,  0, 500000000},
          {-1, 500000000,  1, -1, 500000000},
          {-1, 500000000,  2, -1, 750000000},
          {-1, 500000000,  3, -1, 833333334},

          {0, 0, -3, 0, 0},
          {0, 0, -2, 0, 0},
          {0, 0, -1, 0, 0},
          {0, 0,  1, 0, 0},
          {0, 0,  2, 0, 0},
          {0, 0,  3, 0, 0},

          {0, 500000000, -3, -1, 833333334},
          {0, 500000000, -2, -1, 750000000},
          {0, 500000000, -1, -1, 500000000},
          {0, 500000000,  1,  0, 500000000},
          {0, 500000000,  2,  0, 250000000},
          {0, 500000000,  3,  0, 166666666},

          {1, 0, -3, -1, 666666667},
          {1, 0, -2, -1, 500000000},
          {1, 0, -1, -1,         0},
          {1, 0,  1,  1,         0},
          {1, 0,  2,  0, 500000000},
          {1, 0,  3,  0, 333333333},

          {2, 0, -3, -1, 333333334},
          {2, 0, -2, -1,         0},
          {2, 0, -1, -2,         0},
          {2, 0,  1,  2,         0},
          {2, 0,  2,  1,         0},
          {2, 0,  3,  0, 666666666},

          {3, 0, -3, -1,         0},
          {3, 0, -2, -2, 500000000},
          {3, 0, -1, -3,         0},
          {3, 0,  1,  3,         0},
          {3, 0,  2,  1, 500000000},
          {3, 0,  3,  1,         0},

          {3, 333333333, -3, -2, 888888889},
          {3, 333333333, -2, -2, 333333334},
          {3, 333333333, -1, -4, 666666667},
          {3, 333333333,  1,  3, 333333333},
          {3, 333333333,  2,  1, 666666666},
          {3, 333333333,  3,  1, 111111111},
       };
    }

    @Test(dataProvider = "DividedBy")
    public void dividedBy(long seconds, int nanos, int divisor, long expectedSeconds, int expectedNanos) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        t = t.dividedBy(divisor);
        assertEquals(t.getSeconds(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanos);
    }

    @Test(dataProvider = "DividedBy", expectedExceptions = ArithmeticException.class)
    public void dividedByZero(long seconds, int nanos, int divisor, long expectedSeconds, int expectedNanos) {
       Duration t = Duration.ofSeconds(seconds, nanos);
       t.dividedBy(0);
       fail(t + " divided by zero did not throw ArithmeticException");
    }

    @Test
    public void dividedBy_max() {
        Duration test = Duration.ofSeconds(Long.MAX_VALUE);
        assertEquals(test.dividedBy(Long.MAX_VALUE), Duration.ofSeconds(1));
    }

    //-----------------------------------------------------------------------
    // negated()
    //-----------------------------------------------------------------------
    @Test
    public void test_negated() {
        assertEquals(Duration.ofSeconds(0).negated(), Duration.ofSeconds(0));
        assertEquals(Duration.ofSeconds(12).negated(), Duration.ofSeconds(-12));
        assertEquals(Duration.ofSeconds(-12).negated(), Duration.ofSeconds(12));
        assertEquals(Duration.ofSeconds(12, 20).negated(), Duration.ofSeconds(-12, -20));
        assertEquals(Duration.ofSeconds(12, -20).negated(), Duration.ofSeconds(-12, 20));
        assertEquals(Duration.ofSeconds(-12, -20).negated(), Duration.ofSeconds(12, 20));
        assertEquals(Duration.ofSeconds(-12, 20).negated(), Duration.ofSeconds(12, -20));
        assertEquals(Duration.ofSeconds(Long.MAX_VALUE).negated(), Duration.ofSeconds(-Long.MAX_VALUE));
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void test_negated_overflow() {
        Duration.ofSeconds(Long.MIN_VALUE).negated();
    }

    //-----------------------------------------------------------------------
    // abs()
    //-----------------------------------------------------------------------
    @Test
    public void test_abs() {
        assertEquals(Duration.ofSeconds(0).abs(), Duration.ofSeconds(0));
        assertEquals(Duration.ofSeconds(12).abs(), Duration.ofSeconds(12));
        assertEquals(Duration.ofSeconds(-12).abs(), Duration.ofSeconds(12));
        assertEquals(Duration.ofSeconds(12, 20).abs(), Duration.ofSeconds(12, 20));
        assertEquals(Duration.ofSeconds(12, -20).abs(), Duration.ofSeconds(12, -20));
        assertEquals(Duration.ofSeconds(-12, -20).abs(), Duration.ofSeconds(12, 20));
        assertEquals(Duration.ofSeconds(-12, 20).abs(), Duration.ofSeconds(12, -20));
        assertEquals(Duration.ofSeconds(Long.MAX_VALUE).abs(), Duration.ofSeconds(Long.MAX_VALUE));
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void test_abs_overflow() {
        Duration.ofSeconds(Long.MIN_VALUE).abs();
    }

    //-----------------------------------------------------------------------
    // toNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_toNanos() {
        Duration test = Duration.ofSeconds(321, 123456789);
        assertEquals(test.toNanos(), 321123456789L);
    }

    @Test
    public void test_toNanos_max() {
        Duration test = Duration.ofSeconds(0, Long.MAX_VALUE);
        assertEquals(test.toNanos(), Long.MAX_VALUE);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void test_toNanos_tooBig() {
        Duration test = Duration.ofSeconds(0, Long.MAX_VALUE).plusNanos(1);
        test.toNanos();
    }

    //-----------------------------------------------------------------------
    // toMillis()
    //-----------------------------------------------------------------------
    @Test
    public void test_toMillis() {
        Duration test = Duration.ofSeconds(321, 123456789);
        assertEquals(test.toMillis(), 321000 + 123);
    }

    @Test
    public void test_toMillis_max() {
        Duration test = Duration.ofSeconds(Long.MAX_VALUE / 1000, (Long.MAX_VALUE % 1000) * 1000000);
        assertEquals(test.toMillis(), Long.MAX_VALUE);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void test_toMillis_tooBig() {
        Duration test = Duration.ofSeconds(Long.MAX_VALUE / 1000, ((Long.MAX_VALUE % 1000) + 1) * 1000000);
        test.toMillis();
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_Duration(
            Duration.ofSeconds(-2L, 0),
            Duration.ofSeconds(-2L, 999999998),
            Duration.ofSeconds(-2L, 999999999),
            Duration.ofSeconds(-1L, 0),
            Duration.ofSeconds(-1L, 1),
            Duration.ofSeconds(-1L, 999999998),
            Duration.ofSeconds(-1L, 999999999),
            Duration.ofSeconds(0L, 0),
            Duration.ofSeconds(0L, 1),
            Duration.ofSeconds(0L, 2),
            Duration.ofSeconds(0L, 999999999),
            Duration.ofSeconds(1L, 0),
            Duration.ofSeconds(2L, 0)
        );
    }

    void doTest_comparisons_Duration(Duration... durations) {
        for (int i = 0; i < durations.length; i++) {
            Duration a = durations[i];
            for (int j = 0; j < durations.length; j++) {
                Duration b = durations[j];
                if (i < j) {
                    assertEquals(a.compareTo(b) < 0, true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertEquals(a.compareTo(b) > 0, true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_compareTo_ObjectNull() {
        Duration a = Duration.ofSeconds(0L, 0);
        a.compareTo(null);
    }

    @Test(expectedExceptions = ClassCastException.class)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void compareToNonDuration() {
       Comparable c = Duration.ofSeconds(0L);
       c.compareTo(new Object());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        Duration test5a = Duration.ofSeconds(5L, 20);
        Duration test5b = Duration.ofSeconds(5L, 20);
        Duration test5n = Duration.ofSeconds(5L, 30);
        Duration test6 = Duration.ofSeconds(6L, 20);

        assertEquals(test5a.equals(test5a), true);
        assertEquals(test5a.equals(test5b), true);
        assertEquals(test5a.equals(test5n), false);
        assertEquals(test5a.equals(test6), false);

        assertEquals(test5b.equals(test5a), true);
        assertEquals(test5b.equals(test5b), true);
        assertEquals(test5b.equals(test5n), false);
        assertEquals(test5b.equals(test6), false);

        assertEquals(test5n.equals(test5a), false);
        assertEquals(test5n.equals(test5b), false);
        assertEquals(test5n.equals(test5n), true);
        assertEquals(test5n.equals(test6), false);

        assertEquals(test6.equals(test5a), false);
        assertEquals(test6.equals(test5b), false);
        assertEquals(test6.equals(test5n), false);
        assertEquals(test6.equals(test6), true);
    }

    @Test
    public void test_equals_null() {
        Duration test5 = Duration.ofSeconds(5L, 20);
        assertEquals(test5.equals(null), false);
    }

    @Test
    public void test_equals_otherClass() {
        Duration test5 = Duration.ofSeconds(5L, 20);
        assertEquals(test5.equals(""), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode() {
        Duration test5a = Duration.ofSeconds(5L, 20);
        Duration test5b = Duration.ofSeconds(5L, 20);
        Duration test5n = Duration.ofSeconds(5L, 30);
        Duration test6 = Duration.ofSeconds(6L, 20);

        assertEquals(test5a.hashCode() == test5a.hashCode(), true);
        assertEquals(test5a.hashCode() == test5b.hashCode(), true);
        assertEquals(test5b.hashCode() == test5b.hashCode(), true);

        assertEquals(test5a.hashCode() == test5n.hashCode(), false);
        assertEquals(test5a.hashCode() == test6.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name = "ToString")
    Object[][] provider_toString() {
        return new Object[][] {
            {0, 0, "PT0S"},
            {0, 1, "PT0.000000001S"},
            {0, 10, "PT0.00000001S"},
            {0, 100, "PT0.0000001S"},
            {0, 1000, "PT0.000001S"},
            {0, 10000, "PT0.00001S"},
            {0, 100000, "PT0.0001S"},
            {0, 1000000, "PT0.001S"},
            {0, 10000000, "PT0.01S"},
            {0, 100000000, "PT0.1S"},
            {0, 120000000, "PT0.12S"},
            {0, 123000000, "PT0.123S"},
            {0, 123400000, "PT0.1234S"},
            {0, 123450000, "PT0.12345S"},
            {0, 123456000, "PT0.123456S"},
            {0, 123456700, "PT0.1234567S"},
            {0, 123456780, "PT0.12345678S"},
            {0, 123456789, "PT0.123456789S"},
            {1, 0, "PT1S"},
            {-1, 0, "PT-1S"},
            {-1, 1000, "PT-0.999999S"},
            {-1, 900000000, "PT-0.1S"},
            
            {60, 0, "PT1M"},
            {3600, 0, "PT1H"},
            {7261, 0, "PT2H1M1S"},
//            {Long.MAX_VALUE, 0, "PT9223372036854775807S"},
//            {Long.MIN_VALUE, 0, "PT-9223372036854775808S"},
        };
    }

    @Test(dataProvider = "ToString")
    public void test_toString(long seconds, int nanos, String expected) {
        Duration t = Duration.ofSeconds(seconds, nanos);
        assertEquals(t.toString(), expected);
    }

}
