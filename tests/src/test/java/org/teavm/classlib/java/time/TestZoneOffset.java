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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

public class TestZoneOffset extends AbstractDateTimeTest {

    @Override
    protected List<TTemporalAccessor> samples() {

        TTemporalAccessor[] array = { TZoneOffset.ofHours(1), TZoneOffset.ofHoursMinutesSeconds(-5, -6, -30) };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {

        TTemporalField[] array = { OFFSET_SECONDS, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {

        List<TTemporalField> list = new ArrayList<TTemporalField>(
                Arrays.<TTemporalField> asList(TChronoField.values()));
        list.removeAll(validFields());
        list.add(TJulianFields.JULIAN_DAY);
        list.add(TJulianFields.MODIFIED_JULIAN_DAY);
        list.add(TJulianFields.RATA_DIE);
        return list;
    }

    @Test
    public void test_constant_UTC() {

        TZoneOffset test = TZoneOffset.UTC;
        doTestOffset(test, 0, 0, 0);
    }

    @Test
    public void test_constant_MIN() {

        TZoneOffset test = TZoneOffset.MIN;
        doTestOffset(test, -18, 0, 0);
    }

    @Test
    public void test_constant_MAX() {

        TZoneOffset test = TZoneOffset.MAX;
        doTestOffset(test, 18, 0, 0);
    }

    @Test
    public void test_factory_string_UTC() {

        String[] values = new String[] { "Z", "+0", "+00", "+0000", "+00:00", "+000000", "+00:00:00", "-00", "-0000",
        "-00:00", "-000000", "-00:00:00", };
        for (int i = 0; i < values.length; i++) {
            TZoneOffset test = TZoneOffset.of(values[i]);
            assertSame(test, TZoneOffset.UTC);
        }
    }

    @Test
    public void test_factory_string_invalid() {

        String[] values = new String[] { "", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "ZZ", "0", "+0:00", "+00:0", "+0:0", "+000", "+00000",
        "+0:00:00", "+00:0:00", "+00:00:0", "+0:0:0", "+0:0:00", "+00:0:0", "+0:00:0", "1", "+01_00", "+01;00",
        "+01@00", "+01:AA", "+19", "+19:00", "+18:01", "+18:00:01", "+1801", "+180001", "-0:00", "-00:0", "-0:0",
        "-000", "-00000", "-0:00:00", "-00:0:00", "-00:00:0", "-0:0:0", "-0:0:00", "-00:0:0", "-0:00:0", "-19",
        "-19:00", "-18:01", "-18:00:01", "-1801", "-180001", "-01_00", "-01;00", "-01@00", "-01:AA", "@01:00", };
        for (int i = 0; i < values.length; i++) {
            try {
                TZoneOffset.of(values[i]);
                fail("Should have failed:" + values[i]);
            } catch (TDateTimeException ex) {
                // expected
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_string_null() {

        TZoneOffset.of((String) null);
    }

    @Test
    public void test_factory_string_singleDigitHours() {

        for (int i = -9; i <= 9; i++) {
            String str = (i < 0 ? "-" : "+") + Math.abs(i);
            TZoneOffset test = TZoneOffset.of(str);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test
    public void test_factory_string_hours() {

        for (int i = -18; i <= 18; i++) {
            String str = (i < 0 ? "-" : "+") + Integer.toString(Math.abs(i) + 100).substring(1);
            TZoneOffset test = TZoneOffset.of(str);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test
    public void test_factory_string_hours_minutes_noColon() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    String str = (i < 0 || j < 0 ? "-" : "+") + Integer.toString(Math.abs(i) + 100).substring(1)
                            + Integer.toString(Math.abs(j) + 100).substring(1);
                    TZoneOffset test = TZoneOffset.of(str);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.of("-1800");
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.of("+1800");
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_string_hours_minutes_colon() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    String str = (i < 0 || j < 0 ? "-" : "+") + Integer.toString(Math.abs(i) + 100).substring(1) + ":"
                            + Integer.toString(Math.abs(j) + 100).substring(1);
                    TZoneOffset test = TZoneOffset.of(str);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.of("-18:00");
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.of("+18:00");
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_string_hours_minutes_seconds_noColon() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                for (int k = -59; k <= 59; k++) {
                    if ((i < 0 && j <= 0 && k <= 0) || (i > 0 && j >= 0 && k >= 0)
                            || (i == 0 && ((j < 0 && k <= 0) || (j > 0 && k >= 0) || j == 0))) {
                        String str = (i < 0 || j < 0 || k < 0 ? "-" : "+")
                                + Integer.toString(Math.abs(i) + 100).substring(1)
                                + Integer.toString(Math.abs(j) + 100).substring(1)
                                + Integer.toString(Math.abs(k) + 100).substring(1);
                        TZoneOffset test = TZoneOffset.of(str);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.of("-180000");
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.of("+180000");
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_string_hours_minutes_seconds_colon() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                for (int k = -59; k <= 59; k++) {
                    if ((i < 0 && j <= 0 && k <= 0) || (i > 0 && j >= 0 && k >= 0)
                            || (i == 0 && ((j < 0 && k <= 0) || (j > 0 && k >= 0) || j == 0))) {
                        String str = (i < 0 || j < 0 || k < 0 ? "-" : "+")
                                + Integer.toString(Math.abs(i) + 100).substring(1) + ":"
                                + Integer.toString(Math.abs(j) + 100).substring(1) + ":"
                                + Integer.toString(Math.abs(k) + 100).substring(1);
                        TZoneOffset test = TZoneOffset.of(str);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.of("-18:00:00");
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.of("+18:00:00");
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_int_hours() {

        for (int i = -18; i <= 18; i++) {
            TZoneOffset test = TZoneOffset.ofHours(i);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_tooBig() {

        TZoneOffset.ofHours(19);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_tooSmall() {

        TZoneOffset.ofHours(-19);
    }

    @Test
    public void test_factory_int_hours_minutes() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    TZoneOffset test = TZoneOffset.ofHoursMinutes(i, j);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.ofHoursMinutes(-18, 0);
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.ofHoursMinutes(18, 0);
        doTestOffset(test2, 18, 0, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_tooBig() {

        TZoneOffset.ofHoursMinutes(19, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_tooSmall() {

        TZoneOffset.ofHoursMinutes(-19, 0);
    }

    @Test
    public void test_factory_int_hours_minutes_seconds() {

        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                for (int k = -59; k <= 59; k++) {
                    if ((i < 0 && j <= 0 && k <= 0) || (i > 0 && j >= 0 && k >= 0)
                            || (i == 0 && ((j < 0 && k <= 0) || (j > 0 && k >= 0) || j == 0))) {
                        TZoneOffset test = TZoneOffset.ofHoursMinutesSeconds(i, j, k);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        TZoneOffset test1 = TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0);
        doTestOffset(test1, -18, 0, 0);
        TZoneOffset test2 = TZoneOffset.ofHoursMinutesSeconds(18, 0, 0);
        doTestOffset(test2, 18, 0, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_plusHoursMinusMinutes() {

        TZoneOffset.ofHoursMinutesSeconds(1, -1, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_plusHoursMinusSeconds() {

        TZoneOffset.ofHoursMinutesSeconds(1, 0, -1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_minusHoursPlusMinutes() {

        TZoneOffset.ofHoursMinutesSeconds(-1, 1, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_minusHoursPlusSeconds() {

        TZoneOffset.ofHoursMinutesSeconds(-1, 0, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_zeroHoursMinusMinutesPlusSeconds() {

        TZoneOffset.ofHoursMinutesSeconds(0, -1, 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_zeroHoursPlusMinutesMinusSeconds() {

        TZoneOffset.ofHoursMinutesSeconds(0, 1, -1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_minutesTooLarge() {

        TZoneOffset.ofHoursMinutesSeconds(0, 60, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_minutesTooSmall() {

        TZoneOffset.ofHoursMinutesSeconds(0, -60, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_secondsTooLarge() {

        TZoneOffset.ofHoursMinutesSeconds(0, 0, 60);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_secondsTooSmall() {

        TZoneOffset.ofHoursMinutesSeconds(0, 0, 60);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_hoursTooBig() {

        TZoneOffset.ofHoursMinutesSeconds(19, 0, 0);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_int_hours_minutes_seconds_hoursTooSmall() {

        TZoneOffset.ofHoursMinutesSeconds(-19, 0, 0);
    }

    @Test
    public void test_factory_ofTotalSeconds() {

        assertEquals(TZoneOffset.ofTotalSeconds(60 * 60 + 1), TZoneOffset.ofHoursMinutesSeconds(1, 0, 1));
        assertEquals(TZoneOffset.ofTotalSeconds(18 * 60 * 60), TZoneOffset.ofHours(18));
        assertEquals(TZoneOffset.ofTotalSeconds(-18 * 60 * 60), TZoneOffset.ofHours(-18));
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ofTotalSeconds_tooLarge() {

        TZoneOffset.ofTotalSeconds(18 * 60 * 60 + 1);
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_ofTotalSeconds_tooSmall() {

        TZoneOffset.ofTotalSeconds(-18 * 60 * 60 - 1);
    }

    @Test
    public void test_factory_TemporalAccessor() {

        assertEquals(TZoneOffset.from(TOffsetTime.of(TLocalTime.of(12, 30), TZoneOffset.ofHours(6))),
                TZoneOffset.ofHours(6));
        assertEquals(TZoneOffset.from(TZonedDateTime
                .of(TLocalDateTime.of(TLocalDate.of(2007, 7, 15), TLocalTime.of(17, 30)), TZoneOffset.ofHours(2))),
                TZoneOffset.ofHours(2));
    }

    @Test(expected = TDateTimeException.class)
    public void test_factory_TemporalAccessor_invalid_noDerive() {

        TZoneOffset.from(TLocalTime.of(12, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_factory_TemporalAccessor_null() {

        TZoneOffset.from((TTemporalAccessor) null);
    }

    @Test
    public void test_getTotalSeconds() {

        TZoneOffset offset = TZoneOffset.ofTotalSeconds(60 * 60 + 1);
        assertEquals(offset.getTotalSeconds(), 60 * 60 + 1);
    }

    @Test
    public void test_getId() {

        TZoneOffset offset = TZoneOffset.ofHoursMinutesSeconds(1, 0, 0);
        assertEquals(offset.getId(), "+01:00");
        offset = TZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertEquals(offset.getId(), "+01:02:03");
        offset = TZoneOffset.UTC;
        assertEquals(offset.getId(), "Z");
    }

    @Test
    public void test_getRules() {

        TZoneOffset offset = TZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertEquals(offset.getRules().isFixedOffset(), true);
        assertEquals(offset.getRules().getOffset((TInstant) null), offset);
        assertEquals(offset.getRules().getDaylightSavings((TInstant) null), TDuration.ZERO);
        assertEquals(offset.getRules().getStandardOffset((TInstant) null), offset);
        assertEquals(offset.getRules().nextTransition((TInstant) null), null);
        assertEquals(offset.getRules().previousTransition((TInstant) null), null);

        assertEquals(offset.getRules().isValidOffset((TLocalDateTime) null, offset), true);
        assertEquals(offset.getRules().isValidOffset((TLocalDateTime) null, TZoneOffset.UTC), false);
        assertEquals(offset.getRules().isValidOffset((TLocalDateTime) null, null), false);
        assertEquals(offset.getRules().getOffset((TLocalDateTime) null), offset);
        assertEquals(offset.getRules().getValidOffsets((TLocalDateTime) null), Arrays.asList(offset));
        assertEquals(offset.getRules().getTransition((TLocalDateTime) null), null);
        assertEquals(offset.getRules().getTransitions().size(), 0);
        assertEquals(offset.getRules().getTransitionRules().size(), 0);
    }

    @Test
    public void test_get_TemporalField() {

        assertEquals(TZoneOffset.UTC.get(OFFSET_SECONDS), 0);
        assertEquals(TZoneOffset.ofHours(-2).get(OFFSET_SECONDS), -7200);
        assertEquals(TZoneOffset.ofHoursMinutesSeconds(0, 1, 5).get(OFFSET_SECONDS), 65);
    }

    @Test
    public void test_getLong_TemporalField() {

        assertEquals(TZoneOffset.UTC.getLong(OFFSET_SECONDS), 0);
        assertEquals(TZoneOffset.ofHours(-2).getLong(OFFSET_SECONDS), -7200);
        assertEquals(TZoneOffset.ofHoursMinutesSeconds(0, 1, 5).getLong(OFFSET_SECONDS), 65);
    }

    @Test
    public void test_query() {

        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.chronology()), null);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.localDate()), null);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.localTime()), null);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.offset()), TZoneOffset.UTC);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.precision()), null);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.zone()), TZoneOffset.UTC);
        assertEquals(TZoneOffset.UTC.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expected = NullPointerException.class)
    public void test_query_null() {

        TZoneOffset.UTC.query(null);
    }

    @Test
    public void test_compareTo() {

        TZoneOffset offset1 = TZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        TZoneOffset offset2 = TZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        assertTrue(offset1.compareTo(offset2) > 0);
        assertTrue(offset2.compareTo(offset1) < 0);
        assertTrue(offset1.compareTo(offset1) == 0);
        assertTrue(offset2.compareTo(offset2) == 0);
    }

    @Test
    public void test_equals() {

        TZoneOffset offset1 = TZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        TZoneOffset offset2 = TZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        TZoneOffset offset2b = TZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        assertEquals(offset1.equals(offset2), false);
        assertEquals(offset2.equals(offset1), false);

        assertEquals(offset1.equals(offset1), true);
        assertEquals(offset2.equals(offset2), true);
        assertEquals(offset2.equals(offset2b), true);

        assertEquals(offset1.hashCode() == offset1.hashCode(), true);
        assertEquals(offset2.hashCode() == offset2.hashCode(), true);
        assertEquals(offset2.hashCode() == offset2b.hashCode(), true);
    }

    @Test
    public void test_toString() {

        TZoneOffset offset = TZoneOffset.ofHoursMinutesSeconds(1, 0, 0);
        assertEquals(offset.toString(), "+01:00");
        offset = TZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertEquals(offset.toString(), "+01:02:03");
        offset = TZoneOffset.UTC;
        assertEquals(offset.toString(), "Z");
    }

    private void doTestOffset(TZoneOffset offset, int hours, int minutes, int seconds) {

        assertEquals(offset.getTotalSeconds(), hours * 60 * 60 + minutes * 60 + seconds);
        final String id;
        if (hours == 0 && minutes == 0 && seconds == 0) {
            id = "Z";
        } else {
            String str = (hours < 0 || minutes < 0 || seconds < 0) ? "-" : "+";
            str += Integer.toString(Math.abs(hours) + 100).substring(1);
            str += ":";
            str += Integer.toString(Math.abs(minutes) + 100).substring(1);
            if (seconds != 0) {
                str += ":";
                str += Integer.toString(Math.abs(seconds) + 100).substring(1);
            }
            id = str;
        }
        assertEquals(offset.getId(), id);
        assertEquals(offset, TZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds));
        if (seconds == 0) {
            assertEquals(offset, TZoneOffset.ofHoursMinutes(hours, minutes));
            if (minutes == 0) {
                assertEquals(offset, TZoneOffset.ofHours(hours));
            }
        }
        assertEquals(TZoneOffset.of(id), offset);
        assertEquals(offset.toString(), id);
    }

}
