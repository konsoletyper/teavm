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
import static org.junit.Assert.fail;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ArrayDequeTest {
    @Test
    public void addsToFront() {
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(1);
        deque.addFirst(2);
        Iterator<Integer> iter = deque.iterator();
        assertEquals(2, deque.size());
        assertEquals((Integer) 2, iter.next());
        assertEquals((Integer) 1, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void addsToBack() {
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addLast(1);
        deque.addLast(2);
        Iterator<Integer> iter = deque.iterator();
        assertEquals(2, deque.size());
        assertEquals((Integer) 1, iter.next());
        assertEquals((Integer) 2, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void addsManyToFront() {
        Deque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < 1000; ++i) {
            deque.addFirst(i);
        }
        assertEquals(1000, deque.size());
        Iterator<Integer> iter = deque.iterator();
        assertEquals((Integer) 999, iter.next());
        for (int i = 2; i < 500; ++i) {
            iter.next();
        }
        assertEquals((Integer) 500, iter.next());
        for (int i = 1; i < 500; ++i) {
            iter.next();
        }
        assertEquals((Integer) 0, iter.next());
    }

    @Test
    public void addsManyToBack() {
        Deque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < 1000; ++i) {
            deque.addLast(i);
        }
        assertEquals(1000, deque.size());
        Iterator<Integer> iter = deque.iterator();
        assertEquals((Integer) 0, iter.next());
        for (int i = 1; i < 500; ++i) {
            iter.next();
        }
        assertEquals((Integer) 500, iter.next());
        for (int i = 2; i < 500; ++i) {
            iter.next();
        }
        assertEquals((Integer) 999, iter.next());
    }

    @Test
    public void removesFromFront() {
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(1);
        deque.addFirst(2);
        assertEquals((Integer) 2, deque.removeFirst());
        assertEquals((Integer) 1, deque.removeFirst());
        assertEquals(0, deque.size());
    }

    @Test
    public void removesFromBack() {
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(1);
        deque.addFirst(2);
        assertEquals((Integer) 1, deque.removeLast());
        assertEquals((Integer) 2, deque.removeLast());
        assertEquals(0, deque.size());
    }

    @Test
    public void addAndRemoves() {
        Deque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < 100; ++i) {
            deque.addLast(i);
        }
        assertEquals((Integer) 0, deque.removeFirst());
        for (int i = 1; i < 20; ++i) {
            deque.removeFirst();
        }
        assertEquals((Integer) 20, deque.removeFirst());
        for (int i = 101; i < 111; ++i) {
            deque.addLast(i);
        }
        assertEquals((Integer) 110, deque.removeLast());
        for (int i = 2; i < 40; ++i) {
            deque.removeLast();
        }
        assertEquals((Integer) 70, deque.removeLast());
    }

    @Test
    public void eachRemovedObjectShouldReduceTheSizeByOne() {
        ArrayDeque<Object> arrayDeque = new ArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque.add(object1);
        assertEquals(1, arrayDeque.size());
        arrayDeque.remove(object1);
        assertEquals(0, arrayDeque.size());
        arrayDeque.add(object1);
        arrayDeque.add(object2);
        arrayDeque.add(object3);
        assertEquals(3, arrayDeque.size());
        arrayDeque.remove(object1);
        arrayDeque.remove(object2);
        arrayDeque.remove(object3);
        assertEquals(0, arrayDeque.size());
        arrayDeque.remove(object1);
        assertEquals(0, arrayDeque.size());
    }

    @Test
    public void removeFirstShouldNotContainTheFirstAddedObject() {
        ArrayDeque<Object> arrayDeque1 = new ArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque1.add(object1);
        arrayDeque1.add(object2);
        arrayDeque1.add(object3);
        arrayDeque1.removeFirst();
        assertEquals(2, arrayDeque1.size());
        assertTrue(arrayDeque1.contains(object2));
        assertTrue(arrayDeque1.contains(object3));

        ArrayDeque<Object> arrayDeque2 = new ArrayDeque<>();
        arrayDeque2.add(object1);
        arrayDeque2.add(object2);
        arrayDeque2.add(object3);
        arrayDeque2.remove(object1);
        arrayDeque2.removeFirst();
        assertEquals(1, arrayDeque2.size());
        assertTrue(arrayDeque2.contains(object3));

        ArrayDeque<Object> arrayDeque3 = new ArrayDeque<>();
        arrayDeque3.add(object1);
        arrayDeque3.add(object2);
        arrayDeque3.add(object3);
        arrayDeque3.remove(object2);
        arrayDeque3.removeFirst();
        assertEquals(1, arrayDeque3.size());
        assertTrue(arrayDeque3.contains(object3));

        ArrayDeque<Object> arrayDeque4 = new ArrayDeque<>();
        arrayDeque4.add(object1);
        arrayDeque4.add(object2);
        arrayDeque4.add(object3);
        arrayDeque4.remove(object3);
        arrayDeque4.removeFirst();
        assertEquals(1, arrayDeque4.size());
        assertTrue(arrayDeque4.contains(object2));
    }

    @Test
    public void removeLastShouldNotContainTheLastAddedObject() {
        ArrayDeque<Object> arrayDeque1 = new ArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque1.add(object1);
        arrayDeque1.add(object2);
        arrayDeque1.add(object3);
        arrayDeque1.removeLast();
        assertEquals(2, arrayDeque1.size());
        assertTrue(arrayDeque1.contains(object1));
        assertTrue(arrayDeque1.contains(object2));

        ArrayDeque<Object> arrayDeque2 = new ArrayDeque<>();
        arrayDeque2.add(object1);
        arrayDeque2.add(object2);
        arrayDeque2.add(object3);
        arrayDeque2.remove(object3);
        arrayDeque2.removeLast();
        assertEquals(1, arrayDeque2.size());
        assertTrue(arrayDeque2.contains(object1));

        ArrayDeque<Object> arrayDeque3 = new ArrayDeque<>();
        arrayDeque3.add(object1);
        arrayDeque3.add(object2);
        arrayDeque3.add(object3);
        arrayDeque3.remove(object2);
        arrayDeque3.removeLast();
        assertEquals(1, arrayDeque3.size());
        assertTrue(arrayDeque3.contains(object1));

        ArrayDeque<Object> arrayDeque4 = new ArrayDeque<>();
        arrayDeque4.add(object1);
        arrayDeque4.add(object2);
        arrayDeque4.add(object3);
        arrayDeque4.remove(object3);
        arrayDeque4.removeLast();
        assertEquals(1, arrayDeque4.size());
        assertTrue(arrayDeque4.contains(object1));
    }

    @Test
    public void removeElementInWrappedArray() {
        ArrayDeque<Object> arrayDeque = new ArrayDeque<>(8);
        for (int i = 0; i < 4; ++i) {
            arrayDeque.addLast(0);
        }
        for (int i = 0; i < 4; ++i) {
            arrayDeque.removeFirst();
        }
        for (int i = 0; i < 6; ++i) {
            arrayDeque.addLast(i);
        }
        for (int i = 5; i >= 0; --i) {
            arrayDeque.remove(i);
            arrayDeque.addLast(23);
            arrayDeque.removeLast();
        }
    }

    @Test
    public void testSequencedCollectionReadOnly() {
        Deque<String> deque = new ArrayDeque<>(List.of("0", "1", "2", "3", "4", "5", "6"));
        Deque<String> reversed = deque.reversed();
        Iterator<String> it = reversed.iterator();
        assertEquals("6", it.next());
        assertEquals("5", it.next());
        assertEquals("6", reversed.getFirst());
        assertEquals("0", reversed.getLast());
        assertEquals("6", reversed.peek());
        assertEquals("6", reversed.peekFirst());
        assertEquals("0", reversed.peekLast());
    }

    @Test
    public void testSequencedCollectionMutations() {
        Deque<String> deque = new ArrayDeque<>(List.of("a", "b", "c", "d"));
        assertEquals("a", deque.removeFirst());
        assertEquals("d", deque.removeLast());
        deque.addFirst("u");
        deque.addLast("e");
        assertArrayEquals(new String[] { "u", "b", "c", "e" }, deque.toArray());
        deque = new ArrayDeque<>(List.of("a", "b", "c", "d")).reversed();
        assertEquals("d", deque.removeFirst());
        assertEquals("a", deque.removeLast());
        deque.addFirst("u");
        deque.addLast("e");
        assertEquals("u", deque.remove());
        deque.add("f");
        assertArrayEquals(new String[] { "c", "b", "e", "f" }, deque.toArray());
    }

    @Test
    public void testSequencedCollectionIterator() {
        Deque<String> deque = new ArrayDeque<>(List.of("a", "b", "c", "d"));
        deque.addLast("e");
        deque.addLast("f");
        Iterator<String> it = deque.iterator();
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
        assertTrue(deque.isEmpty());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        deque = new ArrayDeque<>(List.of("a", "b", "c", "d")).reversed();
        it = deque.iterator();
        assertEquals("d", it.next());
        assertEquals("c", it.next());
        it.remove();
        assertArrayEquals(new String[] { "d", "b", "a" }, deque.toArray());
        deque = new ArrayDeque<>(List.of("a", "b", "c", "d")).reversed();
        it = deque.iterator();
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
        assertArrayEquals(new String[] { "d", "a" }, deque.toArray());
    }

    @Test
    public void sequenceCollectionMethodsOnEmpty() {
        var empty = new ArrayDeque<>();

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
    }
}
