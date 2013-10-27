package org.teavm.classlib.org.junit;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class Assert {
    public static void assertTrue(boolean condition) {
        if (!condition) {
            fail();
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            fail();
        }
    }

    @GeneratedBy(AssertNativeGenerator.class)
    public static native void fail();

    public static void assertEquals(Object expected, Object actual) {
        if (expected != null ? !expected.equals(actual) : actual != null) {
            fail();
        }
    }

    public static void assertNotEquals(Object expected, Object actual) {
        if (expected != null ? expected.equals(actual) : actual == null) {
            fail();
        }
    }

    public static void assertNotNull(Object object) {
        if (object == null) {
            fail();
        }
    }

    public static void assertNull(Object object) {
        if (object != null) {
            fail();
        }
    }

    public static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            fail();
        }
    }

    public static void assertNotSame(Object expected, Object actual) {
        if (expected == actual) {
            fail();
        }
    }
}
