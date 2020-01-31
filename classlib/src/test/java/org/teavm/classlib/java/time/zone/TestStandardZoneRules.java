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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransitionRule.TimeDefinition;

@Test
public class TestStandardZoneRules {

    private static final TZoneOffset OFFSET_ZERO = TZoneOffset.ofHours(0);
    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);
    private static final TZoneOffset OFFSET_PTWO = TZoneOffset.ofHours(2);
    public static final String LATEST_TZDB = "2009b";
    private static final int OVERLAP = 2;
    private static final int GAP = 0;

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------
    public void test_serialization_loaded() throws Exception {
        assertSerialization(europeLondon());
        assertSerialization(europeParis());
        assertSerialization(americaNewYork());
    }

    private void assertSerialization(TZoneRules test) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(test);
        baos.close();
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(bais);
        TZoneRules result = (TZoneRules) in.readObject();

        assertEquals(result, test);
    }
    
    //-----------------------------------------------------------------------
    // Etc/GMT
    //-----------------------------------------------------------------------
    private TZoneRules etcGmt() {
        return TZoneId.of("Etc/GMT").getRules();
    }
    
    public void test_EtcGmt_nextTransition() {
        assertNull(etcGmt().nextTransition(TInstant.EPOCH));
    }

    public void test_EtcGmt_previousTransition() {
        assertNull(etcGmt().previousTransition(TInstant.EPOCH));
    }

    //-----------------------------------------------------------------------
    // Europe/London
    //-----------------------------------------------------------------------
    private TZoneRules europeLondon() {
        return TZoneId.of("Europe/London").getRules();
    }

    public void test_London() {
        TZoneRules test = europeLondon();
        assertEquals(test.isFixedOffset(), false);
    }

    public void test_London_preTimeZones() {
        TZoneRules test = europeLondon();
        TZonedDateTime old = createZDT(1800, 1, 1, TZoneOffset.UTC);
        TInstant instant = old.toInstant();
        TZoneOffset offset = TZoneOffset.ofHoursMinutesSeconds(0, -1, -15);
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), TDuration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    public void test_London_getOffset() {
        TZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 1, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 2, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 4, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 5, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 6, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 7, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 8, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 9, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 12, 1, TZoneOffset.UTC)), OFFSET_ZERO);
    }

    public void test_London_getOffset_toDST() {
        TZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 3, 24, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 25, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 26, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 27, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 28, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 29, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 31, TZoneOffset.UTC)), OFFSET_PONE);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_PONE);
    }

    public void test_London_getOffset_fromDST() {
        TZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 10, 24, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 25, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 27, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 28, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 29, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 30, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 31, TZoneOffset.UTC)), OFFSET_ZERO);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_ZERO);
    }

    public void test_London_getOffsetInfo() {
        TZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 1, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 2, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 4, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 5, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 6, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 7, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 8, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 9, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 11, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 12, 1), OFFSET_ZERO, 1);
    }

    public void test_London_getOffsetInfo_toDST() {
        TZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 3, 24), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 25), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 26), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 31), OFFSET_PONE, 1);
        // cutover at 01:00Z
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), OFFSET_ZERO, 1);
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), OFFSET_PONE, 1);
    }

    public void test_London_getOffsetInfo_fromDST() {
        TZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 10, 24), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 25), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 26), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 31), OFFSET_ZERO, 1);
        // cutover at 01:00Z
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), OFFSET_PONE, 1);
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), OFFSET_ZERO, 1);
    }

    public void test_London_getOffsetInfo_gap() {
        TZoneRules test = europeLondon();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_ZERO, GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(trans.getOffsetAfter(), OFFSET_PONE);
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 3, 30, 1, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 3, 30, 2, 0));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), false);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T01:00Z to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_ZERO));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_London_getOffsetInfo_overlap() {
        TZoneRules test = europeLondon();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PONE, OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), OFFSET_PONE);
        assertEquals(trans.getOffsetAfter(), OFFSET_ZERO);
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 10, 26, 2, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 10, 26, 1, 0));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(OFFSET_ZERO), true);
        assertEquals(trans.isValidOffset(OFFSET_PONE), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T02:00+01:00 to Z]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PONE));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_London_getStandardOffset() {
        TZoneRules test = europeLondon();
        TZonedDateTime zdt = createZDT(1840, 1, 1, TZoneOffset.UTC);
        while (zdt.getYear() < 2010) {
            TInstant instant = zdt.toInstant();
            if (zdt.getYear() < 1848) {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.ofHoursMinutesSeconds(0, -1, -15));
            } else if (zdt.getYear() >= 1969 && zdt.getYear() < 1972) {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            } else {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            }
            zdt = zdt.plusMonths(6);
        }
    }

    public void test_London_getTransitions() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition first = trans.get(0);
        assertEquals(first.getDateTimeBefore(), TLocalDateTime.of(1847, 12, 1, 0, 0));
        assertEquals(first.getOffsetBefore(), TZoneOffset.ofHoursMinutesSeconds(0, -1, -15));
        assertEquals(first.getOffsetAfter(), OFFSET_ZERO);

        TZoneOffsetTransition spring1916 = trans.get(1);
        assertEquals(spring1916.getDateTimeBefore(), TLocalDateTime.of(1916, 5, 21, 2, 0));
        assertEquals(spring1916.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(spring1916.getOffsetAfter(), OFFSET_PONE);

        TZoneOffsetTransition autumn1916 = trans.get(2);
        assertEquals(autumn1916.getDateTimeBefore(), TLocalDateTime.of(1916, 10, 1, 3, 0));
        assertEquals(autumn1916.getOffsetBefore(), OFFSET_PONE);
        assertEquals(autumn1916.getOffsetAfter(), OFFSET_ZERO);

        TZoneOffsetTransition zot = null;
        Iterator<TZoneOffsetTransition> it = trans.iterator();
        while (it.hasNext()) {
            zot = it.next();
            if (zot.getDateTimeBefore().getYear() == 1990) {
                break;
            }
        }
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1990, 3, 25, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1990, 10, 28, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1991, 3, 31, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1991, 10, 27, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1992, 3, 29, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1992, 10, 25, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1993, 3, 28, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1993, 10, 24, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1994, 3, 27, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1994, 10, 23, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1995, 3, 26, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1995, 10, 22, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1996, 3, 31, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1996, 10, 27, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1997, 3, 30, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), TLocalDateTime.of(1997, 10, 26, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        assertEquals(it.hasNext(), false);
    }

    public void test_London_getTransitionRules() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransitionRule> rules = test.getTransitionRules();
        assertEquals(rules.size(), 2);

        TZoneOffsetTransitionRule in = rules.get(0);
        assertEquals(in.getMonth(), TMonth.MARCH);
        assertEquals(in.getDayOfMonthIndicator(), 25);  // optimized from -1
        assertEquals(in.getDayOfWeek(), TDayOfWeek.SUNDAY);
        assertEquals(in.getLocalTime(), TLocalTime.of(1, 0));
        assertEquals(in.getTimeDefinition(), TimeDefinition.UTC);
        assertEquals(in.getStandardOffset(), OFFSET_ZERO);
        assertEquals(in.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(in.getOffsetAfter(), OFFSET_PONE);

        TZoneOffsetTransitionRule out = rules.get(1);
        assertEquals(out.getMonth(), TMonth.OCTOBER);
        assertEquals(out.getDayOfMonthIndicator(), 25);  // optimized from -1
        assertEquals(out.getDayOfWeek(), TDayOfWeek.SUNDAY);
        assertEquals(out.getLocalTime(), TLocalTime.of(1, 0));
        assertEquals(out.getTimeDefinition(), TimeDefinition.UTC);
        assertEquals(out.getStandardOffset(), OFFSET_ZERO);
        assertEquals(out.getOffsetBefore(), OFFSET_PONE);
        assertEquals(out.getOffsetAfter(), OFFSET_ZERO);
    }

    //-----------------------------------------------------------------------
    public void test_London_nextTransition_historic() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition first = trans.get(0);
        assertEquals(test.nextTransition(first.getInstant().minusNanos(1)), first);

        for (int i = 0; i < trans.size() - 1; i++) {
            TZoneOffsetTransition cur = trans.get(i);
            TZoneOffsetTransition next = trans.get(i + 1);

            assertEquals(test.nextTransition(cur.getInstant()), next);
            assertEquals(test.nextTransition(next.getInstant().minusNanos(1)), next);
        }
    }

    public void test_London_nextTransition_rulesBased() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransitionRule> rules = test.getTransitionRules();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.nextTransition(last.getInstant()), rules.get(0).createTransition(1998));

        for (int year = 1998; year < 2010; year++) {
            TZoneOffsetTransition a = rules.get(0).createTransition(year);
            TZoneOffsetTransition b = rules.get(1).createTransition(year);
            TZoneOffsetTransition c = rules.get(0).createTransition(year + 1);

            assertEquals(test.nextTransition(a.getInstant()), b);
            assertEquals(test.nextTransition(b.getInstant().minusNanos(1)), b);

            assertEquals(test.nextTransition(b.getInstant()), c);
            assertEquals(test.nextTransition(c.getInstant().minusNanos(1)), c);
        }
    }

    public void test_London_nextTransition_lastYear() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransitionRule> rules = test.getTransitionRules();
        TZoneOffsetTransition zot = rules.get(1).createTransition(TYear.MAX_VALUE);
        assertEquals(test.nextTransition(zot.getInstant()), null);
    }

    //-----------------------------------------------------------------------
    public void test_London_previousTransition_historic() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition first = trans.get(0);
        assertEquals(test.previousTransition(first.getInstant()), null);
        assertEquals(test.previousTransition(first.getInstant().minusNanos(1)), null);

        for (int i = 0; i < trans.size() - 1; i++) {
            TZoneOffsetTransition prev = trans.get(i);
            TZoneOffsetTransition cur = trans.get(i + 1);

            assertEquals(test.previousTransition(cur.getInstant()), prev);
            assertEquals(test.previousTransition(prev.getInstant().plusSeconds(1)), prev);
            assertEquals(test.previousTransition(prev.getInstant().plusNanos(1)), prev);
        }
    }

    public void test_London_previousTransition_rulesBased() {
        TZoneRules test = europeLondon();
        List<TZoneOffsetTransitionRule> rules = test.getTransitionRules();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.previousTransition(last.getInstant().plusSeconds(1)), last);
        assertEquals(test.previousTransition(last.getInstant().plusNanos(1)), last);

        // Jan 1st of year between transitions and rules
        TZonedDateTime odt = TZonedDateTime.ofInstant(last.getInstant(), last.getOffsetAfter());
        odt = odt.withDayOfYear(1).plusYears(1).with(TLocalTime.MIDNIGHT);
        assertEquals(test.previousTransition(odt.toInstant()), last);

        // later years
        for (int year = 1998; year < 2010; year++) {
            TZoneOffsetTransition a = rules.get(0).createTransition(year);
            TZoneOffsetTransition b = rules.get(1).createTransition(year);
            TZoneOffsetTransition c = rules.get(0).createTransition(year + 1);

            assertEquals(test.previousTransition(c.getInstant()), b);
            assertEquals(test.previousTransition(b.getInstant().plusSeconds(1)), b);
            assertEquals(test.previousTransition(b.getInstant().plusNanos(1)), b);

            assertEquals(test.previousTransition(b.getInstant()), a);
            assertEquals(test.previousTransition(a.getInstant().plusSeconds(1)), a);
            assertEquals(test.previousTransition(a.getInstant().plusNanos(1)), a);
        }
    }

    //-----------------------------------------------------------------------
    // Europe/Dublin
    //-----------------------------------------------------------------------
    private TZoneRules europeDublin() {
        return TZoneId.of("Europe/Dublin").getRules();
    }

    public void test_Dublin() {
        TZoneRules test = europeDublin();
        assertEquals(test.isFixedOffset(), false);
    }

    public void test_Dublin_getOffset() {
        TZoneRules test = europeDublin();
        assertEquals(test.getOffset(createInstant(2008, 1, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 2, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 4, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 5, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 6, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 7, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 8, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 9, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 12, 1, TZoneOffset.UTC)), OFFSET_ZERO);
    }

    public void test_Dublin_getOffset_toDST() {
        TZoneRules test = europeDublin();
        assertEquals(test.getOffset(createInstant(2008, 3, 24, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 25, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 26, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 27, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 28, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 29, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 31, TZoneOffset.UTC)), OFFSET_PONE);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_PONE);
    }

    public void test_Dublin_getOffset_fromDST() {
        TZoneRules test = europeDublin();
        assertEquals(test.getOffset(createInstant(2008, 10, 24, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 25, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 27, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 28, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 29, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 30, TZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 31, TZoneOffset.UTC)), OFFSET_ZERO);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_ZERO);
    }

    public void test_Dublin_getOffsetInfo() {
        TZoneRules test = europeDublin();
        checkOffset(test, createLDT(2008, 1, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 2, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 4, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 5, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 6, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 7, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 8, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 9, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 11, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 12, 1), OFFSET_ZERO, 1);
    }

    public void test_Dublin_getOffsetInfo_toDST() {
        TZoneRules test = europeDublin();
        checkOffset(test, createLDT(2008, 3, 24), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 25), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 26), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 31), OFFSET_PONE, 1);
        // cutover at 01:00Z
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), OFFSET_ZERO, 1);
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), OFFSET_PONE, 1);
    }

    public void test_Dublin_getOffsetInfo_fromDST() {
        TZoneRules test = europeDublin();
        checkOffset(test, createLDT(2008, 10, 24), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 25), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 26), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 31), OFFSET_ZERO, 1);
        // cutover at 01:00Z
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), OFFSET_PONE, 1);
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), OFFSET_ZERO, 1);
    }

    public void test_Dublin_getOffsetInfo_gap() {
        TZoneRules test = europeDublin();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_ZERO, GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(trans.getOffsetAfter(), OFFSET_PONE);
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 3, 30, 1, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 3, 30, 2, 0));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), false);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T01:00Z to +01:00]");
    }

    public void test_Dublin_getOffsetInfo_overlap() {
        TZoneRules test = europeDublin();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PONE, OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), OFFSET_PONE);
        assertEquals(trans.getOffsetAfter(), OFFSET_ZERO);
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 10, 26, 2, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 10, 26, 1, 0));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(OFFSET_ZERO), true);
        assertEquals(trans.isValidOffset(OFFSET_PONE), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T02:00+01:00 to Z]");
    }

    public void test_Dublin_getStandardOffset() {
        TZoneRules test = europeDublin();
        TZonedDateTime zdt = createZDT(1840, 1, 1, TZoneOffset.UTC);
        while (zdt.getYear() < 2010) {
            TInstant instant = zdt.toInstant();
            if (zdt.getYear() < 1881) {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.ofHoursMinutes(0, -25));
            } else if (zdt.getYear() >= 1881 && zdt.getYear() < 1917) {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.ofHoursMinutesSeconds(0, -25, -21));
            } else if (zdt.getYear() >= 1917 && zdt.getYear() < 1969) {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO, zdt.toString());
            } else if (zdt.getYear() >= 1969 && zdt.getYear() < 1972) {
                // from 1968-02-18 to 1971-10-31, permanent UTC+1
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
                assertEquals(test.getOffset(instant), OFFSET_PONE, zdt.toString());
            } else {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO, zdt.toString());
                assertEquals(test.getOffset(instant), zdt.getMonth() == TMonth.JANUARY ? OFFSET_ZERO : OFFSET_PONE, zdt.toString());
            }
            zdt = zdt.plusMonths(6);
        }
    }

    public void test_Dublin_dst() {
        TZoneRules test = europeDublin();
        assertEquals(test.isDaylightSavings(createZDT(1960, 1, 1, TZoneOffset.UTC).toInstant()), false);
        assertEquals(test.getDaylightSavings(createZDT(1960, 1, 1, TZoneOffset.UTC).toInstant()), TDuration.ofHours(0));
        assertEquals(test.isDaylightSavings(createZDT(1960, 7, 1, TZoneOffset.UTC).toInstant()), true);
        assertEquals(test.getDaylightSavings(createZDT(1960, 7, 1, TZoneOffset.UTC).toInstant()), TDuration.ofHours(1));
        // check negative DST is correctly handled
        assertEquals(test.isDaylightSavings(createZDT(2016, 1, 1, TZoneOffset.UTC).toInstant()), false);
        assertEquals(test.getDaylightSavings(createZDT(2016, 1, 1, TZoneOffset.UTC).toInstant()), TDuration.ofHours(0));
        assertEquals(test.isDaylightSavings(createZDT(2016, 7, 1, TZoneOffset.UTC).toInstant()), true);
        assertEquals(test.getDaylightSavings(createZDT(2016, 7, 1, TZoneOffset.UTC).toInstant()), TDuration.ofHours(1));

        // TZDB data is messed up, comment out tests until better fix available
        TDateTimeFormatter formatter1 = new TDateTimeFormatterBuilder().appendZoneText(TTextStyle.FULL).toFormatter();
        assertEquals(formatter1.format(createZDT(2016, 1, 1, TZoneId.of("Europe/Dublin"))), "Greenwich Mean Time");
        assertEquals(formatter1.format(createZDT(2016, 7, 1, TZoneId.of("Europe/Dublin"))).startsWith("Irish S"), true);

        TDateTimeFormatter formatter2 = new TDateTimeFormatterBuilder().appendZoneText(TTextStyle.SHORT).toFormatter();
        assertEquals(formatter2.format(createZDT(2016, 1, 1, TZoneId.of("Europe/Dublin"))), "GMT");
        assertEquals(formatter2.format(createZDT(2016, 7, 1, TZoneId.of("Europe/Dublin"))), "IST");
    }

    //-----------------------------------------------------------------------
    // Europe/Paris
    //-----------------------------------------------------------------------
    private TZoneRules europeParis() {
        return TZoneId.of("Europe/Paris").getRules();
    }

    public void test_Paris() {
        TZoneRules test = europeParis();
        assertEquals(test.isFixedOffset(), false);
    }

    public void test_Paris_preTimeZones() {
        TZoneRules test = europeParis();
        TZonedDateTime old = createZDT(1800, 1, 1, TZoneOffset.UTC);
        TInstant instant = old.toInstant();
        TZoneOffset offset = TZoneOffset.ofHoursMinutesSeconds(0, 9, 21);
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), TDuration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    public void test_Paris_getOffset() {
        TZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 1, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 2, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 4, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 5, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 6, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 7, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 8, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 9, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 1, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 12, 1, TZoneOffset.UTC)), OFFSET_PONE);
    }

    public void test_Paris_getOffset_toDST() {
        TZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 3, 24, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 25, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 26, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 27, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 28, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 29, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 31, TZoneOffset.UTC)), OFFSET_PTWO);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_PTWO);
    }

    public void test_Paris_getOffset_fromDST() {
        TZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 10, 24, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 25, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 27, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 28, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 29, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 30, TZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 31, TZoneOffset.UTC)), OFFSET_PONE);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, TZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC)), OFFSET_PONE);
    }

    public void test_Paris_getOffsetInfo() {
        TZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 1, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 2, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 4, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 5, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 6, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 7, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 8, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 9, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 11, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 12, 1), OFFSET_PONE, 1);
    }

    public void test_Paris_getOffsetInfo_toDST() {
        TZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 3, 24), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 25), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 26), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 27), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 28), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 29), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 30), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 31), OFFSET_PTWO, 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 1, 59, 59, 999999999), OFFSET_PONE, 1);
        checkOffset(test, TLocalDateTime.of(2008, 3, 30, 3, 0, 0, 0), OFFSET_PTWO, 1);
    }

    public void test_Paris_getOffsetInfo_fromDST() {
        TZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 10, 24), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 25), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 26), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 27), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 28), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 29), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 30), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 31), OFFSET_PONE, 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 1, 59, 59, 999999999), OFFSET_PTWO, 1);
        checkOffset(test, TLocalDateTime.of(2008, 10, 26, 3, 0, 0, 0), OFFSET_PONE, 1);
    }

    public void test_Paris_getOffsetInfo_gap() {
        TZoneRules test = europeParis();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 30, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PONE, GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), OFFSET_PONE);
        assertEquals(trans.getOffsetAfter(), OFFSET_PTWO);
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), false);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T02:00+01:00 to +02:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PONE));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_Paris_getOffsetInfo_overlap() {
        TZoneRules test = europeParis();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 10, 26, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PTWO, OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), OFFSET_PTWO);
        assertEquals(trans.getOffsetAfter(), OFFSET_PONE);
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, TZoneOffset.UTC));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T03:00+02:00 to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PTWO));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_Paris_getStandardOffset() {
        TZoneRules test = europeParis();
        TZonedDateTime zdt = createZDT(1840, 1, 1, TZoneOffset.UTC);
        while (zdt.getYear() < 2010) {
            TInstant instant = zdt.toInstant();
            if (zdt.toLocalDate().isBefore(TLocalDate.of(1911, 3, 11))) {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.ofHoursMinutesSeconds(0, 9, 21));
            } else if (zdt.toLocalDate().isBefore(TLocalDate.of(1940, 6, 14))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            } else if (zdt.toLocalDate().isBefore(TLocalDate.of(1944, 8, 25))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            } else if (zdt.toLocalDate().isBefore(TLocalDate.of(1945, 9, 16))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            } else {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            }
            zdt = zdt.plusMonths(6);
        }
    }

    //-----------------------------------------------------------------------
    // America/New_York
    //-----------------------------------------------------------------------
    private TZoneRules americaNewYork() {
        return TZoneId.of("America/New_York").getRules();
    }

    public void test_NewYork() {
        TZoneRules test = americaNewYork();
        assertEquals(test.isFixedOffset(), false);
    }

    public void test_NewYork_preTimeZones() {
        TZoneRules test = americaNewYork();
        TZonedDateTime old = createZDT(1800, 1, 1, TZoneOffset.UTC);
        TInstant instant = old.toInstant();
        TZoneOffset offset = TZoneOffset.of("-04:56:02");
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), TDuration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    public void test_NewYork_getOffset() {
        TZoneRules test = americaNewYork();
        TZoneOffset offset = TZoneOffset.ofHours(-5);
        assertEquals(test.getOffset(createInstant(2008, 1, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 2, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 4, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 5, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 6, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 7, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 8, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 9, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 10, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 12, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 1, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 2, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 4, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 5, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 6, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 7, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 8, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 9, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 10, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 12, 28, offset)), TZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffset_toDST() {
        TZoneRules test = americaNewYork();
        TZoneOffset offset = TZoneOffset.ofHours(-5);
        assertEquals(test.getOffset(createInstant(2008, 3, 8, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 9, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 10, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 11, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 12, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 13, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 14, offset)), TZoneOffset.ofHours(-4));
        // cutover at 02:00 local
        assertEquals(test.getOffset(createInstant(2008, 3, 9, 1, 59, 59, 999999999, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 9, 2, 0, 0, 0, offset)), TZoneOffset.ofHours(-4));
    }

    public void test_NewYork_getOffset_fromDST() {
        TZoneRules test = americaNewYork();
        TZoneOffset offset = TZoneOffset.ofHours(-4);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 2, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 3, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 4, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 5, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 6, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 7, offset)), TZoneOffset.ofHours(-5));
        // cutover at 02:00 local
        assertEquals(test.getOffset(createInstant(2008, 11, 2, 1, 59, 59, 999999999, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 2, 2, 0, 0, 0, offset)), TZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffsetInfo() {
        TZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 1, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 2, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 4, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 5, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 6, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 7, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 8, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 9, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 10, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 12, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 1, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 2, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 4, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 5, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 6, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 7, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 8, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 9, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 10, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 12, 28), TZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_toDST() {
        TZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 3, 8), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 9), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 10), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 11), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 12), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 13), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 14), TZoneOffset.ofHours(-4), 1);
        // cutover at 02:00 local
        checkOffset(test, TLocalDateTime.of(2008, 3, 9, 1, 59, 59, 999999999), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, TLocalDateTime.of(2008, 3, 9, 3, 0, 0, 0), TZoneOffset.ofHours(-4), 1);
    }

    public void test_NewYork_getOffsetInfo_fromDST() {
        TZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 11, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 2), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 3), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 4), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 5), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 6), TZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 7), TZoneOffset.ofHours(-5), 1);
        // cutover at 02:00 local
        checkOffset(test, TLocalDateTime.of(2008, 11, 2, 0, 59, 59, 999999999), TZoneOffset.ofHours(-4), 1);
        checkOffset(test, TLocalDateTime.of(2008, 11, 2, 2, 0, 0, 0), TZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_gap() {
        TZoneRules test = americaNewYork();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 9, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, TZoneOffset.ofHours(-5), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(-5));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(-4));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 9, 2, 0, TZoneOffset.ofHours(-5)));
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-5)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-4)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-09T02:00-05:00 to -04:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(-5)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_NewYork_getOffsetInfo_overlap() {
        TZoneRules test = americaNewYork();
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 11, 2, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test, dateTime, TZoneOffset.ofHours(-4), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(-4));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(-5));
        assertEquals(trans.getInstant(), createInstant(2008, 11, 2, 2, 0, TZoneOffset.ofHours(-4)));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-5)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-4)), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-11-02T02:00-04:00 to -05:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(-4)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_NewYork_getStandardOffset() {
        TZoneRules test = americaNewYork();
        TZonedDateTime dateTime = createZDT(1860, 1, 1, TZoneOffset.UTC);
        while (dateTime.getYear() < 2010) {
            TInstant instant = dateTime.toInstant();
            if (dateTime.toLocalDate().isBefore(TLocalDate.of(1883, 11, 18))) {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.of("-04:56:02"));
            } else {
                assertEquals(test.getStandardOffset(instant), TZoneOffset.ofHours(-5));
            }
            dateTime = dateTime.plusMonths(6);
        }
    }

    //-----------------------------------------------------------------------
    // Kathmandu
    //-----------------------------------------------------------------------
    private TZoneRules asiaKathmandu() {
        return TZoneId.of("Asia/Kathmandu").getRules();
    }

    public void test_Kathmandu_nextTransition_historic() {
        TZoneRules test = asiaKathmandu();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition first = trans.get(0);
        assertEquals(test.nextTransition(first.getInstant().minusNanos(1)), first);

        for (int i = 0; i < trans.size() - 1; i++) {
            TZoneOffsetTransition cur = trans.get(i);
            TZoneOffsetTransition next = trans.get(i + 1);

            assertEquals(test.nextTransition(cur.getInstant()), next);
            assertEquals(test.nextTransition(next.getInstant().minusNanos(1)), next);
        }
    }

    public void test_Kathmandu_nextTransition_noRules() {
        TZoneRules test = asiaKathmandu();
        List<TZoneOffsetTransition> trans = test.getTransitions();

        TZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.nextTransition(last.getInstant()), null);
    }

    //-------------------------------------------------------------------------
    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitions_immutable() {
        TZoneRules test = europeParis();
        test.getTransitions().clear();
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitionRules_immutable() {
        TZoneRules test = europeParis();
        test.getTransitionRules().clear();
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    public void test_equals() {
        TZoneRules test1 = europeLondon();
        TZoneRules test2 = europeParis();
        TZoneRules test2b = europeParis();
        assertEquals(test1.equals(test2), false);
        assertEquals(test2.equals(test1), false);

        assertEquals(test1.equals(test1), true);
        assertEquals(test2.equals(test2), true);
        assertEquals(test2.equals(test2b), true);

        assertEquals(test1.hashCode() == test1.hashCode(), true);
        assertEquals(test2.hashCode() == test2.hashCode(), true);
        assertEquals(test2.hashCode() == test2b.hashCode(), true);
    }

    public void test_equals_null() {
        assertEquals(europeLondon().equals(null), false);
    }

    public void test_equals_notZoneRules() {
        assertEquals(europeLondon().equals("Europe/London"), false);
    }

    public void test_toString() {
        assertEquals(europeLondon().toString().contains("TZoneRules"), true);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private TInstant createInstant(int year, int month, int day, TZoneOffset offset) {
        return TLocalDateTime.of(year, month, day, 0, 0).toInstant(offset);
    }

    private TInstant createInstant(int year, int month, int day, int hour, int min, TZoneOffset offset) {
        return TLocalDateTime.of(year, month, day, hour, min).toInstant(offset);
    }

    private TInstant createInstant(int year, int month, int day, int hour, int min, int sec, int nano, TZoneOffset offset) {
        return TLocalDateTime.of(year, month, day, hour, min, sec, nano).toInstant(offset);
    }

    private TZonedDateTime createZDT(int year, int month, int day, TZoneId zone) {
        return TLocalDateTime.of(year, month, day, 0, 0).atZone(zone);
    }

    private TLocalDateTime createLDT(int year, int month, int day) {
        return TLocalDateTime.of(year, month, day, 0, 0);
    }

    private TZoneOffsetTransition checkOffset(TZoneRules rules, TLocalDateTime dateTime, TZoneOffset offset, int type) {
        List<TZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        assertEquals(validOffsets.size(), type);
        assertEquals(rules.getOffset(dateTime), offset);
        if (type == 1) {
            assertEquals(validOffsets.get(0), offset);
            return null;
        } else {
            TZoneOffsetTransition zot = rules.getTransition(dateTime);
            assertNotNull(zot);
            assertEquals(zot.isOverlap(), type == 2);
            assertEquals(zot.isGap(), type == 0);
            assertEquals(zot.isValidOffset(offset), type == 2);
            return zot;
        }
    }

}
