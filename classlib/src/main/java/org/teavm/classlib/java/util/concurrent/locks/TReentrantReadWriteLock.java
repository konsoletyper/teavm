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

public class TReentrantReadWriteLock implements TReadWriteLock {
    private int readCount;
    private int writeHoldCount;
    private Thread writeOwner;
    private final ReadLock readerLock = new ReadLock(this);
    private final WriteLock writerLock = new WriteLock(this);

    public TReentrantReadWriteLock() {
    }

    public TReentrantReadWriteLock(boolean fair) {
        // Fairness is not meaningful in single-threaded environment
    }

    @Override
    public TLock readLock() {
        return readerLock;
    }

    @Override
    public TLock writeLock() {
        return writerLock;
    }

    public boolean isWriteLocked() {
        return writeOwner != null;
    }

    public boolean isWriteLockedByCurrentThread() {
        return writeOwner == TThread.currentThread();
    }

    public int getWriteHoldCount() {
        return isWriteLockedByCurrentThread() ? writeHoldCount : 0;
    }

    public int getReadHoldCount() {
        return readCount;
    }

    public int getReadLockCount() {
        return readCount;
    }

    public final boolean isFair() {
        return false;
    }

    public Thread getOwner() {
        return writeOwner;
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

    public static class ReadLock implements TLock {
        private final TReentrantReadWriteLock sync;

        ReadLock(TReentrantReadWriteLock sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            if (sync.writeOwner != null && sync.writeOwner != TThread.currentThread()) {
                throw new IllegalMonitorStateException("Write lock held by another thread");
            }
            sync.readCount++;
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock();
        }

        @Override
        public boolean tryLock() {
            if (sync.writeOwner != null && sync.writeOwner != TThread.currentThread()) {
                return false;
            }
            sync.readCount++;
            return true;
        }

        @Override
        public boolean tryLock(long time, TTimeUnit unit) throws InterruptedException {
            return tryLock();
        }

        @Override
        public void unlock() {
            if (sync.readCount <= 0) {
                throw new IllegalMonitorStateException();
            }
            sync.readCount--;
        }

        @Override
        public TCondition newCondition() {
            return new TConditionImpl();
        }

        @Override
        public String toString() {
            return "ReadLock: " + sync.readCount + " readers";
        }
    }

    public static class WriteLock implements TLock {
        private final TReentrantReadWriteLock sync;

        WriteLock(TReentrantReadWriteLock sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            Thread current = TThread.currentThread();
            if (sync.writeOwner == current) {
                sync.writeHoldCount++;
                return;
            }
            if (sync.readCount > 0 || sync.writeOwner != null) {
                throw new IllegalMonitorStateException("Cannot acquire write lock");
            }
            sync.writeOwner = current;
            sync.writeHoldCount = 1;
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock();
        }

        @Override
        public boolean tryLock() {
            Thread current = TThread.currentThread();
            if (sync.writeOwner == current) {
                sync.writeHoldCount++;
                return true;
            }
            if (sync.readCount > 0 || sync.writeOwner != null) {
                return false;
            }
            sync.writeOwner = current;
            sync.writeHoldCount = 1;
            return true;
        }

        @Override
        public boolean tryLock(long time, TTimeUnit unit) throws InterruptedException {
            return tryLock();
        }

        @Override
        public void unlock() {
            Thread current = TThread.currentThread();
            if (sync.writeOwner != current) {
                throw new IllegalMonitorStateException();
            }
            sync.writeHoldCount--;
            if (sync.writeHoldCount == 0) {
                sync.writeOwner = null;
            }
        }

        @Override
        public TCondition newCondition() {
            return new TConditionImpl();
        }

        @Override
        public String toString() {
            return "WriteLock: holdCount=" + sync.writeHoldCount + ", owner=" + sync.writeOwner;
        }
    }

    @Override
    public String toString() {
        return "ReentrantReadWriteLock[writeLock=" + writerLock + ", readLock=" + readerLock + "]";
    }
}
