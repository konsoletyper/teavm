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

public class TCountDownLatch {
    private int count;
    private java.util.Queue<WaitNode> waiters;

    public TCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    public void await() throws TInterruptedException {
        if (count == 0) {
            return;
        }
        awaitAsync(0);
    }

    public boolean await(long timeout, TTimeUnit unit) throws TInterruptedException {
        if (count == 0) {
            return true;
        }
        if (timeout <= 0) {
            return count == 0;
        }
        return awaitAsync(unit.toMillis(timeout));
    }

    @Async
    private native void awaitAsync(long timeoutMillis) throws TInterruptedException;

    private void awaitAsync(long timeoutMillis, AsyncCallback<Void> callback) {
        if (count == 0) {
            callback.complete(null);
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
        waiters.add(node);

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

    public void countDown() {
        if (count > 0) {
            count--;
            if (count == 0) {
                notifyWaiters();
            }
        }
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return super.toString() + "[Count = " + count + "]";
    }

    private void notifyWaiters() {
        if (waiters == null) {
            return;
        }
        WaitNode node;
        while ((node = waiters.poll()) != null) {
            node.complete();
        }
        waiters = null;
    }

    class WaitNode implements PlatformRunnable, EventQueue.Event {
        AsyncCallback<Void> callback;
        boolean done;
        int timerId = -1;

        WaitNode(AsyncCallback<Void> callback) {
            this.callback = callback;
        }

        void complete() {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            callback.complete(null);
        }

        @Override
        public void run() {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            if (count == 0) {
                callback.complete(null);
            } else {
                callback.complete(null);
            }
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
