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

import static org.teavm.classlib.java.time.TDayOfWeek.MONDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.TUESDAY;
import static org.teavm.classlib.java.time.TMonth.AUGUST;
import static org.teavm.classlib.java.time.TMonth.FEBRUARY;
import static org.teavm.classlib.java.time.TMonth.MARCH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HOURS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MINUTES;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.dayOfWeekInMonth;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.firstInMonth;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.lastDayOfMonth;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.next;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.nextOrSame;

import org.teavm.classlib.java.time.zone.TZoneOffsetTransition;

public class FluentAPIChecker {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        TClock clock = TClock.systemDefaultZone();

        TLocalTime tod = TLocalTime.now(clock);
        tod.plusHours(6).plusMinutes(2);
        tod.plus(6, HOURS).plus(2, MINUTES);

        TLocalDate date = null;
        date = TLocalDate.now(clock).plusDays(3);
        date = TLocalDate.now(clock).plus(3, DAYS);
        date = TLocalDate.now(TClock.systemDefaultZone()).plus(3, DAYS);

        date = TLocalDate.of(2007, 3, 20);
        date = TLocalDate.of(2007, MARCH, 20);
        date = TYear.of(2007).atMonth(3).atDay(20);
        date = TYear.of(2007).atMonth(MARCH).atDay(20);

        date = date.with(lastDayOfMonth());
        date = date.with(next(MONDAY));
        date = date.with(nextOrSame(MONDAY));
        date = date.with(dayOfWeekInMonth(2, TUESDAY));
        date = date.with(firstInMonth(MONDAY));
        date = date.with(TYear.of(2009));
        date = date.with(TMonth.of(6));
        date = date.with(AUGUST);

//        DateTimeFields fri13 = DateTimeFields.of(
//                DAY_OF_WEEK, FRIDAY.getValue(), DAY_OF_MONTH, 13);
//        if (fri13.matches(date)) {
//            TSystem.out.println("Spooky");
//        }

        TPeriod d2 = TPeriod.ofDays(3);
        TSystem.out.println(d2);

        tod.withHour(12).withMinute(30);

        TMonthDay md = TMonthDay.of(FEBRUARY, 4);
        md = md.with(MARCH);

        DAY_OF_MONTH.range().getMaximum();
        date.getMonth().maxLength();
        date.range(DAY_OF_MONTH).getMaximum();
        FEBRUARY.maxLength();

        TDayOfWeek dow = MONDAY;
        dow = dow.plus(1);
//
//        int dayIndex = day.value();
//        int dayIndex = day.value(Territory.US);
//        int dayIndex = day.valueIndexedFrom(SUNDAY);
////        SundayBasedDayOfWeek.MONDAY != TDayOfWeek.MONDAY;
//        Territory.US.dayOfWeekComparator();

        TZoneOffset offset = TZoneOffset.ofHours(1);
        TZoneId paris = TZoneId.of("Europe/Paris");

        for (TZoneOffsetTransition trans : paris.getRules().getTransitions()) {
            TSystem.out.println("Paris transition: " + trans);
        }
        TSystem.out.println("Summer time Paris starts: " + paris.getRules().getTransitionRules().get(0));
        TSystem.out.println("Summer time Paris ends: " + paris.getRules().getTransitionRules().get(1));

        TLocalDateTime ldt = date.atTime(tod);
        TZonedDateTime zdt1 = date.atStartOfDay(paris);
        TZonedDateTime zdt2 = date.atTime(12, 0).atZone(paris);

        {
            TYear year = TYear.of(2002);
            TYearMonth sixNationsMonth = year.atMonth(FEBRUARY);
            TLocalDate englandWales = sixNationsMonth.atDay(12);
            TLocalDate engWal = TYear.of(2009).atMonth(FEBRUARY).atDay(12);
        }

        TClock tickingClock = TClock.tickSeconds(paris);
        for (int i = 0; i < 20; i++) {
            TSystem.out.println(TLocalTime.now(tickingClock));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
        }
    }

}
