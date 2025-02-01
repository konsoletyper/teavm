/*
 *  Copyright 2025 konsoletyper.
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
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Address;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@OnlyPlatform(TestPlatform.WEBASSEMBLY_GC)
@SkipJVM
public class HeapTreeTest {
    private static Address address = Heap.getStart();

    @Test
    public void insert() {
        Heap.resetRoot();
        for (var i = 0; i < 100; ++i) {
            var node = create();
            node.size = i;
            Heap.insert(node);
        }

        var list = new ArrayList<Integer>();
        collect(Heap.getRoot(), list);
        assertEquals(100, list.size());
        for (var i = 0; i < 100; ++i) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    @Test
    public void delete() {
        for (var i = 0; i < 100; ++i) {
            address = Heap.getStart();
            Heap.resetRoot();
            HeapNode nodeToDelete = null;
            for (var j = 0; j < 100; ++j) {
                var node = create();
                node.size = j;
                Heap.insert(node);
                if (j == i) {
                    nodeToDelete = node;
                }
            }
            Heap.delete(nodeToDelete);
            var list = new ArrayList<Integer>();
            collect(Heap.getRoot(), list);
            assertEquals(99, list.size());
            for (var j = 0; j < i; ++j) {
                assertEquals(j, list.get(j).intValue());
            }
            for (var j = i; j < 99; ++j) {
                assertEquals(j + 1, list.get(j).intValue());
            }
        }
    }

    private void collect(HeapNode node, List<Integer> target) {
        if (node == null) {
            return;
        }
        collect(node.left, target);
        target.add(node.size);
        collect(node.right, target);
    }

    private static HeapNode create() {
        HeapNode result = address.toStructure();
        address = address.add(HeapNode.class, 1);
        return result;
    }
}
