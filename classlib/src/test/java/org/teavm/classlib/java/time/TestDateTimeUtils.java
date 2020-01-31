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
package org.threeten.bp;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TestDateTimeUtils {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final TimeZone PARIS_TZ = TimeZone.getTimeZone("Europe/Paris");

    //-----------------------------------------------------------------------
    public void test_toInstant_Date() {
        Date date = new Date(123456);
        assertEquals(DateTimeUtils.toInstant(date), Instant.ofEpochMilli(123456));
    }

    public void test_toDate_Instant() {
        Instant instant = Instant.ofEpochMilli(123456);
        assertEquals(DateTimeUtils.toDate(instant), new Date(123456));
    }

    //-----------------------------------------------------------------------
    public void test_toInstant_Calendar() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(123456);
        assertEquals(DateTimeUtils.toInstant(calendar), Instant.ofEpochMilli(123456));
    }

    public void test_toZDT_Calendar() {
        ZonedDateTime zdt = ZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        Calendar calendar = GregorianCalendar.getInstance(PARIS_TZ);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.clear();
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        assertEquals(DateTimeUtils.toZonedDateTime(calendar), zdt);
    }

    public void test_toCalendar_ZDT() {
        ZonedDateTime zdt = ZonedDateTime.of(2012, 6, 30, 11, 30, 40, 0, PARIS);
        GregorianCalendar calendar = new GregorianCalendar(PARIS_TZ);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(2012, 6 - 1, 30, 11, 30, 40);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(calendar.getTimeInMillis());
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        GregorianCalendar test = DateTimeUtils.toGregorianCalendar(zdt);
        assertEquals(test, calendar);
    }

    //-----------------------------------------------------------------------
    public void test_toZoneId_TimeZone() {
        assertEquals(DateTimeUtils.toZoneId(PARIS_TZ), PARIS);
    }

    public void test_toTimeZone_ZoneId() {
        assertEquals(DateTimeUtils.toTimeZone(PARIS), PARIS_TZ);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalDate_SqlDate() {
        @SuppressWarnings("deprecation")
        java.sql.Date sqlDate = new java.sql.Date(2012 - 1900, 6 - 1, 30);
        LocalDate localDate = LocalDate.of(2012, 6, 30);
        assertEquals(DateTimeUtils.toLocalDate(sqlDate), localDate);
    }

    public void test_toSqlDate_LocalDate() {
        @SuppressWarnings("deprecation")
        java.sql.Date sqlDate = new java.sql.Date(2012 - 1900, 6 - 1, 30);
        LocalDate localDate = LocalDate.of(2012, 6, 30);
        assertEquals(DateTimeUtils.toSqlDate(localDate), sqlDate);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalTime_SqlTime() {
        @SuppressWarnings("deprecation")
        java.sql.Time sqlTime = new java.sql.Time(11, 30, 40);
        LocalTime localTime = LocalTime.of(11, 30, 40);
        assertEquals(DateTimeUtils.toLocalTime(sqlTime), localTime);
    }

    public void test_toSqlTime_LocalTime() {
        @SuppressWarnings("deprecation")
        java.sql.Time sqlTime = new java.sql.Time(11, 30, 40);
        LocalTime localTime = LocalTime.of(11, 30, 40);
        assertEquals(DateTimeUtils.toSqlTime(localTime), sqlTime);
    }

    //-----------------------------------------------------------------------
    public void test_toLocalDateTime_SqlTimestamp() {
        @SuppressWarnings("deprecation")
        java.sql.Timestamp sqlDateTime = new java.sql.Timestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        LocalDateTime localDateTime = LocalDateTime.of(2012, 6, 30, 11, 30, 40, 0);
        assertEquals(DateTimeUtils.toLocalDateTime(sqlDateTime), localDateTime);
    }

    public void test_toSqlTimestamp_LocalDateTime() {
        @SuppressWarnings("deprecation")
        java.sql.Timestamp sqlDateTime = new java.sql.Timestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        LocalDateTime localDateTime = LocalDateTime.of(2012, 6, 30, 11, 30, 40, 0);
        assertEquals(DateTimeUtils.toSqlTimestamp(localDateTime), sqlDateTime);
    }

    //-----------------------------------------------------------------------
    public void test_toInstant_SqlTimestamp() {
        @SuppressWarnings("deprecation")
        java.sql.Timestamp sqlDateTime = new java.sql.Timestamp(2012 - 1900, 6 - 1, 30, 11, 30, 40, 0);
        assertEquals(DateTimeUtils.toInstant(sqlDateTime), Instant.ofEpochMilli(sqlDateTime.getTime()));
    }

    public void test_toSqlTimestamp_Instant() {
        Instant instant = Instant.ofEpochMilli(123456);
        java.sql.Timestamp sqlDateTime = new java.sql.Timestamp(instant.toEpochMilli());
        assertEquals(DateTimeUtils.toSqlTimestamp(instant), sqlDateTime);
    }

}
