/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.text;

import static org.junit.Assert.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class DecimalFormatParseTest {
    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);

    @Test
    public void parsesNumber() throws ParseException {
        DecimalFormat format = createFormat("#,#00.#");
        assertEquals(2L, format.parse("2"));
        assertEquals(23L, format.parse("23"));
        assertEquals(23L, format.parse("23.0"));
        assertEquals(2300L, format.parse("2,3,0,0"));
        assertEquals(23.1, format.parse("23.1"));
    }

    @Test
    public void parsesLargeValue() throws ParseException {
        DecimalFormat format = createFormat("#,#00.#");
        assertEquals(9223372036854775807L, format.parse("9223372036854775807"));
        assertEquals(99E18, format.parse("99000000000000000000"));
        assertEquals(3.333333333333333E20, format.parse("333333333333333333456").doubleValue(), 1000000);
        assertEquals(10E20, format.parse("999999999999999999999").doubleValue(), 1000000);
    }

    @Test
    public void parsesExponential() throws ParseException {
        DecimalFormat format = createFormat("0.#E0");
        assertEquals(23L, format.parse("2.3E1"));
        assertEquals(23L, format.parse("2300E-2"));
        assertEquals(0.23, format.parse("2300E-4").doubleValue(), 0.0001);
        assertEquals(99E18, format.parse("99E18"));
    }

    @Test
    public void parsesPrefixSuffix() throws ParseException {
        DecimalFormat format = createFormat("[0.#E0]");
        assertEquals(23L, format.parse("[23]"));
        assertEquals(-23L, format.parse("-[23]"));
        try {
            format.parse("23");
            fail("Exception expected as there aren't neither prefix nor suffix");
        } catch (ParseException e) {
            assertEquals(0, e.getErrorOffset());
        }
        try {
            format.parse("[23");
            fail("Exception expected as there is no suffix");
        } catch (ParseException e) {
            assertEquals(3, e.getErrorOffset());
        }
    }

    private DecimalFormat createFormat(String format) {
        return new DecimalFormat(format, symbols);
    }
}
