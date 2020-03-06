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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.io.UnsupportedEncodingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
        assertArrayEquals(new char[] { '1', '2', '3', '\0' }, buffer);
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
        String str = new String(new char[] { (char) 56178, (char) 56972 });
        assertEquals(969356, str.codePointAt(0));
        assertEquals(56972, str.codePointAt(1));
    }

    @Test
    public void exposesWrongSurrogates() {
        String str = new String(new char[] { (char) 56972, (char) 56178 });
        assertEquals(56972, str.codePointAt(0));
        assertEquals(56178, str.codePointAt(1));
    }

    @Test
    public void exposesSupplementaryCodePointBefore() {
        String str = new String(new char[] { (char) 56178, (char) 56972 });
        assertEquals(969356, str.codePointBefore(2));
        assertEquals(56178, str.codePointBefore(1));
    }

    @Test
    public void countsCodePoints() {
        String str = new String(new char[] { (char) 56178, (char) 56972, 'a', 'b' });
        assertEquals(3, str.codePointCount(0, 4));
        assertEquals(1, str.codePointCount(0, 2));
        assertEquals(2, str.codePointCount(2, 4));
        assertEquals(3, str.codePointCount(1, 4));
    }

    @Test
    public void givesOffsetByCodePoint() {
        String str = new String(new char[] { (char) 56178, (char) 56972, 'a', 'b' });
        assertEquals(2, str.offsetByCodePoints(0, 1));
        assertEquals(2, str.offsetByCodePoints(1, 1));
        assertEquals(4, str.offsetByCodePoints(0, 3));
        assertEquals(4, str.offsetByCodePoints(1, 3));
    }

    @Test
    public void findsCodePoint() {
        String str = new String(new char[] { 'a', 'b', (char) 56178, (char) 56972, 'c',
                (char) 56178, (char) 56972, 'c', 'd' });
        assertEquals(2, str.indexOf(969356));
        assertEquals(4, str.indexOf('c'));
    }

    @Test
    public void findsCodePointBackward() {
        String str = new String(new char[] { 'a', 'b', (char) 56178, (char) 56972, 'c',
                (char) 56178, (char) 56972, 'c', 'd' });
        assertEquals(5, str.lastIndexOf(969356));
        assertEquals(7, str.lastIndexOf('c'));
    }

    @Test
    public void findsString() {
        assertEquals(1, "abcdbcd".indexOf("bc"));
        assertEquals(3, "abcdbcd".indexOf("dbcd"));
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
        assertArrayEquals(new char[] { '1', '2', '3' }, array);
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

    @Test
    public void createFromLongUTF8ByteArray() throws UnsupportedEncodingException {
        byte[] bytes = new byte[16384];
        for (int i = 0; i < bytes.length;) {
            bytes[i++] = -16;
            bytes[i++] = -66;
            bytes[i++] = -78;
            bytes[i++] = -69;
        }
        String str = new String(bytes, "UTF-8");
        assertEquals('\uD8BB', str.charAt(8190));
        assertEquals('\uDCBB', str.charAt(8191));
    }

    @Test
    public void getByteArray() throws UnsupportedEncodingException {
        byte[] bytes = "123".getBytes("UTF-8");
        assertArrayEquals(new byte[] { 49, 50, 51 }, bytes);
        assertEquals(3, bytes.length);
    }

    @Test
    public void getUTF8ByteArray() throws UnsupportedEncodingException {
        byte[] bytes = "A\u00BB\u0BBB\uD8BB\uDCBB".getBytes("UTF-8");
        assertArrayEquals(new byte[] { 65, -62, -69, -32, -82, -69, -16, -66, -78, -69 }, bytes);
    }

    @Test
    public void getUTF8ByteArrayOfLongString() throws UnsupportedEncodingException {
        char[] chars = new char[8192];
        for (int i = 0; i < chars.length;) {
            chars[i++] = '\uD8BB';
            chars[i++] = '\uDCBB';
        }
        byte[] bytes = new String(chars).getBytes("UTF-8");
        assertEquals(-16, bytes[16380]);
        assertEquals(-66, bytes[16381]);
        assertEquals(-78, bytes[16382]);
        assertEquals(-69, bytes[16383]);
    }

    @Test
    public void createsStringFromCodePoints() {
        int[] codePoints = { 97, 98, 969356, 99, 969356, 99, 100 };
        String str = new String(codePoints, 0, codePoints.length);
        assertEquals('a', str.charAt(0));
        assertEquals('b', str.charAt(1));
        assertEquals(56178, str.charAt(2));
        assertEquals(56972, str.charAt(3));
        assertEquals('c', str.charAt(4));
        assertEquals(56178, str.charAt(5));
        assertEquals(56972, str.charAt(6));
        assertEquals('c', str.charAt(7));
        assertEquals('d', str.charAt(8));
    }

    @Test
    public void makesLowerCase() {
        assertEquals("foo bar", "FoO bAr".toLowerCase());
    }

    @Test
    public void interns() {
        assertSame("xabc".substring(1).intern(), "abcx".substring(0, 3).intern());
        assertSame("xabc".substring(1).intern(), "abc");
    }

    @Test
    public void internsConstants() {
        assertSame("abc", ("a" + "bc").intern());
    }

    @Test
    public void joinStrings() {
        String str = String.join("/", "a", "b");
        assertEquals(3, str.length());
        assertEquals("a/b", str);
    }

    @Test
    public void joinSingleString() {
        String str = String.join("/", "a");
        assertEquals(1, str.length());
        assertEquals("a", str);
    }

    @Test
    public void joinNoStrings() {
        String str = String.join("/");
        assertEquals(0, str.length());
        assertEquals("", str);
    }
    
    @Test
    public void testIsBlank() {
        assertTrue(new String(new char[0]).isBlank());
        assertTrue(new String(new char[] { ' ', ' ' }).isBlank());
        assertFalse(new String(new char[] { ' ', 'x', ' ' }).isBlank());
        assertFalse(new String(new char[] { 'a', ' ' }).isBlank());
    }    
}
