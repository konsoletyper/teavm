/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.runtime.heap;

import org.teavm.interop.Address;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
public final class Heap {
    private Heap() {
    }

    private static Address start;
    private static Address end;
    private static int currentSize;
    private static int maxSize;
    private static HeapNode root;

    static void init(Address start, int minSize, int maxSize) {
        Heap.start = start;
        Heap.end = start.add(minSize);
        Heap.currentSize = minSize;
        Heap.maxSize = maxSize;
        root = start.toStructure();
        root.size = minSize - Structure.sizeOf(HeapRecord.class);
        root.left = null;
        root.right = null;
        root.list = null;
        root.height = 1;
        root.flags = 0;
    }

    static Address getStart() {
        return start;
    }

    static Address getEnd() {
        return end;
    }

    static HeapNode getRoot() {
        return root;
    }

    static int getCurrentSize() {
        return currentSize;
    }

    static int getMaxSize() {
        return maxSize;
    }

    static void resetRoot() {
        root = null;
    }

    @Export(name = "teavm.malloc")
    public static Address alloc(int bytes) {
        bytes = alignSize(bytes);
        var result = tryAlloc(bytes);
        if (result == null) {
            var last = lastRecord();
            var amountToGrow = bytes - (!HeapRecord.isAllocated(last) ? HeapRecord.size(last) : 0);
            if (amountToGrow > 0 && tryExtend(amountToGrow, last)) {
                result = tryAlloc(bytes);
            }
        }
        return result;
    }

    @Export(name = "teavm.realloc")
    public static Address realloc(Address address, int newSize) {
        newSize = alignSize(newSize);

        var record = HeapRecord.recordOf(address);
        var oldSize = HeapRecord.size(record);
        if (oldSize == newSize) {
            return address;
        }

        if (newSize < oldSize) {
            shrink(record, oldSize, newSize);
            return address;
        } else {
            return expand(record, oldSize, newSize);
        }
    }

    private static int alignSize(int bytes) {
        bytes = (Integer.divideUnsigned(bytes - 1, Address.sizeOf()) + 1) * Address.sizeOf();
        bytes = Math.max(bytes, Structure.sizeOf(HeapNode.class) - Structure.sizeOf(HeapRecord.class));
        return bytes;
    }

    private static void shrink(HeapRecord record, int oldSize, int newSize) {
        var oldNextRecord = HeapRecord.next(record);
        if (oldNextRecord.toAddress() == end || HeapRecord.isAllocated(oldNextRecord)) {
            if (oldSize - newSize < Structure.sizeOf(HeapNode.class) - Structure.sizeOf(HeapRecord.class)) {
                return;
            }
        }
        record.size = newSize;
        var newNextRecord = HeapRecord.next(record);
        newNextRecord.previousSize = newSize;
        if (oldNextRecord.toAddress() == end) {
            newNextRecord.size = oldNextRecord.size;
        } else if (HeapRecord.isAllocated(oldNextRecord)) {
            oldNextRecord.previousSize = newSize - oldSize - Structure.sizeOf(HeapRecord.class);
            newNextRecord.size = oldNextRecord.size;
        } else {
            var nextNextRecord = HeapRecord.next(oldNextRecord);
            delete((HeapNode) oldNextRecord);
            newNextRecord.size = newSize - oldSize + HeapRecord.size(oldNextRecord);
            nextNextRecord.previousSize = newNextRecord.size;
        }
        insert((HeapNode) newNextRecord);
    }

    private static Address expand(HeapRecord record, int oldSize, int newSize) {
        var nextRecord = HeapRecord.next(record);
        if (nextRecord.toAddress() == end) {
            if (!tryExtend(newSize - oldSize, record)) {
                return null;
            }
        } else if (!HeapRecord.isAllocated(nextRecord)) {
            var nextNextRecord = HeapRecord.next(nextRecord);
            if (nextNextRecord.toAddress() == end) {
                if (!tryExtend(newSize - oldSize, nextRecord)) {
                    return null;
                }
            }
        }

        if (!HeapRecord.isAllocated(nextRecord)
                && HeapRecord.size(nextRecord) + Structure.sizeOf(HeapRecord.class) >= newSize - oldSize) {
            expandInPlace(record, oldSize, newSize, nextRecord);
            return HeapRecord.dataOf(record);
        } else {
            var newRegion = alloc(newSize);
            var oldAddress = HeapRecord.dataOf(record);
            Address.moveMemoryBlock(oldAddress, newRegion, oldSize);
            release(oldAddress);
            return newRegion;
        }
    }

