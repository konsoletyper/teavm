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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;

import org.junit.Assert;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;

public class TestIsoChronology {

    @Test
    public void test_chrono_byName() {

        TChronology c = TIsoChronology.INSTANCE;
        TChronology test = TChronology.of("ISO");
        Assert.assertNotNull("The ISO calendar could not be found byName", test);
        Assert.assertEquals("ID mismatch", test.getId(), "ISO");
        Assert.assertEquals("Type mismatch", test.getCalendarType(), "iso8601");
        Assert.assertEquals(test, c);
    }

    @Test
    public void instanceNotNull() {

        assertNotNull(TIsoChronology.INSTANCE);
    }

    @Test
    public void test_eraOf() {

        assertEquals(TIsoChronology.INSTANCE.eraOf(0), TIsoEra.BCE);
        assertEquals(TIsoChronology.INSTANCE.eraOf(1), TIsoEra.CE);
    }

    Object[][] data_samples() {

        return new Object[][] { { TIsoChronology.INSTANCE.date(1, 7, 8), TLocalDate.of(1, 7, 8) },
        { TIsoChronology.INSTANCE.date(1, 7, 20), TLocalDate.of(1, 7, 20) },
        { TIsoChronology.INSTANCE.date(1, 7, 21), TLocalDate.of(1, 7, 21) },

        { TIsoChronology.INSTANCE.date(2, 7, 8), TLocalDate.of(2, 7, 8) },
        { TIsoChronology.INSTANCE.date(3, 6, 27), TLocalDate.of(3, 6, 27) },
        { TIsoChronology.INSTANCE.date(3, 5, 23), TLocalDate.of(3, 5, 23) },
        { TIsoChronology.INSTANCE.date(4, 6, 16), TLocalDate.of(4, 6, 16) },
        { TIsoChronology.INSTANCE.date(4, 7, 3), TLocalDate.of(4, 7, 3) },
        { TIsoChronology.INSTANCE.date(4, 7, 4), TLocalDate.of(4, 7, 4) },
        { TIsoChronology.INSTANCE.date(5, 1, 1), TLocalDate.of(5, 1, 1) },
        { TIsoChronology.INSTANCE.date(1727, 3, 3), TLocalDate.of(1727, 3, 3) },
        { TIsoChronology.INSTANCE.date(1728, 10, 28), TLocalDate.of(1728, 10, 28) },
        { TIsoChronology.INSTANCE.date(2012, 10, 29), TLocalDate.of(2012, 10, 29) }, };
    }

