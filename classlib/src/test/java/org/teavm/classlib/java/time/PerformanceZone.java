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

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.threeten.bp.zone.ZoneRules;

/**
 * Test Performance.
 */
public class PerformanceZone {

    /** The year to test. */
    private static final int YEAR = 1980;
    /** Size. */
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();
    static {
        NF.setGroupingUsed(true);
    }
    /** Size. */
    private static final int SIZE = 200000;

    /**
     * Main.
     * @param args  the arguments
     */
    public static void main(String[] args) {
        LocalTime time = LocalTime.of(12, 30, 20);
        System.out.println(time);

        for (int i = 0; i < 6; i++) {
            jsrLocalGetOffset();
            jsrInstantGetOffset();
            jsrRulesLocalGetOffset();
            jsrRulesInstantGetOffset();
            jdkLocalGetOffset();
            jdkInstantGetOffset();
            System.out.println();
        }
    }

    //-----------------------------------------------------------------------
    private static void jsrLocalGetOffset() {
        LocalDateTime dt = LocalDateTime.of(YEAR, 6, 1, 12, 0);
        ZoneId tz = ZoneId.of("Europe/London");
        ZoneOffset[] list = new ZoneOffset[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getRules().getOffset(dt);
        }
        long end = System.nanoTime();
        System.out.println("JSR-Loc: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrInstantGetOffset() {
        Instant instant = LocalDateTime.of(YEAR, 6, 1, 12, 0).toInstant(ZoneOffset.ofHours(1));
        ZoneId tz = ZoneId.of("Europe/London");
        ZoneOffset[] list = new ZoneOffset[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getRules().getOffset(instant);
        }
        long end = System.nanoTime();
        System.out.println("JSR-Ins: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrRulesLocalGetOffset() {
        LocalDateTime dt = LocalDateTime.of(YEAR, 6, 1, 12, 0);
        ZoneRules tz = ZoneId.of("Europe/London").getRules();
        ZoneOffset[] list = new ZoneOffset[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(dt);
        }
        long end = System.nanoTime();
        System.out.println("JSR-LoR: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrRulesInstantGetOffset() {
        Instant instant = LocalDateTime.of(YEAR, 6, 1, 12, 0).toInstant(ZoneOffset.ofHours(1));
        ZoneRules tz = ZoneId.of("Europe/London").getRules();
        ZoneOffset[] list = new ZoneOffset[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(instant);
        }
        long end = System.nanoTime();
        System.out.println("JSR-InR: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jdkLocalGetOffset() {
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Europe/London");
        int[] list = new int[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(GregorianCalendar.AD, YEAR, 0, 11, Calendar.SUNDAY, 0);
        }
        long end = System.nanoTime();
        System.out.println("GCalLoc: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jdkInstantGetOffset() {
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Europe/London");
        GregorianCalendar dt = new GregorianCalendar(tz);
        dt.setGregorianChange(new Date(Long.MIN_VALUE));
        dt.set(YEAR, 5, 1, 12, 0);
        int[] list = new int[SIZE];
        long start = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(dt.getTimeInMillis());
        }
        long end = System.nanoTime();
        System.out.println("GCalIns: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

}
