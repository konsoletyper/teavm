/*
 *  Copyright 2026 Carl Stainton.
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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Single-threaded queue operations, checked against the JVM and TeaVM implementations. */
@RunWith(TeaVMTestRunner.class)
public class ConcurrentLinkedQueueTest {
    @Test
    public void emptyQueue() {
        var queue = new ConcurrentLinkedQueue<String>();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
        assertNull(queue.poll());
        assertFalse(queue.iterator().hasNext());
    }

    @Test
    public void retainsInsertionOrderAndDuplicates() {
        var queue = new ConcurrentLinkedQueue<String>();
        assertTrue(queue.offer("first"));
        assertTrue(queue.offer("second"));
        assertTrue(queue.offer("first"));
        assertEquals("first", queue.peek());
        assertEquals(3, queue.size());
        assertEquals("first", queue.poll());
        assertEquals("second", queue.poll());
        assertEquals("first", queue.poll());
        assertNull(queue.poll());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void copiesCollectionInIterationOrder() {
        var source = new ArrayList<>(Arrays.asList("first", "second"));
        var queue = new ConcurrentLinkedQueue<>(source);
        source.clear();
        assertArrayEquals(new String[] { "first", "second" }, queue.toArray(new String[0]));
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullCollection() {
        new ConcurrentLinkedQueue<String>((Collection<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullCollectionElement() {
        new ConcurrentLinkedQueue<>(Arrays.asList("first", null));
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullOffer() {
        new ConcurrentLinkedQueue<String>().offer(null);
    }

    @Test
    public void iteratorFollowsQueueOrderAndCanRemoveHead() {
        var queue = new ConcurrentLinkedQueue<>(Arrays.asList("first", "second", "third"));
        Iterator<String> iterator = queue.iterator();
        assertEquals("first", iterator.next());
        iterator.remove();
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(2, queue.size());
        assertEquals("second", queue.peek());
    }

    @Test
    public void supportsInheritedQueueOperations() {
        var queue = new ConcurrentLinkedQueue<String>();
        assertTrue(queue.add("first"));
        assertEquals("first", queue.element());
        assertEquals("first", queue.remove());
        assertTrue(queue.isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    public void removeFromEmptyQueueFails() {
        new ConcurrentLinkedQueue<String>().remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void elementFromEmptyQueueFails() {
        new ConcurrentLinkedQueue<String>().element();
    }

    @Test
    public void supportsCollectionOperationsAndReuseAfterClear() {
        var queue = new ConcurrentLinkedQueue<String>();
        assertTrue(queue.addAll(Arrays.asList("first", "second", "third")));
        assertTrue(queue.contains("second"));
        assertTrue(queue.remove("second"));
        assertFalse(queue.contains("second"));
        assertFalse(queue.remove("missing"));
        assertArrayEquals(new String[] { "first", "third" }, queue.toArray(new String[0]));
        queue.clear();
        assertTrue(queue.isEmpty());
        queue.offer("again");
        assertEquals("again", queue.poll());
    }

    @Test
    public void growsAfterInterleavedOffersAndPolls() {
        var queue = new ConcurrentLinkedQueue<Integer>();
        for (int i = 0; i < 32; ++i) {
            queue.offer(i);
        }
        for (int i = 0; i < 24; ++i) {
            assertEquals(Integer.valueOf(i), queue.poll());
        }
        for (int i = 32; i < 160; ++i) {
            queue.offer(i);
        }
        assertEquals(136, queue.size());
        for (int i = 24; i < 160; ++i) {
            assertEquals(Integer.valueOf(i), queue.poll());
        }
        assertTrue(queue.isEmpty());
    }
}
