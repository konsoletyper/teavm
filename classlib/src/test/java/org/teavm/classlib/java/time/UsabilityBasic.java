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

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.previousOrSame;

import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public final class UsabilityBasic {

    public static void main(String[] args) {

        simpleCalendar();
        System.out.println("------");
        lookup();
        System.out.println("------");
        period();
        System.out.println("------");
        print1();
        System.out.println("------");
        print2();
    }

    private UsabilityBasic() {

    }

    private static void simpleCalendar() {

        TLocalDate date = TLocalDate.now();
        System.out.println(date);

        date = date.withDayOfMonth(1);
        System.out.println(date);

        int month = date.getMonth().getValue();
        date = date.with(previousOrSame(TDayOfWeek.MONDAY));
        System.out.println(date);

        while (date.getMonth().getValue() <= month) {
            String row = "";
            for (int i = 0; i < 7; i++) {
                row += date.getDayOfMonth() + " ";
                date = date.plusDays(1);
            }
            System.out.println(row);
        }
    }

    private static void lookup() {

        TLocalDate date = TLocalDate.now();
        TLocalTime time = TLocalTime.now();
        TLocalDateTime dateTime = TLocalDateTime.now();

        output(date, TChronoField.DAY_OF_MONTH);
        output(date, TChronoField.MONTH_OF_YEAR);
        output(date, TChronoField.YEAR);

        output(dateTime, TChronoField.DAY_OF_MONTH);
        output(time, TChronoField.HOUR_OF_DAY);
        output(time, TChronoField.MINUTE_OF_HOUR);

        TTemporalAccessor cal = date;
        System.out.println("DoM: " + cal.get(DAY_OF_MONTH));
    }

    protected static void output(TLocalDate date, TTemporalField field) {

        System.out.println(field + " " + date.getLong(field));
    }

    protected static void output(TLocalDateTime dateTime, TTemporalField field) {

        System.out.println(field + " " + dateTime.getLong(field));
    }

    protected static void output(TLocalTime time, TTemporalField field) {

        System.out.println(field + " " + time.getLong(field));
    }

    private static void period() {

        TLocalDate date1 = TLocalDate.now();
        TLocalDate date2 = TLocalDate.now().plusDays(25367);
        System.out.println(TChronoUnit.DAYS.between(date1, date2));
        System.out.println(TChronoUnit.YEARS.between(date1, date2));

        date1 = TLocalDate.of(2012, 2, 20);
        date2 = TLocalDate.of(2014, 2, 19);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));
        date2 = TLocalDate.of(2014, 2, 20);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));
        date2 = TLocalDate.of(2014, 2, 21);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));
        date2 = TLocalDate.of(2010, 2, 19);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));
        date2 = TLocalDate.of(2010, 2, 20);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));
        date2 = TLocalDate.of(2010, 2, 21);
        System.out.println(TChronoUnit.YEARS.between(date1, date2));

        TLocalDate date3 = TLocalDate.now().plus(3, TChronoUnit.DAYS);
        System.out.println("3 days later " + date3);
    }

    private static void print1() {

        TDateTimeFormatter f = new TDateTimeFormatterBuilder().appendText(TChronoField.AMPM_OF_DAY).appendLiteral(' ')
                .appendValue(TChronoField.AMPM_OF_DAY).toFormatter();
        System.out.println(f.format(TLocalTime.of(12, 30)));
        System.out.println(f.format(TZonedDateTime.now()));
    }

    private static void print2() {

        TDateTimeFormatter f = new TDateTimeFormatterBuilder().appendText(TChronoField.MONTH_OF_YEAR).appendLiteral(' ')
                .appendValue(TChronoField.YEAR).toFormatter();
        System.out.println(f.format(TLocalDate.now()));
        System.out.println(f.format(TYearMonth.now()));
        System.out.println(f.format(TZonedDateTime.now()));
    }

}
