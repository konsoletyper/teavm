/*
 *  Copyright 2015 Alexey Andreev.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class ThreadTest {
    @Test
    public void sleeps() throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(100);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 100, "Thread.sleep did not wait enough: " + duration);
    }

    @Test
    public void sleepInterrupted() {
        long start = System.currentTimeMillis();
        var mainThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ok
            }
            mainThread.interrupt();
        }).start();
        try {
            Thread.sleep(5000);
            fail("Exception expected");
        } catch (InterruptedException e) {
            long end = System.currentTimeMillis();
            assertEquals(Thread.currentThread(), mainThread);
            assertFalse(mainThread.isInterrupted());
            assertTrue(end - start < 5000, "Wait time " + (end - start));
        }
    }

    @Test
    public void sleepAfterInterruptedSleep() throws InterruptedException {
        var mainThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ok
            }
            mainThread.interrupt();
        }).start();
        try {
            Thread.sleep(5000);
            fail("Exception expected");
        } catch (InterruptedException e) {
            // expected
        }
        // A later suspension on the same thread must not throw the earlier exception again.
        long start = System.currentTimeMillis();
        Thread.sleep(20);
        assertTrue(System.currentTimeMillis() - start >= 20);
    }

    @Test
    public void catchesAsyncException() {
        try {
            throwException();
            fail("Exception should have been thrown");
        } catch (IllegalStateException e) {
            // all is ok
        }
    }


    private void throwException() {
        Thread.yield();
        throw new IllegalStateException();
    }

    @Test
    public void asyncVirtualCallsSupported() {
        List<A> alist = new ArrayList<>();
        alist.add(new A() {
            @Override int foo() {
                return 3;
            }
        });
        alist.add(new A() {
            @Override int foo() {
                Thread.yield();
                return 5;
            }
        });
        int sum = 0;
        for (A a : alist) {
            sum += a.foo();
        }
        assertEquals(8, sum);
    }
    
    @Test
    public void join() throws InterruptedException {
        var runnable = new Runnable() {
            boolean finished;
            
            @Override
            public void run() {
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                finished = true;
            }
        };
        var other = new Thread(runnable);
        other.start();
        Thread.sleep(150);
        assertFalse(runnable.finished);
        other.join();
        assertTrue(runnable.finished);
    }

    @Test
    public void joinAlreadyFinished() throws InterruptedException {
        var runnable = new Runnable() {
            boolean finished;

            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                finished = true;
            }
        };
        var other = new Thread(runnable);
        other.start();
        Thread.sleep(750);
        assertTrue(runnable.finished);
        other.join();
        assertTrue(runnable.finished);
    }

    abstract class A {
        abstract int foo();
    }

    // Thread.State: NEW "A thread that has not yet started", RUNNABLE "A thread executing in the Java virtual
    // machine", TERMINATED "A thread that has exited". isAlive(): "A thread is alive if it has been started and has
    // not yet died."
    @Test
    public void threadStates() throws InterruptedException {
        var insideState = new Thread.State[1];
        var insideAlive = new boolean[1];
        var thread = new Thread(() -> {
            insideState[0] = Thread.currentThread().getState();
            insideAlive[0] = Thread.currentThread().isAlive();
        });
        assertEquals(Thread.State.NEW, thread.getState());
        assertFalse(thread.isAlive());
        thread.start();
        // Started, but it has not run yet: it runs only when this thread suspends.
        assertTrue(thread.isAlive());
        assertEquals(Thread.State.RUNNABLE, thread.getState());
        thread.join();
        assertEquals(Thread.State.RUNNABLE, insideState[0]);
        assertTrue(insideAlive[0]);
        assertEquals(Thread.State.TERMINATED, thread.getState());
        assertFalse(thread.isAlive());
        assertEquals(Thread.State.RUNNABLE, Thread.currentThread().getState());
        assertTrue(Thread.currentThread().isAlive());

        // A thread that ends with an exception is terminated too.
        var failing = new Thread(() -> {
            throw new IllegalStateException();
        });
        failing.setUncaughtExceptionHandler((t, e) -> { });
        failing.start();
        failing.join();
        assertEquals(Thread.State.TERMINATED, failing.getState());
        assertFalse(failing.isAlive());
    }

    // Accepted gap: getState() reports RUNNABLE for a thread that sleeps, waits or blocks on a monitor.
    @Test
    @Disabled("Thread.getState() does not report TIMED_WAITING, WAITING or BLOCKED")
    public void threadStatesWhileSuspended() throws InterruptedException {
        var lock = new Object();
        var sleeping = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ok
            }
        });
        var waiting = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ok
                }
            }
        });
        sleeping.start();
        waiting.start();
        Thread.sleep(100);
        assertEquals(Thread.State.TIMED_WAITING, sleeping.getState());
        assertEquals(Thread.State.WAITING, waiting.getState());
        var blocked = new Thread(() -> {
            synchronized (lock) {
                lock.notifyAll();
            }
        });
        synchronized (lock) {
            blocked.start();
            Thread.sleep(100);
            assertEquals(Thread.State.BLOCKED, blocked.getState());
        }
        sleeping.join();
        waiting.join();
        blocked.join();
    }

    // Accepted gap: start(): "IllegalThreadStateException - if the thread was already started."
    @Test
    @Disabled("Thread.start() does not throw IllegalThreadStateException when the thread was already started")
    public void startTwiceThrows() throws InterruptedException {
        var thread = new Thread(() -> { });
        thread.start();
        thread.join();
        try {
            thread.start();
            fail("IllegalThreadStateException expected");
        } catch (IllegalThreadStateException e) {
            // ok
        }
    }

    // sleep(long, int): "IllegalArgumentException - if the value of millis is negative, or the value of nanos is
    // not in the range 0-999999".
    @Test
    public void sleepOverloadsAndSpinWait() throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(20, 500);
        Thread.sleep(Duration.ofMillis(20));
        Thread.onSpinWait();
        assertTrue(System.currentTimeMillis() - start >= 40);

        assertThrows(IllegalArgumentException.class, () -> Thread.sleep(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Thread.sleep(0, -1));
        assertThrows(IllegalArgumentException.class, () -> Thread.sleep(0, 1_000_000));
        Thread.sleep(0, 999_999);
        Thread.sleep(Duration.ZERO);
        Thread.sleep(Duration.ofNanos(-1));
        Thread.sleep(Duration.ofSeconds(Long.MIN_VALUE));

        // onSpinWait() lets another thread run, so a spin loop on a flag that thread sets ends.
        var flag = new boolean[1];
        new Thread(() -> flag[0] = true).start();
        while (!flag[0]) {
            Thread.onSpinWait();
        }
    }

    // A sleep too long for a host timer, or for a deadline in a long, must still wait until interrupted.
    @Test
    public void longSleepsWaitUntilInterrupted() throws InterruptedException {
        var sleeps = new ArrayList<Runnable>();
        sleeps.add(() -> sleepUnchecked(Long.MAX_VALUE));
        sleeps.add(() -> sleepUnchecked(Integer.MAX_VALUE + 1L));
        sleeps.add(() -> sleepUnchecked(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999)));
        sleeps.add(() -> sleepUnchecked(Duration.ofDays(30)));
        var outcomes = new String[sleeps.size()];
        var threads = new ArrayList<Thread>();
        for (var i = 0; i < sleeps.size(); ++i) {
            var index = i;
            var thread = new Thread(() -> {
                try {
                    sleeps.get(index).run();
                    outcomes[index] = "returned";
                } catch (RuntimeException e) {
                    outcomes[index] = e.getCause() instanceof InterruptedException ? "interrupted" : e.toString();
                }
            });
            threads.add(thread);
            thread.start();
        }
        Thread.sleep(200);
        for (var i = 0; i < threads.size(); ++i) {
            assertTrue(threads.get(i).isAlive(), "sleep #" + i + " ended early: " + outcomes[i]);
            threads.get(i).interrupt();
        }
        for (var thread : threads) {
            thread.join();
        }
        for (var i = 0; i < outcomes.length; ++i) {
            assertEquals("interrupted", outcomes[i], "sleep #" + i);
        }
    }

    // sleep: "InterruptedException - if any thread has interrupted the current thread. The interrupted status of
    // the current thread is cleared when this exception is thrown."
    @Test
    public void sleepThrowsForEarlierInterrupt() {
        var current = Thread.currentThread();
        var sleeps = new ArrayList<Runnable>();
        sleeps.add(() -> sleepUnchecked(5000));
        sleeps.add(() -> sleepUnchecked(Duration.ofSeconds(5)));
        for (var i = 0; i < sleeps.size(); ++i) {
            current.interrupt();
            long start = System.currentTimeMillis();
            try {
                sleeps.get(i).run();
                fail("sleep #" + i + " did not throw");
            } catch (RuntimeException e) {
                assertTrue(e.getCause() instanceof InterruptedException, "sleep #" + i + ": " + e);
            }
            assertTrue(System.currentTimeMillis() - start < 2500, "sleep #" + i + " waited");
            assertFalse(current.isInterrupted(), "sleep #" + i + " left the interrupt status set");
        }
    }

    private static void sleepUnchecked(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sleepUnchecked(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // join(long millis, int nanos): "IllegalArgumentException - if the value of millis is negative, or the value
    // of nanos is not in the range 0-999999". join(long millis): "If the thread has not been started then this
    // method returns immediately, without waiting."
    @Test
    public void joinEdgeCases() throws InterruptedException {
        var notStarted = new Thread(() -> { });
        long start = System.currentTimeMillis();
        notStarted.join(1000);
        assertTrue(System.currentTimeMillis() - start < 500, "join() waited for a thread that was not started");
        notStarted.join();
        notStarted.join(0, 1);

        for (var thread : List.of(notStarted, Thread.currentThread())) {
            assertThrows(IllegalArgumentException.class, () -> thread.join(-1));
            assertThrows(IllegalArgumentException.class, () -> thread.join(0, -1));
            assertThrows(IllegalArgumentException.class, () -> thread.join(0, 1_000_000));
            assertThrows(IllegalArgumentException.class, () -> thread.join(Long.MIN_VALUE, 0));
        }

        // A timed join on a thread that outlives the timeout waits for the timeout, then returns.
        var longRunning = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // ok
            }
        });
        longRunning.start();
        start = System.currentTimeMillis();
        longRunning.join(100);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 100 && elapsed < 5000, "join(100) took " + elapsed);
        assertTrue(longRunning.isAlive());
        start = System.currentTimeMillis();
        longRunning.join(50, 500_000);
        elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 50 && elapsed < 5000, "join(50, 500000) took " + elapsed);
        assertTrue(longRunning.isAlive());
        longRunning.interrupt();
        longRunning.join();
        assertFalse(longRunning.isAlive());
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void assertThrows(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
        } catch (Throwable e) {
            if (type.isInstance(e)) {
                return;
            }
            fail("Expected " + type.getName() + ", got " + e);
        }
        fail("Expected " + type.getName());
    }

    @Test
    public void joinWithLongMaxValueWaits() throws InterruptedException {
        var done = new boolean[1];
        var thread = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ignore
            }
            done[0] = true;
        });
        thread.start();
        thread.join(Long.MAX_VALUE);
        assertTrue(done[0]);
        assertFalse(thread.isAlive());
    }

    // getContextClassLoader(): "The context ClassLoader is provided by the creator of the thread". It may be null.
    @Test
    public void contextClassLoaderIsKeptAndInherited() throws InterruptedException {
        var current = Thread.currentThread();
        var original = current.getContextClassLoader();
        var loader = new ClassLoader() { };
        try {
            var createdBefore = new Thread(() -> { });
            current.setContextClassLoader(loader);
            assertSame(loader, current.getContextClassLoader());
            assertSame(original, createdBefore.getContextClassLoader());
            var seen = new ClassLoader[2];
            var childLoader = new ClassLoader() { };
            var thread = new Thread(() -> {
                seen[0] = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(childLoader);
                seen[1] = Thread.currentThread().getContextClassLoader();
            });
            thread.start();
            thread.join();
            assertSame(loader, seen[0]);
            assertSame(childLoader, seen[1]);
            assertSame(loader, current.getContextClassLoader());

            current.setContextClassLoader(null);
            assertNull(current.getContextClassLoader());
            var inheritsNull = new ClassLoader[] { loader };
            thread = new Thread(() -> inheritsNull[0] = Thread.currentThread().getContextClassLoader());
            thread.start();
            thread.join();
            assertNull(inheritsNull[0]);
        } finally {
            current.setContextClassLoader(original);
        }
    }
}
