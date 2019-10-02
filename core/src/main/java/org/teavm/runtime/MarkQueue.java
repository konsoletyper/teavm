/*
 *  Copyright 2016 Alexey Andreev.
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

import org.teavm.interop.Address;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
final class MarkQueue {
    private MarkQueue() {
    }

    private static int head;
    private static int tail;
    private static int limit;

    static void init() {
        head = 0;
        tail = 0;
        limit = GC.gcStorageSize() / 4;
    }

    static void enqueue(RuntimeObject object) {
        GC.gcStorageAddress().add(4 * tail).putInt(pack(object.toAddress()));
        if (++tail >= limit) {
            tail = 0;
        }
        if (tail == head) {
            ExceptionHandling.printStack();
            GC.outOfMemory();
        }
    }

    static RuntimeObject dequeue() {
        Address result = unpack(GC.gcStorageAddress().add(4 * head).getInt());
        if (++head >= limit) {
            head = 0;
        }
        return result.toStructure();
    }

    private static int pack(Address address) {
        return (int) ((address.toLong() - GC.heapAddress().toLong()) >>> 2);
    }

    private static Address unpack(int packed) {
        return GC.heapAddress().add((long) packed << 2);
    }

    static boolean isEmpty() {
        return head == tail;
    }
}
