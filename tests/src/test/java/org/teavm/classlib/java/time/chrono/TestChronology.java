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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Set;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Chrono class.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestChronology {

    @BeforeMethod
    public void setUp() {
        // Ensure each of the classes are initialized (until initialization is fixed)
        Chronology c;
        c = HijrahChronology.INSTANCE;
        c = IsoChronology.INSTANCE;
        c = JapaneseChronology.INSTANCE;
        c = MinguoChronology.INSTANCE;
        c = ThaiBuddhistChronology.INSTANCE;
        c.toString();  // avoids variable being marked as unused
    }

    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Object[][] data_of_calendars() {
        return new Object[][] {
                    {"Hijrah-umalqura", "islamic-umalqura", "Hijrah calendar"},
                    {"ISO", "iso8601", "ISO calendar"},
                    {"Japanese", "japanese", "Japanese calendar"},
                    {"Minguo", "roc", "Minguo Calendar"},
                    {"ThaiBuddhist", "buddhist", "ThaiBuddhist calendar"},
                };
    }

    @Test(dataProvider = "calendars")
    public void test_getters(String chronoId, String calendarSystemType, String description) {
        Chronology chrono = Chronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        assertEquals(chrono.getId(), chronoId);
        assertEquals(chrono.getCalendarType(), calendarSystemType);
    }

    @Test(dataProvider = "calendars")
    public void test_required_calendars(String chronoId, String calendarSystemType, String description) {
        Chronology chrono = Chronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        chrono = Chronology.of(calendarSystemType);
        assertNotNull(chrono, "Required calendar not found by type: " + chronoId);
        Set<Chronology> cals = Chronology.getAvailableChronologies();
        assertTrue(cals.contains(chrono), "Required calendar not found in set of available calendars");
    }

    @Test
    public void test_calendar_list() {
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        assertNotNull(chronos, "Required list of calendars must be non-null");
        for (Chronology chrono : chronos) {
            Chronology lookup = Chronology.of(chrono.getId());
            assertNotNull(lookup, "Required calendar not found: " + chrono);
        }
        assertEquals(chronos.size() >= data_of_calendars().length, true, "Required list of calendars too short");
    }

    /**
     * Compute the number of days from the Epoch and compute the date from the number of days.
     */
    @Test(dataProvider = "calendars")
    public void test_epoch(String name, String alias, String description) {
        Chronology chrono = Chronology.of(name); // a chronology. In practice this is rarely hardcoded
        ChronoLocalDate date1 = chrono.dateNow();
        long epoch1 = date1.getLong(ChronoField.EPOCH_DAY);
        ChronoLocalDate date2 = date1.with(ChronoField.EPOCH_DAY, epoch1);
        assertEquals(date1, date2, "Date from epoch day is not same date: " + date1 + " != " + date2);
        long epoch2 = date1.getLong(ChronoField.EPOCH_DAY);
        assertEquals(epoch1, epoch2, "Epoch day not the same: " + epoch1 + " != " + epoch2);
    }

    //-----------------------------------------------------------------------
    // locale based lookup
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarsystemtype")
    Object[][] data_CalendarType() {
        return new Object[][] {
            {HijrahChronology.INSTANCE, "islamic-umalqura"},
            {IsoChronology.INSTANCE, "iso8601"},
            {JapaneseChronology.INSTANCE, "japanese"},
            {MinguoChronology.INSTANCE, "roc"},
            {ThaiBuddhistChronology.INSTANCE, "buddhist"},
        };
    }

    @Test(dataProvider = "calendarsystemtype")
    public void test_getCalendarType(Chronology chrono, String calendarType) {
        assertEquals(chrono.getCalendarType(), calendarType);
    }

    @Test
    public void test_lookupLocale_jp_JP() {
        Chronology test = Chronology.ofLocale(new Locale("ja", "JP"));
        Assert.assertEquals(test.getId(), "ISO");
        Assert.assertEquals(test, IsoChronology.INSTANCE);
    }

    @Test
    public void test_lookupLocale_jp_JP_JP() {
        Chronology test = Chronology.ofLocale(new Locale("ja", "JP", "JP"));
        Assert.assertEquals(test.getId(), "Japanese");
        Assert.assertEquals(test, JapaneseChronology.INSTANCE);
    }
}
