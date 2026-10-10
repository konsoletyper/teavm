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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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
        
        assertThrows(NullPointerException.class, () -> Set.of("q", null));
        assertThrows(NullPointerException.class, () -> Set.of(null, "q"));
        assertThrows(NullPointerException.class, () -> Set.of(null));
        assertThrows(NullPointerException.class, () -> Set.of(null, null, null));
        assertThrows(NullPointerException.class, () -> Set.of(null, "q", "w"));
        assertThrows(NullPointerException.class, () -> Set.of("q", "w", null));
    }

    @Test
    public void copyOfWorks() {
        testOf(new String[0], Set.copyOf(new HashSet<>()));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                Set.copyOf(Arrays.asList("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a")));
        // Duplicates must be silently removed by copyOf(). Unlike of() where they throw an exception.
        testOf(new String[] { "q", "e", "r", "u", "i", "o", "p" },
                Set.copyOf(Arrays.asList("q", "q", "e", "r", "q", "q", "u", "i", "o", "p", "q")));

        try {
            // copyOf() must throw a NullPointerException on any 'null' element.
            Set.copyOf(Arrays.asList("q", "q", "e", "r", "q", "q", "u", "i", "o", "p", "q", null));
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // ok
        }
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
            assertTrue(actual.contains(value), "contains returns true for existing elements");
        }

        assertFalse(actual.contains("*"), "contains return false for non-existing element");

        assertEquals(expected.length == 0, actual.isEmpty(), "isEmpty works properly");

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

            assertTrue(found, "iterator returned strange value");
        }

        for (String e : expectedCopy) {
            assertNull(e, "Iterator did not return all of expected elements");
        }
    }

    @Test
    public void hashCodeTest() {
        Set<String> a = new LinkedHashSet<>();
        a.add("foo");
        a.add("bar");
        Set<String> b = new LinkedHashSet<>();
        b.add("bar");
        b.add("foo");

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void immutableSetRejectsNullLookup() {
        for (Set<String> set : List.of(Set.<String>of(), Set.of("a"), Set.of("a", "b"), Set.of("a", "b", "c"))) {
            assertThrows(NullPointerException.class, () -> set.contains(null));
        }
    }
}
