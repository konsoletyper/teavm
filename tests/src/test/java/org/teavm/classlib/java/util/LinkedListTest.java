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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class LinkedListTest {
    @Test
    public void emptyListCreated() {
        LinkedList<String> list = new LinkedList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.iterator().hasNext());
    }

    @Test
    public void elementAdded() {
        LinkedList<String> list = new LinkedList<>();
        list.add("foo");
        Iterator<String> iter = list.iterator();
        assertEquals("foo", iter.next());
        assertEquals(1, list.size());
        assertFalse(iter.hasNext());
    }

    @Test
    public void elementRetrievedByIndex() {
        LinkedList<String> list = new LinkedList<>();
        list.add("foo");
        list.add("bar");
        list.add("baz");
        assertEquals("foo", list.get(0));
        assertEquals("bar", list.get(1));
        assertEquals("baz", list.get(2));
    }

    @Test
    public void listIteratorPositioned() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(1);
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals("2", iter.next());
    }

    @Test
    public void listIteratorMoved() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(1);
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertEquals("a", iter.next());
        assertEquals("a", iter.previous());
        assertEquals("3", iter.previous());
        assertEquals(2, iter.nextIndex());
        assertEquals(1, iter.previousIndex());
    }

    @Test(expected = NoSuchElementException.class)
    public void listInteratorCantMoveBeyondLowerBound() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(1);
        assertEquals("1", iter.previous());
        iter.previous();
    }

    @Test(expected = NoSuchElementException.class)
    public void listInteratorCantMoveBeyondUpperBound() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(4);
        assertEquals("b", iter.next());
        iter.next();
    }

    @Test
    public void listIteratorRemovesItem() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(2);
        assertEquals("3", iter.next());
        iter.remove();
        assertEquals(2, iter.nextIndex());
        assertEquals("a", iter.next());
        assertArrayEquals(new String[] { "1", "2", "a", "b" }, list.toArray(new String[0]));
        assertEquals(4, list.size());
    }

    @Test
    public void listIteratorAddsItem() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(2);
        iter.add("*");
        assertEquals("3", iter.next());
        assertArrayEquals(new String[] { "1", "2", "*", "3", "a", "b" }, list.toArray(new String[0]));
    }

    @Test
    public void listIteratorReplacesItem() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(2);
        iter.next();
        iter.set("*");
        assertEquals("a", iter.next());
        assertArrayEquals(new String[] { "1", "2", "*", "a", "b" }, list.toArray(new String[0]));
    }

    @Test
    public void listIteratorRemovesPreviousItem() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(2);
        iter.previous();
        iter.remove();
        assertEquals(1, iter.nextIndex());
        assertEquals("3", iter.next());
        assertEquals(4, list.size());
    }

    @Test(expected = IllegalStateException.class)
    public void freshListIteratorWithOffsetDoesNotAllowRemoval() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        ListIterator<String> iter = list.listIterator(2);
        iter.remove();
    }

    @Test
    public void addsToTail() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        list.addLast("*");
        assertArrayEquals(new String[] { "1", "2", "3", "a", "b", "*" }, list.toArray(new String[0]));
    }

    @Test
    public void addsToHead() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        list.addFirst("*");
        assertArrayEquals(new String[] { "*", "1", "2", "3", "a", "b" }, list.toArray(new String[0]));
    }

    @Test
    public void removesFromTail() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        assertEquals("b", list.removeLast());
        assertEquals(4, list.size());
        assertEquals("a", list.getLast());
        Iterator<String> iter = list.iterator();
        assertEquals("1", iter.next());
        iter.next();
        iter.next();
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void removesFromHead() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "a", "b"));
        assertEquals("1", list.removeFirst());
        assertEquals(4, list.size());
        assertEquals("2", list.getFirst());
        Iterator<String> iter = list.descendingIterator();
        assertEquals("b", iter.next());
        iter.next();
        iter.next();
        assertEquals("2", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void removesFirstOccurrence() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "1", "2"));
        assertFalse(list.removeFirstOccurrence("*"));
        assertTrue(list.removeFirstOccurrence("2"));
        assertEquals(4, list.size());
        assertArrayEquals(new String[] { "1", "3", "1", "2" }, list.toArray(new String[0]));
    }

    @Test
    public void removesLastOccurrence() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(Arrays.asList("1", "2", "3", "1", "2"));
        assertFalse(list.removeLastOccurrence("*"));
        assertTrue(list.removeLastOccurrence("2"));
        assertEquals(4, list.size());
        assertArrayEquals(new String[] { "1", "2", "3", "1" }, list.toArray(new String[0]));
    }

    @Test
    public void pushes() {
        LinkedList<String> list = new LinkedList<>();
        list.push("foo");
        assertEquals("foo", list.peek());
        list.push("bar");
        assertEquals("bar", list.peek());
    }
}
