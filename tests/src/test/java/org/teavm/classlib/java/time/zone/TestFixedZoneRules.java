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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZoneRules for fixed offset time-zones.
 */
@TeaVMTest
public class TestFixedZoneRules {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_M18 = ZoneOffset.ofHours(-18);
    private static final LocalDateTime LDT = LocalDateTime.of(2010, 12, 3, 11, 30);
    private static final Instant INSTANT = LDT.toInstant(OFFSET_PONE);

    private static ZoneRules make(ZoneOffset offset) {
        return offset.getRules();
    }

    static Object[][] data_rules() {
        return new Object[][] {
            {make(OFFSET_PONE), OFFSET_PONE},
            {make(OFFSET_PTWO), OFFSET_PTWO},
            {make(OFFSET_M18), OFFSET_M18},
        };
    }

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @Test
    public void test_data_nullInput() {
        ZoneRules test = make(OFFSET_PONE);
        assertEquals(OFFSET_PONE, test.getOffset((Instant) null));
        assertEquals(OFFSET_PONE, test.getOffset((LocalDateTime) null));
        assertEquals(1, test.getValidOffsets(null).size());
        assertEquals(OFFSET_PONE, test.getValidOffsets(null).get(0));
        assertEquals(null, test.getTransition(null));
        assertEquals(OFFSET_PONE, test.getStandardOffset(null));
        assertEquals(Duration.ZERO, test.getDaylightSavings(null));
        assertFalse(test.isDaylightSavings(null));
        assertEquals(null, test.nextTransition(null));
        assertEquals(null, test.previousTransition(null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getOffset_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(expectedOffset, test.getOffset(INSTANT));
        assertEquals(expectedOffset, test.getOffset((Instant) null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getOffset_LocalDateTime(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(expectedOffset, test.getOffset(LDT));
        assertEquals(expectedOffset, test.getOffset((LocalDateTime) null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getValidOffsets_LDT(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(1, test.getValidOffsets(LDT).size());
        assertEquals(expectedOffset, test.getValidOffsets(LDT).get(0));
        assertEquals(1, test.getValidOffsets(null).size());
        assertEquals(expectedOffset, test.getValidOffsets(null).get(0));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getTransition_LDT(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(null, test.getTransition(LDT));
        assertEquals(null, test.getTransition(null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_isValidOffset_LDT_ZO(ZoneRules test, ZoneOffset expectedOffset) {
        assertTrue(test.isValidOffset(LDT, expectedOffset));
        assertFalse(test.isValidOffset(LDT, ZoneOffset.UTC));
        assertFalse(test.isValidOffset(LDT, null));

        assertTrue(test.isValidOffset(null, expectedOffset));
        assertFalse(test.isValidOffset(null, ZoneOffset.UTC));
        assertFalse(test.isValidOffset(null, null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getStandardOffset_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(expectedOffset, test.getStandardOffset(INSTANT));
        assertEquals(expectedOffset, test.getStandardOffset(null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getDaylightSavings_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(Duration.ZERO, test.getDaylightSavings(INSTANT));
        assertEquals(Duration.ZERO, test.getDaylightSavings(null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_isDaylightSavings_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertFalse(test.isDaylightSavings(INSTANT));
        assertFalse(test.isDaylightSavings(null));
    }

    //-------------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_nextTransition_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(null, test.nextTransition(INSTANT));
        assertEquals(null, test.nextTransition(null));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_previousTransition_Instant(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(null, test.previousTransition(INSTANT));
        assertEquals(null, test.previousTransition(null));
    }

    //-------------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getTransitions(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(0, test.getTransitions().size());
    }

    @Test
    public void test_getTransitions_immutable() {
        ZoneRules test = make(OFFSET_PTWO);
        assertThrows(UnsupportedOperationException.class,
                () -> test.getTransitions().add(ZoneOffsetTransition.of(LDT, OFFSET_PONE, OFFSET_PTWO)));
    }

    @ParameterizedTest
    @MethodSource("data_rules")
    public void test_getTransitionRules(ZoneRules test, ZoneOffset expectedOffset) {
        assertEquals(0, test.getTransitionRules().size());
    }

    @Test
    public void test_getTransitionRules_immutable() {
        ZoneRules test = make(OFFSET_PTWO);
        assertThrows(UnsupportedOperationException.class,
                () -> test.getTransitionRules().add(ZoneOffsetTransitionRule.of(Month.JULY, 2, null,
                LocalTime.of(12, 30), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD,
                OFFSET_PONE, OFFSET_PTWO, OFFSET_PONE)));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equalsHashCode() {
        ZoneRules a = make(OFFSET_PONE);
        ZoneRules b = make(OFFSET_PTWO);

        assertTrue(a.equals(a));
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(b.equals(b));

        assertFalse(a.equals("Rubbish"));
        assertFalse(a.equals(null));

        assertTrue(a.hashCode() == a.hashCode());
        assertTrue(b.hashCode() == b.hashCode());
    }

}
