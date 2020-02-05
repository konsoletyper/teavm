/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;

import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TYearMonth;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

public class TestDateTimeFormatter {

    private static final TDateTimeFormatter BASIC_FORMATTER = TDateTimeFormatter.ofPattern("'ONE'd");

    private static final TDateTimeFormatter DATE_FORMATTER = TDateTimeFormatter.ofPattern("'ONE'uuuu MM dd");

    private TDateTimeFormatter fmt;

    @Before
    public void setUp() {

        this.fmt = new TDateTimeFormatterBuilder().appendLiteral("ONE")
                .appendValue(DAY_OF_MONTH, 1, 2, TSignStyle.NOT_NEGATIVE).toFormatter();
    }

    @Test
    public void test_withLocale() {

        TDateTimeFormatter base = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        TDateTimeFormatter test = base.withLocale(Locale.GERMAN);
        assertEquals(test.getLocale(), Locale.GERMAN);
    }

    @Test(expected = NullPointerException.class)
    public void test_withLocale_null() {

        TDateTimeFormatter base = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        base.withLocale((Locale) null);
    }

    @Test
    public void test_print_Calendrical() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        String result = test.format(TLocalDate.of(2008, 6, 30));
        assertEquals(result, "ONE30");
    }

    @Test(expected = TDateTimeException.class)
    public void test_print_Calendrical_noSuchField() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.format(TLocalTime.of(11, 30));
    }

    @Test(expected = NullPointerException.class)
    public void test_print_Calendrical_null() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.format((TTemporalAccessor) null);
    }

    @Test
    public void test_print_CalendricalAppendable() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        test.formatTo(TLocalDate.of(2008, 6, 30), buf);
        assertEquals(buf.toString(), "ONE30");
    }

    @Test(expected = TDateTimeException.class)
    public void test_print_CalendricalAppendable_noSuchField() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        test.formatTo(TLocalTime.of(11, 30), buf);
    }

    @Test(expected = NullPointerException.class)
    public void test_print_CalendricalAppendable_nullCalendrical() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        test.formatTo((TTemporalAccessor) null, buf);
    }

    @Test(expected = NullPointerException.class)
    public void test_print_CalendricalAppendable_nullAppendable() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.formatTo(TLocalDate.of(2008, 6, 30), (Appendable) null);
    }

    @Test(expected = IOException.class) // IOException
    public void test_print_CalendricalAppendable_ioError() throws Throwable {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        try {
            test.formatTo(TLocalDate.of(2008, 6, 30), new MockIOExceptionAppendable());
        } catch (TDateTimeException ex) {
            assertEquals(ex.getCause() instanceof IOException, true);
            throw ex.getCause();
        }
    }

    @Test
    public void test_parse_Class_String() {

        TLocalDate result = DATE_FORMATTER.parse("ONE2012 07 27", TLocalDate.FROM);
        assertEquals(result, TLocalDate.of(2012, 7, 27));
    }

    @Test
    public void test_parse_Class_CharSequence() {

        TLocalDate result = DATE_FORMATTER.parse(new StringBuilder("ONE2012 07 27"), TLocalDate.FROM);
        assertEquals(result, TLocalDate.of(2012, 7, 27));
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parse_Class_String_parseError() {

        try {
            DATE_FORMATTER.parse("ONE2012 07 XX", TLocalDate.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(ex.getMessage().contains("ONE2012 07 XX"), true);
            assertEquals(ex.getParsedString(), "ONE2012 07 XX");
            assertEquals(ex.getErrorIndex(), 11);
            throw ex;
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parse_Class_String_parseErrorLongText() {

        try {
            DATE_FORMATTER.parse("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                    TLocalDate.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(
                    ex.getMessage().contains("ONEXXX6789012345678901234567890123456789012345678901234567890123..."),
                    true);
            assertEquals(ex.getParsedString(),
                    "ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789");
            assertEquals(ex.getErrorIndex(), 3);
            throw ex;
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parse_Class_String_parseIncomplete() {

        try {
            DATE_FORMATTER.parse("ONE2012 07 27SomethingElse", TLocalDate.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(ex.getMessage().contains("ONE2012 07 27SomethingElse"), true);
            assertEquals(ex.getParsedString(), "ONE2012 07 27SomethingElse");
            assertEquals(ex.getErrorIndex(), 13);
            throw ex;
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_parse_Class_String_nullText() {

        DATE_FORMATTER.parse((String) null, TLocalDate.FROM);
    }

    @Test(expected = NullPointerException.class)
    public void test_parse_Class_String_nullRule() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parse("30", (TTemporalQuery<?>) null);
    }

    @Test
    public void test_parseBest_firstOption() {

        TDateTimeFormatter test = TDateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        TTemporalAccessor result = test.parseBest("2011-06-30", TLocalDate.FROM, TYearMonth.FROM);
        assertEquals(result, TLocalDate.of(2011, 6, 30));
    }

    @Test
    public void test_parseBest_secondOption() {

        TDateTimeFormatter test = TDateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        TTemporalAccessor result = test.parseBest("2011-06", TLocalDate.FROM, TYearMonth.FROM);
        assertEquals(result, TYearMonth.of(2011, 6));
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parseBest_String_parseError() {

        TDateTimeFormatter test = TDateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        try {
            test.parseBest("2011-XX-30", TLocalDate.FROM, TYearMonth.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(ex.getMessage().contains("XX"), true);
            assertEquals(ex.getParsedString(), "2011-XX-30");
            assertEquals(ex.getErrorIndex(), 5);
            throw ex;
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parseBest_String_parseErrorLongText() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        try {
            test.parseBest("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                    TLocalDate.FROM, TYearMonth.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(
                    ex.getMessage().contains("ONEXXX6789012345678901234567890123456789012345678901234567890123..."),
                    true);
            assertEquals(ex.getParsedString(),
                    "ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789");
            assertEquals(ex.getErrorIndex(), 3);
            throw ex;
        }
    }

    @Test(expected = TDateTimeParseException.class)
    public void test_parseBest_String_parseIncomplete() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        try {
            test.parseBest("ONE30SomethingElse", TYearMonth.FROM, TLocalDate.FROM);
        } catch (TDateTimeParseException ex) {
            assertEquals(ex.getMessage().contains("could not be parsed"), true);
            assertEquals(ex.getMessage().contains("ONE30SomethingElse"), true);
            assertEquals(ex.getParsedString(), "ONE30SomethingElse");
            assertEquals(ex.getErrorIndex(), 5);
            throw ex;
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_parseBest_String_nullText() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parseBest((String) null, TYearMonth.FROM, TLocalDate.FROM);
    }

    @Test(expected = NullPointerException.class)
    public void test_parseBest_String_nullRules() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parseBest("30", (TTemporalQuery<?>[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_parseBest_String_zeroRules() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parseBest("30", new TTemporalQuery<?>[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_parseBest_String_oneRule() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parseBest("30", TLocalDate.FROM);
    }

    @Test
    public void test_parseToBuilder_StringParsePosition() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        TTemporalAccessor result = test.parseUnresolved("ONE30XXX", pos);
        assertEquals(pos.getIndex(), 5);
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(result.getLong(DAY_OF_MONTH), 30L);
    }

    @Test
    public void test_parseToBuilder_StringParsePosition_parseError() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        TTemporalAccessor result = test.parseUnresolved("ONEXXX", pos);
        assertEquals(pos.getIndex(), 0); // TODO: is this right?
        assertEquals(pos.getErrorIndex(), 3);
        assertEquals(result, null);
    }

    @Test(expected = NullPointerException.class)
    public void test_parseToBuilder_StringParsePosition_nullString() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        test.parseUnresolved((String) null, pos);
    }

    @Test(expected = NullPointerException.class)
    public void test_parseToBuilder_StringParsePosition_nullParsePosition() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        test.parseUnresolved("ONE30", (ParsePosition) null);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void test_parseToBuilder_StringParsePosition_invalidPosition() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(6);
        test.parseUnresolved("ONE30", pos);
    }

    @Test
    public void test_toFormat_format() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        String result = format.format(TLocalDate.of(2008, 6, 30));
        assertEquals(result, "ONE30");
    }

    @Test(expected = NullPointerException.class)
    public void test_toFormat_format_null() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        format.format(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_toFormat_format_notCalendrical() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        format.format("Not a Calendrical");
    }

    @Test
    public void test_toFormat_parseObject_String() throws Exception {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        TDateTimeBuilder result = (TDateTimeBuilder) format.parseObject("ONE30");
        assertEquals(result.getLong(DAY_OF_MONTH), 30L);
    }

    @Test
    public void test_toFormat_parseObject_String_parseError() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        try {
            format.parseObject("ONEXXX");
            fail("Expected ParseException");
        } catch (ParseException ex) {
            assertEquals(ex.getMessage().contains("ONEXXX"), true);
            assertEquals(ex.getErrorOffset(), 3);
        }
    }

    @Test(expected = ParseException.class)
    public void test_toFormat_parseObject_String_parseErrorLongText() throws Exception {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        try {
            format.parseObject("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789");
        } catch (TDateTimeParseException ex) {
            assertEquals(
                    ex.getMessage().contains("ONEXXX6789012345678901234567890123456789012345678901234567890123..."),
                    true);
            assertEquals(ex.getParsedString(),
                    "ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789");
            assertEquals(ex.getErrorIndex(), 3);
            throw ex;
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_toFormat_parseObject_String_null() throws Exception {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        format.parseObject((String) null);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        TDateTimeBuilder result = (TDateTimeBuilder) format.parseObject("ONE30XXX", pos);
        assertEquals(pos.getIndex(), 5);
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(result.getLong(DAY_OF_MONTH), 30L);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_parseError() {

        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        TTemporalAccessor result = (TTemporalAccessor) format.parseObject("ONEXXX", pos);
        assertEquals(pos.getIndex(), 0); // TODO: is this right?
        assertEquals(pos.getErrorIndex(), 3);
        assertEquals(result, null);
    }

    @Test(expected = NullPointerException.class)
    public void test_toFormat_parseObject_StringParsePosition_nullString() {

        // SimpleDateFormat has this behavior
        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        format.parseObject((String) null, pos);
    }

    @Test(expected = NullPointerException.class)
    public void test_toFormat_parseObject_StringParsePosition_nullParsePosition() {

        // SimpleDateFormat has this behavior
        TDateTimeFormatter test = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        Format format = test.toFormat();
        format.parseObject("ONE30", (ParsePosition) null);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_invalidPosition_tooBig() {

        // SimpleDateFormat has this behavior
        TDateTimeFormatter dtf = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(6);
        Format test = dtf.toFormat();
        assertNull(test.parseObject("ONE30", pos));
        assertTrue(pos.getErrorIndex() >= 0);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_invalidPosition_tooSmall() {

        // SimpleDateFormat throws StringIndexOutOfBoundException
        TDateTimeFormatter dtf = this.fmt.withLocale(Locale.ENGLISH).withDecimalStyle(TDecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(-1);
        Format test = dtf.toFormat();
        assertNull(test.parseObject("ONE30", pos));
        assertTrue(pos.getErrorIndex() >= 0);
    }

    @Test
    public void test_toFormat_Class_format() {

        Format format = BASIC_FORMATTER.toFormat();
        String result = format.format(TLocalDate.of(2008, 6, 30));
        assertEquals(result, "ONE30");
    }

    @Test
    public void test_toFormat_Class_parseObject_String() throws Exception {

        Format format = DATE_FORMATTER.toFormat(TLocalDate.FROM);
        TLocalDate result = (TLocalDate) format.parseObject("ONE2012 07 27");
        assertEquals(result, TLocalDate.of(2012, 7, 27));
    }

    @Test(expected = ParseException.class)
    public void test_toFormat_parseObject_StringParsePosition_dateTimeError() throws Exception {

        Format format = DATE_FORMATTER.toFormat(TLocalDate.FROM);
        format.parseObject("ONE2012 07 32");
    }

    @Test(expected = NullPointerException.class)
    public void test_toFormat_Class() {

        BASIC_FORMATTER.toFormat(null);
    }

    public void test_parse_allZones() {

        for (String zoneStr : TZoneId.getAvailableZoneIds()) {
            TZoneId zone = TZoneId.of(zoneStr);
            TZonedDateTime base = TZonedDateTime.of(2014, 12, 31, 12, 0, 0, 0, zone);
            TZonedDateTime test = TZonedDateTime.parse(base.toString());
            assertEquals(test, base);
        }
    }

}
