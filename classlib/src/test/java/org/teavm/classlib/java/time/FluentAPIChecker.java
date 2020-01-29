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

import static org.threeten.bp.DayOfWeek.MONDAY;
import static org.threeten.bp.DayOfWeek.TUESDAY;
import static org.threeten.bp.Month.AUGUST;
import static org.threeten.bp.Month.FEBRUARY;
import static org.threeten.bp.Month.MARCH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.HOURS;
import static org.threeten.bp.temporal.ChronoUnit.MINUTES;
import static org.threeten.bp.temporal.TemporalAdjusters.dayOfWeekInMonth;
import static org.threeten.bp.temporal.TemporalAdjusters.firstInMonth;
import static org.threeten.bp.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.threeten.bp.temporal.TemporalAdjusters.next;
import static org.threeten.bp.temporal.TemporalAdjusters.nextOrSame;

import org.threeten.bp.zone.ZoneOffsetTransition;

/**
 * Test the fluency of the whole API.
 */
public class FluentAPIChecker {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();

        LocalTime tod = LocalTime.now(clock);
        tod.plusHours(6).plusMinutes(2);
        tod.plus(6, HOURS).plus(2, MINUTES);

        LocalDate date = null;
        date = LocalDate.now(clock).plusDays(3);
        date = LocalDate.now(clock).plus(3, DAYS);
        date = LocalDate.now(Clock.systemDefaultZone()).plus(3, DAYS);

        date = LocalDate.of(2007, 3, 20);
        date = LocalDate.of(2007, MARCH, 20);
        date = Year.of(2007).atMonth(3).atDay(20);
        date = Year.of(2007).atMonth(MARCH).atDay(20);

        date = date.with(lastDayOfMonth());
        date = date.with(next(MONDAY));
        date = date.with(nextOrSame(MONDAY));
        date = date.with(dayOfWeekInMonth(2, TUESDAY));
        date = date.with(firstInMonth(MONDAY));
        date = date.with(Year.of(2009));
        date = date.with(Month.of(6));
        date = date.with(AUGUST);

//        DateTimeFields fri13 = DateTimeFields.of(
//                DAY_OF_WEEK, FRIDAY.getValue(), DAY_OF_MONTH, 13);
//        if (fri13.matches(date)) {
//            System.out.println("Spooky");
//        }

        Period d2 = Period.ofDays(3);
        System.out.println(d2);

        tod.withHour(12).withMinute(30);

        MonthDay md = MonthDay.of(FEBRUARY, 4);
        md = md.with(MARCH);

        DAY_OF_MONTH.range().getMaximum();
        date.getMonth().maxLength();
        date.range(DAY_OF_MONTH).getMaximum();
        FEBRUARY.maxLength();

        DayOfWeek dow = MONDAY;
        dow = dow.plus(1);
//
//        int dayIndex = day.value();
//        int dayIndex = day.value(Territory.US);
//        int dayIndex = day.valueIndexedFrom(SUNDAY);
////        SundayBasedDayOfWeek.MONDAY != DayOfWeek.MONDAY;
//        Territory.US.dayOfWeekComparator();

        ZoneOffset offset = ZoneOffset.ofHours(1);
        ZoneId paris = ZoneId.of("Europe/Paris");

        for (ZoneOffsetTransition trans : paris.getRules().getTransitions()) {
            System.out.println("Paris transition: " + trans);
        }
        System.out.println("Summer time Paris starts: " + paris.getRules().getTransitionRules().get(0));
        System.out.println("Summer time Paris ends: " + paris.getRules().getTransitionRules().get(1));

        LocalDateTime ldt = date.atTime(tod);
        ZonedDateTime zdt1 = date.atStartOfDay(paris);
        ZonedDateTime zdt2 = date.atTime(12, 0).atZone(paris);

        {
            Year year = Year.of(2002);
            YearMonth sixNationsMonth = year.atMonth(FEBRUARY);
            LocalDate englandWales = sixNationsMonth.atDay(12);
            LocalDate engWal = Year.of(2009).atMonth(FEBRUARY).atDay(12);
        }

        Clock tickingClock = Clock.tickSeconds(paris);
        for (int i = 0; i < 20; i++) {
            System.out.println(LocalTime.now(tickingClock));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
        }
    }

}
