/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.DoubleAccumulator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class AtomicArrayAndAdderTest {

    @Test
    public void atomicIntegerArray() {
        AtomicIntegerArray arr = new AtomicIntegerArray(3);
        assertEquals(3, arr.length());
        assertEquals(0, arr.get(0));

        arr.set(0, 10);
        assertEquals(10, arr.get(0));

        assertEquals(10, arr.getAndSet(0, 20));
        assertEquals(20, arr.get(0));

        assertTrue(arr.compareAndSet(0, 20, 30));
        assertEquals(30, arr.get(0));
        assertFalse(arr.compareAndSet(0, 20, 40));
        assertEquals(30, arr.get(0));

        assertEquals(30, arr.getAndIncrement(0));
        assertEquals(31, arr.get(0));

        assertEquals(31, arr.getAndDecrement(0));
        assertEquals(30, arr.get(0));

        assertEquals(30, arr.getAndAdd(0, 5));
        assertEquals(35, arr.get(0));

        assertEquals(36, arr.incrementAndGet(0));
        assertEquals(35, arr.decrementAndGet(0));

        assertEquals(40, arr.addAndGet(0, 5));
    }

    @Test
    public void atomicIntegerArrayFromExisting() {
        int[] src = { 1, 2, 3 };
        AtomicIntegerArray arr = new AtomicIntegerArray(src);
        assertEquals(3, arr.length());
        assertEquals(1, arr.get(0));
        assertEquals(2, arr.get(1));
        assertEquals(3, arr.get(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void atomicIntegerArrayOutOfBounds() {
        AtomicIntegerArray arr = new AtomicIntegerArray(3);
        arr.get(5);
    }

    @Test
    public void atomicLongArray() {
        AtomicLongArray arr = new AtomicLongArray(3);
        assertEquals(3, arr.length());
        assertEquals(0L, arr.get(0));

        arr.set(0, 10000000000L);
        assertEquals(10000000000L, arr.get(0));

        assertTrue(arr.compareAndSet(0, 10000000000L, 20000000000L));
        assertEquals(20000000000L, arr.get(0));

        assertEquals(20000000000L, arr.getAndAdd(0, 5));
        assertEquals(20000000005L, arr.get(0));
    }

    @Test
    public void atomicReferenceArray() {
        AtomicReferenceArray<String> arr = new AtomicReferenceArray<>(3);
        assertEquals(3, arr.length());
        assertEquals(null, arr.get(0));

        arr.set(0, "hello");
        assertEquals("hello", arr.get(0));

        String old = arr.getAndSet(0, "world");
        assertEquals("hello", old);
        assertEquals("world", arr.get(0));

        assertTrue(arr.compareAndSet(0, "world", "test"));
        assertEquals("test", arr.get(0));
        assertFalse(arr.compareAndSet(0, "wrong", "other"));
    }

    @Test
    public void longAdder() {
        LongAdder adder = new LongAdder();
        assertEquals(0, adder.sum());
        assertEquals(0, adder.longValue());

        adder.add(10);
        assertEquals(10, adder.sum());

        adder.increment();
        assertEquals(11, adder.sum());

        adder.decrement();
        assertEquals(10, adder.sum());

        adder.add(-5);
        assertEquals(5, adder.sum());

        assertEquals(5, adder.sumThenReset());
        assertEquals(0, adder.sum());

        adder.add(42);
        adder.reset();
        assertEquals(0, adder.sum());
    }

    @Test
    public void doubleAdder() {
        DoubleAdder adder = new DoubleAdder();
        assertEquals(0.0, adder.sum(), 0.001);

        adder.add(10.5);
        assertEquals(10.5, adder.sum(), 0.001);

        adder.add(0.3);
        assertEquals(10.8, adder.sum(), 0.001);

        double result = adder.sumThenReset();
        assertEquals(10.8, result, 0.001);
        assertEquals(0.0, adder.sum(), 0.001);
    }

    @Test
    public void longAccumulator() {
        LongAccumulator acc = new LongAccumulator(Long::max, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, acc.get());

        acc.accumulate(5);
        assertEquals(5, acc.get());

        acc.accumulate(3);
        assertEquals(5, acc.get());

        acc.accumulate(10);
        assertEquals(10, acc.get());

        long result = acc.getThenReset();
        assertEquals(10, result);
        assertEquals(Long.MIN_VALUE, acc.get());
    }

    @Test
    public void doubleAccumulator() {
        DoubleAccumulator acc = new DoubleAccumulator(Math::max, Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, acc.get(), 0.001);

        acc.accumulate(3.14);
        assertEquals(3.14, acc.get(), 0.001);

        acc.accumulate(2.71);
        assertEquals(3.14, acc.get(), 0.001);

        acc.accumulate(9.99);
        assertEquals(9.99, acc.get(), 0.001);
    }

    @Test
    public void atomicMarkableReference() {
        AtomicMarkableReference<String> ref = new AtomicMarkableReference<>("hello", false);
        assertEquals("hello", ref.getReference());
        assertFalse(ref.isMarked());

        boolean[] markHolder = new boolean[1];
        assertEquals("hello", ref.get(markHolder));
        assertFalse(markHolder[0]);

        assertTrue(ref.compareAndSet("hello", "world", false, true));
        assertEquals("world", ref.getReference());
        assertTrue(ref.isMarked());

        assertFalse(ref.compareAndSet("hello", "test", false, true));

        assertTrue(ref.attemptMark("world", false));
        assertFalse(ref.isMarked());

        ref.set("new", true);
        assertEquals("new", ref.getReference());
        assertTrue(ref.isMarked());
    }

    @Test
    public void atomicStampedReference() {
        AtomicStampedReference<String> ref = new AtomicStampedReference<>("hello", 0);
        assertEquals("hello", ref.getReference());
        assertEquals(0, ref.getStamp());

        int[] stampHolder = new int[1];
        assertEquals("hello", ref.get(stampHolder));
        assertEquals(0, stampHolder[0]);

        assertTrue(ref.compareAndSet("hello", "world", 0, 1));
        assertEquals("world", ref.getReference());
        assertEquals(1, ref.getStamp());

        assertFalse(ref.compareAndSet("world", "test", 0, 2));

        assertTrue(ref.attemptStamp("world", 5));
        assertEquals(5, ref.getStamp());

        ref.set("new", 10);
        assertEquals("new", ref.getReference());
        assertEquals(10, ref.getStamp());
    }
}
