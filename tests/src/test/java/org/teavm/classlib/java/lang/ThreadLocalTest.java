/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class ThreadLocalTest {
    private volatile int counter;

    @Test
    @OnlyPlatform(TestPlatform.JAVASCRIPT)
    public void concurrentUpdate() throws InterruptedException {
        var local = new ThreadLocal<String>();
        var monitor = new Object();
        var results = new String[5];
        counter = results.length;

        for (var n = 0; n < results.length; ++n) {
            var threadIndex = n;
            new Thread(() -> {
                var prefix = Character.toString((char) ('a' + threadIndex));
                for (var i = 0; i < 10; ++i) {
                    var old = local.get();
                    if (old == null) {
                        old = "";
                    }
                    local.set(old + prefix + i);
                    sleep();
                }
                synchronized (monitor) {
                    results[threadIndex] = local.get();
                    --counter;
                    monitor.notifyAll();
                }
            }).start();
        }

        do {
            synchronized (monitor) {
                monitor.wait();
            }
        } while (counter > 0);

        assertEquals("a0a1a2a3a4a5a6a7a8a9", results[0]);
        assertEquals("b0b1b2b3b4b5b6b7b8b9", results[1]);
        assertEquals("c0c1c2c3c4c5c6c7c8c9", results[2]);
        assertEquals("d0d1d2d3d4d5d6d7d8d9", results[3]);
        assertEquals("e0e1e2e3e4e5e6e7e8e9", results[4]);
    }

    private void sleep() {
        if (!PlatformDetector.isJavaScript()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            sleepInJs();
        }
    }

    @Async
    private static native void sleepInJs();

    private static void sleepInJs(AsyncCallback<Void> callback) {
        Window.setTimeout(() -> callback.complete(null), 0);
    }
}
