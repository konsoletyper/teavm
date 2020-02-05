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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;

public class TestJapaneseChronology {

    @Test
    public void test_chrono_byName() {

        TChronology c = TJapaneseChronology.INSTANCE;
        TChronology test = TChronology.of("Japanese");
        Assert.assertNotNull("The Japanese calendar could not be found byName", test);
        Assert.assertEquals("ID mismatch", test.getId(), "Japanese");
        Assert.assertEquals("Type mismatch", test.getCalendarType(), "japanese");
        Assert.assertEquals(test, c);
    }

    Object[][] data_samples() {

        return new Object[][] { { TJapaneseChronology.INSTANCE.date(1890, 3, 3), TLocalDate.of(1890, 3, 3) },
        { TJapaneseChronology.INSTANCE.date(1890, 10, 28), TLocalDate.of(1890, 10, 28) },
        { TJapaneseChronology.INSTANCE.date(1890, 10, 29), TLocalDate.of(1890, 10, 29) }, };
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

            assertEquals(TJapaneseChronology.INSTANCE.date(iso), jdate);
        }
    }

    Object[][] data_badDates() {

        return new Object[][] { { 1728, 0, 0 }, { 1890, 0, 0 },

        { 1890, -1, 1 }, { 1890, 0, 1 }, { 1890, 14, 1 }, { 1890, 15, 1 },

        { 1890, 1, -1 }, { 1890, 1, 0 }, { 1890, 1, 32 },

        { 1890, 12, -1 }, { 1890, 12, 0 }, { 1890, 12, 32 }, };
    }

    @Test
    public void test_badDates() {

        for (Object[] data : data_badDates()) {
            int year = (int) data[0];
            int month = (int) data[1];
            int dom = (int) data[2];

            try {
                TJapaneseChronology.INSTANCE.date(year, month, dom);
                fail("Expected TDateTimeException");
            } catch (TDateTimeException e) {
                // expected
            }
        }
    }

    @Test
    public void test_adjust1() {

        TChronoLocalDate base = TJapaneseChronology.INSTANCE.date(1890, 10, 29);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TJapaneseChronology.INSTANCE.date(1890, 10, 31));
    }

    @Test
    public void test_adjust2() {

        TChronoLocalDate base = TJapaneseChronology.INSTANCE.date(1890, 12, 2);
        TChronoLocalDate test = base.with(TTemporalAdjusters.lastDayOfMonth());
        assertEquals(test, TJapaneseChronology.INSTANCE.date(1890, 12, 31));
    }

    @Test
    public void test_adjust_toLocalDate() {

        TChronoLocalDate jdate = TJapaneseChronology.INSTANCE.date(1890, 1, 4);
        TChronoLocalDate test = jdate.with(TLocalDate.of(2012, 7, 6));
        assertEquals(test, TJapaneseChronology.INSTANCE.date(2012, 7, 6));
    }

    @Test(expected = TDateTimeException.class)
    public void test_adjust_toMonth() {

        TChronoLocalDate jdate = TJapaneseChronology.INSTANCE.date(1890, 1, 4);
        jdate.with(TMonth.APRIL);
    }

    @Test
    public void test_LocalDate_adjustToJapaneseDate() {

        TChronoLocalDate jdate = TJapaneseChronology.INSTANCE.date(1890, 10, 29);
        TLocalDate test = TLocalDate.MIN.with(jdate);
        assertEquals(test, TLocalDate.of(1890, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToJapaneseDate() {

        TChronoLocalDate jdate = TJapaneseChronology.INSTANCE.date(1890, 10, 29);
        TLocalDateTime test = TLocalDateTime.MIN.with(jdate);
        assertEquals(test, TLocalDateTime.of(1890, 10, 29, 0, 0));
    }

    Object[][] data_japaneseEras() {

        return new Object[][] { { TJapaneseEra.MEIJI, -1, "Meiji" }, { TJapaneseEra.TAISHO, 0, "Taisho" },
        { TJapaneseEra.SHOWA, 1, "Showa" }, { TJapaneseEra.HEISEI, 2, "Heisei" }, };
    }

    @Test
    public void test_Japanese_Eras() {

        for (Object[] data : data_japaneseEras()) {
            TEra era = (TEra) data[0];
            int eraValue = (int) data[1];
            String name = (String) data[2];

            assertEquals("EraValue", era.getValue(), eraValue);
            assertEquals("TEra Name", era.toString(), name);
            assertEquals("JapaneseChrono.eraOf()", era, TJapaneseChronology.INSTANCE.eraOf(eraValue));
            assertEquals(TJapaneseEra.valueOf(name), era);
            List<TEra> eras = TJapaneseChronology.INSTANCE.eras();
            assertTrue("TEra is not present in TJapaneseChronology.INSTANCE.eras()", eras.contains(era));
        }
    }

    @Test
    public void test_Japanese_badEras() {

        int badEras[] = { -1000, -998, -997, -2, 4, 1000 };
        for (int badEra : badEras) {
            try {
                TEra era = TJapaneseChronology.INSTANCE.eraOf(badEra);
                fail("TJapaneseChronology.eraOf returned " + era + " + for invalid eraValue " + badEra);
            } catch (TDateTimeException ex) {
                // ignore expected exception
            }
        }
        try {
            TEra era = TJapaneseEra.valueOf("Rubbish");
            fail("TJapaneseEra.valueOf returned " + era + " + for invalid era name Rubbish");
        } catch (IllegalArgumentException ex) {
            // ignore expected exception
        }
    }

    @Test
    public void test_Japanese_registerEra() {

        try {
            TJapaneseEra.registerEra(TJapaneseEra.SHOWA.endDate(), "TestAdditional");
            fail("TJapaneseEra.registerEra should have failed");
        } catch (TDateTimeException ex) {
            // ignore expected exception
        }
        TJapaneseEra additional = TJapaneseEra.registerEra(TLocalDate.of(2100, 1, 1), "TestAdditional");
        assertEquals(TJapaneseEra.of(3), additional);
        assertEquals(TJapaneseEra.valueOf("TestAdditional"), additional);
        assertEquals(TJapaneseEra.values()[4], additional);
        try {
            TJapaneseEra.registerEra(TLocalDate.of(2200, 1, 1), "TestAdditional2");
            fail("TJapaneseEra.registerEra should have failed");
        } catch (TDateTimeException ex) {
            // ignore expected exception
        }
    }

    Object[][] data_toString() {

        return new Object[][] { { TJapaneseChronology.INSTANCE.date(1873, 9, 8), "Japanese Meiji 6-09-08" },
        { TJapaneseChronology.INSTANCE.date(1912, 7, 29), "Japanese Meiji 45-07-29" },
        { TJapaneseChronology.INSTANCE.date(1912, 7, 30), "Japanese Taisho 1-07-30" },
        { TJapaneseChronology.INSTANCE.date(1926, 12, 24), "Japanese Taisho 15-12-24" },
        { TJapaneseChronology.INSTANCE.date(1926, 12, 25), "Japanese Showa 1-12-25" },
        { TJapaneseChronology.INSTANCE.date(1989, 1, 7), "Japanese Showa 64-01-07" },
        { TJapaneseChronology.INSTANCE.date(1989, 1, 8), "Japanese Heisei 1-01-08" },
        { TJapaneseChronology.INSTANCE.date(2012, 12, 6), "Japanese Heisei 24-12-06" }, };
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
    public void test_equals_true() {

        assertTrue(TJapaneseChronology.INSTANCE.equals(TJapaneseChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {

        assertFalse(TJapaneseChronology.INSTANCE.equals(TIsoChronology.INSTANCE));
    }

}
