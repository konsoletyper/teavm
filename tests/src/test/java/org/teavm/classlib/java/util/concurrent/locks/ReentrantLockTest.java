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
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {
    @Test
    void constructorDefaults() {
        var lock = new ReentrantLock();
        assertFalse(lock.isLocked());
        assertEquals(0, lock.getHoldCount());
        assertFalse(lock.isHeldByCurrentThread());
        assertFalse(lock.isFair());
    }

    @Test
    void lockAndUnlock() {
        var lock = new ReentrantLock();
        lock.lock();
        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());
        assertEquals(1, lock.getHoldCount());
        lock.unlock();
        assertFalse(lock.isLocked());
        assertEquals(0, lock.getHoldCount());
    }

    @Test
    void reentrancy() {
        var lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        lock.lock();
        assertEquals(3, lock.getHoldCount());
        assertTrue(lock.isHeldByCurrentThread());
        lock.unlock();
        assertEquals(2, lock.getHoldCount());
        assertTrue(lock.isLocked());
        lock.unlock();
        assertEquals(1, lock.getHoldCount());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void tryLock() {
        var lock = new ReentrantLock();
        assertTrue(lock.tryLock());
        assertTrue(lock.isHeldByCurrentThread());
        assertTrue(lock.tryLock()); // reentrant
        assertEquals(2, lock.getHoldCount());
        lock.unlock();
        lock.unlock();
    }

    @Test
    void unlockWithoutLockThrows() {
        var lock = new ReentrantLock();
        assertThrows(IllegalMonitorStateException.class, lock::unlock);
    }

    @Test
    void newCondition() {
        var lock = new ReentrantLock();
        var condition = lock.newCondition();
        assertNotNull(condition);
    }

    @Test
    void toStringFormat() {
        var lock = new ReentrantLock();
        String s = lock.toString();
        assertTrue(s.contains("Hold count = 0"));
    }
}
