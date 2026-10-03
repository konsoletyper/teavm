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
package org.teavm.classlib.java.time.format;

import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTest;

/**
 * Test parsing of edge cases.
 */
@TeaVMTest
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US, fr_FR"))
public class TestDateTimeParsing {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneOffset OFFSET_0230 = ZoneOffset.ofHoursMinutes(2, 30);

    private static final DateTimeFormatter LOCALFIELDS = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_ZONEID = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendZoneId().toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_OFFSETID = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendOffsetId().toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_WITH_PARIS = LOCALFIELDS.withZone(PARIS);
    private static final DateTimeFormatter LOCALFIELDS_WITH_0230 = LOCALFIELDS.withZone(OFFSET_0230);
    private static final DateTimeFormatter INSTANT = new DateTimeFormatterBuilder()
        .appendInstant().toFormatter();
    private static final DateTimeFormatter INSTANT_WITH_PARIS = INSTANT.withZone(PARIS);
    private static final DateTimeFormatter INSTANT_WITH_0230 = INSTANT.withZone(OFFSET_0230);
    private static final DateTimeFormatter INSTANT_OFFSETID = new DateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendOffsetId().toFormatter();
    private static final DateTimeFormatter INSTANT_OFFSETSECONDS = new DateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS_WITH_PARIS = INSTANTSECONDS.withZone(PARIS);
    private static final DateTimeFormatter INSTANTSECONDS_NOS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS_NOS_WITH_PARIS = INSTANTSECONDS_NOS.withZone(PARIS);
    private static final DateTimeFormatter INSTANTSECONDS_OFFSETSECONDS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();
    private static final DateTimeFormatter INSTANT_OFFSETSECONDS_ZONE = new DateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ')
        .appendValue(OFFSET_SECONDS).appendLiteral(' ')
        .appendZoneId().toFormatter();

