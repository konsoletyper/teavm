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
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;

import org.junit.Test;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.NumberPrinterParser;
import org.teavm.classlib.java.time.temporal.MockFieldValue;

public class TestNumberPrinter extends AbstractTestPrinterParser {

    @Test(expected = TDateTimeException.class)
    public void test_print_emptyCalendrical() {

        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, 1, 2, TSignStyle.NEVER);
        pp.print(this.printEmptyContext, this.buf);
    }

    @Test
    public void test_print_append() {

        this.printContext.setDateTime(TLocalDate.of(2012, 1, 3));
        NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, 1, 2, TSignStyle.NEVER);
        this.buf.append("EXISTING");
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "EXISTING3");
    }

    Object[][] provider_pad() {

        return new Object[][] { { 1, 1, -10, null }, { 1, 1, -9, "9" }, { 1, 1, -1, "1" }, { 1, 1, 0, "0" },
        { 1, 1, 3, "3" }, { 1, 1, 9, "9" }, { 1, 1, 10, null },

        { 1, 2, -100, null }, { 1, 2, -99, "99" }, { 1, 2, -10, "10" }, { 1, 2, -9, "9" }, { 1, 2, -1, "1" },
        { 1, 2, 0, "0" }, { 1, 2, 3, "3" }, { 1, 2, 9, "9" }, { 1, 2, 10, "10" }, { 1, 2, 99, "99" },
        { 1, 2, 100, null },

        { 2, 2, -100, null }, { 2, 2, -99, "99" }, { 2, 2, -10, "10" }, { 2, 2, -9, "09" }, { 2, 2, -1, "01" },
        { 2, 2, 0, "00" }, { 2, 2, 3, "03" }, { 2, 2, 9, "09" }, { 2, 2, 10, "10" }, { 2, 2, 99, "99" },
        { 2, 2, 100, null },

        { 1, 3, -1000, null }, { 1, 3, -999, "999" }, { 1, 3, -100, "100" }, { 1, 3, -99, "99" }, { 1, 3, -10, "10" },
        { 1, 3, -9, "9" }, { 1, 3, -1, "1" }, { 1, 3, 0, "0" }, { 1, 3, 3, "3" }, { 1, 3, 9, "9" }, { 1, 3, 10, "10" },
        { 1, 3, 99, "99" }, { 1, 3, 100, "100" }, { 1, 3, 999, "999" }, { 1, 3, 1000, null },

        { 2, 3, -1000, null }, { 2, 3, -999, "999" }, { 2, 3, -100, "100" }, { 2, 3, -99, "99" }, { 2, 3, -10, "10" },
        { 2, 3, -9, "09" }, { 2, 3, -1, "01" }, { 2, 3, 0, "00" }, { 2, 3, 3, "03" }, { 2, 3, 9, "09" },
        { 2, 3, 10, "10" }, { 2, 3, 99, "99" }, { 2, 3, 100, "100" }, { 2, 3, 999, "999" }, { 2, 3, 1000, null },

        { 3, 3, -1000, null }, { 3, 3, -999, "999" }, { 3, 3, -100, "100" }, { 3, 3, -99, "099" }, { 3, 3, -10, "010" },
        { 3, 3, -9, "009" }, { 3, 3, -1, "001" }, { 3, 3, 0, "000" }, { 3, 3, 3, "003" }, { 3, 3, 9, "009" },
        { 3, 3, 10, "010" }, { 3, 3, 99, "099" }, { 3, 3, 100, "100" }, { 3, 3, 999, "999" }, { 3, 3, 1000, null },

        { 1, 10, Integer.MAX_VALUE - 1, "2147483646" }, { 1, 10, Integer.MAX_VALUE, "2147483647" },
        { 1, 10, Integer.MIN_VALUE + 1, "2147483647" }, { 1, 10, Integer.MIN_VALUE, "2147483648" }, };
    }

    @Test
    public void test_pad_NOT_NEGATIVE() {

        for (Object[] data : provider_pad()) {
            int minPad = (int) data[0];
            int maxPad = (int) data[1];
            long value = ((Number) data[2]).longValue();
            String result = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(DAY_OF_MONTH, value));
            NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minPad, maxPad, TSignStyle.NOT_NEGATIVE);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(this.printContext, sb);
                if (result == null || value < 0) {
                    fail("Expected exception");
                }
                assertEquals(sb.toString(), result);
            } catch (TDateTimeException ex) {
                if (result == null || value < 0) {
                    assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
                } else {
                    throw ex;
                }
            }
        }
    }

    @Test
    public void test_pad_NEVER() {

        for (Object[] data : provider_pad()) {
            int minPad = (int) data[0];
            int maxPad = (int) data[1];
            long value = ((Number) data[2]).longValue();
            String result = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(DAY_OF_MONTH, value));
            NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minPad, maxPad, TSignStyle.NEVER);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(this.printContext, sb);
                if (result == null) {
                    fail("Expected exception");
                }
                assertEquals(sb.toString(), result);
            } catch (TDateTimeException ex) {
                if (result != null) {
                    throw ex;
                }
                assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
            }
        }
    }

    @Test
    public void test_pad_NORMAL() {

        for (Object[] data : provider_pad()) {
            int minPad = (int) data[0];
            int maxPad = (int) data[1];
            long value = ((Number) data[2]).longValue();
            String result = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(DAY_OF_MONTH, value));
            NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minPad, maxPad, TSignStyle.NORMAL);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(this.printContext, sb);
                if (result == null) {
                    fail("Expected exception");
                }
                assertEquals(sb.toString(), (value < 0 ? "-" + result : result));
            } catch (TDateTimeException ex) {
                if (result != null) {
                    throw ex;
                }
                assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
            }
        }
    }

    @Test
    public void test_pad_ALWAYS() {

        for (Object[] data : provider_pad()) {
            int minPad = (int) data[0];
            int maxPad = (int) data[1];
            long value = ((Number) data[2]).longValue();
            String result = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(DAY_OF_MONTH, value));
            NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minPad, maxPad, TSignStyle.ALWAYS);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(this.printContext, sb);
                if (result == null) {
                    fail("Expected exception");
                }
                assertEquals(sb.toString(), (value < 0 ? "-" + result : "+" + result));
            } catch (TDateTimeException ex) {
                if (result != null) {
                    throw ex;
                }
                assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
            }
        }
    }

    @Test
    public void test_pad_EXCEEDS_PAD() {

        for (Object[] data : provider_pad()) {
            int minPad = (int) data[0];
            int maxPad = (int) data[1];
            long value = ((Number) data[2]).longValue();
            String result = (String) data[3];

            this.printContext.setDateTime(new MockFieldValue(DAY_OF_MONTH, value));
            NumberPrinterParser pp = new NumberPrinterParser(DAY_OF_MONTH, minPad, maxPad, TSignStyle.EXCEEDS_PAD);
            StringBuilder sb = new StringBuilder();
            try {
                pp.print(this.printContext, sb);
                if (result == null) {
                    fail("Expected exception");
                    return; // unreachable
                }
                if (result.length() > minPad || value < 0) {
                    result = (value < 0 ? "-" + result : "+" + result);
                }
                assertEquals(sb.toString(), result);
            } catch (TDateTimeException ex) {
                if (result != null) {
                    throw ex;
                }
                assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
            }
        }
    }

    @Test
    public void test_toString1() {

        NumberPrinterParser pp = new NumberPrinterParser(HOUR_OF_DAY, 1, 19, TSignStyle.NORMAL);
        assertEquals(pp.toString(), "Value(HourOfDay)");
    }

    @Test
    public void test_toString2() {

        NumberPrinterParser pp = new NumberPrinterParser(HOUR_OF_DAY, 2, 2, TSignStyle.NOT_NEGATIVE);
        assertEquals(pp.toString(), "Value(HourOfDay,2)");
    }

    @Test
    public void test_toString3() {

        NumberPrinterParser pp = new NumberPrinterParser(HOUR_OF_DAY, 1, 2, TSignStyle.NOT_NEGATIVE);
        assertEquals(pp.toString(), "Value(HourOfDay,1,2,NOT_NEGATIVE)");
    }

}
