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

import org.junit.Test;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.StringLiteralPrinterParser;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

public class TestStringLiteralParser extends AbstractTestPrinterParser {

    Object[][] data_success() {

        return new Object[][] {
        // match
        { new StringLiteralPrinterParser("hello"), true, "hello", 0, 5 },
        { new StringLiteralPrinterParser("hello"), true, "helloOTHER", 0, 5 },
        { new StringLiteralPrinterParser("hello"), true, "OTHERhelloOTHER", 5, 10 },
        { new StringLiteralPrinterParser("hello"), true, "OTHERhello", 5, 10 },

        // no match
        { new StringLiteralPrinterParser("hello"), true, "", 0, ~0 },
        { new StringLiteralPrinterParser("hello"), true, "a", 1, ~1 },
        { new StringLiteralPrinterParser("hello"), true, "HELLO", 0, ~0 },
        { new StringLiteralPrinterParser("hello"), true, "hlloo", 0, ~0 },
        { new StringLiteralPrinterParser("hello"), true, "OTHERhllooOTHER", 5, ~5 },
        { new StringLiteralPrinterParser("hello"), true, "OTHERhlloo", 5, ~5 },
        { new StringLiteralPrinterParser("hello"), true, "h", 0, ~0 },
        { new StringLiteralPrinterParser("hello"), true, "OTHERh", 5, ~5 },

        // case insensitive
        { new StringLiteralPrinterParser("hello"), false, "hello", 0, 5 },
        { new StringLiteralPrinterParser("hello"), false, "HELLO", 0, 5 },
        { new StringLiteralPrinterParser("hello"), false, "HelLo", 0, 5 },
        { new StringLiteralPrinterParser("hello"), false, "HelLO", 0, 5 }, };
    }

    @Test
    public void test_parse_success() {

        for (Object[] data : data_success()) {
            StringLiteralPrinterParser pp = (StringLiteralPrinterParser) data[0];
            boolean caseSensitive = (boolean) data[1];
            String text = (String) data[2];
            int pos = (int) data[3];
            int expectedPos = (int) data[4];

            this.parseContext.setCaseSensitive(caseSensitive);
            int result = pp.parse(this.parseContext, text, pos);
            assertEquals(result, expectedPos);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.chronology()), null);
            assertEquals(this.parseContext.toParsed().query(TTemporalQueries.zoneId()), null);
        }
    }

    Object[][] data_error() {

        return new Object[][] {
        { new StringLiteralPrinterParser("hello"), "hello", -1, IndexOutOfBoundsException.class },
        { new StringLiteralPrinterParser("hello"), "hello", 6, IndexOutOfBoundsException.class }, };
    }

    @Test
    public void test_parse_error() {

        for (Object[] data : data_error()) {
            StringLiteralPrinterParser pp = (StringLiteralPrinterParser) data[0];
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

}
