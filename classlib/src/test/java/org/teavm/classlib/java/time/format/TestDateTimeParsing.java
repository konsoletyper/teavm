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

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;

import org.teavm.classlib.java.util.TLocale;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;

@Test
public class TestDateTimeParsing {

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");
    private static final TZoneOffset OFFSET_0230 = TZoneOffset.ofHoursMinutes(2, 30);

    private static final TDateTimeFormatter LOCALFIELDS = new TDateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter();
    private static final TDateTimeFormatter LOCALFIELDS_ZONEID = new TDateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendZoneId().toFormatter();
    private static final TDateTimeFormatter LOCALFIELDS_OFFSETID = new TDateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendOffsetId().toFormatter();
    private static final TDateTimeFormatter LOCALFIELDS_WITH_PARIS = LOCALFIELDS.withZone(PARIS);
    private static final TDateTimeFormatter LOCALFIELDS_WITH_0230 = LOCALFIELDS.withZone(OFFSET_0230);
    private static final TDateTimeFormatter INSTANT = new TDateTimeFormatterBuilder()
        .appendInstant().toFormatter();
    private static final TDateTimeFormatter INSTANT_WITH_PARIS = INSTANT.withZone(PARIS);
    private static final TDateTimeFormatter INSTANT_WITH_0230 = INSTANT.withZone(OFFSET_0230);
    private static final TDateTimeFormatter INSTANT_OFFSETID = new TDateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendOffsetId().toFormatter();
    private static final TDateTimeFormatter INSTANT_OFFSETSECONDS = new TDateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();
    private static final TDateTimeFormatter INSTANTSECONDS = new TDateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).toFormatter();
    private static final TDateTimeFormatter INSTANTSECONDS_WITH_PARIS = INSTANTSECONDS.withZone(PARIS);
    private static final TDateTimeFormatter INSTANTSECONDS_NOS = new TDateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
    private static final TDateTimeFormatter INSTANTSECONDS_NOS_WITH_PARIS = INSTANTSECONDS_NOS.withZone(PARIS);
    private static final TDateTimeFormatter INSTANTSECONDS_OFFSETSECONDS = new TDateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();
    private static final TDateTimeFormatter INSTANT_OFFSETSECONDS_ZONE = new TDateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ')
        .appendValue(OFFSET_SECONDS).appendLiteral(' ')
        .appendZoneId().toFormatter();

    @DataProvider(name = "instantZones")
    Object[][] data_instantZones() {
        return new Object[][] {
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 Europe/Paris", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 +02:30", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {LOCALFIELDS_OFFSETID, "2014-06-30 01:02:03 +02:30", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {LOCALFIELDS_WITH_PARIS, "2014-06-30 01:02:03", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_WITH_0230, "2014-06-30 01:02:03", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {INSTANT_WITH_PARIS, "2014-06-30T01:02:03Z", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, TZoneOffset.UTC).withZoneSameInstant(PARIS)},
            {INSTANT_WITH_0230, "2014-06-30T01:02:03Z", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, TZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETID, "2014-06-30T01:02:03Z +02:30", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, TZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETSECONDS, "2014-06-30T01:02:03Z 9000", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, TZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANTSECONDS_WITH_PARIS, "86402", TInstant.ofEpochSecond(86402).atZone(PARIS)},
            {INSTANTSECONDS_NOS_WITH_PARIS, "86402.123456789", TInstant.ofEpochSecond(86402, 123456789).atZone(PARIS)},
            {INSTANTSECONDS_OFFSETSECONDS, "86402 9000", TInstant.ofEpochSecond(86402).atZone(OFFSET_0230)},
            {INSTANT_OFFSETSECONDS_ZONE, "2016-10-30T00:30:00Z 7200 Europe/Paris",
                TZonedDateTime.ofStrict(TLocalDateTime.of(2016, 10, 30, 2, 30), TZoneOffset.ofHours(2), PARIS)},
            {INSTANT_OFFSETSECONDS_ZONE, "2016-10-30T01:30:00Z 3600 Europe/Paris",
                TZonedDateTime.ofStrict(TLocalDateTime.of(2016, 10, 30, 2, 30), TZoneOffset.ofHours(1), PARIS)},
        };
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_ZDT(TDateTimeFormatter formatter, String text, TZonedDateTime expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(TZonedDateTime.from(actual), expected);
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_LDT(TDateTimeFormatter formatter, String text, TZonedDateTime expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(TLocalDateTime.from(actual), expected.toLocalDateTime());
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_Instant(TDateTimeFormatter formatter, String text, TZonedDateTime expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(TInstant.from(actual), expected.toInstant());
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_supported(TDateTimeFormatter formatter, String text, TZonedDateTime expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(actual.isSupported(INSTANT_SECONDS), true);
        assertEquals(actual.isSupported(EPOCH_DAY), true);
        assertEquals(actual.isSupported(SECOND_OF_DAY), true);
        assertEquals(actual.isSupported(NANO_OF_SECOND), true);
        assertEquals(actual.isSupported(MICRO_OF_SECOND), true);
        assertEquals(actual.isSupported(MILLI_OF_SECOND), true);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "instantNoZone")
    Object[][] data_instantNoZone() {
        return new Object[][] {
            {INSTANT, "2014-06-30T01:02:03Z", TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, TZoneOffset.UTC).toInstant()},
            {INSTANTSECONDS, "86402", TInstant.ofEpochSecond(86402)},
            {INSTANTSECONDS_NOS, "86402.123456789", TInstant.ofEpochSecond(86402, 123456789)},
        };
    }

    @Test(dataProvider = "instantNoZone", expectedExceptions = TDateTimeException.class)
    public void test_parse_instantNoZone_ZDT(TDateTimeFormatter formatter, String text, TInstant expected) {
        TTemporalAccessor actual = formatter.parse(text);
        TZonedDateTime.from(actual);
    }

    @Test(dataProvider = "instantNoZone", expectedExceptions = TDateTimeException.class)
    public void test_parse_instantNoZone_LDT(TDateTimeFormatter formatter, String text, TInstant expected) {
        TTemporalAccessor actual = formatter.parse(text);
        TLocalDateTime.from(actual);
    }

    @Test(dataProvider = "instantNoZone")
    public void test_parse_instantNoZone_Instant(TDateTimeFormatter formatter, String text, TInstant expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(TInstant.from(actual), expected);
    }

    @Test(dataProvider = "instantNoZone")
    public void test_parse_instantNoZone_supported(TDateTimeFormatter formatter, String text, TInstant expected) {
        TTemporalAccessor actual = formatter.parse(text);
        assertEquals(actual.isSupported(INSTANT_SECONDS), true);
        assertEquals(actual.isSupported(EPOCH_DAY), false);
        assertEquals(actual.isSupported(SECOND_OF_DAY), false);
        assertEquals(actual.isSupported(NANO_OF_SECOND), true);
        assertEquals(actual.isSupported(MICRO_OF_SECOND), true);
        assertEquals(actual.isSupported(MILLI_OF_SECOND), true);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_fromField_InstantSeconds() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(INSTANT_SECONDS).toFormatter();
        TTemporalAccessor acc = fmt.parse("86402");
        TInstant expected = TInstant.ofEpochSecond(86402);
        assertEquals(acc.isSupported(INSTANT_SECONDS), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(INSTANT_SECONDS), 86402L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 0L);
        assertEquals(TInstant.from(acc), expected);
    }

    @Test
    public void test_parse_fromField_InstantSeconds_NanoOfSecond() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(INSTANT_SECONDS).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TTemporalAccessor acc = fmt.parse("86402.123456789");
        TInstant expected = TInstant.ofEpochSecond(86402, 123456789);
        assertEquals(acc.isSupported(INSTANT_SECONDS), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(INSTANT_SECONDS), 86402L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 123456789L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 123456L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 123L);
        assertEquals(TInstant.from(acc), expected);
    }

    @Test
    public void test_parse_fromField_SecondOfDay() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(SECOND_OF_DAY).toFormatter();
        TTemporalAccessor acc = fmt.parse("864");
        assertEquals(acc.isSupported(SECOND_OF_DAY), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(SECOND_OF_DAY), 864L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 0L);
    }

    @Test
    public void test_parse_fromField_SecondOfDay_NanoOfSecond() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(SECOND_OF_DAY).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TTemporalAccessor acc = fmt.parse("864.123456789");
        assertEquals(acc.isSupported(SECOND_OF_DAY), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(SECOND_OF_DAY), 864L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 123456789L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 123456L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 123L);
    }

    @Test
    public void test_parse_fromField_SecondOfMinute() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(SECOND_OF_MINUTE).toFormatter();
        TTemporalAccessor acc = fmt.parse("32");
        assertEquals(acc.isSupported(SECOND_OF_MINUTE), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(SECOND_OF_MINUTE), 32L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 0L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 0L);
    }

    @Test
    public void test_parse_fromField_SecondOfMinute_NanoOfSecond() {
        TDateTimeFormatter fmt = new TDateTimeFormatterBuilder()
            .appendValue(SECOND_OF_MINUTE).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
        TTemporalAccessor acc = fmt.parse("32.123456789");
        assertEquals(acc.isSupported(SECOND_OF_MINUTE), true);
        assertEquals(acc.isSupported(NANO_OF_SECOND), true);
        assertEquals(acc.isSupported(MICRO_OF_SECOND), true);
        assertEquals(acc.isSupported(MILLI_OF_SECOND), true);
        assertEquals(acc.getLong(SECOND_OF_MINUTE), 32L);
        assertEquals(acc.getLong(NANO_OF_SECOND), 123456789L);
        assertEquals(acc.getLong(MICRO_OF_SECOND), 123456L);
        assertEquals(acc.getLong(MILLI_OF_SECOND), 123L);
    }

    @Test
    public void test_parse_tzdbGmtZone() {
        String dateString = "2015,7,21,0,0,0,GMT+02:00";
        TDateTimeFormatter formatter = TDateTimeFormatter.ofPattern("yyyy,M,d,H,m,s,z", TLocale.US);
        TZonedDateTime parsed = TZonedDateTime.parse(dateString, formatter);
        assertEquals(parsed, TZonedDateTime.of(2015, 7, 21, 0, 0, 0, 0, TZoneId.of("Etc/GMT-2")));
    }

}
