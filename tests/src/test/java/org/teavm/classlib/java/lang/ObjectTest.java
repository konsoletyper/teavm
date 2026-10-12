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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
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
}
