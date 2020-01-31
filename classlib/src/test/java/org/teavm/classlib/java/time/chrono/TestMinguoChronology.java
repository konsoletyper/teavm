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

import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;

@Test
public class TestMinguoChronology {

    //-----------------------------------------------------------------------
    // Chrono.ofName("Minguo")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        TChronology c = TMinguoChronology.INSTANCE;
        TChronology test = TChronology.of("Minguo");
        Assert.assertNotNull(test, "The Minguo calendar could not be found byName");
        Assert.assertEquals(test.getId(), "Minguo", "ID mismatch");
        Assert.assertEquals(test.getCalendarType(), "roc", "Type mismatch");
        Assert.assertEquals(test, c);
    }

    //-----------------------------------------------------------------------
    // creation, toLocalDate()
    //-----------------------------------------------------------------------
    @DataProvider(name="samples")
    Object[][] data_samples() {
        return new Object[][] {
            {TMinguoChronology.INSTANCE.date(1, 1, 1), TLocalDate.of(1912, 1, 1)},
            {TMinguoChronology.INSTANCE.date(1, 1, 2), TLocalDate.of(1912, 1, 2)},
            {TMinguoChronology.INSTANCE.date(1, 1, 3), TLocalDate.of(1912, 1, 3)},

            {TMinguoChronology.INSTANCE.date(2, 1, 1), TLocalDate.of(1913, 1, 1)},
            {TMinguoChronology.INSTANCE.date(3, 1, 1), TLocalDate.of(1914, 1, 1)},
            {TMinguoChronology.INSTANCE.date(3, 12, 6), TLocalDate.of(1914, 12, 6)},
            {TMinguoChronology.INSTANCE.date(4, 1, 1), TLocalDate.of(1915, 1, 1)},
            {TMinguoChronology.INSTANCE.date(4, 7, 3), TLocalDate.of(1915, 7, 3)},
            {TMinguoChronology.INSTANCE.date(4, 7, 4), TLocalDate.of(1915, 7, 4)},
            {TMinguoChronology.INSTANCE.date(5, 1, 1), TLocalDate.of(1916, 1, 1)},
            {TMinguoChronology.INSTANCE.date(100, 3, 3), TLocalDate.of(2011, 3, 3)},
            {TMinguoChronology.INSTANCE.date(101, 10, 28), TLocalDate.of(2012, 10, 28)},
            {TMinguoChronology.INSTANCE.date(101, 10, 29), TLocalDate.of(2012, 10, 29)},
        };
    }

    @Test(dataProvider="samples")
    public void test_toLocalDate(TChronoLocalDate minguo, TLocalDate iso) {
        assertEquals(TLocalDate.from(minguo), iso);
    }

    @Test(dataProvider="samples")
    public void test_fromCalendrical(TChronoLocalDate minguo, TLocalDate iso) {
        assertEquals(TMinguoChronology.INSTANCE.date(iso), minguo);
    }

    @SuppressWarnings("unused")
    @Test(dataProvider="samples")
    public void test_MinguoDate(TChronoLocalDate minguoDate, TLocalDate iso) {
        TChronoLocalDate hd = minguoDate;
        TChronoLocalDateTime<?> hdt = hd.atTime(TLocalTime.NOON);
        TZoneOffset zo = TZoneOffset.ofHours(1);
        TChronoZonedDateTime<?> hzdt = hdt.atZone(zo);
        hdt = hdt.plus(1, TChronoUnit.YEARS);
        hdt = hdt.plus(1, TChronoUnit.MONTHS);
        hdt = hdt.plus(1, TChronoUnit.DAYS);
        hdt = hdt.plus(1, TChronoUnit.HOURS);
        hdt = hdt.plus(1, TChronoUnit.MINUTES);
        hdt = hdt.plus(1, TChronoUnit.SECONDS);
        hdt = hdt.plus(1, TChronoUnit.NANOS);
        TChronoLocalDateTime<?> a2 = hzdt.toLocalDateTime();
        TChronoLocalDate a3 = a2.toLocalDate();
        TChronoLocalDate a5 = hzdt.toLocalDate();
        //TSystem.out.printf(" d: %s, dt: %s; odt: %s; zodt: %s; a4: %s%n", date, hdt, hodt, hzdt, a5);
    }

    @Test()
    public void test_MinguoChrono() {
        TChronoLocalDate h1 = TMinguoChronology.INSTANCE.date(TMinguoEra.ROC, 1, 2, 3);
        TChronoLocalDate h2 = h1;
        TChronoLocalDateTime<?> h3 = h2.atTime(TLocalTime.NOON);
        @SuppressWarnings("unused")
        TChronoZonedDateTime<?> h4 = h3.atZone(TZoneOffset.UTC);
    }

    @DataProvider(name="badDates")
    Object[][] data_badDates() {
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

    @Test(dataProvider="badDates", expectedExceptions=TDateTimeException.class)
    public void test_badDates(int year, int month, int dom) {
        TMinguoChronology.INSTANCE.date(year, month, dom);
    }

    //-----------------------------------------------------------------------
    // with(DateTimeAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        TChronoLocalDate base = TMinguoChronology.INSTANCE.date(2012, 10, 29);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TMinguoChronology.INSTANCE.date(2012, 10, 31));
    }

    @Test
    public void test_adjust2() {
        TChronoLocalDate base = TMinguoChronology.INSTANCE.date(1728, 12, 2);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TMinguoChronology.INSTANCE.date(1728, 12, 31));
    }

    //-----------------------------------------------------------------------
    // TMinguoDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        TChronoLocalDate minguo = TMinguoChronology.INSTANCE.date(99, 1, 4);
        TChronoLocalDate test = minguo.with(TLocalDate.of(2012, 7, 6));
        assertEquals(test, TMinguoChronology.INSTANCE.date(101, 7, 6));
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_adjust_toMonth() {
        TChronoLocalDate minguo = TMinguoChronology.INSTANCE.date(1726, 1, 4);
        minguo.with(TMonth.APRIL);
    }

    //-----------------------------------------------------------------------
    // TLocalDate.with(TMinguoDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToMinguoDate() {
        TChronoLocalDate minguo = TMinguoChronology.INSTANCE.date(101, 10, 29);
        TLocalDate test = TLocalDate.MIN.with(minguo);
        assertEquals(test, TLocalDate.of(2012, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToMinguoDate() {
        TChronoLocalDate minguo = TMinguoChronology.INSTANCE.date(101, 10, 29);
        TLocalDateTime test = TLocalDateTime.MIN.with(minguo);
        assertEquals(test, TLocalDateTime.of(2012, 10, 29, 0, 0));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="toString")
    Object[][] data_toString() {
        return new Object[][] {
            {TMinguoChronology.INSTANCE.date(1, 1, 1), "Minguo ROC 1-01-01"},
            {TMinguoChronology.INSTANCE.date(1728, 10, 28), "Minguo ROC 1728-10-28"},
            {TMinguoChronology.INSTANCE.date(1728, 10, 29), "Minguo ROC 1728-10-29"},
            {TMinguoChronology.INSTANCE.date(1727, 12, 5), "Minguo ROC 1727-12-05"},
            {TMinguoChronology.INSTANCE.date(1727, 12, 6), "Minguo ROC 1727-12-06"},
        };
    }

    @Test(dataProvider="toString")
    public void test_toString(TChronoLocalDate minguo, String expected) {
        assertEquals(minguo.toString(), expected);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(TMinguoChronology.INSTANCE.equals(TMinguoChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(TMinguoChronology.INSTANCE.equals(TIsoChronology.INSTANCE));
    }

}
