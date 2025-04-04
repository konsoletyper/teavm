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

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@OnlyPlatform(TestPlatform.WEBASSEMBLY_GC)
@SkipJVM
public class HeapTest {
    private Address startBackup;
    private int minSizeBackup;
    private int maxSizeBackup;

    @Before
    public void saveHeapState() {
        startBackup = Heap.getStart();
        minSizeBackup = Heap.getCurrentSize();
        maxSizeBackup = Heap.getMaxSize();
    }

    @After
    public void restoreHeapState() {
        Heap.init(startBackup, minSizeBackup, maxSizeBackup);
    }

    @Test
    public void allocRelease() {
        var a = Heap.alloc(65536);
        dump();
        assertEquals(8, a.diff(Heap.getStart()));
        check();

        var b = Heap.alloc(256);
        dump();
        assertEquals(65552, b.diff(Heap.getStart()));
        check();

        Heap.release(a);
        dump();
        a = Heap.alloc(256);
        dump();
        assertEquals(8, a.diff(Heap.getStart()));
        check();

        var c = Heap.alloc(256);
        dump();
        assertEquals(272, c.diff(Heap.getStart()));
        check();

        var d = Heap.alloc(65536);
        dump();
        assertEquals(65816, d.diff(Heap.getStart()));
        check();

        Heap.release(a);
        dump();
        check();
        Heap.release(c);
        dump();
        check();
        a = Heap.alloc(65536);
        dump();
        check();
        assertEquals(8, a.diff(Heap.getStart()));
        dump();
        check();
    }

    @Test
    public void grow() {
        var a = Heap.alloc(65536);
        dump();
        assertEquals(8, a.diff(Heap.getStart()));
        check();

        var b = Heap.alloc(2 * 1024 * 1024);
        dump();
        assertEquals(65552, b.diff(Heap.getStart()));
        check();

        var c = Heap.alloc(256);
        dump();
        assertEquals(2162712, c.diff(Heap.getStart()));
        check();
    }

    @Test
    public void alloc1() {
        var a = Heap.alloc(256);
        dump();
        var b = Heap.alloc(32);
        dump();
        Heap.release(a);
        dump();
        Heap.release(b);
        dump();
        check();
    }

    @Test
    public void smallAlloc() {
        var a = Heap.alloc(3);
        dump();
        assertEquals(8, a.diff(Heap.getStart()));

        var b = Heap.alloc(5);
        dump();
        assertEquals(32, b.diff(Heap.getStart()));
    }

    // This test is not stable, so I would not add it to CI.
    // Instead, it's useful to uncomment and run it when things should be updated in Heap
    public void randomAlloc() {
        var list = new ArrayList<Integer>();
        var random = new Random();
        for (var i = 0; i < 1000; ++i) {
            if (random.nextBoolean()) {
                var size = random.nextInt(2048);
                var addr = Heap.alloc(size).toInt();
                list.add(addr);
                System.out.println("Allocated " + size + " at " + (addr - Heap.getStart().toInt()));
                dump();
                check();
            } else if (!list.isEmpty()) {
                var addr = Address.fromInt(list.remove(random.nextInt(list.size())));
                Heap.release(addr);
                System.out.println("Released at " + (addr.toInt() - Heap.getStart().toInt()));
                dump();
                check();
            }
        }
    }

    private static void dump() {
        var addr = Heap.getStart();
        while (addr != Heap.getEnd()) {
            HeapRecord struct = addr.toStructure();
            var next = addr.add(Structure.sizeOf(HeapRecord.class) + HeapRecord.size(struct));
            var allocated = HeapRecord.isAllocated(struct);
            System.out.print(allocated ? "*" : ".");
            System.out.print("[");
            System.out.print(addr.diff(Heap.getStart()) + Structure.sizeOf(HeapRecord.class));
            System.out.print("..");
            System.out.print(next.diff(Heap.getStart()));
            System.out.print(")");
            if (!allocated) {
                var node = (HeapNode) struct;
                if (node == Heap.getRoot()) {
                    System.out.print("#");
                }
                if ((node.flags & HeapNode.FLAG_LIST) != 0) {
                    System.out.print("@");
                }
                System.out.print(":");
                if (node.left == null) {
                    System.out.print("_");
                } else {
                    System.out.print(node.left.toAddress().diff(Heap.getStart()));
                }
                System.out.print(":");
                if (node.right == null) {
                    System.out.print("_");
                } else {
                    System.out.print(node.right.toAddress().diff(Heap.getStart()));
                }
                if (node.list != null) {
                    System.out.print(":");
                    System.out.print(node.list.toAddress().diff(Heap.getStart()));
                }
            }
            System.out.print(" ");
            addr = next;
        }
        System.out.println();
    }

    private static void check() {
        var reachedInTree = new TreeSet<Integer>();
        if (Heap.getRoot() != null) {
            var root = Heap.getRoot();
            check(root, 0, Integer.MAX_VALUE, reachedInTree);
        }
        var addr = Heap.getStart();
        var reachedInHeap = new TreeSet<Integer>();
        Address prev = null;
        while (addr != Heap.getEnd()) {
            HeapRecord struct = addr.toStructure();
            if (prev != null) {
                if (addr.add(-struct.previousSize - Structure.sizeOf(HeapRecord.class)) != prev) {
                    throw new AssertionError("Previous size at " + addr.diff(Heap.getStart()) + " inconsistent. "
                            + "Should be " + (struct.toAddress().diff(prev) - Structure.sizeOf(HeapRecord.class))
                            + ", but actual value is " + struct.previousSize);
                }
            }
            var next = addr.add(Structure.sizeOf(HeapRecord.class) + HeapRecord.size(struct));
            if (!HeapRecord.isAllocated(struct)) {
                reachedInHeap.add((int) struct.toAddress().diff(Heap.getStart()));
            }
            prev = addr;
            addr = next;
        }
        assertEquals("Nodes reached through tree links, differ from nodes, reached by direct traversal",
                reachedInHeap, reachedInTree);
    }

    private static void check(HeapNode node, int min, int max, Set<Integer> reached) {
        if (node == null) {
            return;
        }
        reached.add((int) node.toAddress().diff(Heap.getStart()));
        if ((node.size & HeapRecord.ALLOCATED) != 0) {
            throw new AssertionError("Node at " + node.toAddress().diff(Heap.getStart()) + ": invalid allocated flag");
        }
        if (node.size < min || node.size > max) {
            throw new AssertionError("Node at " + node.toAddress().diff(Heap.getStart()) + ": size out of range ["
                    + min + "," + max + "]");
        }
        var height = 0;
        check(node.left, min, node.size - 1, reached);
        if (node.left != null) {
            height = Math.max(height, node.left.height);
        }
        check(node.right, node.size + 1, max, reached);
        if (node.right != null) {
            height = Math.max(height, node.right.height);
        }
        if (height + 1 != node.height) {
            throw new AssertionError("Node at " + node.toAddress().diff(Heap.getStart()) + ": inconsistent height");
        }
        if (node.list != null) {
            var prev = node;
            var n = node.list;
            while (n != null) {
                reached.add((int) n.toAddress().diff(Heap.getStart()));
                if (n.left != prev) {
                    throw new AssertionError("Node at " + node.toAddress().diff(Heap.getStart())
                            + ": double-linked listed corrupt");
                }
                prev = n;
                n = n.right;
            }
        }
    }
}
