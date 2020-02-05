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
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.util.Locale;

import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.ReducedPrinterParser;
import org.teavm.classlib.java.time.temporal.MockFieldValue;

public class TestReducedPrinter extends AbstractTestPrinterParser {

    @Test(expected = TDateTimeException.class)
    public void test_print_emptyCalendrical() {

        ReducedPrinterParser pp = new ReducedPrinterParser(YEAR, 2, 2, 2010, null);
        pp.print(this.printEmptyContext, this.buf);
    }

    @Test
    public void test_print_append() {

        this.printContext.setDateTime(TLocalDate.of(2012, 1, 1));
        ReducedPrinterParser pp = new ReducedPrinterParser(YEAR, 2, 2, 2010, null);
        this.buf.append("EXISTING");
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "EXISTING12");
    }

    Object[][] provider_pivot() {

        return new Object[][] { { 1, 2010, 2010, "0" }, { 1, 2010, 2011, "1" }, { 1, 2010, 2012, "2" },
        { 1, 2010, 2013, "3" }, { 1, 2010, 2014, "4" }, { 1, 2010, 2015, "5" }, { 1, 2010, 2016, "6" },
        { 1, 2010, 2017, "7" }, { 1, 2010, 2018, "8" }, { 1, 2010, 2019, "9" }, { 1, 2010, 2009, "9" },
        { 1, 2010, 2020, "0" },

        { 2, 2010, 2010, "10" }, { 2, 2010, 2011, "11" }, { 2, 2010, 2021, "21" }, { 2, 2010, 2099, "99" },
        { 2, 2010, 2100, "00" }, { 2, 2010, 2109, "09" }, { 2, 2010, 2009, "09" }, { 2, 2010, 2110, "10" },

        { 2, 2005, 2005, "05" }, { 2, 2005, 2099, "99" }, { 2, 2005, 2100, "00" }, { 2, 2005, 2104, "04" },
        { 2, 2005, 2004, "04" }, { 2, 2005, 2105, "05" },

        { 3, 2005, 2005, "005" }, { 3, 2005, 2099, "099" }, { 3, 2005, 2100, "100" }, { 3, 2005, 2999, "999" },
        { 3, 2005, 3000, "000" }, { 3, 2005, 3004, "004" }, { 3, 2005, 2004, "004" }, { 3, 2005, 3005, "005" },

        { 9, 2005, 2005, "000002005" }, { 9, 2005, 2099, "000002099" }, { 9, 2005, 2100, "000002100" },
        { 9, 2005, 999999999, "999999999" }, { 9, 2005, 1000000000, "000000000" }, { 9, 2005, 1000002004, "000002004" },
        { 9, 2005, 2004, "000002004" }, { 9, 2005, 1000002005, "000002005" },

        { 2, -2005, -2005, "05" }, { 2, -2005, -2000, "00" }, { 2, -2005, -1999, "99" }, { 2, -2005, -1904, "04" },
        { 2, -2005, -2006, "06" }, { 2, -2005, -1905, "05" }, };
    }

    @Test
    public void test_pivot() {

        for (Object[] data : provider_pivot()) {
            int width = (int) data[0];
            int baseValue = (int) data[1];
            int value = (int) data[2];
            String result = (String) data[3];

            TZonedDateTime zdt = TLocalDateTime.of(2011, 6, 30, 12, 30, 40, 0).atZone(TZoneId.of("Europe/Paris"));
            TDateTimePrintContext context = new TDateTimePrintContext(zdt, Locale.ENGLISH, TDecimalStyle.STANDARD);
            context.setDateTime(new MockFieldValue(YEAR, value));
            ReducedPrinterParser pp = new ReducedPrinterParser(YEAR, width, width, baseValue, null);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(context, sb);
                if (result == null) {
                    fail("Expected exception");
                }
                assertEquals(sb.toString(), result);
            } catch (TDateTimeException ex) {
                if (result == null || value < 0) {
                    assertEquals(ex.getMessage().contains(YEAR.toString()), true);
                } else {
                    throw ex;
                }
            }
        }
    }

    @Test
    public void test_toString() {

        ReducedPrinterParser pp = new ReducedPrinterParser(YEAR, 2, 2, 2005, null);
        assertEquals(pp.toString(), "ReducedValue(TYear,2,2,2005)");
    }

}
