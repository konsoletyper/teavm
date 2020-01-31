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

import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.HOUR_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.MINUTE_OF_HOUR;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.NANO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.SECOND_OF_MINUTE;
import static org.threeten.bp.temporal.ChronoField.YEAR;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.threeten.bp.format.DateTimeFormatter;

/**
 * Test Performance.
 */
public class Performance {

    /** Size. */
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();
    static {
        NF.setGroupingUsed(true);
    }
    /** Size. */
    private static final int SIZE = 100000;
    /** Results. */
    private static final Map<String, long[]> RESULTS = new TreeMap<String, long[]>();
    /** Count. */
    private static int loop = 0;

    /**
     * Main.
     * @param args  the arguments
     */
    public static void main(String[] args) {
        for (loop = 0; loop < 5; loop++) {
            System.out.println("-------------------------------------");
            process();
        }

        System.out.println();
        for (String name : RESULTS.keySet()) {
            System.out.println(name + " " + Arrays.toString(RESULTS.get(name)));
        }

        System.out.println();
        for (String name : RESULTS.keySet()) {
            long[] r = RESULTS.get(name);
            BigDecimal percent = BigDecimal.valueOf(r[6], 1);
            String max = ("           " + NF.format(r[0]));
            max = max.substring(max.length() - 12);
            String min = ("           " + NF.format(r[5]));
            min = min.substring(min.length() - 12);
            System.out.println(name + "\t" + max + "\t" + min + "\t-" + percent + "%");
        }
    }
    public static void process() {
        LocalTime time = LocalTime.of(12, 30, 20);
        System.out.println(time);

        List<LocalDateTime> ldt = setupDateTime();
        queryListDateTime(ldt);
        formatListDateTime(ldt);
        sortListDateTime(ldt);

        List<ZonedDateTime> zdt = setupZonedDateTime();
        queryListZonedDateTime(zdt);
        formatListZonedDateTime(zdt);
        sortListZonedDateTime(zdt);

        List<Instant> instants = setupInstant();
        queryListInstant(instants);
        formatListInstant(instants);
        sortListInstant(instants);

        List<Date> judates = setupDate();
        queryListDate(judates);
        formatListDate(judates);
        sortListDate(judates);

        List<LocalDate> ld = setupLocalDate();
        queryListLocalDate(ld);
        formatListLocalDate(ld);
        sortListLocalDate(ld);

        List<LocalTime> lt = setupTime();
        queryListTime(lt);
        formatListTime(lt);
        sortListTime(lt);

        List<GregorianCalendar> gcals = setupGCal();
        queryListGCal(gcals);
        formatListGCal(gcals);
        sortListGCal(gcals);

        deriveTime(lt);
        deriveDateTime(ldt);
    }

    //-----------------------------------------------------------------------
    private static List<LocalDateTime> setupDateTime() {
        Random random = new Random(47658758756875687L);
        List<LocalDateTime> list = new ArrayList<LocalDateTime>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            LocalDateTime t = LocalDateTime.of(
                    random.nextInt(10000), random.nextInt(12) + 1, random.nextInt(28) + 1,
                    random.nextInt(24), random.nextInt(60), random.nextInt(60));
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("LocalDT:   Setup:  " + NF.format(end - start) + " ns");
        result("LocalDT-I", end - start);
        return list;
    }

    private static void sortListDateTime(List<LocalDateTime> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("LocalDT:   Sort:   " + NF.format(end - start) + " ns " + list.get(0));
        result("LocalDT-S", end - start);
    }

