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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
}
