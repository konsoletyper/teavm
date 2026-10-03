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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistEra;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test.
 */
@TeaVMTest
public class TestThaiBuddhistChronology {

    private static final int YDIFF = 543;

    //-----------------------------------------------------------------------
    // Chrono.ofName("ThaiBuddhist")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = ThaiBuddhistChronology.INSTANCE;
        Chronology test = Chronology.of("ThaiBuddhist");
        Assertions.assertNotNull(test, "The ThaiBuddhist calendar could not be found byName");
        Assertions.assertEquals("ThaiBuddhist", test.getId(), "ID mismatch");
        Assertions.assertEquals("buddhist", test.getCalendarType(), "Type mismatch");
        Assertions.assertEquals(c, test);
    }

    //-----------------------------------------------------------------------
    // creation, toLocalDate()
    //-----------------------------------------------------------------------
    static Object[][] data_samples() {
        return new Object[][] {
            {ThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 1), LocalDate.of(1, 1, 1)},
            {ThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 2), LocalDate.of(1, 1, 2)},
            {ThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 3), LocalDate.of(1, 1, 3)},

            {ThaiBuddhistChronology.INSTANCE.date(2 + YDIFF, 1, 1), LocalDate.of(2, 1, 1)},
            {ThaiBuddhistChronology.INSTANCE.date(3 + YDIFF, 1, 1), LocalDate.of(3, 1, 1)},
            {ThaiBuddhistChronology.INSTANCE.date(3 + YDIFF, 12, 6), LocalDate.of(3, 12, 6)},
            {ThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 1, 1), LocalDate.of(4, 1, 1)},
            {ThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 7, 3), LocalDate.of(4, 7, 3)},
            {ThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 7, 4), LocalDate.of(4, 7, 4)},
            {ThaiBuddhistChronology.INSTANCE.date(5 + YDIFF, 1, 1), LocalDate.of(5, 1, 1)},
            {ThaiBuddhistChronology.INSTANCE.date(1662 + YDIFF, 3, 3), LocalDate.of(1662, 3, 3)},
            {ThaiBuddhistChronology.INSTANCE.date(1728 + YDIFF, 10, 28), LocalDate.of(1728, 10, 28)},
            {ThaiBuddhistChronology.INSTANCE.date(1728 + YDIFF, 10, 29), LocalDate.of(1728, 10, 29)},
            {ThaiBuddhistChronology.INSTANCE.date(2555, 8, 29), LocalDate.of(2012, 8, 29)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_toLocalDate(ChronoLocalDate jdate, LocalDate iso) {
        assertEquals(iso, LocalDate.from(jdate));
    }

    @ParameterizedTest
    @MethodSource("data_samples")
    public void test_fromCalendrical(ChronoLocalDate jdate, LocalDate iso) {
        assertEquals(jdate, ThaiBuddhistChronology.INSTANCE.date(iso));
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
        assertThrows(DateTimeException.class, () -> ThaiBuddhistChronology.INSTANCE.date(year, month, dom));
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(1728, 10, 29);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(1728, 10, 31), test);
    }

    @Test
    public void test_adjust2() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(1728, 12, 2);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(1728, 12, 31), test);
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_BE() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(2555, 8, 29);
        ChronoLocalDate test = base.with(YEAR, 2554);
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(2554, 8, 29), test);
    }

    @Test
    public void test_withYear_BBE() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        ChronoLocalDate test = base.with(YEAR_OF_ERA, 2554);
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(-2553, 8, 29), test);
    }

    //-----------------------------------------------------------------------
    // withEra()
    //-----------------------------------------------------------------------
    @Test
    public void test_withEra_BE() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(2555, 8, 29);
        ChronoLocalDate test = base.with(ChronoField.ERA, ThaiBuddhistEra.BE.getValue());
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(2555, 8, 29), test);
    }

    @Test
    public void test_withEra_BBE() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        ChronoLocalDate test = base.with(ChronoField.ERA, ThaiBuddhistEra.BEFORE_BE.getValue());
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29), test);
    }

    @Test
    public void test_withEra_swap() {
        ChronoLocalDate base = ThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        ChronoLocalDate test = base.with(ChronoField.ERA, ThaiBuddhistEra.BE.getValue());
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(2555, 8, 29), test);
    }

    //-----------------------------------------------------------------------
    // BuddhistDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        ChronoLocalDate jdate = ThaiBuddhistChronology.INSTANCE.date(1726, 1, 4);
        ChronoLocalDate test = jdate.with(LocalDate.of(2012, 7, 6));
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(2555, 7, 6), test);
    }

    @Test
    public void test_adjust_toMonth() {
        ChronoLocalDate jdate = ThaiBuddhistChronology.INSTANCE.date(1726, 1, 4);
        assertThrows(DateTimeException.class, () -> jdate.with(Month.APRIL));
    }

    //-----------------------------------------------------------------------
    // LocalDate.with(BuddhistDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToBuddhistDate() {
        ChronoLocalDate jdate = ThaiBuddhistChronology.INSTANCE.date(2555, 10, 29);
        LocalDate test = LocalDate.MIN.with(jdate);
        assertEquals(LocalDate.of(2012, 10, 29), test);
    }

    @Test
    public void test_LocalDateTime_adjustToBuddhistDate() {
        ChronoLocalDate jdate = ThaiBuddhistChronology.INSTANCE.date(2555, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(jdate);
        assertEquals(LocalDateTime.of(2012, 10, 29, 0, 0), test);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
            {ThaiBuddhistChronology.INSTANCE.date(544, 1, 1), "ThaiBuddhist BE 544-01-01"},
            {ThaiBuddhistChronology.INSTANCE.date(2271, 10, 28), "ThaiBuddhist BE 2271-10-28"},
            {ThaiBuddhistChronology.INSTANCE.date(2271, 10, 29), "ThaiBuddhist BE 2271-10-29"},
            {ThaiBuddhistChronology.INSTANCE.date(2270, 12, 5), "ThaiBuddhist BE 2270-12-05"},
            {ThaiBuddhistChronology.INSTANCE.date(2270, 12, 6), "ThaiBuddhist BE 2270-12-06"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(ChronoLocalDate jdate, String expected) {
        assertEquals(expected, jdate.toString());
    }

    //-----------------------------------------------------------------------
    // chronology range(ChronoField)
    //-----------------------------------------------------------------------
    @Test
    public void test_Chrono_range() {
        long minYear = LocalDate.MIN.getYear() + YDIFF;
        long maxYear = LocalDate.MAX.getYear() + YDIFF;
        assertEquals(ValueRange.of(minYear, maxYear), ThaiBuddhistChronology.INSTANCE.range(YEAR));
        assertEquals(ValueRange.of(1, -minYear + 1, maxYear), ThaiBuddhistChronology.INSTANCE.range(YEAR_OF_ERA));

        assertEquals(DAY_OF_MONTH.range(), ThaiBuddhistChronology.INSTANCE.range(DAY_OF_MONTH));
        assertEquals(DAY_OF_YEAR.range(), ThaiBuddhistChronology.INSTANCE.range(DAY_OF_YEAR));
        assertEquals(MONTH_OF_YEAR.range(), ThaiBuddhistChronology.INSTANCE.range(MONTH_OF_YEAR));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(ThaiBuddhistChronology.INSTANCE.equals(ThaiBuddhistChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(ThaiBuddhistChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

}
