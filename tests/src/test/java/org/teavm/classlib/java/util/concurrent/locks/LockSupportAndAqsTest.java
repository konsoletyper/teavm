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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

public class LockSupportAndAqsTest {
    @Test
    void parkReturnsImmediately() {
        // In single-threaded TeaVM, park() should return immediately
        LockSupport.park();
        LockSupport.parkNanos(1000);
        LockSupport.parkUntil(System.currentTimeMillis() + 1000);
        // If we get here, no deadlock occurred
        assertTrue(true);
    }

    @Test
    void unparkIsNoOp() {
        LockSupport.unpark(Thread.currentThread());
        // No exception means success
        assertTrue(true);
    }

    @Test
    void aqsStateManagement() {
        var sync = new TestAQS();
        assertEquals(0, sync.getStateAccessor());
        sync.setStateAccessor(5);
        assertEquals(5, sync.getStateAccessor());
        assertTrue(sync.compareAndSetStateAccessor(5, 10));
        assertEquals(10, sync.getStateAccessor());
        assertFalse(sync.compareAndSetStateAccessor(5, 15));
        assertEquals(10, sync.getStateAccessor());
    }

    @Test
    void aqsAcquireRelease() {
        var sync = new TestAQS();
        sync.acquire(1);
        assertEquals(1, sync.getStateAccessor());
        sync.release(1);
        assertEquals(0, sync.getStateAccessor());
    }

    @Test
    void aqsConditionObject() {
        var sync = new TestAQS();
        var condition = sync.new ConditionObject();
        assertNotNull(condition);
    }

    @Test
    void aqsOwnsCondition() {
        var sync = new TestAQS();
        var condition = sync.new ConditionObject();
        assertTrue(sync.owns(condition));
    }

    private static class TestAQS extends AbstractQueuedSynchronizer {
        int getStateAccessor() {
            return getState();
        }

        void setStateAccessor(int newState) {
            setState(newState);
        }

        boolean compareAndSetStateAccessor(int expect, int update) {
            return compareAndSetState(expect, update);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return compareAndSetState(0, arg);
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(getState() - arg);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }
    }
}
