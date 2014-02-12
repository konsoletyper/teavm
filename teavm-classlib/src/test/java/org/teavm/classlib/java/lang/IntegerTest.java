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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class IntegerTest {
    @Test
    public void parsesInteger() {
        assertEquals(0, Integer.parseInt("0", 10));
        assertEquals(473, Integer.parseInt("473", 10));
        assertEquals(42, Integer.parseInt("+42", 10));
        assertEquals(0, Integer.parseInt("-0", 10));
        assertEquals(-255, Integer.parseInt("-FF", 16));
        assertEquals(102, Integer.parseInt("1100110", 2));
        assertEquals(2147483647, Integer.parseInt("2147483647", 10));
        assertEquals(411787, Integer.parseInt("Kona", 27));
    }

    @Test
    public void parsesMinInteger() {
        assertEquals(-2147483648, Integer.parseInt("-2147483648", 10));
        assertEquals(-2147483648, Integer.parseInt("-80000000", 16));
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsTooBigInteger() {
        Integer.parseInt("2147483648", 10);
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsIntegerWithDigitsOutOfRadix() {
        Integer.parseInt("99", 8);
    }

    @Test
    public void writesInteger() {
        assertEquals("473", Integer.toString(473, 10));
        assertEquals("-ff", Integer.toString(-255, 16));
        assertEquals("kona", Integer.toString(411787, 27));
    }

    @Test
    public void decodes() {
        assertEquals(Integer.valueOf(123), Integer.decode("123"));
        assertEquals(Integer.valueOf(83), Integer.decode("0123"));
        assertEquals(Integer.valueOf(255), Integer.decode("0xFF"));
        assertEquals(Integer.valueOf(65535), Integer.decode("+0xFFFF"));
        assertEquals(Integer.valueOf(-255), Integer.decode("-0xFF"));
        assertEquals(Integer.valueOf(2748), Integer.decode("+#ABC"));
    }
}
