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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransitionRule.TimeDefinition;

@Test
public class TestFixedZoneRules {

    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);
    private static final TZoneOffset OFFSET_PTWO = TZoneOffset.ofHours(2);
    private static final TZoneOffset OFFSET_M18 = TZoneOffset.ofHours(-18);
    private static final TLocalDateTime LDT = TLocalDateTime.of(2010, 12, 3, 11, 30);
    private static final TInstant INSTANT = LDT.toInstant(OFFSET_PONE);

    private TZoneRules make(TZoneOffset offset) {
        return offset.getRules();
    }

    @DataProvider(name="rules")
    Object[][] data_rules() {
        return new Object[][] {
            {make(OFFSET_PONE), OFFSET_PONE},
            {make(OFFSET_PTWO), OFFSET_PTWO},
            {make(OFFSET_M18), OFFSET_M18},
        };
    }

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------
    @Test(dataProvider="rules")
    public void test_serialization(TZoneRules test, TZoneOffset expectedOffset) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(test);
        baos.close();
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(bais);
        TZoneRules result = (TZoneRules) in.readObject();

        assertEquals(result, test);
        assertEquals(result.getClass(), test.getClass());
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @Test
    public void test_data_nullInput() {
        TZoneRules test = make(OFFSET_PONE);
        assertEquals(test.getOffset((TInstant) null), OFFSET_PONE);
        assertEquals(test.getOffset((TLocalDateTime) null), OFFSET_PONE);
        assertEquals(test.getValidOffsets(null).size(), 1);
        assertEquals(test.getValidOffsets(null).get(0), OFFSET_PONE);
        assertEquals(test.getTransition(null), null);
        assertEquals(test.getStandardOffset(null), OFFSET_PONE);
        assertEquals(test.getDaylightSavings(null), TDuration.ZERO);
        assertEquals(test.isDaylightSavings(null), false);
        assertEquals(test.nextTransition(null), null);
        assertEquals(test.previousTransition(null), null);
    }

    @Test(dataProvider="rules")
    public void test_getOffset_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getOffset(INSTANT), expectedOffset);
        assertEquals(test.getOffset((TInstant) null), expectedOffset);
    }

    @Test(dataProvider="rules")
    public void test_getOffset_LocalDateTime(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getOffset(LDT), expectedOffset);
        assertEquals(test.getOffset((TLocalDateTime) null), expectedOffset);
    }

    @Test(dataProvider="rules")
    public void test_getValidOffsets_LDT(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getValidOffsets(LDT).size(), 1);
        assertEquals(test.getValidOffsets(LDT).get(0), expectedOffset);
        assertEquals(test.getValidOffsets(null).size(), 1);
        assertEquals(test.getValidOffsets(null).get(0), expectedOffset);
    }

    @Test(dataProvider="rules")
    public void test_getTransition_LDT(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getTransition(LDT), null);
        assertEquals(test.getTransition(null), null);
    }

    @Test(dataProvider="rules")
    public void test_isValidOffset_LDT_ZO(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.isValidOffset(LDT, expectedOffset), true);
        assertEquals(test.isValidOffset(LDT, TZoneOffset.UTC), false);
        assertEquals(test.isValidOffset(LDT, null), false);

        assertEquals(test.isValidOffset(null, expectedOffset), true);
        assertEquals(test.isValidOffset(null, TZoneOffset.UTC), false);
        assertEquals(test.isValidOffset(null, null), false);
    }

    @Test(dataProvider="rules")
    public void test_getStandardOffset_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getStandardOffset(INSTANT), expectedOffset);
        assertEquals(test.getStandardOffset(null), expectedOffset);
    }

    @Test(dataProvider="rules")
    public void test_getDaylightSavings_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getDaylightSavings(INSTANT), TDuration.ZERO);
        assertEquals(test.getDaylightSavings(null), TDuration.ZERO);
    }

    @Test(dataProvider="rules")
    public void test_isDaylightSavings_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.isDaylightSavings(INSTANT), false);
        assertEquals(test.isDaylightSavings(null), false);
    }

    //-------------------------------------------------------------------------
    @Test(dataProvider="rules")
    public void test_nextTransition_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.nextTransition(INSTANT), null);
        assertEquals(test.nextTransition(null), null);
    }

    @Test(dataProvider="rules")
    public void test_previousTransition_Instant(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.previousTransition(INSTANT), null);
        assertEquals(test.previousTransition(null), null);
    }

    //-------------------------------------------------------------------------
    @Test(dataProvider="rules")
    public void test_getTransitions(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getTransitions().size(), 0);
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitions_immutable() {
        TZoneRules test = make(OFFSET_PTWO);
        test.getTransitions().add(TZoneOffsetTransition.of(LDT, OFFSET_PONE, OFFSET_PTWO));
    }

    @Test(dataProvider="rules")
    public void test_getTransitionRules(TZoneRules test, TZoneOffset expectedOffset) {
        assertEquals(test.getTransitionRules().size(), 0);
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitionRules_immutable() {
        TZoneRules test = make(OFFSET_PTWO);
        test.getTransitionRules().add(TZoneOffsetTransitionRule.of(TMonth.JULY, 2, null, TLocalTime.of(12, 30), false, TimeDefinition.STANDARD, OFFSET_PONE, OFFSET_PTWO, OFFSET_PONE));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equalsHashCode() {
        TZoneRules a = make(OFFSET_PONE);
        TZoneRules b = make(OFFSET_PTWO);

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
