/*
 *  Copyright 2013 Alexey Andreev.
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
import java.io.UnsupportedEncodingException;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class StringTest {
    @Test
    public void charsExtracted() {
        String str = "123";
        assertEquals('1', str.charAt(0));
        assertEquals('2', str.charAt(1));
        assertEquals('3', str.charAt(2));
    }

    @Test
    public void lengthComputed() {
        String str = "123";
        assertEquals(3, str.length());
    }

    @Test
    public void stringCreatedFromChars() {
        String str = new String(new char[] { '1', '2', '3' });
        assertEquals("123", str);
    }

    @Test
    public void stringsAreEqual() {
        String a = new String(new char[] { '1', '2', '3' });
        String b = new String(new char[] { '1', '2', '3' });
        assertEquals(a, b);
    }

    @Test
    public void stringsAreNotEqual() {
        String a = new String(new char[] { '1', '2', '3' });
        String b = new String(new char[] { '1', '4', '3' });
        assertNotEquals(a, b);
    }

    @Test
    public void stringCharactersRead() {
        char[] buffer = new char[4];
        "123".getChars(0, 3, buffer, 0);
        assertEquals('1', buffer[0]);
        assertEquals('2', buffer[1]);
        assertEquals('3', buffer[2]);
    }

    @Test
    public void stringIsEqualToBuilder() {
        assertTrue("123".contentEquals(new StringBuilder().append('1').append('2').append('3')));
    }

    @Test
    public void comparesSameStrings() {
        String a = "123";
        String b = new String(a);
        assertTrue(a.compareTo(b) == 0);
    }

    @Test
    public void comparesToPrecedingStrings() {
        assertTrue("abc".compareTo("abbc") > 0);
    }

    @Test
    public void comparesToSuccessorStrings() {
        assertTrue("abc".compareTo("abdc") < 0);
    }

    @Test
    public void startsWithWorks() {
        assertTrue("123".startsWith("12"));
    }

    @Test
    public void regionsMatched() {
        assertTrue("12345".regionMatches(2, "23456", 1, 2));
    }

    @Test
    public void endsWithWorkds() {
        assertTrue("12345".endsWith("45"));
    }

    @Test
    public void exposesSupplementaryCodePoint() {
        String str = new String(new char[] { (char)56178, (char)56972 });
        assertEquals(969356, str.codePointAt(0));
        assertEquals(56972, str.codePointAt(1));
    }

    @Test
    public void exposesWrongSurrogates() {
        String str = new String(new char[] { (char)56972, (char)56178 });
        assertEquals(56972, str.codePointAt(0));
        assertEquals(56178, str.codePointAt(1));
    }

    @Test
    public void exposesSupplementaryCodePointBefore() {
        String str = new String(new char[] { (char)56178, (char)56972 });
        assertEquals(969356, str.codePointBefore(2));
        assertEquals(56178, str.codePointBefore(1));
    }

    @Test
    public void countsCodePoints() {
        String str = new String(new char[] { (char)56178, (char)56972, 'a', 'b' });
        assertEquals(3, str.codePointCount(0, 4));
        assertEquals(1, str.codePointCount(0, 2));
        assertEquals(2, str.codePointCount(2, 4));
        assertEquals(3, str.codePointCount(1, 4));
    }

    @Test
    public void givesOffsetByCodePoint() {
        String str = new String(new char[] { (char)56178, (char)56972, 'a', 'b' });
        assertEquals(2, str.offsetByCodePoints(0, 1));
        assertEquals(2, str.offsetByCodePoints(1, 1));
        assertEquals(4, str.offsetByCodePoints(0, 3));
        assertEquals(4, str.offsetByCodePoints(1, 3));
    }

    @Test
    public void findsCodePoint() {
        String str = new String(new char[] { 'a', 'b', (char)56178, (char)56972, 'c',
                (char)56178, (char)56972, 'c', 'd' });
        assertEquals(2, str.indexOf(969356));
        assertEquals(4, str.indexOf('c'));
    }

    @Test
    public void findsCodePointBackward() {
        String str = new String(new char[] { 'a', 'b', (char)56178, (char)56972, 'c',
                (char)56178, (char)56972, 'c', 'd' });
        assertEquals(5, str.lastIndexOf(969356));
        assertEquals(7, str.lastIndexOf('c'));
    }

    @Test
    public void findsString() {
        assertEquals(1, "abcdbcd".indexOf("bc"));
        assertEquals(-1, "abcdbcd".indexOf("bb"));
    }

    @Test
    public void findsStringBackward() {
        assertEquals(4, "abcdbcd".lastIndexOf("bc"));
        assertEquals(-1, "abcdbcd".lastIndexOf("bb"));
    }

    @Test
    public void concatenatesStrings() {
        assertEquals("abcd", "ab".concat("cd"));
    }

    @Test
    public void replacesCharacter() {
        assertEquals("abbdbbd", "abcdbcd".replace('c', 'b'));
    }

    @Test
    public void containsWorks() {
        assertTrue("abcd".contains("bc"));
    }

    @Test
    public void sequenceReplaced() {
        assertEquals("ba", "aaa".replace("aa", "b"));
    }

    @Test
    public void trimWorks() {
        assertEquals("ab", "  ab   ".trim());
        assertEquals("ab", "ab".trim());
        assertEquals("", "  ".trim());
    }

    @Test
    public void convertedToCharArray() {
        char[] array = "123".toCharArray();
        assertEquals(3, array.length);
        assertEquals('1', array[0]);
        assertEquals('2', array[1]);
        assertEquals('3', array[2]);
    }

    @Test
    public void createdFromByteArray() throws UnsupportedEncodingException {
        byte[] bytes = { 49, 50, 51 };
        assertEquals("123", new String(bytes, "UTF-8"));
    }

    @Test
    public void createdFromUTF8ByteArray() throws UnsupportedEncodingException {
        byte[] bytes = { 65, -62, -69, -32, -82, -69, -16, -66, -78, -69 };
        assertEquals("A\u00BB\u0BBB\uD8BB\uDCBB", new String(bytes, "UTF-8"));
    }
}
