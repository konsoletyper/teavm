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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class MultiThreadConcurrentHashMapTest {
    private ArrayBlockingQueue<Runnable> backgroundTasks = new ArrayBlockingQueue<>(100);
    private boolean stopped;

    public MultiThreadConcurrentHashMapTest() {
        var t = new Thread(() -> {
            while (!stopped) {
                try {
                    backgroundTasks.take().run();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
    }

    @After
    public void dispose() {
        backgroundTasks.add(() -> stopped = true);
    }

    @Test
    @SkipJVM
    public void containsValue() {
        var key = new Wrapper("q");
        var value = new Wrapper("23");
        var map = new ConcurrentHashMap<>(Map.of(key, value));

        assertTrue(map.containsValue(new Wrapper("23")));
        assertTrue(map.containsValue(new Wrapper("23", () -> map.remove(key))));
    }

    // In JVM we have deadlock here, since JVM implementation relies on blocking.
    // Our algorithm is non-blocking so no problem for TeaVM.
    @Test
    @SkipJVM
    public void concurrentPut() {
        var key = new Wrapper("q");
        var value = new Wrapper("23");
        var map = new ConcurrentHashMap<>(Map.of(key, value));

        map.put(key, value);
        var old = map.put(new Wrapper("q", () -> map.put(key, new Wrapper("24"))), new Wrapper("25"));

        assertEquals("24", old.s);
        assertEquals("25", map.get(key).s);
    }

    @Test
    @SkipJVM
    public void concurrentPutRemove() {
        var key = new Wrapper("q");
        var value = new Wrapper("23");
        var map = new ConcurrentHashMap<>(Map.of(key, value));

        map.put(key, value);
        var old = map.put(new Wrapper("q", () -> map.remove(key)), new Wrapper("25"));

        assertNull(old);
        assertEquals("25", map.get(key).s);
    }

    @Test
    @SkipJVM
    public void concurrentRemovePut() {
        var key = new Wrapper("q");
        var value = new Wrapper("23");
        var map = new ConcurrentHashMap<>(Map.of(key, value));

        map.put(key, value);
        var old = map.remove(new Wrapper("q", () -> map.put(key, new Wrapper("24"))));

        assertEquals("24", old.s);
        assertNull(map.get(key));
    }

    private void runInBackground(Runnable runnable) {
        backgroundTasks.add(runnable);
    }

    private class Wrapper {
        private final String s;
        private Runnable task;

        Wrapper(String s) {
            this(s, null);
        }

        Wrapper(String s, Runnable task) {
            this.s = s;
            this.task = task;
        }

        @Override
        public boolean equals(Object o) {
            awaitIfNecessary();
            if (o instanceof Wrapper) {
                ((Wrapper) o).awaitIfNecessary();
            }

            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var key = (Wrapper) o;
            return Objects.equals(s, key.s);
        }

        @Override
        public int hashCode() {
            awaitIfNecessary();
            return s.hashCode();
        }

        private void awaitIfNecessary() {
            if (task != null) {
                var t = task;
                task = null;
                runInBackground(() -> {
                    t.run();
                    send();
                });
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        void send() {
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
