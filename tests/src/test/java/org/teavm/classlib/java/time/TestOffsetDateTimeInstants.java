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

import static org.testng.Assert.assertEquals;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.Test;

/**
 * Test OffsetDateTime creation.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestOffsetDateTimeInstants {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_MAX = ZoneOffset.ofHours(18);
    private static final ZoneOffset OFFSET_MIN = ZoneOffset.ofHours(-18);

    //-----------------------------------------------------------------------
    @Test(expectedExceptions = NullPointerException.class)
    public void factory_ofInstant_nullInstant() {
        OffsetDateTime.ofInstant((Instant) null, OFFSET_PONE);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_ofInstant_nullOffset() {
        Instant instant = Instant.ofEpochSecond(0L);
        OffsetDateTime.ofInstant(instant, (ZoneOffset) null);
    }

    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_PONE);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), 1 + (i >= 23 * 60 * 60 ? 1 : 0));
            assertEquals(test.getHour(), ((i / (60 * 60)) + 1) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
        }
    }

    public void factory_ofInstant_allDaysInCycle() {
        // sanity check using different algorithm
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0),
                ZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            Instant instant = Instant.ofEpochSecond(i * 24L * 60L * 60L);
            OffsetDateTime test = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(test, expected);
            expected = expected.plusDays(1);
        }
    }

    public void factory_ofInstant_history() {
        doTest_factory_ofInstant_all(-2820, 2820);
    }

    //-----------------------------------------------------------------------
    public void factory_ofInstant_minYear() {
        doTest_factory_ofInstant_all(Year.MIN_VALUE, Year.MIN_VALUE + 420);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_ofInstant_tooLow() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public void factory_ofInstant_maxYear() {
        doTest_factory_ofInstant_all(Year.MAX_VALUE - 420, Year.MAX_VALUE);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_ofInstant_tooBig() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        long year = Year.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    //-----------------------------------------------------------------------
    public void factory_ofInstant_minWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), Year.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    public void factory_ofInstant_minWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), Year.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    public void factory_ofInstant_maxWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), Year.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    public void factory_ofInstant_maxWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), Year.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions = DateTimeException.class)
    public void factory_ofInstant_maxInstantWithMaxOffset() {
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
        OffsetDateTime.ofInstant(instant, OFFSET_MAX);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_ofInstant_maxInstantWithMinOffset() {
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
        OffsetDateTime.ofInstant(instant, OFFSET_MIN);
    }

    //-----------------------------------------------------------------------
    private void doTest_factory_ofInstant_all(long minYear, long maxYear) {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int minOffset = minYear <= 0 ? 0 : 3;
        int maxOffset = maxYear <= 0 ? 0 : 3;
        long minDays = (minYear * 365L + ((minYear + minOffset) / 4L - (minYear + minOffset) / 100L
                + (minYear + minOffset) / 400L)) - days0000to1970;
        long maxDays = (maxYear * 365L + ((maxYear + maxOffset) / 4L - (maxYear + maxOffset) / 100L
                + (maxYear + maxOffset) / 400L)) + 365L - days0000to1970;

        final LocalDate maxDate = LocalDate.of(Year.MAX_VALUE, 12, 31);
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of((int) minYear, 1, 1), LocalTime.of(0, 0, 0, 0),
                ZoneOffset.UTC);
        for (long i = minDays; i < maxDays; i++) {
            Instant instant = Instant.ofEpochSecond(i * 24L * 60L * 60L);
            try {
                OffsetDateTime test = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
                assertEquals(test, expected);
                if (!expected.toLocalDate().equals(maxDate)) {
                    expected = expected.plusDays(1);
                }
            } catch (RuntimeException ex) {
                System.out.println("RuntimeException: " + i + " " + expected);
                throw ex;
            } catch (Error ex) {
                System.out.println("Error: " + i + " " + expected);
                throw ex;
            }
        }
    }

    //-----------------------------------------------------------------------
    public void test_toInstant_19700101() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    public void test_toInstant_19700101_oneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 1), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 0);
        assertEquals(test.getNano(), 1);
    }

    public void test_toInstant_19700101_minusOneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(23, 59, 59, 999999999),
                ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), -1);
        assertEquals(test.getNano(), 999999999);
    }

    public void test_toInstant_19700102() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 24L * 60L * 60L);
        assertEquals(test.getNano(), 0);
    }

    public void test_toInstant_19691231() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), -24L * 60L * 60L);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    public void test_toEpochSecond_19700101() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 0);
    }

    public void test_toEpochSecond_19700101_oneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 1), ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 0);
    }

    public void test_toEpochSecond_19700101_minusOneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(23, 59, 59, 999999999),
                ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), -1);
    }

    public void test_toEpochSecond_19700102() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 24L * 60L * 60L);
    }

    public void test_toEpochSecond_19691231() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), -24L * 60L * 60L);
    }

}
