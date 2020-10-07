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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class SetTest {
    @Test
    public void of() {
        testOf(new String[0], Set.of());
        testOf(new String[] { "q" }, Set.of("q"));
        testOf(new String[] { "q", "w" }, Set.of("q", "w"));
        testOf(new String[] { "q", "w", "e" }, Set.of("q", "w", "e"));
        testOf(new String[] { "q", "w", "e", "r" }, Set.of("q", "w", "e", "r"));
        testOf(new String[] { "q", "w", "e", "r", "t" }, Set.of("q", "w", "e", "r", "t"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y" }, Set.of("q", "w", "e", "r", "t", "y"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u" }, Set.of("q", "w", "e", "r", "t", "y", "u"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i" },
                Set.of("q", "w", "e", "r", "t", "y", "u", "i"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o" },
                Set.of("q", "w", "e", "r", "t", "y", "u", "i", "o"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
                Set.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                Set.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a"));

        expectIAE(() -> Set.of("q", "q"));
        expectIAE(() -> Set.of("q", "w", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "y", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "y", "u", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "y", "u", "i", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "q"));
        expectIAE(() -> Set.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "q"));
    }

    private void expectIAE(Runnable r) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private void testOf(String[] expected, Set<String> actual) {
        if (actual.size() != expected.length) {
            fail("Expected size is " + expected.length + ", actual size is " + actual.size());
        }

        try {
            actual.remove("*");
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

        for (String value : expected) {
            assertTrue("contains returns true for existing elements", actual.contains(value));
        }

        assertFalse("contains return false for non-existing element", actual.contains("*"));

        assertEquals("isEmpty works properly", expected.length == 0, actual.isEmpty());

        String[] expectedCopy = expected.clone();
        for (String elem : actual) {
            boolean found = false;
            for (int i = 0; i < expectedCopy.length; ++i) {
                if (elem.equals(expectedCopy[i])) {
                    expectedCopy[i] = null;
                    found = true;
                    break;
                }
            }

            assertTrue("iterator returned strange value", found);
        }

        for (String e : expectedCopy) {
            assertNull("Iterator did not return all of expected elements", e);
        }
    }
}
