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
package org.teavm.classlib.java.time;

import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZoneOffset.
 */
@TeaVMTest
public class TestZoneOffset extends AbstractDateTimeTest {

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { ZoneOffset.ofHours(1), ZoneOffset.ofHoursMinutesSeconds(-5, -6, -30) };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            OFFSET_SECONDS,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<>(Arrays.asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    // constants
    //-----------------------------------------------------------------------
    @Test
    public void test_constant_UTC() {
        ZoneOffset test = ZoneOffset.UTC;
        doTestOffset(test, 0, 0, 0);
    }

    @Test
    public void test_constant_MIN() {
        ZoneOffset test = ZoneOffset.MIN;
        doTestOffset(test, -18, 0, 0);
    }

    @Test
    public void test_constant_MAX() {
        ZoneOffset test = ZoneOffset.MAX;
        doTestOffset(test, 18, 0, 0);
    }

    //-----------------------------------------------------------------------
    // of(String)
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_string_UTC() {
        String[] values = new String[] {
            "Z", "+0",
            "+00", "+0000", "+00:00", "+000000", "+00:00:00",
            "-00", "-0000", "-00:00", "-000000", "-00:00:00",
        };
        for (int i = 0; i < values.length; i++) {
            ZoneOffset test = ZoneOffset.of(values[i]);
            assertSame(ZoneOffset.UTC, test);
        }
    }

    @Test
    public void test_factory_string_invalid() {
        String[] values = new String[] {
            "", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "ZZ",
            "0", "+0:00", "+00:0", "+0:0",
            "+000", "+00000",
            "+0:00:00", "+00:0:00", "+00:00:0", "+0:0:0", "+0:0:00", "+00:0:0", "+0:00:0",
            "1", "+01_00", "+01;00", "+01@00", "+01:AA",
            "+19", "+19:00", "+18:01", "+18:00:01", "+1801", "+180001",
            "-0:00", "-00:0", "-0:0",
            "-000", "-00000",
            "-0:00:00", "-00:0:00", "-00:00:0", "-0:0:0", "-0:0:00", "-00:0:0", "-0:00:0",
            "-19", "-19:00", "-18:01", "-18:00:01", "-1801", "-180001",
            "-01_00", "-01;00", "-01@00", "-01:AA",
            "@01:00",
        };
        for (int i = 0; i < values.length; i++) {
            try {
                ZoneOffset.of(values[i]);
                fail("Should have failed:" + values[i]);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test
    public void test_factory_string_null() {
        assertThrows(NullPointerException.class, () -> ZoneOffset.of((String) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_string_singleDigitHours() {
        for (int i = -9; i <= 9; i++) {
            String str = (i < 0 ? "-" : "+") + Math.abs(i);
            ZoneOffset test = ZoneOffset.of(str);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test
    public void test_factory_string_hours() {
        for (int i = -18; i <= 18; i++) {
            String str = (i < 0 ? "-" : "+") + Integer.toString(Math.abs(i) + 100).substring(1);
            ZoneOffset test = ZoneOffset.of(str);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test
    public void test_factory_string_hours_minutes_noColon() {
        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    String str = (i < 0 || j < 0 ? "-" : "+")
                            + Integer.toString(Math.abs(i) + 100).substring(1)
                            + Integer.toString(Math.abs(j) + 100).substring(1);
                    ZoneOffset test = ZoneOffset.of(str);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.of("-1800");
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.of("+1800");
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_string_hours_minutes_colon() {
        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    String str = (i < 0 || j < 0 ? "-" : "+")
                            + Integer.toString(Math.abs(i) + 100).substring(1) + ":"
                            + Integer.toString(Math.abs(j) + 100).substring(1);
                    ZoneOffset test = ZoneOffset.of(str);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.of("-18:00");
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.of("+18:00");
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
                        ZoneOffset test = ZoneOffset.of(str);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.of("-180000");
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.of("+180000");
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
                        ZoneOffset test = ZoneOffset.of(str);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.of("-18:00:00");
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.of("+18:00:00");
        doTestOffset(test2, 18, 0, 0);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_hours() {
        for (int i = -18; i <= 18; i++) {
            ZoneOffset test = ZoneOffset.ofHours(i);
            doTestOffset(test, i, 0, 0);
        }
    }

    @Test
    public void test_factory_int_hours_tooBig() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHours(19));
    }

    @Test
    public void test_factory_int_hours_tooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHours(-19));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_hours_minutes() {
        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                if ((i < 0 && j <= 0) || (i > 0 && j >= 0) || i == 0) {
                    ZoneOffset test = ZoneOffset.ofHoursMinutes(i, j);
                    doTestOffset(test, i, j, 0);
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.ofHoursMinutes(-18, 0);
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.ofHoursMinutes(18, 0);
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_int_hours_minutes_tooBig() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutes(19, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_tooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutes(-19, 0));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_hours_minutes_seconds() {
        for (int i = -17; i <= 17; i++) {
            for (int j = -59; j <= 59; j++) {
                for (int k = -59; k <= 59; k++) {
                    if ((i < 0 && j <= 0 && k <= 0) || (i > 0 && j >= 0 && k >= 0)
                            || (i == 0 && ((j < 0 && k <= 0) || (j > 0 && k >= 0) || j == 0))) {
                        ZoneOffset test = ZoneOffset.ofHoursMinutesSeconds(i, j, k);
                        doTestOffset(test, i, j, k);
                    }
                }
            }
        }
        ZoneOffset test1 = ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0);
        doTestOffset(test1, -18, 0, 0);
        ZoneOffset test2 = ZoneOffset.ofHoursMinutesSeconds(18, 0, 0);
        doTestOffset(test2, 18, 0, 0);
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_plusHoursMinusMinutes() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(1, -1, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_plusHoursMinusSeconds() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(1, 0, -1));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_minusHoursPlusMinutes() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(-1, 1, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_minusHoursPlusSeconds() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(-1, 0, 1));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_zeroHoursMinusMinutesPlusSeconds() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, -1, 1));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_zeroHoursPlusMinutesMinusSeconds() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, 1, -1));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_minutesTooLarge() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, 60, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_minutesTooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, -60, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_secondsTooLarge() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, 0, 60));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_secondsTooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(0, 0, 60));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_hoursTooBig() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(19, 0, 0));
    }

    @Test
    public void test_factory_int_hours_minutes_seconds_hoursTooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofHoursMinutesSeconds(-19, 0, 0));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_ofTotalSeconds() {
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(1, 0, 1), ZoneOffset.ofTotalSeconds(60 * 60 + 1));
        assertEquals(ZoneOffset.ofHours(18), ZoneOffset.ofTotalSeconds(18 * 60 * 60));
        assertEquals(ZoneOffset.ofHours(-18), ZoneOffset.ofTotalSeconds(-18 * 60 * 60));
    }

    @Test
    public void test_factory_ofTotalSeconds_tooLarge() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofTotalSeconds(18 * 60 * 60 + 1));
    }

    @Test
    public void test_factory_ofTotalSeconds_tooSmall() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.ofTotalSeconds(-18 * 60 * 60 - 1));
    }

    //-----------------------------------------------------------------------
    // from(TemporalAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_TemporalAccessor() {
        assertEquals(ZoneOffset.ofHours(6),
                ZoneOffset.from(OffsetTime.of(LocalTime.of(12, 30), ZoneOffset.ofHours(6))));
        assertEquals(ZoneOffset.ofHours(2),
                ZoneOffset.from(ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2007, 7, 15),
                LocalTime.of(17, 30)), ZoneOffset.ofHours(2))));
    }

    @Test
    public void test_factory_TemporalAccessor_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> ZoneOffset.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_TemporalAccessor_null() {
        assertThrows(NullPointerException.class, () -> ZoneOffset.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // getTotalSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_getTotalSeconds() {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(60 * 60 + 1);
        assertEquals(60 * 60 + 1, offset.getTotalSeconds());
    }

    //-----------------------------------------------------------------------
    // getId()
    //-----------------------------------------------------------------------
    @Test
    public void test_getId() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 0);
        assertEquals("+01:00", offset.getId());
        offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertEquals("+01:02:03", offset.getId());
        offset = ZoneOffset.UTC;
        assertEquals("Z", offset.getId());
    }

    //-----------------------------------------------------------------------
    // getRules()
    //-----------------------------------------------------------------------
    @Test
    public void test_getRules() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertTrue(offset.getRules().isFixedOffset());
        assertEquals(offset, offset.getRules().getOffset((Instant) null));
        assertEquals(Duration.ZERO, offset.getRules().getDaylightSavings((Instant) null));
        assertEquals(offset, offset.getRules().getStandardOffset((Instant) null));
        assertEquals(null, offset.getRules().nextTransition((Instant) null));
        assertEquals(null, offset.getRules().previousTransition((Instant) null));

        assertTrue(offset.getRules().isValidOffset((LocalDateTime) null, offset));
        assertFalse(offset.getRules().isValidOffset((LocalDateTime) null, ZoneOffset.UTC));
        assertFalse(offset.getRules().isValidOffset((LocalDateTime) null, null));
        assertEquals(offset, offset.getRules().getOffset((LocalDateTime) null));
        assertIterableEquals(Arrays.asList(offset), offset.getRules().getValidOffsets((LocalDateTime) null));
        assertEquals(null, offset.getRules().getTransition((LocalDateTime) null));
        assertEquals(0, offset.getRules().getTransitions().size());
        assertEquals(0, offset.getRules().getTransitionRules().size());
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(0, ZoneOffset.UTC.get(OFFSET_SECONDS));
        assertEquals(-7200, ZoneOffset.ofHours(-2).get(OFFSET_SECONDS));
        assertEquals(65, ZoneOffset.ofHoursMinutesSeconds(0, 1, 5).get(OFFSET_SECONDS));
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(0, ZoneOffset.UTC.getLong(OFFSET_SECONDS));
        assertEquals(-7200, ZoneOffset.ofHours(-2).getLong(OFFSET_SECONDS));
        assertEquals(65, ZoneOffset.ofHoursMinutesSeconds(0, 1, 5).getLong(OFFSET_SECONDS));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(null, ZoneOffset.UTC.query(TemporalQueries.chronology()));
        assertEquals(null, ZoneOffset.UTC.query(TemporalQueries.localDate()));
        assertEquals(null, ZoneOffset.UTC.query(TemporalQueries.localTime()));
        assertEquals(ZoneOffset.UTC, ZoneOffset.UTC.query(TemporalQueries.offset()));
        assertEquals(null, ZoneOffset.UTC.query(TemporalQueries.precision()));
        assertEquals(ZoneOffset.UTC, ZoneOffset.UTC.query(TemporalQueries.zone()));
        assertEquals(null, ZoneOffset.UTC.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> ZoneOffset.UTC.query(null));
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo() {
        ZoneOffset offset1 = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        ZoneOffset offset2 = ZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        assertTrue(offset1.compareTo(offset2) > 0);
        assertTrue(offset2.compareTo(offset1) < 0);
        assertTrue(offset1.compareTo(offset1) == 0);
        assertTrue(offset2.compareTo(offset2) == 0);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        ZoneOffset offset1 = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        ZoneOffset offset2 = ZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        ZoneOffset offset2b = ZoneOffset.ofHoursMinutesSeconds(2, 3, 4);
        assertFalse(offset1.equals(offset2));
        assertFalse(offset2.equals(offset1));

        assertTrue(offset1.equals(offset1));
        assertTrue(offset2.equals(offset2));
        assertTrue(offset2.equals(offset2b));

        assertTrue(offset1.hashCode() == offset1.hashCode());
        assertTrue(offset2.hashCode() == offset2.hashCode());
        assertTrue(offset2.hashCode() == offset2b.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 0);
        assertEquals("+01:00", offset.toString());
        offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        assertEquals("+01:02:03", offset.toString());
        offset = ZoneOffset.UTC;
        assertEquals("Z", offset.toString());
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private void doTestOffset(ZoneOffset offset, int hours, int minutes, int seconds) {
        assertEquals(hours * 60 * 60 + minutes * 60 + seconds, offset.getTotalSeconds());
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
        assertEquals(id, offset.getId());
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds), offset);
        if (seconds == 0) {
            assertEquals(ZoneOffset.ofHoursMinutes(hours, minutes), offset);
            if (minutes == 0) {
                assertEquals(ZoneOffset.ofHours(hours), offset);
            }
        }
        assertEquals(offset, ZoneOffset.of(id));
        assertEquals(id, offset.toString());
    }

}
