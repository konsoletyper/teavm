/*
 *  Copyright 2026 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.classlib.java.lang.TThread;

public class TFutureTask<V extends TObject> implements TFuture<V>, TRunnable {
    private static final int NEW = 0;
    private static final int RUNNING = 1;
    private static final int SUCCESS = 2;
    private static final int FAILED = 3;
    private static final int CANCELLED = 4;

    private final TCallable<V> callable;
    private int state;
    private V result;
    private Throwable failure;
    private TThread runner;

    public TFutureTask(TCallable<V> callable) {
        if (callable == null) {
            throw new NullPointerException("callable");
        }
        this.callable = callable;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (state != NEW) {
                return;
            }
            state = RUNNING;
            runner = TThread.currentThread();
        }

        V value = null;
        Throwable error = null;
        try {
            value = callable.call();
        } catch (Throwable e) {
            error = e;
        }

        synchronized (this) {
            runner = null;
            if (state == CANCELLED) {
                return;
            }
            result = value;
            failure = error;
            state = error == null ? SUCCESS : FAILED;
            notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (state == SUCCESS || state == FAILED || state == CANCELLED) {
                return false;
            }
            state = CANCELLED;
            if (mayInterruptIfRunning && runner != null) {
                runner.interrupt();
            }
            notifyAll();
            return true;
        }
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
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        long timeoutNanos = unit.toNanos(timeout);
        if (timeout > 0 && timeoutNanos <= 0) {
            timeoutNanos = Long.MAX_VALUE;
        }
        long start = System.nanoTime();
        synchronized (this) {
            while (!isDone()) {
                if (timeoutNanos == 0) {
                    throw new TTimeoutException();
                }
                long remaining = timeoutNanos - (System.nanoTime() - start);
                if (remaining <= 0) {
                    throw new TTimeoutException();
                }
                long millis = remaining / 1_000_000L;
                int nanos = (int) (remaining % 1_000_000L);
                wait(millis, nanos);
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
}
