/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.time.zone.ZoneRules;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestFixedZoneRules {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);

    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);

    private static final ZoneOffset OFFSET_M18 = ZoneOffset.ofHours(-18);

    private static final LocalDateTime LDT = LocalDateTime.of(2010, 12, 3, 11, 30);

    private static final Instant INSTANT = LDT.toInstant(OFFSET_PONE);

    private ZoneRules make(ZoneOffset offset) {

        return offset.getRules();
    }

    Object[][] data_rules() {

        return new Object[][] { { make(OFFSET_PONE), OFFSET_PONE }, { make(OFFSET_PTWO), OFFSET_PTWO },
        { make(OFFSET_M18), OFFSET_M18 }, };
    }

    @Test
    public void test_data_nullInput() {

        ZoneRules test = make(OFFSET_PONE);
        assertEquals(test.getOffset((Instant) null), OFFSET_PONE);
        assertEquals(test.getOffset((LocalDateTime) null), OFFSET_PONE);
        assertEquals(test.getValidOffsets(null).size(), 1);
        assertEquals(test.getValidOffsets(null).get(0), OFFSET_PONE);
        assertEquals(test.getTransition(null), null);
        assertEquals(test.getStandardOffset(null), OFFSET_PONE);
        assertEquals(test.getDaylightSavings(null), Duration.ZERO);
        assertEquals(test.isDaylightSavings(null), false);
        assertEquals(test.nextTransition(null), null);
        assertEquals(test.previousTransition(null), null);
    }

    @Test
    public void test_getOffset_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getOffset(INSTANT), expectedOffset);
            assertEquals(test.getOffset((Instant) null), expectedOffset);
        }
    }

    @Test
    public void test_getOffset_LocalDateTime() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getOffset(LDT), expectedOffset);
            assertEquals(test.getOffset((LocalDateTime) null), expectedOffset);
        }
    }

    @Test
    public void test_getValidOffsets_LDT() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getValidOffsets(LDT).size(), 1);
            assertEquals(test.getValidOffsets(LDT).get(0), expectedOffset);
            assertEquals(test.getValidOffsets(null).size(), 1);
            assertEquals(test.getValidOffsets(null).get(0), expectedOffset);
        }
    }

    @Test
    public void test_getTransition_LDT() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getTransition(LDT), null);
            assertEquals(test.getTransition(null), null);
        }
    }

    @Test
    public void test_isValidOffset_LDT_ZO() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.isValidOffset(LDT, expectedOffset), true);
            assertEquals(test.isValidOffset(LDT, ZoneOffset.UTC), false);
            assertEquals(test.isValidOffset(LDT, null), false);

            assertEquals(test.isValidOffset(null, expectedOffset), true);
            assertEquals(test.isValidOffset(null, ZoneOffset.UTC), false);
            assertEquals(test.isValidOffset(null, null), false);
        }
    }

    @Test
    public void test_getStandardOffset_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getStandardOffset(INSTANT), expectedOffset);
            assertEquals(test.getStandardOffset(null), expectedOffset);
        }
    }

    @Test
    public void test_getDaylightSavings_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];
            ZoneOffset expectedOffset = (ZoneOffset) data[1];

            assertEquals(test.getDaylightSavings(INSTANT), Duration.ZERO);
            assertEquals(test.getDaylightSavings(null), Duration.ZERO);
        }
    }

    @Test
    public void test_isDaylightSavings_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];

            assertEquals(test.isDaylightSavings(INSTANT), false);
            assertEquals(test.isDaylightSavings(null), false);
        }
    }

    @Test
    public void test_nextTransition_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];

            assertEquals(test.nextTransition(INSTANT), null);
            assertEquals(test.nextTransition(null), null);
        }
    }

    @Test
    public void test_previousTransition_Instant() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];

            assertEquals(test.previousTransition(INSTANT), null);
            assertEquals(test.previousTransition(null), null);
        }
    }

    @Test
    public void test_getTransitions() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];

            assertEquals(test.getTransitions().size(), 0);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getTransitions_immutable() {

        ZoneRules test = make(OFFSET_PTWO);
        test.getTransitions().add(ZoneOffsetTransition.of(LDT, OFFSET_PONE, OFFSET_PTWO));
    }

    @Test
    public void test_getTransitionRules() {

        for (Object[] data : data_rules()) {
            ZoneRules test = (ZoneRules) data[0];

            assertEquals(test.getTransitionRules().size(), 0);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getTransitionRules_immutable() {

        ZoneRules test = make(OFFSET_PTWO);
        test.getTransitionRules().add(ZoneOffsetTransitionRule.of(Month.JULY, 2, null, LocalTime.of(12, 30), false,
                TimeDefinition.STANDARD, OFFSET_PONE, OFFSET_PTWO, OFFSET_PONE));
    }

    @Test
    public void test_equalsHashCode() {

        ZoneRules a = make(OFFSET_PONE);
        ZoneRules b = make(OFFSET_PTWO);

        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);

        assertEquals(a.equals("Rubbish"), false);
        assertEquals(a.equals(null), false);

        assertEquals(a.hashCode() == a.hashCode(), true);
        assertEquals(b.hashCode() == b.hashCode(), true);
    }

}
