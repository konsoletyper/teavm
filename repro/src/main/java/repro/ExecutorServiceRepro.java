/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

public final class ExecutorServiceRepro {
    private ExecutorServiceRepro() {
    }

    public static void main(String[] args) throws Exception {
        int[] threadFactoryCalls = { 0 };
        ExecutorService executor = Executors.newCachedThreadPool(task -> {
            ++threadFactoryCalls[0];
            return new Thread(task, "unciv-worker");
        });

        Future<Integer> result = executor.submit(() -> {
            check("unciv-worker".equals(Thread.currentThread().getName()), "custom thread factory");
            return 42;
        });
        check(result.get(1, TimeUnit.SECONDS) == 42, "Future.get timed result");
        Future<Integer> reused = executor.submit(() -> 7);
        check(reused.get(1, TimeUnit.SECONDS) == 7, "cached worker result");
        check(threadFactoryCalls[0] >= 1, "thread factory invoked");

        Future<Integer> delayed = executor.submit(() -> {
            Thread.sleep(10);
            return 5;
        });
        try {
            delayed.get(0, TimeUnit.MILLISECONDS);
            throw new AssertionError("timed get should time out");
        } catch (TimeoutException expected) {
            // Expected: zero timeout does not block even if the worker has not started yet.
        }
        check(delayed.get(1, TimeUnit.SECONDS) == 5, "Future completion after timeout");

        FutureTask<Integer> completed = new FutureTask<>(() -> 9);
        completed.run();
        check(completed.get(-1, TimeUnit.SECONDS) == 9, "completed Future accepts negative timeout");
        FutureTask<Integer> incomplete = new FutureTask<>(() -> 10);
        try {
            incomplete.get(-1, TimeUnit.SECONDS);
            throw new AssertionError("incomplete Future with negative timeout should time out");
        } catch (TimeoutException expected) {
            // Expected: a nonpositive timeout does not wait.
        }
        executor.shutdown();
        check(executor.awaitTermination(1, TimeUnit.SECONDS), "executor termination");
        try {
            executor.execute(() -> { });
            throw new AssertionError("shutdown executor should reject work");
        } catch (RejectedExecutionException expected) {
            // Expected.
        }

        ExecutorService dependentExecutor = Executors.newCachedThreadPool();
        FutureTask<Integer> dependency = new FutureTask<>(() -> 42);
        Future<Integer> waiting = dependentExecutor.submit(() -> dependency.get() + 1);
        dependentExecutor.execute(dependency);
        check(waiting.get(1, TimeUnit.SECONDS) == 43, "cached pool starts work beyond waiting workers");
        dependentExecutor.shutdown();
        check(dependentExecutor.awaitTermination(1, TimeUnit.SECONDS), "dependent executor termination");

        AtomicReferenceArray<Integer> array = new AtomicReferenceArray<>(1);
        array.set(0, 1);
        check(array.compareAndSet(0, 1, 2) && array.get(0) == 2, "atomic reference array overlay");
        try {
            array.get(1);
            throw new AssertionError("atomic array should check bounds");
        } catch (IndexOutOfBoundsException expected) {
            // Expected.
        }

        ExecutorService cancelledExecutor = Executors.newCachedThreadPool();
        FutureTask<Integer> cancelled = new FutureTask<>(() -> 1);
        check(cancelled.cancel(true), "future cancellation");
        cancelledExecutor.execute(cancelled);
        try {
            cancelled.get();
            throw new AssertionError("cancelled Future.get should throw");
        } catch (CancellationException expected) {
            // Expected.
        }
        cancelledExecutor.shutdownNow();
        check(cancelledExecutor.awaitTermination(1, TimeUnit.SECONDS), "shutdownNow termination");
        System.out.println("executor=ok");

        Thread caller = Thread.currentThread();
        testLockSupport();
        check(Thread.currentThread() == caller, "park restores caller thread identity");
        System.out.println("executor=ok,park=ok");
    }

    private static void testLockSupport() throws Exception {
        Object blocker = ExecutorServiceRepro.class;
        AtomicInteger resumed = new AtomicInteger();
        Thread worker = new Thread(() -> {
            LockSupport.park(blocker);
            resumed.set(1);
        }, "park-worker");
        worker.start();
        Thread.sleep(1);
        check(LockSupport.getBlocker(worker) == blocker, "park blocker");
        LockSupport.unpark(worker);
        worker.join();
        check(resumed.get() == 1, "unpark a parked thread");

        AtomicInteger interrupted = new AtomicInteger();
        Thread interruptWorker = new Thread(() -> {
            LockSupport.park(blocker);
            interrupted.set(Thread.currentThread().isInterrupted() ? 1 : 0);
        }, "interrupt-worker");
        interruptWorker.start();
        Thread.sleep(1);
        interruptWorker.interrupt();
        interruptWorker.join();
        check(interrupted.get() == 1, "park preserves interrupt status");

        AtomicInteger timed = new AtomicInteger();
        Thread timedWorker = new Thread(() -> {
            LockSupport.parkNanos(blocker, 1_000_000L);
            timed.set(1);
        }, "timed-park-worker");
        timedWorker.start();
        timedWorker.join();
        check(timed.get() == 1, "timed park resumes");

        AtomicInteger earlyUnpark = new AtomicInteger();
        Thread earlyWorker = new Thread(() -> {
            LockSupport.park();
            earlyUnpark.set(1);
        }, "early-unpark-worker");
        earlyWorker.start();
        LockSupport.unpark(earlyWorker);
        earlyWorker.join();
        check(earlyUnpark.get() == 1, "unpark permit before park");

        AtomicInteger zeroTimeoutPermit = new AtomicInteger();
        Thread zeroTimeoutWorker = new Thread(() -> {
            LockSupport.parkNanos(0);
            LockSupport.park();
            zeroTimeoutPermit.set(1);
        }, "zero-timeout-worker");
        zeroTimeoutWorker.start();
        LockSupport.unpark(zeroTimeoutWorker);
        zeroTimeoutWorker.join();
        check(zeroTimeoutPermit.get() == 1, "zero timeout preserves permit");

        AtomicInteger repeatedUnpark = new AtomicInteger();
        Thread repeatedWorker = new Thread(() -> {
            LockSupport.park();
            repeatedUnpark.incrementAndGet();
            LockSupport.park();
            repeatedUnpark.incrementAndGet();
        }, "repeated-unpark-worker");
        repeatedWorker.start();
        Thread.sleep(1);
        LockSupport.unpark(repeatedWorker);
        LockSupport.unpark(repeatedWorker);
        Thread.sleep(1);
        check(repeatedUnpark.get() == 1, "repeated unparks coalesce");
        LockSupport.unpark(repeatedWorker);
        repeatedWorker.join();
        check(repeatedUnpark.get() == 2, "second park requires another unpark");
        System.out.println("park-races=ok");

        try {
            LockSupport.getBlocker(null);
            throw new AssertionError("getBlocker(null) should throw");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            System.out.println("failed:" + message);
            throw new AssertionError(message);
        }
    }
}
