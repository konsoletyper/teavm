/*
 *  Copyright 2018 Alexey Andreev.
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
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ArrayBlockingQueueTest {
    @Test
    public void constructed() {
        List<Integer> list = Arrays.asList(2, 3, 5);

        try {
            new ArrayBlockingQueue<>(2, false, list);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            new ArrayBlockingQueue<>(0);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            new ArrayBlockingQueue<>(1, false, null);
            fail("IAE expected");
        } catch (NullPointerException e) {
            // Expected
        }

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5, false, list);
        assertEquals(3, queue.size());
        assertEquals(2, queue.remainingCapacity());

        assertEquals(2, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
        assertEquals(5, queue.poll().intValue());
        assertNull(queue.poll());
    }

    @Test
    public void singleThread() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        assertEquals(1, queue.poll().intValue());
        queue.add(4);
        assertEquals(2, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
        assertEquals(4, queue.poll().intValue());
        assertNull(queue.poll());

        queue.add(5);
        assertEquals(5, queue.poll().intValue());
    }

    @Test
    public void blockingAddition() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
        assertTrue(queue.offer(1));
        assertFalse(queue.offer(2));

        new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // Do nothing
            }
            queue.poll();
        }).start();

        long start = System.currentTimeMillis();
        queue.put(3);
        long end = System.currentTimeMillis();
        assertTrue("Wait time " + (end - start), start + 50 < end && start + 5000 > end);

        assertEquals(3, queue.remove().intValue());
    }

    @Test
    public void blockingRemoval() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        assertNull(queue.poll());

        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                // Do nothing
            }
            queue.add(1);
            queue.add(2);
        }).start();

        long start = System.currentTimeMillis();
        int a = queue.take();
        long end = System.currentTimeMillis();
        int b = queue.take();

        assertTrue("Wait time " + (end - start), start + 100 < end && start + 5000 > end);
        assertEquals(1, a);
        assertEquals(2, b);
    }

    @Test
    public void shiftQueueSize() {
        assertEquals(6, shiftQueue().size());
    }

    @Test
    public void remove() {
        BlockingQueue<Integer> queue = simpleQueue();
        assertTrue(queue.remove(2));
        assertEquals(2, queue.size());
        assertEquals(1, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
        assertNull(queue.poll());

        queue = shiftQueue();
        assertTrue(queue.remove(1));
        assertEquals(5, queue.size());
        assertEquals(0, queue.poll().intValue());
        assertEquals(2, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
        assertEquals(4, queue.poll().intValue());
        assertEquals(5, queue.poll().intValue());
        assertNull(queue.poll());

        queue = shiftQueue();
        assertTrue(queue.remove(4));
        assertEquals(5, queue.size());
        assertEquals(0, queue.poll().intValue());
        assertEquals(1, queue.poll().intValue());
        assertEquals(2, queue.poll().intValue());
        assertEquals(3, queue.poll().intValue());
        assertEquals(5, queue.poll().intValue());
        assertNull(queue.poll());
    }

    @Test
    public void dumpsToArray() {
        BlockingQueue<Integer> queue = simpleQueue();
        assertArrayEquals(new Integer[] { 1, 2, 3 }, queue.toArray());

        queue = shiftQueue();
        assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4, 5 }, queue.toArray());
    }

    @Test
    public void dumpsToTypedArray() {
        BlockingQueue<Integer> queue = simpleQueue();
        assertArrayEquals(new Integer[] { 1, 2, 3 }, queue.toArray(new Integer[3]));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, queue.toArray(new Integer[1]));

        Integer[] array = new Integer[] { 10, 11, 12, 13, 14, 15 };
        assertArrayEquals(new Integer[] { 1, 2, 3, null, 14, 15 }, queue.toArray(array));
    }

    @Test
    public void drains() {
        BlockingQueue<Integer> queue = simpleQueue();
        List<Integer> target = new ArrayList<>();
        assertEquals(2, queue.drainTo(target, 2));
        assertEquals(Arrays.asList(1, 2), target);

        queue = simpleQueue();
        target.clear();
        assertEquals(3, queue.drainTo(target, 4));
        assertEquals(Arrays.asList(1, 2, 3), target);

        queue = shiftQueue();
        target.clear();
        assertEquals(2, queue.drainTo(target, 2));
        assertEquals(Arrays.asList(0, 1), target);

        queue = shiftQueue();
        target.clear();
        assertEquals(4, queue.drainTo(target, 4));
        assertEquals(Arrays.asList(0, 1, 2, 3), target);
    }

    @Test
    public void iterator() {
        BlockingQueue<Integer> queue = simpleQueue();
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(queue));

        queue = shiftQueue();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), new ArrayList<>(queue));
    }

    private BlockingQueue<Integer> simpleQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        queue.add(0);
        queue.remove();
        queue.add(1);
        queue.add(2);
        queue.add(3);
        return queue;
    }

    private BlockingQueue<Integer> shiftQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        for (int i = 0; i < 7; ++i) {
            queue.add(i);
        }
        while (!queue.isEmpty()) {
            queue.remove();
        }

        for (int i = 0; i < 6; ++i) {
            queue.add(i);
        }
        return queue;
    }
}
