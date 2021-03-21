/*
 *  Copyright 2018 Alexey Andreev.
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

import java.util.Arrays;
import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;

@StaticInit
public final class EventQueue {
    private static Node[] data = new Node[16];
    private static int size;
    private static boolean finished;
    private static int idGenerator;

    private EventQueue() {
    }

    public static int offer(Event action) {
        return offer(action, System.currentTimeMillis());
    }

    public static int offer(Event action, long time) {
        ensureCapacity(size + 1);
        int current = size;
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (time < data[parent].time) {
                data[current] = data[parent];
                current = parent;
            } else {
                break;
            }
        }
        int id = idGenerator++;
        data[current] = new Node(id, action, time);
        ++size;
        if (current == 0) {
            interrupt();
        }
        return id;
    }

    public static void kill(int id) {
        for (int i = 0; i < size; ++i) {
            if (data[i].id == id) {
                remove(i);
                break;
            }
        }
    }

    public static void process() {
        while (size > 0 && !finished) {
            next();
        }
    }

    @Export(name = "teavm_processQueue")
    public static long processSingle() {
        if (size == 0) {
            return -1;
        }

        Node node = data[0];
        long currentTime = System.currentTimeMillis();
        if (node.time <= System.currentTimeMillis()) {
            remove(0);
            node.event.run();
            if (size == 0) {
                return -1;
            }
            return Math.max(0, node.time - currentTime);
        } else {
            return node.time - currentTime;
        }
    }

    @Export(name = "teavm_stopped")
    public static boolean isStopped() {
        return finished;
    }

    public static void stop() {
        finished = true;
    }

    private static void next() {
        while (data.length == 0) {
            waitUntil(System.currentTimeMillis() + 1000);
        }
        Node node = data[0];
        waitUntil(node.time);
        if (node.time <= System.currentTimeMillis()) {
            remove(0);
            node.event.run();
        }
    }

    private static void remove(int index) {
        --size;
        if (index < size) {
            data[index] = data[size];
            update(index);
        }
        data[size] = null;
    }

    private static void update(int index) {
        Node item = data[index];
        while (true) {
            int left = index * 2 + 1;
            int right = left + 1;
            int next;
            if (left >= size) {
                break;
            } else if (right >= size || data[left].time < data[right].time) {
                next = left;
            } else {
                next = right;
            }
            if (item.time <= data[next].time) {
                break;
            }
            data[index] = data[next];
            index = next;
        }
        data[index] = item;
    }

    private static void waitUntil(long time) {
        long diff = time - System.currentTimeMillis();
        if (diff <= 0) {
            return;
        }
        waitFor(diff);
    }

    @Import(name = "teavm_waitFor")
    @RuntimeInclude("fiber.h")
    private static native void waitFor(long time);

    @Import(name = "teavm_interrupt", module = "teavm")
    @RuntimeInclude("fiber.h")
    private static native void interrupt();

    private static void ensureCapacity(int capacity) {
        if (data.length >= capacity) {
            return;
        }
        capacity = Math.max(capacity, data.length * 3 / 2);
        data = Arrays.copyOf(data, capacity);
    }

    static class Node {
        final int id;
        final Event event;
        final long time;

        Node(int id, Event event, long time) {
            this.id = id;
            this.event = event;
            this.time = time;
        }
    }

    public interface Event {
        void run();
    }
}
