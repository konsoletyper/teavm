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
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.OffsetIdPrinterParser;

@Test
public class TestZoneOffsetPrinter extends AbstractTestPrinterParser {

    private static final TZoneOffset OFFSET_0130 = TZoneOffset.of("+01:30");

    //-----------------------------------------------------------------------
    @DataProvider(name="offsets")
    Object[][] provider_offsets() {
        return new Object[][] {
            {"+HH", "NO-OFFSET", TZoneOffset.UTC},
            {"+HH", "+01", TZoneOffset.ofHours(1)},
            {"+HH", "-01", TZoneOffset.ofHours(-1)},

            {"+HHMM", "NO-OFFSET", TZoneOffset.UTC},
            {"+HHMM", "+0102", TZoneOffset.ofHoursMinutes(1, 2)},
            {"+HHMM", "-0102", TZoneOffset.ofHoursMinutes(-1, -2)},

            {"+HH:MM", "NO-OFFSET", TZoneOffset.UTC},
            {"+HH:MM", "+01:02", TZoneOffset.ofHoursMinutes(1, 2)},
            {"+HH:MM", "-01:02", TZoneOffset.ofHoursMinutes(-1, -2)},

            {"+HHMMss", "NO-OFFSET", TZoneOffset.UTC},
            {"+HHMMss", "+0100", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HHMMss", "+0102", TZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HHMMss", "+0159", TZoneOffset.ofHoursMinutesSeconds(1, 59, 0)},
            {"+HHMMss", "+0200", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HHMMss", "+1800", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HHMMss", "+010215", TZoneOffset.ofHoursMinutesSeconds(1, 2, 15)},
            {"+HHMMss", "-0100", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HHMMss", "-0200", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HHMMss", "-1800", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HHMMss", "NO-OFFSET", TZoneOffset.UTC},
            {"+HHMMss", "+0100", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HHMMss", "+010203", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HHMMss", "+015959", TZoneOffset.ofHoursMinutesSeconds(1, 59, 59)},
            {"+HHMMss", "+0200", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HHMMss", "+1800", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HHMMss", "-0100", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HHMMss", "-0200", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HHMMss", "-1800", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HH:MM:ss", "NO-OFFSET", TZoneOffset.UTC},
            {"+HH:MM:ss", "+01:00", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HH:MM:ss", "+01:02", TZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HH:MM:ss", "+01:59", TZoneOffset.ofHoursMinutesSeconds(1, 59, 0)},
            {"+HH:MM:ss", "+02:00", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HH:MM:ss", "+18:00", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HH:MM:ss", "+01:02:15", TZoneOffset.ofHoursMinutesSeconds(1, 2, 15)},
            {"+HH:MM:ss", "-01:00", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HH:MM:ss", "-02:00", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HH:MM:ss", "-18:00", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HH:MM:ss", "NO-OFFSET", TZoneOffset.UTC},
            {"+HH:MM:ss", "+01:00", TZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HH:MM:ss", "+01:02:03", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HH:MM:ss", "+01:59:59", TZoneOffset.ofHoursMinutesSeconds(1, 59, 59)},
            {"+HH:MM:ss", "+02:00", TZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HH:MM:ss", "+18:00", TZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HH:MM:ss", "-01:00", TZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HH:MM:ss", "-02:00", TZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HH:MM:ss", "-18:00", TZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HHMMSS", "NO-OFFSET", TZoneOffset.UTC},
            {"+HHMMSS", "+010203", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HHMMSS", "-010203", TZoneOffset.ofHoursMinutesSeconds(-1, -2, -3)},
            {"+HHMMSS", "+010200", TZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HHMMSS", "-010200", TZoneOffset.ofHoursMinutesSeconds(-1, -2, 0)},

            {"+HH:MM:SS", "NO-OFFSET", TZoneOffset.UTC},
            {"+HH:MM:SS", "+01:02:03", TZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HH:MM:SS", "-01:02:03", TZoneOffset.ofHoursMinutesSeconds(-1, -2, -3)},
            {"+HH:MM:SS", "+01:02:00", TZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HH:MM:SS", "-01:02:00", TZoneOffset.ofHoursMinutesSeconds(-1, -2, 0)},
        };
    }

    @Test(dataProvider="offsets")
    public void test_print(String pattern, String expected, TZoneOffset offset) throws Exception {
        buf.append("EXISTING");
        printContext.setDateTime(new TDateTimeBuilder(OFFSET_SECONDS, offset.getTotalSeconds()));
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("NO-OFFSET", pattern);
        pp.print(printContext, buf);
        assertEquals(buf.toString(), "EXISTING" + expected);
    }

    @Test(dataProvider="offsets")
    public void test_toString(String pattern, String expected, TZoneOffset offset) throws Exception {
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("NO-OFFSET", pattern);
        assertEquals(pp.toString(), "Offset(" + pattern + ",'NO-OFFSET')");
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=TDateTimeException.class)
    public void test_print_emptyCalendrical() throws Exception {
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        pp.print(printEmptyContext, buf);
    }

    public void test_print_emptyAppendable() throws Exception {
        printContext.setDateTime(new TDateTimeBuilder(OFFSET_SECONDS, OFFSET_0130.getTotalSeconds()));
        OffsetIdPrinterParser pp = new OffsetIdPrinterParser("Z", "+HH:MM:ss");
        pp.print(printContext, buf);
        assertEquals(buf.toString(), "+01:30");
    }

}
