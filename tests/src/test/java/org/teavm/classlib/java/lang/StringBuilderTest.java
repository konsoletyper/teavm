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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class StringBuilderTest {
    @Test
    public void integerAppended() {
        var sb = new StringBuilder();
        sb.append(23)
                .append(" ").append(123456)
                .append(" ").append(-23)
                .append(" ").append(Integer.MAX_VALUE)
                .append(" ").append(Integer.MIN_VALUE);
        assertEquals("23 123456 -23 2147483647 -2147483648", sb.toString());
    }

    @Test
    public void integerInserted() {
        var sb = new StringBuilder("[]");
        sb.insert(1, 23);
        assertEquals("[23]", sb.toString());

        sb = new StringBuilder("[]");
        sb.insert(1, 10);
        assertEquals("[10]", sb.toString());

        sb = new StringBuilder("[]");
        sb.insert(1, 100);
        assertEquals("[100]", sb.toString());
    }

    @Test
    public void longAppended() {
        var sb = new StringBuilder();
        sb.append(23L)
                .append(" ").append(2971215073L)
                .append(" ").append(-23L)
                .append(" ").append(12345678901234L)
                .append(" ").append(Long.MAX_VALUE)
                .append(" ").append(Long.MIN_VALUE);
        assertEquals("23 2971215073 -23 12345678901234 9223372036854775807 -9223372036854775808", sb.toString());
    }

    @Test
    public void floatAppended() {
        var sb = new StringBuilder();
        sb.append(1.234E25F)
                .append(" ").append(9.8765E30F)
                .append(" ").append(-1.234E25F)
                .append(" ").append(-9.8765E30F)
                .append(" ").append(3.402823E38f)
                .append(" ").append(1.234E-25F)
                .append(" ").append(9.8764E-30F)
                .append(" ").append(-1.234E-25F)
                .append(" ").append(-9.8764E-30F)
                .append(" ").append(1.17549E-38f)
                .append(" ").append(1200f)
                .append(" ").append(0.023f)
                .append(" ").append(0f)
                .append(" ").append(-0.0f)
                .append(" ").append(1f)
                .append(" ").append(Float.NaN)
                .append(" ").append(Float.POSITIVE_INFINITY)
                .append(" ").append(Float.NEGATIVE_INFINITY);
        assertEquals("1.234E25 9.8765E30 -1.234E25 -9.8765E30 3.402823E38"
                + " 1.234E-25 9.8764E-30 -1.234E-25 -9.8764E-30 1.17549E-38"
                + " 1200.0 0.023 0.0 -0.0 1.0 NaN Infinity -Infinity", sb.toString());
    }

    @Test
    public void doubleAppended() {
        var sb = new StringBuilder();
        sb.append(1.23456789E150)
                .append(" ").append(10.0)
                .append(" ").append(20.0)
                .append(" ").append(100.0)
                .append(" ").append(1000.0)
                .append(" ").append(0.1)
                .append(" ").append(0.01)
                .append(" ").append(1e20)
                .append(" ").append(2e20)
                .append(" ").append(1e-12)
                .append(" ").append(-1.23456789E150)
                .append(" ").append(1.23456789E-150)
                .append(" ").append(1.79769313486231E308)
                .append(" ").append(3E-308)
                .append(" ").append(1200.0)
                .append(" ").append(0.023)
                .append(" ").append(0.0)
                .append(" ").append(-0.0)
                .append(" ").append(1.0)
                .append(" ").append(Double.NaN)
                .append(" ").append(Double.POSITIVE_INFINITY)
                .append(" ").append(Double.NEGATIVE_INFINITY);
        assertEquals("1.23456789E150 10.0 20.0 100.0 1000.0 0.1 0.01 1.0E20 2.0E20 1.0E-12"
                + " -1.23456789E150 1.23456789E-150 1.79769313486231E308 3.0E-308"
                + " 1200.0 0.023"
                + " 0.0 -0.0 1.0 NaN Infinity -Infinity", sb.toString());
    }

    @Test
    public void appendsCodePoint() {
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(969356);
        assertEquals(56178, sb.charAt(0));
        assertEquals(56972, sb.charAt(1));
    }

    @Test
    public void deletesRange() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; ++i) {
            sb.append((char) ('0' + i));
        }
        sb.delete(4, 6);
        assertEquals(8, sb.length());
        assertEquals('0', sb.charAt(0));
        assertEquals('3', sb.charAt(3));
        assertEquals('6', sb.charAt(4));
        assertEquals('9', sb.charAt(7));
    }

    @Test
    public void deletesNothing() {
        StringBuilder sb = new StringBuilder();
        sb.delete(0, 0);
        assertEquals(0, sb.length());
    }

    @Test
    public void replacesRangeWithSequenceOfSameLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; ++i) {
            sb.append((char) ('0' + i));
        }
        sb.replace(4, 6, "ab");
        assertEquals(10, sb.length());
        assertEquals('0', sb.charAt(0));
        assertEquals('3', sb.charAt(3));
        assertEquals('a', sb.charAt(4));
        assertEquals('6', sb.charAt(6));
        assertEquals('9', sb.charAt(9));
    }

    @Test
    public void replacesRangeWithShorterSequence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; ++i) {
            sb.append((char) ('0' + i));
        }
        sb.replace(4, 6, "a");
        assertEquals(9, sb.length());
        assertEquals('0', sb.charAt(0));
        assertEquals('3', sb.charAt(3));
        assertEquals('a', sb.charAt(4));
        assertEquals('6', sb.charAt(5));
        assertEquals('9', sb.charAt(8));
    }

    @Test
    public void replacesRangeWithLongerSequence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; ++i) {
            sb.append((char) ('0' + i));
        }
        sb.replace(4, 6, "abc");
        assertEquals(11, sb.length());
        assertEquals('0', sb.charAt(0));
        assertEquals('3', sb.charAt(3));
        assertEquals('a', sb.charAt(4));
        assertEquals('c', sb.charAt(6));
        assertEquals('6', sb.charAt(7));
        assertEquals('9', sb.charAt(10));
    }

    @Test
    public void searchedBackward() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; ++i) {
            sb.append((char) ('0' + i));
        }
        assertEquals(3, sb.lastIndexOf("345"));
        assertEquals(-1, sb.lastIndexOf("35"));
        assertEquals(1, sb.lastIndexOf("12", 3));
        assertEquals(-1, sb.lastIndexOf("67", 2));
        assertEquals(7, sb.lastIndexOf("78", 100));
    }

    @Test
    public void substringWorks() {
        assertEquals("23", new StringBuilder("123").substring(1, 3));
    }

    @Test
    public void indexOf() {
        StringBuilder sb = new StringBuilder();
        sb.append("12345");
        assertEquals(3, sb.indexOf("45"));
        assertEquals(-1, sb.indexOf("56"));
        assertEquals(0, sb.indexOf("12345"));
        assertEquals(0, sb.indexOf("123"));
    }

    @Test
    public void delete() {
        StringBuilder sb = new StringBuilder("abcdef");
        try {
            sb.delete(-1, 3);
            fail();
        } catch (StringIndexOutOfBoundsException e) {
            // ok
        }
        try {
            sb.delete(7, 8);
            fail();
        } catch (StringIndexOutOfBoundsException e) {
            // ok
        }
        sb.delete(6, 50);
        assertEquals("abcdef", sb.toString());
        sb.delete(5, 50);
        assertEquals("abcde", sb.toString());
        sb.delete(1, 4);
        assertEquals("ae", sb.toString());
    }

    @Test
    public void replace() {
        StringBuilder sb = new StringBuilder("abcdef");
        try {
            sb.replace(-1, 3, "h");
            fail();
        } catch (StringIndexOutOfBoundsException e) {
            // ok
        }
        try {
            sb.replace(7, 8, "h");
            fail();
        } catch (StringIndexOutOfBoundsException e) {
            // ok
        }
        sb.replace(6, 50, "g");
        assertEquals("abcdefg", sb.toString());
        sb.replace(6, 50, "h");
        assertEquals("abcdefh", sb.toString());
        sb.replace(1, 6, "g");
        assertEquals("agh", sb.toString());
        sb.replace(1, 1, "bc");
        assertEquals("abcgh", sb.toString());
    }

    @Test
    public void charStream() {
        assertEquals(0, new StringBuilder().chars().toArray().length);
        assertArrayEquals(new int[] {'A', 'B', 'C', '1', '2', '3'}, new StringBuilder("ABC123").chars().toArray());
    }

    @Test
    public void codePointsStream() {
        assertEquals(0, "".codePoints().toArray().length);
        assertArrayEquals(new int[] {'A', 'B', 'C', '1', '2', '3'}, new StringBuilder("ABC123").chars().toArray());

        assertArrayEquals(new int[] { 969356 }, new StringBuilder().append(new char[] { (char) 56178, (char) 56972 })
                .codePoints().toArray());
        assertArrayEquals(new int[] { 56972, 56178 }, new StringBuilder().append(
                new char[] { (char) 56972, (char) 56178 }).codePoints().toArray());
        assertArrayEquals(new int[] { 56178 }, new StringBuilder().append((char) 56178).codePoints().toArray());
    }

    // StringBuilder/StringBuffer.append(CharSequence s): "If s is null, then the four characters "null" are
    // appended." append(StringBuffer sb): "If sb is null, then the four characters "null" are appended."
    // insert(int dstOffset, CharSequence s): "If s is null, then the four characters "null" are inserted into
    // this sequence."
    @Test
    public void nullCharSequenceAppendedAsNull() {
        CharSequence nullSequence = null;
        assertEquals("abnull", new StringBuilder("ab").append(nullSequence).toString());
        assertEquals("abnull", new StringBuilder("ab").append((StringBuffer) null).toString());
        assertEquals("null", new StringBuilder().append(nullSequence).toString());
        assertEquals("abnullnull", new StringBuilder("ab").append(nullSequence).append(nullSequence).toString());

        assertEquals("abnull", new StringBuffer("ab").append(nullSequence).toString());
        assertEquals("abnull", new StringBuffer("ab").append((StringBuffer) null).toString());
        assertEquals("null", new StringBuffer().append(nullSequence).toString());
    }

    @Test
    public void nullCharSequenceInsertedAsNull() {
        CharSequence nullSequence = null;
        for (int index = 0; index <= 2; ++index) {
            String expected = "ab".substring(0, index) + "null" + "ab".substring(index);
            assertEquals(expected, new StringBuilder("ab").insert(index, nullSequence).toString());
            assertEquals(expected, new StringBuffer("ab").insert(index, nullSequence).toString());
        }
        // "IndexOutOfBoundsException - if the offset is invalid."
        assertThrows(IndexOutOfBoundsException.class, () -> new StringBuilder("ab").insert(-1, nullSequence));
        assertThrows(IndexOutOfBoundsException.class, () -> new StringBuilder("ab").insert(3, nullSequence));
        assertThrows(IndexOutOfBoundsException.class, () -> new StringBuffer("ab").insert(3, nullSequence));
    }

    // append(CharSequence s, int start, int end): "If s is null, then this method appends characters as if the s
    // parameter was a sequence which contains the four characters "null"." It throws IndexOutOfBoundsException
    // "if start is negative, or start is greater than end or end is greater than s.length()".
    @Test
    public void nullCharSequenceRangeAppendedAsNull() {
        CharSequence nullSequence = null;
        for (int start = -1; start <= 5; ++start) {
            for (int end = -1; end <= 5; ++end) {
                int s = start;
                int e = end;
                String expected = s >= 0 && s <= e && e <= 4 ? "ab" + "null".substring(s, e) : "IOOBE";
                String where = "start=" + s + ", end=" + e;
                assertEquals(expected, outcome(() -> new StringBuilder("ab").append(nullSequence, s, e)), where);
                assertEquals(expected, outcome(() -> new StringBuffer("ab").append(nullSequence, s, e)), where);
            }
        }
    }

    // insert(int dstOffset, CharSequence s, int start, int end): "If s is null, then this method inserts characters
    // as if the s parameter was a sequence containing the four characters "null"." It throws
    // IndexOutOfBoundsException "if dstOffset is negative or greater than this.length(), or start or end are
    // negative, or start is greater than end or end is greater than s.length()".
    @Test
    public void nullCharSequenceRangeInsertedAsNull() {
        CharSequence nullSequence = null;
        for (int index = -1; index <= 3; ++index) {
            for (int start = -1; start <= 5; ++start) {
                for (int end = -1; end <= 5; ++end) {
                    int i = index;
                    int s = start;
                    int e = end;
                    String expected = i >= 0 && i <= 2 && s >= 0 && s <= e && e <= 4
                            ? "ab".substring(0, i) + "null".substring(s, e) + "ab".substring(i)
                            : "IOOBE";
                    String where = "index=" + i + ", start=" + s + ", end=" + e;
                    assertEquals(expected, outcome(() -> new StringBuilder("ab").insert(i, nullSequence, s, e)),
                            where);
                    assertEquals(expected, outcome(() -> new StringBuffer("ab").insert(i, nullSequence, s, e)),
                            where);
                }
            }
        }
    }

    // Known gap, not fixed here: the range inserts don't check dstOffset. insert(int dstOffset, CharSequence s,
    // int start, int end) throws IndexOutOfBoundsException "if dstOffset is negative or greater than
    // this.length()", and insert(int index, char[] str, int offset, int len) throws StringIndexOutOfBoundsException
    // "if index is negative or greater than length()".
    @Test
    @Disabled("insert(int, CharSequence, int, int) and insert(int, char[], int, int) don't check the insert index")
    public void rangeInsertRejectsInvalidIndex() {
        for (int index : new int[] { -1, 3, 4 }) {
            int i = index;
            assertEquals("IOOBE", outcome(() -> new StringBuilder("ab").insert(i, "xy", 0, 1)), "index=" + i);
            assertEquals("IOOBE", outcome(() -> new StringBuilder("ab").insert(i, new char[] { 'x' }, 0, 1)),
                    "index=" + i);
            assertEquals("IOOBE", outcome(() -> new StringBuffer("ab").insert(i, "xy", 0, 1)), "index=" + i);
        }
    }

    private static String outcome(Supplier<CharSequence> action) {
        try {
            return action.get().toString();
        } catch (IndexOutOfBoundsException ex) {
            return "IOOBE";
        }
    }
}