    private static void queryListDateTime(List<LocalDateTime> list) {
        long total = 0;
        long start = System.nanoTime();
        for (LocalDateTime dt : list) {
            total += dt.getYear();
            total += dt.getMonth().getValue();
            total += dt.getDayOfMonth();
            total += dt.getHour();
            total += dt.getMinute();
            total += dt.getSecond();
        }
        long end = System.nanoTime();
        System.out.println("LocalDT:   Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("LocalDT-Q", end - start);
    }

    private static void formatListDateTime(List<LocalDateTime> list) {
        StringBuilder buf = new StringBuilder();
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE.withLocale(Locale.ENGLISH);
        long start = System.nanoTime();
        for (LocalDateTime dt : list) {
            buf.setLength(0);
            buf.append(format.format(dt));
        }
        long end = System.nanoTime();
        System.out.println("LocalDT:   Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("LocalDT-P", end - start);
    }

    private static void deriveDateTime(List<LocalDateTime> list) {
        long total = 0;
        long start = System.nanoTime();
        for (LocalDateTime dt : list) {
            total += dt.get(YEAR);
            total += dt.get(MONTH_OF_YEAR);
            total += dt.get(DAY_OF_MONTH);
            total += dt.get(HOUR_OF_DAY);
            total += dt.get(MINUTE_OF_HOUR);
        }
        long end = System.nanoTime();
        System.out.println("LocalDT:   Derive: " + NF.format(end - start) + " ns" + " " + total);
        result("LocalDT-V", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<LocalDate> setupLocalDate() {
        Random random = new Random(47658758756875687L);
        List<LocalDate> list = new ArrayList<LocalDate>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            LocalDate t = LocalDate.of(random.nextInt(10000), random.nextInt(12) + 1, random.nextInt(28) + 1);
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("LocalD:    Setup:  " + NF.format(end - start) + " ns");
        result("LocalD-I", end - start);
        return list;
    }

    private static void sortListLocalDate(List<LocalDate> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("LocalD:    Sort:   " + NF.format(end - start) + " ns " + list.get(0));
        result("LocalD-S", end - start);
    }

    private static void queryListLocalDate(List<LocalDate> list) {
        long total = 0;
        long start = System.nanoTime();
        for (LocalDate dt : list) {
            total += dt.getYear();
            total += dt.getMonth().getValue();
            total += dt.getDayOfMonth();
        }
        long end = System.nanoTime();
        System.out.println("LocalD:    Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("LocalD-Q", end - start);
    }

    private static void formatListLocalDate(List<LocalDate> list) {
        StringBuilder buf = new StringBuilder();
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE.withLocale(Locale.ENGLISH);
        long start = System.nanoTime();
        for (LocalDate dt : list) {
            buf.setLength(0);
            buf.append(format.format(dt));
        }
        long end = System.nanoTime();
        System.out.println("LocalD:    Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("LocalD-P", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<LocalTime> setupTime() {
        Random random = new Random(47658758756875687L);
        List<LocalTime> list = new ArrayList<LocalTime>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            LocalTime t = LocalTime.of(random.nextInt(24), random.nextInt(60), random.nextInt(60), random.nextInt(1000000000));
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("LocalT:    Setup:  " + NF.format(end - start) + " ns");
        result("LocalT-I", end - start);
        return list;
    }

    private static void sortListTime(List<LocalTime> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("LocalT:    Sort:   " + NF.format(end - start) + " ns " + list.get(0));
        result("LocalT-S", end - start);
    }

    private static void queryListTime(List<LocalTime> list) {
        long total = 0;
        long start = System.nanoTime();
        for (LocalTime dt : list) {
            total += dt.getHour();
            total += dt.getMinute();
            total += dt.getSecond();
            total += dt.getNano();
        }
        long end = System.nanoTime();
        System.out.println("LocalT:    Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("LocalT-Q", end - start);
    }

    private static void formatListTime(List<LocalTime> list) {
        StringBuilder buf = new StringBuilder();
        DateTimeFormatter format = DateTimeFormatter.ISO_TIME.withLocale(Locale.ENGLISH);
        long start = System.nanoTime();
        for (LocalTime dt : list) {
            buf.setLength(0);
            buf.append(format.format(dt));
        }
        long end = System.nanoTime();
        System.out.println("LocalT:    Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("LocalT-P", end - start);
    }

    private static void deriveTime(List<LocalTime> list) {
        long total = 0;
        long start = System.nanoTime();
        for (LocalTime dt : list) {
            total += dt.get(HOUR_OF_DAY);
            total += dt.get(MINUTE_OF_HOUR);
            total += dt.get(SECOND_OF_MINUTE);
            total += dt.get(NANO_OF_SECOND);
        }
        long end = System.nanoTime();
        System.out.println("LocalT:    Derive: " + NF.format(end - start) + " ns" + " " + total);
        result("LocalT-V", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<ZonedDateTime> setupZonedDateTime() {
        ZoneId tz = ZoneId.of("Europe/London");
        Random random = new Random(47658758756875687L);
        List<ZonedDateTime> list = new ArrayList<ZonedDateTime>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            ZonedDateTime t = LocalDateTime.of(
                    2008/*random.nextInt(10000)*/, random.nextInt(12) + 1, random.nextInt(28) + 1,
                    random.nextInt(24), random.nextInt(60), random.nextInt(60), 0).atZone(tz);
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("ZonedDT:   Setup:  " + NF.format(end - start) + " ns");
        result("ZonedDT-I", end - start);
        return list;
    }

    private static void sortListZonedDateTime(List<ZonedDateTime> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("ZonedDT:   Sort:   " + NF.format(end - start) + " ns");
        result("ZonedDT-S", end - start);
    }

    private static void queryListZonedDateTime(List<ZonedDateTime> list) {
        long total = 0;
        long start = System.nanoTime();
        for (ZonedDateTime dt : list) {
            total += dt.getYear();
            total += dt.getMonth().getValue();
            total += dt.getDayOfMonth();
            total += dt.getHour();
            total += dt.getMinute();
            total += dt.getSecond();
        }
        long end = System.nanoTime();
        System.out.println("ZonedDT:   Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("ZonedDT-Q", end - start);
    }

    private static void formatListZonedDateTime(List<ZonedDateTime> list) {
        StringBuilder buf = new StringBuilder();
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE.withLocale(Locale.ENGLISH);
        long start = System.nanoTime();
        for (ZonedDateTime dt : list) {
            buf.setLength(0);
            buf.append(format.format(dt));
        }
        long end = System.nanoTime();
        System.out.println("ZonedDT:   Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("ZonedDT-P", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<Instant> setupInstant() {
        Random random = new Random(47658758756875687L);
        List<Instant> list = new ArrayList<Instant>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            Instant t = Instant.ofEpochMilli(random.nextLong());
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("Instant:   Setup:  " + NF.format(end - start) + " ns");
        result("Instant-I", end - start);
        return list;
    }

    private static void sortListInstant(List<Instant> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("Instant:   Sort:   " + NF.format(end - start) + " ns");
        result("Instant-S", end - start);
    }

    private static void queryListInstant(List<Instant> list) {
        long total = 0;
        long start = System.nanoTime();
        for (Instant dt : list) {
            total += dt.getEpochSecond();
            total += dt.getNano();
        }
        long end = System.nanoTime();
        System.out.println("Instant:   Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("Instant-Q", end - start);
    }

    private static void formatListInstant(List<Instant> list) {
        StringBuilder buf = new StringBuilder();
        long start = System.nanoTime();
        for (Instant dt : list) {
            buf.setLength(0);
            buf.append(dt.toString());
        }
        long end = System.nanoTime();
        System.out.println("Instant:   Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("Instant-P", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<Date> setupDate() {
        Random random = new Random(47658758756875687L);
        List<Date> list = new ArrayList<Date>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            Date t = new Date(random.nextLong());
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("Date:      Setup:  " + NF.format(end - start) + " ns");
        result("JUDate-I", end - start);
        return list;
    }

    private static void sortListDate(List<Date> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("Date:      Sort:   " + NF.format(end - start) + " ns " + list.get(0));
        result("JUDate-S", end - start);
    }

    private static void queryListDate(List<Date> list) {
        long total = 0;
        long start = System.nanoTime();
        for (Date dt : list) {
            total += dt.getTime();
        }
        long end = System.nanoTime();
        System.out.println("Date:      Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("JUDate-Q", end - start);
    }

    private static void formatListDate(List<Date> list) {
        StringBuilder buf = new StringBuilder();
        long start = System.nanoTime();
        for (Date dt : list) {
            buf.setLength(0);
            buf.append(dt.toString());
        }
        long end = System.nanoTime();
        System.out.println("Date:      Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("JUDate-P", end - start);
    }

    //-----------------------------------------------------------------------
    private static List<GregorianCalendar> setupGCal() {
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Europe/London");
        Random random = new Random(47658758756875687L);
        List<GregorianCalendar> list = new ArrayList<GregorianCalendar>(SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            GregorianCalendar t = new GregorianCalendar(tz);
            t.setGregorianChange(new Date(Long.MIN_VALUE));
            t.set(random.nextInt(10000), random.nextInt(12), random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60), random.nextInt(60));
            list.add(t);
        }
        long end = System.nanoTime();
        System.out.println("GCalendar: Setup:  " + NF.format(end - start) + " ns");
        result("GregCal-I", end - start);
        return list;
    }

    private static void sortListGCal(List<GregorianCalendar> list) {
        long start = System.nanoTime();
        Collections.sort(list);
        long end = System.nanoTime();
        System.out.println("GCalendar: Sort:   " + NF.format(end - start) + " ns");
        result("GregCal-S", end - start);
    }

    private static void queryListGCal(List<GregorianCalendar> list) {
        long total = 0;
        long start = System.nanoTime();
        for (GregorianCalendar gcal : list) {
            total += gcal.get(Calendar.YEAR);
            total += gcal.get(Calendar.MONTH + 1);
            total += gcal.get(Calendar.DAY_OF_MONTH);
            total += gcal.get(Calendar.HOUR_OF_DAY);
            total += gcal.get(Calendar.MINUTE);
            total += gcal.get(Calendar.SECOND);
            total += gcal.get(Calendar.SECOND);
        }
        long end = System.nanoTime();
        System.out.println("GCalendar: Query:  " + NF.format(end - start) + " ns" + " " + total);
        result("GregCal-Q", end - start);
    }

    private static void formatListGCal(List<GregorianCalendar> list) {
        StringBuilder buf = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        long start = System.nanoTime();
        for (GregorianCalendar gcal : list) {
            buf.setLength(0);
            buf.append(format.format(gcal.getTime()));
        }
        long end = System.nanoTime();
        System.out.println("GCalendar: Format: " + NF.format(end - start) + " ns" + " " + buf);
        result("GregCal-P", end - start);
    }

    //-----------------------------------------------------------------------
    private static void result(String name, long result) {
        long[] values = RESULTS.get(name);
        if (values == null) {
            values = new long[7];
            RESULTS.put(name, values);
        }
        values[loop] = result;
        if (loop == 4) {
            values[5] = Math.min(values[0], Math.min(values[1], Math.min(values[2], Math.min(values[3], values[4]))));
            values[6] = (((values[0] - values[5]) * 1000) / values[0]);
        }
    }

}
