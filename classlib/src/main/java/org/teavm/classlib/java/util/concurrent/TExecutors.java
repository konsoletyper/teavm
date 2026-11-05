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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.classlib.java.lang.TThread;

public final class TExecutors {
    private TExecutors() {
    }

    public static TExecutorService newCachedThreadPool() {
        return newCachedThreadPool(task -> new TThread(task));
    }

    public static TExecutorService newCachedThreadPool(TThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        return new CachedThreadPool(threadFactory);
    }

    public static TScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new TScheduledThreadPoolExecutor(corePoolSize);
    }

    public static TScheduledExecutorService newScheduledThreadPool(int corePoolSize, TThreadFactory threadFactory) {
        return new TScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    public static TScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new TScheduledThreadPoolExecutor(1);
    }

    public static TScheduledExecutorService newSingleThreadScheduledExecutor(TThreadFactory threadFactory) {
        return new TScheduledThreadPoolExecutor(1, threadFactory);
    }

    private static final class CachedThreadPool implements TExecutorService {
        private static final long KEEP_ALIVE_MILLIS = 60_000;
        private final TThreadFactory threadFactory;
        private final ArrayDeque<TRunnable> tasks = new ArrayDeque<>();
        private final List<TThread> workers = new ArrayList<>();
        private int idleWorkers;
        private boolean shutdown;

        CachedThreadPool(TThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        @Override
        public void execute(TRunnable command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            synchronized (this) {
                if (shutdown) {
                    throw new TRejectedExecutionException();
                }
                tasks.addLast(command);
                if (tasks.size() > idleWorkers) {
                    try {
                        startWorker();
                    } catch (RuntimeException | Error e) {
                        tasks.removeLast();
                        throw e;
                    }
                }
                notifyAll();
            }
        }

        private void startWorker() {
            TThread worker = threadFactory.newThread(new Worker());
            if (worker == null) {
                throw new TRejectedExecutionException("threadFactory returned null");
            }
            workers.add(worker);
            try {
                worker.start();
            } catch (RuntimeException | Error e) {
                workers.remove(worker);
                throw e;
            }
        }

        @Override
        public synchronized void shutdown() {
            shutdown = true;
            notifyAll();
        }

        @Override
        public List<TRunnable> shutdownNow() {
            List<TRunnable> pending = new ArrayList<>();
            List<TThread> toInterrupt;
            synchronized (this) {
                shutdown = true;
                while (!tasks.isEmpty()) {
                    pending.add(tasks.removeFirst());
                }
                toInterrupt = new ArrayList<>(workers);
                notifyAll();
            }
            for (TThread worker : toInterrupt) {
                worker.interrupt();
            }
            return pending;
        }

        @Override
        public synchronized boolean isShutdown() {
            return shutdown;
        }

        @Override
        public synchronized boolean isTerminated() {
            return shutdown && workers.isEmpty() && tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, TTimeUnit unit) throws InterruptedException {
            if (unit == null) {
                throw new NullPointerException("unit");
            }
            long timeoutNanos = unit.toNanos(timeout);
            long start = System.nanoTime();
            synchronized (this) {
                while (!isTerminated()) {
                    if (timeoutNanos <= 0) {
                        return false;
                    }
                    long remaining = timeoutNanos - (System.nanoTime() - start);
                    if (remaining <= 0) {
                        return false;
                    }
                    wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                }
                return true;
            }
        }

        @Override
        public TFuture<TObject> submit(TRunnable task) {
            return submit(task, null);
        }

        @Override
        public <V extends TObject> TFuture<V> submit(TRunnable task, V result) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            TFutureTask<V> future = new TFutureTask<>(() -> {
                task.run();
                return result;
            });
            execute(future);
            return future;
        }

        @Override
        public <V extends TObject> TFuture<V> submit(TCallable<V> task) {
            TFutureTask<V> future = new TFutureTask<>(task);
            execute(future);
            return future;
        }

        private void workerExited(TThread worker) {
            synchronized (this) {
                workers.remove(worker);
                if (tasks.size() > idleWorkers) {
                    startWorker();
                }
                notifyAll();
            }
        }

        private final class Worker implements TRunnable {
            @Override
            public void run() {
                TThread current = TThread.currentThread();
                try {
                    runTasks();
                } finally {
                    workerExited(current);
                }
            }

            private void runTasks() {
                while (true) {
                    TRunnable task;
                    synchronized (CachedThreadPool.this) {
                        long deadline = System.currentTimeMillis() + KEEP_ALIVE_MILLIS;
                        while (tasks.isEmpty() && !shutdown) {
                            long remaining = deadline - System.currentTimeMillis();
                            if (remaining <= 0) {
                                return;
                            }
                            ++idleWorkers;
                            try {
                                CachedThreadPool.this.wait(remaining);
                            } catch (InterruptedException e) {
                                if (shutdown) {
                                    return;
                                }
                            } finally {
                                --idleWorkers;
                            }
                        }
                        if (tasks.isEmpty() && shutdown) {
                            return;
                        }
                        task = tasks.removeFirst();
                    }
                    task.run();
                }
            }
        }
    }
}
