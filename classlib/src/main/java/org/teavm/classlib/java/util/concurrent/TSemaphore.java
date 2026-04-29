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

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TInterruptedException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TSemaphore {
    private int permits;
    private final boolean fair;
    private java.util.Queue<WaitNode> waiters;

    public TSemaphore(int permits) {
        this(permits, false);
    }

    public TSemaphore(int permits, boolean fair) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }
        this.permits = permits;
        this.fair = fair;
    }

    public void acquire() throws TInterruptedException {
        acquireAsync(0);
    }

    public void acquireUninterruptibly() {
        try {
            acquire();
        } catch (TInterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            return true;
        }
        return false;
    }

    public boolean tryAcquire(long timeout, TTimeUnit unit) throws TInterruptedException {
        if (tryAcquire()) {
            return true;
        }
        return acquireAsync(unit.toMillis(timeout));
    }

    @Async
    private native boolean acquireAsync(long timeoutMillis) throws TInterruptedException;

    private void acquireAsync(long timeoutMillis, AsyncCallback<Boolean> callback) {
        if (permits > 0 && (!fair || waiters == null || waiters.isEmpty())) {
            permits--;
            callback.complete(true);
            return;
        }
        if (Thread.interrupted()) {
            callback.error(new TInterruptedException());
            return;
        }

        WaitNode node = new WaitNode(callback);
        if (waiters == null) {
            waiters = new java.util.ArrayDeque<>();
        }

        if (fair) {
            waiters.add(node);
        } else {
            // Non-fair: still add to queue but may be served out of order when permits available
            waiters.add(node);
        }

        if (timeoutMillis > 0) {
            long timeLimit = System.currentTimeMillis() + timeoutMillis;
            if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                node.timerId = EventQueue.offer(node, timeLimit);
            } else {
                int timeout = Math.max(0, (int) Math.min(timeoutMillis, Integer.MAX_VALUE));
                node.timerId = Platform.schedule(node, timeout);
            }
        }
    }

    public void release() {
        permits++;
        notifyWaiters();
    }

    public void acquire(int permits) throws TInterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }
        for (int i = 0; i < permits; i++) {
            acquire();
        }
    }

    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }
        this.permits += permits;
        for (int i = 0; i < permits; i++) {
            notifyWaiters();
        }
    }

    public int availablePermits() {
        return permits;
    }

    public int drainPermits() {
        int result = permits;
        permits = 0;
        return result;
    }

    public boolean isFair() {
        return fair;
    }

    @Override
    public String toString() {
        return super.toString() + "[Permits = " + permits + "]";
    }

    private void notifyWaiters() {
        if (waiters == null) {
            return;
        }
        while (permits > 0 && !waiters.isEmpty()) {
            WaitNode node = waiters.peek();
            if (node != null && !node.done) {
                permits--;
                waiters.poll();
                node.complete(true);
            } else {
                waiters.poll();
            }
        }
    }

    class WaitNode implements PlatformRunnable, EventQueue.Event {
        AsyncCallback<Boolean> callback;
        boolean done;
        int timerId = -1;

        WaitNode(AsyncCallback<Boolean> callback) {
            this.callback = callback;
        }

        void complete(boolean result) {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            callback.complete(result);
        }

        @Override
        public void run() {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            callback.complete(false);
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
