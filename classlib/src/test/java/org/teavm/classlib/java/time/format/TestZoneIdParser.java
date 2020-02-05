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
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.ZoneIdPrinterParser;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.zone.TZoneRulesProvider;

public class TestZoneIdParser extends AbstractTestPrinterParser {

    private static final String AMERICA_DENVER = "America/Denver";

    private static final TZoneId TIME_ZONE_DENVER = TZoneId.of(AMERICA_DENVER);

    Object[][] data_error() {

        return new Object[][] {
        { new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null), "hello", -1, IndexOutOfBoundsException.class },
        { new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null), "hello", 6, IndexOutOfBoundsException.class }, };
    }

    @Test
    public void test_parse_error() {

        for (Object[] data : data_error()) {
            ZoneIdPrinterParser pp = (ZoneIdPrinterParser) data[0];
            String text = (String) data[1];
            int pos = (int) data[2];
            Class<?> expected = (Class<?>) data[3];

            try {
                pp.parse(this.parseContext, text, pos);
            } catch (RuntimeException ex) {
                assertTrue(expected.isInstance(ex));
                assertEquals(this.parseContext.toParsed().fieldValues.size(), 0);
            }
        }
    }

    @Test
    public void test_parse_exactMatch_Denver() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, AMERICA_DENVER, 0);
        assertEquals(result, AMERICA_DENVER.length());
        assertParsed(TIME_ZONE_DENVER);
    }

    @Test
    public void test_parse_startStringMatch_Denver() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, AMERICA_DENVER + "OTHER", 0);
        assertEquals(result, AMERICA_DENVER.length());
        assertParsed(TIME_ZONE_DENVER);
    }

    @Test
    public void test_parse_midStringMatch_Denver() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHER" + AMERICA_DENVER + "OTHER", 5);
        assertEquals(result, 5 + AMERICA_DENVER.length());
        assertParsed(TIME_ZONE_DENVER);
    }

    @Test
    public void test_parse_endStringMatch_Denver() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHER" + AMERICA_DENVER, 5);
        assertEquals(result, 5 + AMERICA_DENVER.length());
        assertParsed(TIME_ZONE_DENVER);
    }

    @Test
    public void test_parse_partialMatch() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHERAmerica/Bogusville", 5);
        assertEquals(result, -6);
        assertParsed(null);
    }

    Object[][] populateTestData() {

        Set<String> ids = TZoneRulesProvider.getAvailableZoneIds();
        Object[][] rtnval = new Object[ids.size()][];
        int i = 0;
        for (String id : ids) {
            rtnval[i++] = new Object[] { id, TZoneId.of(id) };
        }
        return rtnval;
    }

    @Test
    @Ignore
    public void test_parse_exactMatch() {

        for (Object[] data : populateTestData()) {
            String parse = (String) data[0];
            TZoneId expected = (TZoneId) data[1];

            this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD,
                    TIsoChronology.INSTANCE);
            ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
            int result = pp.parse(this.parseContext, parse, 0);
            assertEquals(parse + " expected " + expected, result, parse.length());
            assertParsed(expected);
        }
    }

    @Test
    public void test_parse_lowerCase() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        this.parseContext.setCaseSensitive(false);
        int result = pp.parse(this.parseContext, "europe/london", 0);
        assertEquals(result, 13);
        assertParsed(TZoneId.of("Europe/London"));
    }

    @Test
    public void test_parse_endStringMatch_utc() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHERZ", 5);
        assertEquals(result, 6);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_endStringMatch_utc_plus1() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHER+01:00", 5);
        assertEquals(result, 11);
        assertParsed(TZoneId.of("+01:00"));
    }

    @Test
    public void test_parse_midStringMatch_utc() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHERZOTHER", 5);
        assertEquals(result, 6);
        assertParsed(TZoneOffset.UTC);
    }

    @Test
    public void test_parse_midStringMatch_utc_plus1() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), null);
        int result = pp.parse(this.parseContext, "OTHER+01:00OTHER", 5);
        assertEquals(result, 11);
        assertParsed(TZoneId.of("+01:00"));
    }

    @Test
    public void test_toString_id() {

        ZoneIdPrinterParser pp = new ZoneIdPrinterParser(TTemporalQueries.zoneId(), "TZoneId()");
        assertEquals(pp.toString(), "TZoneId()");
    }

    private void assertParsed(TZoneId expectedZone) {

        assertEquals(this.parseContext.toParsed().zone, expectedZone);
    }

}
