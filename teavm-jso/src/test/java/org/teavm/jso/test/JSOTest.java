package org.teavm.jso.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class JSOTest {
    @Test
    public void complexConstructorParenthesized() {
        RegExp regexp = getWindow().createRegExp(".");
        assertEquals(".", regexp.getSource());
    }

    private static Window getWindow() {
        return (Window)JS.getGlobal();
    }
}
