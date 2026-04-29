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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.teavm.classlib.java.lang.TRunnable;

public abstract class TAbstractExecutorService implements TExecutorService {
    @Override
    public TFuture<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> TFuture<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return submit(new RunnableAdapter<>(task, result));
    }

    @Override
    public <T> List<TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws TInterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        List<TFuture<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> task : tasks) {
                TFuture<T> future = submit(task);
                futures.add(future);
            }
            for (TFuture<T> future : futures) {
                if (!future.isDone()) {
                    try {
                        future.get();
                    } catch (TExecutionException | TCancellationException e) {
                        // Ignore exceptions from individual tasks
                    }
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (TFuture<T> future : futures) {
                    future.cancel(true);
                }
            }
        }
    }

    @Override
    public <T> List<TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TTimeUnit unit)
            throws TInterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        List<TFuture<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> task : tasks) {
                TFuture<T> future = submit(task);
                futures.add(future);
            }
            long lastTime = System.nanoTime();
            for (TFuture<T> future : futures) {
                if (!future.isDone()) {
                    try {
                        future.get(nanos, TTimeUnit.NANOSECONDS);
                    } catch (TExecutionException | TCancellationException e) {
                        // Ignore
                    } catch (TTimeoutException e) {
                        // Timed out
                    }
                }
                long now = System.nanoTime();
                nanos -= now - lastTime;
                lastTime = now;
                if (nanos <= 0) {
                    break;
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (TFuture<T> future : futures) {
                    future.cancel(true);
                }
            }
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws TInterruptedException, TExecutionException {
        try {
            return invokeAny(tasks, Long.MAX_VALUE, TTimeUnit.NANOSECONDS);
        } catch (TTimeoutException e) {
            throw new TExecutionException(e);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TTimeUnit unit)
            throws TInterruptedException, TExecutionException, TTimeoutException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException();
        }
        long nanos = unit.toNanos(timeout);
        List<Throwable> exceptions = new ArrayList<>();
        Iterator<? extends Callable<T>> it = tasks.iterator();
        long lastTime = System.nanoTime();
        while (it.hasNext()) {
            try {
                return it.next().call();
            } catch (Throwable e) {
                exceptions.add(e);
            }
            long now = System.nanoTime();
            nanos -= now - lastTime;
            lastTime = now;
            if (nanos <= 0) {
                break;
            }
        }
        TTimeoutException timeoutEx = new TTimeoutException();
        for (Throwable e : exceptions) {
            timeoutEx.addSuppressed(e);
        }
        throw timeoutEx;
    }

    static class RunnableAdapter<T> implements Callable<T> {
        private final Runnable task;
        private final T result;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        @Override
        public T call() {
            task.run();
            return result;
        }
    }
}
