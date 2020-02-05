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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class TestChronology {

    @Before
    public void setUp() {

        // Ensure each of the classes are initialized (until initialization is fixed)
        Chronology c;
        c = HijrahChronology.INSTANCE;
        c = IsoChronology.INSTANCE;
        c = JapaneseChronology.INSTANCE;
        c = MinguoChronology.INSTANCE;
        c = ThaiBuddhistChronology.INSTANCE;
        c.toString(); // avoids variable being marked as unused
    }

    Object[][] data_of_calendars() {

        return new Object[][] { { "Hijrah-umalqura", "islamic-umalqura", "Hijrah calendar" },
        { "ISO", "iso8601", "ISO calendar" }, { "Japanese", "japanese", "Japanese calendar" },
        { "Minguo", "roc", "Minguo TCalendar" }, { "ThaiBuddhist", "buddhist", "ThaiBuddhist calendar" }, };
    }

    public void test_getters() {

        for (Object[] data : data_of_calendars()) {
            String chronoId = (String) data[0];
            String calendarSystemType = (String) data[1];
            String description = (String) data[2];

            Chronology chrono = Chronology.of(chronoId);
            assertNotNull("Required calendar not found by ID: " + chronoId, chrono);
            assertEquals(chrono.getId(), chronoId);
            assertEquals(chrono.getCalendarType(), calendarSystemType);
        }
    }

    @Test
    public void test_required_calendars() {

        for (Object[] data : data_of_calendars()) {
            String chronoId = (String) data[0];
            String calendarSystemType = (String) data[1];

            Chronology chrono = Chronology.of(chronoId);
            assertNotNull("Required calendar not found by ID: " + chronoId, chrono);
            chrono = Chronology.of(calendarSystemType);
            assertNotNull("Required calendar not found by ID: " + chronoId, chrono);
            Set<Chronology> cals = Chronology.getAvailableChronologies();
            assertTrue("Required calendar not found in set of available calendars", cals.contains(chrono));
        }
    }

    @Test
    public void test_calendar_list() {

        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        assertNotNull("Required list of calendars must be non-null", chronos);
        for (Chronology chrono : chronos) {
            Chronology lookup = Chronology.of(chrono.getId());
            assertNotNull("Required calendar not found: " + chrono, lookup);
        }
        assertEquals("Required list of calendars too short", chronos.size() >= data_of_calendars().length, true);
    }

    @Test
    public void test_epoch() {

        for (Object[] data : data_of_calendars()) {
            String name = (String) data[0];

            Chronology chrono = Chronology.of(name); // a chronology. In practice this is rarely hardcoded
            ChronoLocalDate date1 = chrono.dateNow();
            long epoch1 = date1.getLong(ChronoField.EPOCH_DAY);
            ChronoLocalDate date2 = date1.with(ChronoField.EPOCH_DAY, epoch1);
            assertEquals("Date from epoch day is not same date: " + date1 + " != " + date2, date1, date2);
            long epoch2 = date1.getLong(ChronoField.EPOCH_DAY);
            assertEquals("Epoch day not the same: " + epoch1 + " != " + epoch2, epoch1, epoch2);
        }
    }

    Object[][] data_CalendarType() {

        return new Object[][] { { HijrahChronology.INSTANCE, "islamic-umalqura" },
        { IsoChronology.INSTANCE, "iso8601" }, { JapaneseChronology.INSTANCE, "japanese" },
        { MinguoChronology.INSTANCE, "roc" }, { ThaiBuddhistChronology.INSTANCE, "buddhist" }, };
    }

    @Test
    public void test_getCalendarType() {

        for (Object[] data : data_CalendarType()) {
            Chronology chrono = (Chronology) data[0];
            String calendarType = (String) data[1];

            assertEquals(chrono.getCalendarType(), calendarType);
        }
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
