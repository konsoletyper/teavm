package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
class TStringBuilderTests {
    @Test
    public void integerAppended() {
        TStringBuilder sb = new TStringBuilder();
        sb.append(23);
        assertEquals("23", sb.toString());
    }
}
