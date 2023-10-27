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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class CharacterTest {
    @Test
    public void digitsRecognized() {
        assertEquals(2, Character.digit('2', 10));
        assertEquals(-1, Character.digit('.', 10));
        assertEquals(6, Character.digit('\u096C', 10));
        assertEquals(15, Character.digit('F', 16));
    }

    @Test
    public void classesRecognized() {
        assertEquals(Character.DECIMAL_DIGIT_NUMBER, Character.getType('2'));
        assertEquals(Character.UPPERCASE_LETTER, Character.getType('Q'));
        assertEquals(Character.LOWERCASE_LETTER, Character.getType('w'));
        assertEquals(Character.MATH_SYMBOL, Character.getType(0x21F7));
        assertEquals(Character.NON_SPACING_MARK, Character.getType(0xFE25));
        assertEquals(Character.DECIMAL_DIGIT_NUMBER, Character.getType(0x1D7D9));
    }

    @Test
    public void lowerCase() {
        assertEquals('1', Character.toLowerCase('1'));
        assertEquals('a', Character.toLowerCase('a'));
        assertEquals('b', Character.toLowerCase('b'));
        assertEquals('z', Character.toLowerCase('z'));
        assertEquals('@', Character.toLowerCase('@'));
        assertEquals('a', Character.toLowerCase('A'));
        assertEquals('b', Character.toLowerCase('B'));
        assertEquals('z', Character.toLowerCase('Z'));
        assertEquals('щ', Character.toLowerCase('щ'));
        assertEquals('щ', Character.toLowerCase('Щ'));
        assertEquals('ü', Character.toLowerCase('ü'));
        assertEquals('ü', Character.toLowerCase('Ü'));
    }

    @Test
    public void upperCase() {
        assertEquals('1', Character.toUpperCase('1'));
        assertEquals('A', Character.toUpperCase('a'));
        assertEquals('B', Character.toUpperCase('b'));
        assertEquals('Z', Character.toUpperCase('z'));
        assertEquals('@', Character.toUpperCase('@'));
        assertEquals('A', Character.toUpperCase('A'));
        assertEquals('B', Character.toUpperCase('B'));
        assertEquals('Z', Character.toUpperCase('Z'));
        assertEquals('Щ', Character.toUpperCase('щ'));
        assertEquals('Щ', Character.toUpperCase('Щ'));
        assertEquals('Ü', Character.toUpperCase('ü'));
        assertEquals('Ü', Character.toUpperCase('Ü'));
    }

    @Test
    public void reverseBytes() {
        assertEquals((char) 12405, Character.reverseBytes((char) 30000));
        assertEquals((char) -12150, Character.reverseBytes((char) -30000));
    }

    @Test
    public void characterForDigit() {
        assertEquals('\0', Character.forDigit(-1, 5));
        assertEquals('\0', Character.forDigit(6, 5));
        assertEquals('\0', Character.forDigit(1, 1));
        assertEquals('\0', Character.forDigit(1, 37));
        assertEquals('a', Character.forDigit(10, 11));
        assertEquals('\0', Character.forDigit(10, 10));
        assertEquals('5', Character.forDigit(5, 6));
    }

    @Test
    public void offsetByCodePointsCharSequence() {
        int result = Character.offsetByCodePoints("a\uD800\uDC00b", 0, 2);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("abcd", 3, -1);
        assertEquals(2, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b", 0, 3);
        assertEquals(4, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b", 3, -1);
        assertEquals(1, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b", 3, 0);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("\uD800\uDC00bc", 3, 0);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("a\uDC00bc", 3, -1);
        assertEquals(2, result);
        result = Character.offsetByCodePoints("a\uD800bc", 3, -1);
        assertEquals(2, result);
        try {
            Character.offsetByCodePoints(null, 0, 1);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc", -1, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc", 4, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc", 1, 3);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc", 1, -2);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }
    @Test
    public void offsetByCodePointsCharArray() {
        int result = Character.offsetByCodePoints("a\uD800\uDC00b"
                .toCharArray(), 0, 4, 0, 2);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b".toCharArray(),
                0, 4, 0, 3);
        assertEquals(4, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b\uD800c"
                .toCharArray(), 0, 5, 0, 3);
        assertEquals(4, result);
        result = Character
                .offsetByCodePoints("abcd".toCharArray(), 0, 4, 3, -1);
        assertEquals(2, result);
        result = Character
                .offsetByCodePoints("abcd".toCharArray(), 1, 2, 3, -2);
        assertEquals(1, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b".toCharArray(),
                0, 4, 3, -1);
        assertEquals(1, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b".toCharArray(),
                0, 2, 2, -1);
        assertEquals(1, result);
        result = Character.offsetByCodePoints("a\uD800\uDC00b".toCharArray(),
                0, 4, 3, 0);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("\uD800\uDC00bc".toCharArray(),
                0, 4, 3, 0);
        assertEquals(3, result);
        result = Character.offsetByCodePoints("a\uDC00bc".toCharArray(), 0, 4,
                3, -1);
        assertEquals(2, result);
        result = Character.offsetByCodePoints("a\uD800bc".toCharArray(), 0, 4,
                3, -1);
        assertEquals(2, result);
        try {
            Character.offsetByCodePoints(null, 0, 4, 1, 1);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abcd".toCharArray(), -1, 4, 1, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abcd".toCharArray(), 0, -1, 1, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abcd".toCharArray(), 2, 4, 1, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abcd".toCharArray(), 1, 3, 0, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abcd".toCharArray(), 1, 1, 3, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc".toCharArray(), 0, 3, 1, 3);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc".toCharArray(), 0, 2, 1, 2);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            Character.offsetByCodePoints("abc".toCharArray(), 1, 3, 1, -2);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }
}
