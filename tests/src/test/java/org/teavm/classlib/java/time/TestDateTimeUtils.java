/*
 * Copyright (c) 2007-present Stephen Colebourne & Michael Nascimento Santos
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
package org.teavm.classlib.java.time;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;
import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;

public class TestDateTimeUtils {

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");

    private static final TimeZone PARIS_TZ = TimeZone.getTimeZone("Europe/Paris");

    @Test
    public void test_toInstant_Date() {

        TDate date = new TDate(123456);
        assertEquals(TDateTimeUtils.toInstant(date), TInstant.ofEpochMilli(123456));
    }

    @Test
    public void test_toDate_Instant() {

        TInstant instant = TInstant.ofEpochMilli(123456);
        assertEquals(TDateTimeUtils.toDate(instant), new TDate(123456));
    }

    @Test
    public void test_toInstant_Calendar() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(123456);
        assertEquals(TDateTimeUtils.toInstant(calendar), TInstant.ofEpochMilli(123456));
    }

    @Test
    public void test_toZDT_Calendar() {

        TZonedDateTime zdt = TZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        Calendar calendar = GregorianCalendar.getInstance(PARIS_TZ);
        calendar.setFirstDayOfWeek(TCalendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.clear();
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        assertEquals(TDateTimeUtils.toZonedDateTime(calendar), zdt);
    }

    @Test
    public void test_toCalendar_ZDT() {

        TZonedDateTime zdt = TZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        GregorianCalendar calendar = new GregorianCalendar(PARIS_TZ);
        calendar.setFirstDayOfWeek(TCalendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        calendar.set(TCalendar.MILLISECOND, 0);
        calendar.setTimeInMillis(calendar.getTimeInMillis());
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        GregorianCalendar test = TDateTimeUtils.toGregorianCalendar(zdt);
        assertEquals(test, calendar);
    }

    @Test
    public void test_toZoneId_TimeZone() {

        assertEquals(TDateTimeUtils.toZoneId(PARIS_TZ), PARIS);
    }

    @Test
    public void test_toTimeZone_ZoneId() {

        assertEquals(TDateTimeUtils.toTimeZone(PARIS), PARIS_TZ);
    }

}
