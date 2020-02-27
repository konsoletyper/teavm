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

package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class CharArrayWriterTest {
    char[] hw = { 'H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd' };
    CharArrayWriter cw = new CharArrayWriter();
    CharArrayReader cr;

    @Test
    public void constructor() {
        cw = new CharArrayWriter(90);
        assertEquals("Created incorrect writer", 0, cw.size());
    }

    @Test
    public void constructorI() {
        cw = new CharArrayWriter();
        assertEquals("Created incorrect writer", 0, cw.size());
    }

    @Test
    public void close() {
        cw.close();
    }

    @Test
    public void flush() {
        cw.flush();
    }

    @Test
    public void reset() throws IOException {
        cw.write("HelloWorld", 5, 5);
        cw.reset();
        cw.write("HelloWorld", 0, 5);
        cr = new CharArrayReader(cw.toCharArray());
        char[] c = new char[100];
        cr.read(c, 0, 5);
        assertEquals("Reset failed to reset buffer", "Hello", new String(c, 0, 5));
    }

    @Test
    public void size() {
        assertEquals("Returned incorrect size", 0, cw.size());
        cw.write(hw, 5, 5);
        assertEquals("Returned incorrect size", 5, cw.size());
    }

    @Test
    public void toCharArray() throws IOException {
        cw.write("HelloWorld", 0, 10);
        cr = new CharArrayReader(cw.toCharArray());
        char[] c = new char[100];
        cr.read(c, 0, 10);
        assertEquals("toCharArray failed to return correct array", "HelloWorld", new String(c, 0, 10));
    }

    @Test
    public void test_toString() {
        cw.write("HelloWorld", 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        assertEquals("Returned incorrect string", "World", cw.toString());
    }

    @Test
    public void write$CII() throws IOException {
        cw.write(hw, 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        char[] c = new char[100];
        cr.read(c, 0, 5);
        assertEquals("Writer failed to write correct chars", "World", new String(c, 0, 5));
    }

    @Test
    public void write$CII_2() {
        // Regression for HARMONY-387
        CharArrayWriter obj = new CharArrayWriter();
        try {
            obj.write(new char[] { '0' }, 0, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException t) {
            assertEquals(
                    "IndexOutOfBoundsException rather than a subclass expected",
                    IndexOutOfBoundsException.class, t.getClass());
        }
    }

    @Test
    public void writeI() throws IOException {
        cw.write('T');
        cr = new CharArrayReader(cw.toCharArray());
        assertEquals("Writer failed to write char", 'T', cr.read());
    }

    @Test
    public void writeLjava_lang_StringII() throws IOException {
        cw.write("HelloWorld", 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        char[] c = new char[100];
        cr.read(c, 0, 5);
        assertEquals("Writer failed to write correct chars", "World", new String(c, 0, 5));
    }

    @Test
    public void writeLjava_lang_StringII_2() throws StringIndexOutOfBoundsException {
        // Regression for HARMONY-387
        CharArrayWriter obj = new CharArrayWriter();
        try {
            obj.write((String) null, -1, 0);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
            // Expected
        }
    }

    @Test
    public void writeToLjava_io_Writer() throws IOException {
        cw.write("HelloWorld", 0, 10);
        StringWriter sw = new StringWriter();
        cw.writeTo(sw);
        assertEquals("Writer failed to write correct chars", "HelloWorld", sw.toString());
    }

    @Test
    public void appendChar() {
        char testChar = ' ';
        CharArrayWriter writer = new CharArrayWriter(10);
        writer.append(testChar);
        writer.flush();
        assertEquals(String.valueOf(testChar), writer.toString());
        writer.close();
    }

    @Test
    public void appendCharSequence() {
        String testString = "My Test String";
        CharArrayWriter writer = new CharArrayWriter(10);
        writer.append(testString);
        writer.flush();
        assertEquals(testString, writer.toString());
        writer.close();
    }

    @Test
    public void test_appendCharSequenceIntInt() {
        String testString = "My Test String";
        CharArrayWriter writer = new CharArrayWriter(10);
        writer.append(testString, 1, 3);
        writer.flush();
        assertEquals(testString.substring(1, 3), writer.toString());
        writer.close();
    }
}
