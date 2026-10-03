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

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.java.time.AbstractTest;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZoneOffsetTransition.
 */
@TeaVMTest
public class TestZoneOffsetTransition extends AbstractTest {

    private static final ZoneOffset OFFSET_0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_0200 = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_0230 = ZoneOffset.ofHoursMinutes(2, 30);
    private static final ZoneOffset OFFSET_0300 = ZoneOffset.ofHours(3);
    private static final ZoneOffset OFFSET_0400 = ZoneOffset.ofHours(4);

    //-----------------------------------------------------------------------
    // factory
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_nullTransition() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetTransition.of(null, OFFSET_0100, OFFSET_0200));
    }

    @Test
    public void test_factory_nullOffsetBefore() {
        assertThrows(NullPointerException.class,
                () -> ZoneOffsetTransition.of(LocalDateTime.of(2010, 12, 3, 11, 30), null, OFFSET_0200));
    }

    @Test
    public void test_factory_nullOffsetAfter() {
        assertThrows(NullPointerException.class,
                () -> ZoneOffsetTransition.of(LocalDateTime.of(2010, 12, 3, 11, 30), OFFSET_0200, null));
    }

    @Test
    public void test_factory_sameOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> ZoneOffsetTransition.of(LocalDateTime.of(2010, 12, 3, 11, 30), OFFSET_0200, OFFSET_0200));
    }

    @Test
    public void test_factory_noNanos() {
        assertThrows(IllegalArgumentException.class,
                () -> ZoneOffsetTransition.of(LocalDateTime.of(2010, 12, 3, 11, 30, 0, 500), OFFSET_0200, OFFSET_0300));
    }

    //-----------------------------------------------------------------------
    // getters
    //-----------------------------------------------------------------------
    @Test
    public void test_getters_gap() throws Exception {
        LocalDateTime before = LocalDateTime.of(2010, 3, 31, 1, 0);
        LocalDateTime after = LocalDateTime.of(2010, 3, 31, 2, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(before, OFFSET_0200, OFFSET_0300);
        assertTrue(test.isGap());
        assertFalse(test.isOverlap());
        assertEquals(before, test.getDateTimeBefore());
        assertEquals(after, test.getDateTimeAfter());
        assertEquals(before.toInstant(OFFSET_0200), test.getInstant());
        assertEquals(OFFSET_0200, test.getOffsetBefore());
        assertEquals(OFFSET_0300, test.getOffsetAfter());
        assertEquals(Duration.of(1, HOURS), test.getDuration());
    }

    @Test
    public void test_getters_overlap() throws Exception {
        LocalDateTime before = LocalDateTime.of(2010, 10, 31, 1, 0);
        LocalDateTime after = LocalDateTime.of(2010, 10, 31, 0, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(before, OFFSET_0300, OFFSET_0200);
        assertFalse(test.isGap());
        assertTrue(test.isOverlap());
        assertEquals(before, test.getDateTimeBefore());
        assertEquals(after, test.getDateTimeAfter());
        assertEquals(before.toInstant(OFFSET_0300), test.getInstant());
        assertEquals(OFFSET_0300, test.getOffsetBefore());
        assertEquals(OFFSET_0200, test.getOffsetAfter());
        assertEquals(Duration.of(-1, HOURS), test.getDuration());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_serialization_unusual1() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(Year.MAX_VALUE, 12, 31, 1, 31, 53);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, ZoneOffset.of("+02:04:56"),
                ZoneOffset.of("-10:02:34"));
    }

    @Test
    public void test_serialization_unusual2() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(Year.MIN_VALUE, 1, 1, 12, 1, 3);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, ZoneOffset.of("+02:04:56"),
                ZoneOffset.of("+10:02:34"));
    }

    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        LocalDateTime ldt = LocalDateTime.of(Year.MIN_VALUE, 1, 1, 12, 1, 3);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, ZoneOffset.of("+02:04:56"),
                ZoneOffset.of("+10:02:34"));
    }

    //-----------------------------------------------------------------------
    // isValidOffset()
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidOffset_gap() {
        LocalDateTime ldt = LocalDateTime.of(2010, 3, 31, 1, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, OFFSET_0200, OFFSET_0300);
        assertFalse(test.isValidOffset(OFFSET_0100));
        assertFalse(test.isValidOffset(OFFSET_0200));
        assertFalse(test.isValidOffset(OFFSET_0230));
        assertFalse(test.isValidOffset(OFFSET_0300));
        assertFalse(test.isValidOffset(OFFSET_0400));
    }

    @Test
    public void test_isValidOffset_overlap() {
        LocalDateTime ldt = LocalDateTime.of(2010, 10, 31, 1, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, OFFSET_0300, OFFSET_0200);
        assertFalse(test.isValidOffset(OFFSET_0100));
        assertTrue(test.isValidOffset(OFFSET_0200));
        assertFalse(test.isValidOffset(OFFSET_0230));
        assertTrue(test.isValidOffset(OFFSET_0300));
        assertFalse(test.isValidOffset(OFFSET_0400));
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo() {
        ZoneOffsetTransition a = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L - 1, 0, OFFSET_0200), OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition b = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L, 0, OFFSET_0300), OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransition c = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L + 1, 0, OFFSET_0100), OFFSET_0100, OFFSET_0400);

        assertTrue(a.compareTo(a) == 0);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(a.compareTo(c) < 0);

        assertTrue(b.compareTo(a) > 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(b.compareTo(c) < 0);

        assertTrue(c.compareTo(a) > 0);
        assertTrue(c.compareTo(b) > 0);
        assertTrue(c.compareTo(c) == 0);
    }

    @Test
    public void test_compareTo_sameInstant() {
        ZoneOffsetTransition a = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L, 0, OFFSET_0200), OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition b = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L, 0, OFFSET_0300), OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransition c = ZoneOffsetTransition.of(
                LocalDateTime.ofEpochSecond(23875287L, 0, OFFSET_0100), OFFSET_0100, OFFSET_0400);

        assertTrue(a.compareTo(a) == 0);
        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.compareTo(c) == 0);

        assertTrue(b.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(b.compareTo(c) == 0);

        assertTrue(c.compareTo(a) == 0);
        assertTrue(c.compareTo(b) == 0);
        assertTrue(c.compareTo(c) == 0);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        LocalDateTime ldtA = LocalDateTime.of(2010, 3, 31, 1, 0);
        ZoneOffsetTransition a1 = ZoneOffsetTransition.of(ldtA, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition a2 = ZoneOffsetTransition.of(ldtA, OFFSET_0200, OFFSET_0300);
        LocalDateTime ldtB = LocalDateTime.of(2010, 10, 31, 1, 0);
        ZoneOffsetTransition b = ZoneOffsetTransition.of(ldtB, OFFSET_0300, OFFSET_0200);

        assertTrue(a1.equals(a1));
        assertTrue(a1.equals(a2));
        assertFalse(a1.equals(b));
        assertTrue(a2.equals(a1));
        assertTrue(a2.equals(a2));
        assertFalse(a2.equals(b));
        assertFalse(b.equals(a1));
        assertFalse(b.equals(a2));
        assertTrue(b.equals(b));

        assertFalse(a1.equals(""));
        assertFalse(a1.equals(null));
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode_floatingWeek_gap_notEndOfDay() {
        LocalDateTime ldtA = LocalDateTime.of(2010, 3, 31, 1, 0);
        ZoneOffsetTransition a1 = ZoneOffsetTransition.of(ldtA, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition a2 = ZoneOffsetTransition.of(ldtA, OFFSET_0200, OFFSET_0300);
        LocalDateTime ldtB = LocalDateTime.of(2010, 10, 31, 1, 0);
        ZoneOffsetTransition b = ZoneOffsetTransition.of(ldtB, OFFSET_0300, OFFSET_0200);

        assertEquals(a1.hashCode(), a1.hashCode());
        assertEquals(a2.hashCode(), a1.hashCode());
        assertEquals(b.hashCode(), b.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString_gap() {
        LocalDateTime ldt = LocalDateTime.of(2010, 3, 31, 1, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, OFFSET_0200, OFFSET_0300);
        assertEquals("Transition[Gap at 2010-03-31T01:00+02:00 to +03:00]", test.toString());
    }

    @Test
    public void test_toString_overlap() {
        LocalDateTime ldt = LocalDateTime.of(2010, 10, 31, 1, 0);
        ZoneOffsetTransition test = ZoneOffsetTransition.of(ldt, OFFSET_0300, OFFSET_0200);
        assertEquals("Transition[Overlap at 2010-10-31T01:00+03:00 to +02:00]", test.toString());
    }

}
