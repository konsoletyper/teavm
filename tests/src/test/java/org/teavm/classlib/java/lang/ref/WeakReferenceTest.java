/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.classlib.java.lang.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.support.GCSupport;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
public class WeakReferenceTest {
    @Test
    public void weakReferenceDeps() {
        var a = new A();
        var b = new B();
        var weakRefs = List.of(new WeakReference<>(a), new WeakReference<>(b));
        var sb = new StringBuilder();
        for (var ref : weakRefs) {
            sb.append(ref.get().foo()).append(";");
        }
        assertEquals("A.foo;B.foo;", sb.toString());
    }

    @Test
    @SkipPlatform({TestPlatform.WEBASSEMBLY_GC, TestPlatform.JAVASCRIPT})
    public void deref() {
        var ref = createAndTestRef(null);
        GCSupport.tryToTriggerGC(ref);
        assertNull(ref.get());
    }

    @Test
    @SkipPlatform({TestPlatform.WEBASSEMBLY_GC, TestPlatform.JAVASCRIPT})
    public void refQueue() {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        GCSupport.tryToTriggerGC(ref);
        int attemptCount = 0;
        Object value;
        do {
            value = queue.poll();
            if (value != null) {
                break;
            }
            waitInJVM();
        } while (attemptCount++ < 50);
        assertSame(ref, value);
    }

    private static void waitInJVM() {
        if (!PlatformDetector.isTeaVM()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    @Test
    @SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY_GC, TestPlatform.JAVASCRIPT })
    public void queueRemove() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        var threadQueue = new ArrayBlockingQueue<>(4);
        var thread = new Thread(() -> {
            try {
                threadQueue.add(queue.remove());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();

        GCSupport.tryToTriggerGC(ref);
        var result = threadQueue.poll(2, TimeUnit.SECONDS);
        assertSame(ref, result);
    }

    private WeakReference<Object> createAndTestRef(ReferenceQueue<Object> queue) {
        var obj = new Object();
        var ref = new WeakReference<>(obj, queue);
        assertSame(obj, ref.get());
        return ref;
    }

    @Test
    public void clear() {
        var obj = new Object();
        var ref = new WeakReference<>(obj);
        assertSame(obj, ref.get());

        ref.clear();
        assertNull(ref.get());
    }

    @Test
    public void enqueueClears() {
        var obj = new Object();
        var ref = new WeakReference<>(obj, new ReferenceQueue<>());
        assertSame(obj, ref.get());

        ref.enqueue();
        assertNull(ref.get());
    }

    @Test
    public void clearedRefNotEnqueued() {
        var queue = new ReferenceQueue<>();
        var refs = new WeakReference<?>[2];
        runInAnotherThread(() -> {
            var obj = new Object();
            refs[0] = new WeakReference<>(obj, queue);
            refs[1] = new WeakReference<>(obj, queue);
        });
        refs[1].clear();
        GCSupport.tryToTriggerGC(refs[0]);
        assertEquals(List.of(refs[0]), pollUntilEnqueued(queue, refs[0]));
    }

    @Test
    public void unreachableRefNotEnqueued() {
        var queue = new ReferenceQueue<>();
        var holder = new Object[1];
        var refs = new WeakReference<?>[2];
        runInAnotherThread(() -> {
            holder[0] = new Object();
            refs[0] = new WeakReference<>(holder[0], queue);
            refs[1] = new WeakReference<>(new WeakReference<>(holder[0], queue));
        });
        GCSupport.tryToTriggerGC(refs[1]);
        assertNull(refs[1].get());

        holder[0] = null;
        GCSupport.tryToTriggerGC(refs[0]);
        assertEquals(List.of(refs[0]), pollUntilEnqueued(queue, refs[0]));
    }

    // Objects that a test expects to be collected are created in another thread. When the optimizer inlines a helper
    // method, its temporary variables become variables of the test method, which may keep them reachable.
    private static void runInAnotherThread(Runnable action) {
        var thread = new Thread(action);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> pollUntilEnqueued(ReferenceQueue<?> queue, Object expected) {
        var result = new ArrayList<Object>();
        for (var i = 0; i < 50 && !result.contains(expected); ++i) {
            sleep(100);
            for (var ref = queue.poll(); ref != null; ref = queue.poll()) {
                result.add(ref);
            }
        }
        sleep(100);
        for (var ref = queue.poll(); ref != null; ref = queue.poll()) {
            result.add(ref);
        }
        return result;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    interface I {
        String foo();
    }

    static class A implements I {
        @Override
        public String foo() {
            return "A.foo";
        }
    }
    
    static class B implements I {
        @Override
        public String foo() {
            return "B.foo";
        }
    }
}
