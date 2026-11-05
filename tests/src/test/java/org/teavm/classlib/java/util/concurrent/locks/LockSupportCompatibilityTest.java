/*
 * Copyright 2026 TeaVM contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.classlib.java.util.concurrent.locks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform({ TestPlatform.WEBASSEMBLY_GC, TestPlatform.C })
public class LockSupportCompatibilityTest {
    @Test
    public void unparkWakesParkAndPreservesThreadIdentity() throws Exception {
        Thread caller = Thread.currentThread();
        Object blocker = new Object();
        AtomicInteger resumed = new AtomicInteger();
        AtomicReference<Thread> expected = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            LockSupport.park(blocker);
            resumed.set(Thread.currentThread() == expected.get() ? 1 : 0);
        });
        expected.set(worker);
        worker.start();
        waitForBlocker(worker, blocker);
        LockSupport.unpark(worker);
        worker.join();
        assertEquals(1, resumed.get());
        assertSame(caller, Thread.currentThread());
    }

    @Test
    public void unparkBeforeParkAndZeroTimeoutPermit() throws Exception {
        AtomicInteger resumed = new AtomicInteger();
        Thread worker = new Thread(() -> {
            LockSupport.parkNanos(0);
            LockSupport.park();
            resumed.set(1);
        });
        worker.start();
        LockSupport.unpark(worker);
        worker.join();
        assertEquals(1, resumed.get());
    }

    @Test
    public void repeatedUnparksCoalesce() throws Exception {
        Object blocker = new Object();
        AtomicInteger parksCompleted = new AtomicInteger();
        Thread worker = new Thread(() -> {
            LockSupport.park(blocker);
            parksCompleted.incrementAndGet();
            LockSupport.park(blocker);
            parksCompleted.incrementAndGet();
        });
        worker.start();
        waitForBlocker(worker, blocker);
        LockSupport.unpark(worker);
        LockSupport.unpark(worker);
        waitForCounter(parksCompleted, 1);
        assertEquals(1, parksCompleted.get());
        LockSupport.unpark(worker);
        worker.join();
        assertEquals(2, parksCompleted.get());
    }

    @Test
    public void interruptWakesParkWithoutClearingStatus() throws Exception {
        Object blocker = new Object();
        AtomicInteger interrupted = new AtomicInteger();
        Thread worker = new Thread(() -> {
            LockSupport.park(blocker);
            interrupted.set(Thread.currentThread().isInterrupted() ? 1 : 0);
        });
        worker.start();
        waitForBlocker(worker, blocker);
        worker.interrupt();
        worker.join();
        assertEquals(1, interrupted.get());
    }

    @Test
    public void getBlockerRejectsNull() {
        assertThrows(NullPointerException.class, () -> LockSupport.getBlocker(null));
    }

    private static void waitForBlocker(Thread thread, Object blocker) throws InterruptedException {
        for (int i = 0; i < 1_000 && LockSupport.getBlocker(thread) != blocker; i++) {
            Thread.sleep(1);
        }
        assertSame(blocker, LockSupport.getBlocker(thread));
    }

    private static void waitForCounter(AtomicInteger counter, int expected) throws InterruptedException {
        for (int i = 0; i < 1_000 && counter.get() < expected; i++) {
            Thread.sleep(1);
        }
        assertEquals(expected, counter.get());
    }
}
