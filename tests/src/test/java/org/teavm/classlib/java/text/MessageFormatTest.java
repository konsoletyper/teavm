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

package org.teavm.classlib.java.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class MessageFormatTest {
    private MessageFormat format1;
    private MessageFormat format2;
    private MessageFormat format3;
    private Locale defaultLocale;

    public MessageFormatTest() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        // test with repeating formats and max argument index < max offset
        String pattern = "A {3, number, currency} B {2, time} C {0, number, percent} D {4}  "
                + "E {1,choice,0#off|1#on} F {0, date}";
        format1 = new MessageFormat(pattern);

        // test with max argument index > max offset
        pattern = "A {3, number, currency} B {8, time} C {0, number, percent} D {6}  "
                + "E {1,choice,0#off|1#on} F {0, date}";
        format2 = new MessageFormat(pattern);

        // test with argument number being zero
        pattern = "A B C D E F";
        format3 = new MessageFormat(pattern);
    }

    @Test
    public void constructorLjava_lang_StringLjava_util_Locale() {
        // Test for method java.text.MessageFormat(java.lang.String,
        // java.util.Locale)
        Locale mk = new Locale("mk", "MK");
        MessageFormat format = new MessageFormat(
                "Date: {0,date} Currency: {1, number, currency} Integer: {2, number, integer}",
                mk);

        assertTrue(format.getLocale().equals(mk), "Wrong locale1");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance(DateFormat.DEFAULT, mk)), "Wrong locale2");
        assertTrue(format.getFormats()[1].equals(NumberFormat.getCurrencyInstance(mk)), "Wrong locale3");
        assertTrue(format.getFormats()[2].equals(NumberFormat.getIntegerInstance(mk)), "Wrong locale4");
    }

    @Test
    public void constructorLjava_lang_String() {
        // Test for method java.text.MessageFormat(java.lang.String)
        MessageFormat format = new MessageFormat(
                "abc {4,time} def {3,date} ghi {2,number} jkl {1,choice,0#low|1#high} mnop {0}");
        assertTrue(format.getClass() == MessageFormat.class, "Not a MessageFormat");
        Format[] formats = format.getFormats();
        assertNotNull(formats, "null formats");
        assertTrue(formats.length >= 5, "Wrong format count: " + formats.length);
        assertTrue(formats[0].equals(DateFormat.getTimeInstance()), "Wrong time format");
        assertTrue(formats[1].equals(DateFormat.getDateInstance()), "Wrong date format");
        assertTrue(formats[2].equals(NumberFormat.getInstance()), "Wrong number format");
        assertTrue(formats[3].equals(new ChoiceFormat("0.0#low|1.0#high")), "Wrong choice format");
        assertNull(formats[4], "Wrong string format");

        Date date = new Date();
        FieldPosition pos = new FieldPosition(-1);
        StringBuffer buffer = new StringBuffer();
        format.format(new Object[] { "123", 1.6, 7.2, date, date }, buffer, pos);
        String result = buffer.toString();
        buffer.setLength(0);
        buffer.append("abc ");
        buffer.append(DateFormat.getTimeInstance().format(date));
        buffer.append(" def ");
        buffer.append(DateFormat.getDateInstance().format(date));
        buffer.append(" ghi ");
        buffer.append(NumberFormat.getInstance().format(7.2));
        buffer.append(" jkl high mnop 123");
        assertTrue(result.equals(buffer.toString()), "Wrong answer:\n" + result + "\n" + buffer);

        assertEquals("Test message", new MessageFormat("Test message").format(new Object[0]), "Simple string");

        result = new MessageFormat("Don't").format(new Object[0]);
        assertTrue("Dont".equals(result), "Should not throw IllegalArgumentException: " + result);

        try {
            new MessageFormat("Invalid {1,foobar} format descriptor!");
            fail("Expected test_ConstructorLjava_lang_String to throw IAE.");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            new MessageFormat("Invalid {1,date,invalid-spec} format descriptor!");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        
        // Regression for HARMONY-65
        try {
            new MessageFormat("{0,number,integer");
            fail("Assert 0: Failed to detect unmatched brackets.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void applyPatternLjava_lang_String() {
        MessageFormat format = new MessageFormat("test");
        format.applyPattern("xx {0}");
        assertEquals("xx 46", format.format(new Object[] { 46 }), "Invalid number");
        Date date = new Date();
        String result = format.format(new Object[] { date });
        String expected = "xx " + DateFormat.getInstance().format(date);
        assertTrue(result.equals(expected), "Invalid date:\n" + result + "\n" + expected);
        format = new MessageFormat("{0,date}{1,time}{2,number,integer}");
        format.applyPattern("nothing");
        assertEquals("nothing", format.toPattern(), "Found formats");

        format.applyPattern("{0}");
        assertNull(format.getFormats()[0], "Wrong format");
        assertEquals("{0}", format.toPattern(), "Wrong pattern");

        format.applyPattern("{0, \t\u001ftime }");
        assertTrue(format.getFormats()[0].equals(DateFormat.getTimeInstance()), "Wrong time format");
        assertEquals("{0,time}", format.toPattern(), "Wrong time pattern");
        format.applyPattern("{0,Time, Short\n}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getTimeInstance(DateFormat.SHORT)),
                "Wrong short time format");
        assertEquals("{0,time,short}", format.toPattern(), "Wrong short time pattern");
        format.applyPattern("{0,TIME,\nmedium  }");
        assertTrue(format.getFormats()[0].equals(DateFormat.getTimeInstance(DateFormat.MEDIUM)),
                "Wrong medium time format");
        assertEquals("{0,time}", format.toPattern(), "Wrong medium time pattern");
        format.applyPattern("{0,time,LONG}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getTimeInstance(DateFormat.LONG)),
                "Wrong long time format");
        assertEquals("{0,time,long}", format.toPattern(), "Wrong long time pattern");

        format.applyPattern("{0, date}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance()), "Wrong date format");
        assertEquals("{0,date}", format.toPattern(), "Wrong date pattern");
        format.applyPattern("{0, date, short}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance(DateFormat.SHORT)),
                "Wrong short date format");
        assertEquals("{0,date,short}", format.toPattern(), "Wrong short date pattern");
        format.applyPattern("{0, date, medium}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance(DateFormat.MEDIUM)),
                "Wrong medium date format");
        assertEquals("{0,date}", format.toPattern(), "Wrong medium date pattern");
        format.applyPattern("{0, date, long}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance(DateFormat.LONG)),
                "Wrong long date format");
        assertEquals("{0,date,long}", format.toPattern(), "Wrong long date pattern");
        format.applyPattern("{0, date, full}");
        assertTrue(format.getFormats()[0].equals(DateFormat.getDateInstance(DateFormat.FULL)),
                "Wrong full date format");
        assertEquals("{0,date,full}", format.toPattern(), "Wrong full date pattern");

        format.applyPattern("{0, date, MMM d {hh:mm:ss}}");
        assertEquals(" MMM d {hh:mm:ss}", ((SimpleDateFormat) (format.getFormats()[0])).toPattern(),
                "Wrong time/date format");
        //assertEquals("Wrong time/date pattern", "{0,date, MMM d {hh:mm:ss}}", format.toPattern());

        format.applyPattern("{0, number}");
        assertTrue(format.getFormats()[0].equals(NumberFormat.getNumberInstance()), "Wrong number format");
        assertEquals("{0,number}",  format.toPattern(), "Wrong number pattern");
        format.applyPattern("{0, number, currency}");
        assertTrue(format.getFormats()[0].equals(NumberFormat.getCurrencyInstance()), "Wrong currency number format");
        assertEquals("{0,number,currency}", format.toPattern(), "Wrong currency number pattern");
        format.applyPattern("{0, number, percent}");
        assertTrue(format.getFormats()[0].equals(NumberFormat.getPercentInstance()), "Wrong percent number format");
        assertEquals("{0,number,percent}", format.toPattern(), "Wrong percent number pattern");
        format.applyPattern("{0, number, integer}");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setParseIntegerOnly(true);
        assertTrue(format.getFormats()[0].equals(nf), "Wrong integer number format");
        assertEquals("{0,number,integer}", format.toPattern(), "Wrong integer number pattern");

        format.applyPattern("{0, number, {'#'}##0.0E0}");

        /*
         * TODO validate these assertions 
         * String actual = ((DecimalFormat)(format.getFormats()[0])).toPattern(); 
         * assertEquals("Wrong pattern number format", "' {#}'##0.0E0", actual); 
         * assertEquals("Wrong pattern number pattern", "{0,number,' {#}'##0.0E0}", format.toPattern());
         * 
         */

        format.applyPattern("{0, choice,0#no|1#one|2#{1,number}}");
        assertEquals("0.0#no|1.0#one|2.0#{1,number}", ((ChoiceFormat) format.getFormats()[0]).toPattern(),
                "Wrong choice format");
        //assertEquals("Wrong choice pattern", "{0,choice,0.0#no|1.0#one|2.0#{1,number}}", format.toPattern());
        assertEquals("3.6", format.format(new Object[] { 2, 3.6f }), "Wrong formatted choice");

        try {
            format.applyPattern("WRONG MESSAGE FORMAT {0,number,{}");
            fail("Expected IllegalArgumentException for invalid pattern");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        // Regression for HARMONY-65
        MessageFormat mf = new MessageFormat("{0,number,integer}");
        String badpattern = "{0,number,#";
        try {
            mf.applyPattern(badpattern);
            fail("Assert 0: Failed to detect unmatched brackets.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void test_clone() {
        MessageFormat format = new MessageFormat("'{'choice'}'{0}");
        MessageFormat clone = (MessageFormat) format.clone();
        assertTrue(format.equals(clone), "Clone not equal");
        assertEquals("{choice}{0}", format.format(new Object[] {}), "Wrong answer");
        clone.setFormat(0, DateFormat.getInstance());
        assertTrue(!format.equals(clone), "Clone shares format data");
        format = (MessageFormat) clone.clone();
        Format[] formats = clone.getFormats();
        ((SimpleDateFormat) formats[0]).applyPattern("adk123");
        assertTrue(!format.equals(clone), "Clone shares format data");
    }

    @Test
    public void test_equalsLjava_lang_Object() {
        MessageFormat format1 = new MessageFormat("{0}");
        MessageFormat format2 = new MessageFormat("{1}");
        assertTrue(!format1.equals(format2), "Should not be equal");
        format2.applyPattern("{0}");
        assertTrue(format1.equals(format2), "Should be equal");
        SimpleDateFormat date = (SimpleDateFormat) DateFormat.getTimeInstance();
        format1.setFormat(0, DateFormat.getTimeInstance());
        format2.setFormat(0, new SimpleDateFormat(date.toPattern()));
        assertTrue(format1.equals(format2), "Should be equal2");
    }

    @Test
    public void test_hashCode() {
        assertEquals(3648, new MessageFormat("rr", null).hashCode(), "Should be equal");
    }

    @Test
    public void format$Ljava_lang_ObjectLjava_lang_StringBufferLjava_text_FieldPosition() {
        MessageFormat format = new MessageFormat("{1,number,integer}");
        StringBuffer buffer = new StringBuffer();
        format.format(new Object[] { "0", 53.863 }, buffer, new FieldPosition(0));
        assertEquals("54", buffer.toString(), "Wrong result");
        format.applyPattern("{0,choice,0#zero|1#one '{1,choice,2#two {2,time}}'}");
        Date date = new Date();
        String expected = "one two " + DateFormat.getTimeInstance().format(date);
        String result = format.format(new Object[] { 1.6, 3, date });
        assertTrue(expected.equals(result), "Choice not recursive:\n" + expected + "\n" + result);
    }

    @Test
    public void getFormats() {
        // test with repeating formats and max argument index < max offset
        Format[] formats = format1.getFormats();
        Format[] correctFormats = new Format[] {
                NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(),
                NumberFormat.getPercentInstance(), null,
                new ChoiceFormat("0#off|1#on"), DateFormat.getDateInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test1:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1:wrong format for pattern index " + i + ":");
        }

        // test with max argument index > max offset
        formats = format2.getFormats();
        correctFormats = new Format[] { NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(),
                NumberFormat.getPercentInstance(), null,
                new ChoiceFormat("0#off|1#on"), DateFormat.getDateInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test2:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2:wrong format for pattern index " + i + ":");
        }

        // test with argument number being zero
        formats = format3.getFormats();
        assertEquals(0, formats.length, "Test3: Returned wrong number of formats:");
    }

    @Test
    public void getFormatsByArgumentIndex() {
        // test with repeating formats and max argument index < max offset
        Format[] formats = format1.getFormatsByArgumentIndex();
        Format[] correctFormats = new Format[] { DateFormat.getDateInstance(),
                new ChoiceFormat("0#off|1#on"), DateFormat.getTimeInstance(),
                NumberFormat.getCurrencyInstance(), null };

        assertEquals(correctFormats.length, formats.length, "Test1:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1:wrong format for argument index " + i + ":");
        }

        // test with max argument index > max offset
        formats = format2.getFormatsByArgumentIndex();
        correctFormats = new Format[] { DateFormat.getDateInstance(),
                new ChoiceFormat("0#off|1#on"), null,
                NumberFormat.getCurrencyInstance(), null, null, null, null,
                DateFormat.getTimeInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test2:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2:wrong format for argument index " + i + ":");
        }

        // test with argument number being zero
        formats = format3.getFormatsByArgumentIndex();
        assertEquals(0, formats.length, "Test3: Returned wrong number of formats:");
    }

    @Test
    public void setFormatByArgumentIndexILjava_text_Format() {
        MessageFormat f1 = (MessageFormat) format1.clone();
        f1.setFormatByArgumentIndex(0, DateFormat.getTimeInstance());
        f1.setFormatByArgumentIndex(4, new ChoiceFormat("1#few|2#ok|3#a lot"));

        // test with repeating formats and max argument index < max offset
        // compare getFormatsByArgumentIndex() results after calls to
        // setFormatByArgumentIndex()
        Format[] formats = f1.getFormatsByArgumentIndex();

        Format[] correctFormats = new Format[] { DateFormat.getTimeInstance(),
                new ChoiceFormat("0#off|1#on"), DateFormat.getTimeInstance(),
                NumberFormat.getCurrencyInstance(),
                new ChoiceFormat("1#few|2#ok|3#a lot")
        };

        assertEquals(correctFormats.length, formats.length, "Test1A:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1B:wrong format for argument index " + i + ":");
        }

        // compare getFormats() results after calls to
        // setFormatByArgumentIndex()
        formats = f1.getFormats();

        correctFormats = new Format[] { NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(), DateFormat.getTimeInstance(),
                new ChoiceFormat("1#few|2#ok|3#a lot"),
                new ChoiceFormat("0#off|1#on"), DateFormat.getTimeInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test1C:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1D:wrong format for pattern index " + i + ":");
        }

        // test setting argumentIndexes that are not used
        MessageFormat f2 = (MessageFormat) format2.clone();
        f2.setFormatByArgumentIndex(2, NumberFormat.getPercentInstance());
        f2.setFormatByArgumentIndex(4, DateFormat.getTimeInstance());

        formats = f2.getFormatsByArgumentIndex();
        correctFormats = format2.getFormatsByArgumentIndex();

        assertEquals(correctFormats.length, formats.length, "Test2A:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2B:wrong format for argument index " + i + ":");
        }

        formats = f2.getFormats();
        correctFormats = format2.getFormats();

        assertEquals(correctFormats.length, formats.length, "Test2C:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2D:wrong format for pattern index " + i + ":");
        }

        // test exceeding the argumentIndex number
        MessageFormat f3 = (MessageFormat) format3.clone();
        f3.setFormatByArgumentIndex(1, NumberFormat.getCurrencyInstance());

        formats = f3.getFormatsByArgumentIndex();
        assertEquals(0, formats.length, "Test3A:Returned wrong number of formats:");

        formats = f3.getFormats();
        assertEquals(0, formats.length, "Test3B:Returned wrong number of formats:");
    }

    @Test
    public void setFormatsByArgumentIndex$Ljava_text_Format() {
        MessageFormat f1 = (MessageFormat) format1.clone();

        // test with repeating formats and max argument index < max offset
        // compare getFormatsByArgumentIndex() results after calls to
        // setFormatsByArgumentIndex(Format[])
        Format[] correctFormats = new Format[] { DateFormat.getTimeInstance(),
                new ChoiceFormat("0#off|1#on"), DateFormat.getTimeInstance(),
                NumberFormat.getCurrencyInstance(),
                new ChoiceFormat("1#few|2#ok|3#a lot")
        };

        f1.setFormatsByArgumentIndex(correctFormats);
        Format[] formats = f1.getFormatsByArgumentIndex();

        assertEquals(correctFormats.length, formats.length, "Test1A:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1B:wrong format for argument index " + i + ":");
        }

        // compare getFormats() results after calls to
        // setFormatByArgumentIndex()
        formats = f1.getFormats();
        correctFormats = new Format[] { NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(), DateFormat.getTimeInstance(),
                new ChoiceFormat("1#few|2#ok|3#a lot"),
                new ChoiceFormat("0#off|1#on"), DateFormat.getTimeInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test1C:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test1D:wrong format for pattern index " + i + ":");
        }

        // test setting argumentIndexes that are not used
        MessageFormat f2 = (MessageFormat) format2.clone();
        Format[] inputFormats = new Format[] { DateFormat.getDateInstance(),
                new ChoiceFormat("0#off|1#on"),
                NumberFormat.getPercentInstance(),
                NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(), null, null, null,
                DateFormat.getTimeInstance()
        };
        f2.setFormatsByArgumentIndex(inputFormats);

        formats = f2.getFormatsByArgumentIndex();
        correctFormats = format2.getFormatsByArgumentIndex();

        assertEquals(correctFormats.length, formats.length, "Test2A:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2B:wrong format for argument index " + i + ":");
        }

        formats = f2.getFormats();
        correctFormats = new Format[] { NumberFormat.getCurrencyInstance(),
                DateFormat.getTimeInstance(), DateFormat.getDateInstance(),
                null, new ChoiceFormat("0#off|1#on"),
                DateFormat.getDateInstance()
        };

        assertEquals(correctFormats.length, formats.length, "Test2C:Returned wrong number of formats:");
        for (int i = 0; i < correctFormats.length; i++) {
            assertEquals(correctFormats[i], formats[i], "Test2D:wrong format for pattern index " + i + ":");
        }

        // test exceeding the argumentIndex number
        MessageFormat f3 = (MessageFormat) format3.clone();
        f3.setFormatsByArgumentIndex(inputFormats);

        formats = f3.getFormatsByArgumentIndex();
        assertEquals(0, formats.length, "Test3A:Returned wrong number of formats:");

        formats = f3.getFormats();
        assertEquals(0, formats.length, "Test3B:Returned wrong number of formats:");
    }

    @Test
    public void parseLjava_lang_StringLjava_text_ParsePosition() {
        MessageFormat format = new MessageFormat("date is {0,date,MMM d, yyyy}");
        ParsePosition pos = new ParsePosition(2);
        Object[] result = format.parse("xxdate is Feb 28, 1999", pos);
        assertTrue(result.length >= 1, "No result: " + result.length);
        assertTrue(result[0].equals(new GregorianCalendar(1999, Calendar.FEBRUARY, 28).getTime()), "Wrong answer");

        MessageFormat mf = new MessageFormat("vm={0},{1},{2}");
        result = mf.parse("vm=win,foo,bar", new ParsePosition(0));
        assertTrue(result[0].equals("win") && result[1].equals("foo") && result[2].equals("bar"), "Invalid parse");

        mf = new MessageFormat("{0}; {0}; {0}");
        String parse = "a; b; c";
        result = mf.parse(parse, new ParsePosition(0));
        assertEquals("c", result[0], "Wrong variable result");

        mf = new MessageFormat("before {0}, after {1,number}");
        parse = "before you, after 42";
        pos.setIndex(0);
        pos.setErrorIndex(8);
        result = mf.parse(parse, pos);
        assertEquals(2, result.length);
    }

    @Test
    public void setLocaleLjava_util_Locale() {
        MessageFormat format = new MessageFormat("date {0,date}");
        format.setLocale(Locale.CHINA);
        assertEquals(Locale.CHINA, format.getLocale(), "Wrong locale1");
        format.applyPattern("{1,date}");
        assertEquals(DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.CHINA), format.getFormats()[0],
                "Wrong locale3");
    }

    @Test
    public void toPattern() {
        String pattern = "[{0}]";
        MessageFormat mf = new MessageFormat(pattern);
        assertTrue(mf.toPattern().equals(pattern), "Wrong pattern");
        
        // Regression for HARMONY-59
        new MessageFormat("CHOICE {1,choice}").toPattern();
    }

    @AfterEach
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }
    
    @Test
    public void constructorLjava_util_Locale() {
        // Regression for HARMONY-65
        try {
            new MessageFormat("{0,number,integer", Locale.US);
            fail("Assert 0: Failed to detect unmatched brackets.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void parse() throws ParseException {
        // Regression for HARMONY-63
        MessageFormat mf = new MessageFormat("{0,number,#,####}", Locale.US);
        Object[] res = mf.parse("1,00,00");
        assertEquals(1, res.length, "Assert 0: incorrect size of parsed data ");
        assertEquals(10000L, res[0], "Assert 1: parsed value incorrectly");
    }

    @Test
    public void format_Object() {
        // Regression for HARMONY-1875
        Locale.setDefault(Locale.CANADA); 
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); 
        String pat = "text here {0, date, yyyyyyyyy } and here";
        String etalon = "text here  000002007  and here";
        MessageFormat obj = new MessageFormat(pat); 
        assertEquals(etalon, obj.format(new Object[] { new Date(1198141737640L) }));
        
        assertEquals("{0}", MessageFormat.format("{0}", (Object[]) null));
        assertEquals("nullABC", MessageFormat.format("{0}{1}", new String[]{null, "ABC"}));
    } 

    @Test
    public void testHARMONY5323() { 
        Object[] messageArgs = new Object[11];
        for (int i = 0; i < messageArgs.length; i++) {
            messageArgs[i] = "dumb" + i;
        }

        String res = MessageFormat.format("bgcolor=\"{10}\"", messageArgs);
        assertEquals(res, "bgcolor=\"dumb10\"");
    } 
}
