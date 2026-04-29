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

import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.util.concurrent.TTimeUnit;

public class TReentrantLock implements TLock {
    int holdCount;
    Thread owner;

    public TReentrantLock() {
    }

    public TReentrantLock(boolean fair) {
        // Fairness is not meaningful in single-threaded environment
    }

    @Override
    public void lock() {
        Thread current = TThread.currentThread();
        if (owner == current) {
            holdCount++;
        } else {
            owner = current;
            holdCount = 1;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock();
    }

    @Override
    public boolean tryLock() {
        Thread current = TThread.currentThread();
        if (owner == current) {
            holdCount++;
            return true;
        }
        if (owner == null) {
            owner = current;
            holdCount = 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TTimeUnit unit) throws InterruptedException {
        return tryLock();
    }

    @Override
    public void unlock() {
        Thread current = TThread.currentThread();
        if (owner != current) {
            throw new IllegalMonitorStateException();
        }
        holdCount--;
        if (holdCount == 0) {
            owner = null;
        }
    }

    @Override
    public TCondition newCondition() {
        return new TConditionImpl();
    }

    public boolean isHeldByCurrentThread() {
        return owner == TThread.currentThread();
    }

    public int getHoldCount() {
        return isHeldByCurrentThread() ? holdCount : 0;
    }

    public boolean isLocked() {
        return owner != null;
    }

    public final boolean isFair() {
        return false;
    }

    public Thread getOwner() {
        return owner;
    }

    public boolean hasQueuedThreads() {
        return false;
    }

    public boolean hasQueuedThread(Thread thread) {
        return false;
    }

    public int getQueueLength() {
        return 0;
    }

    public boolean hasWaiters(TCondition condition) {
        if (condition instanceof TConditionImpl) {
            return ((TConditionImpl) condition).hasWaiters();
        }
        return false;
    }

    public int getWaitQueueLength(TCondition condition) {
        if (condition instanceof TConditionImpl) {
            return ((TConditionImpl) condition).getWaitQueueLength();
        }
        return 0;
    }

    @Override
    public String toString() {
        return super.toString() + "[Hold count = " + holdCount + ", Owner = " + owner + "]";
    }
}
