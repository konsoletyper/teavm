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
package org.threeten.bp;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * A set of utilities to assist in bridging the gap to Java 8.
 * <p>
 * This class is not found in Java SE 8 but provides methods that are.
 */
public final class DateTimeUtils {

    /**
     * Restricted constructor.
     */
    private DateTimeUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code java.util.Date} to an {@code Instant}.
     *
     * @param utilDate  the util date, not null
     * @return the instant, not null
     */
    public static Instant toInstant(Date utilDate) {
        return Instant.ofEpochMilli(utilDate.getTime());
    }

    /**
     * Converts an {@code Instant} to a {@code java.util.Date}.
     * <p>
     * Fractions of the instant smaller than milliseconds will be dropped.
     *
     * @param instant  the instant, not null
     * @return the util date, not null
     * @throws IllegalArgumentException if the conversion fails
     */
    public static Date toDate(Instant instant) {
        try {
            return new Date(instant.toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code Calendar} to an {@code Instant}.
     *
     * @param calendar  the calendar, not null
     * @return the instant, not null
     */
    public static Instant toInstant(Calendar calendar) {
        return Instant.ofEpochMilli(calendar.getTimeInMillis());
    }

    /**
     * Converts a {@code Calendar} to a {@code ZonedDateTime}.
     * <p>
     * Note that {@code GregorianCalendar} supports a Julian-Gregorian cutover
     * date and {@code ZonedDateTime} does not so some differences will occur.
     *
     * @param calendar  the calendar, not null
     * @return the instant, not null
     */
    public static ZonedDateTime toZonedDateTime(Calendar calendar) {
        Instant instant = Instant.ofEpochMilli(calendar.getTimeInMillis());
        ZoneId zone = toZoneId(calendar.getTimeZone());
        return ZonedDateTime.ofInstant(instant, zone);
    }

    /**
     * Converts a {@code ZonedDateTime} to a {@code Calendar}.
     * <p>
     * The resulting {@code GregorianCalendar} is pure Gregorian and uses
     * ISO week definitions, starting on Monday and with 4 days in a minimal week.
     * <p>
     * Fractions of the instant smaller than milliseconds will be dropped.
     *
     * @param zdt  the zoned date-time, not null
     * @return the calendar, not null
     * @throws IllegalArgumentException if the conversion fails
     */
    public static GregorianCalendar toGregorianCalendar(ZonedDateTime zdt) {
        TimeZone zone = toTimeZone(zdt.getZone());
        GregorianCalendar cal = new GregorianCalendar(zone);
        cal.setGregorianChange(new Date(Long.MIN_VALUE));
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        try {
            cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
        return cal;
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code TimeZone} to a {@code ZoneId}.
     * 
     * @param timeZone  the time-zone, not null
     * @return the zone, not null
     */
    public static ZoneId toZoneId(TimeZone timeZone) {
        return ZoneId.of(timeZone.getID(), ZoneId.SHORT_IDS);
    }

    /**
     * Converts a {@code ZoneId} to a {@code TimeZone}.
     * 
     * @param zoneId  the zone, not null
     * @return the time-zone, not null
     */
    public static TimeZone toTimeZone(ZoneId zoneId) {
        String tzid = zoneId.getId();
        if (tzid.startsWith("+") || tzid.startsWith("-")) {
            tzid = "GMT" + tzid;
        } else if (tzid.equals("Z")) {
            tzid = "UTC";
        }
        return TimeZone.getTimeZone(tzid);
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code java.sql.Date} to a {@code LocalDate}.
     *
     * @param sqlDate  the SQL date, not null
     * @return the local date, not null
     */
    @SuppressWarnings("deprecation")
    public static LocalDate toLocalDate(java.sql.Date sqlDate) {
        return LocalDate.of(sqlDate.getYear() + 1900, sqlDate.getMonth() + 1, sqlDate.getDate());
    }

    /**
     * Converts a {@code LocalDate} to a {@code java.sql.Date}.
     *
     * @param date  the local date, not null
     * @return the SQL date, not null
     */
    @SuppressWarnings("deprecation")
    public static java.sql.Date toSqlDate(LocalDate date) {
        return new java.sql.Date(date.getYear() - 1900, date.getMonthValue() -1, date.getDayOfMonth());
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code java.sql.Time} to a {@code LocalTime}.
     *
     * @param sqlTime  the SQL time, not null
     * @return the local time, not null
     */
    @SuppressWarnings("deprecation")
    public static LocalTime toLocalTime(java.sql.Time sqlTime) {
        return LocalTime.of(sqlTime.getHours(), sqlTime.getMinutes(), sqlTime.getSeconds());
    }

    /**
     * Converts a {@code LocalTime} to a {@code java.sql.Time}.
     *
     * @param time  the local time, not null
     * @return the SQL time, not null
     */
    @SuppressWarnings("deprecation")
    public static java.sql.Time toSqlTime(LocalTime time) {
        return new java.sql.Time(time.getHour(), time.getMinute(), time.getSecond());
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a {@code LocalDateTime} to a {@code java.sql.Timestamp}.
     *
     * @param dateTime  the local date-time, not null
     * @return the SQL timestamp, not null
     */
    @SuppressWarnings("deprecation")
    public static Timestamp toSqlTimestamp(LocalDateTime dateTime) {
        return new Timestamp(
                dateTime.getYear() - 1900,
                dateTime.getMonthValue() - 1,
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond(),
                dateTime.getNano());
    }

    /**
     * Converts a {@code java.sql.Timestamp} to a {@code LocalDateTime}.
     *
     * @param sqlTimestamp  the SQL timestamp, not null
     * @return the local date-time, not null
     */
    @SuppressWarnings("deprecation")
    public static LocalDateTime toLocalDateTime(Timestamp sqlTimestamp) {
        return LocalDateTime.of(
                sqlTimestamp.getYear() + 1900,
                sqlTimestamp.getMonth() + 1,
                sqlTimestamp.getDate(),
                sqlTimestamp.getHours(),
                sqlTimestamp.getMinutes(),
                sqlTimestamp.getSeconds(),
                sqlTimestamp.getNanos());
    }

    /**
     * Converts an {@code Instant} to a {@code java.sql.Timestamp}.
     *
     * @param instant  the instant, not null
     * @return the SQL timestamp, not null
     */
    public static Timestamp toSqlTimestamp(Instant instant) {
        try {
            Timestamp ts = new Timestamp(instant.getEpochSecond() * 1000);
            ts.setNanos(instant.getNano());
            return ts;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Converts a {@code java.sql.Timestamp} to an {@code Instant}.
     *
     * @param sqlTimestamp  the SQL timestamp, not null
     * @return the instant, not null
     */
    public static Instant toInstant(Timestamp sqlTimestamp) {
        return Instant.ofEpochSecond(sqlTimestamp.getTime() / 1000, sqlTimestamp.getNanos());
    }

}
