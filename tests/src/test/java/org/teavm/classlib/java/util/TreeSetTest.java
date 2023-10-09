/*
 *  Copyright 2023 ihromant.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class TreeSetTest {
    @Test
    public void testSortedSetReadOnly() {
        SortedSet<String> set = new TreeSet<>(List.of("1", "2", "3", "4", "5", "6"));
        SortedSet<String> reversed = set.reversed();
        Iterator<String> it = reversed.iterator();
        assertEquals("6", it.next());
        assertEquals("5", it.next());
        assertEquals("6", reversed.getFirst());
        assertEquals("1", reversed.getLast());
        SortedSet<String> subset = reversed.subSet("3", "1");
        it = subset.iterator();
        assertTrue(it.hasNext());
        assertEquals("3", it.next());
        assertTrue(it.hasNext());
        assertEquals("2", it.next());
        try {
            assertFalse(it.hasNext());
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        subset = reversed.headSet("4");
        it = subset.iterator();
        assertTrue(it.hasNext());
        assertEquals("6", it.next());
        assertTrue(it.hasNext());
        assertEquals("5", it.next());
        try {
            assertFalse(it.hasNext());
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        subset = reversed.tailSet("2");
        it = subset.iterator();
        assertTrue(it.hasNext());
        assertEquals("2", it.next());
        assertTrue(it.hasNext());
        assertEquals("1", it.next());
        try {
            assertFalse(it.hasNext());
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    @Test
    public void testSequencedCollectionIterator() {
        SortedSet<String> set = new TreeSet<>(List.of("a", "b", "c", "d")).reversed();
        Iterator<String> it = set.iterator();
        assertEquals("d", it.next());
        assertEquals("c", it.next());
        it.remove();
        assertArrayEquals(new String[] { "d", "b", "a" }, set.toArray());
        set = new TreeSet<>(List.of("a", "b", "c", "d")).reversed();
        it = set.iterator();
        assertEquals("d", it.next());
        assertEquals("c", it.next());
        it.remove();
        assertEquals("b", it.next());
        it.remove();
        assertEquals("a", it.next());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        assertArrayEquals(new String[] { "d", "a" }, set.toArray());
    }

    @Test
    public void sequenceCollectionMethodsOnEmpty() {
        var empty = new TreeSet<>();

        try {
            empty.getFirst();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            empty.getLast();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }

        try {
            empty.removeFirst();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            empty.removeLast();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            empty.addFirst("r");
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        try {
            empty.addLast("r");
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

    @Test
    public void testMutationsIterator() {
        SortedSet<String> set = new TreeSet<>(List.of("a", "b", "c", "d"));
        set.add("e");
        set.add("f");
        Iterator<String> it = set.iterator();
        assertEquals("a", it.next());
        it.remove();
        assertEquals("b", it.next());
        it.remove();
        assertEquals("c", it.next());
        it.remove();
        assertEquals("d", it.next());
        it.remove();
        assertEquals("e", it.next());
        it.remove();
        assertEquals("f", it.next());
        it.remove();
        assertTrue(set.isEmpty());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        set = new TreeSet<>(List.of("a", "b", "c", "d"));
        set.add("e");
        set.add("f");
        it = set.reversed().iterator();
        assertEquals("f", it.next());
        it.remove();
        assertEquals("e", it.next());
        it.remove();
        assertEquals("d", it.next());
        it.remove();
        assertEquals("c", it.next());
        it.remove();
        assertEquals("b", it.next());
        it.remove();
        assertEquals("a", it.next());
        it.remove();
        assertTrue(set.isEmpty());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
    }
}
