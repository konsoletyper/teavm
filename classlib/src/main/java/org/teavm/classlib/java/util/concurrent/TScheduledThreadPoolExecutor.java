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
package org.teavm.classlib.java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.classlib.java.lang.TThread;

public class TScheduledThreadPoolExecutor extends TThreadPoolExecutor implements TScheduledExecutorService {
    private static final TThreadFactory DEFAULT_THREAD_FACTORY = TThread::new;

    private final TDelayedWorkQueue<TRunnable> delayedWorkQueue;
    private long nextSequenceNumber;
    private volatile boolean shutdownNow;
    private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;
    private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;
    private volatile boolean removeOnCancelPolicy;

    public TScheduledThreadPoolExecutor(int corePoolSize) {
        this(corePoolSize, DEFAULT_THREAD_FACTORY, new AbortPolicy());
    }

    public TScheduledThreadPoolExecutor(int corePoolSize, TThreadFactory threadFactory) {
        this(corePoolSize, threadFactory, new AbortPolicy());
    }

    public TScheduledThreadPoolExecutor(int corePoolSize, TRejectedExecutionHandler handler) {
        this(corePoolSize, DEFAULT_THREAD_FACTORY, handler);
    }

    public TScheduledThreadPoolExecutor(int corePoolSize, TThreadFactory threadFactory,
            TRejectedExecutionHandler handler) {
        this(corePoolSize, new TDelayedWorkQueue<>(), threadFactory, handler);
    }

