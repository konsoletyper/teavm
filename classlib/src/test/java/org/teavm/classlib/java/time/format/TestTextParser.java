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
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.Locale;

import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.TextPrinterParser;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

public class TestTextParser extends AbstractTestPrinterParser {

    private static final TDateTimeTextProvider PROVIDER = TDateTimeTextProvider.getInstance();

    Object[][] data_error() {

        return new Object[][] {
        { new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER), "Monday", -1,
        IndexOutOfBoundsException.class },
        { new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER), "Monday", 7,
        IndexOutOfBoundsException.class }, };
    }

    @Test
    public void test_parse_error() {

        for (Object[] data : data_error()) {
            TextPrinterParser pp = (TextPrinterParser) data[0];
            String text = (String) data[1];
            int pos = (int) data[2];
            Class<?> expected = (Class<?>) data[3];

            try {
                pp.parse(this.parseContext, text, pos);
            } catch (RuntimeException ex) {
                assertTrue(expected.isInstance(ex));
                assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
                assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
            }
        }
    }

    @Test
    public void test_parse_midStr() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "XxxMondayXxx", 3);
        assertEquals(newPos, 9);
        assertParsed(this.parseContext, DAY_OF_WEEK, 1L);
    }

    @Test
    public void test_parse_remainderIgnored() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Wednesday", 0);
        assertEquals(newPos, 3);
        assertParsed(this.parseContext, DAY_OF_WEEK, 3L);
    }

    @Test
    public void test_parse_noMatch1() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Munday", 0);
        assertEquals(newPos, ~0);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_noMatch2() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Monday", 3);
        assertEquals(newPos, ~3);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_noMatch_atEnd() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Monday", 6);
        assertEquals(newPos, ~6);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    Object[][] provider_number() {

        return new Object[][] { { DAY_OF_MONTH, TTextStyle.FULL, 1, "1" }, { DAY_OF_MONTH, TTextStyle.FULL, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.FULL, 30, "30" }, { DAY_OF_MONTH, TTextStyle.FULL, 31, "31" },

        { DAY_OF_MONTH, TTextStyle.SHORT, 1, "1" }, { DAY_OF_MONTH, TTextStyle.SHORT, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 30, "30" }, { DAY_OF_MONTH, TTextStyle.SHORT, 31, "31" }, };
    }

    @Test
    public void test_parseNumber() {

        for (Object[] data : provider_number()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            int newPos = pp.parse(this.parseContext, input, 0);
            assertEquals(newPos, input.length());
            assertParsed(this.parseContext, field, (long) value);
        }
    }

    Object[][] provider_text() {

        return new Object[][] { { DAY_OF_WEEK, TTextStyle.FULL, 1, "Monday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 2, "Tuesday" }, { DAY_OF_WEEK, TTextStyle.FULL, 3, "Wednesday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 4, "Thursday" }, { DAY_OF_WEEK, TTextStyle.FULL, 5, "Friday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 6, "Saturday" }, { DAY_OF_WEEK, TTextStyle.FULL, 7, "Sunday" },

        { DAY_OF_WEEK, TTextStyle.SHORT, 1, "Mon" }, { DAY_OF_WEEK, TTextStyle.SHORT, 2, "Tue" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 3, "Wed" }, { DAY_OF_WEEK, TTextStyle.SHORT, 4, "Thu" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 5, "Fri" }, { DAY_OF_WEEK, TTextStyle.SHORT, 6, "Sat" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 7, "Sun" },

        { MONTH_OF_YEAR, TTextStyle.FULL, 1, "January" }, { MONTH_OF_YEAR, TTextStyle.FULL, 12, "December" },

        { MONTH_OF_YEAR, TTextStyle.SHORT, 1, "Jan" }, { MONTH_OF_YEAR, TTextStyle.SHORT, 12, "Dec" }, };
    }

    @Test
    public void test_parseText() {

        for (Object[] data : provider_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            int newPos = pp.parse(this.parseContext, input, 0);
            assertEquals(newPos, input.length());
            assertParsed(this.parseContext, field, (long) value);
        }
    }

    @Test
    public void test_parse_strict_caseSensitive_parseUpper() {

        for (Object[] data : provider_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            this.parseContext.setCaseSensitive(true);
            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            int newPos = pp.parse(this.parseContext, input.toUpperCase(), 0);
            assertEquals(newPos, ~0);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
        }
    }

    @Test
    public void test_parse_strict_caseInsensitive_parseUpper() {

        for (Object[] data : provider_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            this.parseContext.setCaseSensitive(false);
            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            int newPos = pp.parse(this.parseContext, input.toUpperCase(), 0);
            assertEquals(newPos, input.length());
            assertParsed(this.parseContext, field, (long) value);
        }
    }

    @Test
    public void test_parse_strict_caseSensitive_parseLower() {

        for (Object[] data : provider_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            this.parseContext.setCaseSensitive(true);
            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            int newPos = pp.parse(this.parseContext, input.toLowerCase(), 0);
            assertEquals(newPos, ~0);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
        }
    }

    @Test
    public void test_parse_strict_caseInsensitive_parseLower() {

        for (Object[] data : provider_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String input = (String) data[3];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            this.parseContext.setCaseSensitive(false);
            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            int newPos = pp.parse(this.parseContext, input.toLowerCase(), 0);
            assertEquals(newPos, input.length());
            assertParsed(this.parseContext, field, (long) value);
        }
    }

    @Test
    public void test_parse_full_strict_full_match() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "January", 0);
        assertEquals(newPos, 7);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_full_strict_short_noMatch() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Janua", 0);
        assertEquals(newPos, ~0);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_full_strict_number_noMatch() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "1", 0);
        assertEquals(newPos, ~0);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_short_strict_full_match() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "January", 0);
        assertEquals(newPos, 3);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_short_strict_short_match() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Janua", 0);
        assertEquals(newPos, 3);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_short_strict_number_noMatch() {

        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "1", 0);
        assertEquals(newPos, ~0);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_french_short_strict_full_noMatch() {

        this.parseContext.setLocale(Locale.FRENCH);
        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "janvier", 0); // correct short form is 'janv.'
        assertEquals(newPos, ~0);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
    }

    @Test
    public void test_parse_french_short_strict_short_match() {

        this.parseContext.setLocale(Locale.FRENCH);
        this.parseContext.setStrict(true);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "janv.", 0);
        assertEquals(newPos, 5);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_full_lenient_full_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "January", 0);
        assertEquals(newPos, 7);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_full_lenient_short_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Janua", 0);
        assertEquals(newPos, 3);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_full_lenient_number_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        int newPos = pp.parse(this.parseContext, "1", 0);
        assertEquals(newPos, 1);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_short_lenient_full_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "January", 0);
        assertEquals(newPos, 7);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_short_lenient_short_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "Janua", 0);
        assertEquals(newPos, 3);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    @Test
    public void test_parse_short_lenient_number_match() {

        this.parseContext.setStrict(false);
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        int newPos = pp.parse(this.parseContext, "1", 0);
        assertEquals(newPos, 1);
        assertParsed(this.parseContext, MONTH_OF_YEAR, 1L);
    }

    private void assertParsed(TDateTimeParseContext context, TTemporalField field, Long value) {

        if (value == null) {
            assertEquals(context.getParsed(field), null);
        } else {
            assertEquals(context.getParsed(field), value);
        }
    }

}
