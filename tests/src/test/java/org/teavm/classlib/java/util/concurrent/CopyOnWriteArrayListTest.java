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
package org.teavm.classlib.java.util.concurrent;
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes, 
 * Pat Fisher, Mike Judd. 
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class CopyOnWriteArrayListTest {
    private static final int SIZE = 20;

    private static CopyOnWriteArrayList<Object> populatedArray(int n) {
        var a = new CopyOnWriteArrayList<>();
        assertTrue(a.isEmpty());
        for (int i = 0; i < n; ++i) {
            a.add(i);
        }
        assertFalse(a.isEmpty());
        assertEquals(n, a.size());
        return a;
    }

    @Test
    public void testConstructor() {
        var a = new CopyOnWriteArrayList<>();
        assertTrue(a.isEmpty());
    }

    @Test
    public void testConstructor2() {
        var ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i) {
            ints[i] = i;
        }
        var a = new CopyOnWriteArrayList<>(ints);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(ints[i], a.get(i));
        }
    }

    @Test
    public void testConstructor3() {
        var ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i) {
            ints[i] = i;
        }
        var a = new CopyOnWriteArrayList<>(Arrays.asList(ints));
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(ints[i], a.get(i));
        }
    }

    @Test
    public void testAddAll() {
        var full = populatedArray(3);
        var v = new ArrayList<>();
        v.add(3);
        v.add(4);
        v.add(5);
        full.addAll(v);
        assertEquals(6, full.size());
    }

    @Test
    public void testAddAllAbsent() {
        var full = populatedArray(3);
        var v = new ArrayList<>();
        v.add(3);
        v.add(4);
        v.add(1); // will not add this element
        full.addAllAbsent(v);
        assertEquals(5, full.size());
    }

    @Test
    public void testAddIfAbsent() {
        var full = populatedArray(SIZE);
        full.addIfAbsent(1);
        assertEquals(SIZE, full.size());
    }

    @Test
    public void testAddIfAbsent2() {
        var full = populatedArray(SIZE);
        full.addIfAbsent(3);
        assertTrue(full.contains(3));
    }

    @Test
    public void testClear() {
        var full = populatedArray(SIZE);
        full.clear();
        assertEquals(0, full.size());
    }


    @Test
    public void testClone() {
        var l1 = populatedArray(SIZE);
        @SuppressWarnings("unchecked")
        var l2 = (CopyOnWriteArrayList<Object>) l1.clone();
        assertEquals(l1, l2);
        l1.clear();
        assertNotEquals(l1, l2);
    }

    @Test
    public void testContains() {
        var full = populatedArray(3);
        assertTrue(full.contains(1));
        assertFalse(full.contains(5));
    }

    @Test
    public void testAddIndex() {
        var full = populatedArray(3);
        full.add(0, -1);
        assertEquals(4, full.size());
        assertEquals(-1, full.get(0));
        assertEquals(0, full.get(1));

        full.add(2, -2);
        assertEquals(5, full.size());
        assertEquals(-2, full.get(2));
        assertEquals(2, full.get(4));
    }

    @Test
    public void testEquals() {
        var a = populatedArray(3);
        var b = populatedArray(3);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
        a.add(-1);
        assertNotEquals(a, b);
        assertNotEquals(b, a);
        b.add(-1);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }
    
    @Test
    public void testContainsAll() {
        var full = populatedArray(3);
        var v = new ArrayList<>();
        v.add(1);
        v.add(2);
        assertTrue(full.containsAll(v));
        v.add(6);
        assertFalse(full.containsAll(v));
    }

    @Test
    public void testGet() {
        var full = populatedArray(3);
        assertEquals(0, ((Integer) full.get(0)).intValue());
    }

    @Test
    public void testIndexOf() {
        var full = populatedArray(3);
        assertEquals(1, full.indexOf(1));
        assertEquals(-1, full.indexOf("puppies"));
    }

    @Test
    public void testIndexOf2() {
        var full = populatedArray(3);
        assertEquals(1, full.indexOf(1, 0));
        assertEquals(-1, full.indexOf(1, 2));
    }

    @Test
    public void testIsEmpty() {
        var empty = new CopyOnWriteArrayList<>();
        var full = populatedArray(SIZE);
        assertTrue(empty.isEmpty());
        assertFalse(full.isEmpty());
    }

    @Test
    public void testIterator() {
        var full = populatedArray(SIZE);
        var i = full.iterator();
        int j;
        for (j = 0; i.hasNext(); j++) {
            assertEquals(j, ((Integer) i.next()).intValue());
        }
        assertEquals(SIZE, j);
    }

    @Test
    public void testIteratorRemove() {
        var full = populatedArray(SIZE);
        var it = full.iterator();
        it.next();
        try {
            it.remove();
            shouldThrow();
        } catch (UnsupportedOperationException success) {
            // do nothing
        }
    }

    @Test
    public void testToString() {
        var full = populatedArray(3);
        var s = full.toString();
        for (int i = 0; i < 3; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    @Test
    public void testLastIndexOf1() {
        var full = populatedArray(3);
        full.add(1);
        full.add(3);
        assertEquals(3, full.lastIndexOf(1));
        assertEquals(-1, full.lastIndexOf(6));
    }

    @Test
    public void testLastIndexOf2() {
        var full = populatedArray(3);
        full.add(1);
        full.add(3);
        assertEquals(3, full.lastIndexOf(1, 4));
        assertEquals(-1, full.lastIndexOf(3, 3));
    }

    @Test
    public void testListIterator1() {
        var full = populatedArray(SIZE);
        var i = full.listIterator();
        int j;
        for (j = 0; i.hasNext(); j++) {
            assertEquals(j, ((Integer) i.next()).intValue());
        }
        assertEquals(SIZE, j);
    }

    @Test
    public void testListIterator2() {
        var full = populatedArray(3);
        var i = full.listIterator(1);
        int j;
        for (j = 0; i.hasNext(); j++) {
            assertEquals(j + 1, ((Integer) i.next()).intValue());
        }
        assertEquals(2, j);
    }

    @Test
    public void testRemove() {
        var full = populatedArray(3);
        assertEquals(2, full.remove(2));
        assertEquals(2, full.size());
    }

    @Test
    public void testRemoveAll() {
        var full = populatedArray(3);
        var v = new ArrayList<>();
        v.add(1);
        v.add(2);
        full.removeAll(v);
        assertEquals(1, full.size());
    }

    @Test
    public void testSet() {
        var full = populatedArray(3);
        assertEquals(2, full.set(2, 4));
        assertEquals(4, ((Integer) full.get(2)).intValue());
    }

    @Test
    public void testSize() {
        var empty = new CopyOnWriteArrayList();
        var full = populatedArray(SIZE);
        assertEquals(SIZE, full.size());
        assertEquals(0, empty.size());
    }

    @Test
    public void testToArray() {
        var full = populatedArray(3);
        var o = full.toArray();
        assertEquals(3, o.length);
        assertEquals(0, ((Integer) o[0]).intValue());
        assertEquals(1, ((Integer) o[1]).intValue());
        assertEquals(2, ((Integer) o[2]).intValue());
    }

    @Test
    public void testToArray2() {
        var full = populatedArray(3);
        var i = new Integer[3];
        i = full.toArray(i);
        assertEquals(3, i.length);
        assertEquals(0, i[0].intValue());
        assertEquals(1, i[1].intValue());
        assertEquals(2, i[2].intValue());
    }

    @Test
    public void testSubList() {
        var a = populatedArray(10);
        assertTrue(a.subList(1, 1).isEmpty());
        for (int j = 0; j < 9; ++j) {
            for (int i = j; i < 10; ++i) {
                var b = a.subList(j, i);
                for (int k = j; k < i; ++k) {
                    assertEquals(k, b.get(k - j));
                }
            }
        }

        var s = a.subList(2, 5);
        assertEquals(s.size(), 3);
        s.set(2, -1);
        assertEquals(a.get(4), -1);
        s.clear();
        assertEquals(a.size(), 7);
    }

    @Test
    @Ignore
    public void testToArray_ArrayStoreException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("zfasdfsdf");
            c.add("asdadasd");
            c.toArray(new Long[5]);
            shouldThrow();
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    @Test
    public void testGet1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.get(-1);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    @Test
    public void testGet2_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.add("asdad");
            c.get(100);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testSet1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.set(-1, "qwerty");
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    @Test
    public void testSet2() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.add("asdad");
            c.set(100, "qwerty");
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testAdd1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add(-1, "qwerty");
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    @Test
    public void testAdd2_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.add("asdasdasd");
            c.add(100, "qwerty");
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testRemove1_IndexOutOfBounds() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.remove(-1);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testRemove2_IndexOutOfBounds() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.add("adasdasd");
            c.remove(100);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    @Test
    public void testAddAll1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.addAll(-1, new LinkedList<>());
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    @Test
    public void testAddAll2_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.add("asdasdasd");
            c.addAll(100, new LinkedList<>());
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testListIterator1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.listIterator(-1);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testListIterator2_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("adasd");
            c.add("asdasdas");
            c.listIterator(100);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testSubList1_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.subList(-1, 100);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testSubList2_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.add("asdasd");
            c.subList(1, 100);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testSubList3_IndexOutOfBoundsException() {
        try {
            var c = new CopyOnWriteArrayList<>();
            c.subList(3, 1);
            shouldThrow();
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /**
     * fail with message "should throw exception"
     */
    private void shouldThrow() {
        fail("Should throw exception");
    }
}