    @Test
    public void test_toLocalDate() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate isoDate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(TLocalDate.from(isoDate), iso);
        }
    }

    @Test
    public void test_fromCalendrical() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate isoDate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(TIsoChronology.INSTANCE.date(iso), isoDate);
        }
    }

    Object[][] data_badDates() {

        return new Object[][] { { 2012, 0, 0 },

        { 2012, -1, 1 }, { 2012, 0, 1 }, { 2012, 14, 1 }, { 2012, 15, 1 },

        { 2012, 1, -1 }, { 2012, 1, 0 }, { 2012, 1, 32 },

        { 2012, 12, -1 }, { 2012, 12, 0 }, { 2012, 12, 32 }, };
    }

    @Test
    public void test_badDates() {

        for (Object[] data : data_badDates()) {
            int year = (int) data[0];
            int month = (int) data[1];
            int dom = (int) data[2];

            try {
                TIsoChronology.INSTANCE.date(year, month, dom);
                fail("Expected TDateTimeException");
            } catch (TDateTimeException e) {
                // expected
            }
        }
    }

    @Test
    public void test_date_withEra() {

        int year = 5;
        int month = 5;
        int dayOfMonth = 5;
        TChronoLocalDate test = TIsoChronology.INSTANCE.date(TIsoEra.BCE, year, month, dayOfMonth);
        assertEquals(test.getEra(), TIsoEra.BCE);
        assertEquals(test.get(TChronoField.YEAR_OF_ERA), year);
        assertEquals(test.get(TChronoField.MONTH_OF_YEAR), month);
        assertEquals(test.get(TChronoField.DAY_OF_MONTH), dayOfMonth);

        assertEquals(test.get(YEAR), 1 + (-1 * year));
        assertEquals(test.get(ERA), 0);
        assertEquals(test.get(YEAR_OF_ERA), year);
    }

    @Test(expected = ClassCastException.class)
    public void test_date_withEra_withWrongEra() {

        TIsoChronology.INSTANCE.date(THijrahEra.AH, 1, 1, 1);
    }

    @Test
    public void test_adjust1() {

        TChronoLocalDate base = TIsoChronology.INSTANCE.date(1728, 10, 28);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TIsoChronology.INSTANCE.date(1728, 10, 31));
    }

    @Test
    public void test_adjust2() {

        TChronoLocalDate base = TIsoChronology.INSTANCE.date(1728, 12, 2);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TIsoChronology.INSTANCE.date(1728, 12, 31));
    }

    @Test
    public void test_adjust_toLocalDate() {

        TChronoLocalDate isoDate = TIsoChronology.INSTANCE.date(1726, 1, 4);
        TChronoLocalDate test = isoDate.with(TLocalDate.of(2012, 7, 6));
        assertEquals(test, TIsoChronology.INSTANCE.date(2012, 7, 6));
    }

    @Test
    public void test_adjust_toMonth() {

        TChronoLocalDate isoDate = TIsoChronology.INSTANCE.date(1726, 1, 4);
        assertEquals(TIsoChronology.INSTANCE.date(1726, 4, 4), isoDate.with(TMonth.APRIL));
    }

    @Test
    public void test_LocalDate_adjustToISODate() {

        TChronoLocalDate isoDate = TIsoChronology.INSTANCE.date(1728, 10, 29);
        TLocalDate test = TLocalDate.MIN.with(isoDate);
        assertEquals(test, TLocalDate.of(1728, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToISODate() {

        TChronoLocalDate isoDate = TIsoChronology.INSTANCE.date(1728, 10, 29);
        TLocalDateTime test = TLocalDateTime.MIN.with(isoDate);
        assertEquals(test, TLocalDateTime.of(1728, 10, 29, 0, 0));
    }

    Object[][] leapYearInformation() {

        return new Object[][] { { 2000, true }, { 1996, true }, { 1600, true },

        { 1900, false }, { 2100, false },

        { -500, false }, { -400, true }, { -300, false }, { -100, false }, { -5, false }, { -4, true }, { -3, false },
        { -2, false }, { -1, false }, { 0, true }, { 1, false }, { 3, false }, { 4, true }, { 5, false },
        { 100, false }, { 300, false }, { 400, true }, { 500, false }, };
    }

    @Test
    public void test_isLeapYear() {

        for (Object[] data : leapYearInformation()) {
            int year = (int) data[0];
            boolean isLeapYear = (boolean) data[1];

            assertEquals(TIsoChronology.INSTANCE.isLeapYear(year), isLeapYear);
        }
    }

    @Test
    public void test_now() {

        assertEquals(TLocalDate.from(TIsoChronology.INSTANCE.dateNow()), TLocalDate.now());
    }

    Object[][] data_toString() {

        return new Object[][] { { TIsoChronology.INSTANCE.date(1, 1, 1), "0001-01-01" },
        { TIsoChronology.INSTANCE.date(1728, 10, 28), "1728-10-28" },
        { TIsoChronology.INSTANCE.date(1728, 10, 29), "1728-10-29" },
        { TIsoChronology.INSTANCE.date(1727, 12, 5), "1727-12-05" },
        { TIsoChronology.INSTANCE.date(1727, 12, 6), "1727-12-06" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : data_toString()) {
            TChronoLocalDate isoDate = (TChronoLocalDate) data[0];
            String expected = (String) data[1];

            assertEquals(isoDate.toString(), expected);
        }
    }

    @Test
    public void test_equals_true() {

        assertTrue(TIsoChronology.INSTANCE.equals(TIsoChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {

        assertFalse(TIsoChronology.INSTANCE.equals(THijrahChronology.INSTANCE));
    }

}
