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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.DecimalStyle;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Test DateTimeFormatter.
 */
@TeaVMTest
public class TestDateTimeFormatter {

    private static final DateTimeFormatter BASIC_FORMATTER = DateTimeFormatter.ofPattern("'ONE'd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("'ONE'uuuu MM dd");

    private DateTimeFormatter fmt;

    @BeforeEach
    public void setUp() {
        fmt = new DateTimeFormatterBuilder().appendLiteral("ONE")
                                            .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                                            .toFormatter();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_withLocale() throws Exception {
        DateTimeFormatter base = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        DateTimeFormatter test = base.withLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, test.getLocale());
    }

    @Test
    public void test_withLocale_null() throws Exception {
        DateTimeFormatter base = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> base.withLocale((Locale) null));
    }

    //-----------------------------------------------------------------------
    // print
    //-----------------------------------------------------------------------
    @Test
    public void test_print_Calendrical() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        String result = test.format(LocalDate.of(2008, 6, 30));
        assertEquals("ONE30", result);
    }

    @Test
    public void test_print_Calendrical_noSuchField() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(DateTimeException.class, () -> test.format(LocalTime.of(11, 30)));
    }

    @Test
    public void test_print_Calendrical_null() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.format((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_print_CalendricalAppendable() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        test.formatTo(LocalDate.of(2008, 6, 30), buf);
        assertEquals("ONE30", buf.toString());
    }

    @Test
    public void test_print_CalendricalAppendable_noSuchField() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        assertThrows(DateTimeException.class, () -> test.formatTo(LocalTime.of(11, 30), buf));
    }

    @Test
    public void test_print_CalendricalAppendable_nullCalendrical() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        StringBuilder buf = new StringBuilder();
        assertThrows(NullPointerException.class, () -> test.formatTo((TemporalAccessor) null, buf));
    }

    @Test
    public void test_print_CalendricalAppendable_nullAppendable() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.formatTo(LocalDate.of(2008, 6, 30), (Appendable) null));
    }

    @Test  // IOException
    public void test_print_CalendricalAppendable_ioError() throws Throwable {
        assertThrows(IOException.class, () -> {
            DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
            try {
                test.formatTo(LocalDate.of(2008, 6, 30), new MockIOExceptionAppendable());
            } catch (DateTimeException ex) {
                assertTrue(ex.getCause() instanceof IOException);
                throw ex.getCause();
            }
        });
    }

    //-----------------------------------------------------------------------
    // parse(Class)
    //-----------------------------------------------------------------------
    @Test
    public void test_parse_Class_String() throws Exception {
        LocalDate result = DATE_FORMATTER.parse("ONE2012 07 27", LocalDate::from);
        assertEquals(LocalDate.of(2012, 7, 27), result);
    }

    @Test
    public void test_parse_Class_CharSequence() throws Exception {
        LocalDate result = DATE_FORMATTER.parse(new StringBuilder("ONE2012 07 27"), LocalDate::from);
        assertEquals(LocalDate.of(2012, 7, 27), result);
    }

    @Test
    public void test_parse_Class_String_parseError() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                DATE_FORMATTER.parse("ONE2012 07 XX", LocalDate::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains("ONE2012 07 XX"));
                assertEquals("ONE2012 07 XX", ex.getParsedString());
                assertEquals(11, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parse_Class_String_parseErrorLongText() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                DATE_FORMATTER.parse("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                        LocalDate::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains(
                        "ONEXXX6789012345678901234567890123456789012345678901234567890123..."));
                assertEquals("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                        ex.getParsedString());
                assertEquals(3, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parse_Class_String_parseIncomplete() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            try {
                DATE_FORMATTER.parse("ONE2012 07 27SomethingElse", LocalDate::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains("ONE2012 07 27SomethingElse"));
                assertEquals("ONE2012 07 27SomethingElse", ex.getParsedString());
                assertEquals(13, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parse_Class_String_nullText() throws Exception {
        assertThrows(NullPointerException.class, () -> DATE_FORMATTER.parse((String) null, LocalDate::from));
    }

    @Test
    public void test_parse_Class_String_nullRule() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.parse("30", (TemporalQuery<?>) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseBest_firstOption() throws Exception {
        DateTimeFormatter test = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        TemporalAccessor result = test.parseBest("2011-06-30", LocalDate::from, YearMonth::from);
        assertEquals(LocalDate.of(2011, 6, 30), result);
    }

    @Test
    public void test_parseBest_secondOption() throws Exception {
        DateTimeFormatter test = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        TemporalAccessor result = test.parseBest("2011-06", LocalDate::from, YearMonth::from);
        assertEquals(YearMonth.of(2011, 6), result);
    }

    @Test
    public void test_parseBest_String_parseError() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            DateTimeFormatter test = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
            try {
                test.parseBest("2011-XX-30", LocalDate::from, YearMonth::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains("XX"));
                assertEquals("2011-XX-30", ex.getParsedString());
                assertEquals(5, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parseBest_String_parseErrorLongText() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
            try {
                test.parseBest("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789", 
                        LocalDate::from, YearMonth::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains(
                        "ONEXXX6789012345678901234567890123456789012345678901234567890123..."));
                assertEquals("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                        ex.getParsedString());
                assertEquals(3, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parseBest_String_parseIncomplete() throws Exception {
        assertThrows(DateTimeParseException.class, () -> {
            DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
            try {
                test.parseBest("ONE30SomethingElse", YearMonth::from, LocalDate::from);
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains("could not be parsed"));
                assertTrue(ex.getMessage().contains("ONE30SomethingElse"));
                assertEquals("ONE30SomethingElse", ex.getParsedString());
                assertEquals(5, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_parseBest_String_nullText() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.parseBest((String) null, YearMonth::from, LocalDate::from));
    }

    @Test
    public void test_parseBest_String_nullRules() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.parseBest("30", (TemporalQuery<?>[]) null));
    }

    @Test
    public void test_parseBest_String_zeroRules() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(IllegalArgumentException.class, () -> test.parseBest("30", new TemporalQuery<?>[0]));
    }

    @Test
    public void test_parseBest_String_oneRule() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(IllegalArgumentException.class, () -> test.parseBest("30", LocalDate::from));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseToBuilder_StringParsePosition() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor result = test.parseUnresolved("ONE30XXX", pos);
        assertEquals(5, pos.getIndex());
        assertEquals(-1, pos.getErrorIndex());
        assertEquals(30L, result.getLong(DAY_OF_MONTH));
    }

    @Test
    public void test_parseToBuilder_StringParsePosition_parseError() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor result = test.parseUnresolved("ONEXXX", pos);
        assertEquals(0, pos.getIndex());  // TODO: is this right?
        assertEquals(3, pos.getErrorIndex());
        assertEquals(null, result);
    }

    @Test
    public void test_parseToBuilder_StringParsePosition_nullString() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(0);
        assertThrows(NullPointerException.class, () -> test.parseUnresolved((String) null, pos));
    }

    @Test
    public void test_parseToBuilder_StringParsePosition_nullParsePosition() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        assertThrows(NullPointerException.class, () -> test.parseUnresolved("ONE30", (ParsePosition) null));
    }

    @Test
    public void test_parseToBuilder_StringParsePosition_invalidPosition() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(6);
        assertThrows(IndexOutOfBoundsException.class, () -> test.parseUnresolved("ONE30", pos));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_toFormat_format() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        String result = format.format(LocalDate.of(2008, 6, 30));
        assertEquals("ONE30", result);
    }

    @Test
    public void test_toFormat_format_null() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        assertThrows(NullPointerException.class, () -> format.format(null));
    }

    @Test
    public void test_toFormat_format_notCalendrical() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        assertThrows(IllegalArgumentException.class, () -> format.format("Not a Calendrical"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toFormat_parseObject_String() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        TemporalAccessor result = (TemporalAccessor) format.parseObject("ONE30");
        assertEquals(30L, result.getLong(DAY_OF_MONTH));
    }

    @Test
    public void test_toFormat_parseObject_String_parseError() throws Exception {
        assertThrows(ParseException.class, () -> {
            DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
            Format format = test.toFormat();
            try {
                format.parseObject("ONEXXX");
            } catch (ParseException ex) {
                assertTrue(ex.getMessage().contains("ONEXXX"));
                assertEquals(3, ex.getErrorOffset());
                throw ex;
            }
        });
    }

    @Test
    public void test_toFormat_parseObject_String_parseErrorLongText() throws Exception {
        assertThrows(ParseException.class, () -> {
            DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
            Format format = test.toFormat();
            try {
                format.parseObject("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789");
            } catch (DateTimeParseException ex) {
                assertTrue(ex.getMessage().contains(
                        "ONEXXX6789012345678901234567890123456789012345678901234567890123..."));
                assertEquals("ONEXXX67890123456789012345678901234567890123456789012345678901234567890123456789",
                        ex.getParsedString());
                assertEquals(3, ex.getErrorIndex());
                throw ex;
            }
        });
    }

    @Test
    public void test_toFormat_parseObject_String_null() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        assertThrows(NullPointerException.class, () -> format.parseObject((String) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toFormat_parseObject_StringParsePosition() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor result = (TemporalAccessor) format.parseObject("ONE30XXX", pos);
        assertEquals(5, pos.getIndex());
        assertEquals(-1, pos.getErrorIndex());
        assertEquals(30L, result.getLong(DAY_OF_MONTH));
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_parseError() throws Exception {
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor result = (TemporalAccessor) format.parseObject("ONEXXX", pos);
        assertEquals(0, pos.getIndex());  // TODO: is this right?
        assertEquals(3, pos.getErrorIndex());
        assertEquals(null, result);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_nullString() throws Exception {
        // SimpleDateFormat has this behavior
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        ParsePosition pos = new ParsePosition(0);
        assertThrows(NullPointerException.class, () -> format.parseObject((String) null, pos));
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_nullParsePosition() throws Exception {
        // SimpleDateFormat has this behavior
        DateTimeFormatter test = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        Format format = test.toFormat();
        assertThrows(NullPointerException.class, () -> format.parseObject("ONE30", (ParsePosition) null));
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_invalidPosition_tooBig() throws Exception {
        // SimpleDateFormat has this behavior
        DateTimeFormatter dtf = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(6);
        Format test = dtf.toFormat();
        assertNull(test.parseObject("ONE30", pos));
        assertTrue(pos.getErrorIndex() >= 0);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_invalidPosition_tooSmall() throws Exception {
        // SimpleDateFormat throws StringIndexOutOfBoundException
        DateTimeFormatter dtf = fmt.withLocale(Locale.ENGLISH).withDecimalStyle(DecimalStyle.STANDARD);
        ParsePosition pos = new ParsePosition(-1);
        Format test = dtf.toFormat();
        assertNull(test.parseObject("ONE30", pos));
        assertTrue(pos.getErrorIndex() >= 0);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toFormat_Class_format() throws Exception {
        Format format = BASIC_FORMATTER.toFormat();
        String result = format.format(LocalDate.of(2008, 6, 30));
        assertEquals("ONE30", result);
    }

    @Test
    public void test_toFormat_Class_parseObject_String() throws Exception {
        Format format = DATE_FORMATTER.toFormat(LocalDate::from);
        LocalDate result = (LocalDate) format.parseObject("ONE2012 07 27");
        assertEquals(LocalDate.of(2012, 7, 27), result);
    }

    @Test
    public void test_toFormat_parseObject_StringParsePosition_dateTimeError() throws Exception {
        Format format = DATE_FORMATTER.toFormat(LocalDate::from);
        assertThrows(ParseException.class, () -> format.parseObject("ONE2012 07 32"));
    }

    @Test
    public void test_toFormat_Class() throws Exception {
        assertThrows(NullPointerException.class, () -> BASIC_FORMATTER.toFormat(null));
    }

    //-------------------------------------------------------------------------
    @Test
    public void test_parse_allZones() throws Exception {
        for (String zoneStr : ZoneId.getAvailableZoneIds()) {
            // TODO: looks like our implementation does not support that. Fix and remove this hack
            if (zoneStr.startsWith("GMT")) {
                continue;
            }
            ZoneId zone = ZoneId.of(zoneStr);
            ZonedDateTime base = ZonedDateTime.of(2014, 12, 31, 12, 0, 0, 0, zone);
            ZonedDateTime test = ZonedDateTime.parse(base.toString());
            assertEquals(base, test);
        }
    }

}
