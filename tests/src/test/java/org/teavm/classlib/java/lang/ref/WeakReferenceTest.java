/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.classlib.java.lang.ref;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@EachTestCompiledSeparately
public class WeakReferenceTest {
    private Node lastNode;

    @Test
    @Ignore
    public void deref() throws InterruptedException {
        var ref = createAndTestRef(null);

        for (var i = 0; i < 100; ++i) {
            lastNode = createNodes(20);
            Thread.sleep(1);
            if (ref.get() == null) {
                break;
            }
            assertNotNull(lastNode);
        }
        assertNull(ref.get());
    }

    @Test
    @Ignore
    public void refQueue() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        var hasValue = false;
        for (var i = 0; i < 100; ++i) {
            lastNode = createNodes(20);
            Thread.sleep(1);
            var polledRef = queue.poll();
            if (polledRef != null) {
                hasValue = true;
                assertNull(ref.get());
                break;
            } else {
                assertNotNull(ref.get());
            }
            assertNotNull(lastNode);
        }
        assertTrue(hasValue);
    }

    @Test
    @Ignore
    public void queueRemove() throws InterruptedException {
        var queue = new ReferenceQueue<>();
        var ref = createAndTestRef(queue);
        var threadQueue = new ArrayBlockingQueue<>(4);
        var thread = new Thread(() -> {
            try {
                threadQueue.add(queue.remove());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        Object value = null;
        for (var i = 0; i < 100; ++i) {
            lastNode = createNodes(20);
            Thread.sleep(1);
            value = threadQueue.poll();
            if (value != null) {
                break;
            }
            assertNotNull(lastNode);
        }
        assertSame(ref, value);
    }

    private WeakReference<Object> createAndTestRef(ReferenceQueue<Object> queue) {
        var obj = new Object();
        var ref = new WeakReference<>(obj, queue);
        assertSame(obj, ref.get());
        return ref;
    }

    @Test
    public void clear() {
        var obj = new Object();
        var ref = new WeakReference<>(obj);
        assertSame(obj, ref.get());

        ref.clear();
        assertNull(ref.get());
    }

    private Node createNodes(int depth) {
        if (depth == 0) {
            return null;
        } else {
            return new Node(createNodes(depth - 1), createNodes(depth - 1));
        }
    }

    private class Node {
        Node left;
        Node right;

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
        }
    }
}
