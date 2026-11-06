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
package org.teavm.classlib.java.time.chrono;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.IsoChronology;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test.
 */
// TODO: looks like all of these tests don't work on JVM
@Disabled
@TeaVMTest
public class TestHijrahChronology {

    //-----------------------------------------------------------------------
    // Chrono.ofName("Hijrah")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = HijrahChronology.INSTANCE;
        Chronology test = Chronology.of("Hijrah");
        Assertions.assertNotNull(test, "The Hijrah calendar could not be found byName");
        Assertions.assertEquals("Hijrah-umalqura", test.getId(), "ID mismatch");
        Assertions.assertEquals("islamic-umalqura", test.getCalendarType(), "Type mismatch");
        Assertions.assertEquals(c, test);
    }

    //-----------------------------------------------------------------------
    // creation, toLocalDate()
    //-----------------------------------------------------------------------
    static Object[][] data_samples() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.date(1, 1, 1), LocalDate.of(622, 7, 19)},
            {HijrahChronology.INSTANCE.date(1, 1, 2), LocalDate.of(622, 7, 20)},
            {HijrahChronology.INSTANCE.date(1, 1, 3), LocalDate.of(622, 7, 21)},

            {HijrahChronology.INSTANCE.date(2, 1, 1), LocalDate.of(623, 7, 8)},
            {HijrahChronology.INSTANCE.date(3, 1, 1), LocalDate.of(624, 6, 27)},
            {HijrahChronology.INSTANCE.date(3, 12, 6), LocalDate.of(625, 5, 23)},
            {HijrahChronology.INSTANCE.date(4, 1, 1), LocalDate.of(625, 6, 16)},
            {HijrahChronology.INSTANCE.date(4, 7, 3), LocalDate.of(625, 12, 12)},
            {HijrahChronology.INSTANCE.date(4, 7, 4), LocalDate.of(625, 12, 13)},
            {HijrahChronology.INSTANCE.date(5, 1, 1), LocalDate.of(626, 6, 5)},
            {HijrahChronology.INSTANCE.date(1662, 3, 3), LocalDate.of(2234, 4, 3)},
            {HijrahChronology.INSTANCE.date(1728, 10, 28), LocalDate.of(2298, 12, 03)},
            {HijrahChronology.INSTANCE.date(1728, 10, 29), LocalDate.of(2298, 12, 04)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_toLocalDate(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(iso, LocalDate.from(hijrahDate));
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_fromCalendrical(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(hijrahDate, HijrahChronology.INSTANCE.date(iso));
    }

    static Object[][] data_badDates() {
        return new Object[][] {
            {1728, 0, 0},

            {1728, -1, 1},
            {1728, 0, 1},
            {1728, 14, 1},
            {1728, 15, 1},

            {1728, 1, -1},
            {1728, 1, 0},
            {1728, 1, 32},

            {1728, 12, -1},
            {1728, 12, 0},
            {1728, 12, 32},
        };
    }

    @ParameterizedTest
    @MethodSource("data_badDates")
    public void test_badDates(int year, int month, int dom) {
        assertThrows(DateTimeException.class, () -> HijrahChronology.INSTANCE.date(year, month, dom));
    }

    //-----------------------------------------------------------------------
    // getLong(field)
    //-----------------------------------------------------------------------
    @Test
    public void test_alignedDayOfWeekInMonth() {
        for (int dom = 1; dom <= 29; dom++) {
            HijrahDate date = HijrahChronology.INSTANCE.date(1728, 10, dom);
            assertEquals(((dom - 1) / 7) + 1, date.getLong(ALIGNED_WEEK_OF_MONTH));
            assertEquals(((dom - 1) % 7) + 1, date.getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
            date = date.plus(Duration.ofDays(1));
        }
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        ChronoLocalDate base = HijrahChronology.INSTANCE.date(1728, 10, 28);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(HijrahChronology.INSTANCE.date(1728, 10, 29), test);
    }

    @Test
    public void test_adjust2() {
        ChronoLocalDate base = HijrahChronology.INSTANCE.date(1728, 12, 2);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(HijrahChronology.INSTANCE.date(1728, 12, 30), test);
    }

    //-----------------------------------------------------------------------
    // HijrahDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        ChronoLocalDate hijrahDate = HijrahChronology.INSTANCE.date(1726, 1, 4);
        ChronoLocalDate test = hijrahDate.with(LocalDate.of(2012, 7, 6));
        assertEquals(HijrahChronology.INSTANCE.date(1433, 8, 16), test);
    }

    @Test
    public void test_adjust_toMonth() {
        ChronoLocalDate hijrahDate = HijrahChronology.INSTANCE.date(1726, 1, 4);
        assertThrows(DateTimeException.class, () -> hijrahDate.with(Month.APRIL));
    }

    //-----------------------------------------------------------------------
    // LocalDate.with(HijrahDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToHijrahDate() {
        ChronoLocalDate hijrahDate = HijrahChronology.INSTANCE.date(1728, 10, 29);
        LocalDate test = LocalDate.MIN.with(hijrahDate);
        assertEquals(LocalDate.of(2298, 12, 4), test);
    }

    @Test
    public void test_LocalDateTime_adjustToHijrahDate() {
        ChronoLocalDate hijrahDate = HijrahChronology.INSTANCE.date(1728, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(hijrahDate);
        assertEquals(LocalDateTime.of(2298, 12, 4, 0, 0), test);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.date(1, 1, 1), "Hijrah-umalqura AH 1-01-01"},
            {HijrahChronology.INSTANCE.date(1728, 10, 28), "Hijrah-umalqura AH 1728-10-28"},
            {HijrahChronology.INSTANCE.date(1728, 10, 29), "Hijrah-umalqura AH 1728-10-29"},
            {HijrahChronology.INSTANCE.date(1727, 12, 5), "Hijrah-umalqura AH 1727-12-05"},
            {HijrahChronology.INSTANCE.date(1727, 12, 6), "Hijrah-umalqura AH 1727-12-06"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(ChronoLocalDate hijrahDate, String expected) {
        assertEquals(expected, hijrahDate.toString());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(HijrahChronology.INSTANCE.equals(HijrahChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(HijrahChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

}