    private static void expandInPlace(HeapRecord record, int oldSize, int newSize, HeapRecord nextRecord) {
        var oldNextRecordSize = HeapRecord.size(nextRecord);
        var nextNextRecord = HeapRecord.next(nextRecord);
        var newNextRecord = HeapRecord.next(record, newSize);
        if (nextNextRecord.toAddress().isLessThan(newNextRecord.toAddress().add(Structure.sizeOf(HeapNode.class)))) {
            newSize = (int) nextNextRecord.toAddress().diff(HeapRecord.dataOf(record));
            newNextRecord = nextNextRecord;
        }
        delete((HeapNode) nextRecord);
        record.size = newSize | HeapNode.ALLOCATED;
        if (nextNextRecord.toAddress() == newNextRecord.toAddress()) {
            if (newNextRecord.toAddress().isLessThan(end)) {
                newNextRecord.previousSize = newSize;
            }
        } else {
            newNextRecord.previousSize = newSize;
            newNextRecord.size = oldNextRecordSize - (newSize - oldSize);
            if (nextNextRecord.toAddress().isLessThan(end)) {
                nextNextRecord.previousSize = newNextRecord.size;
            }
            insert((HeapNode) newNextRecord);
        }
    }

    private static boolean tryExtend(int bytes, HeapRecord last) {
        if (currentSize + bytes > maxSize) {
            return false;
        }
        var grownBytes = grow(bytes);
        if (grownBytes == 0) {
            return false;
        }
        currentSize += grownBytes;
        if (!HeapRecord.isAllocated(last)) {
            delete((HeapNode) last);
            last.size += grownBytes;
            insert((HeapNode) last);
        } else {
            var newEmpty = (HeapNode) end.toStructure();
            newEmpty.size = grownBytes - Structure.sizeOf(HeapRecord.class);
            newEmpty.previousSize = HeapRecord.size(last);
            insert(newEmpty);
        }
        end = start.add(currentSize);
        maxSize = Math.max(currentSize, maxSize);
        notifyHeapResized();
        return grownBytes >= bytes;
    }

    @Import(module = "teavmMemory", name = "notifyHeapResized")
    private static native void notifyHeapResized();

    private static HeapNode lastRecord() {
        HeapRecord record = start.toStructure();
        HeapRecord result = null;
        while (record.toAddress() != end) {
            result = record;
            record = HeapRecord.next(record);
        }
        return (HeapNode) result;
    }

    private static Address tryAlloc(int bytes) {
        if (root == null) {
            return Address.fromInt(0);
        }

        var free = findFree(root, bytes);
        if (free == null) {
            return Address.fromInt(0);
        }
        return allocFromFreeRecord(free, bytes);
    }

    private static Address allocFromFreeRecord(HeapNode free, int bytes) {
        delete(free);
        var currentSize = HeapNode.size(free);
        if (currentSize - bytes >= Structure.sizeOf(HeapNode.class) + Structure.sizeOf(HeapRecord.class)) {
            var nextFree = (HeapNode) HeapRecord.next(free, bytes);
            nextFree.size = currentSize - bytes - Structure.sizeOf(HeapRecord.class);
            nextFree.previousSize = bytes;
            insert(nextFree);
            free.size = bytes | HeapNode.ALLOCATED;
            var nextNext = (HeapNode) HeapRecord.next(nextFree);
            if (nextNext.toAddress().isLessThan(end)) {
                nextNext.previousSize -= bytes + Structure.sizeOf(HeapRecord.class);
            }
        } else {
            free.size |= HeapNode.ALLOCATED;
        }

        return free.toAddress().add(Structure.sizeOf(HeapRecord.class));
    }

