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
package org.teavm.classlib.java.time.zone;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.teavm.classlib.java.time.AbstractTest;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransitionRule.TimeDefinition;

public class TestZoneOffsetTransitionRule extends AbstractTest {

    private static final TLocalTime TIME_0100 = TLocalTime.of(1, 0);

    private static final TZoneOffset OFFSET_0200 = TZoneOffset.ofHours(2);

    private static final TZoneOffset OFFSET_0300 = TZoneOffset.ofHours(3);

    @Test(expected = NullPointerException.class)
    public void test_factory_nullMonth() {

        TZoneOffsetTransitionRule.of(null, 20, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL, OFFSET_0200,
                OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_nullTime() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, null, false, TimeDefinition.WALL, OFFSET_0200,
                OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_nullTimeDefinition() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100, false, null, OFFSET_0200,
                OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_nullStandardOffset() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL, null,
                OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_nullOffsetBefore() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, null, OFFSET_0300);
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_nullOffsetAfter() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_tooSmall() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, -29, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_zero() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 0, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_tooLarge() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 32, TDayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_factory_invalidMidnightFlag() {

        TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test
    public void test_getters_floatingWeek() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), TMonth.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), 20);
        assertEquals(test.getDayOfWeek(), TDayOfWeek.SUNDAY);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }

    @Test
    public void test_getters_floatingWeekBackwards() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -1, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), TMonth.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), -1);
        assertEquals(test.getDayOfWeek(), TDayOfWeek.SUNDAY);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }

    @Test
    public void test_getters_fixedDate() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), TMonth.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), 20);
        assertEquals(test.getDayOfWeek(), null);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }

    @Test
    public void test_createTransition_floatingWeek_gap_notEndOfDay() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 26, 1, 0),
                OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeek_overlap_endOfDay() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY,
                TLocalTime.MIDNIGHT, true, TimeDefinition.WALL, OFFSET_0200, OFFSET_0300, OFFSET_0200);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 27, 0, 0),
                OFFSET_0300, OFFSET_0200);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_last() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -1, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 26, 1, 0),
                OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_seventhLast() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -7, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 19, 1, 0),
                OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_secondLast() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -2, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 26, 1, 0),
                OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_fixedDate() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.STANDARD, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransition trans = new TZoneOffsetTransition(TLocalDateTime.of(2000, TMonth.MARCH, 20, 1, 0),
                OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_equals_monthDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.APRIL, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfMonthDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 21, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfWeekDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SATURDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfWeekDifferentNull() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_localTimeDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY,
                TLocalTime.MIDNIGHT, false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_endOfDayDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY,
                TLocalTime.MIDNIGHT, false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY,
                TLocalTime.MIDNIGHT, true, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_timeDefinitionDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.STANDARD, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_standardOffsetDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0300, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_offsetBeforeDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0300, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_offsetAfterDifferent() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0200);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_string_false() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals("TZDB"), false);
    }

    @Test
    public void test_equals_null_false() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(null), false);
    }

    @Test
    public void test_hashCode_floatingWeek_gap_notEndOfDay() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeek_overlap_endOfDay_nullDayOfWeek() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.OCTOBER, 20, null, TLocalTime.MIDNIGHT, true,
                TimeDefinition.WALL, OFFSET_0200, OFFSET_0300, OFFSET_0200);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.OCTOBER, 20, null, TLocalTime.MIDNIGHT, true,
                TimeDefinition.WALL, OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeekBackwards() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, -1, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, -1, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_fixedDate() {

        TZoneOffsetTransitionRule a = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.STANDARD, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        TZoneOffsetTransitionRule b = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.STANDARD, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_toString_floatingWeek_gap_notEndOfDay() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(),
                "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or after MARCH 20 at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeek_overlap_endOfDay() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.OCTOBER, 20, TDayOfWeek.SUNDAY,
                TLocalTime.MIDNIGHT, true, TimeDefinition.WALL, OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals(test.toString(),
                "TransitionRule[Overlap +03:00 to +02:00, SUNDAY on or after OCTOBER 20 at 24:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeekBackwards_last() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -1, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(),
                "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or before last day of MARCH at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeekBackwards_secondLast() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, -2, TDayOfWeek.SUNDAY, TIME_0100,
                false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(),
                "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or before last day minus 1 of MARCH at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_fixedDate() {

        TZoneOffsetTransitionRule test = TZoneOffsetTransitionRule.of(TMonth.MARCH, 20, null, TIME_0100, false,
                TimeDefinition.STANDARD, OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(),
                "TransitionRule[Gap +02:00 to +03:00, MARCH 20 at 01:00 STANDARD, standard offset +02:00]");
    }

}
