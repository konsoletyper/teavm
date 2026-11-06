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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class StringWriterTest {
    StringWriter sw = new StringWriter();

    @Test
    public void close() {
        try {
            sw.close();
        } catch (IOException e) {
            fail("IOException closing StringWriter : " + e.getMessage());
        }
    }

    @Test
    public void flush() {
        sw.flush();
        sw.write('c');
        assertEquals("c", sw.toString(), "Failed to flush char");
    }

    @Test
    public void getBuffer() {
        sw.write("This is a test string");
        StringBuffer sb = sw.getBuffer();
        assertEquals("This is a test string", sb.toString(), "Incorrect buffer returned");
    }

    @Test
    public void toStringWorks() {
        sw.write("This is a test string");
        assertEquals("This is a test string", sw.toString(), "Incorrect string returned");
    }

    @Test
    public void write$CII() {
        char[] c = new char[1000];
        "This is a test string".getChars(0, 21, c, 0);
        sw.write(c, 0, 21);
        assertEquals("This is a test string", sw.toString(), "Chars not written properly");
    }

    @Test
    public void write$CII_2() {
        StringWriter obj;
        try {
            obj = new StringWriter();
            obj.write(new char[0], 0, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException t) {
            assertEquals(IndexOutOfBoundsException.class, t.getClass(),
                    "IndexOutOfBoundsException rather than a subclass expected");
        }
    }

    @Test
    public void write$CII_3() {
        StringWriter obj;
        try {
            obj = new StringWriter();
            obj.write(new char[0], -1, 0);
            fail("IndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException t) {
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException t) {
            // Do nothing
        }
    }

    @Test
    public void test_write$CII_4() {
        StringWriter obj;
        try {
            obj = new StringWriter();
            obj.write(new char[0], -1, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException t) {
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException t) {
            // Do nothing
        }
    }

    @Test
    public void writeI() {
        sw.write('c');
        assertEquals("c", sw.toString(), "Char not written properly");
    }

    @Test
    public void writeLjava_lang_String() {
        sw.write("This is a test string");
        assertEquals("This is a test string", sw.toString(), "String not written properly");
    }

    @Test
    public void writeLjava_lang_StringII() {
        sw.write("This is a test string", 2, 2);
        assertEquals("is", sw.toString(), "String not written properly");
    }
    
    @Test
    public void appendChar() throws IOException {
        char testChar = ' ';
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testChar);
        assertEquals(String.valueOf(testChar), stringWriter.toString());
        stringWriter.close();
    }

    @Test
    public void appendCharSequence() throws IOException {
        String testString = "My Test String";
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testString);
        assertEquals(String.valueOf(testString), stringWriter.toString());
        stringWriter.close();
    }

    @Test
    public void appendCharSequenceIntInt() throws IOException {
        String testString = "My Test String";
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testString, 1, 3);
        assertEquals(testString.substring(1, 3), stringWriter.toString());
        stringWriter.close();
    }
}
