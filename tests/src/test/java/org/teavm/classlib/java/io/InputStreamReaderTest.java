/*
 *  Copyright 2014 Alexey Andreev.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InputStreamReaderTest {
    @Test
    public void readsChars() throws IOException {
        String str = "foo bar baz";
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < str.length(); ++i) {
            bytes[i] = (byte) str.charAt(i);
        }
        var stream = new ByteArrayInputStream(bytes);
        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        char[] chars = new char[100];
        int readChars = reader.read(chars);
        assertEquals(str.length(), readChars);
        for (int i = 0; i < str.length(); ++i) {
            assertEquals(str.charAt(i), chars[i]);
        }
    }

    @Test
    public void readsCharsOneByOne() throws IOException {
        String str = "foo bar baz";
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < str.length(); ++i) {
            bytes[i] = (byte) str.charAt(i);
        }
        var stream = new ByteArrayInputStream(bytes);
        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        assertEquals('f', reader.read());
        assertEquals('o', reader.read());
        assertEquals('o', reader.read());
        assertEquals(' ', reader.read());
    }

    @Test
    public void readsManyChars() throws IOException {
        StringBuilder sb = new StringBuilder();
        String str = "foo bar baz";
        for (int i = 0; i < 10000; ++i) {
            sb.append(str);
        }
        str = sb.toString();
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < str.length(); ++i) {
            bytes[i] = (byte) str.charAt(i);
        }
        var stream = new ByteArrayInputStream(bytes);
        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        char[] chars = new char[12000];
        int readChars = reader.read(chars);
        assertEquals(chars.length, readChars);
        for (int i = 0; i < chars.length; ++i) {
            assertEquals(str.charAt(i), chars[i]);
        }
    }

    @Test
    public void underflowWhileFillingBuffer() throws IOException {
        var bos = new ByteArrayOutputStream();
        bos.write(0x24);
        for (var i = 0; i < 20000; ++i) {
            bos.write(0xC2);
            bos.write(0xA2);
        }

        var bis = new ByteArrayInputStream(bos.toByteArray());
        var reader = new InputStreamReader(bis, StandardCharsets.UTF_8);
        assertEquals(0x24, reader.read());
        for (var i = 0; i < 20000; ++i) {
            assertEquals(0xA2, reader.read());
        }
        assertEquals(-1, reader.read());
    }

    @Test
    public void nonGreedyRead() throws IOException {
        var in = new TestInputStream();
        var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        var testString = "ABCDEFGHIJKLMNOPABCD";
        for (var i = 0; i < 5; ++i) {
            assertEquals(testString.charAt(i), reader.read());
            assertEquals(5, in.reads);
        }
        for (var i = 5; i < 10; ++i) {
            assertEquals(testString.charAt(i), reader.read());
            assertEquals(10, in.reads);
        }
        for (var i = 10; i < 15; ++i) {
            assertEquals(testString.charAt(i), reader.read());
            assertEquals(15, in.reads);
        }
        for (var i = 15; i < 20; ++i) {
            assertEquals(testString.charAt(i), reader.read());
            assertEquals(20, in.reads);
        }
        in.close();
    }

    @Test
    public void nonGreedyReadToArray() throws IOException {
        var in = new TestInputStream();
        var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        var buffer = new char[10];

        assertEquals(2, reader.read(buffer, 0, 2));
        assertArrayEquals("AB".toCharArray(), Arrays.copyOf(buffer, 2));
        assertEquals(5, in.reads);
        assertEquals(3, reader.read(buffer));
        assertArrayEquals("CDE".toCharArray(), Arrays.copyOf(buffer, 3));
        assertEquals(5, in.reads);

        assertEquals(5, reader.read(buffer));
        assertArrayEquals("FGHIJ".toCharArray(), Arrays.copyOf(buffer, 5));
        assertEquals(10, in.reads);

        assertEquals(5, reader.read(buffer));
        assertArrayEquals("KLMNO".toCharArray(), Arrays.copyOf(buffer, 5));
        assertEquals(15, in.reads);

        assertEquals(5, reader.read(buffer));
        assertArrayEquals("PABCD".toCharArray(), Arrays.copyOf(buffer, 5));
        assertEquals(20, in.reads);
    }

    private static class TestInputStream extends InputStream {
        int reads;
        private byte lastRead;

        @Override
        public int read() {
            reads++;
            return 'A' + (lastRead++ & 15);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            var readBytes = 0;
            while (len-- > 0) {
                reads++;
                readBytes++;
                b[off++] = (byte) ('A' + (lastRead++ & 15));
                if (reads % 5 == 0) {
                    break;
                }
            }
            return readBytes;
        }
    }
}
