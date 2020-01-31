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
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;

import java.io.PrintStream;
import java.util.Set;

import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.THijrahChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.chrono.TJapaneseChronology;
import org.teavm.classlib.java.time.chrono.TMinguoChronology;
import org.teavm.classlib.java.time.chrono.TThaiBuddhistChronology;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;

public final class UsabilityChrono {

    public static void main(String[] args) {
        TSystem.out.println("------");
        newPackagePluggable();
        TSystem.out.println("------");
        epochDays();
        TSystem.out.println("------");
        printMinguoCal();
        TSystem.out.println("------");
        example1();
    }

    private UsabilityChrono() {
    }

    static {
        TChronology c = TJapaneseChronology.INSTANCE;
        c = TMinguoChronology.INSTANCE;
        c = TThaiBuddhistChronology.INSTANCE;
        c = TJapaneseChronology.INSTANCE;
        c = TMinguoChronology.INSTANCE;
        c = THijrahChronology.INSTANCE;
        c = TIsoChronology.INSTANCE;
        c.toString();
    }

    private static void newPackagePluggable() {
        TChronology chrono = TMinguoChronology.INSTANCE;

        TChronoLocalDate date = chrono.dateNow();
        TSystem.out.printf("now: %s%n", date);

        date = date.with(DAY_OF_MONTH, 1);
        TSystem.out.printf("first of month: %s%n", date);

        int month = (int) date.get(TChronoField.MONTH_OF_YEAR);
        date = date.with(DAY_OF_WEEK, 1);
        TSystem.out.printf("start of first week: %s%n", date);

        while (date.get(TChronoField.MONTH_OF_YEAR) <= month) {
            String row = "";
            for (int i = 0; i < 7; i++) {
                row += date.get(TChronoField.DAY_OF_MONTH) + " ";
                date = date.plus(1, TChronoUnit.DAYS);
            }
            TSystem.out.println(row);
        }
    }

    private static void epochDays() {
        output(TLocalDate.now());
        output(TLocalDate.of(1945, 11, 12));
        output(TLocalDate.of(-4713, 11, 24));
        output(TLocalDate.of(1858, 11, 17));
        output(TLocalDate.of(1970, 1, 1));
        output(TLocalDate.of(1, 1, 1));
    }

    protected static void output(TLocalDate date) {
        TSystem.out.println(date);
        TSystem.out.println("EPOCH_DAY " + date.getLong(EPOCH_DAY));
        TSystem.out.println("JDN " + date.getLong(TJulianFields.JULIAN_DAY));
        TSystem.out.println("MJD " + date.getLong(TJulianFields.MODIFIED_JULIAN_DAY));
        TSystem.out.println("RD  " + date.getLong(TJulianFields.RATA_DIE));
        TSystem.out.println();
    }


    static void example1() {
        TSystem.out.printf("Available Calendars%n");

        // Print the Minguo date
        TChronoLocalDate now1 = TMinguoChronology.INSTANCE.dateNow();
        int day = now1.get(TChronoField.DAY_OF_MONTH);
        int dow = now1.get(TChronoField.DAY_OF_WEEK);
        int month = now1.get(TChronoField.MONTH_OF_YEAR);
        int year = now1.get(TChronoField.YEAR);
        TSystem.out.printf("  Today is %s %s %d-%s-%d%n", now1.getChronology().getId(),
                TDayOfWeek.of(dow), year, month, day);

        // Print today's date and the last day of the year for the Minguo TCalendar.
        TChronoLocalDate first = now1
                .with(TChronoField.DAY_OF_MONTH, 1)
                .with(TChronoField.MONTH_OF_YEAR, 1);
        TChronoLocalDate last = first
                .plus(1, TChronoUnit.YEARS)
                .minus(1, TChronoUnit.DAYS);
        TSystem.out.printf("  1st of year: %s; end of year: %s%n", first, last);

        // Enumerate the list of available calendars and print today for each
        TLocalDate  before = TLocalDate.of(-500, 1, 1);
        Set<TChronology> chronos = TChronology.getAvailableChronologies();
        for (TChronology chrono : chronos) {
            TChronoLocalDate date = chrono.dateNow();
            TChronoLocalDate date2 = chrono.date(before);
            TSystem.out.printf("   %20s: %22s, %22s%n", chrono.getId(), date, date2);
        }
    }

    private static void printMinguoCal() {
        String chronoName = "Minguo";
        TChronology chrono = TChronology.of(chronoName);
        TChronoLocalDate today = chrono.dateNow();
        printMonthCal(today, TSystem.out);
    }

    private static void printMonthCal(TChronoLocalDate date, PrintStream out) {

        int lengthOfMonth = (int) date.lengthOfMonth();
        TChronoLocalDate end = date.with(TChronoField.DAY_OF_MONTH, lengthOfMonth);
        end = end.plus(7 - end.get(TChronoField.DAY_OF_WEEK), TChronoUnit.DAYS);
        // Back up to the beginning of the week including the 1st of the month
        TChronoLocalDate start = date.with(TChronoField.DAY_OF_MONTH, 1);
        start = start.minus(start.get(TChronoField.DAY_OF_WEEK), TChronoUnit.DAYS);

        out.printf("%9s TMonth %2d, %4d%n", date.getChronology().getId(),
                date.get(TChronoField.MONTH_OF_YEAR),
                date.get(TChronoField.YEAR));
        String[] colText = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        printMonthRow(colText, " ", out);

        String[] cell = new String[7];
        for ( ; start.compareTo(end) <= 0; start = start.plus(1, TChronoUnit.DAYS)) {
            int ndx = start.get(TChronoField.DAY_OF_WEEK) - 1;
            cell[ndx] = Integer.toString((int) start.get(TChronoField.DAY_OF_MONTH));
            if (ndx == 6) {
                printMonthRow(cell, "|", out);
            }
        }
    }

    private static void printMonthRow(String[] cells, String delim, PrintStream out) {
        for (int i = 0; i < cells.length; i++) {
            out.printf("%s%3s ", delim, cells[i]);
        }
        out.println(delim);
    }

}