    @Export(name = "teavm.free")
    public static void release(Address address) {
        var record = HeapRecord.recordOf(address);
        var size = HeapRecord.size(record);
        if (start.isLessThan(record.toAddress())) {
            var previousRecord = HeapRecord.previous(record);
            if (!HeapRecord.isAllocated(previousRecord)) {
                size += previousRecord.size + Structure.sizeOf(HeapRecord.class);
                delete((HeapNode) previousRecord);
                record = previousRecord;
            }
        }

        var nextRecord = HeapRecord.next(record, size);
        if (nextRecord.toAddress().isLessThan(end) && !HeapRecord.isAllocated(nextRecord)) {
            var bytes = nextRecord.size + Structure.sizeOf(HeapRecord.class);
            size += bytes;
            delete((HeapNode) nextRecord);
        }

        record.size = size;
        insert((HeapNode) record);

        var next = HeapRecord.next(record);
        if (next.toAddress().isLessThan(end)) {
            next.previousSize = size;
        }
    }

    static void insert(HeapNode node) {
        node.height = 1;
        node.list = null;
        node.left = null;
        node.right = null;
        node.flags = 0;
        root = insert(root, node);
    }

    private static HeapNode insert(HeapNode into, HeapNode node) {
        if (into == null) {
            node.left = null;
            node.right = null;
            node.flags &= ~HeapNode.FLAG_LIST;
            return node;
        }
        if (node.size < into.size) {
            into.left = insert(into.left, node);
        } else if (node.size > into.size) {
            into.right = insert(into.right, node);
        } else {
            node.left = into;
            node.flags |= HeapNode.FLAG_LIST;
            node.right = into.list;
            if (into.list != null) {
                into.list.left = node;
            }
            into.list = node;
            return into;
        }
        HeapNode.fix(into);
        return HeapNode.balance(into);
    }

    static void delete(HeapNode node) {
        if ((node.flags & HeapNode.FLAG_LIST) == 0) {
            if (node.list != null) {
                var newHead = node.list;
                updateListHolder(node);
                newHead.list = newHead.right;
                newHead.left = node.left;
                newHead.right = node.right;
                newHead.height = node.height;
                newHead.flags &= ~HeapNode.FLAG_LIST;
            } else {
                root = delete(root, node);
            }
        } else {
            if ((node.left.flags & HeapNode.FLAG_LIST) == 0) {
                node.left.list = node.right;
            } else {
                node.left.right = node.right;
            }
            if (node.right != null) {
                node.right.left = node.left;
            }
        }
    }

    private static void updateListHolder(HeapNode node) {
        if (root == node) {
            root = node.list;
            return;
        }
        var at = root;
        while (true) {
            if (at.left == node) {
                at.left = node.list;
                break;
            } else if (at.right == node) {
                at.right = node.list;
                break;
            } else if (node.size < at.size) {
                at = at.left;
            } else {
                at = at.right;
            }
        }
    }

    private static HeapNode delete(HeapNode root, HeapNode node) {
        if (root == null) {
            return null;
        }
        if (node.size < root.size) {
            root.left = delete(root.left, node);
        } else if (node.size > root.size) {
            root.right = delete(root.right, node);
        } else if (root.left == null) {
            return root.right;
        } else if (root.right == null) {
            return root.left;
        } else {
            var left = root.left;
            var min = findMinimum(root.right);
            min.right = removeMinimum(root.right);
            min.left = left;
            root = min;
        }
        HeapNode.fix(root);
        return HeapNode.balance(root);
    }

    private static HeapNode removeMinimum(HeapNode node) {
        if (node.left == null) {
            return node.right;
        } else {
            node.left = removeMinimum(node.left);
            HeapNode.fix(node);
            return HeapNode.balance(node);
        }
    }

    private static HeapNode findMinimum(HeapNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private static HeapNode findFree(HeapNode node, int bytes) {
        while (node != null) {
            if (node.size < bytes) {
                node = node.right;
            } else if (node.size > bytes) {
                if (node.left == null || node.left.size < bytes) {
                    break;
                }
                node = node.left;
            } else {
                break;
            }
        }
        return node;
    }

    static native int grow(int bytes);
}
