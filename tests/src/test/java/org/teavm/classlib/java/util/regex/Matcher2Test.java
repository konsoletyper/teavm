/*
 *  Copyright 2015 Alexey Andreev.
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

/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SuppressWarnings("nls")
public class Matcher2Test {
    @Test
    public void test_toString() {
        Pattern p = Pattern.compile("foo");
        Matcher m = p.matcher("bar");
        assertNotNull(m.toString());
    }

    @Test
    public void testErrorConditions() throws PatternSyntaxException {
        // Test match cursors in absence of a match
        Pattern p = Pattern.compile("foo");
        Matcher m = p.matcher("bar");
        assertFalse(m.matches());

        try {
            m.start();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.end();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.group();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.start(1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.end(1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.group(1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        // regression test for HARMONY-2418
        try {
            m.usePattern(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // PASSED
        }
    }

    @Test
    public void testErrorConditions2() throws PatternSyntaxException {
        // Test match cursors in absence of a match
        Pattern p = Pattern.compile("(foo[0-9])(bar[a-z])");
        Matcher m = p.matcher("foo1barzfoo2baryfoozbar5");

        assertTrue(m.find());
        assertEquals(0, m.start());
        assertEquals(8, m.end());
        assertEquals(0, m.start(1));
        assertEquals(4, m.end(1));
        assertEquals(4, m.start(2));
        assertEquals(8, m.end(2));

        try {
            m.start(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.end(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.group(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.start(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.end(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.group(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        assertTrue(m.find());
        assertEquals(8, m.start());
        assertEquals(16, m.end());
        assertEquals(8, m.start(1));
        assertEquals(12, m.end(1));
        assertEquals(12, m.start(2));
        assertEquals(16, m.end(2));

        try {
            m.start(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.end(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.group(3);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.start(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.end(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.group(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        assertFalse(m.find());

        try {
            m.start(3);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.end(3);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.group(3);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.start(-1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.end(-1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            m.group(-1);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    /*
     * Regression test for HARMONY-997
     */
    @Test
    public void testReplacementBackSlash() {
        String str = "replace me";
        String replacedString = "me";
        String substitutionString = "\\";
        Pattern pat = Pattern.compile(replacedString);
        Matcher mat = pat.matcher(str);
        try {
            mat.replaceAll(substitutionString);
            fail("IndexOutOfBoundsException should be thrown");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // ok
        }
    }
}
