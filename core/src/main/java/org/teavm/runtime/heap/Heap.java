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

    public static Address alloc(int bytes) {
        bytes = (Integer.divideUnsigned(bytes - 1, Address.sizeOf()) + 1) * Address.sizeOf();
        bytes = Math.max(bytes, Structure.sizeOf(HeapNode.class) - Structure.sizeOf(HeapRecord.class));
        var result = tryAlloc(bytes);
        if (result == null) {
            var last = lastFreeRecord();
            var amountToGrow = bytes - (last != null ? HeapRecord.size(last) : 0);
            if (amountToGrow > 0 && tryExtend(amountToGrow, last)) {
                result = tryAlloc(bytes);
            }
        }
        return result;
    }

    private static boolean tryExtend(int bytes, HeapNode last) {
        if (currentSize + bytes > maxSize) {
            return false;
        }
        var grownBytes = grow(bytes);
        if (grownBytes == 0) {
            return false;
        }
        currentSize += grownBytes;
        delete(last);
        last.size += grownBytes;
        insert(last);
        end = start.add(currentSize);
        maxSize = Math.max(currentSize, maxSize);
        return grownBytes >= bytes;
    }

    private static HeapNode lastFreeRecord() {
        HeapRecord record = start.toStructure();
        HeapRecord result = null;
        while (record.toAddress() != end) {
            var size = HeapRecord.size(record);
            result = HeapRecord.isAllocated(record) ? Address.fromInt(0).toStructure() : record;
            record = record.toAddress().add(Structure.sizeOf(HeapRecord.class) + size).toStructure();
        }
        return (HeapNode) result;
    }

    private static Address tryAlloc(int bytes) {
        if (root == null) {
            return null;
        }

        var free = findFree(root, bytes);
        if (free == null) {
            return null;
        }
        delete(free);
        var currentSize = HeapNode.size(free);
        if (currentSize - bytes >= Structure.sizeOf(HeapNode.class) + Structure.sizeOf(HeapRecord.class)) {
            HeapNode nextFree = free.toAddress().add(Structure.sizeOf(HeapRecord.class) + bytes).toStructure();
            nextFree.size = currentSize - bytes - Structure.sizeOf(HeapRecord.class);
            nextFree.previousSize = bytes;
            insert(nextFree);
            free.size = bytes | HeapNode.ALLOCATED;
            HeapNode nextNext = nextFree.toAddress().add(Structure.sizeOf(HeapRecord.class) + nextFree.size)
                    .toStructure();
            if (nextNext.toAddress().isLessThan(end)) {
                nextNext.previousSize -= bytes + Structure.sizeOf(HeapRecord.class);
            }
        } else {
            free.size |= HeapNode.ALLOCATED;
        }

        return free.toAddress().add(Structure.sizeOf(HeapRecord.class));
    }

    public static void release(Address address) {
        HeapRecord record = address.add(-Structure.sizeOf(HeapRecord.class)).toStructure();
        var size = HeapRecord.size(record);
        if (start.isLessThan(record.toAddress())) {
            HeapRecord previousRecord = record.toAddress()
                    .add(-record.previousSize - Structure.sizeOf(HeapRecord.class))
                    .toStructure();
            if (!HeapRecord.isAllocated(previousRecord)) {
                size += previousRecord.size + Structure.sizeOf(HeapRecord.class);
                delete((HeapNode) previousRecord);
                record = previousRecord;
            }
        }

        HeapRecord nextRecord = record.toAddress().add(Structure.sizeOf(HeapRecord.class) + size).toStructure();
        if (nextRecord.toAddress().isLessThan(end) && !HeapRecord.isAllocated(nextRecord)) {
            var bytes = nextRecord.size + Structure.sizeOf(HeapRecord.class);
            size += bytes;
            delete((HeapNode) nextRecord);
        }

        record.size = size;
        insert((HeapNode) record);

        HeapRecord next = record.toAddress().add(size + Structure.sizeOf(HeapRecord.class)).toStructure();
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
