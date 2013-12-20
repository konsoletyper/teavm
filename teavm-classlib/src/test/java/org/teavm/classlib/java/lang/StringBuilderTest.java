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

/**
 *
 * @author Alexey Andreev
 */
public class StringBuilderTest {
    @Test
    public void integerAppended() {
        StringBuilder sb = new StringBuilder();
        sb.append(23);
        assertEquals("23", sb.toString());
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
        sb.append(1.175494E-38f);
        assertEquals("1.175494E-38", sb.toString());
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
    public void appendsCodePoint() {
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(969356);
        assertEquals(56178, sb.charAt(0));
        assertEquals(56972, sb.charAt(1));
    }
}
