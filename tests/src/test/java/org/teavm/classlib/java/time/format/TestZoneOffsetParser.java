/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.OffsetIdPrinterParser;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestZoneOffsetParser extends AbstractTestPrinterParser {

    Object[][] data_error() {

        return new Object[][] {
        { new OffsetIdPrinterParser("Z", "+HH:MM:ss"), "hello", -1, IndexOutOfBoundsException.class },
        { new OffsetIdPrinterParser("Z", "+HH:MM:ss"), "hello", 6, IndexOutOfBoundsException.class }, };
    }

    @Test
    public void test_parse_error() {

        for (Object[] data : data_error()) {
            OffsetIdPrinterParser pp = (OffsetIdPrinterParser) data[0];
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
    public void test_parse_exactMatch_UTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "Z", 0);
        assertEquals(result, 1);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_startStringMatch_UTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "ZOTHER", 0);
        assertEquals(result, 1);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_midStringMatch_UTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "OTHERZOTHER", 5);
        assertEquals(result, 6);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_endStringMatch_UTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "OTHERZ", 5);
        assertEquals(result, 6);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_exactMatch_UTC_EmptyUTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "", 0);
        assertEquals(result, 0);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_startStringMatch_UTC_EmptyUTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "OTHER", 0);
        assertEquals(result, 0);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_midStringMatch_UTC_EmptyUTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "OTHEROTHER", 5);
        assertEquals(result, 5);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_endStringMatch_UTC_EmptyUTC() {

        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "OTHER", 5);
        assertEquals(result, 5);
        assertParsed(TZoneOffset.UTC);
    }

    Object[][] provider_offsets() {

        return new Object[][] { { "+HH", "+00", TZoneOffset.UTC }, { "+HH", "-00", TZoneOffset.UTC },
        { "+HH", "+01", TZoneOffset.ofHours(1) }, { "+HH", "-01", TZoneOffset.ofHours(-1) },

        { "+HHMM", "+0000", TZoneOffset.UTC }, { "+HHMM", "-0000", TZoneOffset.UTC },
        { "+HHMM", "+0102", TZoneOffset.ofHoursMinutes(1, 2) },
        { "+HHMM", "-0102", TZoneOffset.ofHoursMinutes(-1, -2) },

        { "+HH:MM", "+00:00", TZoneOffset.UTC }, { "+HH:MM", "-00:00", TZoneOffset.UTC },
        { "+HH:MM", "+01:02", TZoneOffset.ofHoursMinutes(1, 2) },
        { "+HH:MM", "-01:02", TZoneOffset.ofHoursMinutes(-1, -2) },

        { "+HHMMss", "+0000", TZoneOffset.UTC }, { "+HHMMss", "-0000", TZoneOffset.UTC },
        { "+HHMMss", "+0100", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0) },
        { "+HHMMss", "+0159", TZoneOffset.ofHoursMinutesSeconds(1, 59, 0) },
        { "+HHMMss", "+0200", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0) },
        { "+HHMMss", "+1800", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0) },
        { "+HHMMss", "+010215", TZoneOffset.ofHoursMinutesSeconds(1, 2, 15) },
        { "+HHMMss", "-0100", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0) },
        { "+HHMMss", "-0200", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0) },
        { "+HHMMss", "-1800", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0) },

        { "+HHMMss", "+000000", TZoneOffset.UTC }, { "+HHMMss", "-000000", TZoneOffset.UTC },
        { "+HHMMss", "+010000", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0) },
        { "+HHMMss", "+010203", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3) },
        { "+HHMMss", "+015959", TZoneOffset.ofHoursMinutesSeconds(1, 59, 59) },
        { "+HHMMss", "+020000", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0) },
        { "+HHMMss", "+180000", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0) },
        { "+HHMMss", "-010000", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0) },
        { "+HHMMss", "-020000", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0) },
        { "+HHMMss", "-180000", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0) },

        { "+HH:MM:ss", "+00:00", TZoneOffset.UTC }, { "+HH:MM:ss", "-00:00", TZoneOffset.UTC },
        { "+HH:MM:ss", "+01:00", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0) },
        { "+HH:MM:ss", "+01:02", TZoneOffset.ofHoursMinutesSeconds(1, 2, 0) },
        { "+HH:MM:ss", "+01:59", TZoneOffset.ofHoursMinutesSeconds(1, 59, 0) },
        { "+HH:MM:ss", "+02:00", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0) },
        { "+HH:MM:ss", "+18:00", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0) },
        { "+HH:MM:ss", "+01:02:15", TZoneOffset.ofHoursMinutesSeconds(1, 2, 15) },
        { "+HH:MM:ss", "-01:00", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0) },
        { "+HH:MM:ss", "-02:00", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0) },
        { "+HH:MM:ss", "-18:00", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0) },

        { "+HH:MM:ss", "+00:00:00", TZoneOffset.UTC }, { "+HH:MM:ss", "-00:00:00", TZoneOffset.UTC },
        { "+HH:MM:ss", "+01:00:00", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0) },
        { "+HH:MM:ss", "+01:02:03", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3) },
        { "+HH:MM:ss", "+01:59:59", TZoneOffset.ofHoursMinutesSeconds(1, 59, 59) },
        { "+HH:MM:ss", "+02:00:00", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0) },
        { "+HH:MM:ss", "+18:00:00", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0) },
        { "+HH:MM:ss", "-01:00:00", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0) },
        { "+HH:MM:ss", "-02:00:00", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0) },
        { "+HH:MM:ss", "-18:00:00", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0) },

        { "+HHMMSS", "+000000", TZoneOffset.UTC }, { "+HHMMSS", "-000000", TZoneOffset.UTC },
        { "+HHMMSS", "+010203", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3) },
        { "+HHMMSS", "-010203", TZoneOffset.ofHoursMinutesSeconds(-1, -2, -3) },

        { "+HH:MM:SS", "+00:00:00", TZoneOffset.UTC }, { "+HH:MM:SS", "-00:00:00", TZoneOffset.UTC },
        { "+HH:MM:SS", "+01:02:03", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3) },
        { "+HH:MM:SS", "-01:02:03", TZoneOffset.ofHoursMinutesSeconds(-1, -2, -3) }, };
    }

    @Test
    public void test_parse_exactMatch() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, parse, 0);
            assertEquals(result, parse.length());
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_startStringMatch() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, parse + ":OTHER", 0);
            assertEquals(result, parse.length());
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_midStringMatch() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, "OTHER" + parse + ":OTHER", 5);
            assertEquals(result, parse.length() + 5);
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_endStringMatch() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, "OTHER" + parse, 5);
            assertEquals(result, parse.length() + 5);
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_exactMatch_EmptyUTC() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", pattern);
            int result = pp.parse(this.parseContext, parse, 0);
            assertEquals(result, parse.length());
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_startStringMatch_EmptyUTC() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", pattern);
            int result = pp.parse(this.parseContext, parse + ":OTHER", 0);
            assertEquals(result, parse.length());
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_midStringMatch_EmptyUTC() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", pattern);
            int result = pp.parse(this.parseContext, "OTHER" + parse + ":OTHER", 5);
            assertEquals(result, parse.length() + 5);
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_endStringMatch_EmptyUTC() {

        for (Object[] data : provider_offsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            TZoneOffset expected = (TZoneOffset) data[2];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("", pattern);
            int result = pp.parse(this.parseContext, "OTHER" + parse, 5);
            assertEquals(result, parse.length() + 5);
            assertParsed(expected);
        }
    }

    Object[][] provider_bigOffsets() {

        return new Object[][] { { "+HH", "+59", 59 * 3600 }, { "+HH", "-19", -(19 * 3600) },

        { "+HHMM", "+1801", 18 * 3600 + 1 * 60 }, { "+HHMM", "-1801", -(18 * 3600 + 1 * 60) },

        { "+HH:MM", "+18:01", 18 * 3600 + 1 * 60 }, { "+HH:MM", "-18:01", -(18 * 3600 + 1 * 60) },

        { "+HHMMss", "+180103", 18 * 3600 + 1 * 60 + 3 }, { "+HHMMss", "-180103", -(18 * 3600 + 1 * 60 + 3) },

        { "+HH:MM:ss", "+18:01:03", 18 * 3600 + 1 * 60 + 3 }, { "+HH:MM:ss", "-18:01:03", -(18 * 3600 + 1 * 60 + 3) },

        { "+HHMMSS", "+180103", 18 * 3600 + 1 * 60 + 3 }, { "+HHMMSS", "-180103", -(18 * 3600 + 1 * 60 + 3) },

        { "+HH:MM:SS", "+18:01:03", 18 * 3600 + 1 * 60 + 3 },
        { "+HH:MM:SS", "-18:01:03", -(18 * 3600 + 1 * 60 + 3) }, };
    }

    @Test
    public void test_parse_bigOffsets() {

        for (Object[] data : provider_bigOffsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            long offsetSecs = ((Number) data[2]).longValue();

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, parse, 0);
            assertEquals(result, parse.length());
            assertEquals(this.parseContext.getParsed(OFFSET_SECONDS), (Long) offsetSecs);
        }
    }

    Object[][] provider_badOffsets() {

        return new Object[][] { { "+HH", "+1", ~0 }, { "+HH", "-1", ~0 }, { "+HH", "01", ~0 }, { "+HH", "01", ~0 },
        { "+HH", "+AA", ~0 },

        { "+HHMM", "+1", ~0 }, { "+HHMM", "+01", ~0 }, { "+HHMM", "+001", ~0 }, { "+HHMM", "0102", ~0 },
        { "+HHMM", "+01:02", ~0 }, { "+HHMM", "+AAAA", ~0 },

        { "+HH:MM", "+1", ~0 }, { "+HH:MM", "+01", ~0 }, { "+HH:MM", "+0:01", ~0 }, { "+HH:MM", "+00:1", ~0 },
        { "+HH:MM", "+0:1", ~0 }, { "+HH:MM", "+:", ~0 }, { "+HH:MM", "01:02", ~0 }, { "+HH:MM", "+0102", ~0 },
        { "+HH:MM", "+AA:AA", ~0 },

        { "+HHMMss", "+1", ~0 }, { "+HHMMss", "+01", ~0 }, { "+HHMMss", "+001", ~0 }, { "+HHMMss", "0102", ~0 },
        { "+HHMMss", "+01:02", ~0 }, { "+HHMMss", "+AAAA", ~0 },

        { "+HH:MM:ss", "+1", ~0 }, { "+HH:MM:ss", "+01", ~0 }, { "+HH:MM:ss", "+0:01", ~0 },
        { "+HH:MM:ss", "+00:1", ~0 }, { "+HH:MM:ss", "+0:1", ~0 }, { "+HH:MM:ss", "+:", ~0 },
        { "+HH:MM:ss", "01:02", ~0 }, { "+HH:MM:ss", "+0102", ~0 }, { "+HH:MM:ss", "+AA:AA", ~0 },

        { "+HHMMSS", "+1", ~0 }, { "+HHMMSS", "+01", ~0 }, { "+HHMMSS", "+001", ~0 }, { "+HHMMSS", "0102", ~0 },
        { "+HHMMSS", "+01:02", ~0 }, { "+HHMMSS", "+AAAA", ~0 },

        { "+HH:MM:SS", "+1", ~0 }, { "+HH:MM:SS", "+01", ~0 }, { "+HH:MM:SS", "+0:01", ~0 },
        { "+HH:MM:SS", "+00:1", ~0 }, { "+HH:MM:SS", "+0:1", ~0 }, { "+HH:MM:SS", "+:", ~0 },
        { "+HH:MM:SS", "01:02", ~0 }, { "+HH:MM:SS", "+0102", ~0 }, { "+HH:MM:SS", "+AA:AA", ~0 }, };
    }

    @Test
    public void test_parse_invalid() {

        for (Object[] data : provider_badOffsets()) {
            String pattern = (String) data[0];
            String parse = (String) data[1];
            int expectedPosition = (int) data[2];

            OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", pattern);
            int result = pp.parse(this.parseContext, parse, 0);
            assertEquals(result, expectedPosition);
        }
    }

    @Test
    public void test_parse_caseSensitiveUTC_matchedCase() {

        this.parseContext.setCaseSensitive(true);
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "Z", 0);
        assertEquals(result, 1);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_caseSensitiveUTC_unmatchedCase() {

        this.parseContext.setCaseSensitive(true);
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "z", 0);
        assertEquals(result, ~0);
        assertParsed(null);
    }

    @Test
    public void test_parse_caseInsensitiveUTC_matchedCase() {

        this.parseContext.setCaseSensitive(false);
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "Z", 0);
        assertEquals(result, 1);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_caseInsensitiveUTC_unmatchedCase() {

        this.parseContext.setCaseSensitive(false);
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        int result = pp.parse(this.parseContext, "z", 0);
        assertEquals(result, 1);
        assertParsed(TZoneOffset.UTC);
    }

    private void assertParsed(TZoneOffset expectedOffset) {

        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
        assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
        if (expectedOffset == null) {
            assertEquals(this.parseContext.getParsed(OFFSET_SECONDS), null);
        } else {
            assertEquals(this.parseContext.getParsed(OFFSET_SECONDS), (Long) (long) expectedOffset.getTotalSeconds());
        }
    }

}
