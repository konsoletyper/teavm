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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class ClassTest {
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
