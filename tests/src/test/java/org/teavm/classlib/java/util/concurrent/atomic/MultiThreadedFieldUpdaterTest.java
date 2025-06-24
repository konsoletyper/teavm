/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI })
public class MultiThreadedFieldUpdaterTest {
    @Test
    public void getAndUpdate() {
        var updater = AtomicReferenceFieldUpdater.newUpdater(TestClass.class, String.class, "value");
        var obj = new TestClass();
        obj.value = "foo";

        var monitor1 = new Monitor();
        var monitor2 = new Monitor();
        var thread = new Thread(() -> {
            synchronized (monitor1) {
                if (monitor1.count > 0) {
                    try {
                        monitor1.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            updater.set(obj, "bar");
            synchronized (monitor2) {
                monitor2.count = 0;
                monitor2.notifyAll();
            }
        });
        thread.setDaemon(true);
        thread.start();

        updater.getAndUpdate(obj, value -> {
            synchronized (monitor1) {
                monitor1.count = 0;
                monitor1.notifyAll();
            }
            var result = value + "!";
            synchronized (monitor2) {
                if (monitor2.count > 0) {
                    try {
                        monitor2.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return result;
        });

        assertEquals("bar!", obj.value);
    }

    private static class TestClass {
        @Reflectable
        volatile String value;
    }

    private static class Monitor {
        int count = 1;
    }
}
