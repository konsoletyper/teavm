/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.classlib.java.lang.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.support.GCSupport;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
public class WeakReferenceTest {
    @Test
    public void weakReferenceDeps() {
        var a = new A();
        var b = new B();
        var weakRefs = List.of(new WeakReference<>(a), new WeakReference<>(b));
        var sb = new StringBuilder();
        for (var ref : weakRefs) {
            sb.append(ref.get().foo()).append(";");
        }
        assertEquals("A.foo;B.foo;", sb.toString());
    }

    @Test
    public void deref() {
        var ref = createAndTestRef(null);
        GCSupport.tryToTriggerGC(ref);
        assertNull(ref.get());
    }

    @Test
    public void refQueue() {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        GCSupport.tryToTriggerGC(ref);
        int attemptCount = 0;
        Object value;
        do {
            value = queue.poll();
            if (value != null) {
                break;
            }
            waitInJVM();
        } while (attemptCount++ < 50);
        assertSame(ref, value);
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void queueRemoveWakesWaitingThread() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        // The referent is created in another thread, so that no variable of this method keeps it reachable.
        var refs = new WeakReference<?>[1];
        var creator = new Thread(() -> refs[0] = new WeakReference<>(new Object(), queue));
        creator.start();
        creator.join();

        var removed = new ArrayBlockingQueue<Object>(1);
        var waiter = new Thread(() -> {
            try {
                removed.add(queue.remove());
            } catch (InterruptedException e) {
                // The test has ended.
            }
        });
        waiter.setDaemon(true);
        waiter.start();

        GCSupport.tryToTriggerGC(refs[0]);
        var result = removed.poll(5, TimeUnit.SECONDS);
        waiter.interrupt();
        assertSame(refs[0], result);
        // The waiting thread took the reference, so the queue does not hold it as well.
        assertNull(queue.poll());
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void queueRemoveWithTimeoutWakesBeforeTimeout() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        var refs = new WeakReference<?>[1];
        var creator = new Thread(() -> refs[0] = new WeakReference<>(new Object(), queue));
        creator.start();
        creator.join();

        var removed = new ArrayBlockingQueue<Object>(1);
        var waiter = new Thread(() -> {
            try {
                var ref = queue.remove(30000);
                if (ref != null) {
                    removed.add(ref);
                }
            } catch (InterruptedException e) {
                // The test has ended.
            }
        });
        waiter.setDaemon(true);
        waiter.start();

        GCSupport.tryToTriggerGC(refs[0]);
        var result = removed.poll(5, TimeUnit.SECONDS);
        waiter.interrupt();
        assertSame(refs[0], result);
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void queueRemoveDeliversEachReferenceOnce() throws InterruptedException {
        // More references than waiters: each waiter takes one reference, and the rest stay in the queue for
        // poll(). No reference is lost or delivered twice.
        var waiterCount = 3;
        var refCount = 5;
        var queue = new ReferenceQueue<>();
        var refs = new WeakReference<?>[refCount];
        var creator = new Thread(() -> {
            for (var i = 0; i < refCount; ++i) {
                refs[i] = new WeakReference<>(new Object(), queue);
            }
        });
        creator.start();
        creator.join();

        var removed = new ArrayBlockingQueue<Object>(refCount);
        var waiters = new Thread[waiterCount];
        for (var i = 0; i < waiterCount; ++i) {
            waiters[i] = new Thread(() -> {
                try {
                    removed.add(queue.remove());
                } catch (InterruptedException e) {
                    // The test has ended.
                }
            });
            waiters[i].setDaemon(true);
            waiters[i].start();
        }

        for (var ref : refs) {
            // Each attempt waits, so skip references whose referent is already collected
            if (ref.get() != null) {
                GCSupport.tryToTriggerGC(ref);
            }
        }
        var received = new ArrayList<Object>();
        for (var i = 0; i < waiterCount; ++i) {
            var ref = removed.poll(5, TimeUnit.SECONDS);
            if (ref != null) {
                received.add(ref);
            }
        }
        for (var waiter : waiters) {
            waiter.interrupt();
        }
        assertEquals(waiterCount, received.size());
        for (var attempt = 0; attempt < 50 && received.size() < refCount; ++attempt) {
            var ref = queue.poll();
            if (ref != null) {
                received.add(ref);
            } else {
                Thread.sleep(100);
            }
        }
        assertNull(queue.poll());
        assertNull(removed.poll());
        assertEquals(refCount, received.size());
        for (var ref : refs) {
            assertEquals(1, received.stream().filter(r -> r == ref).count());
        }
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void queueRemoveTimedOutWaiterLeavesReferenceInQueue() throws InterruptedException {
        // A remove(timeout) that has timed out no longer waits, so a later reference goes to poll().
        var queue = new ReferenceQueue<>();
        assertNull(queue.remove(10));

        var refs = new WeakReference<?>[1];
        var creator = new Thread(() -> refs[0] = new WeakReference<>(new Object(), queue));
        creator.start();
        creator.join();

        GCSupport.tryToTriggerGC(refs[0]);
        Object value = null;
        for (var attempt = 0; attempt < 50 && value == null; ++attempt) {
            value = queue.poll();
            if (value == null) {
                Thread.sleep(100);
            }
        }
        assertSame(refs[0], value);
    }

    private static void waitInJVM() {
        if (!PlatformDetector.isTeaVM()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    @Test
    // On C, ReferenceQueueGenerator generates an empty body for remove(), so remove() returns garbage.
    @SkipPlatform(TestPlatform.C)
    public void queueRemove() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        var threadQueue = new ArrayBlockingQueue<>(4);
        var thread = new Thread(() -> {
            try {
                threadQueue.add(queue.remove());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();

        GCSupport.tryToTriggerGC(ref);
        var result = threadQueue.poll(2, TimeUnit.SECONDS);
        assertSame(ref, result);
    }

    private static WeakReference<Object> createAndTestRef(ReferenceQueue<Object> queue) {
        var refs = new ArrayList<WeakReference<Object>>();
        var referentReturned = new boolean[1];
        runInAnotherThread(() -> {
            var obj = new Object();
            var ref = new WeakReference<>(obj, queue);
            referentReturned[0] = ref.get() == obj;
            refs.add(ref);
        });
        assertTrue(referentReturned[0]);
        return refs.get(0);
    }

    @Test
    public void clear() {
        var obj = new Object();
        var ref = new WeakReference<>(obj);
        assertSame(obj, ref.get());

        ref.clear();
        assertNull(ref.get());
    }

    // Reference.enqueue(): "Clears this reference object and adds it to the queue with which it is registered, if any."
    @Test
    public void enqueueClears() {
        var obj = new Object();
        var queue = new ReferenceQueue<>();
        List<Reference<Object>> refs = List.of(new WeakReference<>(obj, queue), new WeakReference<>(obj),
                new SoftReference<>(obj, queue), new SoftReference<>(obj));
        for (var ref : refs) {
            assertSame(obj, ref.get());
            ref.enqueue();
            assertNull(ref.get());
        }
    }

    // Reference.enqueue(): "Clears this reference object and adds it to the queue with which it is registered, if any."
    @Test
    // Known gap: on JS and Wasm GC, enqueue() clears the reference and returns true, but does not add it to the queue
    @SkipPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC })
    public void enqueueAddsToQueue() {
        var queue = new ReferenceQueue<>();
        var ref = new WeakReference<>(new Object(), queue);
        assertTrue(ref.enqueue());
        assertSame(ref, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void clearTwice() {
        var obj = new Object();
        var queue = new ReferenceQueue<>();
        var refs = List.of(new WeakReference<>(obj), new WeakReference<>(obj, queue));
        for (var ref : refs) {
            ref.clear();
            assertNull(ref.get());
            ref.clear();
            assertNull(ref.get());
        }
        assertNull(queue.poll());
    }

    @Test
    public void nullReferent() {
        var queue = new ReferenceQueue<>();
        var refs = List.of(new WeakReference<>(null), new WeakReference<>(null, queue));
        for (var ref : refs) {
            assertNull(ref.get());
            ref.clear();
            assertNull(ref.get());
        }
        assertNull(queue.poll());
    }

    @Test
    public void clearedRefNotEnqueued() {
        var queue = new ReferenceQueue<>();
        var refs = new WeakReference<?>[2];
        runInAnotherThread(() -> {
            var obj = new Object();
            refs[0] = new WeakReference<>(obj, queue);
            refs[1] = new WeakReference<>(obj, queue);
        });
        refs[1].clear();
        GCSupport.tryToTriggerGC(refs[0]);
        assertEquals(List.of(refs[0]), pollUntilEnqueued(queue, refs[0]));
    }

    @Test
    public void unreachableRefNotEnqueued() {
        var queue = new ReferenceQueue<>();
        var holder = new Object[1];
        var refs = new WeakReference<?>[2];
        runInAnotherThread(() -> {
            holder[0] = new Object();
            refs[0] = new WeakReference<>(holder[0], queue);
            refs[1] = new WeakReference<>(new WeakReference<>(holder[0], queue));
        });
        GCSupport.tryToTriggerGC(refs[1]);
        assertNull(refs[1].get());

        holder[0] = null;
        GCSupport.tryToTriggerGC(refs[0]);
        assertEquals(List.of(refs[0]), pollUntilEnqueued(queue, refs[0]));
    }

    // Objects that a test expects to be collected are created in another thread. When the optimizer inlines a helper
    // method, its temporary variables become variables of the test method, which may keep them reachable.
    private static void runInAnotherThread(Runnable action) {
        var thread = new Thread(action);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> pollUntilEnqueued(ReferenceQueue<?> queue, Object expected) {
        var result = new ArrayList<Object>();
        for (var i = 0; i < 50 && !result.contains(expected); ++i) {
            sleep(100);
            drain(queue, result);
        }
        // Wait a little more to catch references that are enqueued but should not be
        sleep(100);
        drain(queue, result);
        return result;
    }

    private static void drain(ReferenceQueue<?> queue, List<Object> result) {
        for (var ref = queue.poll(); ref != null; ref = queue.poll()) {
            result.add(ref);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    interface I {
        String foo();
    }

    static class A implements I {
        @Override
        public String foo() {
            return "A.foo";
        }
    }
    
    static class B implements I {
        @Override
        public String foo() {
            return "B.foo";
        }
    }
}
