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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockTest {
    @Test
    void readLock() {
        var rwLock = new ReentrantReadWriteLock();
        var readLock = rwLock.readLock();
        readLock.lock();
        assertEquals(1, rwLock.getReadLockCount());
        readLock.unlock();
        assertEquals(0, rwLock.getReadLockCount());
    }

    @Test
    void multipleReaders() {
        var rwLock = new ReentrantReadWriteLock();
        var readLock = rwLock.readLock();
        readLock.lock();
        readLock.lock(); // reentrant read
        assertEquals(2, rwLock.getReadLockCount());
        readLock.unlock();
        readLock.unlock();
    }

    @Test
    void writeLock() {
        var rwLock = new ReentrantReadWriteLock();
        var writeLock = rwLock.writeLock();
        writeLock.lock();
        assertTrue(rwLock.isWriteLocked());
        assertTrue(rwLock.isWriteLockedByCurrentThread());
        assertEquals(1, rwLock.getWriteHoldCount());
        writeLock.unlock();
        assertFalse(rwLock.isWriteLocked());
    }

    @Test
    void writeLockReentrant() {
        var rwLock = new ReentrantReadWriteLock();
        var writeLock = rwLock.writeLock();
        writeLock.lock();
        writeLock.lock();
        assertEquals(2, rwLock.getWriteHoldCount());
        writeLock.unlock();
        assertTrue(rwLock.isWriteLocked());
        writeLock.unlock();
        assertFalse(rwLock.isWriteLocked());
    }

    @Test
    void tryLock() {
        var rwLock = new ReentrantReadWriteLock();
        assertTrue(rwLock.writeLock().tryLock());
        assertTrue(rwLock.readLock().tryLock()); // write owner can also read
        rwLock.readLock().unlock();
        rwLock.writeLock().unlock();
    }

    @Test
    void writeUnlockWithoutLockThrows() {
        var rwLock = new ReentrantReadWriteLock();
        assertThrows(IllegalMonitorStateException.class, rwLock.writeLock()::unlock);
    }

    @Test
    void toStringFormat() {
        var rwLock = new ReentrantReadWriteLock();
        String s = rwLock.toString();
        assertNotNull(s);
    }
}
