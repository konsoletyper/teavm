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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class ConcurrentCoreTest {

    @Test
    public void futureTaskCompleted() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> "hello");
        task.run();
        assertTrue(task.isDone());
        assertFalse(task.isCancelled());
        assertEquals("hello", task.get());
    }

    @Test
    public void futureTaskException() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> {
            throw new RuntimeException("test error");
        });
        task.run();
        assertTrue(task.isDone());
        assertFalse(task.isCancelled());
        try {
            task.get();
            fail("Expected ExecutionException");
        } catch (java.util.concurrent.ExecutionException e) {
            assertEquals("test error", e.getCause().getMessage());
        }
    }

    @Test
    public void futureTaskCancel() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> "hello");
        assertTrue(task.cancel(true));
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
    }

    @Test
    public void futureTaskAlreadyDone() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> "hello");
        task.run();
        assertFalse(task.cancel(true));
        assertEquals("hello", task.get());
    }

    @Test
    public void futureTaskWithResult() throws Exception {
        FutureTask<Integer> task = new FutureTask<>(() -> {}, 42);
        task.run();
        assertTrue(task.isDone());
        assertEquals(Integer.valueOf(42), task.get());
    }

    @Test
    public void countDownLatchImmediate() throws Exception {
        CountDownLatch latch = new CountDownLatch(0);
        assertTrue(latch.getCount() == 0);
        latch.await();
    }

    @Test
    public void countDownLatchCountDown() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        assertEquals(3, latch.getCount());
        latch.countDown();
        assertEquals(2, latch.getCount());
        latch.countDown();
        assertEquals(1, latch.getCount());
        latch.countDown();
        assertEquals(0, latch.getCount());
    }

    @Test
    public void countDownLatchAwait() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        latch.await();
    }

    @Test
    public void semaphoreAcquireRelease() throws Exception {
        Semaphore semaphore = new Semaphore(3);
        assertEquals(3, semaphore.availablePermits());
        semaphore.acquire();
        assertEquals(2, semaphore.availablePermits());
        semaphore.acquire();
        assertEquals(1, semaphore.availablePermits());
        semaphore.release();
        assertEquals(2, semaphore.availablePermits());
    }

    @Test
    public void semaphoreTryAcquire() throws Exception {
        Semaphore semaphore = new Semaphore(1);
        assertTrue(semaphore.tryAcquire());
        assertFalse(semaphore.tryAcquire());
        semaphore.release();
        assertTrue(semaphore.tryAcquire());
    }

    @Test
    public void semaphoreDrainPermits() throws Exception {
        Semaphore semaphore = new Semaphore(5);
        assertEquals(5, semaphore.drainPermits());
        assertEquals(0, semaphore.availablePermits());
    }

    @Test
    public void semaphoreFair() {
        Semaphore fair = new Semaphore(1, true);
        assertTrue(fair.isFair());
        Semaphore unfair = new Semaphore(1, false);
        assertFalse(unfair.isFair());
    }

    @Test
    public void timeoutException() {
        TimeoutException ex = new TimeoutException();
        assertNull(ex.getMessage());
        TimeoutException ex2 = new TimeoutException("timed out");
        assertEquals("timed out", ex2.getMessage());
    }

    @Test
    public void rejectedExecutionException() {
        java.util.concurrent.RejectedExecutionException ex = new java.util.concurrent.RejectedExecutionException();
        assertNull(ex.getMessage());
        java.util.concurrent.RejectedExecutionException ex2 = new java.util.concurrent.RejectedExecutionException("rejected");
        assertEquals("rejected", ex2.getMessage());
    }

    @Test
    public void completionException() {
        java.util.concurrent.CompletionException ex = new java.util.concurrent.CompletionException();
        assertNull(ex.getMessage());
        java.util.concurrent.CompletionException ex2 = new java.util.concurrent.CompletionException("failed", new RuntimeException());
        assertEquals("failed", ex2.getMessage());
    }
}
