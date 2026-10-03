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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.CharArrayReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class CharArrayReaderTest {
    char[] hw = { 'H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd' };
    CharArrayReader cr;

    @Test
    public void constructor$C() throws IOException {
        cr = new CharArrayReader(hw);
        assertTrue(cr.ready(), "Failed to create reader");
    }

    @Test
    public void constructor$CII() throws IOException {
        cr = new CharArrayReader(hw, 5, 5);
        assertTrue(cr.ready(), "Failed to create reader");

        int c = cr.read();
        assertTrue(c == 'W', "Created incorrect reader--returned '" + (char) c + "' instead of 'W'");
    }

    @Test
    public void close() {
        cr = new CharArrayReader(hw);
        cr.close();
        try {
            cr.read();
            fail("Failed to throw exception on read from closed stream");
        } catch (IOException e) {
            // Expected
        }

        // No-op
        cr.close();
    }

    @Test
    public void markI() throws IOException {
        cr = new CharArrayReader(hw);
        cr.skip(5L);
        cr.mark(100);
        cr.read();
        cr.reset();
        assertEquals('W', cr.read(), "Failed to mark correct position");
    }

    @Test
    public void markSupported() {
        cr = new CharArrayReader(hw);
        assertTrue(cr.markSupported(), "markSupported returned false");
    }

    @Test
    public void read() throws IOException {
        cr = new CharArrayReader(hw);
        assertEquals('H', cr.read(), "Read returned incorrect char");
        cr = new CharArrayReader(new char[] { '\u8765' });
        assertTrue(cr.read() == '\u8765', "Incorrect double byte char");
    }

    @Test
    public void read$CII() throws IOException {
        char[] c = new char[11];
        cr = new CharArrayReader(hw);
        cr.read(c, 1, 10);
        assertTrue(new String(c, 1, 10).equals(new String(hw, 0, 10)), "Read returned incorrect chars");
    }

    @Test
    public void ready() throws IOException {
        cr = new CharArrayReader(hw);
        assertTrue(cr.ready(), "ready returned false");
        cr.skip(1000);
        assertTrue(!cr.ready(), "ready returned true");
        cr.close();

        try {
            cr.ready();
            fail("No exception 1");
        } catch (IOException e) {
            // expected
        }
        try {
            cr = new CharArrayReader(hw);
            cr.close();
            cr.ready();
            fail("No exception 2");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void reset() throws IOException {
        cr = new CharArrayReader(hw);
        cr.skip(5L);
        cr.mark(100);
        cr.read();
        cr.reset();
        assertEquals('W', cr.read(), "Reset failed to return to marker position");

        // Regression for HARMONY-4357
        String str = "offsetHello world!";
        char[] data = new char[str.length()];
        str.getChars(0, str.length(), data, 0);
        int offsetLength = 6;
        int length = data.length - offsetLength;

        CharArrayReader reader = new CharArrayReader(data, offsetLength, length);
        reader.reset();
        for (int i = 0; i < length; i++) {
            assertEquals(data[offsetLength + i], (char) reader.read());
        }
    }

    @Test
    public void skipJ() throws IOException {
        cr = new CharArrayReader(hw);
        long skipped = cr.skip(5L);

        assertEquals(5L, skipped, "Failed to skip correct number of chars");
        assertEquals('W', cr.read(), "Skip skipped wrong chars");
    }

    @Test
    public void readIntoBuffer() throws IOException {
        cr = new CharArrayReader(hw);
        var buffer = CharBuffer.allocate(100);
        assertEquals(10, cr.read(buffer));
        buffer.flip();
        assertEquals("HelloWorld", buffer.toString());
        buffer.limit(20);
        assertEquals(0, buffer.get(10));

        cr = new CharArrayReader(hw);
        var array = new char[100];
        buffer = CharBuffer.wrap(array, 10, 80);
        assertEquals(10, cr.read(buffer));
        assertEquals("HelloWorld", new String(array, 10, 10));

        cr = new CharArrayReader(hw);
        buffer = CharBuffer.allocate(5);
        assertEquals(5, cr.read(buffer));
        buffer.flip();
        assertEquals("Hello", buffer.toString());

        cr = new CharArrayReader(hw);
        try {
            cr.read(CharBuffer.allocate(10).asReadOnlyBuffer());
            fail("Expected exception not thrown");
        } catch (ReadOnlyBufferException e) {
            // ok
        }
    }
}
