/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseEra;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestJapaneseChronology {

    @Test
    public void test_chrono_byName() {

        Chronology c = JapaneseChronology.INSTANCE;
        Chronology test = Chronology.of("Japanese");
        Assert.assertNotNull("The Japanese calendar could not be found byName", test);
        Assert.assertEquals("ID mismatch", test.getId(), "Japanese");
        Assert.assertEquals("Type mismatch", test.getCalendarType(), "japanese");
        Assert.assertEquals(test, c);
    }

    Object[][] data_samples() {

        return new Object[][] { { JapaneseChronology.INSTANCE.date(1890, 3, 3), LocalDate.of(1890, 3, 3) },
        { JapaneseChronology.INSTANCE.date(1890, 10, 28), LocalDate.of(1890, 10, 28) },
        { JapaneseChronology.INSTANCE.date(1890, 10, 29), LocalDate.of(1890, 10, 29) }, };
    }

    @Test
    public void test_toLocalDate() {

        for (Object[] data : data_samples()) {
            ChronoLocalDate jdate = (ChronoLocalDate) data[0];
            LocalDate iso = (LocalDate) data[1];

            assertEquals(LocalDate.from(jdate), iso);
        }
    }

    @Test
    public void test_fromCalendrical() {

        for (Object[] data : data_samples()) {
            ChronoLocalDate jdate = (ChronoLocalDate) data[0];
            LocalDate iso = (LocalDate) data[1];

            assertEquals(JapaneseChronology.INSTANCE.date(iso), jdate);
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
                JapaneseChronology.INSTANCE.date(year, month, dom);
                fail("Expected DateTimeException");
            } catch (DateTimeException e) {
                // expected
            }
        }
    }

    @Test
    public void test_adjust1() {

        ChronoLocalDate base = JapaneseChronology.INSTANCE.date(1890, 10, 29);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(1890, 10, 31));
    }

    @Test
    public void test_adjust2() {

        ChronoLocalDate base = JapaneseChronology.INSTANCE.date(1890, 12, 2);
        ChronoLocalDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(1890, 12, 31));
    }

    @Test
    public void test_adjust_toLocalDate() {

        ChronoLocalDate jdate = JapaneseChronology.INSTANCE.date(1890, 1, 4);
        ChronoLocalDate test = jdate.with(LocalDate.of(2012, 7, 6));
        assertEquals(test, JapaneseChronology.INSTANCE.date(2012, 7, 6));
    }

    @Test(expected = DateTimeException.class)
    public void test_adjust_toMonth() {

        ChronoLocalDate jdate = JapaneseChronology.INSTANCE.date(1890, 1, 4);
        jdate.with(Month.APRIL);
    }

    @Test
    public void test_LocalDate_adjustToJapaneseDate() {

        ChronoLocalDate jdate = JapaneseChronology.INSTANCE.date(1890, 10, 29);
        LocalDate test = LocalDate.MIN.with(jdate);
        assertEquals(test, LocalDate.of(1890, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToJapaneseDate() {

        ChronoLocalDate jdate = JapaneseChronology.INSTANCE.date(1890, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(jdate);
        assertEquals(test, LocalDateTime.of(1890, 10, 29, 0, 0));
    }

    Object[][] data_japaneseEras() {

        return new Object[][] { { JapaneseEra.MEIJI, -1, "Meiji" }, { JapaneseEra.TAISHO, 0, "Taisho" },
        { JapaneseEra.SHOWA, 1, "Showa" }, { JapaneseEra.HEISEI, 2, "Heisei" }, };
    }

    @Test
    public void test_Japanese_Eras() {

        for (Object[] data : data_japaneseEras()) {
            Era era = (Era) data[0];
            int eraValue = (int) data[1];
            String name = (String) data[2];

            assertEquals("EraValue", era.getValue(), eraValue);
            assertEquals("Era Name", era.toString(), name);
            assertEquals("JapaneseChrono.eraOf()", era, JapaneseChronology.INSTANCE.eraOf(eraValue));
            assertEquals(JapaneseEra.valueOf(name), era);
            List<Era> eras = JapaneseChronology.INSTANCE.eras();
            assertTrue("Era is not present in JapaneseChronology.INSTANCE.eras()", eras.contains(era));
        }
    }

    @Test
    public void test_Japanese_badEras() {

        int badEras[] = { -1000, -998, -997, -2, 4, 1000 };
        for (int badEra : badEras) {
            try {
                Era era = JapaneseChronology.INSTANCE.eraOf(badEra);
                fail("JapaneseChronology.eraOf returned " + era + " + for invalid eraValue " + badEra);
            } catch (DateTimeException ex) {
                // ignore expected exception
            }
        }
        try {
            Era era = JapaneseEra.valueOf("Rubbish");
            fail("JapaneseEra.valueOf returned " + era + " + for invalid era name Rubbish");
        } catch (IllegalArgumentException ex) {
            // ignore expected exception
        }
    }

    // @Test
    // public void test_Japanese_registerEra() {
    //
    // try {
    // JapaneseEra.registerEra(JapaneseEra.SHOWA.endDate(), "TestAdditional");
    // fail("JapaneseEra.registerEra should have failed");
    // } catch (DateTimeException ex) {
    // // ignore expected exception
    // }
    // JapaneseEra additional = JapaneseEra.registerEra(LocalDate.of(2100, 1, 1), "TestAdditional");
    // assertEquals(JapaneseEra.of(3), additional);
    // assertEquals(JapaneseEra.valueOf("TestAdditional"), additional);
    // assertEquals(JapaneseEra.values()[4], additional);
    // try {
    // JapaneseEra.registerEra(LocalDate.of(2200, 1, 1), "TestAdditional2");
    // fail("JapaneseEra.registerEra should have failed");
    // } catch (DateTimeException ex) {
    // // ignore expected exception
    // }
    // }

    Object[][] data_toString() {

        return new Object[][] { { JapaneseChronology.INSTANCE.date(1873, 9, 8), "Japanese Meiji 6-09-08" },
        { JapaneseChronology.INSTANCE.date(1912, 7, 29), "Japanese Meiji 45-07-29" },
        { JapaneseChronology.INSTANCE.date(1912, 7, 30), "Japanese Taisho 1-07-30" },
        { JapaneseChronology.INSTANCE.date(1926, 12, 24), "Japanese Taisho 15-12-24" },
        { JapaneseChronology.INSTANCE.date(1926, 12, 25), "Japanese Showa 1-12-25" },
        { JapaneseChronology.INSTANCE.date(1989, 1, 7), "Japanese Showa 64-01-07" },
        { JapaneseChronology.INSTANCE.date(1989, 1, 8), "Japanese Heisei 1-01-08" },
        { JapaneseChronology.INSTANCE.date(2012, 12, 6), "Japanese Heisei 24-12-06" }, };
    }

    @Test
    public void test_toString() {

        for (Object[] data : data_toString()) {
            ChronoLocalDate jdate = (ChronoLocalDate) data[0];
            String expected = (String) data[1];

            assertEquals(jdate.toString(), expected);
        }
    }

    @Test
    public void test_equals_true() {

        assertTrue(JapaneseChronology.INSTANCE.equals(JapaneseChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {

        assertFalse(JapaneseChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

}
