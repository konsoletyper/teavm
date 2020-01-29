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
import static org.threeten.bp.temporal.ChronoField.DAY_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.YEAR;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.format.DateTimeFormatterBuilder.ReducedPrinterParser;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;

/**
 * Test ReducedPrinterParser.
 */
@Test
public class TestReducedParser extends AbstractTestPrinterParser {

    //-----------------------------------------------------------------------
    @DataProvider(name="error")
    Object[][] data_error() {
        return new Object[][] {
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12", -1, IndexOutOfBoundsException.class},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12", 3, IndexOutOfBoundsException.class},
        };
    }

    @Test(dataProvider="error")
    public void test_parse_error(ReducedPrinterParser pp, String text, int pos, Class<?> expected) {
        try {
            pp.parse(parseContext, text, pos);
        } catch (RuntimeException ex) {
            assertTrue(expected.isInstance(ex));
            assertEquals(parseContext.toParsed().query(TemporalQueries.chronology()), null);
            assertEquals(parseContext.toParsed().query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    public void test_parse_fieldRangeIgnored() throws Exception {
        ReducedPrinterParser pp = new ReducedPrinterParser(DAY_OF_YEAR, 3, 3, 10, null);
        int newPos = pp.parse(parseContext, "456", 0);
        assertEquals(newPos, 3);
        assertParsed(DAY_OF_YEAR, 456L);  // parsed dayOfYear=456
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="Parse")
    Object[][] provider_parse() {
        return new Object[][] {
             // negative zero
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "-0", 0, ~0, null},

            // general
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "Xxx12Xxx", 3, 5, 2012},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12345", 0, 2, 2012},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12-45", 0, 2, 2012},

            // insufficient digits
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "0", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1", 1, ~1, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1-2", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "9", 0, ~0, null},

            // other junk
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "A0", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "0A", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "  1", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "-1", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "-10", 0, ~0, null},

            // parse OK 1
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "0", 0, 1, 2010},
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "9", 0, 1, 2019},
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "10", 0, 1, 2011},

            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "0", 0, 1, 2010},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "4", 0, 1, 2014},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "5", 0, 1, 2005},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "9", 0, 1, 2009},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "10", 0, 1, 2011},

            // parse OK 2
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "00", 0, 2, 2100},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "09", 0, 2, 2109},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "10", 0, 2, 2010},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "99", 0, 2, 2099},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "100", 0, 2, 2010},

            // parse OK 2
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "05", 0, 2, -2005},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "00", 0, 2, -2000},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "99", 0, 2, -1999},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "06", 0, 2, -1906},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "100", 0, 2, -1910},
       };
    }

    @Test(dataProvider="Parse")
    public void test_parse(ReducedPrinterParser pp, String input, int pos, int parseLen, Integer parseVal) {
        int newPos = pp.parse(parseContext, input, pos);
        assertEquals(newPos, parseLen);
        assertParsed(YEAR, parseVal != null ? (long) parseVal : null);
    }

    @DataProvider(name="ParseLenient")
    Object[][] provider_parseLenient() {
        return new Object[][] {
             // negative zero
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "-0", 0, ~0, null},

            // general
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "Xxx12Xxx", 3, 5, 2012},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12345", 0, 5, 12345},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "12-45", 0, 2, 2012},

            // insufficient digits
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "0", 0, 1, 0},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1", 0, 1, 1},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1", 1, ~1, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "1-2", 0, 1, 1},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "9", 0, 1, 9},

            // other junk
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "A0", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "0A", 0, 1, 0},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "  1", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "-1", 0, ~0, null},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "-10", 0, ~0, null},

            // parse OK 1
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "0", 0, 1, 2010},
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "9", 0, 1, 2019},
            {new ReducedPrinterParser(YEAR, 1, 1, 2010, null), "10", 0, 2, 10},

            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "0", 0, 1, 2010},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "4", 0, 1, 2014},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "5", 0, 1, 2005},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "9", 0, 1, 2009},
            {new ReducedPrinterParser(YEAR, 1, 1, 2005, null), "10", 0, 2, 10},

            // parse OK 2
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "00", 0, 2, 2100},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "09", 0, 2, 2109},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "10", 0, 2, 2010},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "99", 0, 2, 2099},
            {new ReducedPrinterParser(YEAR, 2, 2, 2010, null), "100", 0, 3, 100},

            // parse OK 2
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "05", 0, 2, -2005},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "00", 0, 2, -2000},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "99", 0, 2, -1999},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "06", 0, 2, -1906},
            {new ReducedPrinterParser(YEAR, 2, 2, -2005, null), "100", 0, 3, 100},
       };
    }

    @Test(dataProvider="ParseLenient")
    public void test_parseLenient(ReducedPrinterParser pp, String input, int pos, int parseLen, Integer parseVal) {
        parseContext.setStrict(false);
        int newPos = pp.parse(parseContext, input, pos);
        assertEquals(newPos, parseLen);
        assertParsed(YEAR, parseVal != null ? (long) parseVal : null);
    }

    private void assertParsed(TemporalField field, Long value) {
        if (value == null) {
            assertEquals(parseContext.getParsed(field), null);
        } else {
            assertEquals(parseContext.getParsed(field), value);
        }
    }

}
