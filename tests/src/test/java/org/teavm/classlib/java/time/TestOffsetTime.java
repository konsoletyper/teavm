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

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test OffsetTime.
 */
@TeaVMTest
public class TestOffsetTime extends AbstractDateTimeTest {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final LocalDate DATE = LocalDate.of(2008, 12, 3);
    private OffsetTime test11x30x59x500pone;

    @BeforeEach
    public void setUp() {
        test11x30x59x500pone = OffsetTime.of(LocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { test11x30x59x500pone, OffsetTime.MIN, OffsetTime.MAX};
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            NANO_OF_SECOND,
            NANO_OF_DAY,
            MICRO_OF_SECOND,
            MICRO_OF_DAY,
            MILLI_OF_SECOND,
            MILLI_OF_DAY,
            SECOND_OF_MINUTE,
            SECOND_OF_DAY,
            MINUTE_OF_HOUR,
            MINUTE_OF_DAY,
            CLOCK_HOUR_OF_AMPM,
            HOUR_OF_AMPM,
            CLOCK_HOUR_OF_DAY,
            HOUR_OF_DAY,
            AMPM_OF_DAY,
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
    public void constant_MIN() {
        check(OffsetTime.MIN, 0, 0, 0, 0, ZoneOffset.MAX);
    }

    @Test
    public void constant_MAX() {
        check(OffsetTime.MAX, 23, 59, 59, 999999999, ZoneOffset.MIN);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        ZonedDateTime nowDT = ZonedDateTime.now();

        OffsetTime expected = OffsetTime.now(Clock.systemDefaultZone());
        OffsetTime test = OffsetTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        assertTrue(diff < 100000000);  // less than 0.1 secs
        assertEquals(nowDT.getOffset(), test.getOffset());
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(8, test.getNano());
            assertEquals(ZoneOffset.UTC, test.getOffset());
        }
    }

    @Test
    public void now_Clock_beforeEpoch() {
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals(((i + 24 * 60 * 60) / (60 * 60)) % 24, test.getHour());
            assertEquals(((i + 24 * 60 * 60) / 60) % 60, test.getMinute());
            assertEquals((i + 24 * 60 * 60) % 60, test.getSecond());
            assertEquals(8, test.getNano());
            assertEquals(ZoneOffset.UTC, test.getOffset());
        }
    }