    static Object[][] data_instantZones() {
        return new Object[][] {
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 Europe/Paris", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 +02:30", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {LOCALFIELDS_OFFSETID, "2014-06-30 01:02:03 +02:30", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0,
                    OFFSET_0230)},
            {LOCALFIELDS_WITH_PARIS, "2014-06-30 01:02:03", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_WITH_0230, "2014-06-30 01:02:03", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {INSTANT_WITH_PARIS, "2014-06-30T01:02:03Z",
                    ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(PARIS)},
            {INSTANT_WITH_0230, "2014-06-30T01:02:03Z",
                    ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETID, "2014-06-30T01:02:03Z +02:30",
                    ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETSECONDS, "2014-06-30T01:02:03Z 9000",
                    ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANTSECONDS_WITH_PARIS, "86402", Instant.ofEpochSecond(86402).atZone(PARIS)},
            {INSTANTSECONDS_NOS_WITH_PARIS, "86402.123456789", Instant.ofEpochSecond(86402, 123456789).atZone(PARIS)},
            {INSTANTSECONDS_OFFSETSECONDS, "86402 9000", Instant.ofEpochSecond(86402).atZone(OFFSET_0230)},
            {INSTANT_OFFSETSECONDS_ZONE, "2016-10-30T00:30:00Z 7200 Europe/Paris",
                ZonedDateTime.ofStrict(LocalDateTime.of(2016, 10, 30, 2, 30), ZoneOffset.ofHours(2), PARIS)},
            {INSTANT_OFFSETSECONDS_ZONE, "2016-10-30T01:30:00Z 3600 Europe/Paris",
                ZonedDateTime.ofStrict(LocalDateTime.of(2016, 10, 30, 2, 30), ZoneOffset.ofHours(1), PARIS)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_instantZones")
    public void test_parse_instantZones_ZDT(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(expected, ZonedDateTime.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantZones")
    public void test_parse_instantZones_LDT(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(expected.toLocalDateTime(), LocalDateTime.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantZones")
    public void test_parse_instantZones_Instant(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(expected.toInstant(), Instant.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantZones")
    public void test_parse_instantZones_supported(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertTrue(actual.isSupported(INSTANT_SECONDS));
        assertTrue(actual.isSupported(EPOCH_DAY));
        assertTrue(actual.isSupported(SECOND_OF_DAY));
        assertTrue(actual.isSupported(NANO_OF_SECOND));
        assertTrue(actual.isSupported(MICRO_OF_SECOND));
        assertTrue(actual.isSupported(MILLI_OF_SECOND));
    }

    //-----------------------------------------------------------------------
    static Object[][] data_instantNoZone() {
        return new Object[][] {
            {INSTANT, "2014-06-30T01:02:03Z", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).toInstant()},
            {INSTANTSECONDS, "86402", Instant.ofEpochSecond(86402)},
            {INSTANTSECONDS_NOS, "86402.123456789", Instant.ofEpochSecond(86402, 123456789)},
        };
    }

    @ParameterizedTest
    @MethodSource("data_instantNoZone")
    public void test_parse_instantNoZone_ZDT(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertThrows(DateTimeException.class, () -> ZonedDateTime.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantNoZone")
    public void test_parse_instantNoZone_LDT(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertThrows(DateTimeException.class, () -> LocalDateTime.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantNoZone")
    public void test_parse_instantNoZone_Instant(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(expected, Instant.from(actual));
    }

    @ParameterizedTest
    @MethodSource("data_instantNoZone")
    public void test_parse_instantNoZone_supported(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertTrue(actual.isSupported(INSTANT_SECONDS));
        assertFalse(actual.isSupported(EPOCH_DAY));
        assertFalse(actual.isSupported(SECOND_OF_DAY));
        assertTrue(actual.isSupported(NANO_OF_SECOND));
        assertTrue(actual.isSupported(MICRO_OF_SECOND));
        assertTrue(actual.isSupported(MILLI_OF_SECOND));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_fromField_InstantSeconds() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(INSTANT_SECONDS).toFormatter();
        TemporalAccessor acc = fmt.parse("86402");
        Instant expected = Instant.ofEpochSecond(86402);
        assertTrue(acc.isSupported(INSTANT_SECONDS));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(86402L, acc.getLong(INSTANT_SECONDS));
        assertEquals(0L, acc.getLong(NANO_OF_SECOND));
        assertEquals(0L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(0L, acc.getLong(MILLI_OF_SECOND));
        assertEquals(expected, Instant.from(acc));
    }

    @Test
    public void test_parse_fromField_InstantSeconds_NanoOfSecond() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(INSTANT_SECONDS).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TemporalAccessor acc = fmt.parse("86402.123456789");
        Instant expected = Instant.ofEpochSecond(86402, 123456789);
        assertTrue(acc.isSupported(INSTANT_SECONDS));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(86402L, acc.getLong(INSTANT_SECONDS));
        assertEquals(123456789L, acc.getLong(NANO_OF_SECOND));
        assertEquals(123456L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(123L, acc.getLong(MILLI_OF_SECOND));
        assertEquals(expected, Instant.from(acc));
    }

    @Test
    public void test_parse_fromField_SecondOfDay() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(SECOND_OF_DAY).toFormatter();
        TemporalAccessor acc = fmt.parse("864");
        assertTrue(acc.isSupported(SECOND_OF_DAY));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(864L, acc.getLong(SECOND_OF_DAY));
        assertEquals(0L, acc.getLong(NANO_OF_SECOND));
        assertEquals(0L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(0L, acc.getLong(MILLI_OF_SECOND));
    }

    @Test
    public void test_parse_fromField_SecondOfDay_NanoOfSecond() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(SECOND_OF_DAY).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TemporalAccessor acc = fmt.parse("864.123456789");
        assertTrue(acc.isSupported(SECOND_OF_DAY));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(864L, acc.getLong(SECOND_OF_DAY));
        assertEquals(123456789L, acc.getLong(NANO_OF_SECOND));
        assertEquals(123456L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(123L, acc.getLong(MILLI_OF_SECOND));
    }

    @Test
    public void test_parse_fromField_SecondOfMinute() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(SECOND_OF_MINUTE).toFormatter();
        TemporalAccessor acc = fmt.parse("32");
        assertTrue(acc.isSupported(SECOND_OF_MINUTE));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(32L, acc.getLong(SECOND_OF_MINUTE));
        assertEquals(0L, acc.getLong(NANO_OF_SECOND));
        assertEquals(0L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(0L, acc.getLong(MILLI_OF_SECOND));
    }

    @Test
    public void test_parse_fromField_SecondOfMinute_NanoOfSecond() {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendValue(SECOND_OF_MINUTE).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TemporalAccessor acc = fmt.parse("32.123456789");
        assertTrue(acc.isSupported(SECOND_OF_MINUTE));
        assertTrue(acc.isSupported(NANO_OF_SECOND));
        assertTrue(acc.isSupported(MICRO_OF_SECOND));
        assertTrue(acc.isSupported(MILLI_OF_SECOND));
        assertEquals(32L, acc.getLong(SECOND_OF_MINUTE));
        assertEquals(123456789L, acc.getLong(NANO_OF_SECOND));
        assertEquals(123456L, acc.getLong(MICRO_OF_SECOND));
        assertEquals(123L, acc.getLong(MILLI_OF_SECOND));
    }

    @Test
    @Disabled
    // TODO: fix this and unignore
    public void test_parse_tzdbGmtZone() {
        String dateString = "2015,7,21,0,0,0,GMT+02:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy,M,d,H,m,s,z", Locale.US);
        ZonedDateTime parsed = ZonedDateTime.parse(dateString, formatter);
        assertEquals(ZonedDateTime.of(2015, 7, 21, 0, 0, 0, 0, ZoneId.of("Etc/GMT-2")), parsed);
    }

}
