/*
 *  Copyright 2025 konsoletyper and other contributors.
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
package org.teavm.classlib.java.util.concurrent.locks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.util.concurrent.TTimeUnit;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;
import org.teavm.classlib.PlatformDetector;

public abstract class TAbstractQueuedSynchronizer extends TAbstractOwnableSynchronizer {
    private volatile int state;

    protected TAbstractQueuedSynchronizer() {
    }

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        if (state == expect) {
            state = update;
            return true;
        }
        return false;
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(int arg) {
        if (!tryAcquire(arg)) {
            // In single-threaded environment, acquisition always succeeds after try
            // since there are no competing threads
        }
    }

    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (!tryAcquire(arg)) {
            // In single-threaded environment, no blocking needed
        }
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        return tryAcquire(arg);
    }

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            return true;
        }
        return false;
    }

    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) {
            // In single-threaded environment, no blocking needed
        }
    }

    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (tryAcquireShared(arg) < 0) {
            // In single-threaded environment, no blocking needed
        }
    }

    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        return tryAcquireShared(arg) >= 0;
    }

    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final boolean hasContended() {
        return false;
    }

    public final Thread getFirstQueuedThread() {
        return null;
    }

    public final boolean isQueued(Thread thread) {
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    public final Collection<Thread> getQueuedThreads() {
        return new ArrayList<>();
    }

    public final boolean owns(TAbstractQueuedSynchronizer.ConditionObject condition) {
        return condition.isAssociatedWith(this);
    }

    public final boolean hasWaiters(TAbstractQueuedSynchronizer.ConditionObject condition) {
        return condition.hasWaiters();
    }

    public final int getWaitQueueLength(TAbstractQueuedSynchronizer.ConditionObject condition) {
        return condition.getWaitQueueLength();
    }

    public final Collection<Thread> getWaitingThreads(TAbstractQueuedSynchronizer.ConditionObject condition) {
        return condition.getWaitingThreads();
    }

    @Override
    public String toString() {
        return super.toString() + "[State = " + state + "]";
    }

    public class ConditionObject implements TCondition {
        private int waitCount;

        public ConditionObject() {
        }

        @Override
        @Async
        public native void await() throws InterruptedException;

        private void await(AsyncCallback<Void> callback) {
            waitCount++;
            Thread current = TThread.currentThread();
            int savedState = TAbstractQueuedSynchronizer.this.getState();
            TAbstractQueuedSynchronizer.this.setState(0);
            if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                EventQueue.offer(() -> {
                    waitCount--;
                    TAbstractQueuedSynchronizer.this.setState(savedState);
                    callback.complete(null);
                });
            } else {
                Platform.postpone(() -> {
                    waitCount--;
                    TAbstractQueuedSynchronizer.this.setState(savedState);
                    callback.complete(null);
                });
            }
        }

        @Override
        public void awaitUninterruptibly() {
            // In single-threaded environment, just return
        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            return nanosTimeout;
        }

        @Override
        public boolean await(long time, TTimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public boolean awaitUntil(Date deadline) throws InterruptedException {
            return true;
        }

        @Override
        public void signal() {
            // In single-threaded environment, no actual signaling needed
        }

        @Override
        public void signalAll() {
            // In single-threaded environment, no actual signaling needed
        }

        boolean isAssociatedWith(TAbstractQueuedSynchronizer sync) {
            return TAbstractQueuedSynchronizer.this == sync;
        }

        boolean hasWaiters() {
            return waitCount > 0;
        }

        int getWaitQueueLength() {
            return waitCount;
        }

        Collection<Thread> getWaitingThreads() {
            return new ArrayList<>();
        }
    }
}
