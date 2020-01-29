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
package org.threeten.bp.format;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.format.DateTimeFormatterBuilder.NumberPrinterParser;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;

/**
 * Test NumberPrinterParser.
 */
@Test
public class TestNumberParser extends AbstractTestPrinterParser {

    //-----------------------------------------------------------------------
    @DataProvider(name="error")
    Object[][] data_error() {
        return new Object[][] {
            {new NumberPrinterParser(DAY_OF_MONTH, 1, 2, SignStyle.NEVER), "12", -1, IndexOutOfBoundsException.class},
            {new NumberPrinterParser(DAY_OF_MONTH, 1, 2, SignStyle.NEVER), "12", 3, IndexOutOfBoundsException.class},
        };
    }

    @Test(dataProvider="error")
    public void test_parse_error(NumberPrinterParser pp, String text, int pos, Class<?> expected) {
        try {
            pp.parse(parseContext, text, pos);
        } catch (RuntimeException ex) {
            assertTrue(expected.isInstance(ex));
            assertEquals(parseContext.toParsed().query(TemporalQueries.chronology()), null);
            assertEquals(parseContext.toParsed().query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseData")
    Object[][] provider_parseData() {
        return new Object[][] {
            // normal
            {1, 2, SignStyle.NEVER, 0, "12", 0, 2, 12L},       // normal
            {1, 2, SignStyle.NEVER, 0, "Xxx12Xxx", 3, 5, 12L}, // parse in middle
            {1, 2, SignStyle.NEVER, 0, "99912999", 3, 5, 12L}, // parse in middle
            {2, 4, SignStyle.NEVER, 0, "12345", 0, 4, 1234L},  // stops at max width
            {2, 4, SignStyle.NEVER, 0, "12-45", 0, 2, 12L},    // stops at dash
            {2, 4, SignStyle.NEVER, 0, "123-5", 0, 3, 123L},   // stops at dash
            {1, 10, SignStyle.NORMAL, 0, "2147483647", 0, 10, Integer.MAX_VALUE},
            {1, 10, SignStyle.NORMAL, 0, "-2147483648", 0, 11, Integer.MIN_VALUE},
            {1, 10, SignStyle.NORMAL, 0, "2147483648", 0, 10, 2147483648L},
            {1, 10, SignStyle.NORMAL, 0, "-2147483649", 0, 11, -2147483649L},
            {1, 10, SignStyle.NORMAL, 0, "987659876598765", 0, 10, 9876598765L},
            {1, 19, SignStyle.NORMAL, 0, "999999999999999999", 0, 18, 999999999999999999L},
            {1, 19, SignStyle.NORMAL, 0, "-999999999999999999", 0, 19, -999999999999999999L},
            {1, 19, SignStyle.NORMAL, 0, "1000000000000000000", 0, 19, 1000000000000000000L},
            {1, 19, SignStyle.NORMAL, 0, "-1000000000000000000", 0, 20, -1000000000000000000L},
            {1, 19, SignStyle.NORMAL, 0, "000000000000000000", 0, 18, 0L},
            {1, 19, SignStyle.NORMAL, 0, "0000000000000000000", 0, 19, 0L},
            {1, 19, SignStyle.NORMAL, 0, "9223372036854775807", 0, 19, Long.MAX_VALUE},
            {1, 19, SignStyle.NORMAL, 0, "-9223372036854775808", 0, 20, Long.MIN_VALUE},
            {1, 19, SignStyle.NORMAL, 0, "9223372036854775808", 0, 18, 922337203685477580L},  // last digit not parsed
            {1, 19, SignStyle.NORMAL, 0, "-9223372036854775809", 0, 19, -922337203685477580L}, // last digit not parsed
            // no match
            {1, 2, SignStyle.NEVER, 1, "A1", 0, ~0, 0},
            {1, 2, SignStyle.NEVER, 1, " 1", 0, ~0, 0},
            {1, 2, SignStyle.NEVER, 1, "  1", 1, ~1, 0},
            {2, 2, SignStyle.NEVER, 1, "1", 0, ~0, 0},
            {2, 2, SignStyle.NEVER, 1, "Xxx1", 0, ~0, 0},
            {2, 2, SignStyle.NEVER, 1, "1", 1, ~1, 0},
            {2, 2, SignStyle.NEVER, 1, "Xxx1", 4, ~4, 0},
            {2, 2, SignStyle.NEVER, 1, "1-2", 0, ~0, 0},
            {1, 19, SignStyle.NORMAL, 0, "-000000000000000000", 0, ~0, 0},
            {1, 19, SignStyle.NORMAL, 0, "-0000000000000000000", 0, ~0, 0},
            // parse reserving space 1 (adjacent-parsing)
            {1, 1, SignStyle.NEVER, 1, "12", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 1, "12", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 1, "12345", 0, 4, 1234L},
            {1, 19, SignStyle.NEVER, 1, "12345678901", 0, 10, 1234567890L},
            {1, 19, SignStyle.NEVER, 1, "123456789012345678901234567890", 0, 19, 1234567890123456789L},
            {1, 19, SignStyle.NEVER, 1, "1", 0, 1, 1L},  // error from next field
            {2, 2, SignStyle.NEVER, 1, "12", 0, 2, 12L},  // error from next field
            {2, 19, SignStyle.NEVER, 1, "1", 0, ~0, 0},
            // parse reserving space 2 (adjacent-parsing)
            {1, 1, SignStyle.NEVER, 2, "123", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 2, "123", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 2, "12345", 0, 3, 123L},
            {1, 19, SignStyle.NEVER, 2, "12345678901", 0, 9, 123456789L},
            {1, 19, SignStyle.NEVER, 2, "123456789012345678901234567890", 0, 19, 1234567890123456789L},
            {1, 19, SignStyle.NEVER, 2, "1", 0, 1, 1L},  // error from next field
            {1, 19, SignStyle.NEVER, 2, "12", 0, 1, 1L},  // error from next field
            {2, 2, SignStyle.NEVER, 2, "12", 0, 2, 12L},  // error from next field
            {2, 19, SignStyle.NEVER, 2, "1", 0, ~0, 0},
            {2, 19, SignStyle.NEVER, 2, "1AAAAABBBBBCCCCC", 0, ~0, 0},
        };
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="parseData")
    public void test_parse_fresh(int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth, String text, int pos, int expectedPos, long expectedValue) {
        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minWidth, maxWidth, signStyle);
        if (subsequentWidth > 0) {
            pp = pp.withSubsequentWidth(subsequentWidth);
        }
        int newPos = pp.parse(parseContext, text, pos);
        assertEquals(newPos, expectedPos);
        if (expectedPos > 0) {
            assertParsed(parseContext, DAY_OF_MONTH, expectedValue);
        } else {
            assertEquals(parseContext.toParsed().query(TemporalQueries.chronology()), null);
            assertEquals(parseContext.toParsed().query(TemporalQueries.zoneId()), null);
        }
    }

    @Test(dataProvider="parseData")
    public void test_parse_textField(int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth, String text, int pos, int expectedPos, long expectedValue) {
        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_WEEK, minWidth, maxWidth, signStyle);
        if (subsequentWidth > 0) {
            pp = pp.withSubsequentWidth(subsequentWidth);
        }
        int newPos = pp.parse(parseContext, text, pos);
        assertEquals(newPos, expectedPos);
        if (expectedPos > 0) {
            assertParsed(parseContext, DAY_OF_WEEK, expectedValue);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseSignsStrict")
    Object[][] provider_parseSignsStrict() {
        return new Object[][] {
            // basics
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"1", 1, 2, SignStyle.NEVER, 1, 1},
            {"2", 1, 2, SignStyle.NEVER, 1, 2},
            {"3", 1, 2, SignStyle.NEVER, 1, 3},
            {"4", 1, 2, SignStyle.NEVER, 1, 4},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"6", 1, 2, SignStyle.NEVER, 1, 6},
            {"7", 1, 2, SignStyle.NEVER, 1, 7},
            {"8", 1, 2, SignStyle.NEVER, 1, 8},
            {"9", 1, 2, SignStyle.NEVER, 1, 9},
            {"10", 1, 2, SignStyle.NEVER, 2, 10},
            {"100", 1, 2, SignStyle.NEVER, 2, 10},
            {"100", 1, 3, SignStyle.NEVER, 3, 100},

            // never
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"50", 1, 2, SignStyle.NEVER, 2, 50},
            {"500", 1, 2, SignStyle.NEVER, 2, 50},
            {"-0", 1, 2, SignStyle.NEVER, ~0, null},
            {"-5", 1, 2, SignStyle.NEVER, ~0, null},
            {"-50", 1, 2, SignStyle.NEVER, ~0, null},
            {"-500", 1, 2, SignStyle.NEVER, ~0, null},
            {"-AAA", 1, 2, SignStyle.NEVER, ~0, null},
            {"+0", 1, 2, SignStyle.NEVER, ~0, null},
            {"+5", 1, 2, SignStyle.NEVER, ~0, null},
            {"+50", 1, 2, SignStyle.NEVER, ~0, null},
            {"+500", 1, 2, SignStyle.NEVER, ~0, null},
            {"+AAA", 1, 2, SignStyle.NEVER, ~0, null},

            // not negative
            {"0", 1, 2, SignStyle.NOT_NEGATIVE, 1, 0},
            {"5", 1, 2, SignStyle.NOT_NEGATIVE, 1, 5},
            {"50", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"500", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"-0", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"-5", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"-50", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"-500", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"-AAA", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+0", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+5", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+50", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+500", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+AAA", 1, 2, SignStyle.NOT_NEGATIVE, ~0, null},

            // normal
            {"0", 1, 2, SignStyle.NORMAL, 1, 0},
            {"5", 1, 2, SignStyle.NORMAL, 1, 5},
            {"50", 1, 2, SignStyle.NORMAL, 2, 50},
            {"500", 1, 2, SignStyle.NORMAL, 2, 50},
            {"-0", 1, 2, SignStyle.NORMAL, ~0, null},
            {"-5", 1, 2, SignStyle.NORMAL, 2, -5},
            {"-50", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-500", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-AAA", 1, 2, SignStyle.NORMAL, ~1, null},
            {"+0", 1, 2, SignStyle.NORMAL, ~0, null},
            {"+5", 1, 2, SignStyle.NORMAL, ~0, null},
            {"+50", 1, 2, SignStyle.NORMAL, ~0, null},
            {"+500", 1, 2, SignStyle.NORMAL, ~0, null},
            {"+AAA", 1, 2, SignStyle.NORMAL, ~0, null},

            // always
            {"0", 1, 2, SignStyle.ALWAYS, ~0, null},
            {"5", 1, 2, SignStyle.ALWAYS, ~0, null},
            {"50", 1, 2, SignStyle.ALWAYS, ~0, null},
            {"500", 1, 2, SignStyle.ALWAYS, ~0, null},
            {"-0", 1, 2, SignStyle.ALWAYS, ~0, null},
            {"-5", 1, 2, SignStyle.ALWAYS, 2, -5},
            {"-50", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-500", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-AAA", 1, 2, SignStyle.ALWAYS, ~1, null},
            {"+0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"+5", 1, 2, SignStyle.ALWAYS, 2, 5},
            {"+50", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+500", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+AAA", 1, 2, SignStyle.ALWAYS, ~1, null},

            // exceeds pad
            {"0", 1, 2, SignStyle.EXCEEDS_PAD, 1, 0},
            {"5", 1, 2, SignStyle.EXCEEDS_PAD, 1, 5},
            {"50", 1, 2, SignStyle.EXCEEDS_PAD, ~0, null},
            {"500", 1, 2, SignStyle.EXCEEDS_PAD, ~0, null},
            {"-0", 1, 2, SignStyle.EXCEEDS_PAD, ~0, null},
            {"-5", 1, 2, SignStyle.EXCEEDS_PAD, 2, -5},
            {"-50", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-500", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-AAA", 1, 2, SignStyle.EXCEEDS_PAD, ~1, null},
            {"+0", 1, 2, SignStyle.EXCEEDS_PAD, ~0, null},
            {"+5", 1, 2, SignStyle.EXCEEDS_PAD, ~0, null},
            {"+50", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+500", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+AAA", 1, 2, SignStyle.EXCEEDS_PAD, ~1, null},
       };
    }

    @Test(dataProvider="parseSignsStrict")
    public void test_parseSignsStrict(String input, int min, int max, SignStyle style, int parseLen, Integer parseVal) throws Exception {
        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, min, max, style);
        int newPos = pp.parse(parseContext, input, 0);
        assertEquals(newPos, parseLen);
        assertParsed(parseContext, DAY_OF_MONTH, (parseVal != null ? (long) parseVal : null));
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseSignsLenient")
    Object[][] provider_parseSignsLenient() {
        return new Object[][] {
            // never
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"50", 1, 2, SignStyle.NEVER, 2, 50},
            {"500", 1, 2, SignStyle.NEVER, 3, 500},
            {"-0", 1, 2, SignStyle.NEVER, 2, 0},
            {"-5", 1, 2, SignStyle.NEVER, 2, -5},
            {"-50", 1, 2, SignStyle.NEVER, 3, -50},
            {"-500", 1, 2, SignStyle.NEVER, 4, -500},
            {"-AAA", 1, 2, SignStyle.NEVER, ~1, null},
            {"+0", 1, 2, SignStyle.NEVER, 2, 0},
            {"+5", 1, 2, SignStyle.NEVER, 2, 5},
            {"+50", 1, 2, SignStyle.NEVER, 3, 50},
            {"+500", 1, 2, SignStyle.NEVER, 4, 500},
            {"+AAA", 1, 2, SignStyle.NEVER, ~1, null},
            {"50", 2, 2, SignStyle.NEVER, 2, 50},
            {"-50", 2, 2, SignStyle.NEVER, ~0, null},
            {"+50", 2, 2, SignStyle.NEVER, ~0, null},

            // not negative
            {"0", 1, 2, SignStyle.NOT_NEGATIVE, 1, 0},
            {"5", 1, 2, SignStyle.NOT_NEGATIVE, 1, 5},
            {"50", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"500", 1, 2, SignStyle.NOT_NEGATIVE, 3, 500},
            {"-0", 1, 2, SignStyle.NOT_NEGATIVE, 2, 0},
            {"-5", 1, 2, SignStyle.NOT_NEGATIVE, 2, -5},
            {"-50", 1, 2, SignStyle.NOT_NEGATIVE, 3, -50},
            {"-500", 1, 2, SignStyle.NOT_NEGATIVE, 4, -500},
            {"-AAA", 1, 2, SignStyle.NOT_NEGATIVE, ~1, null},
            {"+0", 1, 2, SignStyle.NOT_NEGATIVE, 2, 0},
            {"+5", 1, 2, SignStyle.NOT_NEGATIVE, 2, 5},
            {"+50", 1, 2, SignStyle.NOT_NEGATIVE, 3, 50},
            {"+500", 1, 2, SignStyle.NOT_NEGATIVE, 4, 500},
            {"+AAA", 1, 2, SignStyle.NOT_NEGATIVE, ~1, null},
            {"50", 2, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"-50", 2, 2, SignStyle.NOT_NEGATIVE, ~0, null},
            {"+50", 2, 2, SignStyle.NOT_NEGATIVE, ~0, null},

            // normal
            {"0", 1, 2, SignStyle.NORMAL, 1, 0},
            {"5", 1, 2, SignStyle.NORMAL, 1, 5},
            {"50", 1, 2, SignStyle.NORMAL, 2, 50},
            {"500", 1, 2, SignStyle.NORMAL, 3, 500},
            {"-0", 1, 2, SignStyle.NORMAL, 2, 0},
            {"-5", 1, 2, SignStyle.NORMAL, 2, -5},
            {"-50", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-500", 1, 2, SignStyle.NORMAL, 4, -500},
            {"-AAA", 1, 2, SignStyle.NORMAL, ~1, null},
            {"+0", 1, 2, SignStyle.NORMAL, 2, 0},
            {"+5", 1, 2, SignStyle.NORMAL, 2, 5},
            {"+50", 1, 2, SignStyle.NORMAL, 3, 50},
            {"+500", 1, 2, SignStyle.NORMAL, 4, 500},
            {"+AAA", 1, 2, SignStyle.NORMAL, ~1, null},
            {"50", 2, 2, SignStyle.NORMAL, 2, 50},
            {"-50", 2, 2, SignStyle.NORMAL, 3, -50},
            {"+50", 2, 2, SignStyle.NORMAL, 3, 50},

            // always
            {"0", 1, 2, SignStyle.ALWAYS, 1, 0},
            {"5", 1, 2, SignStyle.ALWAYS, 1, 5},
            {"50", 1, 2, SignStyle.ALWAYS, 2, 50},
            {"500", 1, 2, SignStyle.ALWAYS, 3, 500},
            {"-0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"-5", 1, 2, SignStyle.ALWAYS, 2, -5},
            {"-50", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-500", 1, 2, SignStyle.ALWAYS, 4, -500},
            {"-AAA", 1, 2, SignStyle.ALWAYS, ~1, null},
            {"+0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"+5", 1, 2, SignStyle.ALWAYS, 2, 5},
            {"+50", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+500", 1, 2, SignStyle.ALWAYS, 4, 500},
            {"+AAA", 1, 2, SignStyle.ALWAYS, ~1, null},

            // exceeds pad
            {"0", 1, 2, SignStyle.EXCEEDS_PAD, 1, 0},
            {"5", 1, 2, SignStyle.EXCEEDS_PAD, 1, 5},
            {"50", 1, 2, SignStyle.EXCEEDS_PAD, 2, 50},
            {"500", 1, 2, SignStyle.EXCEEDS_PAD, 3, 500},
            {"-0", 1, 2, SignStyle.EXCEEDS_PAD, 2, 0},
            {"-5", 1, 2, SignStyle.EXCEEDS_PAD, 2, -5},
            {"-50", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-500", 1, 2, SignStyle.EXCEEDS_PAD, 4, -500},
            {"-AAA", 1, 2, SignStyle.EXCEEDS_PAD, ~1, null},
            {"+0", 1, 2, SignStyle.EXCEEDS_PAD, 2, 0},
            {"+5", 1, 2, SignStyle.EXCEEDS_PAD, 2, 5},
            {"+50", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+500", 1, 2, SignStyle.EXCEEDS_PAD, 4, 500},
            {"+AAA", 1, 2, SignStyle.EXCEEDS_PAD, ~1, null},
       };
    }

    @Test(dataProvider="parseSignsLenient")
    public void test_parseSignsLenient(String input, int min, int max, SignStyle style, int parseLen, Integer parseVal) throws Exception {
        parseContext.setStrict(false);
        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, min, max, style);
        int newPos = pp.parse(parseContext, input, 0);
        assertEquals(newPos, parseLen);
        assertParsed(parseContext, DAY_OF_MONTH, (parseVal != null ? (long) parseVal : null));
    }

    private void assertParsed(DateTimeParseContext context, TemporalField field, Long value) {
        if (value == null) {
            assertEquals(context.getParsed(field), null);
        } else {
            assertEquals(context.getParsed(field), value);
        }
    }

}
