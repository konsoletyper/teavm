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
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.util.TIterator;

public class TThreadPoolExecutor extends TAbstractExecutorService {
    private static final TThreadFactory DEFAULT_THREAD_FACTORY = TThread::new;
    private static final TRejectedExecutionHandler DEFAULT_HANDLER = new AbortPolicy();

    private final TBlockingQueue<TRunnable> workQueue;
    private final List<Worker> workers = new ArrayList<>();
    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveNanos;
    private boolean allowCoreThreadTimeOut;
    private boolean shutdown;
    private boolean stop;
    private boolean terminationNotified;
    private int activeCount;
    private int largestPoolSize;
    private long completedTaskCount;
    private TThreadFactory threadFactory;
    private TRejectedExecutionHandler rejectedExecutionHandler;

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<TRunnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, DEFAULT_THREAD_FACTORY, DEFAULT_HANDLER);
    }

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<TRunnable> workQueue, TThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, DEFAULT_HANDLER);
    }

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<TRunnable> workQueue, TRejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, DEFAULT_THREAD_FACTORY, handler);
    }

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<TRunnable> workQueue, TThreadFactory threadFactory, TRejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("Invalid pool size");
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("Invalid keep-alive time");
        }
        Objects.requireNonNull(unit);
        this.workQueue = Objects.requireNonNull(workQueue);
        this.threadFactory = Objects.requireNonNull(threadFactory);
        rejectedExecutionHandler = Objects.requireNonNull(handler);
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        keepAliveNanos = unit.toNanos(keepAliveTime);
    }

    @Override
    public void execute(TRunnable command) {
        Objects.requireNonNull(command);
        synchronized (this) {
            if (shutdown) {
                reject(command);
                return;
            }
            if (workers.size() < corePoolSize && startWorker(command)) {
                return;
            }
            if (workQueue.offer(command)) {
                if (shutdown && workQueue.remove(command)) {
                    reject(command);
                } else if (workers.isEmpty()) {
                    startWorker(null);
                }
                return;
            }
            if (workers.size() < maximumPoolSize && startWorker(command)) {
                return;
            }
            reject(command);
        }
    }

    protected final void reject(TRunnable command) {
        rejectedExecutionHandler.rejectedExecution(command, this);
    }

    private boolean startWorker(TRunnable firstTask) {
        Worker worker = new Worker(firstTask);
        TThread thread = threadFactory.newThread(worker);
        if (thread == null) {
            return false;
        }
        worker.thread = thread;
        workers.add(worker);
        largestPoolSize = Math.max(largestPoolSize, workers.size());
        try {
            thread.start();
        } catch (RuntimeException | Error e) {
            workers.remove(worker);
            throw e;
        }
        return true;
    }

    private final class Worker implements TRunnable {
        private TRunnable firstTask;
        private TThread thread;
        private boolean idle;

        Worker(TRunnable firstTask) {
            this.firstTask = firstTask;
        }

        @Override
        public void run() {
            boolean completedAbruptly = true;
            try {
                runWorker();
                completedAbruptly = false;
            } finally {
                workerExited(this, completedAbruptly);
            }
        }

        private void runWorker() {
            TRunnable task = firstTask;
            firstTask = null;
            while (task != null || (task = getTask(this)) != null) {
                synchronized (TThreadPoolExecutor.this) {
                    ++activeCount;
                }
                Throwable failure = null;
                try {
                    beforeExecute(thread, task);
                    task.run();
                } catch (RuntimeException | Error e) {
                    failure = e;
                    throw e;
                } finally {
                    try {
                        afterExecute(task, failure);
                    } finally {
                        synchronized (TThreadPoolExecutor.this) {
                            --activeCount;
                            ++completedTaskCount;
                        }
                    }
                }
                task = null;
            }
        }
    }

    private TRunnable getTask(Worker worker) {
        while (true) {
            synchronized (this) {
                if (stop || (shutdown && workQueue.isEmpty())) {
                    return null;
                }
                worker.idle = true;
            }
            TRunnable task;
            try {
                if (timedOutWorker()) {
                    task = workQueue.poll(keepAliveNanos, TTimeUnit.NANOSECONDS);
                } else {
                    task = workQueue.take();
                }
            } catch (InterruptedException e) {
                synchronized (this) {
                    worker.idle = false;
                    if (stop || (shutdown && workQueue.isEmpty())) {
                        return null;
                    }
                }
                continue;
            }
            synchronized (this) {
                worker.idle = false;
                if (task != null) {
                    return task;
                }
                if (shouldExitAfterTimeout()) {
                    return null;
                }
            }
        }
    }

    protected synchronized boolean timedOutWorker() {
        return allowCoreThreadTimeOut || workers.size() > corePoolSize;
    }

    private synchronized boolean shouldExitAfterTimeout() {
        return allowCoreThreadTimeOut ? !workers.isEmpty() : workers.size() > corePoolSize;
    }

    private void workerExited(Worker worker, boolean abrupt) {
        synchronized (this) {
            workers.remove(worker);
            worker.idle = false;
            if (!stop && (!shutdown || !workQueue.isEmpty())) {
                if ((!allowCoreThreadTimeOut && workers.size() < corePoolSize)
                        || (abrupt && !workQueue.isEmpty())) {
                    startWorker(null);
                }
            }
            notifyTerminationIfNeeded();
            notifyAll();
        }
    }

    @Override
    public void shutdown() {
        List<TThread> idleThreads = new ArrayList<>();
        synchronized (this) {
            if (shutdown) {
                return;
            }
            shutdown = true;
            for (Worker worker : workers) {
                if (worker.idle) {
                    idleThreads.add(worker.thread);
                }
            }
            if (workers.isEmpty() && !workQueue.isEmpty()) {
                startWorker(null);
            } else if (workers.isEmpty()) {
                notifyTerminationIfNeeded();
            }
            notifyAll();
        }
        for (TThread thread : idleThreads) {
            thread.interrupt();
        }
    }

    @Override
    public List<TRunnable> shutdownNow() {
        List<TRunnable> pending = new ArrayList<>();
        List<TThread> toInterrupt = new ArrayList<>();
        synchronized (this) {
            shutdown = true;
            stop = true;
            TIterator<TRunnable> iterator = workQueue.iterator();
            while (iterator.hasNext()) {
                TRunnable task = iterator.next();
                pending.add(task);
                iterator.remove();
            }
            for (Worker worker : workers) {
                toInterrupt.add(worker.thread);
            }
            notifyTerminationIfNeeded();
            notifyAll();
        }
        for (TThread thread : toInterrupt) {
            thread.interrupt();
        }
        return pending;
    }

    @Override
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    public synchronized boolean isTerminating() {
        return shutdown && !workers.isEmpty();
    }

    @Override
    public synchronized boolean isTerminated() {
        return shutdown && workers.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TTimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit);
        long timeoutNanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        synchronized (this) {
            while (!isTerminated()) {
                long remaining = timeoutNanos - (System.nanoTime() - start);
                if (remaining <= 0) {
                    return false;
                }
                wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            return true;
        }
    }

    public void setThreadFactory(TThreadFactory threadFactory) {
        this.threadFactory = Objects.requireNonNull(threadFactory);
    }

    public TThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setRejectedExecutionHandler(TRejectedExecutionHandler handler) {
        rejectedExecutionHandler = Objects.requireNonNull(handler);
    }

    public TRejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0 || corePoolSize > maximumPoolSize) {
            throw new IllegalArgumentException("Invalid core pool size");
        }
        List<TThread> toInterrupt = new ArrayList<>();
        synchronized (this) {
            this.corePoolSize = corePoolSize;
            if (workers.size() > corePoolSize) {
                for (Worker worker : workers) {
                    if (worker.idle) {
                        toInterrupt.add(worker.thread);
                    }
                }
            } else if (!workQueue.isEmpty()) {
                int toStart = Math.min(corePoolSize - workers.size(), workQueue.size());
                for (int i = 0; i < toStart; ++i) {
                    startWorker(null);
                }
            }
        }
        for (TThread thread : toInterrupt) {
            thread.interrupt();
        }
    }

    public synchronized int getCorePoolSize() {
        return corePoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("Invalid maximum pool size");
        }
        List<TThread> idleThreads = new ArrayList<>();
        synchronized (this) {
            this.maximumPoolSize = maximumPoolSize;
            while (workers.size() - idleThreads.size() > maximumPoolSize) {
                for (Worker worker : workers) {
                    if (worker.idle && !idleThreads.contains(worker.thread)) {
                        idleThreads.add(worker.thread);
                        break;
                    }
                }
                if (idleThreads.isEmpty()) {
                    break;
                }
            }
        }
        for (TThread thread : idleThreads) {
            thread.interrupt();
        }
    }

    public synchronized int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TTimeUnit unit) {
        Objects.requireNonNull(unit);
        if (time < 0 || (time == 0 && allowCoreThreadTimeOut)) {
            throw new IllegalArgumentException("Invalid keep-alive time");
        }
        List<TThread> toInterrupt = new ArrayList<>();
        synchronized (this) {
            keepAliveNanos = unit.toNanos(time);
            for (Worker worker : workers) {
                if (worker.idle && timedOutWorker()) {
                    toInterrupt.add(worker.thread);
                }
            }
        }
        for (TThread thread : toInterrupt) {
            thread.interrupt();
        }
    }

    public synchronized long getKeepAliveTime(TTimeUnit unit) {
        return Objects.requireNonNull(unit).convert(keepAliveNanos, TTimeUnit.NANOSECONDS);
    }

    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveNanos <= 0) {
            throw new IllegalArgumentException("Keep-alive time must be positive");
        }
        List<TThread> idleThreads = new ArrayList<>();
        synchronized (this) {
            allowCoreThreadTimeOut = value;
            if (value) {
                for (Worker worker : workers) {
                    if (worker.idle) {
                        idleThreads.add(worker.thread);
                    }
                }
            }
            notifyAll();
        }
        for (TThread thread : idleThreads) {
            thread.interrupt();
        }
    }

    public synchronized boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public boolean prestartCoreThread() {
        synchronized (this) {
            if (shutdown || workers.size() >= corePoolSize) {
                return false;
            }
            startWorker(null);
            return true;
        }
    }

    public int prestartAllCoreThreads() {
        int started = 0;
        while (prestartCoreThread()) {
            ++started;
        }
        return started;
    }

    public TBlockingQueue<TRunnable> getQueue() {
        return workQueue;
    }

    public boolean remove(TRunnable task) {
        boolean removed = workQueue.remove(task);
        if (removed) {
            tryTerminate();
        }
        return removed;
    }

    public void purge() {
        TIterator<TRunnable> iterator = workQueue.iterator();
        while (iterator.hasNext()) {
            TRunnable task = iterator.next();
            if (task instanceof TFuture<?> && ((TFuture<?>) task).isCancelled()) {
                iterator.remove();
            }
        }
        tryTerminate();
    }

    public synchronized int getPoolSize() {
        return workers.size();
    }

    protected void ensureWorkerForQueuedTask() {
        synchronized (this) {
            if (workers.isEmpty() && !shutdown) {
                startWorker(null);
            }
        }
    }

    public synchronized int getActiveCount() {
        return activeCount;
    }

    public synchronized int getLargestPoolSize() {
        return largestPoolSize;
    }

    public synchronized long getTaskCount() {
        return completedTaskCount + activeCount + workQueue.size();
    }

    public synchronized long getCompletedTaskCount() {
        return completedTaskCount;
    }

    protected void beforeExecute(TThread thread, TRunnable task) {
    }

    protected void afterExecute(TRunnable task, Throwable failure) {
    }

    protected void terminated() {
    }

    protected void tryTerminate() {
        synchronized (this) {
            if (shutdown && workQueue.isEmpty()) {
                List<TThread> idleThreads = new ArrayList<>();
                for (Worker worker : workers) {
                    if (worker.idle) {
                        idleThreads.add(worker.thread);
                    }
                }
                for (TThread thread : idleThreads) {
                    thread.interrupt();
                }
                notifyTerminationIfNeeded();
                notifyAll();
            }
        }
    }

    private void notifyTerminationIfNeeded() {
        if (shutdown && workQueue.isEmpty() && workers.isEmpty() && !terminationNotified) {
            terminationNotified = true;
            terminated();
        }
    }

    public static class AbortPolicy implements TRejectedExecutionHandler {
        @Override
        public void rejectedExecution(TRunnable task, TThreadPoolExecutor executor) {
            throw new TRejectedExecutionException();
        }
    }

    public static class CallerRunsPolicy implements TRejectedExecutionHandler {
        @Override
        public void rejectedExecution(TRunnable task, TThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                task.run();
            }
        }
    }

    public static class DiscardPolicy implements TRejectedExecutionHandler {
        @Override
        public void rejectedExecution(TRunnable task, TThreadPoolExecutor executor) {
        }
    }

    public static class DiscardOldestPolicy implements TRejectedExecutionHandler {
        @Override
        public void rejectedExecution(TRunnable task, TThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                executor.getQueue().poll();
                executor.execute(task);
            }
        }
    }
}
