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
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.SettingsParser;

public class TestSettingsParser extends AbstractTestPrinterParser {

    @Test
    public void test_print_sensitive() {

        SettingsParser pp = SettingsParser.SENSITIVE;
        StringBuilder buf = new StringBuilder();
        pp.print(this.printContext, buf);
        assertEquals(buf.toString(), "");
    }

    @Test
    public void test_print_strict() {

        SettingsParser pp = SettingsParser.STRICT;
        StringBuilder buf = new StringBuilder();
        pp.print(this.printContext, buf);
        assertEquals(buf.toString(), "");
    }

    @Test
    public void test_print_nulls() {

        SettingsParser pp = SettingsParser.SENSITIVE;
        pp.print(null, null);
    }

    @Test
    public void test_parse_changeStyle_sensitive() {

        SettingsParser pp = SettingsParser.SENSITIVE;
        int result = pp.parse(this.parseContext, "a", 0);
        assertEquals(result, 0);
        assertEquals(this.parseContext.isCaseSensitive(), true);
    }

    @Test
    public void test_parse_changeStyle_insensitive() {

        SettingsParser pp = SettingsParser.INSENSITIVE;
        int result = pp.parse(this.parseContext, "a", 0);
        assertEquals(result, 0);
        assertEquals(this.parseContext.isCaseSensitive(), false);
    }

    @Test
    public void test_parse_changeStyle_strict() {

        SettingsParser pp = SettingsParser.STRICT;
        int result = pp.parse(this.parseContext, "a", 0);
        assertEquals(result, 0);
        assertEquals(this.parseContext.isStrict(), true);
    }

    @Test
    public void test_parse_changeStyle_lenient() {

        SettingsParser pp = SettingsParser.LENIENT;
        int result = pp.parse(this.parseContext, "a", 0);
        assertEquals(result, 0);
        assertEquals(this.parseContext.isStrict(), false);
    }

    @Test
    public void test_toString_sensitive() {

        SettingsParser pp = SettingsParser.SENSITIVE;
        assertEquals(pp.toString(), "ParseCaseSensitive(true)");
    }

    @Test
    public void test_toString_insensitive() {

        SettingsParser pp = SettingsParser.INSENSITIVE;
        assertEquals(pp.toString(), "ParseCaseSensitive(false)");
    }

    @Test
    public void test_toString_strict() {

        SettingsParser pp = SettingsParser.STRICT;
        assertEquals(pp.toString(), "ParseStrict(true)");
    }

    @Test
    public void test_toString_lenient() {

        SettingsParser pp = SettingsParser.LENIENT;
        assertEquals(pp.toString(), "ParseStrict(false)");
    }

}
