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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.CharLiteralPrinterParser;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.PadPrinterParserDecorator;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.StringLiteralPrinterParser;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestPadPrinterDecorator extends AbstractTestPrinterParser {

    @Test
    public void test_print_emptyCalendrical() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 3, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "--Z");
    }

    @Test
    public void test_print_fullDateTime() {

        this.printContext.setDateTime(TLocalDate.of(2008, 12, 3));
        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 3, '-');
        pp.print(this.printContext, this.buf);
        assertEquals(this.buf.toString(), "--Z");
    }

    @Test
    public void test_print_append() {

        this.buf.append("EXISTING");
        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 3, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "EXISTING--Z");
    }

    @Test
    public void test_print_noPadRequiredSingle() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 1, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "Z");
    }

    @Test
    public void test_print_padRequiredSingle() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new CharLiteralPrinterParser('Z'), 5, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "----Z");
    }

    @Test
    public void test_print_noPadRequiredMultiple() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new StringLiteralPrinterParser("WXYZ"), 4, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "WXYZ");
    }

    @Test
    public void test_print_padRequiredMultiple() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new StringLiteralPrinterParser("WXYZ"), 5, '-');
        pp.print(this.printEmptyContext, this.buf);
        assertEquals(this.buf.toString(), "-WXYZ");
    }

    @Test(expected = TDateTimeException.class)
    public void test_print_overPad() {

        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(new StringLiteralPrinterParser("WXYZ"), 3, '-');
        pp.print(this.printEmptyContext, this.buf);
    }

    @Test
    public void test_toString1() {

        CharLiteralPrinterParser wrapped = new CharLiteralPrinterParser('Y');
        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(wrapped, 5, ' ');
        assertEquals(pp.toString(), "Pad('Y',5)");
    }

    @Test
    public void test_toString2() {

        CharLiteralPrinterParser wrapped = new CharLiteralPrinterParser('Y');
        PadPrinterParserDecorator pp = new PadPrinterParserDecorator(wrapped, 5, '-');
        assertEquals(pp.toString(), "Pad('Y',5,'-')");
    }

}
