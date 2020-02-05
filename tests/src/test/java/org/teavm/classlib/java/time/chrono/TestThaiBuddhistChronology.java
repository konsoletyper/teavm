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
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
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
import org.teavm.classlib.java.time.temporal.TValueRange;

public class TestThaiBuddhistChronology {

    private static final int YDIFF = 543;

    @Test
    public void test_chrono_byName() {

        TChronology c = TThaiBuddhistChronology.INSTANCE;
        TChronology test = TChronology.of("ThaiBuddhist");
        Assert.assertNotNull("The ThaiBuddhist calendar could not be found byName", test);
        Assert.assertEquals("ID mismatch", test.getId(), "ThaiBuddhist");
        Assert.assertEquals("Type mismatch", test.getCalendarType(), "buddhist");
        Assert.assertEquals(test, c);
    }

    Object[][] data_samples() {

        return new Object[][] { { TThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 1), TLocalDate.of(1, 1, 1) },
        { TThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 2), TLocalDate.of(1, 1, 2) },
        { TThaiBuddhistChronology.INSTANCE.date(1 + YDIFF, 1, 3), TLocalDate.of(1, 1, 3) },

        { TThaiBuddhistChronology.INSTANCE.date(2 + YDIFF, 1, 1), TLocalDate.of(2, 1, 1) },
        { TThaiBuddhistChronology.INSTANCE.date(3 + YDIFF, 1, 1), TLocalDate.of(3, 1, 1) },
        { TThaiBuddhistChronology.INSTANCE.date(3 + YDIFF, 12, 6), TLocalDate.of(3, 12, 6) },
        { TThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 1, 1), TLocalDate.of(4, 1, 1) },
        { TThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 7, 3), TLocalDate.of(4, 7, 3) },
        { TThaiBuddhistChronology.INSTANCE.date(4 + YDIFF, 7, 4), TLocalDate.of(4, 7, 4) },
        { TThaiBuddhistChronology.INSTANCE.date(5 + YDIFF, 1, 1), TLocalDate.of(5, 1, 1) },
        { TThaiBuddhistChronology.INSTANCE.date(1662 + YDIFF, 3, 3), TLocalDate.of(1662, 3, 3) },
        { TThaiBuddhistChronology.INSTANCE.date(1728 + YDIFF, 10, 28), TLocalDate.of(1728, 10, 28) },
        { TThaiBuddhistChronology.INSTANCE.date(1728 + YDIFF, 10, 29), TLocalDate.of(1728, 10, 29) },
        { TThaiBuddhistChronology.INSTANCE.date(2555, 8, 29), TLocalDate.of(2012, 8, 29) }, };
    }

    @Test
    public void test_toLocalDate() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate jdate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(TLocalDate.from(jdate), iso);
        }
    }

    @Test
    public void test_fromCalendrical() {

        for (Object[] data : data_samples()) {
            TChronoLocalDate jdate = (TChronoLocalDate) data[0];
            TLocalDate iso = (TLocalDate) data[1];

            assertEquals(TThaiBuddhistChronology.INSTANCE.date(iso), jdate);
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
                TThaiBuddhistChronology.INSTANCE.date(year, month, dom);
                fail("Expected TDateTimeException");
            } catch (TDateTimeException e) {
                // expected
            }
        }
    }

    @Test
    public void test_adjust1() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(1728, 10, 29);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(1728, 10, 31));
    }

    @Test
    public void test_adjust2() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(1728, 12, 2);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(1728, 12, 31));
    }

    @Test
    public void test_withYear_BE() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(2555, 8, 29);
        TChronoLocalDate test = base.with(YEAR, 2554);
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(2554, 8, 29));
    }

    @Test
    public void test_withYear_BBE() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        TChronoLocalDate test = base.with(YEAR_OF_ERA, 2554);
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(-2553, 8, 29));
    }

    @Test
    public void test_withEra_BE() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(2555, 8, 29);
        TChronoLocalDate test = base.with(TChronoField.ERA, TThaiBuddhistEra.BE.getValue());
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(2555, 8, 29));
    }

    @Test
    public void test_withEra_BBE() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        TChronoLocalDate test = base.with(TChronoField.ERA, TThaiBuddhistEra.BEFORE_BE.getValue());
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29));
    }

    @Test
    public void test_withEra_swap() {

        TChronoLocalDate base = TThaiBuddhistChronology.INSTANCE.date(-2554, 8, 29);
        TChronoLocalDate test = base.with(TChronoField.ERA, TThaiBuddhistEra.BE.getValue());
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(2555, 8, 29));
    }

    @Test
    public void test_adjust_toLocalDate() {

        TChronoLocalDate jdate = TThaiBuddhistChronology.INSTANCE.date(1726, 1, 4);
        TChronoLocalDate test = jdate.with(TLocalDate.of(2012, 7, 6));
        assertEquals(test, TThaiBuddhistChronology.INSTANCE.date(2555, 7, 6));
    }

    @Test(expected = TDateTimeException.class)
    public void test_adjust_toMonth() {

        TChronoLocalDate jdate = TThaiBuddhistChronology.INSTANCE.date(1726, 1, 4);
        jdate.with(TMonth.APRIL);
    }

    @Test
    public void test_LocalDate_adjustToBuddhistDate() {

        TChronoLocalDate jdate = TThaiBuddhistChronology.INSTANCE.date(2555, 10, 29);
        TLocalDate test = TLocalDate.MIN.with(jdate);
        assertEquals(test, TLocalDate.of(2012, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToBuddhistDate() {

        TChronoLocalDate jdate = TThaiBuddhistChronology.INSTANCE.date(2555, 10, 29);
        TLocalDateTime test = TLocalDateTime.MIN.with(jdate);
        assertEquals(test, TLocalDateTime.of(2012, 10, 29, 0, 0));
    }

    Object[][] data_toString() {

        return new Object[][] { { TThaiBuddhistChronology.INSTANCE.date(544, 1, 1), "ThaiBuddhist BE 544-01-01" },
        { TThaiBuddhistChronology.INSTANCE.date(2271, 10, 28), "ThaiBuddhist BE 2271-10-28" },
        { TThaiBuddhistChronology.INSTANCE.date(2271, 10, 29), "ThaiBuddhist BE 2271-10-29" },
        { TThaiBuddhistChronology.INSTANCE.date(2270, 12, 5), "ThaiBuddhist BE 2270-12-05" },
        { TThaiBuddhistChronology.INSTANCE.date(2270, 12, 6), "ThaiBuddhist BE 2270-12-06" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : data_toString()) {
            TChronoLocalDate jdate = (TChronoLocalDate) data[0];
            String expected = (String) data[1];

            assertEquals(jdate.toString(), expected);
        }
    }

    @Test
    public void test_Chrono_range() {

        long minYear = TLocalDate.MIN.getYear() + YDIFF;
        long maxYear = TLocalDate.MAX.getYear() + YDIFF;
        assertEquals(TThaiBuddhistChronology.INSTANCE.range(YEAR), TValueRange.of(minYear, maxYear));
        assertEquals(TThaiBuddhistChronology.INSTANCE.range(YEAR_OF_ERA), TValueRange.of(1, -minYear + 1, maxYear));

        assertEquals(TThaiBuddhistChronology.INSTANCE.range(DAY_OF_MONTH), DAY_OF_MONTH.range());
        assertEquals(TThaiBuddhistChronology.INSTANCE.range(DAY_OF_YEAR), DAY_OF_YEAR.range());
        assertEquals(TThaiBuddhistChronology.INSTANCE.range(MONTH_OF_YEAR), MONTH_OF_YEAR.range());
    }

    @Test
    public void test_equals_true() {

        assertTrue(TThaiBuddhistChronology.INSTANCE.equals(TThaiBuddhistChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {

        assertFalse(TThaiBuddhistChronology.INSTANCE.equals(TIsoChronology.INSTANCE));
    }

}
