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
package org.teavm.classlib.java.util.concurrent.locks;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.classlib.java.lang.TIllegalMonitorStateException;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;
import org.teavm.classlib.java.util.TDate;
import org.teavm.classlib.java.util.concurrent.TTimeUnit;

/**
 * Reentrant read/write lock using TeaVM's cooperative monitor wait support.
 */
public class TReentrantReadWriteLock implements TReadWriteLock {
    private final Object monitor = new Object();
    private final Map<TThread, Integer> readHolds = new IdentityHashMap<>();
    private final List<LockWaiter> lockWaiters = new ArrayList<>();
    private final boolean fair;
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();
    private TThread writer;
    private int writeHoldCount;
    private int readLockCount;

    public TReentrantReadWriteLock() {
        this(false);
    }

    public TReentrantReadWriteLock(boolean fair) {
        this.fair = fair;
    }

    @Override
    public ReadLock readLock() {
        return readLock;
    }

    @Override
    public WriteLock writeLock() {
        return writeLock;
    }

    public boolean isFair() {
        return fair;
    }

    public int getReadLockCount() {
        synchronized (monitor) {
            return readLockCount;
        }
    }

    public boolean isWriteLocked() {
        synchronized (monitor) {
            return writer != null;
        }
    }

    public boolean isWriteLockedByCurrentThread() {
        synchronized (monitor) {
            return writer == TThread.currentThread();
        }
    }

    public int getWriteHoldCount() {
        synchronized (monitor) {
            return writer == TThread.currentThread() ? writeHoldCount : 0;
        }
    }

    public int getReadHoldCount() {
        synchronized (monitor) {
            Integer count = readHolds.get(TThread.currentThread());
            return count == null ? 0 : count;
        }
    }

    public boolean hasQueuedThreads() {
        synchronized (monitor) {
            return !lockWaiters.isEmpty();
        }
    }

