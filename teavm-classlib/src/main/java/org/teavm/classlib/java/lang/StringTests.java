package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class StringTests {
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
}
