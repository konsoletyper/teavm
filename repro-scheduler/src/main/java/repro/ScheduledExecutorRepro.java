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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScheduledExecutorRepro {
    private static final AtomicInteger uncaughtErrors = new AtomicInteger();
    private static volatile String testPhase = "startup";

    private ScheduledExecutorRepro() {
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            uncaughtErrors.incrementAndGet();
            System.err.println("uncaught worker error during " + testPhase + ": " + error);
            for (StackTraceElement frame : error.getStackTrace()) {
                System.err.println(frame);
            }
        });
        testInterruptedWaitReacquiresMonitor();
        testPhase = "one-shot";
        testOneShotAndOrdering();
        testPhase = "cancel and periodic";
        testCancellationAndPeriodicScheduling();
        testPhase = "shutdown policy";
        testShutdownPolicies();
        testPhase = "factory decline";
        testThreadFactoryDeclineAndNullTimeUnit();
        testTerminationHook();
        testPhase = "complete";
        check(uncaughtErrors.get() == 0, "worker threads complete without uncaught errors");
        System.out.println("scheduled-executor=ok");
    }

    private static void testOneShotAndOrdering() throws Exception {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        List<Integer> order = new CopyOnWriteArrayList<>();
        ScheduledFuture<String> first = executor.schedule(() -> "done", 25, TimeUnit.MILLISECONDS);
        ScheduledFuture<Integer> timed = executor.schedule(() -> 9, 250, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> late = executor.schedule(() -> order.add(2), 60, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> early = executor.schedule(() -> order.add(1), 10, TimeUnit.MILLISECONDS);
        try {
            first.get(2, TimeUnit.SECONDS);
            early.get(2, TimeUnit.SECONDS);
            late.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("scheduled work did not run before the deadline", e);
        }
        check("done".equals(first.get()), "callable result");
        try {
            timed.get(0, TimeUnit.MILLISECONDS);
            throw new AssertionError("incomplete scheduled future should time out immediately");
        } catch (TimeoutException expected) {
            // Expected.
        }
        check(timed.get(2, TimeUnit.SECONDS) == 9, "scheduled future completes after a timed wait");
        check(order.size() == 2 && order.get(0) == 1 && order.get(1) == 2, "deadline ordering");
        executor.shutdown();
        check(executor.awaitTermination(2, TimeUnit.SECONDS), "one-shot executor termination");
    }

    private static void testCancellationAndPeriodicScheduling() throws Exception {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        AtomicInteger cancelledRuns = new AtomicInteger();
        ScheduledFuture<?> cancelled = executor.schedule(cancelledRuns::incrementAndGet, 120, TimeUnit.MILLISECONDS);
        check(cancelled.cancel(false), "cancel a delayed task");
        check(executor.getQueue().isEmpty(), "remove-on-cancel policy removes delayed task");
        Thread.sleep(150);
        check(cancelledRuns.get() == 0, "cancelled task does not execute");

        AtomicInteger fixedRateRuns = new AtomicInteger();
        ScheduledFuture<?> fixedRate = executor.scheduleAtFixedRate(fixedRateRuns::incrementAndGet,
                0, 10, TimeUnit.MILLISECONDS);
        Thread.sleep(80);
        check(fixedRate.cancel(false), "cancel fixed-rate task");
        int fixedRateCount = fixedRateRuns.get();
        Thread.sleep(30);
        check(fixedRateCount >= 2 && fixedRateRuns.get() == fixedRateCount, "fixed-rate cancellation stops rescheduling");

        AtomicInteger fixedDelayRuns = new AtomicInteger();
        ScheduledFuture<?> fixedDelay = executor.scheduleWithFixedDelay(fixedDelayRuns::incrementAndGet,
                0, 10, TimeUnit.MILLISECONDS);
        Thread.sleep(80);
        check(fixedDelay.cancel(false), "cancel fixed-delay task");
        int fixedDelayCount = fixedDelayRuns.get();
        Thread.sleep(30);
        check(fixedDelayCount >= 2 && fixedDelayRuns.get() == fixedDelayCount,
                "fixed-delay cancellation stops rescheduling");
        executor.shutdown();
        check(executor.awaitTermination(2, TimeUnit.SECONDS), "periodic executor termination");
    }

    private static void testShutdownPolicies() throws Exception {
        ScheduledThreadPoolExecutor defaultPolicy = new ScheduledThreadPoolExecutor(1);
        ScheduledFuture<Integer> delayed = defaultPolicy.schedule(() -> 7, 20, TimeUnit.MILLISECONDS);
        defaultPolicy.shutdown();
        check(delayed.get(2, TimeUnit.SECONDS) == 7, "shutdown runs existing delayed tasks by default");
        check(defaultPolicy.awaitTermination(2, TimeUnit.SECONDS), "shutdown drains delayed work");
        try {
            defaultPolicy.execute(() -> { });
            throw new AssertionError("shutdown executor should reject new work");
        } catch (java.util.concurrent.RejectedExecutionException expected) {
            // Expected.
        }

        ScheduledThreadPoolExecutor cancelDelayed = new ScheduledThreadPoolExecutor(1);
        cancelDelayed.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        ScheduledFuture<?> afterShutdown = cancelDelayed.schedule(() -> { }, 1, TimeUnit.SECONDS);
        cancelDelayed.shutdown();
        check(afterShutdown.isCancelled(), "shutdown policy cancels delayed work");
        check(cancelDelayed.awaitTermination(2, TimeUnit.SECONDS), "policy shutdown termination");

        ScheduledThreadPoolExecutor continuePeriodic = new ScheduledThreadPoolExecutor(1);
        continuePeriodic.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
        AtomicInteger runs = new AtomicInteger();
        ScheduledFuture<?> periodic = continuePeriodic.scheduleAtFixedRate(runs::incrementAndGet,
                0, 10, TimeUnit.MILLISECONDS);
        Thread.sleep(50);
        continuePeriodic.shutdown();
        Thread.sleep(40);
        int continuedRuns = runs.get();
        check(continuedRuns >= 2, "periodic policy retains existing periodic task on shutdown");
        check(periodic.cancel(false), "cancel periodic task after shutdown");
        check(continuePeriodic.awaitTermination(2, TimeUnit.SECONDS), "periodic shutdown termination");

        ScheduledThreadPoolExecutor forcedShutdown = new ScheduledThreadPoolExecutor(1);
        forcedShutdown.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
        AtomicInteger forcedRuns = new AtomicInteger();
        forcedShutdown.scheduleAtFixedRate(forcedRuns::incrementAndGet, 0, 10, TimeUnit.MILLISECONDS);
        Thread.sleep(40);
        forcedShutdown.shutdownNow();
        check(forcedShutdown.awaitTermination(2, TimeUnit.SECONDS), "shutdownNow stops periodic executor");
        int stoppedRuns = forcedRuns.get();
        Thread.sleep(30);
        check(stoppedRuns >= 1 && forcedRuns.get() == stoppedRuns,
                "shutdownNow stops periodic work even when graceful policy allows it");
    }

    private static void testThreadFactoryDeclineAndNullTimeUnit() throws Exception {
        try {
            new ThreadPoolExecutor(1, 1, 0, null, new ArrayBlockingQueue<>(4));
            throw new AssertionError("null keep-alive unit should fail");
        } catch (NullPointerException expected) {
            // Expected.
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(4), task -> null);
        AtomicInteger runs = new AtomicInteger();
        executor.execute(runs::incrementAndGet);
        check(executor.getQueue().size() == 1, "factory decline leaves task queued");
        ThreadFactory workingFactory = task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        };
        executor.setThreadFactory(workingFactory);
        executor.execute(runs::incrementAndGet);
        executor.shutdown();
        check(executor.awaitTermination(2, TimeUnit.SECONDS), "factory recovery drains queued work");
        check(runs.get() == 2, "queued and new tasks execute after factory recovery");
    }

    private static void testTerminationHook() throws Exception {
        TrackingExecutor executor = new TrackingExecutor();
        Future<?> completed = executor.submit(() -> { });
        completed.get(2, TimeUnit.SECONDS);
        Thread.sleep(60);
        check(executor.getPoolSize() == 0, "non-core worker times out");
        check(executor.terminationCount.get() == 0, "worker timeout does not terminate a running executor");
        check(!executor.isTerminated(), "running executor remains un-terminated after idle timeout");
        executor.shutdown();
        check(executor.awaitTermination(2, TimeUnit.SECONDS), "zero-worker shutdown terminates");
        check(executor.terminationCount.get() == 1, "termination hook runs once on shutdown");
    }

    private static void testInterruptedWaitReacquiresMonitor() throws Exception {
        Object lock = new Object();
        AtomicInteger state = new AtomicInteger();
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                synchronized (lock) {
                    state.set(1);
                    try {
                        lock.wait();
                        state.set(-1);
                    } catch (InterruptedException expected) {
                        state.set(Thread.holdsLock(lock) && !Thread.currentThread().isInterrupted() ? 2 : -2);
                    }
                }
            }
            if (state.get() == 2) {
                state.set(3);
            }
        });
        waiter.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (state.get() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        check(state.get() == 1, "waiter enters the reentrant monitor");
        synchronized (lock) {
            waiter.interrupt();
            Thread.sleep(20);
            check(state.get() == 1, "interrupted waiter waits to reacquire the owned monitor");
        }
        waiter.join(2_000);
        check(!waiter.isAlive(), "interrupted waiter terminates");
        check(state.get() == 3, "interrupted wait reacquires full monitor depth and clears interrupt status");

        synchronized (lock) {
            Thread.currentThread().interrupt();
            try {
                lock.wait();
                throw new AssertionError("pre-interrupted wait should throw");
            } catch (InterruptedException expected) {
                check(Thread.holdsLock(lock), "pre-interrupted wait retains monitor ownership");
                check(!Thread.currentThread().isInterrupted(), "pre-interrupted wait clears interrupt status");
            }
            try {
                lock.wait(-1);
                throw new AssertionError("negative wait timeout should throw");
            } catch (IllegalArgumentException expected) {
                // Expected.
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TrackingExecutor extends ThreadPoolExecutor {
        private final AtomicInteger terminationCount = new AtomicInteger();

        TrackingExecutor() {
            super(0, 1, 20, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4));
        }

        @Override
        protected void terminated() {
            terminationCount.incrementAndGet();
        }
    }
}
