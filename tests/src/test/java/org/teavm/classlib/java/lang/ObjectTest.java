/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.support.GCSupport;
import org.teavm.junit.AsyncEnabled;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class ObjectTest {
    @Test
    public void objectCreated() {
        Object a = new Object();
        assertNotNull(a);
    }

    @Test
    public void differentInstancesNotEqual() {
        Object a = new Object();
        Object b = new Object();
        assertNotEquals(a, b);
    }

    @Test
    public void sameInstancesAreEqual() {
        Object a = new Object();
        Object b = a;
        assertEquals(a, b);
    }

    @Test
    public void multipleGetClassCallsReturnSameValue() {
        Object a = new Object();
        assertSame(a.getClass(), a.getClass());
    }

    @Test
    public void sameClassesAreEqual() {
        Object a = new Object();
        Object b = new Object();
        assertSame(a.getClass(), b.getClass());
    }

    @Test
    public void properInstanceDetected() {
        assertTrue(Object.class.isInstance(new Object()));
    }

    @Test
    public void toStringWorks() {
        assertTrue(new Object().toString().startsWith("java.lang.Object@"));
        assertTrue(new Object[2].toString().startsWith("[Ljava.lang.Object;@"));
        assertTrue(new byte[3].toString().startsWith("[B@"));
    }

    @Test
    @AsyncEnabled
    public void waitWorks() throws InterruptedException {
        long start = System.currentTimeMillis();
        final Object lock = new Object();
        synchronized (lock) {
            lock.wait(110);
        }
        long end = System.currentTimeMillis();
        assertTrue(end - start > 100);
    }

    @Test
    @AsyncEnabled
    public void repeatedLocksPreserveIdentityAndOwnership() {
        Object[] locks = { new Object(), new int[1], new Object[1] };
        int[] identities = new int[locks.length];
        for (int i = 0; i < locks.length; i++) {
            identities[i] = System.identityHashCode(locks[i]);
        }
        Object inner = new Object();
        int innerIdentity = System.identityHashCode(inner);
        for (int pass = 0; pass < 20; pass++) {
            for (int i = 0; i < locks.length; i++) {
                Object lock = locks[i];
                assertFalse(Thread.holdsLock(lock));
                synchronized (lock) {
                    synchronized (lock) {
                        assertTrue(Thread.holdsLock(lock));
                        assertEquals(identities[i], System.identityHashCode(lock));
                    }
                    synchronized (inner) {
                        assertTrue(Thread.holdsLock(lock));
                        assertEquals(innerIdentity, System.identityHashCode(inner));
                    }
                    assertTrue(Thread.holdsLock(lock));
                }
                assertFalse(Thread.holdsLock(lock));
                assertEquals(identities[i], System.identityHashCode(lock));
                assertEquals(innerIdentity, System.identityHashCode(inner));
            }
            // Collect while a deleted monitor is kept for reuse.
            System.gc();
        }
    }

    @Test
    @AsyncEnabled
    public void repeatedWaitNotifyPreservesReentrantLock() throws InterruptedException {
        Object lock = new Object();
        Object other = new Object();
        boolean[] ready = new boolean[2];
        int identity = System.identityHashCode(lock);
        int otherIdentity = System.identityHashCode(other);
        for (int i = 0; i < 4; i++) {
            ready[0] = false;
            ready[1] = false;
            boolean otherFirst = i % 2 == 1;
            Thread notifier = new Thread(() -> {
                // Locked while the monitor of lock has a waiter; otherwise only after it has been deleted.
                if (otherFirst) {
                    synchronized (other) {
                        assertTrue(Thread.holdsLock(other));
                    }
                }
                synchronized (lock) {
                    ready[0] = true;
                    lock.notifyAll();
                }
                synchronized (other) {
                    ready[1] = true;
                }
            });
            synchronized (lock) {
                synchronized (lock) {
                    notifier.start();
                    while (!ready[0]) {
                        lock.wait();
                    }
                    assertTrue(Thread.holdsLock(lock));
                    assertEquals(identity, System.identityHashCode(lock));
                }
                assertTrue(Thread.holdsLock(lock));
            }
            notifier.join();
            assertTrue(ready[1]);
            assertFalse(Thread.holdsLock(lock));
            assertFalse(Thread.holdsLock(other));
            assertEquals(identity, System.identityHashCode(lock));
            assertEquals(otherIdentity, System.identityHashCode(other));
        }
    }

    @Test
    @AsyncEnabled
    public void notifyAfterTimedOutWaitWakesWaiter() throws InterruptedException {
        Object lock = new Object();
        // [0]: the waiter's timed wait has ended and it waits again; [1]: the main thread has notified.
        boolean[] state = new boolean[2];
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(20);
                    state[0] = true;
                    while (!state[1]) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    // ends the thread
                }
            }
        });
        waiter.start();
        while (true) {
            synchronized (lock) {
                // The waiter holds the lock from setting state[0] until it waits, so it waits now.
                if (state[0]) {
                    state[1] = true;
                    lock.notify();
                    break;
                }
            }
            Thread.sleep(10);
        }
        waiter.join(2000);
        boolean woken = !waiter.isAlive();
        if (!woken) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        assertTrue(woken, "notify() after a timed-out wait() did not wake the waiting thread");
    }

    @Test
    @AsyncEnabled
    public void interruptedWaitHoldsMonitorAndDoesNotAbsorbNotify() throws InterruptedException {
        Object lock = new Object();
        // [0]: in the first wait; [1]: interrupted, waits again; [2]: notified; [3]: second wait returned.
        boolean[] state = new boolean[4];
        boolean[] heldLockAfterInterrupt = new boolean[1];
        boolean[] flagClearedAfterInterrupt = new boolean[1];
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    state[0] = true;
                    lock.wait();
                } catch (InterruptedException e) {
                    heldLockAfterInterrupt[0] = Thread.holdsLock(lock);
                    flagClearedAfterInterrupt[0] = !Thread.currentThread().isInterrupted();
                }
                state[1] = true;
                try {
                    while (!state[2]) {
                        lock.wait();
                    }
                    state[3] = true;
                } catch (InterruptedException e) {
                    // ends the thread
                }
            }
        });
        waiter.start();
        waitUntil(lock, state, 0);
        waiter.interrupt();
        waitUntil(lock, state, 1);
        synchronized (lock) {
            state[2] = true;
            lock.notify();
        }
        waiter.join(2000);
        if (waiter.isAlive()) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        assertTrue(heldLockAfterInterrupt[0], "wait() did not hold the monitor when it threw InterruptedException");
        assertTrue(flagClearedAfterInterrupt[0], "InterruptedException from wait() left the interrupt status set");
        assertTrue(state[3], "notify() after an interrupted wait() did not wake the waiting thread");
    }

    // The waiter holds the lock from setting a flag until it waits, so once the flag is set, it waits.
    private static void waitUntil(Object lock, boolean[] state, int index) throws InterruptedException {
        while (true) {
            synchronized (lock) {
                if (state[index]) {
                    return;
                }
            }
            Thread.sleep(10);
        }
    }

    @Test
    @AsyncEnabled
    public void allQueuedThreadsEnterMonitor() throws InterruptedException {
        var lock = new Object();
        var entered = new int[1];
        var threads = new Thread[3];
        synchronized (lock) {
            for (var i = 0; i < threads.length; ++i) {
                threads[i] = new Thread(() -> {
                    synchronized (lock) {
                        entered[0]++;
                        // Makes this method async, so the synchronized block can wait for the monitor.
                        Thread.yield();
                    }
                });
                threads[i].start();
            }
            // Let every thread block on the monitor while this thread holds it.
            Thread.sleep(100);
        }
        for (var thread : threads) {
            thread.join(2000);
        }
        assertEquals(threads.length, entered[0]);
    }

    @Test
    @AsyncEnabled
    public void contendedMonitorBlocksUntilReleased() throws InterruptedException {
        var lock = new Object();
        var events = new ArrayList<String>();
        var error = new RuntimeException[1];
        var other = new Thread(() -> {
            try {
                synchronized (lock) {
                    events.add("other");
                }
            } catch (RuntimeException e) {
                error[0] = e;
            }
        });
        synchronized (lock) {
            other.start();
            Thread.sleep(50);
            events.add("main");
        }
        other.join();
        assertNull(error[0]);
        assertEquals(List.of("main", "other"), events);
    }

    // wait(): "InterruptedException - if any thread interrupted the current thread before or while the current
    // thread was waiting. The interrupted status of the current thread is cleared when this exception is thrown."
    @Test
    @AsyncEnabled
    public void waitThrowsForEarlierInterrupt() {
        var current = Thread.currentThread();
        var lock = new Object();
        var names = List.of("wait()", "wait(5000)", "wait(5000, 1)");
        var actions = List.<WaitAction>of(Object::wait, o -> o.wait(5000), o -> o.wait(5000, 1));
        for (var i = 0; i < actions.size(); ++i) {
            synchronized (lock) {
                current.interrupt();
                long start = System.currentTimeMillis();
                var watchdog = interruptLater(current);
                var threw = false;
                try {
                    actions.get(i).run(lock);
                } catch (InterruptedException e) {
                    threw = true;
                }
                watchdog.interrupt();
                assertTrue(threw, names.get(i) + " did not throw");
                assertTrue(System.currentTimeMillis() - start < 2500, names.get(i) + " waited");
                assertFalse(current.isInterrupted(), names.get(i) + " left the interrupt status set");
                assertTrue(Thread.holdsLock(lock), names.get(i) + " released the monitor");
            }
        }
    }

    interface WaitAction {
        void run(Object lock) throws InterruptedException;
    }

    // Ends a call that blocks although it should throw at once, so that the test fails on its time check instead
    // of hanging. Stop the returned thread with interrupt().
    private static Thread interruptLater(Thread target) {
        var watchdog = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return;
            }
            target.interrupt();
        });
        watchdog.start();
        return watchdog;
    }

    private Thread keptWaiter;

    @Test
    @AsyncEnabled
    public void notifiedWaitDoesNotKeepItsLock() throws InterruptedException {
        var ref = runNotifiedWaiterAndDropLock();
        if (ref.get() != null) {
            GCSupport.tryToTriggerGC(ref);
        }
        assertNotNull(keptWaiter);
        assertNull(ref.get(), "an ended thread whose wait notify() ended kept the lock alive");
    }

    // Only the waiter's interrupt handler may keep the lock: the waiter takes the lock from a holder that it clears,
    // so neither its Runnable nor the test refers to the lock once the waiter has ended.
    private WeakReference<Object> runNotifiedWaiterAndDropLock() throws InterruptedException {
        Object[] holder = { new Object() };
        var ref = new WeakReference<>(holder[0]);
        boolean[] state = new boolean[2];
        keptWaiter = new Thread(() -> waitUntilNotified(holder, state));
        keptWaiter.start();
        waitUntil(ref.get(), state, 0);
        synchronized (ref.get()) {
            state[1] = true;
            ref.get().notify();
        }
        keptWaiter.join(2000);
        assertTrue(!keptWaiter.isAlive(), "the waiter did not end");
        assertNull(holder[0]);
        return ref;
    }

    private static void waitUntilNotified(Object[] holder, boolean[] state) {
        Object lock = holder[0];
        holder[0] = null;
        synchronized (lock) {
            state[0] = true;
            try {
                while (!state[1]) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                // ends the thread
            }
        }
    }

}
