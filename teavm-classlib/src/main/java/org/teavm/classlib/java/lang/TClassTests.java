package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
class TClassTests {
    @Test
    public void classNameEvaluated() {
        assertEquals("java.lang.Object", Object.class.getName());
    }

    @Test
    public void objectClassNameEvaluated() {
        assertEquals("java.lang.Object", new Object().getClass().getName());
    }

    @Test
    public void objectClassConsideredNotArray() {
        assertFalse(Object.class.isArray());
    }

    @Test
    public void arrayClassConsideredArray() {
        assertTrue(Object[].class.isArray());
    }

    @Test
    public void arrayComponentTypeDetected() {
        assertEquals(Object.class, Object[].class.getComponentType());
    }

    @Test
    public void arrayOfArraysComponentTypeDetected() {
        assertEquals(Object[].class, Object[][].class.getComponentType());
    }

    @Test
    public void nonArrayComponentTypeIsNull() {
        assertNull(Object.class.getComponentType());
    }
}