    private TScheduledThreadPoolExecutor(int corePoolSize, TDelayedWorkQueue<TRunnable> workQueue,
            TThreadFactory threadFactory, TRejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, 0, TTimeUnit.NANOSECONDS, workQueue, threadFactory, handler);
        delayedWorkQueue = workQueue;
    }

    @Override
    public TScheduledFuture<TObject> schedule(TRunnable command, long delay, TTimeUnit unit) {
        Objects.requireNonNull(command);
        long triggerTime = triggerTime(delay, unit);
        return delayedExecute(new ScheduledTask<>(command, null, triggerTime, 0, false, nextSequenceNumber()));
    }

    @Override
    public <V extends TObject> TScheduledFuture<V> schedule(TCallable<V> callable, long delay, TTimeUnit unit) {
        Objects.requireNonNull(callable);
        long triggerTime = triggerTime(delay, unit);
        return delayedExecute(new ScheduledTask<>(callable, triggerTime, 0, false, nextSequenceNumber()));
    }

    @Override
    public TScheduledFuture<?> scheduleAtFixedRate(TRunnable command, long initialDelay, long period, TTimeUnit unit) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(unit);
        long periodNanos = unit.toNanos(period);
        if (periodNanos <= 0) {
            throw new IllegalArgumentException("period must be positive");
        }
        ScheduledTask<TObject> task = new ScheduledTask<>(command, null,
                triggerTime(initialDelay, unit), periodNanos, true, nextSequenceNumber());
        return delayedExecute(task);
    }

    @Override
    public TScheduledFuture<?> scheduleWithFixedDelay(TRunnable command, long initialDelay, long delay,
            TTimeUnit unit) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(unit);
        long delayNanos = unit.toNanos(delay);
        if (delayNanos <= 0) {
            throw new IllegalArgumentException("delay must be positive");
        }
        ScheduledTask<TObject> task = new ScheduledTask<>(command, null,
                triggerTime(initialDelay, unit), -delayNanos, false, nextSequenceNumber());
        return delayedExecute(task);
    }

    private long triggerTime(long delay, TTimeUnit unit) {
        Objects.requireNonNull(unit);
        long delayNanos = Math.max(0, unit.toNanos(delay));
        long now = System.nanoTime();
        if (delayNanos > 0 && now > Long.MAX_VALUE - delayNanos) {
            return Long.MAX_VALUE;
        }
        return now + delayNanos;
    }

    private synchronized long nextSequenceNumber() {
        return nextSequenceNumber++;
    }

    @Override
    protected synchronized boolean timedOutWorker() {
        if (!delayedWorkQueue.isEmpty()) {
            return false;
        }
        return super.timedOutWorker();
    }

    private <V extends TObject> TScheduledFuture<V> delayedExecute(ScheduledTask<V> task) {
        if (isShutdown()) {
            reject(task);
            return task;
        }
        delayedWorkQueue.offer(task);
        if (isShutdown() && delayedWorkQueue.remove(task)) {
            task.cancel(false);
            reject(task);
            return task;
        }
        try {
            ensureWorkerForQueuedTask();
        } catch (RuntimeException | Error e) {
            delayedWorkQueue.remove(task);
            throw e;
        }
        return task;
    }

    private boolean enqueuePeriodic(ScheduledTask<?> task) {
        synchronized (this) {
            if (shutdownNow || (isShutdown() && !continueExistingPeriodicTasksAfterShutdownPolicy)) {
                return false;
            }
            if (task.isCancelled()) {
                return false;
            }
            delayedWorkQueue.offer(task);
            return true;
        }
    }

    private void taskCancelled(ScheduledTask<?> task) {
        if (removeOnCancelPolicy) {
            delayedWorkQueue.remove(task);
            tryTerminate();
        }
    }

    @Override
    public void execute(TRunnable command) {
        schedule(command, 0, TTimeUnit.NANOSECONDS);
    }

    @Override
    public synchronized void setCorePoolSize(int corePoolSize) {
        super.setCorePoolSize(corePoolSize);
    }

    public synchronized void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        continueExistingPeriodicTasksAfterShutdownPolicy = value;
        if (!value && isShutdown()) {
            onShutdown();
        }
    }

    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return continueExistingPeriodicTasksAfterShutdownPolicy;
    }

    public synchronized void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        executeExistingDelayedTasksAfterShutdownPolicy = value;
        if (!value && isShutdown()) {
            onShutdown();
        }
    }

    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return executeExistingDelayedTasksAfterShutdownPolicy;
    }

    public void setRemoveOnCancelPolicy(boolean value) {
        removeOnCancelPolicy = value;
        if (value) {
            purge();
        }
    }

    public boolean getRemoveOnCancelPolicy() {
        return removeOnCancelPolicy;
    }

    @Override
    public synchronized void shutdown() {
        onShutdown();
        super.shutdown();
    }

    private void onShutdown() {
        List<TRunnable> toCancel = new ArrayList<>();
        for (org.teavm.classlib.java.util.TIterator<TRunnable> iterator = delayedWorkQueue.iterator();
                iterator.hasNext();) {
            TRunnable runnable = iterator.next();
            if (!(runnable instanceof ScheduledTask)) {
                continue;
            }
            ScheduledTask<?> task = (ScheduledTask<?>) runnable;
            if ((task.isPeriodic() && !continueExistingPeriodicTasksAfterShutdownPolicy)
                    || (!task.isPeriodic() && !executeExistingDelayedTasksAfterShutdownPolicy)) {
                toCancel.add(task);
            }
        }
        for (TRunnable runnable : toCancel) {
            ((ScheduledTask<?>) runnable).cancel(false);
            delayedWorkQueue.remove(runnable);
        }
    }

    @Override
    public List<TRunnable> shutdownNow() {
        shutdownNow = true;
        return super.shutdownNow();
    }

    private final class ScheduledTask<V extends TObject> implements TRunnableScheduledFuture<V> {
        private static final int NEW = 0;
        private static final int RUNNING = 1;
        private static final int SUCCESS = 2;
        private static final int FAILED = 3;
        private static final int CANCELLED = 4;

        private final TRunnable runnable;
        private final TCallable<V> callable;
        private final V runnableResult;
        private final long periodNanos;
        private final boolean fixedRate;
        private final long sequenceNumber;
        private long time;
        private int state;
        private V result;
        private Throwable failure;
        private TThread runner;

        ScheduledTask(TRunnable runnable, V result, long time, long periodNanos, boolean fixedRate,
                long sequenceNumber) {
            this.runnable = runnable;
            callable = null;
            runnableResult = result;
            this.time = time;
            this.periodNanos = periodNanos;
            this.fixedRate = fixedRate;
            this.sequenceNumber = sequenceNumber;
        }

        ScheduledTask(TCallable<V> callable, long time, long periodNanos, boolean fixedRate, long sequenceNumber) {
            runnable = null;
            this.callable = callable;
            runnableResult = null;
            this.time = time;
            this.periodNanos = periodNanos;
            this.fixedRate = fixedRate;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public void run() {
            synchronized (this) {
                if (state != NEW) {
                    return;
                }
                if (getDelay(TTimeUnit.NANOSECONDS) > 0) {
                    return;
                }
                state = RUNNING;
                runner = TThread.currentThread();
            }

            V value = null;
            Throwable error = null;
            try {
                if (callable != null) {
                    value = callable.call();
                } else {
                    runnable.run();
                    value = runnableResult;
                }
            } catch (Throwable e) {
                error = e;
            }

            boolean repeat = false;
            synchronized (this) {
                runner = null;
                if (state == CANCELLED) {
                    notifyAll();
                    return;
                }
                if (error != null) {
                    failure = error;
                    state = FAILED;
                } else if (isPeriodic()) {
                    time = fixedRate ? saturatingAdd(time, periodNanos)
                            : saturatingAdd(System.nanoTime(), -periodNanos);
                    state = NEW;
                    repeat = true;
                } else {
                    result = value;
                    state = SUCCESS;
                }
                notifyAll();
            }
            if (repeat && !enqueuePeriodic(this)) {
                cancel(false);
            }
        }

        private long saturatingAdd(long left, long right) {
            if (right > 0 && left > Long.MAX_VALUE - right) {
                return Long.MAX_VALUE;
            }
            if (right < 0 && left < Long.MIN_VALUE - right) {
                return Long.MIN_VALUE;
            }
            return left + right;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            TThread toInterrupt;
            synchronized (this) {
                if (state == SUCCESS || state == FAILED || state == CANCELLED) {
                    return false;
                }
                state = CANCELLED;
                toInterrupt = mayInterruptIfRunning ? runner : null;
                notifyAll();
            }
            if (toInterrupt != null) {
                toInterrupt.interrupt();
            }
            taskCancelled(this);
            return true;
        }

        @Override
        public synchronized boolean isCancelled() {
            return state == CANCELLED;
        }

        @Override
        public synchronized boolean isDone() {
            return state == SUCCESS || state == FAILED || state == CANCELLED;
        }

        @Override
        public V get() throws InterruptedException, TExecutionException {
            synchronized (this) {
                while (!isDone()) {
                    wait();
                }
                return reportResult();
            }
        }

        @Override
        public V get(long timeout, TTimeUnit unit)
                throws InterruptedException, TExecutionException, TTimeoutException {
            Objects.requireNonNull(unit);
            long timeoutNanos = unit.toNanos(timeout);
            long start = System.nanoTime();
            synchronized (this) {
                while (!isDone()) {
                    long remaining = timeoutNanos - (System.nanoTime() - start);
                    if (remaining <= 0) {
                        throw new TTimeoutException();
                    }
                    wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                }
                return reportResult();
            }
        }

        private V reportResult() throws TExecutionException {
            if (state == CANCELLED) {
                throw new TCancellationException();
            }
            if (failure != null) {
                throw new TExecutionException(failure);
            }
            return result;
        }

        @Override
        public long getDelay(TTimeUnit unit) {
            return Objects.requireNonNull(unit).convert(time - System.nanoTime(), TTimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(TDelayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof TScheduledThreadPoolExecutor.ScheduledTask) {
                ScheduledTask<?> that = (ScheduledTask<?>) other;
                int result = Long.compare(time, that.time);
                return result != 0 ? result : Long.compare(sequenceNumber, that.sequenceNumber);
            }
            return Long.compare(getDelay(TTimeUnit.NANOSECONDS), other.getDelay(TTimeUnit.NANOSECONDS));
        }

        @Override
        public boolean isPeriodic() {
            return periodNanos != 0;
        }
    }
}
