/*
 *  Copyright 2014 Alexey Andreev.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class BooleanTest {
    @Test
    public void parsesBoolean() {
        assertEquals(true, Boolean.parseBoolean("TruE"));
        assertEquals(false, Boolean.parseBoolean("False"));
        assertEquals(false, Boolean.parseBoolean("True15"));
    }

    @Test
    public void getFromSystemProperty() {
        System.setProperty("test.foo", "true");
        System.setProperty("test.bar", "false");

        assertTrue(Boolean.getBoolean("test.foo"));
        assertFalse(Boolean.getBoolean("test.bar"));
        assertFalse(Boolean.getBoolean("test.baz"));
        assertFalse(Boolean.getBoolean(null));
    }

    @Test
    public void logicalMethods() {
        assertTrue(Boolean.logicalAnd(true, true));
        assertFalse(Boolean.logicalAnd(false, true));
        assertFalse(Boolean.logicalAnd(true, false));
        assertFalse(Boolean.logicalAnd(false, false));

        assertTrue(Boolean.logicalOr(true, true));
        assertTrue(Boolean.logicalOr(false, true));
        assertTrue(Boolean.logicalOr(true, false));
        assertFalse(Boolean.logicalOr(false, false));

        assertFalse(Boolean.logicalXor(true, true));
        assertTrue(Boolean.logicalXor(false, true));
        assertTrue(Boolean.logicalXor(true, false));
        assertFalse(Boolean.logicalXor(false, false));
    }

    @Test
    public void hashCodeTest() {
        assertEquals(Boolean.hashCode(true), Boolean.TRUE.hashCode());
        assertEquals(Boolean.hashCode(false), Boolean.FALSE.hashCode());
    }

    @Test
    public void compares() {
        assertEquals(0, Boolean.compare(false, false));
        assertEquals(-1, Boolean.compare(false, true));
        assertEquals(1, Boolean.compare(true, false));
        assertEquals(0, Boolean.compare(true, true));
    }
}
