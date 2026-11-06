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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoEra;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test.
 */
@TeaVMTest
public class TestMinguoChronology {

    //-----------------------------------------------------------------------
    // Chrono.ofName("Minguo")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = MinguoChronology.INSTANCE;
        Chronology test = Chronology.of("Minguo");
        Assertions.assertNotNull(test, "The Minguo calendar could not be found byName");
        Assertions.assertEquals("Minguo", test.getId(), "ID mismatch");
        Assertions.assertEquals("roc", test.getCalendarType(), "Type mismatch");
        Assertions.assertEquals(c, test);
    }

    //-----------------------------------------------------------------------
    // creation, toLocalDate()
    //-----------------------------------------------------------------------
    static Object[][] data_samples() {
        return new Object[][] {
            {MinguoChronology.INSTANCE.date(1, 1, 1), LocalDate.of(1912, 1, 1)},
            {MinguoChronology.INSTANCE.date(1, 1, 2), LocalDate.of(1912, 1, 2)},
            {MinguoChronology.INSTANCE.date(1, 1, 3), LocalDate.of(1912, 1, 3)},

            {MinguoChronology.INSTANCE.date(2, 1, 1), LocalDate.of(1913, 1, 1)},
            {MinguoChronology.INSTANCE.date(3, 1, 1), LocalDate.of(1914, 1, 1)},
            {MinguoChronology.INSTANCE.date(3, 12, 6), LocalDate.of(1914, 12, 6)},
            {MinguoChronology.INSTANCE.date(4, 1, 1), LocalDate.of(1915, 1, 1)},
            {MinguoChronology.INSTANCE.date(4, 7, 3), LocalDate.of(1915, 7, 3)},
            {MinguoChronology.INSTANCE.date(4, 7, 4), LocalDate.of(1915, 7, 4)},
            {MinguoChronology.INSTANCE.date(5, 1, 1), LocalDate.of(1916, 1, 1)},
            {MinguoChronology.INSTANCE.date(100, 3, 3), LocalDate.of(2011, 3, 3)},
            {MinguoChronology.INSTANCE.date(101, 10, 28), LocalDate.of(2012, 10, 28)},
            {MinguoChronology.INSTANCE.date(101, 10, 29), LocalDate.of(2012, 10, 29)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_toLocalDate(ChronoLocalDate minguo, LocalDate iso) {
        assertEquals(iso, LocalDate.from(minguo));
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_fromCalendrical(ChronoLocalDate minguo, LocalDate iso) {
        assertEquals(minguo, MinguoChronology.INSTANCE.date(iso));
    }

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_MinguoDate(ChronoLocalDate minguoDate, LocalDate iso) {
        ChronoLocalDate hd = minguoDate;
        ChronoLocalDateTime<?> hdt = hd.atTime(LocalTime.NOON);
        ZoneOffset zo = ZoneOffset.ofHours(1);
        ChronoZonedDateTime<?> hzdt = hdt.atZone(zo);
        hdt = hdt.plus(1, ChronoUnit.YEARS);
        hdt = hdt.plus(1, ChronoUnit.MONTHS);
        hdt = hdt.plus(1, ChronoUnit.DAYS);
        hdt = hdt.plus(1, ChronoUnit.HOURS);
        hdt = hdt.plus(1, ChronoUnit.MINUTES);
        hdt = hdt.plus(1, ChronoUnit.SECONDS);
        hdt = hdt.plus(1, ChronoUnit.NANOS);
        ChronoLocalDateTime<?> a2 = hzdt.toLocalDateTime();
        ChronoLocalDate a3 = a2.toLocalDate();
        ChronoLocalDate a5 = hzdt.toLocalDate();
        //System.out.printf(" d: %s, dt: %s; odt: %s; zodt: %s; a4: %s%n", date, hdt, hodt, hzdt, a5);
    }

    @Test
    public void test_MinguoChrono() {
        ChronoLocalDate h1 = MinguoChronology.INSTANCE.date(MinguoEra.ROC, 1, 2, 3);
        ChronoLocalDate h2 = h1;
        ChronoLocalDateTime<?> h3 = h2.atTime(LocalTime.NOON);
        @SuppressWarnings("unused")
        ChronoZonedDateTime<?> h4 = h3.atZone(ZoneOffset.UTC);
    }

    static Object[][] data_badDates() {
        return new Object[][] {
            {1912, 0, 0},

            {1912, -1, 1},
            {1912, 0, 1},
            {1912, 14, 1},
            {1912, 15, 1},

            {1912, 1, -1},
            {1912, 1, 0},
            {1912, 1, 32},
            {1912, 2, 29},
            {1912, 2, 30},

            {1912, 12, -1},
            {1912, 12, 0},
            {1912, 12, 32},
            };
    }

    @ParameterizedTest
    @MethodSource("data_badDates")
    public void test_badDates(int year, int month, int dom) {
        assertThrows(DateTimeException.class, () -> MinguoChronology.INSTANCE.date(year, month, dom));
    }

    //-----------------------------------------------------------------------
    // with(DateTimeAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        ChronoLocalDate base = MinguoChronology.INSTANCE.date(2012, 10, 29);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(MinguoChronology.INSTANCE.date(2012, 10, 31), test);
    }

    @Test
    public void test_adjust2() {
        ChronoLocalDate base = MinguoChronology.INSTANCE.date(1728, 12, 2);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(MinguoChronology.INSTANCE.date(1728, 12, 31), test);
    }

    //-----------------------------------------------------------------------
    // MinguoDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        ChronoLocalDate minguo = MinguoChronology.INSTANCE.date(99, 1, 4);
        ChronoLocalDate test = minguo.with(LocalDate.of(2012, 7, 6));
        assertEquals(MinguoChronology.INSTANCE.date(101, 7, 6), test);
    }

    @Test
    public void test_adjust_toMonth() {
        ChronoLocalDate minguo = MinguoChronology.INSTANCE.date(1726, 1, 4);
        assertThrows(DateTimeException.class, () -> minguo.with(Month.APRIL));
    }

    //-----------------------------------------------------------------------
    // LocalDate.with(MinguoDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToMinguoDate() {
        ChronoLocalDate minguo = MinguoChronology.INSTANCE.date(101, 10, 29);
        LocalDate test = LocalDate.MIN.with(minguo);
        assertEquals(LocalDate.of(2012, 10, 29), test);
    }

    @Test
    public void test_LocalDateTime_adjustToMinguoDate() {
        ChronoLocalDate minguo = MinguoChronology.INSTANCE.date(101, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(minguo);
        assertEquals(LocalDateTime.of(2012, 10, 29, 0, 0), test);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
            {MinguoChronology.INSTANCE.date(1, 1, 1), "Minguo ROC 1-01-01"},
            {MinguoChronology.INSTANCE.date(1728, 10, 28), "Minguo ROC 1728-10-28"},
            {MinguoChronology.INSTANCE.date(1728, 10, 29), "Minguo ROC 1728-10-29"},
            {MinguoChronology.INSTANCE.date(1727, 12, 5), "Minguo ROC 1727-12-05"},
            {MinguoChronology.INSTANCE.date(1727, 12, 6), "Minguo ROC 1727-12-06"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(ChronoLocalDate minguo, String expected) {
        assertEquals(expected, minguo.toString());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(MinguoChronology.INSTANCE.equals(MinguoChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(MinguoChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

}
