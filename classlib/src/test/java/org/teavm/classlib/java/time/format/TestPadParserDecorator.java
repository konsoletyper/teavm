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
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import org.junit.Test;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.CharLiteralPrinterParser;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.NumberPrinterParser;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.PadPrinterParserDecorator;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public class TestPadParserDecorator extends AbstractTestPrinterParser {

    @Test(expected = IndexOutOfBoundsException.class)
    public void test_parse_negativePosition() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 3, '-');
        pp.parse(this.parseContext, "--Z", -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void test_parse_offEndPosition() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 3, '-');
        pp.parse(this.parseContext, "--Z", 4);
    }

    @Test
    public void test_parse() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(
                new NumberPrinterParser(MONTH_OF_YEAR, 1, 3, TSignStyle.NEVER), 3, '-');
        int result = pp.parse(this.parseContext, "--2", 0);
        assertEquals(result, 3);
        assertParsed(MONTH_OF_YEAR, 2L);
    }

    @Test
    public void test_parse_noReadBeyond() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(
                new NumberPrinterParser(MONTH_OF_YEAR, 1, 3, TSignStyle.NEVER), 3, '-');
        int result = pp.parse(this.parseContext, "--22", 0);
        assertEquals(result, 3);
        assertParsed(MONTH_OF_YEAR, 2L);
    }

    @Test
    public void test_parse_textLessThanPadWidth() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(
                new NumberPrinterParser(MONTH_OF_YEAR, 1, 3, TSignStyle.NEVER), 3, '-');
        int result = pp.parse(this.parseContext, "-1", 0);
        assertEquals(result, ~0);
    }

    @Test
    public void test_parse_decoratedErrorPassedBack() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(
                new NumberPrinterParser(MONTH_OF_YEAR, 1, 3, TSignStyle.NEVER), 3, '-');
        int result = pp.parse(this.parseContext, "--A", 0);
        assertEquals(result, ~2);
    }

    @Test
    public void test_parse_decoratedDidNotParseToPadWidth() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(
                new NumberPrinterParser(MONTH_OF_YEAR, 1, 3, TSignStyle.NEVER), 3, '-');
        int result = pp.parse(this.parseContext, "-1X", 0);
        assertEquals(result, ~1);
    }

    private void assertParsed(TTemporalField field, Long value) {

        if (value == null) {
            assertEquals(this.parseContext.getParsed(field), null);
        } else {
            assertEquals(this.parseContext.getParsed(field), value);
        }
    }

}
