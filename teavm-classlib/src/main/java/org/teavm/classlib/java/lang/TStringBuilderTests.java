package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
class TStringBuilderTests {
    public void integerAppended() {
        TStringBuilder sb = new TStringBuilder();
        sb.append(23);
        assertEquals("23", sb.toString());
    }

    @Test
    public void largeIntegerAppended() {
        TStringBuilder sb = new TStringBuilder();
        sb.append(123456);
        assertEquals("123456", sb.toString());
    }

    @Test
    public void negativeIntegerAppended() {
        TStringBuilder sb = new TStringBuilder();
        sb.append(-23);
        assertEquals("-23", sb.toString());
    }

    @Test
    public void maxIntegerAppended() {
        TStringBuilder sb = new TStringBuilder();
        sb.append(2147483647);
        assertEquals("2147483647", sb.toString());
    }
}
