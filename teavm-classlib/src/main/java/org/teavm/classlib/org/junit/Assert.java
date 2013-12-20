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

    public static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            fail();
        }
    }

    public static void assertNotEquals(long expected, long actual) {
        if (expected == actual) {
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