    @Test
    public void now_Clock_offsets() {
        Instant base = LocalDateTime.of(1970, 1, 1, 12, 0).toInstant(ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base, offset);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals((12 + i) % 24, test.getHour());
            assertEquals(0, test.getMinute());
            assertEquals(0, test.getSecond());
            assertEquals(0, test.getNano());
            assertEquals(offset, test.getOffset());
        }
    }

    @Test
    public void now_Clock_nullZoneId() {
        assertThrows(NullPointerException.class, () -> OffsetTime.now((ZoneId) null));
    }

    @Test
    public void now_Clock_nullClock() {
        assertThrows(NullPointerException.class, () -> OffsetTime.now((Clock) null));
    }

    //-----------------------------------------------------------------------
    // factories
    //-----------------------------------------------------------------------
    private void check(OffsetTime test, int h, int m, int s, int n, ZoneOffset offset) {
        assertEquals(LocalTime.of(h, m, s, n), test.toLocalTime());
        assertEquals(offset, test.getOffset());

        assertEquals(h, test.getHour());
        assertEquals(m, test.getMinute());
        assertEquals(s, test.getSecond());
        assertEquals(n, test.getNano());

        assertEquals(test, test);
        assertEquals(test.hashCode(), test.hashCode());
        assertEquals(test, OffsetTime.of(LocalTime.of(h, m, s, n), offset));
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHM() {
        OffsetTime test = OffsetTime.of(LocalTime.of(11, 30), OFFSET_PONE);
        check(test, 11, 30, 0, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHMS() {
        OffsetTime test = OffsetTime.of(LocalTime.of(11, 30, 10), OFFSET_PONE);
        check(test, 11, 30, 10, 0, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHMSN() {
        OffsetTime test = OffsetTime.of(LocalTime.of(11, 30, 10, 500), OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_LocalTimeZoneOffset() {
        LocalTime localTime = LocalTime.of(11, 30, 10, 500);
        OffsetTime test = OffsetTime.of(localTime, OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test
    public void factory_LocalTimeZoneOffset_nullTime() {
        assertThrows(NullPointerException.class, () -> OffsetTime.of((LocalTime) null, OFFSET_PONE));
    }

    @Test
    public void factory_LocalTimeZoneOffset_nullOffset() {
        LocalTime localTime = LocalTime.of(11, 30, 10, 500);
        assertThrows(NullPointerException.class, () -> OffsetTime.of(localTime, (ZoneOffset) null));
    }

    //-----------------------------------------------------------------------
    // ofInstant()
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_nullInstant() {
        assertThrows(NullPointerException.class, () -> OffsetTime.ofInstant((Instant) null, ZoneOffset.UTC));
    }

    @Test
    public void factory_ofInstant_nullOffset() {
        Instant instant = Instant.ofEpochSecond(0L);
        assertThrows(NullPointerException.class, () -> OffsetTime.ofInstant(instant, (ZoneOffset) null));
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            OffsetTime test = OffsetTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals((i / (60 * 60)) % 24, test.getHour());
            assertEquals((i / 60) % 60, test.getMinute());
            assertEquals(i % 60, test.getSecond());
            assertEquals(8, test.getNano());
        }
    }

    @Test
    public void factory_ofInstant_beforeEpoch() {
        for (int i = -1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            OffsetTime test = OffsetTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(((i + 24 * 60 * 60) / (60 * 60)) % 24, test.getHour());
            assertEquals(((i + 24 * 60 * 60) / 60) % 60, test.getMinute());
            assertEquals((i + 24 * 60 * 60) % 60, test.getSecond());
            assertEquals(8, test.getNano());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_maxYear() {
        OffsetTime test = OffsetTime.ofInstant(Instant.MAX, ZoneOffset.UTC);
        assertEquals(23, test.getHour());
        assertEquals(59, test.getMinute());
        assertEquals(59, test.getSecond());
        assertEquals(999999999, test.getNano());
    }

    @Test
    public void factory_ofInstant_minYear() {
        OffsetTime test = OffsetTime.ofInstant(Instant.MIN, ZoneOffset.UTC);
        assertEquals(0, test.getHour());
        assertEquals(0, test.getMinute());
        assertEquals(0, test.getSecond());
        assertEquals(0, test.getNano());
    }

    //-----------------------------------------------------------------------
    // from(TemporalAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_TemporalAccessor_OT() {
        assertEquals(OffsetTime.of(LocalTime.of(17, 30), OFFSET_PONE),
                OffsetTime.from(OffsetTime.of(LocalTime.of(17, 30), OFFSET_PONE)));
    }

    @Test
    public void test_from_TemporalAccessor_ZDT() {
        ZonedDateTime base = LocalDateTime.of(2007, 7, 15, 11, 30, 59, 500).atZone(OFFSET_PONE);
        assertEquals(test11x30x59x500pone, OffsetTime.from(base));
    }

    @Test
    public void factory_from_TemporalAccessor_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> OffsetTime.from(LocalDate.of(2007, 7, 15)));
    }

    @Test
    public void factory_from_TemporalAccessor_null() {
        assertThrows(NullPointerException.class, () -> OffsetTime.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void factory_parse_validText(int h, int m, int s, int n, String offsetId, String parsable) {
        OffsetTime t = OffsetTime.parse(parsable);
        assertNotNull(t, parsable);
        check(t, h, m, s, n, ZoneOffset.of(offsetId));
    }

    static Object[][] provider_sampleBadParse() {
        return new Object[][]{
                {"00;00"},
                {"12-00"},
                {"-01:00"},
                {"00:00:00-09"},
                {"00:00:00,09"},
                {"00:00:abs"},
                {"11"},
                {"11:30"},
                {"11:30+01:00[Europe/Paris]"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleBadParse")
    public void factory_parse_invalidText(String unparsable) {
        assertThrows(DateTimeParseException.class, () -> OffsetTime.parse(unparsable));
    }

    //-----------------------------------------------------------------------s
    @Test
    public void factory_parse_illegalHour() {
        assertThrows(DateTimeParseException.class, () -> OffsetTime.parse("25:00+01:00"));
    }

    @Test
    public void factory_parse_illegalMinute() {
        assertThrows(DateTimeParseException.class, () -> OffsetTime.parse("12:60+01:00"));
    }

    @Test
    public void factory_parse_illegalSecond() {
        assertThrows(DateTimeParseException.class, () -> OffsetTime.parse("12:12:60+01:00"));
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s XXX");
        OffsetTime test = OffsetTime.parse("11 30 0 +01:00", f);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30), ZoneOffset.ofHours(1)), test);
    }

    @Test
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        assertThrows(NullPointerException.class, () -> OffsetTime.parse((String) null, f));
    }

    @Test
    public void factory_parse_formatter_nullFormatter() {
        assertThrows(NullPointerException.class, () -> OffsetTime.parse("ANY", null));
    }

    //-----------------------------------------------------------------------
    // constructor
    //-----------------------------------------------------------------------
    @Test
    @Disabled("Relies on reflective access to JDK internals")
    public void constructor_nullTime() throws Throwable  {
        assertThrows(NullPointerException.class, () -> {
            Constructor<OffsetTime> con = OffsetTime.class.getDeclaredConstructor(LocalTime.class, ZoneOffset.class);
            con.setAccessible(true);
            try {
                con.newInstance(null, OFFSET_PONE);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        });
    }

    @Test
    @Disabled("Relies on reflective access to JDK internals")
    public void constructor_nullOffset() throws Throwable  {
        assertThrows(NullPointerException.class, () -> {
            Constructor<OffsetTime> con = OffsetTime.class.getDeclaredConstructor(LocalTime.class, ZoneOffset.class);
            con.setAccessible(true);
            try {
                con.newInstance(LocalTime.of(11, 30), null);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        });
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleTimes() {
        return new Object[][] {
            {11, 30, 20, 500, OFFSET_PONE},
            {11, 0, 0, 0, OFFSET_PONE},
            {23, 59, 59, 999999999, OFFSET_PONE},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_get(int h, int m, int s, int n, ZoneOffset offset) {
        LocalTime localTime = LocalTime.of(h, m, s, n);
        OffsetTime a = OffsetTime.of(localTime, offset);

        assertEquals(localTime, a.toLocalTime());
        assertEquals(offset, a.getOffset());
        assertEquals(localTime.toString() + offset.toString(), a.toString());
        assertEquals(localTime.getHour(), a.getHour());
        assertEquals(localTime.getMinute(), a.getMinute());
        assertEquals(localTime.getSecond(), a.getSecond());
        assertEquals(localTime.getNano(), a.getNano());
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        OffsetTime test = OffsetTime.of(LocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
        assertEquals(12, test.get(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.get(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.get(ChronoField.NANO_OF_SECOND));
        assertEquals(0, test.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, test.get(ChronoField.AMPM_OF_DAY));

        assertEquals(3600, test.get(ChronoField.OFFSET_SECONDS));
    }

    @Test
    public void test_getLong_TemporalField() {
        OffsetTime test = OffsetTime.of(LocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
        assertEquals(12, test.getLong(ChronoField.HOUR_OF_DAY));
        assertEquals(30, test.getLong(ChronoField.MINUTE_OF_HOUR));
        assertEquals(40, test.getLong(ChronoField.SECOND_OF_MINUTE));
        assertEquals(987654321, test.getLong(ChronoField.NANO_OF_SECOND));
        assertEquals(0, test.getLong(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, test.getLong(ChronoField.AMPM_OF_DAY));

        assertEquals(3600, test.getLong(ChronoField.OFFSET_SECONDS));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(null, test11x30x59x500pone.query(TemporalQueries.chronology()));
        assertEquals(null, test11x30x59x500pone.query(TemporalQueries.localDate()));
        assertEquals(test11x30x59x500pone.toLocalTime(), test11x30x59x500pone.query(TemporalQueries.localTime()));
        assertEquals(test11x30x59x500pone.getOffset(), test11x30x59x500pone.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.NANOS, test11x30x59x500pone.query(TemporalQueries.precision()));
        assertEquals(test11x30x59x500pone.getOffset(), test11x30x59x500pone.query(TemporalQueries.zone()));
        assertEquals(null, test11x30x59x500pone.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.query(null));
    }

    //-----------------------------------------------------------------------
    // withOffsetSameLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameLocal() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withOffsetSameLocal(OFFSET_PTWO);
        assertEquals(base.toLocalTime(), test.toLocalTime());
        assertEquals(OFFSET_PTWO, test.getOffset());
    }

    @Test
    public void test_withOffsetSameLocal_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withOffsetSameLocal(OFFSET_PONE);
        assertEquals(base, test);
    }

    @Test
    public void test_withOffsetSameLocal_null() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> base.withOffsetSameLocal(null));
    }

    //-----------------------------------------------------------------------
    // withOffsetSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameInstant() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        OffsetTime expected = OffsetTime.of(LocalTime.of(12, 30, 59), OFFSET_PTWO);
        assertEquals(expected, test);
    }

    @Test
    public void test_withOffsetSameInstant_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withOffsetSameInstant(OFFSET_PONE);
        assertEquals(base, test);
    }

    @Test
    public void test_withOffsetSameInstant_null() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> base.withOffsetSameInstant(null));
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final OffsetTime sample = OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE);
        TemporalAdjuster adjuster = new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {
                return sample;
            }
        };
        assertEquals(sample, test11x30x59x500pone.with(adjuster));
    }

    @Test
    public void test_with_adjustment_LocalTime() {
        OffsetTime test = test11x30x59x500pone.with(LocalTime.of(13, 30));
        assertEquals(OffsetTime.of(LocalTime.of(13, 30), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_OffsetTime() {
        OffsetTime test = test11x30x59x500pone.with(OffsetTime.of(LocalTime.of(13, 35), OFFSET_PTWO));
        assertEquals(OffsetTime.of(LocalTime.of(13, 35), OFFSET_PTWO), test);
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {
        OffsetTime test = test11x30x59x500pone.with(OFFSET_PTWO);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30, 59, 500), OFFSET_PTWO), test);
    }

    @Test
    public void test_with_adjustment_AmPm() {
        OffsetTime test = test11x30x59x500pone.with(new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {
                return dateTime.with(HOUR_OF_DAY, 23);
            }
        });
        assertEquals(OffsetTime.of(LocalTime.of(23, 30, 59, 500), OFFSET_PONE), test);
    }

    @Test
    public void test_with_adjustment_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.with((TemporalAdjuster) null));
    }

    //-----------------------------------------------------------------------
    // with(TemporalField, long)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_TemporalField() {
        OffsetTime test = OffsetTime.of(LocalTime.of(12, 30, 40, 987654321), OFFSET_PONE);
        assertEquals(OffsetTime.of(LocalTime.of(15, 30, 40, 987654321), OFFSET_PONE),
                test.with(ChronoField.HOUR_OF_DAY, 15));
        assertEquals(OffsetTime.of(LocalTime.of(12, 50, 40, 987654321), OFFSET_PONE),
                test.with(ChronoField.MINUTE_OF_HOUR, 50));
        assertEquals(OffsetTime.of(LocalTime.of(12, 30, 50, 987654321), OFFSET_PONE),
                test.with(ChronoField.SECOND_OF_MINUTE, 50));
        assertEquals(OffsetTime.of(LocalTime.of(12, 30, 40, 12345), OFFSET_PONE),
                test.with(ChronoField.NANO_OF_SECOND, 12345));
        assertEquals(OffsetTime.of(LocalTime.of(18, 30, 40, 987654321), OFFSET_PONE),
                test.with(ChronoField.HOUR_OF_AMPM, 6));
        assertEquals(OffsetTime.of(LocalTime.of(0, 30, 40, 987654321), OFFSET_PONE),
                test.with(ChronoField.AMPM_OF_DAY, 0));

        assertEquals(OffsetTime.of(LocalTime.of(12, 30, 40, 987654321), ZoneOffset.ofHoursMinutesSeconds(2, 0, 5)),
                test.with(ChronoField.OFFSET_SECONDS, 7205));
    }

    @Test
    public void test_with_TemporalField_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.with((TemporalField) null, 0));
    }

    @Test
    public void test_with_TemporalField_invalidField() {
        assertThrows(DateTimeException.class, () -> test11x30x59x500pone.with(ChronoField.YEAR, 0));
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withHour(15);
        assertEquals(OffsetTime.of(LocalTime.of(15, 30, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_withHour_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withHour(11);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withMinute(15);
        assertEquals(OffsetTime.of(LocalTime.of(11, 15, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_withMinute_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withMinute(30);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withSecond(15);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30, 15), OFFSET_PONE), test);
    }

    @Test
    public void test_withSecond_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.withSecond(59);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        OffsetTime test = base.withNano(15);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30, 59, 15), OFFSET_PONE), test);
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        OffsetTime test = base.withNano(1);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(test11x30x59x500pone, test11x30x59x500pone.truncatedTo(NANOS));
        assertEquals(test11x30x59x500pone.withNano(0), test11x30x59x500pone.truncatedTo(SECONDS));
        assertEquals(test11x30x59x500pone.with(LocalTime.MIDNIGHT), test11x30x59x500pone.truncatedTo(DAYS));
    }

    @Test
    public void test_truncatedTo_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.truncatedTo(null));
    }

    //-----------------------------------------------------------------------
    // plus(PlusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_PlusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MINUTES);
        OffsetTime t = test11x30x59x500pone.plus(period);
        assertEquals(OffsetTime.of(LocalTime.of(11, 37, 59, 500), OFFSET_PONE), t);
    }

    @Test
    public void test_plus_PlusAdjuster_noChange() {
        OffsetTime t = test11x30x59x500pone.plus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(test11x30x59x500pone, t);
    }

    @Test
    public void test_plus_PlusAdjuster_zero() {
        OffsetTime t = test11x30x59x500pone.plus(Period.ZERO);
        assertEquals(test11x30x59x500pone, t);
    }

    @Test
    public void test_plus_PlusAdjuster_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.plus(null));
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusHours(13);
        assertEquals(OffsetTime.of(LocalTime.of(0, 30, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_plusHours_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusHours(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusMinutes(30);
        assertEquals(OffsetTime.of(LocalTime.of(12, 0, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_plusMinutes_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusMinutes(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusSeconds(1);
        assertEquals(OffsetTime.of(LocalTime.of(11, 31, 0), OFFSET_PONE), test);
    }

    @Test
    public void test_plusSeconds_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusSeconds(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetTime test = base.plusNanos(1);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30, 59, 1), OFFSET_PONE), test);
    }

    @Test
    public void test_plusNanos_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.plusNanos(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minus(MinusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_MinusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MINUTES);
        OffsetTime t = test11x30x59x500pone.minus(period);
        assertEquals(OffsetTime.of(LocalTime.of(11, 23, 59, 500), OFFSET_PONE), t);
    }

    @Test
    public void test_minus_MinusAdjuster_noChange() {
        OffsetTime t = test11x30x59x500pone.minus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(test11x30x59x500pone, t);
    }

    @Test
    public void test_minus_MinusAdjuster_zero() {
        OffsetTime t = test11x30x59x500pone.minus(Period.ZERO);
        assertEquals(test11x30x59x500pone, t);
    }

    @Test
    public void test_minus_MinusAdjuster_null() {
        assertThrows(NullPointerException.class, () -> test11x30x59x500pone.minus(null));
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusHours(-13);
        assertEquals(OffsetTime.of(LocalTime.of(0, 30, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_minusHours_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusHours(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusMinutes(50);
        assertEquals(OffsetTime.of(LocalTime.of(10, 40, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_minusMinutes_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusMinutes(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusSeconds(60);
        assertEquals(OffsetTime.of(LocalTime.of(11, 29, 59), OFFSET_PONE), test);
    }

    @Test
    public void test_minusSeconds_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusSeconds(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59, 0), OFFSET_PONE);
        OffsetTime test = base.minusNanos(1);
        assertEquals(OffsetTime.of(LocalTime.of(11, 30, 58, 999999999), OFFSET_PONE), test);
    }

    @Test
    public void test_minusNanos_zero() {
        OffsetTime base = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetTime test = base.minusNanos(0);
        assertEquals(base, test);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_time() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 29), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30), OFFSET_PONE);  // a is before b due to time
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(convertInstant(a).compareTo(convertInstant(b)) < 0);
    }

    @Test
    public void test_compareTo_offset() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30), OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30), OFFSET_PONE);  // a is before b due to offset
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(convertInstant(a).compareTo(convertInstant(b)) < 0);
    }

    @Test
    public void test_compareTo_both() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 50), OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 20), OFFSET_PONE);  // a is before b on instant scale
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(convertInstant(a).compareTo(convertInstant(b)) < 0);
    }

    @Test
    public void test_compareTo_bothNearStartOfDay() {
        OffsetTime a = OffsetTime.of(LocalTime.of(0, 10), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(2, 30), OFFSET_PTWO);  // a is before b on instant scale
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(convertInstant(a).compareTo(convertInstant(b)) < 0);
    }

    @Test
    public void test_compareTo_hourDifference() {
        OffsetTime a = OffsetTime.of(LocalTime.of(10, 0), OFFSET_PONE);
        // a is before b despite being same time-line time
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 0), OFFSET_PTWO);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(a) == 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(convertInstant(a).compareTo(convertInstant(b)) == 0);
    }

    @Test
    public void test_compareTo_null() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonOffsetTime() {
       Comparable c = test11x30x59x500pone;
       assertThrows(ClassCastException.class, () -> c.compareTo(new Object()));
    }

    private Instant convertInstant(OffsetTime ot) {
        return DATE.atTime(ot.toLocalTime()).toInstant(ot.getOffset());
    }

    //-----------------------------------------------------------------------
    // isAfter() / isBefore() / isEqual()
    //-----------------------------------------------------------------------
    @Test
    public void test_isBeforeIsAfterIsEqual1() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 58), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);  // a is before b due to time
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual1nanos() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59, 3), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30, 59, 4), OFFSET_PONE);  // a is before b due to time
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30, 58), OFFSET_PONE);  // a is before b due to offset
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2nanos() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59, 4),
                ZoneOffset.ofTotalSeconds(OFFSET_PONE.getTotalSeconds() + 1));
        OffsetTime b = OffsetTime.of(LocalTime.of(11, 30, 59, 3), OFFSET_PONE);  // a is before b due to offset
        assertTrue(a.isBefore(b));
        assertFalse(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertFalse(b.isEqual(a));
        assertTrue(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBeforeIsAfterIsEqual_instantComparison() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(LocalTime.of(10, 30, 59), OFFSET_PONE);  // a is same instant as b
        assertFalse(a.isBefore(b));
        assertTrue(a.isEqual(b));
        assertFalse(a.isAfter(b));

        assertFalse(b.isBefore(a));
        assertTrue(b.isEqual(a));
        assertFalse(b.isAfter(a));

        assertFalse(a.isBefore(a));
        assertFalse(b.isBefore(b));

        assertTrue(a.isEqual(a));
        assertTrue(b.isEqual(b));

        assertFalse(a.isAfter(a));
        assertFalse(b.isAfter(b));
    }

    @Test
    public void test_isBefore_null() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isBefore(null));
    }

    @Test
    public void test_isAfter_null() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isAfter(null));
    }

    @Test
    public void test_isEqual_null() {
        OffsetTime a = OffsetTime.of(LocalTime.of(11, 30, 59), OFFSET_PONE);
        assertThrows(NullPointerException.class, () -> a.isEqual(null));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_true(int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        assertTrue(a.equals(b));
        assertTrue(a.hashCode() == b.hashCode());
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_hour_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        h = h == 23 ? 22 : h;
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h + 1, m, s, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_minute_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        m = m == 59 ? 58 : m;
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h, m + 1, s, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_second_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        s = s == 59 ? 58 : s;
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h, m, s + 1, n), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_nano_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        n = n == 999999999 ? 999999998 : n;
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h, m, s, n + 1), OFFSET_PONE);
        assertFalse(a.equals(b));
    }
    @ParameterizedTest
    @MethodSource("provider_sampleTimes")
    public void test_equals_false_offset_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetTime a = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PONE);
        OffsetTime b = OffsetTime.of(LocalTime.of(h, m, s, n), OFFSET_PTWO);
        assertFalse(a.equals(b));
    }

    @Test
    public void test_equals_itself_true() {
        assertTrue(test11x30x59x500pone.equals(test11x30x59x500pone));
    }

    @Test
    public void test_equals_string_false() {
        assertFalse(test11x30x59x500pone.equals("2007-07-15"));
    }

    @Test
    public void test_equals_null_false() {
        assertFalse(test11x30x59x500pone.equals(null));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] provider_sampleToString() {
        return new Object[][] {
            {11, 30, 59, 0, "Z", "11:30:59Z"},
            {11, 30, 59, 0, "+01:00", "11:30:59+01:00"},
            {11, 30, 59, 999000000, "Z", "11:30:59.999Z"},
            {11, 30, 59, 999000000, "+01:00", "11:30:59.999+01:00"},
            {11, 30, 59, 999000, "Z", "11:30:59.000999Z"},
            {11, 30, 59, 999000, "+01:00", "11:30:59.000999+01:00"},
            {11, 30, 59, 999, "Z", "11:30:59.000000999Z"},
            {11, 30, 59, 999, "+01:00", "11:30:59.000000999+01:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("provider_sampleToString")
    public void test_toString(int h, int m, int s, int n, String offsetId, String expected) {
        OffsetTime t = OffsetTime.of(LocalTime.of(h, m, s, n), ZoneOffset.of(offsetId));
        String str = t.toString();
        assertEquals(expected, str);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s");
        String t = OffsetTime.of(LocalTime.of(11, 30), OFFSET_PONE).format(f);
        assertEquals("11 30 0", t);
    }

    @Test
    public void test_format_formatter_null() {
        assertThrows(NullPointerException.class, () -> OffsetTime.of(LocalTime.of(11, 30), OFFSET_PONE).format(null));
    }

}
