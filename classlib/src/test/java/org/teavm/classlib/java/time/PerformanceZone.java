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

import java.text.NumberFormat;
import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;
import org.teavm.classlib.java.util.TGregorianCalendar;

import org.teavm.classlib.java.time.zone.TZoneRules;

public class PerformanceZone {

    private static final int YEAR = 1980;
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();
    static {
        NF.setGroupingUsed(true);
    }
    private static final int SIZE = 200000;

    public static void main(String[] args) {
        TLocalTime time = TLocalTime.of(12, 30, 20);
        TSystem.out.println(time);

        for (int i = 0; i < 6; i++) {
            jsrLocalGetOffset();
            jsrInstantGetOffset();
            jsrRulesLocalGetOffset();
            jsrRulesInstantGetOffset();
            jdkLocalGetOffset();
            jdkInstantGetOffset();
            TSystem.out.println();
        }
    }

    //-----------------------------------------------------------------------
    private static void jsrLocalGetOffset() {
        TLocalDateTime dt = TLocalDateTime.of(YEAR, 6, 1, 12, 0);
        TZoneId tz = TZoneId.of("Europe/London");
        TZoneOffset[] list = new TZoneOffset[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getRules().getOffset(dt);
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("JSR-Loc: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrInstantGetOffset() {
        TInstant instant = TLocalDateTime.of(YEAR, 6, 1, 12, 0).toInstant(TZoneOffset.ofHours(1));
        TZoneId tz = TZoneId.of("Europe/London");
        TZoneOffset[] list = new TZoneOffset[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getRules().getOffset(instant);
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("JSR-Ins: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrRulesLocalGetOffset() {
        TLocalDateTime dt = TLocalDateTime.of(YEAR, 6, 1, 12, 0);
        TZoneRules tz = TZoneId.of("Europe/London").getRules();
        TZoneOffset[] list = new TZoneOffset[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(dt);
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("JSR-LoR: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jsrRulesInstantGetOffset() {
        TInstant instant = TLocalDateTime.of(YEAR, 6, 1, 12, 0).toInstant(TZoneOffset.ofHours(1));
        TZoneRules tz = TZoneId.of("Europe/London").getRules();
        TZoneOffset[] list = new TZoneOffset[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(instant);
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("JSR-InR: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jdkLocalGetOffset() {
        java.util.TTimeZone tz = java.util.TTimeZone.getTimeZone("Europe/London");
        int[] list = new int[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(TGregorianCalendar.AD, YEAR, 0, 11, TCalendar.SUNDAY, 0);
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("GCalLoc: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

    //-----------------------------------------------------------------------
    private static void jdkInstantGetOffset() {
        java.util.TTimeZone tz = java.util.TTimeZone.getTimeZone("Europe/London");
        TGregorianCalendar dt = new TGregorianCalendar(tz);
        dt.setGregorianChange(new TDate(Long.MIN_VALUE));
        dt.set(YEAR, 5, 1, 12, 0);
        int[] list = new int[SIZE];
        long start = TSystem.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            list[i] = tz.getOffset(dt.getTimeInMillis());
        }
        long end = TSystem.nanoTime();
        TSystem.out.println("GCalIns: Setup:  " + NF.format(end - start) + " ns" + list[0]);
    }

}
