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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.teavm.classlib.java.util.TLocale;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.THijrahChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.chrono.TJapaneseChronology;
import org.teavm.classlib.java.time.chrono.TMinguoChronology;
import org.teavm.classlib.java.time.chrono.TThaiBuddhistChronology;
import org.teavm.classlib.java.time.temporal.TChronoField;

@Test
public class TestChronology {

    @Before
    public void setUp() {
        // Ensure each of the classes are initialized (until initialization is fixed)
        TChronology c;
        c = THijrahChronology.INSTANCE;
        c = TIsoChronology.INSTANCE;
        c = TJapaneseChronology.INSTANCE;
        c = TMinguoChronology.INSTANCE;
        c = TThaiBuddhistChronology.INSTANCE;
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
                    {"Minguo", "roc", "Minguo TCalendar"},
                    {"ThaiBuddhist", "buddhist", "ThaiBuddhist calendar"},
                };
    }

    @Test(dataProvider = "calendars")
    public void test_getters(String chronoId, String calendarSystemType, String description) {
        TChronology chrono = TChronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        assertEquals(chrono.getId(), chronoId);
        assertEquals(chrono.getCalendarType(), calendarSystemType);
    }

    @Test(dataProvider = "calendars")
    public void test_required_calendars(String chronoId, String calendarSystemType, String description) {
        TChronology chrono = TChronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        chrono = TChronology.of(calendarSystemType);
        assertNotNull(chrono, "Required calendar not found by type: " + chronoId);
        Set<TChronology> cals = TChronology.getAvailableChronologies();
        assertTrue(cals.contains(chrono), "Required calendar not found in set of available calendars");
    }

    @Test
    public void test_calendar_list() {
        Set<TChronology> chronos = TChronology.getAvailableChronologies();
        assertNotNull(chronos, "Required list of calendars must be non-null");
        for (TChronology chrono : chronos) {
            TChronology lookup = TChronology.of(chrono.getId());
            assertNotNull(lookup, "Required calendar not found: " + chrono);
        }
        assertEquals(chronos.size() >= data_of_calendars().length, true, "Required list of calendars too short");
    }

    @Test(dataProvider = "calendars")
    public void test_epoch(String name, String alias, String description) {
        TChronology chrono = TChronology.of(name); // a chronology. In practice this is rarely hardcoded
        TChronoLocalDate date1 = chrono.dateNow();
        long epoch1 = date1.getLong(TChronoField.EPOCH_DAY);
        TChronoLocalDate date2 = date1.with(TChronoField.EPOCH_DAY, epoch1);
        assertEquals(date1, date2, "TDate from epoch day is not same date: " + date1 + " != " + date2);
        long epoch2 = date1.getLong(TChronoField.EPOCH_DAY);
        assertEquals(epoch1, epoch2, "Epoch day not the same: " + epoch1 + " != " + epoch2);
    }

    //-----------------------------------------------------------------------
    // locale based lookup
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarsystemtype")
    Object[][] data_CalendarType() {
        return new Object[][] {
            {THijrahChronology.INSTANCE, "islamic-umalqura"},
            {TIsoChronology.INSTANCE, "iso8601"},
            {TJapaneseChronology.INSTANCE, "japanese"},
            {TMinguoChronology.INSTANCE, "roc"},
            {TThaiBuddhistChronology.INSTANCE, "buddhist"},
        };
    }

    @Test(dataProvider = "calendarsystemtype")
    public void test_getCalendarType(TChronology chrono, String calendarType) {
        assertEquals(chrono.getCalendarType(), calendarType);
    }

//    @Test(dataProvider = "calendarsystemtype")
//    public void test_lookupLocale(TChronology chrono, String calendarType) {
//        TLocale locale = new TLocale.Builder().setLanguage("en").setRegion("CA").setUnicodeLocaleKeyword("ca", calendarType).build();
//        assertEquals(TChronology.ofLocale(locale), chrono);
//    }

    @Test
    public void test_lookupLocale_jp_JP() {
        TChronology test = TChronology.ofLocale(new TLocale("ja", "JP"));
        Assert.assertEquals(test.getId(), "ISO");
        Assert.assertEquals(test, TIsoChronology.INSTANCE);
    }

    @Test
    public void test_lookupLocale_jp_JP_JP() {
        TChronology test = TChronology.ofLocale(new TLocale("ja", "JP", "JP"));
        Assert.assertEquals(test.getId(), "Japanese");
        Assert.assertEquals(test, TJapaneseChronology.INSTANCE);
    }

    //-----------------------------------------------------------------------
    // serialization; serialize and check each calendar system
    //-----------------------------------------------------------------------
    @Test(dataProvider = "calendarsystemtype")
    public void test_chronoSerializationSingleton(TChronology chrono, String calendarType) throws Exception {
        TChronology orginal = chrono;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        TChronology ser = (TChronology) in.readObject();
        assertSame(ser, chrono, "Deserialized Chrono is not the singleton serialized");
    }

}
