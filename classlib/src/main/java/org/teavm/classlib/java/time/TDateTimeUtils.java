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
package org.teavm.classlib.java.time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;

public final class TDateTimeUtils {

    private TDateTimeUtils() {

    }

    public static TInstant toInstant(TDate utilDate) {

        return TInstant.ofEpochMilli(utilDate.getTime());
    }

    public static TDate toDate(TInstant instant) {

        try {
            return new TDate(instant.toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static TInstant toInstant(Calendar calendar) {

        return TInstant.ofEpochMilli(calendar.getTimeInMillis());
    }

    public static TZonedDateTime toZonedDateTime(Calendar calendar) {

        TInstant instant = TInstant.ofEpochMilli(calendar.getTimeInMillis());
        TZoneId zone = toZoneId(calendar.getTimeZone());
        return TZonedDateTime.ofInstant(instant, zone);
    }

    public static GregorianCalendar toGregorianCalendar(TZonedDateTime zdt) {

        TimeZone zone = toTimeZone(zdt.getZone());
        GregorianCalendar cal = new GregorianCalendar(zone);
        cal.setGregorianChange(new Date(Long.MIN_VALUE));
        cal.setFirstDayOfWeek(TCalendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        try {
            cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
        return cal;
    }

    public static TZoneId toZoneId(TimeZone timeZone) {

        return TZoneId.of(timeZone.getID(), TZoneId.SHORT_IDS);
    }

    public static TimeZone toTimeZone(TZoneId zoneId) {

        String tzid = zoneId.getId();
        if (tzid.startsWith("+") || tzid.startsWith("-")) {
            tzid = "GMT" + tzid;
        } else if (tzid.equals("Z")) {
            tzid = "UTC";
        }
        return TimeZone.getTimeZone(tzid);
    }

}
