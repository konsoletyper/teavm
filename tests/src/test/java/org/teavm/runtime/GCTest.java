/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@OnlyPlatform(TestPlatform.C)
@EachTestCompiledSeparately
public class GCTest {
    private static final int GARBAGE_COUNT = 2000;
    private static Object sink;

    @Test
    public void youngMonitorSurvivesGc() {
        // Dead objects below the monitor, so that defragmentation has a gap to move the monitor into
        makeGarbage();
        var lock = new Object();
        var hash = System.identityHashCode(lock);
        synchronized (lock) {
            forceGc();
            forceGc();
            var filler = overwriteFreeMemory();
            assertEquals(hash, System.identityHashCode(lock));
            assertTrue(Thread.holdsLock(lock));
            // One more GC traverses the monitor again: crashes if the header points to garbage
            forceGc();
            assertEquals(hash, System.identityHashCode(lock));
            sink = filler;
        }
        assertFalse(Thread.holdsLock(lock));
        assertEquals(hash, System.identityHashCode(lock));
    }

    @Test
    public void youngMonitorOfOldObjectSurvivesGc() {
        var lock = new Object();
        var hash = System.identityHashCode(lock);
        // Full GC in the C backend; promotes lock to the old generation
        System.gc();
        makeGarbage();
        synchronized (lock) {
            // The monitor is young and reachable only through the header of an old object,
            // i.e. only through the card table
            forceGc();
            forceGc();
            var filler = overwriteFreeMemory();
            assertEquals(hash, System.identityHashCode(lock));
            assertTrue(Thread.holdsLock(lock));
            forceGc();
            assertEquals(hash, System.identityHashCode(lock));
            sink = filler;
        }
        assertFalse(Thread.holdsLock(lock));
        assertEquals(hash, System.identityHashCode(lock));
    }

    @Test
    public void monitorOfStringLiteralSurvivesGc() {
        // String literals live outside the heap
        var lock = "MonitorGcTest.monitorOfStringLiteralSurvivesGc";
        var hash = System.identityHashCode(lock);
        makeGarbage();
        synchronized (lock) {
            forceGc();
            forceGc();
            var filler = overwriteFreeMemory();
            assertEquals(hash, System.identityHashCode(lock));
            assertTrue(Thread.holdsLock(lock));
            forceGc();
            assertEquals(hash, System.identityHashCode(lock));
            sink = filler;
        }
        assertFalse(Thread.holdsLock(lock));
        assertEquals(hash, System.identityHashCode(lock));
    }

    @Test
    public void stringLiteralsWithoutMonitorUnaffectedByGc() {
        var first = "MonitorGcTest.stringLiteralsWithoutMonitorUnaffectedByGc.1";
        var second = "MonitorGcTest.stringLiteralsWithoutMonitorUnaffectedByGc.2";
        var firstHash = System.identityHashCode(first);
        makeGarbage();
        forceGc();
        forceGc();
        System.gc();
        forceGc();
        assertEquals(firstHash, System.identityHashCode(first));
        // Never had identity hash code assigned
        var secondHash = System.identityHashCode(second);
        forceGc();
        assertEquals(secondHash, System.identityHashCode(second));
        synchronized (second) {
            forceGc();
            assertTrue(Thread.holdsLock(second));
        }
        assertEquals(secondHash, System.identityHashCode(second));
    }

    @Test
    public void synchronizedMethodAllocating() {
        var worker = new Worker();
        var hash = System.identityHashCode(worker);
        for (var i = 0; i < 20; ++i) {
            makeGarbage();
            assertEquals(GARBAGE_COUNT * 10, worker.work());
            assertEquals(hash, System.identityHashCode(worker));
            assertFalse(Thread.holdsLock(worker));
        }
    }

    @Test
    public void manyMonitorsSurviveGc() {
        var locks = new Object[50];
        var hashes = new int[locks.length];
        for (var i = 0; i < locks.length; ++i) {
            makeGarbage();
            locks[i] = new Object();
            hashes[i] = System.identityHashCode(locks[i]);
        }
        lockRecursively(locks, hashes, 0);
        for (var i = 0; i < locks.length; ++i) {
            assertFalse(Thread.holdsLock(locks[i]));
            assertEquals(hashes[i], System.identityHashCode(locks[i]));
        }
    }

    private static void lockRecursively(Object[] locks, int[] hashes, int index) {
        if (index == locks.length) {
            forceGc();
            forceGc();
            var filler = overwriteFreeMemory();
            for (var i = 0; i < locks.length; ++i) {
                assertTrue(Thread.holdsLock(locks[i]));
                assertEquals(hashes[i], System.identityHashCode(locks[i]));
            }
            forceGc();
            sink = filler;
            return;
        }
        synchronized (locks[index]) {
            makeGarbage();
            lockRecursively(locks, hashes, index + 1);
            assertTrue(Thread.holdsLock(locks[index]));
        }
    }

    static class Worker {
        synchronized int work() {
            var total = 0;
            for (var i = 0; i < 10; ++i) {
                synchronized (this) {
                    total += allocate();
                }
            }
            if (!Thread.holdsLock(this)) {
                return -1;
            }
            return total;
        }

        private int allocate() {
            var count = 0;
            for (var i = 0; i < GARBAGE_COUNT; ++i) {
                sink = new int[8];
                ++count;
            }
            return count;
        }
    }

    private static void makeGarbage() {
        for (var i = 0; i < GARBAGE_COUNT; ++i) {
            sink = new Object[4];
        }
        sink = null;
    }

    // Allocates until a young object becomes unreachable, i.e. at least one GC has happened
    private static void forceGc() {
        var ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            sink = new int[64];
        }
        sink = null;
    }

    // Fills memory freed by GC (including the old location of relocated monitors) with a recognizable pattern
    private static int[][] overwriteFreeMemory() {
        var result = new int[GARBAGE_COUNT][];
        for (var i = 0; i < result.length; ++i) {
            var array = new int[6];
            for (var j = 0; j < array.length; ++j) {
                array[j] = 0xDEADBEEF;
            }
            result[i] = array;
        }
        return result;
    }
}
