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

import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;
import org.teavm.classlib.java.util.TGregorianCalendar;
import org.teavm.classlib.java.util.TTimeZone;

import org.junit.Test;

@Test
public class TestDateTimeUtils {

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");
    private static final TTimeZone PARIS_TZ = TTimeZone.getTimeZone("Europe/Paris");

    //-----------------------------------------------------------------------
    public void test_toInstant_Date() {
        TDate date = new TDate(123456);
        assertEquals(TDateTimeUtils.toInstant(date), TInstant.ofEpochMilli(123456));
    }

    public void test_toDate_Instant() {
        TInstant instant = TInstant.ofEpochMilli(123456);
        assertEquals(TDateTimeUtils.toDate(instant), new TDate(123456));
    }

    //-----------------------------------------------------------------------
    public void test_toInstant_Calendar() {
        TCalendar calendar = TGregorianCalendar.getInstance();
        calendar.setTimeInMillis(123456);
        assertEquals(TDateTimeUtils.toInstant(calendar), TInstant.ofEpochMilli(123456));
    }

    public void test_toZDT_Calendar() {
        TZonedDateTime zdt = TZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        TCalendar calendar = TGregorianCalendar.getInstance(PARIS_TZ);
        calendar.setFirstDayOfWeek(TCalendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.clear();
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        assertEquals(TDateTimeUtils.toZonedDateTime(calendar), zdt);
    }

    public void test_toCalendar_ZDT() {
        TZonedDateTime zdt = TZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        TGregorianCalendar calendar = new TGregorianCalendar(PARIS_TZ);
        calendar.setFirstDayOfWeek(TCalendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        calendar.set(TCalendar.MILLISECOND, 0);
        calendar.setTimeInMillis(calendar.getTimeInMillis());
        calendar.setGregorianChange(new TDate(Long.MIN_VALUE));
        TGregorianCalendar test = TDateTimeUtils.toGregorianCalendar(zdt);
        assertEquals(test, calendar);
    }

    //-----------------------------------------------------------------------
    public void test_toZoneId_TimeZone() {
        assertEquals(TDateTimeUtils.toZoneId(PARIS_TZ), PARIS);
    }

    public void test_toTimeZone_ZoneId() {
        assertEquals(TDateTimeUtils.toTimeZone(PARIS), PARIS_TZ);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalDate_SqlDate() {
        @SuppressWarnings("deprecation")
        java.sql.TDate sqlDate = new java.sql.TDate(2012 - 1900, 6 - 1, 30);
        TLocalDate localDate = TLocalDate.of(2012, 6, 30);
        assertEquals(TDateTimeUtils.toLocalDate(sqlDate), localDate);
    }

    public void test_toSqlDate_LocalDate() {
        @SuppressWarnings("deprecation")
        java.sql.TDate sqlDate = new java.sql.TDate(2012 - 1900, 6 - 1, 30);
        TLocalDate localDate = TLocalDate.of(2012, 6, 30);
        assertEquals(TDateTimeUtils.toSqlDate(localDate), sqlDate);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalTime_SqlTime() {
        @SuppressWarnings("deprecation")
        java.sql.Time sqlTime = new java.sql.Time(11, 30, 40);
        TLocalTime localTime = TLocalTime.of(11, 30, 40);
        assertEquals(TDateTimeUtils.toLocalTime(sqlTime), localTime);
    }

    public void test_toSqlTime_LocalTime() {
        @SuppressWarnings("deprecation")
        java.sql.Time sqlTime = new java.sql.Time(11, 30, 40);
        TLocalTime localTime = TLocalTime.of(11, 30, 40);
        assertEquals(TDateTimeUtils.toSqlTime(localTime), sqlTime);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalDateTime_SqlTimestamp() {
        @SuppressWarnings("deprecation")
        java.sql.TTimestamp sqlDateTime = new java.sql.TTimestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        TLocalDateTime localDateTime = TLocalDateTime.of(2012, 6, 30, 11, 30, 40, 0);
        assertEquals(TDateTimeUtils.toLocalDateTime(sqlDateTime), localDateTime);
    }

    public void test_toSqlTimestamp_LocalDateTime() {
        @SuppressWarnings("deprecation")
        java.sql.TTimestamp sqlDateTime = new java.sql.TTimestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        TLocalDateTime localDateTime = TLocalDateTime.of(2012, 6, 30, 11, 30, 40, 0);
        assertEquals(TDateTimeUtils.toSqlTimestamp(localDateTime), sqlDateTime);
    }

    //-----------------------------------------------------------------------
    public void test_toInstant_SqlTimestamp() {
        @SuppressWarnings("deprecation")
        java.sql.TTimestamp sqlDateTime = new java.sql.TTimestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        assertEquals(TDateTimeUtils.toInstant(sqlDateTime), TInstant.ofEpochMilli(sqlDateTime.getTime()));
    }

    public void test_toSqlTimestamp_Instant() {
        TInstant instant = TInstant.ofEpochMilli(123456);
        java.sql.TTimestamp sqlDateTime = new java.sql.TTimestamp(instant.toEpochMilli());
        assertEquals(TDateTimeUtils.toSqlTimestamp(instant), sqlDateTime);
    }

}
