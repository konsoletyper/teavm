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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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
        
        assertThrows(NullPointerException.class, () -> Map.of(null, "q"));
        assertThrows(NullPointerException.class, () -> Map.of("q", null));
        assertThrows(NullPointerException.class, () -> Map.of("q", "w", "e", null));
        assertThrows(NullPointerException.class, () -> Map.of("q", "w", null, "e"));
        assertThrows(NullPointerException.class, () -> Map.of("q", "w", "e", "r", "t", null));
        assertThrows(NullPointerException.class, () -> Map.of("q", "w", "e", "r", null, "t"));
        
        assertThrows(IllegalArgumentException.class, () -> Map.of("q", "w", "q", "e"));
        assertThrows(IllegalArgumentException.class, () -> Map.of("q", "w", "e", "r", "e", "t"));
    }

    @Test
    public void copyOfWorks() {
        testOf(new String[0], Map.copyOf(new HashMap<>()));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                Map.copyOf(
                        Map.ofEntries(Map.entry("q", 0), Map.entry("w", 1), Map.entry("e", 2), Map.entry("r", 3),
                        Map.entry("t", 4), Map.entry("y", 5), Map.entry("u", 6), Map.entry("i", 7), Map.entry("o", 8),
                        Map.entry("p", 9), Map.entry("a", 10))));
    }
    
    @Test
    public void copyOfOptimized() {
        Map<String, Integer> mapCopy1 = Map.copyOf(Map.of("q", 0, "w", 1, "e", 2));
        Map<String, Integer> mapCopy2 = Map.copyOf(mapCopy1);

        assertSame(mapCopy1, mapCopy2, "Must not create copies of immutable collections");
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
            assertTrue(actual.containsKey(key), "containsKey returns true for existing elements");
            assertTrue(actual.containsValue(i), "containsValue returns true for existing elements");
            assertTrue(actual.entrySet().contains(Map.entry(key, i)), "contains returns true for existing elements");
        }

        assertFalse(actual.containsKey("*"), "containsKey return false for non-existing element");
        assertFalse(actual.containsValue(-1), "containsValue return false for non-existing element");
        for (String key : expected) {
            assertFalse(actual.entrySet().contains(Map.entry(key, -1)),
                    "contains return false for non-existing element");
        }

        assertEquals(expected.length == 0, actual.isEmpty(), "isEmpty works properly");

        String[] expectedCopy = expected.clone();
        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            boolean found = false;
            for (int i = 0; i < expectedCopy.length; ++i) {
                if (entry.getKey().equals(expectedCopy[i])) {
                    assertEquals((Object) i, entry.getValue(), "Strange value of entry.getValue()");
                    expectedCopy[i] = null;
                    found = true;
                    break;
                }
            }

            assertTrue(found, "iterator returned strange value");
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
            assertNull(e, "Iterator did not return all of expected elements");
        }
    }

    @Test
    public void testReplaceAll() {
        Map<String, Integer> base = Map.of("a", 1, "b", 2);
        Map<String, Integer> hashMap = new HashMap<>(base);
        Map<String, Integer> treeMap = new TreeMap<>(base);
        hashMap.replaceAll((k, v) -> v * 10);
        treeMap.replaceAll((k, v) -> v * 10);
        assertEquals(Map.of("a", 10, "b", 20), hashMap);
        assertEquals(Map.of("a", 10, "b", 20), treeMap);
    }
}
