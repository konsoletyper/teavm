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

import java.util.concurrent.Callable;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TInterruptedException;
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TFutureTask<V> implements TRunnableFuture<V> {
    private static final int NEW = 0;
    private static final int COMPLETING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED = 6;

    private Callable<V> callable;
    private Object outcome;
    private volatile int state = NEW;
    private Thread runner;
    private java.util.Queue<WaitNode> waiters;

    public TFutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
    }

    public TFutureTask(Runnable runnable, V result) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        this.callable = new TAbstractExecutorService.RunnableAdapter<>(runnable, result);
    }

    @Override
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && unsafeCompareAndSwapState(NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) {
            return false;
        }
        try {
            if (mayInterruptIfRunning) {
                if (runner != null) {
                    runner.interrupt();
                }
                unsafeCompareAndSwapState(INTERRUPTING, INTERRUPTED);
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    @Override
    public V get() throws TInterruptedException, TExecutionException {
        int s = state;
        if (s <= COMPLETING) {
            s = awaitDone(false, 0L);
        }
        return report(s);
    }

    @Override
    public V get(long timeout, TTimeUnit unit) throws TInterruptedException, TExecutionException, TTimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (Thread.interrupted()) {
            throw new TInterruptedException();
        }
        int s = state;
        if (s <= COMPLETING) {
            long nanos = unit.toNanos(timeout);
            s = awaitDone(true, nanos);
        }
        if (s <= COMPLETING) {
            throw new TTimeoutException();
        }
        return report(s);
    }

    @Override
    public void run() {
        if (state != NEW || !unsafeCompareAndSwapState(NEW, COMPLETING)) {
            return;
        }
        try {
            runner = Thread.currentThread();
            if (state == COMPLETING) {
                V result = callable.call();
                unsafeCompareAndSwapState(COMPLETING, NORMAL);
                outcome = result;
            }
        } catch (Throwable ex) {
            if (state == COMPLETING) {
                unsafeCompareAndSwapState(COMPLETING, EXCEPTIONAL);
                outcome = ex;
            }
        } finally {
            runner = null;
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
            finishCompletion();
        }
    }

    protected void done() {
    }

    protected boolean runAndReset() {
        if (state != NEW) {
            return false;
        }
        try {
            runner = Thread.currentThread();
            if (state == NEW) {
                callable.call();
            }
        } catch (Throwable ex) {
            unsafeCompareAndSwapState(NEW, EXCEPTIONAL);
            outcome = ex;
            return false;
        } finally {
            runner = null;
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
        return true;
    }

    protected void set(V v) {
        if (unsafeCompareAndSwapState(NEW, COMPLETING)) {
            outcome = v;
            unsafeCompareAndSwapState(COMPLETING, NORMAL);
            finishCompletion();
        }
    }

    protected void setException(Throwable t) {
        if (unsafeCompareAndSwapState(NEW, COMPLETING)) {
            outcome = t;
            unsafeCompareAndSwapState(COMPLETING, EXCEPTIONAL);
            finishCompletion();
        }
    }

    @SuppressWarnings("unchecked")
    private V report(int s) throws TExecutionException {
        Object x = outcome;
        if (s == NORMAL) {
            return (V) x;
        }
        if (s >= CANCELLED) {
            throw new TCancellationException();
        }
        throw new TExecutionException((Throwable) x);
    }

    private void finishCompletion() {
        if (waiters != null) {
            WaitNode w;
            while ((w = waiters.poll()) != null) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    EventQueue.offer(w);
                } else {
                    Platform.postpone(w);
                }
            }
        }
        done();
        callable = null;
    }

    private void handlePossibleCancellationInterrupt(int s) {
        if (s == INTERRUPTING) {
            while (state == INTERRUPTING) {
                Thread.yield();
            }
        }
    }

    @Async
    private native int awaitDone(boolean timed, long nanos) throws TInterruptedException;

    private void awaitDone(boolean timed, long nanos, AsyncCallback<Integer> callback) {
        if (Thread.interrupted()) {
            callback.error(new TInterruptedException());
            return;
        }
        int s = state;
        if (s > COMPLETING) {
            callback.complete(s);
            return;
        }
        if (s == COMPLETING) {
            Thread.yield();
            callback.complete(state);
            return;
        }

        WaitNode node = new WaitNode(callback);
        if (waiters == null) {
            waiters = new java.util.ArrayDeque<>();
        }
        waiters.add(node);

        if (timed && nanos > 0) {
            long millis = nanos / 1_000_000;
            if (millis > 0) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    node.timerId = EventQueue.offer(node, System.currentTimeMillis() + millis);
                } else {
                    node.timerId = Platform.schedule(node, (int) Math.min(millis, Integer.MAX_VALUE));
                }
            } else {
                node.timerId = -2;
            }
        }
    }

    private boolean unsafeCompareAndSwapState(int expected, int update) {
        if (state == expected) {
            state = update;
            return true;
        }
        return false;
    }

    class WaitNode implements PlatformRunnable, EventQueue.Event {
        AsyncCallback<Integer> callback;
        boolean completed;
        int timerId = -1;

        WaitNode(AsyncCallback<Integer> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            if (completed) {
                return;
            }
            completed = true;
            cancelTimer();
            callback.complete(state);
        }

        void cancelTimer() {
            if (timerId >= 0) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    EventQueue.kill(timerId);
                } else {
                    Platform.killSchedule(timerId);
                }
                timerId = -1;
            }
        }
    }
}
