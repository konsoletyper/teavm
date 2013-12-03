package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class StringBuilderTests {
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
}
