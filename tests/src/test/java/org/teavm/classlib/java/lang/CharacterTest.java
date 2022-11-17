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
}
