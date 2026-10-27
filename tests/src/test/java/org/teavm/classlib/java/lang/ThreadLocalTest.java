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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
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

    @Test
    public void inheritableThreadLocalPassesValueToNewThread() throws InterruptedException {
        var local = new InheritableThreadLocal<String>() {
            @Override
            protected String childValue(String parentValue) {
                return parentValue + "-child";
            }
        };
        var plain = new ThreadLocal<String>();
        local.set("parent");
        plain.set("parent");
        var seen = new String[2];
        var thread = new Thread(() -> {
            seen[0] = local.get();
            seen[1] = plain.get();
        });
        local.set("changed after create");
        thread.start();
        thread.join();
        assertEquals("parent-child", seen[0]);
        assertNull(seen[1]);
        assertEquals("changed after create", local.get());
    }

    // InheritableThreadLocal Javadoc: "when a child thread is created, the child receives initial values for all
    // inheritable thread-local variables for which the parent has values." childValue: "This method is called
    // from within the parent thread before the child is started."
    @Test
    public void inheritableThreadLocalEdgeCases() throws InterruptedException {
        var mainThread = Thread.currentThread();
        var childValueThreads = new Thread[1];
        var initialCalls = new int[1];
        var set = new InheritableThreadLocal<String>() {
            @Override
            protected String initialValue() {
                initialCalls[0]++;
                return "init";
            }

            @Override
            protected String childValue(String parentValue) {
                childValueThreads[0] = Thread.currentThread();
                return parentValue;
            }
        };
        var gotten = new InheritableThreadLocal<String>() {
            @Override
            protected String initialValue() {
                initialCalls[0]++;
                return "gotten";
            }
        };
        var unset = new InheritableThreadLocal<String>() {
            @Override
            protected String initialValue() {
                initialCalls[0]++;
                return "own";
            }
        };
        var setToNull = new InheritableThreadLocal<String>() {
            @Override
            protected String initialValue() {
                initialCalls[0]++;
                return "not null";
            }
        };
        var removed = new InheritableThreadLocal<String>();

        set.set("a");
        assertEquals("gotten", gotten.get());
        setToNull.set(null);
        removed.set("gone");
        removed.remove();
        assertEquals(1, initialCalls[0]);

        var seen = new String[8];
        var child = new Thread(() -> {
            seen[0] = set.get();
            seen[1] = gotten.get();
            seen[2] = setToNull.get();
            seen[3] = removed.get();
            seen[4] = unset.get();
            set.set("changed in child");
            // A grandchild takes the child's values, including ones the child inherited.
            var grandchild = new Thread(() -> {
                seen[5] = set.get();
                seen[6] = gotten.get();
            });
            grandchild.start();
            try {
                grandchild.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertSame(mainThread, childValueThreads[0]);
        child.start();
        child.join();

        assertEquals("a", seen[0]);
        assertEquals("gotten", seen[1]);
        assertNull(seen[2]);
        assertNull(seen[3]);
        assertEquals("own", seen[4]);
        assertEquals("changed in child", seen[5]);
        assertEquals("gotten", seen[6]);
        // Only unset.get() in the child called initialValue again.
        assertEquals(2, initialCalls[0]);
        assertEquals("a", set.get());
        assertEquals("own", unset.get());
    }

    // childValue is user code. Both locals create another InheritableThreadLocal there, so whichever runs first
    // changes the set of locals while the snapshot still has the other one to visit.
    @Test
    public void childValueMayCreateInheritableThreadLocal() throws InterruptedException {
        var created = new ArrayList<InheritableThreadLocal<String>>();
        class Creating extends InheritableThreadLocal<String> {
            @Override
            protected String childValue(String parentValue) {
                created.add(new InheritableThreadLocal<>());
                return parentValue + "!";
            }
        }
        var first = new Creating();
        var second = new Creating();
        first.set("1");
        second.set("2");
        var seen = new String[2];
        var thread = new Thread(() -> {
            seen[0] = first.get();
            seen[1] = second.get();
        });
        thread.start();
        thread.join();
        assertEquals("1!", seen[0]);
        assertEquals("2!", seen[1]);
        assertEquals(2, created.size());
    }
}
