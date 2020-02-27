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

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class InputStreamReaderTest {
    @Test
    public void readsChars() throws IOException {
        String str = "foo bar baz";
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < str.length(); ++i) {
            bytes[i] = (byte) str.charAt(i);
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
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
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
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
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        char[] chars = new char[12000];
        int readChars = reader.read(chars);
        assertEquals(chars.length, readChars);
        for (int i = 0; i < chars.length; ++i) {
            assertEquals(str.charAt(i), chars[i]);
        }
    }
}
