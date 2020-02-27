/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formattable;
import java.util.Formatter;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.Locale;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class FormatterTest {
    @Test(expected = UnknownFormatConversionException.class)
    public void unexpectedEndOfFormatString() {
        new Formatter().format("%1", "foo");
    }

    @Test(expected = DuplicateFormatFlagsException.class)
    public void duplicateFlag() {
        new Formatter().format("%--s", "q");
    }

    @Test(expected = UnknownFormatConversionException.class)
    public void noPrecisionAfterDot() {
        new Formatter().format("%1.s", "q");
    }

    @Test
    public void bothPreviousModifierAndArgumentIndexPresent() {
        String result = new Formatter().format("%s %2$<s", "q", "w").toString();
        assertEquals("q q", result);
    }

    @Test
    public void formatsBoolean() {
        assertEquals("true", new Formatter().format("%b", true).toString());
        assertEquals("false", new Formatter().format("%b", false).toString());

        assertEquals("true", new Formatter().format("%b", new Object()).toString());
        assertEquals("false", new Formatter().format("%b", null).toString());

        assertEquals("  true", new Formatter().format("%6b", true).toString());
        assertEquals("true  ", new Formatter().format("%-6b", true).toString());
        assertEquals("true", new Formatter().format("%2b", true).toString());
        assertEquals("tr", new Formatter().format("%2.2b", true).toString());
        assertEquals("  tr", new Formatter().format("%4.2b", true).toString());
        assertEquals("TRUE", new Formatter().format("%B", true).toString());

        try {
            new Formatter().format("%+b", true);
            fail("Should have thrown exception");
        } catch (FormatFlagsConversionMismatchException e) {
            assertEquals("+", e.getFlags());
            assertEquals('b', e.getConversion());
        }
    }

    @Test
    public void formatsString() {
        assertEquals("23 foo", new Formatter().format("%s %s", 23, "foo").toString());
        assertEquals("0:-1:-1", new Formatter().format("%s", new A()).toString());
        assertEquals("0:2:-1", new Formatter().format("%2s", new A()).toString());
        assertEquals("0:2:3", new Formatter().format("%2.3s", new A()).toString());
        assertEquals("1:3:-1", new Formatter().format("%-3s", new A()).toString());
    }

    static class A implements Formattable {
        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            formatter.format("%s", flags + ":" + width + ":" + precision);
        }
    }

    @Test
    public void formatsHashCode() {
        assertEquals("18cc6 17C13", new Formatter().format("%h %H", "foo", "bar").toString());
    }

    @Test
    public void respectsFormatArgumentOrder() {
        String result = new Formatter().format("%s %s %<s %1$s %<s %s %1$s %s %<s", "a", "b", "c", "d").toString();
        assertEquals("a b b a a c a d d", result);
    }

    @Test
    public void formatsChar() {
        assertEquals("x:  Y:\uDBFF\uDFFF ", new Formatter().format("%c:%3C:%-3c", 'x', 'y', 0x10ffff).toString());

        try {
            new Formatter().format("%c", Integer.MAX_VALUE);
            fail("IllegalFormatCodePointException expected");
        } catch (IllegalFormatCodePointException e) {
            assertEquals(Integer.MAX_VALUE, e.getCodePoint());
        }

        assertEquals("null", new Formatter().format("%c", new Object[] { null }).toString());

        try {
            new Formatter().format("%C", new A());
            fail("IllegalFormatConversionException expected");
        } catch (IllegalFormatConversionException e) {
            assertEquals(A.class, e.getArgumentClass());
        }

        try {
            new Formatter().format("%3.1c", 'X');
            fail("IllegalFormatPrecisionException expected");
        } catch (IllegalFormatPrecisionException e) {
            assertEquals(1, e.getPrecision());
        }
    }

    @Test
    public void formatsDecimalInteger() {
        assertEquals("1 2 3 4", new Formatter().format("%d %d %d %d", (byte) 1, (short) 2, 3, 4L).toString());

        assertEquals("00023", new Formatter().format("%05d", 23).toString());
        assertEquals("-0023", new Formatter().format("%05d", -23).toString());
        assertEquals("00001,234", new Formatter(Locale.US).format("%0,9d", 1234).toString());
        assertEquals("(001,234)", new Formatter(Locale.US).format("%0,(9d", -1234).toString());

        assertEquals("1 12 123 1,234 12,345 123,456 1,234,567", new Formatter(Locale.US)
                .format("%,d %,d %,d %,d %,d %,d %,d", 1, 12, 123, 1234, 12345, 123456, 1234567).toString());

        assertEquals("  -123:-234  ", new Formatter().format("%6d:%-6d", -123, -234).toString());

        assertEquals("+123 +0123 +0", new Formatter().format("%+d %+05d %+d", 123, 123, 0).toString());

        assertEquals(": 123:-123:", new Formatter().format(":% d:% d:", 123, -123).toString());

        try {
            new Formatter().format("%#d", 23);
            fail("Should have thrown exception 1");
        } catch (FormatFlagsConversionMismatchException e) {
            assertEquals("#", e.getFlags());
        }

        try {
            new Formatter().format("% +d", 23);
            fail("Should have thrown exception 2");
        } catch (IllegalFormatFlagsException e) {
            assertTrue(e.getFlags().contains("+"));
            assertTrue(e.getFlags().contains(" "));
        }

        try {
            new Formatter().format("%-01d", 23);
            fail("Should have thrown exception 3");
        } catch (IllegalFormatFlagsException e) {
            assertTrue(e.getFlags().contains("-"));
            assertTrue(e.getFlags().contains("0"));
        }

        try {
            new Formatter().format("%-d", 23);
            fail("Should have thrown exception 4");
        } catch (MissingFormatWidthException e) {
            assertTrue(e.getFormatSpecifier().contains("d"));
        }

        try {
            new Formatter().format("%1.2d", 23);
            fail("Should have thrown exception 5");
        } catch (IllegalFormatPrecisionException e) {
            assertEquals(2, e.getPrecision());
        }
    }

    @Test
    public void formatsOctalInteger() {
        assertEquals("1 2 3 4", new Formatter().format("%o %o %o %o", (byte) 1, (short) 2, 3, 4L).toString());

        assertEquals("00027", new Formatter().format("%05o", 23).toString());
        assertEquals("0173", new Formatter().format("%#o", 123).toString());

        try {
            new Formatter().format("%-01o", 23);
            fail("Should have thrown exception 1");
        } catch (IllegalFormatFlagsException e) {
            assertTrue(e.getFlags().contains("-"));
            assertTrue(e.getFlags().contains("0"));
        }

        try {
            new Formatter().format("%-o", 23);
            fail("Should have thrown exception 2");
        } catch (MissingFormatWidthException e) {
            assertTrue(e.getFormatSpecifier().contains("o"));
        }

        try {
            new Formatter().format("%1.2o", 23);
            fail("Should have thrown exception 3");
        } catch (IllegalFormatPrecisionException e) {
            assertEquals(2, e.getPrecision());
        }
    }

    @Test
    public void formatsHexInteger() {
        assertEquals("1 2 3 4", new Formatter().format("%x %x %x %x", (byte) 1, (short) 2, 3, 4L).toString());

        assertEquals("00017", new Formatter().format("%05x", 23).toString());
        assertEquals("0x7b", new Formatter().format("%#x", 123).toString());

        try {
            new Formatter().format("%-01x", 23);
            fail("Should have thrown exception 1");
        } catch (IllegalFormatFlagsException e) {
            assertTrue(e.getFlags().contains("-"));
            assertTrue(e.getFlags().contains("0"));
        }

        try {
            new Formatter().format("%-x", 23);
            fail("Should have thrown exception 2");
        } catch (MissingFormatWidthException e) {
            assertTrue(e.getFormatSpecifier().contains("x"));
        }

        try {
            new Formatter().format("%1.2x", 23);
            fail("Should have thrown exception 3");
        } catch (IllegalFormatPrecisionException e) {
            assertEquals(2, e.getPrecision());
        }
    }
}
