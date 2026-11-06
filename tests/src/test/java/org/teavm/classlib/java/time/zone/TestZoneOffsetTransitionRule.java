/*
 *  Copyright 2020 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.java.time.AbstractTest;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZoneOffsetTransitionRule.
 */
@TeaVMTest
public class TestZoneOffsetTransitionRule extends AbstractTest {

    private static final LocalTime TIME_0100 = LocalTime.of(1, 0);
    private static final ZoneOffset OFFSET_0200 = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_0300 = ZoneOffset.ofHours(3);

    //-----------------------------------------------------------------------
    // factory
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_nullMonth() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                null, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_nullTime() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, null, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_nullTimeDefinition() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, null,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_nullStandardOffset() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                null, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_nullOffsetBefore() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, null, OFFSET_0300));
    }

    @Test
    public void test_factory_nullOffsetAfter() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, null));
    }

    @Test
    public void test_factory_invalidDayOfMonthIndicator_tooSmall() {
        assertThrows(IllegalArgumentException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, -29, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_invalidDayOfMonthIndicator_zero() {
        assertThrows(IllegalArgumentException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 0, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_invalidDayOfMonthIndicator_tooLarge() {
        assertThrows(IllegalArgumentException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 32, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    @Test
    public void test_factory_invalidMidnightFlag() {
        assertThrows(IllegalArgumentException.class, () -> ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300));
    }

    //-----------------------------------------------------------------------
    // getters
    //-----------------------------------------------------------------------
    @Test
    public void test_getters_floatingWeek() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(Month.MARCH, test.getMonth());
        assertEquals(20, test.getDayOfMonthIndicator());
        assertEquals(DayOfWeek.SUNDAY, test.getDayOfWeek());
        assertEquals(TIME_0100, test.getLocalTime());
        assertFalse(test.isMidnightEndOfDay());
        assertEquals(TimeDefinition.WALL, test.getTimeDefinition());
        assertEquals(OFFSET_0200, test.getStandardOffset());
        assertEquals(OFFSET_0200, test.getOffsetBefore());
        assertEquals(OFFSET_0300, test.getOffsetAfter());
    }

    @Test
    public void test_getters_floatingWeekBackwards() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(Month.MARCH, test.getMonth());
        assertEquals(-1, test.getDayOfMonthIndicator());
        assertEquals(DayOfWeek.SUNDAY, test.getDayOfWeek());
        assertEquals(TIME_0100, test.getLocalTime());
        assertFalse(test.isMidnightEndOfDay());
        assertEquals(TimeDefinition.WALL, test.getTimeDefinition());
        assertEquals(OFFSET_0200, test.getStandardOffset());
        assertEquals(OFFSET_0200, test.getOffsetBefore());
        assertEquals(OFFSET_0300, test.getOffsetAfter());
    }

    @Test
    public void test_getters_fixedDate() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(Month.MARCH, test.getMonth());
        assertEquals(20, test.getDayOfMonthIndicator());
        assertEquals(null, test.getDayOfWeek());
        assertEquals(TIME_0100, test.getLocalTime());
        assertFalse(test.isMidnightEndOfDay());
        assertEquals(TimeDefinition.WALL, test.getTimeDefinition());
        assertEquals(OFFSET_0200, test.getStandardOffset());
        assertEquals(OFFSET_0200, test.getOffsetBefore());
        assertEquals(OFFSET_0300, test.getOffsetAfter());
    }

    @Test
    public void test_serialization_unusualOffsets() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                ZoneOffset.ofHoursMinutesSeconds(-12, -20, -50),
                ZoneOffset.ofHoursMinutesSeconds(-4, -10, -34),
                ZoneOffset.ofHours(-18));
    }

    @Test
    public void test_serialization_endOfDay() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.FRIDAY, LocalTime.MIDNIGHT, true, TimeDefinition.UTC,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test
    public void test_serialization_unusualTime() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.WEDNESDAY, LocalTime.of(13, 34, 56), false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                        Month.MARCH, 20, DayOfWeek.TUESDAY, LocalTime.of(13, 34, 56), false, TimeDefinition.STANDARD,
                        OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    //-----------------------------------------------------------------------
    // createTransition()
    //-----------------------------------------------------------------------
    @Test
    public void test_createTransition_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(trans, test.createTransition(2000));
    }

    @Test
    public void test_createTransition_floatingWeek_overlap_endOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 27, 0, 0), OFFSET_0300, OFFSET_0200);
        assertEquals(trans, test.createTransition(2000));
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_last() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(trans, test.createTransition(2000));
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_seventhLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -7, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 19, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(trans, test.createTransition(2000));
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_secondLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -2, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(trans, test.createTransition(2000));
    }

    @Test
    public void test_createTransition_fixedDate() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 20, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(trans, test.createTransition(2000));
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_monthDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.APRIL, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_dayOfMonthDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 21, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_dayOfWeekDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SATURDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_dayOfWeekDifferentNull() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_localTimeDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_endOfDayDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_timeDefinitionDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_standardOffsetDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0300, OFFSET_0200, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_offsetBeforeDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0300);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_offsetAfterDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0200);
        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));
    }

    @Test
    public void test_equals_string_false() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertFalse(a.equals("TZDB"));
    }

    @Test
    public void test_equals_null_false() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertFalse(a.equals(null));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeek_overlap_endOfDay_nullDayOfWeek() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, null, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, null, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeekBackwards() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_hashCode_fixedDate() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(b.hashCode(), a.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals("TransitionRule[Gap +02:00 to +03:00, "
                + "SUNDAY on or after MARCH 20 at 01:00 WALL, standard offset +02:00]", test.toString());
    }

    @Test
    public void test_toString_floatingWeek_overlap_endOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals("TransitionRule[Overlap +03:00 to +02:00, "
                + "SUNDAY on or after OCTOBER 20 at 24:00 WALL, standard offset +02:00]", test.toString());
    }

    @Test
    public void test_toString_floatingWeekBackwards_last() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals("TransitionRule[Gap +02:00 to +03:00, "
                + "SUNDAY on or before last day of MARCH at 01:00 WALL, standard offset +02:00]", test.toString());
    }

    @Test
    public void test_toString_floatingWeekBackwards_secondLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -2, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals("TransitionRule[Gap +02:00 to +03:00, "
                + "SUNDAY on or before last day minus 1 of MARCH at 01:00 WALL, standard offset +02:00]",
                test.toString());
    }

    @Test
    public void test_toString_fixedDate() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals("TransitionRule[Gap +02:00 to +03:00, MARCH 20 at 01:00 STANDARD, " + "standard offset +02:00]",
                test.toString());
    }

}
