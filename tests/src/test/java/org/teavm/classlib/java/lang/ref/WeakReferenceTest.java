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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.support.GCSupport;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class WeakReferenceTest {
    @Test
    @SkipPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY, TestPlatform.WASI,
            TestPlatform.WEBASSEMBLY_GC })
    public void deref() {
        var ref = createAndTestRef(null);
        GCSupport.tryToTriggerGC(ref);
        assertNull(ref.get());
    }

    @Test
    @SkipPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC })
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
    @SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC })
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
}