    public boolean hasQueuedThread(TThread thread) {
        synchronized (monitor) {
            for (LockWaiter waiter : lockWaiters) {
                if (waiter.thread == thread) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getQueueLength() {
        synchronized (monitor) {
            return lockWaiters.size();
        }
    }

    private int readHoldCount(TThread thread) {
        Integer count = readHolds.get(thread);
        return count == null ? 0 : count;
    }

    private boolean canAcquire(LockMode mode, TThread thread, LockWaiter waiter, boolean honorFairness) {
        if (mode == LockMode.WRITE) {
            if (writer == thread) {
                return true;
            }
            if (writer != null || readLockCount != 0) {
                return false;
            }
            return queueAllows(mode, waiter, honorFairness);
        }

        if (writer != null && writer != thread) {
            return false;
        }
        if (writer == thread || readHoldCount(thread) != 0) {
            return true;
        }
        return queueAllows(mode, waiter, honorFairness);
    }

    private boolean queueAllows(LockMode mode, LockWaiter waiter, boolean honorFairness) {
        if (!honorFairness || !fair) {
            if (mode == LockMode.WRITE) {
                return true;
            }
            for (LockWaiter queued : lockWaiters) {
                if (queued.mode == LockMode.WRITE) {
                    return false;
                }
            }
            return true;
        }
        if (waiter == null) {
            return lockWaiters.isEmpty();
        }
        for (LockWaiter queued : lockWaiters) {
            if (queued == waiter) {
                return true;
            }
            if (mode == LockMode.WRITE || queued.mode == LockMode.WRITE) {
                return false;
            }
        }
        return false;
    }

    private void acquire(LockMode mode, TThread thread) {
        if (mode == LockMode.WRITE) {
            if (writer == thread) {
                writeHoldCount++;
            } else {
                writer = thread;
                writeHoldCount = 1;
            }
        } else {
            readHolds.put(thread, readHoldCount(thread) + 1);
            readLockCount++;
        }
    }

    private void release(LockMode mode, TThread thread) {
        synchronized (monitor) {
            if (mode == LockMode.WRITE) {
                if (writer != thread) {
                    throw new TIllegalMonitorStateException();
                }
                if (--writeHoldCount == 0) {
                    writer = null;
                    monitor.notifyAll();
                }
            } else {
                int holds = readHoldCount(thread);
                if (holds == 0) {
                    throw new TIllegalMonitorStateException();
                }
                if (holds == 1) {
                    readHolds.remove(thread);
                } else {
                    readHolds.put(thread, holds - 1);
                }
                readLockCount--;
                if (readLockCount == 0) {
                    monitor.notifyAll();
                }
            }
        }
    }

    private void lock(LockMode mode) {
        TThread thread = TThread.currentThread();
        boolean interrupted = false;
        synchronized (monitor) {
            if (canAcquire(mode, thread, null, true)) {
                acquire(mode, thread);
                return;
            }
            LockWaiter waiter = new LockWaiter(thread, mode);
            lockWaiters.add(waiter);
            while (!canAcquire(mode, thread, waiter, true)) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    interrupted = true;
                }
            }
            lockWaiters.remove(waiter);
            acquire(mode, thread);
            monitor.notifyAll();
        }
        if (interrupted) {
            thread.interrupt();
        }
    }

    private void lockInterruptibly(LockMode mode) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        TThread thread = TThread.currentThread();
        synchronized (monitor) {
            if (canAcquire(mode, thread, null, true)) {
                acquire(mode, thread);
                return;
            }
            LockWaiter waiter = new LockWaiter(thread, mode);
            lockWaiters.add(waiter);
            try {
                while (!canAcquire(mode, thread, waiter, true)) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        throw e;
                    }
                }
            } catch (InterruptedException e) {
                lockWaiters.remove(waiter);
                monitor.notifyAll();
                throw e;
            }
            lockWaiters.remove(waiter);
            acquire(mode, thread);
            monitor.notifyAll();
        }
    }

    private boolean tryLock(LockMode mode, boolean honorFairness) {
        TThread thread = TThread.currentThread();
        synchronized (monitor) {
            if (!canAcquire(mode, thread, null, honorFairness)) {
                return false;
            }
            acquire(mode, thread);
            return true;
        }
    }

    private boolean tryLock(LockMode mode, long timeout, TTimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long timeoutNanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        TThread thread = TThread.currentThread();
        synchronized (monitor) {
            if (canAcquire(mode, thread, null, true)) {
                acquire(mode, thread);
                return true;
            }
            if (timeoutNanos <= 0) {
                return false;
            }
            LockWaiter waiter = new LockWaiter(thread, mode);
            lockWaiters.add(waiter);
            try {
                while (!canAcquire(mode, thread, waiter, true)) {
                    long remaining = timeoutNanos - (System.nanoTime() - start);
                    if (remaining <= 0) {
                        lockWaiters.remove(waiter);
                        monitor.notifyAll();
                        return false;
                    }
                    try {
                        monitor.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        throw e;
                    }
                }
            } catch (InterruptedException e) {
                lockWaiters.remove(waiter);
                monitor.notifyAll();
                throw e;
            }
            lockWaiters.remove(waiter);
            acquire(mode, thread);
            monitor.notifyAll();
            return true;
        }
    }

    private boolean awaitCondition(List<ConditionWaiter> waiters, boolean timed, long timeoutNanos,
            boolean uninterruptible) throws InterruptedException {
        if (!uninterruptible && Thread.interrupted()) {
            throw new InterruptedException();
        }
        TThread thread = TThread.currentThread();
        int savedHolds;
        ConditionWaiter waiter = new ConditionWaiter();
        synchronized (monitor) {
            if (writer != thread) {
                throw new TIllegalMonitorStateException();
            }
            savedHolds = writeHoldCount;
            waiters.add(waiter);
            writer = null;
            writeHoldCount = 0;
            monitor.notifyAll();
        }

        long start = System.nanoTime();
        boolean interrupted = false;
        boolean signalled = false;
        while (!signalled) {
            long elapsed = System.nanoTime() - start;
            long remaining = timed && timeoutNanos > elapsed ? timeoutNanos - elapsed : 0;
            synchronized (waiter.monitor) {
                if (waiter.signalled) {
                    signalled = true;
                    break;
                }
                if (timed && remaining <= 0) {
                    break;
                }
                try {
                    if (timed) {
                        waiter.monitor.wait(remaining / 1_000_000L,
                                (int) (remaining % 1_000_000L));
                    } else {
                        waiter.monitor.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    if (!uninterruptible) {
                        if (waiter.signalled) {
                            interrupted = true;
                            signalled = true;
                            break;
                        }
                        removeConditionWaiter(waiters, waiter);
                        reacquireWriteLock(savedHolds);
                        Thread.interrupted();
                        throw e;
                    }
                    interrupted = true;
                }
            }
        }
        removeConditionWaiter(waiters, waiter);
        reacquireWriteLock(savedHolds);
        if (interrupted) {
            thread.interrupt();
        }
        return signalled;
    }

    private void removeConditionWaiter(List<ConditionWaiter> waiters, ConditionWaiter waiter) {
        synchronized (monitor) {
            if (waiter.inQueue) {
                waiters.remove(waiter);
                waiter.inQueue = false;
            }
        }
    }

    private void reacquireWriteLock(int holdCount) {
        for (int i = 0; i < holdCount; ++i) {
            writeLock.lock();
        }
    }

    private void signalCondition(List<ConditionWaiter> waiters, boolean all) {
        List<ConditionWaiter> resumed = new ArrayList<>();
        synchronized (monitor) {
            if (writer != TThread.currentThread()) {
                throw new TIllegalMonitorStateException();
            }
            do {
                if (waiters.isEmpty()) {
                    break;
                }
                ConditionWaiter waiter = waiters.remove(0);
                waiter.inQueue = false;
                waiter.signalled = true;
                resumed.add(waiter);
            } while (all);
        }
        for (ConditionWaiter waiter : resumed) {
            synchronized (waiter.monitor) {
                waiter.monitor.notifyAll();
            }
        }
    }

    private enum LockMode {
        READ,
        WRITE
    }

    private static final class LockWaiter {
        final TThread thread;
        final LockMode mode;

        LockWaiter(TThread thread, LockMode mode) {
            this.thread = thread;
            this.mode = mode;
        }
    }

    private static final class ConditionWaiter {
        final Object monitor = new Object();
        volatile boolean signalled;
        boolean inQueue = true;
    }

    public final class ReadLock implements TLock {
        @Override
        public void lock() {
            TReentrantReadWriteLock.this.lock(LockMode.READ);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            TReentrantReadWriteLock.this.lockInterruptibly(LockMode.READ);
        }

        @Override
        public boolean tryLock() {
            return TReentrantReadWriteLock.this.tryLock(LockMode.READ, false);
        }

        @Override
        public boolean tryLock(long timeout, TTimeUnit unit) throws InterruptedException {
            return TReentrantReadWriteLock.this.tryLock(LockMode.READ, timeout, unit);
        }

        @Override
        public void unlock() {
            TReentrantReadWriteLock.this.release(LockMode.READ, TThread.currentThread());
        }

        @Override
        public TCondition newCondition() {
            throw new TUnsupportedOperationException();
        }
    }

    public final class WriteLock implements TLock {
        private final List<ConditionWaiter> conditionWaiters = new ArrayList<>();
        private final TCondition condition = new WriteCondition();

        @Override
        public void lock() {
            TReentrantReadWriteLock.this.lock(LockMode.WRITE);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            TReentrantReadWriteLock.this.lockInterruptibly(LockMode.WRITE);
        }

        @Override
        public boolean tryLock() {
            return TReentrantReadWriteLock.this.tryLock(LockMode.WRITE, false);
        }

        @Override
        public boolean tryLock(long timeout, TTimeUnit unit) throws InterruptedException {
            return TReentrantReadWriteLock.this.tryLock(LockMode.WRITE, timeout, unit);
        }

        @Override
        public void unlock() {
            TReentrantReadWriteLock.this.release(LockMode.WRITE, TThread.currentThread());
        }

        @Override
        public TCondition newCondition() {
            return condition;
        }

        public boolean isHeldByCurrentThread() {
            return isWriteLockedByCurrentThread();
        }

        public int getHoldCount() {
            return getWriteHoldCount();
        }

        private final class WriteCondition implements TCondition {
            @Override
            public void await() throws InterruptedException {
                awaitCondition(conditionWaiters, false, 0, false);
            }

            @Override
            public void awaitUninterruptibly() {
                try {
                    awaitCondition(conditionWaiters, false, 0, true);
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public long awaitNanos(long nanosTimeout) throws InterruptedException {
                long start = System.nanoTime();
                awaitCondition(conditionWaiters, true, nanosTimeout, false);
                long elapsed = System.nanoTime() - start;
                return nanosTimeout <= 0 ? nanosTimeout : nanosTimeout - elapsed;
            }

            @Override
            public boolean await(long time, TTimeUnit unit) throws InterruptedException {
                long nanos = unit.toNanos(time);
                return awaitCondition(conditionWaiters, true, nanos, false);
            }

            @Override
            public boolean awaitUntil(TDate deadline) throws InterruptedException {
                long deadlineMillis = deadline.getTime();
                long nowMillis = System.currentTimeMillis();
                long timeoutMillis = deadlineMillis <= nowMillis ? 0 : deadlineMillis - nowMillis;
                long nanos = TTimeUnit.MILLISECONDS.toNanos(timeoutMillis);
                return awaitCondition(conditionWaiters, true, nanos, false);
            }

            @Override
            public void signal() {
                signalCondition(conditionWaiters, false);
            }

            @Override
            public void signalAll() {
                signalCondition(conditionWaiters, true);
            }
        }
    }
}
