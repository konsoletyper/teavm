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
import org.teavm.classlib.java.lang.TRuntimeException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TCyclicBarrier {
    private final int parties;
    private final Runnable barrierAction;
    private int count;
    private int generation;
    private boolean broken;
    private java.util.Queue<WaitNode> waiters;

    public TCyclicBarrier(int parties) {
        this(parties, null);
    }

    public TCyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties <= 0");
        }
        this.parties = parties;
        this.barrierAction = barrierAction;
        this.count = parties;
    }

    public int await() throws TInterruptedException, BrokenBarrierException {
        return await(0);
    }

    public int await(long timeout, TTimeUnit unit) throws TInterruptedException, BrokenBarrierException,
            TTimeoutException {
        return await(unit != null ? unit.toMillis(timeout) : 0);
    }

    @Async
    private native int await(long timeoutMillis) throws TInterruptedException, BrokenBarrierException,
            TTimeoutException;

    private int await(long timeoutMillis, AsyncCallback<Integer> callback) {
        if (broken) {
            callback.error(new BrokenBarrierException());
            return 0;
        }
        if (Thread.interrupted()) {
            breakBarrier();
            callback.error(new TInterruptedException());
            return 0;
        }

        int index = --count;
        if (index == 0) {
            // Last thread to arrive: trip the barrier
            int gen = generation;
            if (barrierAction != null) {
                try {
                    barrierAction.run();
                } catch (Throwable e) {
                    breakBarrier();
                    callback.error(new BrokenBarrierException());
                    return 0;
                }
            }
            nextGeneration();
            notifyWaiters(gen);
            callback.complete(index);
            return 0;
        }

        // Wait for other threads
        int currentGen = generation;
        WaitNode node = new WaitNode(callback, currentGen);
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

        return 0;
    }

    public int getParties() {
        return parties;
    }

    public boolean isBroken() {
        return broken;
    }

    public void reset() {
        breakBarrier();
        nextGeneration();
    }

    public int getNumberWaiting() {
        return parties - count;
    }

    private void breakBarrier() {
        broken = true;
        count = parties;
        int gen = generation;
        notifyWaiters(gen);
    }

    private void nextGeneration() {
        count = parties;
        broken = false;
        generation++;
    }

    private void notifyWaiters(int gen) {
        if (waiters == null) {
            return;
        }
        WaitNode node;
        while ((node = waiters.poll()) != null) {
            if (node.generation == gen) {
                node.complete();
            }
        }
    }

    class WaitNode implements PlatformRunnable, EventQueue.Event {
        AsyncCallback<Integer> callback;
        boolean done;
        int timerId = -1;
        int generation;

        WaitNode(AsyncCallback<Integer> callback, int generation) {
            this.callback = callback;
            this.generation = generation;
        }

        void complete() {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            callback.complete(parties - count);
        }

        @Override
        public void run() {
            if (done) {
                return;
            }
            done = true;
            cancelTimer();
            if (broken) {
                callback.error(new BrokenBarrierException());
            } else {
                callback.error(new TTimeoutException());
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

    public static class BrokenBarrierException extends TRuntimeException {
        public BrokenBarrierException() {
            super();
        }

        public BrokenBarrierException(String message) {
            super(message);
        }
    }
}
