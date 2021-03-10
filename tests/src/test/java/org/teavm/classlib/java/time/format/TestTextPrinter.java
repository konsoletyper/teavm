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
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.TextPrinterParser;
import org.teavm.classlib.java.time.temporal.MockFieldValue;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestTextPrinter extends AbstractTestPrinterParser {

    private static final TDateTimeTextProvider PROVIDER = TDateTimeTextProvider.getInstance();

    @Test(expected = TDateTimeException.class)
    public void test_print_emptyCalendrical() {

        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        pp.print(this.printEmptyContext, this.buf);
    }

    public void test_print_append() {

        this.printContext.setDateTime(TLocalDate.of(2012, 4, 18));
        TextPrinterParser pp = new TextPrinterParser(DAY_OF_WEEK, TTextStyle.FULL, PROVIDER);
        this.buf.append("EXISTING");
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "EXISTINGWednesday");
    }

    Object[][] provider_dow() {

        return new Object[][] { { DAY_OF_WEEK, TTextStyle.FULL, 1, "Monday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 2, "Tuesday" }, { DAY_OF_WEEK, TTextStyle.FULL, 3, "Wednesday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 4, "Thursday" }, { DAY_OF_WEEK, TTextStyle.FULL, 5, "Friday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 6, "Saturday" }, { DAY_OF_WEEK, TTextStyle.FULL, 7, "Sunday" },

        { DAY_OF_WEEK, TTextStyle.SHORT, 1, "Mon" }, { DAY_OF_WEEK, TTextStyle.SHORT, 2, "Tue" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 3, "Wed" }, { DAY_OF_WEEK, TTextStyle.SHORT, 4, "Thu" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 5, "Fri" }, { DAY_OF_WEEK, TTextStyle.SHORT, 6, "Sat" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 7, "Sun" },

        { DAY_OF_MONTH, TTextStyle.FULL, 1, "1" }, { DAY_OF_MONTH, TTextStyle.FULL, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.FULL, 3, "3" }, { DAY_OF_MONTH, TTextStyle.FULL, 28, "28" },
        { DAY_OF_MONTH, TTextStyle.FULL, 29, "29" }, { DAY_OF_MONTH, TTextStyle.FULL, 30, "30" },
        { DAY_OF_MONTH, TTextStyle.FULL, 31, "31" },

        { DAY_OF_MONTH, TTextStyle.SHORT, 1, "1" }, { DAY_OF_MONTH, TTextStyle.SHORT, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 3, "3" }, { DAY_OF_MONTH, TTextStyle.SHORT, 28, "28" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 29, "29" }, { DAY_OF_MONTH, TTextStyle.SHORT, 30, "30" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 31, "31" },

        { MONTH_OF_YEAR, TTextStyle.FULL, 1, "January" }, { MONTH_OF_YEAR, TTextStyle.FULL, 12, "December" },

        { MONTH_OF_YEAR, TTextStyle.SHORT, 1, "Jan" }, { MONTH_OF_YEAR, TTextStyle.SHORT, 12, "Dec" }, };
    }

    @Test
    public void test_print() {

        for (Object[] data : provider_dow()) {
            StringBuilder sb = new StringBuilder();
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String expected = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(field, value));
            TextPrinterParser pp = new TextPrinterParser(field, style, PROVIDER);
            pp.print(this.printContext, sb);
            assertEquals(sb.toString(), expected);
        }
    }

    @Test
    public void test_print_french_long() {

        this.printContext.setLocale(Locale.FRENCH);
        this.printContext.setDateTime(TLocalDate.of(2012, 1, 1));
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "janvier");
    }

    @Test
    public void test_print_french_short() {

        this.printContext.setLocale(Locale.FRENCH);
        this.printContext.setDateTime(TLocalDate.of(2012, 1, 1));
        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "janv.");
    }

    @Test
    public void test_toString1() {

        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.FULL, PROVIDER);
        assertEquals(pp.toString(), "Text(MonthOfYear)");
    }

    @Test
    public void test_toString2() {

        TextPrinterParser pp = new TextPrinterParser(MONTH_OF_YEAR, TTextStyle.SHORT, PROVIDER);
        assertEquals(pp.toString(), "Text(MonthOfYear,SHORT)");
    }

}
