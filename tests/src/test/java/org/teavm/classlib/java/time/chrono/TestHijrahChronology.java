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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;

import org.junit.Assert;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;

public class TestHijrahChronology {

    @Test
    public void test_chrono_byName() {

        TChronology c = THijrahChronology.INSTANCE;
        TChronology test = TChronology.of("Hijrah");
        Assert.assertNotNull("The Hijrah calendar could not be found byName", test);
        Assert.assertEquals("ID mismatch", test.getId(), "Hijrah-umalqura");
        Assert.assertEquals("Type mismatch", test.getCalendarType(), "islamic-umalqura");
        Assert.assertEquals(test, c);
    }

    Object[][] data_samples() {

        return new Object[][] { { THijrahChronology.INSTANCE.date(1, 1, 1), TLocalDate.of(622, 7, 19) },
        { THijrahChronology.INSTANCE.date(1, 1, 2), TLocalDate.of(622, 7, 20) },
        { THijrahChronology.INSTANCE.date(1, 1, 3), TLocalDate.of(622, 7, 21) },

        { THijrahChronology.INSTANCE.date(2, 1, 1), TLocalDate.of(623, 7, 8) },
        { THijrahChronology.INSTANCE.date(3, 1, 1), TLocalDate.of(624, 6, 27) },
        { THijrahChronology.INSTANCE.date(3, 12, 6), TLocalDate.of(625, 5, 23) },
        { THijrahChronology.INSTANCE.date(4, 1, 1), TLocalDate.of(625, 6, 16) },
        { THijrahChronology.INSTANCE.date(4, 7, 3), TLocalDate.of(625, 12, 12) },
        { THijrahChronology.INSTANCE.date(4, 7, 4), TLocalDate.of(625, 12, 13) },
        { THijrahChronology.INSTANCE.date(5, 1, 1), TLocalDate.of(626, 6, 5) },
        { THijrahChronology.INSTANCE.date(1662, 3, 3), TLocalDate.of(2234, 4, 3) },
        { THijrahChronology.INSTANCE.date(1728, 10, 28), TLocalDate.of(2298, 12, 03) },
        { THijrahChronology.INSTANCE.date(1728, 10, 29), TLocalDate.of(2298, 12, 04) }, };
    }

    @Test
    public void test_toLocalDate() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate hijrahDate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(TLocalDate.from(hijrahDate), iso);
        }
    }

    @Test
    public void test_fromCalendrical() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate hijrahDate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(THijrahChronology.INSTANCE.date(iso), hijrahDate);
        }
    }

    Object[][] data_badDates() {

        return new Object[][] { { 1728, 0, 0 },

        { 1728, -1, 1 }, { 1728, 0, 1 }, { 1728, 14, 1 }, { 1728, 15, 1 },

        { 1728, 1, -1 }, { 1728, 1, 0 }, { 1728, 1, 32 },

        { 1728, 12, -1 }, { 1728, 12, 0 }, { 1728, 12, 32 }, };
    }

    @Test
    public void test_badDates() {

        for (Object[] data : data_badDates()) {
            int year = (int) data[0];
            int month = (int) data[1];
            int dom = (int) data[2];

            try {
                THijrahChronology.INSTANCE.date(year, month, dom);
                fail("Expected TDateTimeException");
            } catch (TDateTimeException e) {
                // expected
            }
        }
    }

    @Test
    public void test_alignedDayOfWeekInMonth() {

        for (int dom = 1; dom <= 29; dom++) {
            THijrahDate date = THijrahChronology.INSTANCE.date(1728, 10, dom);
            assertEquals(date.getLong(ALIGNED_WEEK_OF_MONTH), ((dom - 1) / 7) + 1);
            assertEquals(date.getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH), ((dom - 1) % 7) + 1);
            date = date.plusDays(1);
        }
    }

    @Test
    public void test_adjust1() {

        TChronoLocalDate base = THijrahChronology.INSTANCE.date(1728, 10, 28);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, THijrahChronology.INSTANCE.date(1728, 10, 29));
    }

    @Test
    public void test_adjust2() {

        TChronoLocalDate base = THijrahChronology.INSTANCE.date(1728, 12, 2);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, THijrahChronology.INSTANCE.date(1728, 12, 30));
    }

    @Test
    public void test_adjust_toLocalDate() {

        TChronoLocalDate hijrahDate = THijrahChronology.INSTANCE.date(1726, 1, 4);
        TChronoLocalDate test = hijrahDate.with(TLocalDate.of(2012, 7, 6));
        assertEquals(test, THijrahChronology.INSTANCE.date(1433, 8, 16));
    }

    @Test(expected = TDateTimeException.class)
    public void test_adjust_toMonth() {

        TChronoLocalDate hijrahDate = THijrahChronology.INSTANCE.date(1726, 1, 4);
        hijrahDate.with(TMonth.APRIL);
    }

    @Test
    public void test_LocalDate_adjustToHijrahDate() {

        TChronoLocalDate hijrahDate = THijrahChronology.INSTANCE.date(1728, 10, 29);
        TLocalDate test = TLocalDate.MIN.with(hijrahDate);
        assertEquals(test, TLocalDate.of(2298, 12, 4));
    }

    @Test
    public void test_LocalDateTime_adjustToHijrahDate() {

        TChronoLocalDate hijrahDate = THijrahChronology.INSTANCE.date(1728, 10, 29);
        TLocalDateTime test = TLocalDateTime.MIN.with(hijrahDate);
        assertEquals(test, TLocalDateTime.of(2298, 12, 4, 0, 0));
    }

    Object[][] data_toString() {

        return new Object[][] { { THijrahChronology.INSTANCE.date(1, 1, 1), "Hijrah-umalqura AH 1-01-01" },
        { THijrahChronology.INSTANCE.date(1728, 10, 28), "Hijrah-umalqura AH 1728-10-28" },
        { THijrahChronology.INSTANCE.date(1728, 10, 29), "Hijrah-umalqura AH 1728-10-29" },
        { THijrahChronology.INSTANCE.date(1727, 12, 5), "Hijrah-umalqura AH 1727-12-05" },
        { THijrahChronology.INSTANCE.date(1727, 12, 6), "Hijrah-umalqura AH 1727-12-06" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : data_toString()) {
            TChronoLocalDate hijrahDate = (TChronoLocalDate) data[0];
            String expected = (String) data[1];

            assertEquals(hijrahDate.toString(), expected);
        }
    }

    @Test
    public void test_equals_true() {

        assertTrue(THijrahChronology.INSTANCE.equals(THijrahChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {

        assertFalse(THijrahChronology.INSTANCE.equals(TIsoChronology.INSTANCE));
    }

}
