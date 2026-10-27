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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
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
    public void endsWithWorks() {
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
        assertEquals("xaxaxax", "aaa".replace("", "x"));
        assertEquals("axc", "abc".replace("b", "x"));
        assertEquals("abc", "abc".replace("bc", "bc"));
        assertEquals("xyzbxyz", "abcbabc".replace("abc", "xyz"));
        assertEquals("[]b[]b[]", "aabaabaa".replace("aa", "[]"));
        assertEquals("--", "abcabc".replace("abc", "-"));
        assertEquals("", "abcabc".replace("abc", ""));
        assertEquals("aXYZXYZb", "a12b".replace(new StringBuilder("1"), "XYZ").replace("2", "XYZ"));
        assertEquals("ab", "ab".replace("abc", "x"));
        assertEquals("[]b[]", "aabaa".replace(new StringBuilder("aa"), new StringBuilder("[]")));
        assertEquals("-b-", "aabaa".replace(new StringBuilder("aa"), "-"));
        assertSame("ab", "ab".replace(new StringBuilder("ac"), "x"));
    }

    @Test
    public void sequenceReplaceReturnsSameStringWhenNotFound() {
        var s = new String(new char[] { 'a', 'b', 'c' });
        assertSame(s, s.replace("bd", "x"));
        assertSame(s, s.replace("d", "x"));
    }

    @Test
    public void trimWorks() {
        assertEquals("ab", "  ab   ".trim());
        assertEquals("ab", "ab".trim());
        assertEquals("", "  ".trim());
    }

    @Test
    public void stripWorks() {
        assertEquals("ab", "  ab   ".strip());
        assertEquals("ab", "ab".strip());
        assertEquals("", "  \t".strip());
        assertEquals("ab", "\t\n \u2008ab\r\f".strip());
    }

    @Test
    public void stripLeadingWorks() {
        assertEquals("ab   ", "  ab   ".stripLeading());
        assertEquals("ab   ", "ab   ".stripLeading());
        assertEquals("ab", "ab".stripLeading());
        assertEquals("a b", "a b".stripLeading());
        assertEquals("", "  \t".stripLeading());
        assertEquals("ab", "\t\n \u2008\r\fab".stripLeading());
    }

    @Test
    public void stripTrailingWorks() {
        assertEquals("  ab", "  ab   ".stripTrailing());
        assertEquals("  ab", "  ab".stripTrailing());
        assertEquals("ab", "ab".stripTrailing());
        assertEquals("a b", "a b".stripTrailing());
        assertEquals("", "  \t".stripTrailing());
        assertEquals("ab", "ab\t\n \u2008\r\f".stripTrailing());
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
    public void convertsCase() {
        assertEquals("foo bar", "FoO bAr".toLowerCase());
        assertEquals("FOO BAR", "FoO bAr".toUpperCase());
    }

    private String common = "i̇stanbul";
    private String turkish = "istanbul";

    @Test
    @SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY_GC })
    public void convertsCaseLocaled() {
        assertEquals(turkish, "İstanbul".toLowerCase(new Locale("tr", "TR")));
        assertEquals(common, "İstanbul".toLowerCase(Locale.US));
        assertNotEquals(turkish, common);
        assertEquals("İSTANBUL", common.toUpperCase(Locale.US));
        assertEquals("İSTANBUL", turkish.toUpperCase(new Locale("tr", "TR")));
        assertNotEquals(common.toUpperCase(Locale.US), turkish.toUpperCase(new Locale("tr", "TR")));
        assertEquals(common.toUpperCase(Locale.US), common.toUpperCase(Locale.CANADA));
        assertEquals(common.toUpperCase(Locale.US), common.toUpperCase(Locale.ROOT));
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

    // String.join(CharSequence delimiter, CharSequence... elements) and
    // join(CharSequence delimiter, Iterable<? extends CharSequence> elements): "Note that if an element is null,
    // then "null" is added."
    @Test
    public void joinNullElements() {
        CharSequence nullSequence = null;
        assertEquals("null", String.join("/", nullSequence));
        assertEquals("a/null/b", String.join("/", "a", null, "b"));
        assertEquals("null/a/null", String.join("/", null, "a", null));
        assertEquals("null--null", String.join("--", nullSequence, nullSequence));
        assertEquals("a/null/b", String.join("/", Arrays.asList("a", null, "b")));
        assertEquals("null/a/null", String.join("/", Arrays.asList(null, "a", null)));
        assertEquals("null", String.join("/", Arrays.asList(nullSequence)));
    }

    // String.substring(int beginIndex, int endIndex) throws IndexOutOfBoundsException "if the beginIndex is
    // negative, or endIndex is larger than the length of this String object, or beginIndex is larger than
    // endIndex". subSequence(int, int) behaves as substring. This holds for empty ranges too.
    @Test
    public void substringChecksBounds() {
        String str = "null";
        for (int begin = -1; begin <= 5; ++begin) {
            for (int end = -1; end <= 5; ++end) {
                String expected = begin >= 0 && begin <= end && end <= 4 ? "null".substring(begin, end) : "IOOBE";
                String where = "begin=" + begin + ", end=" + end;
                String actual;
                try {
                    actual = str.substring(begin, end);
                } catch (IndexOutOfBoundsException e) {
                    actual = "IOOBE";
                }
                assertEquals(expected, actual, where);
                try {
                    actual = str.subSequence(begin, end).toString();
                } catch (IndexOutOfBoundsException e) {
                    actual = "IOOBE";
                }
                assertEquals(expected, actual, where);
            }
        }
        assertThrows(IndexOutOfBoundsException.class, () -> str.substring(5));
        assertThrows(IndexOutOfBoundsException.class, () -> str.substring(-1));
        assertEquals("", str.substring(4));
    }
    
    @Test
    public void testIsBlank() {
        assertTrue(new String(new char[0]).isBlank());
        assertTrue(new String(new char[] { ' ', ' ' }).isBlank());
        assertFalse(new String(new char[] { ' ', 'x', ' ' }).isBlank());
        assertFalse(new String(new char[] { 'a', ' ' }).isBlank());
    }

    @Test
    public void testChars() {
        assertEquals(0, "".chars().toArray().length);
        assertArrayEquals(new int[] {'A', 'B', 'C', '1', '2', '3'}, "ABC123".chars().toArray());
    }

    @Test
    public void codePointsStream() {
        assertEquals(0, "".codePoints().toArray().length);
        assertArrayEquals(new int[] {'A', 'B', 'C', '1', '2', '3'}, "ABC123".chars().toArray());

        assertArrayEquals(new int[] { 969356 }, new String(new char[] { (char) 56178, (char) 56972 })
                .codePoints().toArray());
        assertArrayEquals(new int[] { 56972, 56178 }, new String(new char[] { (char) 56972, (char) 56178 })
                .codePoints().toArray());
        assertArrayEquals(new int[] { 56178 }, String.valueOf((char) 56178).codePoints().toArray());
    }
    
    @Test
    public void translateEscapes() {
        assertEquals("abc", "abc".translateEscapes());
        assertEquals("\n\r\t\n", "\\n\\r\\t\\12".translateEscapes());
    }
    
    @Test
    public void stripIndent() {
        assertEquals("abc", "abc".stripIndent());
        assertEquals("abc", "  abc".stripIndent());
        assertEquals("abc\ndef", " abc\n def".stripIndent());
        assertEquals("abc\n\ndef\n", " abc\n \n def\n ".stripIndent());
        assertEquals("abc\n\ndef\n", " abc\r\n \n def\r ".stripIndent());
        assertEquals("", " ".stripIndent());
        assertEquals("\n", " \n ".stripIndent());
    }

    // Javadoc of String(byte[] ascii, int hibyte, int offset, int count): "Each char c in the resulting
    // string is constructed from the corresponding component b in the byte array such that:
    // c == (char)(((hibyte & 0xff) << 8) | (b & 0xff))"
    @Test
    @SuppressWarnings("deprecation")
    public void hibyteConstructor() {
        assertEquals("Hi", new String(new byte[] { 72, 105 }, 0, 0, 2));
        assertEquals("Łł", new String(new byte[] { 65, 66 }, 1, 0, 2));
        assertEquals("i", new String(new byte[] { 72, 105, 33 }, 0, 1, 1));
        assertEquals("", new String(new byte[] { 72, 105 }, 0, 2, 0));
        assertEquals("", new String(new byte[0], 5, 0, 0));

        // Exhaustive over every byte value and a set of hibytes, including ones with bits above 0xff
        // that must be masked off and negative ones.
        var bytes = new byte[256];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) i;
        }
        int[] hibytes = { 0, 1, 0x7f, 0x80, 0xd8, 0xdc, 0xff, 0x100, 0x1ff, 0x12345, -1, -256, Integer.MIN_VALUE,
                Integer.MAX_VALUE };
        for (int hibyte : hibytes) {
            var s = new String(bytes, hibyte, 0, bytes.length);
            var s2 = new String(bytes, hibyte);
            assertEquals(256, s.length());
            assertEquals(s, s2);
            for (int b = 0; b < 256; ++b) {
                char expected = (char) (((hibyte & 0xff) << 8) | b);
                assertEquals(expected, s.charAt(b), "hibyte=" + hibyte + ", b=" + b);
            }
            var sub = new String(bytes, hibyte, 10, 20);
            assertEquals(s.substring(10, 30), sub);
        }
    }

    // Javadoc of String(byte[] ascii, int hibyte, int offset, int count): "Throws:
    // IndexOutOfBoundsException - If offset is negative, count is negative, or offset is greater than
    // ascii.length - count". The class Javadoc: "Unless otherwise noted, passing a null argument to a
    // constructor or method in this class will cause a NullPointerException to be thrown."
    @Test
    @SuppressWarnings("deprecation")
    public void hibyteConstructorChecksBounds() {
        var bytes = new byte[] { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, 1, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, Integer.MAX_VALUE, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> new String(bytes, 0, Integer.MIN_VALUE, 0));
        assertEquals(3, new String(bytes, 0, 0, 3).length());
        assertEquals(0, new String(bytes, 0, 3, 0).length());
        assertThrows(NullPointerException.class, () -> new String(null, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> new String((byte[]) null, 0));
    }

    // Javadoc of lines(): "A line terminator is one of the following: a line feed character "\n" (U+000A), a
    // carriage return character "\r" (U+000D), or a carriage return followed immediately by a line feed "\r\n"
    // (U+000D U+000A)." "A line is either a sequence of zero or more characters followed by a line terminator, or
    // it is a sequence of one or more characters followed by the end of the string. A line does not include the
    // line terminator."
    @Test
    public void linesExamples() {
        assertEquals(List.of(), "".lines().collect(Collectors.toList()));
        assertEquals(List.of(""), "\n".lines().collect(Collectors.toList()));
        assertEquals(List.of(""), "\r".lines().collect(Collectors.toList()));
        assertEquals(List.of(""), "\r\n".lines().collect(Collectors.toList()));
        assertEquals(List.of("", ""), "\n\r".lines().collect(Collectors.toList()));
        assertEquals(List.of("", ""), "\r\r".lines().collect(Collectors.toList()));
        assertEquals(List.of("", ""), "\n\n".lines().collect(Collectors.toList()));
        assertEquals(List.of("a", "b", "c"), "a\nb\r\nc".lines().collect(Collectors.toList()));
        assertEquals(List.of("a", "b", "c"), "a\rb\nc\r\n".lines().collect(Collectors.toList()));
        assertEquals(List.of("a", "", "b"), "a\r\rb".lines().collect(Collectors.toList()));
        assertEquals(List.of(" a ", "\t"), " a \r\n\t".lines().collect(Collectors.toList()));
        assertEquals(3, "a\n\nb\n".lines().count());
        assertEquals("b", "a\rb\r\nc".lines().skip(1).findFirst().get());
    }

    @Test
    public void linesProperty() {
        forEachLinesCase((text, lines) -> {
            assertEquals(lines, text.lines().collect(Collectors.toList()), () -> describe(text));
            assertEquals(lines.size(), text.lines().count(), () -> describe(text));
            // Partial consumption: the stream stops and resumes in the middle of the string
            var iter = text.lines().iterator();
            for (var line : lines) {
                assertTrue(iter.hasNext(), () -> describe(text));
                assertEquals(line, iter.next(), () -> describe(text));
            }
            assertFalse(iter.hasNext(), () -> describe(text));
            for (int k = 0; k <= lines.size(); ++k) {
                assertEquals(lines.subList(0, k), text.lines().limit(k).collect(Collectors.toList()),
                        () -> describe(text));
            }
        });
    }

    // Javadoc of indent(int): "If n > 0 then n spaces (U+0020) are inserted at the beginning of each line." "If n
    // < 0 then up to n white space characters are removed from the beginning of each line. If a given line does
    // not contain sufficient white space then all leading white space characters are removed. Each white space
    // character is treated as a single character." "If n == 0 then the line remains unchanged. However, line
    // terminators are still normalized." "Then, each line is suffixed with a line feed "\n" (U+000A)." "If this
    // string is empty, then an empty string is returned." White space is as defined by
    // Character.isWhitespace (via stripLeading/indexOfNonWhitespace).
    @Test
    public void indentExamples() {
        assertEquals("", "".indent(0));
        assertEquals("", "".indent(5));
        assertEquals("", "".indent(-5));
        assertEquals("  a\n  b\n", "a\nb".indent(2));
        assertEquals("a\n  b\n", "  a\n    b".indent(-2));
        assertEquals("a\nb\nc\n", "a\rb\r\nc".indent(0));
        assertEquals("a\n", "a\n".indent(0));
        assertEquals("  \n", "\n".indent(2));
        assertEquals("\n\n", " \r\t\r\n".indent(-1));
        assertEquals("x\n", "\t x".indent(-2));
        assertEquals(" x\n", "\t\u2003 x".indent(-2));
        assertEquals("\u00A0x\n", "\u00A0x".indent(-1));
        assertEquals("x \n", "\u000B\f\u001Cx ".indent(Integer.MIN_VALUE));
        assertEquals("x \n", "\u000B\f\u001Cx ".indent(-Integer.MAX_VALUE));
    }

    @Test
    public void indentProperty() {
        int[] ns = { 0, 1, 2, 3, -1, -2, -3, -100, -Integer.MAX_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE };
        forEachLinesCase((text, lines) -> {
            for (int n : ns) {
                var expected = new StringBuilder();
                for (var line : lines) {
                    int leading = 0;
                    while (leading < line.length() && Character.isWhitespace(line.charAt(leading))) {
                        leading++;
                    }
                    if (n > 0) {
                        expected.append(" ".repeat(n)).append(line);
                    } else {
                        expected.append(line.substring((int) Math.min(leading, -(long) n)));
                    }
                    expected.append('\n');
                }
                int m = n;
                assertEquals(expected.toString(), text.indent(n), () -> "n=" + m + ", " + describe(text));
            }
        });
    }

    // Javadoc of transform(Function): "This method allows the application of a function to this string. The
    // function should expect a single argument of String type and produce an R result."
    @Test
    public void transform() {
        assertEquals("xy", "x".transform(s -> s + "y"));
        assertEquals(Integer.valueOf(3), "abc".transform(String::length));
        var str = "abc";
        assertSame(str, str.transform(s -> s));
        assertNull("abc".transform(s -> null));
    }

    // Line contents: empty, plain, leading spaces and tabs, other Character.isWhitespace characters (EM SPACE,
    // VT, FF, FS), NBSP (not white space), trailing white space, white space only.
    private static final String[] LINE_CONTENTS = { "", "a", "  a", "\t a", " \t\u2003b", "\u00A0c",
            "\u000B\f\u001Cd e ", "   " };
    private static final String[] TERMINATORS = { "\n", "\r", "\r\n" };

    // Every string of 0 to 3 lines from LINE_CONTENTS, joined by every combination of terminators, with and
    // without each kind of trailing terminator. The consumer gets the text and the lines it was built from.
    private static void forEachLinesCase(java.util.function.BiConsumer<String, List<String>> consumer) {
        consumer.accept("", List.of());
        for (int count = 1; count <= 3; ++count) {
            int contentCombos = (int) Math.pow(LINE_CONTENTS.length, count);
            int terminatorCombos = (int) Math.pow(TERMINATORS.length, count - 1);
            for (int c = 0; c < contentCombos; ++c) {
                for (int t = 0; t < terminatorCombos; ++t) {
                    for (int trailing = -1; trailing < TERMINATORS.length; ++trailing) {
                        var lines = new java.util.ArrayList<String>();
                        var terminators = new java.util.ArrayList<String>();
                        int cc = c;
                        int tt = t;
                        for (int i = 0; i < count; ++i) {
                            lines.add(LINE_CONTENTS[cc % LINE_CONTENTS.length]);
                            cc /= LINE_CONTENTS.length;
                            if (i < count - 1) {
                                terminators.add(TERMINATORS[tt % TERMINATORS.length]);
                                tt /= TERMINATORS.length;
                            }
                        }
                        if (trailing >= 0) {
                            terminators.add(TERMINATORS[trailing]);
                        } else if (lines.get(count - 1).isEmpty()) {
                            // An empty last line with no terminator is not a line; the same text is covered
                            // by the case with one line fewer.
                            continue;
                        }
                        var sb = new StringBuilder();
                        boolean ambiguous = false;
                        for (int i = 0; i < count; ++i) {
                            sb.append(lines.get(i));
                            if (i < terminators.size()) {
                                // "\r" + "" + "\n..." reads as "\r\n": covered by another case
                                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r'
                                        && terminators.get(i).charAt(0) == '\n') {
                                    ambiguous = true;
                                }
                                sb.append(terminators.get(i));
                            }
                        }
                        if (!ambiguous) {
                            consumer.accept(sb.toString(), lines);
                        }
                    }
                }
            }
        }
    }

    private static String describe(String text) {
        var sb = new StringBuilder("text=\"");
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                sb.append("\\u").append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
