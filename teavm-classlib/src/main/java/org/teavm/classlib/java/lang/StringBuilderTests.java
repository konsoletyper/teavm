package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class StringBuilderTests {
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
    public void appendsCodePoint() {
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(969356);
        assertEquals(56178, sb.charAt(0));
        assertEquals(56972, sb.charAt(1));
    }
}
