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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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

    @Test
    public void copyOfWorks() {
        testOf(new String[0], List.copyOf(new ArrayList<>()));
        testOf(new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a" },
                List.copyOf(Arrays.asList("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a")));

        try {
            // copyOf() must throw a NullPointerException on any 'null' element.
            List<String> listWithNull = new ArrayList<>(1);
            listWithNull.add(null);

            List.copyOf(listWithNull);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // ok
        }
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

    @Test
    public void testSequencedCollection() {
        List<String> list = List.of("0", "1", "2", "3", "4", "5", "6");
        List<String> reversed = list.reversed();
        assertEquals("1", reversed.get(5));
        Iterator<String> it = reversed.iterator();
        assertEquals("6", it.next());
        assertEquals("5", it.next());
        assertEquals("6", reversed.getFirst());
        assertEquals("0", reversed.getLast());
        ListIterator<String> lit = reversed.listIterator();
        assertFalse(lit.hasPrevious());
        assertTrue(lit.hasNext());
        assertEquals("6", lit.next());
        assertEquals("5", lit.next());
        assertEquals("5", lit.previous());
        lit = reversed.listIterator(2);
        assertEquals("4", lit.next());
        lit.previous();
        assertEquals("5", lit.previous());
        assertSame(list, reversed.reversed());
        List<String> subList = reversed.subList(3, 5);
        assertEquals("2", subList.getLast());
        assertEquals("3", subList.listIterator().next());
        StringBuilder sb = new StringBuilder();
        subList.forEach(sb::append);
        assertEquals("32", sb.toString());
        List<Integer> duplicates = List.of(0, 1, 2, 3, 2, 1, 0, 0).reversed();
        assertEquals(2, duplicates.indexOf(1));
        assertEquals(6, duplicates.lastIndexOf(1));
        try {
            list.removeFirst();
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        try {
            reversed.removeLast();
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }
}
