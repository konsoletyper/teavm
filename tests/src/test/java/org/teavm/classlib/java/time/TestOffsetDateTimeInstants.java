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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Test OffsetDateTime creation.
 */
@TeaVMTest
public class TestOffsetDateTimeInstants {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_MAX = ZoneOffset.ofHours(18);
    private static final ZoneOffset OFFSET_MIN = ZoneOffset.ofHours(-18);

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_nullInstant() {
        assertThrows(NullPointerException.class, () -> OffsetDateTime.ofInstant((Instant) null, OFFSET_PONE));
    }

    @Test
    public void factory_ofInstant_nullOffset() {
        Instant instant = Instant.ofEpochSecond(0L);
        assertThrows(NullPointerException.class, () -> OffsetDateTime.ofInstant(instant, (ZoneOffset) null));
    }

    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_PONE);
            assertEquals(1970, test.getYear());
            assertEquals(Month.JANUARY, test.getMonth());
            assertEquals(1 + (i >= 23 * 60 * 60 ? 1 : 0), test.getDayOfMonth());
            assertEquals(((i / (60 * 60)) + 1) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
        }
    }

    public void factory_ofInstant_allDaysInCycle() {
        // sanity check using different algorithm
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0),
                ZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            Instant instant = Instant.ofEpochSecond(i * 24L * 60L * 60L);
            OffsetDateTime test = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(expected, test);
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

    @Test
    public void factory_ofInstant_tooLow() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        assertThrows(DateTimeException.class, () -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    public void factory_ofInstant_maxYear() {
        doTest_factory_ofInstant_all(Year.MAX_VALUE - 420, Year.MAX_VALUE);
    }

    @Test
    public void factory_ofInstant_tooBig() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        long year = Year.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        assertThrows(DateTimeException.class, () -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    //-----------------------------------------------------------------------
    public void factory_ofInstant_minWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(Year.MIN_VALUE, test.getYear());
        assertEquals(1, test.getMonth().getValue());
        assertEquals(1, test.getDayOfMonth());
        assertEquals(OFFSET_MIN, test.getOffset());
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    public void factory_ofInstant_minWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days0000to1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(Year.MIN_VALUE, test.getYear());
        assertEquals(1, test.getMonth().getValue());
        assertEquals(1, test.getDayOfMonth());
        assertEquals(OFFSET_MAX, test.getOffset());
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    public void factory_ofInstant_maxWithMinOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(Year.MAX_VALUE, test.getYear());
        assertEquals(12, test.getMonth().getValue());
        assertEquals(31, test.getDayOfMonth());
        assertEquals(OFFSET_MIN, test.getOffset());
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(0, test.getNano());
    }

    public void factory_ofInstant_maxWithMaxOffset() {
        long days0000to1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days0000to1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        OffsetDateTime test = OffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(Year.MAX_VALUE, test.getYear());
        assertEquals(12, test.getMonth().getValue());
        assertEquals(31, test.getDayOfMonth());
        assertEquals(OFFSET_MAX, test.getOffset());
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(0, test.getNano());
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_maxInstantWithMaxOffset() {
        assertThrows(DateTimeException.class, () -> {
            Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
            OffsetDateTime.ofInstant(instant, OFFSET_MAX);
        });
    }

    @Test
    public void factory_ofInstant_maxInstantWithMinOffset() {
        assertThrows(DateTimeException.class, () -> {
            Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
            OffsetDateTime.ofInstant(instant, OFFSET_MIN);
        });
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
                assertEquals(expected, test);
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
    @Test
    public void test_toInstant_19700101() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(0, test.getEpochSecond());
        assertEquals(0, test.getNano());
    }

    @Test
    public void test_toInstant_19700101_oneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 1), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(0, test.getEpochSecond());
        assertEquals(1, test.getNano());
    }

    @Test
    public void test_toInstant_19700101_minusOneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(23, 59, 59, 999999999),
                ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(-1, test.getEpochSecond());
        assertEquals(999999999, test.getNano());
    }

    @Test
    public void test_toInstant_19700102() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(24L * 60L * 60L, test.getEpochSecond());
        assertEquals(0, test.getNano());
    }

    @Test
    public void test_toInstant_19691231() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(-24L * 60L * 60L, test.getEpochSecond());
        assertEquals(0, test.getNano());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochSecond_19700101() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(0, dt.toEpochSecond());
    }

    @Test
    public void test_toEpochSecond_19700101_oneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0, 1), ZoneOffset.UTC);
        assertEquals(0, dt.toEpochSecond());
    }

    @Test
    public void test_toEpochSecond_19700101_minusOneNano() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(23, 59, 59, 999999999),
                ZoneOffset.UTC);
        assertEquals(-1, dt.toEpochSecond());
    }

    @Test
    public void test_toEpochSecond_19700102() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(24L * 60L * 60L, dt.toEpochSecond());
    }

    @Test
    public void test_toEpochSecond_19691231() {
        OffsetDateTime dt = OffsetDateTime.of(LocalDate.of(1969, 12, 31), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(-24L * 60L * 60L, dt.toEpochSecond());
    }

}
