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
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class MapTest {
    @Test
    public void of() {
        testOf(new String[0], Map.of());
        testOf(new String[] { "q" }, Map.of("q", 0));
        testOf(new String[] { "q", "w" }, Map.of("q", 0, "w", 1));
        testOf(new String[] { "q", "w", "e" }, Map.of("q", 0, "w", 1, "e", 2));
        testOf(new String[] { "q", "w", "e", "r" }, Map.of("q", 0, "w", 1, "e", 2, "r", 3));
        testOf(new String[] { "q", "w", "e", "r", "t" }, Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4));
        testOf(new String[] { "q", "w", "e", "r", "t", "y" }, Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4, "y", 5));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u" },
                Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4, "y", 5, "u", 6));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i" },
                Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4, "y", 5, "u", 6, "i", 7));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o" },
                Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4, "y", 5, "u", 6, "i", 7, "o", 8));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
                Map.of("q", 0, "w", 1, "e", 2, "r", 3, "t", 4, "y", 5, "u", 6, "i", 7, "o", 8, "p", 9));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                Map.ofEntries(Map.entry("q", 0), Map.entry("w", 1), Map.entry("e", 2), Map.entry("r", 3),
                        Map.entry("t", 4), Map.entry("y", 5), Map.entry("u", 6), Map.entry("i", 7), Map.entry("o", 8),
                        Map.entry("p", 9), Map.entry("a", 10)));
    }

    private void testOf(String[] expected, Map<String, Integer> actual) {
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
            actual.put("*", -1);
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

        for (int i = 0; i < expected.length; i++) {
            String key = expected[i];
            assertTrue("containsKey returns true for existing elements", actual.containsKey(key));
            assertTrue("containsValue returns true for existing elements", actual.containsValue(i));
            assertTrue("contains returns true for existing elements", actual.entrySet().contains(Map.entry(key, i)));
        }

        assertFalse("containsKey return false for non-existing element", actual.containsKey("*"));
        assertFalse("containsValue return false for non-existing element", actual.containsValue(-1));
        for (String key : expected) {
            assertFalse("contains return false for non-existing element",
                    actual.entrySet().contains(Map.entry(key, -1)));
        }

        assertEquals("isEmpty works properly", expected.length == 0, actual.isEmpty());

        String[] expectedCopy = expected.clone();
        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            boolean found = false;
            for (int i = 0; i < expectedCopy.length; ++i) {
                if (entry.getKey().equals(expectedCopy[i])) {
                    assertEquals("Strange value of entry.getValue()", (Object) i, entry.getValue());
                    expectedCopy[i] = null;
                    found = true;
                    break;
                }
            }

            assertTrue("iterator returned strange value", found);
        }

        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            try {
                entry.setValue(-1);
                fail("Entries should be immutable");
            } catch (UnsupportedOperationException e) {
                // ok
            }
        }

        for (String e : expectedCopy) {
            assertNull("Iterator did not return all of expected elements", e);
        }
    }
}
