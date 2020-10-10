/*
 *  Copyright 2020 konsoletyper.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class ListTest {
    @Test
    public void of() {
        testOf(new String[0], List.of());
        testOf(new String[] { "q" }, List.of("q"));
        testOf(new String[] { "q", "w" }, List.of("q", "w"));
        testOf(new String[] { "q", "w", "e" }, List.of("q", "w", "e"));
        testOf(new String[] { "q", "w", "e", "r" }, List.of("q", "w", "e", "r"));
        testOf(new String[] { "q", "w", "e", "r", "t" }, List.of("q", "w", "e", "r", "t"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y" }, List.of("q", "w", "e", "r", "t", "y"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u" }, List.of("q", "w", "e", "r", "t", "y", "u"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i" },
                List.of("q", "w", "e", "r", "t", "y", "u", "i"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o" },
                List.of("q", "w", "e", "r", "t", "y", "u", "i", "o"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
                List.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                List.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a"));
    }

    private void testOf(String[] expected, List<String> actual) {
        if (actual.size() != expected.length) {
            fail("Expected size is " + expected.length + ", actual size is " + actual.size());
        }

        for (int i = 0; i < expected.length; ++i) {
            assertEquals("Element #" + i, expected[i], actual.get(i));
        }

        try {
            actual.get(-1);
            fail("get out of bounds does not throw exception");
        } catch (IndexOutOfBoundsException e) {
            // ok;
        }

        try {
            actual.get(expected.length);
            fail("get out of bounds does not throw exception");
        } catch (IndexOutOfBoundsException e) {
            // ok;
        }

        try {
            actual.set(0, "1");
            fail("set should not work");
        } catch (UnsupportedOperationException e) {
            // ok;
        }

        try {
            actual.remove(0);
            fail("remove should not work");
        } catch (UnsupportedOperationException e) {
            // ok;
        }

        try {
            actual.add("2");
            fail("add should not work");
        } catch (UnsupportedOperationException e) {
            // ok;
        }

        try {
            actual.clear();
            fail("clear should not work");
        } catch (UnsupportedOperationException e) {
            // ok;
        }

        for (int i = 0; i < expected.length; ++i) {
            assertEquals("indexOf of element #" + i + " is correct", i, actual.indexOf(expected[i]));
        }

        for (String value : expected) {
            assertTrue("contains returns true for existing elements", actual.contains(value));
        }

        assertFalse("contains return false for non-existing element", actual.contains("*"));

        assertEquals("isEmpty works properly", expected.length == 0, actual.isEmpty());
    }
}
