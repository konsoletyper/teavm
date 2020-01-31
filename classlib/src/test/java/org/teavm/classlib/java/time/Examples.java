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

import static org.teavm.classlib.java.time.TMonth.DECEMBER;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.lastDayOfMonth;

import org.teavm.classlib.java.util.TLocale;

import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TSignStyle;

public class Examples {

    public static void main(String[] args) {
        TClock clock = TClock.systemDefaultZone();

        TZonedDateTime zdt = TZonedDateTime.now(clock);
        TSystem.out.println("Current date-time: " + zdt);

        TZonedDateTime zdtNewYork = TZonedDateTime.now(TClock.system(TZoneId.of("America/New_York")));
        TSystem.out.println("Current date-time in New York: " + zdtNewYork);

        TZonedDateTime zdtParis = TZonedDateTime.now(TClock.system(TZoneId.of("Europe/Paris")));
        TSystem.out.println("Current date-time in Paris: " + zdtParis);

        TLocalDateTime ldt = TLocalDateTime.now(clock);
        TSystem.out.println("Current local date-time: " + ldt);

        TYear year = TYear.now(clock);
        TSystem.out.println("TYear: " + year.getValue());

        TLocalDate today = TLocalDate.now(clock);
        TSystem.out.println("Today: " + today);

        TSystem.out.println("Current day-of-year: " + today.get(DAY_OF_YEAR));

        TLocalTime time = TLocalTime.now(clock);
        TSystem.out.println("Current time of day: " + time);

        TLocalDate later = TLocalDate.now(clock).plusMonths(2).plusDays(3);
        TSystem.out.println("Two months three days after today: " + later);

//        ISOPeriod period = ISOPeriod.of(3, MONTHS);
//        TLocalDate moreLater = TLocalDate.now(clock).plus(period);
//        TSystem.out.println("TPeriod " + period + " after today : " + moreLater);

        TLocalDate dec = TLocalDate.now(clock).with(DECEMBER);
        TSystem.out.println("Change to same day in December: " + dec);

        TLocalDate lastDayOfMonth = TLocalDate.now(clock).with(lastDayOfMonth());
        TSystem.out.println("Last day of month: " + lastDayOfMonth);

///        TLocalDate tempDate = TLocalDate.now(clock);
//        DateTimeFields fri13matcher = DateTimeFields.of(
//                DAY_OF_WEEK, FRIDAY.getValue(), DAY_OF_MONTH, 13);
//        boolean fri13 = fri13matcher.matches(tempDate);
//        TSystem.out.println("Is Friday the Thirteenth: " + fri13);

        TLocalDateTime dt = TLocalDateTime.of(2008, 3, 30, 1, 30);
        TSystem.out.println("Local date-time in Spring DST gap: " + dt);

        TZonedDateTime resolved = TZonedDateTime.of(dt, TZoneId.of("Europe/London"));
        TSystem.out.println("...resolved to valid date-time in Europe/London: " + resolved);

        String formattedRFC = TDateTimeFormatter.RFC_1123_DATE_TIME.format(resolved);
        TSystem.out.println("...printed as RFC1123: " + formattedRFC);

        TDateTimeFormatter f = new TDateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, TSignStyle.ALWAYS)
            .appendLiteral(' ')
            .appendText(MONTH_OF_YEAR)
            .appendLiteral('(')
            .appendValue(MONTH_OF_YEAR)
            .appendLiteral(')')
            .appendLiteral(' ')
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter(TLocale.ENGLISH);
        String formatted = f.format(resolved);
        TSystem.out.println("...printed using complex format: " + formatted);

        TMonthDay bday = TMonthDay.of(DECEMBER, 3);
        TSystem.out.println("Brazillian birthday (no year): " + bday);
    }

}
