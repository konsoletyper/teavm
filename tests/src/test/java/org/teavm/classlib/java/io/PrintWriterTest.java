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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class PrintWriterTest {
    static class Bogus {
        public String toString() {
            return "Bogus";
        }
    }

    PrintWriter pw;

    ByteArrayOutputStream bao;

    BufferedReader br;

    public PrintWriterTest() {
        bao = new ByteArrayOutputStream();
        pw = new PrintWriter(bao, false);
    }

    @Test
    public void constructorLjava_io_OutputStream() {
        String s;
        pw.println("Random Chars");
        pw.write("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            assertEquals("Incorrect string written/read", "Random Chars", s);
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s.equals("Hello World"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    @Test
    public void constructorLjava_io_OutputStreamZ() {
        String s;
        pw = new PrintWriter(bao, true);
        pw.println("Random Chars");
        pw.write("Hello World");
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s.equals("Random Chars"));
            pw.flush();
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s.equals("Random Chars"));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s.equals("Hello World"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    @Test
    public void constructorLjava_io_Writer() {
        StringWriter sw = new StringWriter();
        pw = new PrintWriter(sw);
        pw.print("Hello");
        pw.flush();
        assertEquals("Failed to construct proper writer", "Hello", sw.toString());
    }

    @Test
    public void constructorLjava_io_WriterZ() {
        StringWriter sw = new StringWriter();
        pw = new PrintWriter(sw, true);
        pw.print("Hello");
        // Auto-flush should have happened
        assertEquals("Failed to construct proper writer",  "Hello", sw.toString());
    }

    @Test
    public void checkError() {
        pw.close();
        pw.print(490000000000.08765);
        assertTrue("Failed to return error", pw.checkError());
    }

    @Test
    public void close() {
        pw.close();
        pw.println("l");
        assertTrue("Write on closed stream failed to generate error", pw.checkError());
    }

    @Test
    public void flush() {
        final double dub = 490000000000.08765;
        pw.print(dub);
        pw.flush();
        assertTrue("Failed to flush", new String(bao.toByteArray()).equals(String.valueOf(dub)));
    }

    @Test
    public void print$C() {
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.print(schars);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("Hello World"));
    }

    @Test
    public void printC() {
        pw.print('c');
        pw.flush();
        assertEquals("Wrote incorrect char string", "c", new String(bao.toByteArray()));
    }

    @Test
    public void printD() {
        final double dub = 490000000000.08765;
        pw.print(dub);
        pw.flush();
        assertTrue("Wrote incorrect double string", new String(bao.toByteArray()).equals(String.valueOf(dub)));
    }

    @Test
    public void printF() {
        final float flo = 49.08765f;
        pw.print(flo);
        pw.flush();
        assertTrue("Wrote incorrect float string", new String(bao.toByteArray()).equals(String.valueOf(flo)));
    }

    @Test
    public void printI() {
        pw.print(4908765);
        pw.flush();
        assertEquals("Wrote incorrect int string", "4908765", new String(bao.toByteArray()));
    }

    @Test
    public void printJ() {
        pw.print(49087650000L);
        pw.flush();
        assertEquals("Wrote incorrect long string", "49087650000", new String(bao.toByteArray()));
    }

    @Test
    public void printLjava_lang_Object() {
        pw.print((Object) null);
        pw.flush();
        assertEquals("Did not write null", "null", new String(bao.toByteArray()));
        bao.reset();

        pw.print(new Bogus());
        pw.flush();
        assertEquals("Wrote in incorrect Object string", "Bogus", new String(bao.toByteArray()));
    }

    @Test
    public void printLjava_lang_String() {
        pw.print((String) null);
        pw.flush();
        assertEquals("did not write null", "null", new String(bao.toByteArray()));
        bao.reset();

        pw.print("Hello World");
        pw.flush();
        assertEquals("Wrote incorrect  string", "Hello World", new String(bao.toByteArray()));
    }

    @Test
    public void printZ() {
        pw.print(true);
        pw.flush();
        assertEquals("Wrote in incorrect boolean string", "true", new String(bao.toByteArray()));
    }

    @Test
    public void println() {
        String s;
        pw.println("Blarg");
        pw.println();
        pw.println("Bleep");
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals("Blarg"));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals(""));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals("Bleep"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    @Test
    public void test_println$C() {
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.println(schars);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("Hello World"));
    }

    @Test
    public void printlnC() {
        String s = null;
        pw.println("Random Chars");
        pw.println('c');
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            s = br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char string: " + s, s.equals("c"));
    }

    @Test
    public void printlnD() {
        String s = null;
        final double dub = 4000000000000000.657483;
        pw.println("Random Chars");
        pw.println(dub);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect double string: " + s, s.equals(String.valueOf(dub)));
    }

    @Test
    public void printlnF() {
        String s;
        final float flo = 40.4646464f;
        pw.println("Random Chars");
        pw.println(flo);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
            assertTrue("Wrote incorrect float string: " + s + " wanted: " + String.valueOf(flo),
                    s.equals(String.valueOf(flo)));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }

    }

    @Test
    public void printlnI() {
        String s = null;
        pw.println("Random Chars");
        pw.println(400000);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect int string: " + s, s.equals("400000"));
    }

    @Test
    public void printlnJ() {
        String s = null;
        pw.println("Random Chars");
        pw.println(4000000000000L);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect long string: " + s, s.equals("4000000000000"));
    }

    @Test
    public void printlnLjava_lang_Object() {
        String s = null;
        pw.println("Random Chars");
        pw.println(new Bogus());
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect Object string: " + s, s.equals("Bogus"));
    }

    @Test
    public void printlnLjava_lang_String() {
        String s = null;
        pw.println("Random Chars");
        pw.println("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect string: " + s, s.equals("Hello World"));
    }

    @Test
    public void printlnZ() {
        String s = null;
        pw.println("Random Chars");
        pw.println(false);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect boolean string: " + s, s.equals("false"));
    }

    @Test
    public void write$C() {
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.write(schars);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test: " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("Hello World"));
    }

    @Test
    public void write$CII() {
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.write(schars, 6, 5);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("World"));
    }

    @Test
    public void writeI() throws IOException {
        char[] cab = new char[3];
        pw.write('a');
        pw.write('b');
        pw.write('c');
        pw.flush();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(bao.toByteArray()));
        cab[0] = (char) isr.read();
        cab[1] = (char) isr.read();
        cab[2] = (char) isr.read();
        assertTrue("Wrote incorrect ints", cab[0] == 'a' && cab[1] == 'b' && cab[2] == 'c');

    }

    @Test
    public void writeLjava_lang_String() {
        String s = null;
        pw.println("Random Chars");
        pw.write("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("Hello World"));
    }

    @Test
    public void writeLjava_lang_StringII() {
        String s = null;
        pw.println("Random Chars");
        pw.write("Hello World", 6, 5);
        pw.flush();
        try {
            br = new BufferedReader(new StringReader(bao.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("World"));
    }

    @Test
    public void appendChar() {
        char testChar = ' ';
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.append(testChar);
        printWriter.flush();
        assertEquals(String.valueOf(testChar), out.toString());
        printWriter.close();
    }

    @Test
    public void appendCharSequence() {
        String testString = "My Test String";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.append(testString);
        printWriter.flush();
        assertEquals(testString, out.toString());
        printWriter.close();

    }

    @Test
    public void appendCharSequenceIntInt() {
        String testString = "My Test String";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.append(testString, 1, 3);
        printWriter.flush();
        assertEquals(testString.substring(1, 3), out.toString());
        printWriter.close();
    }

    @Test
    public void printfLjava_lang_String$Ljava_lang_Object() {
        pw.printf("%s %s", "Hello", "World");
        pw.flush();
        assertEquals("Wrote incorrect string", "Hello World",  new String(bao.toByteArray()));
    }

    @Test
    public void printfLjava_util_Locale_Ljava_lang_String_$Ljava_lang_Object() {
        pw.printf(Locale.US, "%s %s", "Hello", "World");
        pw.flush();
        assertEquals("Wrote incorrect string", "Hello World", new String(bao.toByteArray()));
    }
}
