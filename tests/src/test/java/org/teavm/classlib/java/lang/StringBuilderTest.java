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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class StringBuilderTest {
    @Test
    public void integerAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(23);
        assertEquals("23", sb.toString());
    }

    @Test
    public void integerInserted() {
        StringBuilder sb = new StringBuilder("[]");
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
    public void largeIntegerAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(123456);
        assertEquals("123456", sb.toString());
    }

    @Test
    public void negativeIntegerAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(-23);
        assertEquals("-23", sb.toString());
    }

    @Test
    public void maxIntegerAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(2147483647);
        assertEquals("2147483647", sb.toString());
    }

    @Test
    public void longAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(23L);
        assertEquals("23", sb.toString());
    }

    @Test
    public void longAppended2() {
        StringBuilder sb = new StringBuilder();
        sb.append(2971215073L);
        assertEquals("2971215073", sb.toString());
    }

    @Test
    public void negativeLongAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(-23L);
        assertEquals("-23", sb.toString());
    }

    @Test
    public void largeLongAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(12345678901234L);
        assertEquals("12345678901234", sb.toString());
    }

    @Test
    public void maxLongAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(9223372036854775807L);
        assertEquals("9223372036854775807", sb.toString());
    }

    @Test
    public void floatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.234E25F);
        assertEquals("1.234E25", sb.toString());
    }

    @Test
    public void floatAppended2() {
        StringBuilder sb = new StringBuilder();
        sb.append(9.8765E30F);
        assertEquals("9.8765E30", sb.toString());
    }

    @Test
    public void negativeFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(-1.234E25F);
        assertEquals("-1.234E25", sb.toString());
    }

    @Test
    public void negativeFloatAppended2() {
        StringBuilder sb = new StringBuilder();
        sb.append(9.8765E30F);
        assertEquals("9.8765E30", sb.toString());
    }

    @Test
    public void maxFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(3.402823E38f);
        assertEquals("3.402823E38", sb.toString());
    }

    @Test
    public void smallFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.234E-25F);
        assertEquals("1.234E-25", sb.toString());
    }

    @Test
    public void smallFloatAppended2() {
        StringBuilder sb = new StringBuilder();
        sb.append(9.8764E-30F);
        assertEquals("9.8764E-30", sb.toString());
    }

    @Test
    public void negativeSmallFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(-1.234E-25F);
        assertEquals("-1.234E-25", sb.toString());
    }

    @Test
    public void negativeSmallFloatAppended2() {
        StringBuilder sb = new StringBuilder();
        sb.append(-9.8764E-30F);
        assertEquals("-9.8764E-30", sb.toString());
    }

    @Test
    public void minFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.17549E-38f);
        assertEquals("1.17549E-38", sb.toString());
    }

    @Test
    public void normalFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1200f);
        assertEquals("1200.0", sb.toString());
    }

    @Test
    public void normalSmallFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(0.023f);
        assertEquals("0.023", sb.toString());
    }

    @Test
    public void zeroFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(0f);
        assertEquals("0.0", sb.toString());
    }

    @Test
    public void oneFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1f);
        assertEquals("1.0", sb.toString());
    }

    @Test
    public void nanFloatAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(Float.NaN);
        assertEquals("NaN", sb.toString());
    }

    @Test
    public void positiveInfinityAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(Float.POSITIVE_INFINITY);
        assertEquals("Infinity", sb.toString());
    }

    @Test
    public void negativeInfinityAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(Float.NEGATIVE_INFINITY);
        assertEquals("-Infinity", sb.toString());
    }

    @Test
    public void doubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.23456789E150);
        assertEquals("1.23456789E150", sb.toString());
    }

    @Test
    public void powTenDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(10.0);
        assertEquals("10.0", sb.toString());
        sb.setLength(0);
        sb.append(20.0);
        assertEquals("20.0", sb.toString());
        sb.setLength(0);
        sb.append(100.0);
        assertEquals("100.0", sb.toString());
        sb.setLength(0);
        sb.append(1000.0);
        assertEquals("1000.0", sb.toString());
        sb.setLength(0);
        sb.append(0.1);
        assertEquals("0.1", sb.toString());
        sb.setLength(0);
        sb.append(0.01);
        assertEquals("0.01", sb.toString());
        sb.setLength(0);
        sb.append(1e20);
        assertEquals("1.0E20", sb.toString());
        sb.setLength(0);
        sb.append(2e20);
        assertEquals("2.0E20", sb.toString());
        sb.setLength(0);
        sb.append(1e-12);
        assertEquals("1.0E-12", sb.toString());
    }

    @Test
    public void negativeDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(-1.23456789E150);
        assertEquals("-1.23456789E150", sb.toString());
    }

    @Test
    public void smallDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.23456789E-150);
        assertEquals("1.23456789E-150", sb.toString());
    }

    @Test
    public void maxDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1.79769313486231E308);
        assertEquals("1.79769313486231E308", sb.toString());
    }

    @Test
    public void minDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(3E-308);
        assertEquals("3.0E-308", sb.toString());
    }

    @Test
    public void zeroDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(0);
        assertEquals("0", sb.toString());
    }

    @Test
    public void doubleInfinityAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(Double.POSITIVE_INFINITY);
        assertEquals("Infinity", sb.toString());
    }

    @Test
    public void doubleNaNAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(Double.NaN);
        assertEquals("NaN", sb.toString());
    }

    @Test
    public void normalDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(1200.0);
        assertEquals("1200.0", sb.toString());
    }

    @Test
    public void normalSmallDoubleAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(0.023);
        assertEquals("0.023", sb.toString());
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
    public void substringWithUpperBoundAtEndWorks() {
        assertEquals("23", "123".substring(1, 3));
    }
}
