/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class EnumSetTest {
    @Test
    public void emptyCreated() {
        EnumSet<L> set = EnumSet.noneOf(L.class);
        assertEquals("Size", 0, set.size());
        assertFalse("Iterator.hasNext must return false", set.iterator().hasNext());
        assertFalse("Does not contain E1", set.contains(L.E1));
        assertFalse("Does not contain E36", set.contains(L.E36));

        try {
            set.iterator().next();
            fail("Iterator expected to throw exception");
        } catch (NoSuchElementException e) {
            // OK
        }
    }

    @Test
    public void allItemsCreated() {
        EnumSet<L> set = EnumSet.allOf(L.class);
        assertEquals("Size", 36, set.size());
        assertTrue("Iterator.hasNext must return true", set.iterator().hasNext());
        assertEquals("Iterator.next must return E1", L.E1, set.iterator().next());
        assertTrue("Contains E1", set.contains(L.E1));
        assertTrue("Contains E36", set.contains(L.E36));
    }

    @Test
    public void itemAdded() {
        EnumSet<L> set = EnumSet.noneOf(L.class);
        assertTrue("Adding absent E2 must return true", set.add(L.E2));
        assertEquals("Iterator must return E2", L.E2, set.iterator().next());
        assertTrue("Set must contain E2", set.contains(L.E2));
        assertEquals("Size must be 1 after first addition", 1, set.size());

        assertFalse("Adding existing E2 must return false", set.add(L.E2));
        assertEquals("Iterator must return E2 after repeated addition", L.E2, set.iterator().next());
        assertTrue("Set must contain E2 after repeated addition", set.contains(L.E2));
        assertEquals("Size must be 1 after repeated addition", 1, set.size());

        assertTrue("Adding absent E4 must return true", set.add(L.E4));
        assertTrue("Set must contain E4", set.contains(L.E4));
        assertEquals("Size must be 2", 2, set.size());

        assertTrue("Adding absent E33 must return true", set.add(L.E33));
        assertTrue("Set must contain E4", set.contains(L.E33));
        assertEquals("Size must be 3", 3, set.size());
    }

    @Test
    public void iteratorWorks() {
        EnumSet<L> set = EnumSet.noneOf(L.class);
        set.add(L.E1);
        set.add(L.E4);
        set.add(L.E33);
        set.add(L.E2);

        List<L> items = new ArrayList<>();
        Iterator<L> iter = set.iterator();
        while (iter.hasNext()) {
            items.add(iter.next());
        }
        try {
            iter.next();
            fail("Can't call Iterator.next after entire collection got iterated");
        } catch (NoSuchElementException e) {
            // OK
        }

        assertEquals(Arrays.asList(L.E1, L.E2, L.E4, L.E33), items);

        try {
            set.iterator().remove();
            fail("Can't call Iterator.remove right after initialization");
        } catch (IllegalStateException e) {
            // OK
        }

        iter = EnumSet.copyOf(set).iterator();
        iter.next();
        iter.remove();
        try {
            iter.remove();
            fail("Can't call Iterator.remove right after previous removal");
        } catch (IllegalStateException e) {
            // OK
        }

        iter = set.iterator();
        iter.next();
        iter.remove();
        assertEquals(EnumSet.of(L.E2, L.E4, L.E33), set);
    }

    @Test
    public void removeAll() {
        EnumSet<L> original = EnumSet.of(L.E2, L.E3, L.E5, L.E8, L.E32);

        EnumSet<L> set = original.clone();
        assertTrue(set.removeAll(EnumSet.of(L.E3, L.E10, L.E32)));
        assertEquals(EnumSet.of(L.E2, L.E5, L.E8), set);

        set = original.clone();
        assertFalse(set.removeAll(EnumSet.of(L.E4, L.E33)));
        assertEquals(original, set);
    }

    @Test
    public void contains() {
        EnumSet<L> set = EnumSet.of(L.E2, L.E3, L.E5, L.E8, L.E32);
        assertFalse(set.contains(L.E1));
        assertTrue(set.contains(L.E2));
        assertTrue(set.contains(L.E3));
        assertFalse(set.contains(L.E4));
        assertTrue(set.contains(L.E5));
        assertTrue(set.contains(L.E8));
        assertFalse(set.contains(L.E31));
        assertTrue(set.contains(L.E32));
        assertFalse(set.contains(L.E33));
    }

    @Test
    public void add() {
        EnumSet<L> set = EnumSet.of(L.E2, L.E4);
        assertFalse(set.add(L.E2));
        assertTrue(set.add(L.E3));
        assertEquals(EnumSet.of(L.E2, L.E3, L.E4), set);
    }

    @Test
    public void containsAll() {
        EnumSet<L> set = EnumSet.of(L.E2, L.E3, L.E5, L.E8, L.E32);
        assertFalse(set.containsAll(EnumSet.of(L.E1)));
        assertFalse(set.containsAll(EnumSet.of(L.E1, L.E4)));
        assertTrue(set.containsAll(EnumSet.of(L.E2)));
        assertTrue(set.containsAll(EnumSet.of(L.E2, L.E5)));
        assertFalse(set.containsAll(EnumSet.of(L.E2, L.E4)));
    }

    @Test
    public void addAll() {
        EnumSet<L> set = EnumSet.of(L.E2, L.E4);

        assertTrue(set.addAll(EnumSet.of(L.E2, L.E3)));
        assertEquals(EnumSet.of(L.E2, L.E3, L.E4), set);

        assertFalse(set.addAll(EnumSet.of(L.E2, L.E4)));
        assertEquals(EnumSet.of(L.E2, L.E3, L.E4), set);

        assertTrue(set.addAll(EnumSet.of(L.E5, L.E6)));
        assertEquals(EnumSet.of(L.E2, L.E3, L.E4, L.E5, L.E6), set);
    }

    @Test
    public void retainAll() {
        EnumSet<L> original = EnumSet.of(L.E2, L.E4, L.E5);

        EnumSet<L> set = original.clone();
        assertTrue(set.retainAll(EnumSet.of(L.E2, L.E4)));
        assertEquals(EnumSet.of(L.E2, L.E4), set);

        set = original.clone();
        assertTrue(set.retainAll(EnumSet.of(L.E1, L.E2)));
        assertEquals(EnumSet.of(L.E2), set);

        set = original.clone();
        assertTrue(set.retainAll(EnumSet.of(L.E1)));
        assertEquals(EnumSet.noneOf(L.class), set);

        set = original.clone();
        assertFalse(set.retainAll(EnumSet.of(L.E2, L.E4, L.E5, L.E6)));
        assertEquals(original, set);
    }

    @Test
    public void iterator() {
        Set<EnumFoo> set = EnumSet.noneOf(EnumFoo.class);
        set.add(EnumFoo.a);
        set.add(EnumFoo.b);
        Iterator<EnumFoo> iterator = set.iterator();
        Iterator<EnumFoo> anotherIterator = set.iterator();
        assertNotSame("Should not be same", iterator, anotherIterator);
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expectedd
        }
        assertTrue("Should has next element:", iterator.hasNext());
        assertSame("Should be identical", EnumFoo.a, iterator.next());
        iterator.remove();
        assertTrue("Should has next element:", iterator.hasNext());
        assertSame("Should be identical", EnumFoo.b, iterator.next());
        assertFalse("Should not has next element:", iterator.hasNext());
        assertFalse("Should not has next element:", iterator.hasNext());
        assertEquals("Size should be 1:", 1, set.size());
        try {
            iterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        set = EnumSet.noneOf(EnumFoo.class);
        set.add(EnumFoo.a);
        iterator = set.iterator();
        assertEquals("Should be equal", EnumFoo.a, iterator.next());
        iterator.remove();
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        Set<EmptyEnum> emptySet = EnumSet.allOf(EmptyEnum.class);
        Iterator<EmptyEnum> emptyIterator = emptySet.iterator();
        try {
            emptyIterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        Set<EnumWithInnerClass> setWithSubclass = EnumSet
                .allOf(EnumWithInnerClass.class);
        setWithSubclass.remove(EnumWithInnerClass.e);
        Iterator<EnumWithInnerClass> iteratorWithSubclass = setWithSubclass
                .iterator();
        assertSame("Should be same", EnumWithInnerClass.a, iteratorWithSubclass.next());
        assertTrue("Should return true", iteratorWithSubclass.hasNext());
        assertSame("Should be same", EnumWithInnerClass.b, iteratorWithSubclass.next());
        setWithSubclass.remove(EnumWithInnerClass.c);
        assertTrue("Should return true", iteratorWithSubclass.hasNext());
        assertSame("Should be same", EnumWithInnerClass.c, iteratorWithSubclass.next());
        assertTrue("Should return true", iteratorWithSubclass.hasNext());
        assertSame("Should be same", EnumWithInnerClass.d, iteratorWithSubclass.next());
        setWithSubclass.add(EnumWithInnerClass.e);
        assertTrue("Should return true", iteratorWithSubclass.hasNext());
        assertSame("Should be same", EnumWithInnerClass.f, iteratorWithSubclass.next());
        set = EnumSet.noneOf(EnumFoo.class);
        iterator = set.iterator();
        try {
            iterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        set.add(EnumFoo.a);
        iterator = set.iterator();
        assertEquals("Should return EnumFoo.a", EnumFoo.a, iterator.next());
        assertEquals("Size of set should be 1", 1, set.size());
        iterator.remove();
        assertEquals("Size of set should be 0", 0, set.size());
        assertFalse("Should return false", set.contains(EnumFoo.a));
        set.add(EnumFoo.a);
        set.add(EnumFoo.b);
        iterator = set.iterator();
        assertEquals("Should be equals", EnumFoo.a, iterator.next());
        iterator.remove();
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertTrue("Should have next element", iterator.hasNext());
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals("Size of set should be 1", 1, set.size());
        assertTrue("Should have next element", iterator.hasNext());
        assertEquals("Should return EnumFoo.b", EnumFoo.b, iterator.next());
        set.remove(EnumFoo.b);
        assertEquals("Size of set should be 0", 0, set.size());
        iterator.remove();
        assertFalse("Should return false", set.contains(EnumFoo.a));
        assertFalse("Should return false", set.contains(EnumFoo.b));
        // test enum type with more than 64 elements
        Set<HugeEnum> hugeSet = EnumSet.noneOf(HugeEnum.class);
        hugeSet.add(HugeEnum.a);
        hugeSet.add(HugeEnum.b);
        Iterator<HugeEnum> hIterator = hugeSet.iterator();
        Iterator<HugeEnum> anotherHugeIterator = hugeSet.iterator();
        assertNotSame(hIterator, anotherHugeIterator);
        try {
            hIterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertTrue(hIterator.hasNext());
        assertSame(HugeEnum.a, hIterator.next());
        hIterator.remove();
        assertTrue(hIterator.hasNext());
        assertSame(HugeEnum.b, hIterator.next());
        assertFalse(hIterator.hasNext());
        assertFalse(hIterator.hasNext());
        assertEquals(1, hugeSet.size());
        try {
            hIterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        Set<HugeEnumWithInnerClass> hugeSetWithSubclass = EnumSet
                .allOf(HugeEnumWithInnerClass.class);
        hugeSetWithSubclass.remove(HugeEnumWithInnerClass.e);
        Iterator<HugeEnumWithInnerClass> hugeIteratorWithSubclass = hugeSetWithSubclass.iterator();
        assertSame(HugeEnumWithInnerClass.a, hugeIteratorWithSubclass.next());
        assertTrue(hugeIteratorWithSubclass.hasNext());
        assertSame(HugeEnumWithInnerClass.b, hugeIteratorWithSubclass.next());
        setWithSubclass.remove(HugeEnumWithInnerClass.c);
        assertTrue(hugeIteratorWithSubclass.hasNext());
        assertSame(HugeEnumWithInnerClass.c, hugeIteratorWithSubclass.next());
        assertTrue(hugeIteratorWithSubclass.hasNext());
        assertSame(HugeEnumWithInnerClass.d, hugeIteratorWithSubclass.next());
        hugeSetWithSubclass.add(HugeEnumWithInnerClass.e);
        assertTrue(hugeIteratorWithSubclass.hasNext());
        assertSame(HugeEnumWithInnerClass.f, hugeIteratorWithSubclass.next());
        hugeSet = EnumSet.noneOf(HugeEnum.class);
        hIterator = hugeSet.iterator();
        try {
            hIterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        hugeSet.add(HugeEnum.a);
        hIterator = hugeSet.iterator();
        assertEquals(HugeEnum.a, hIterator.next());
        assertEquals(1, hugeSet.size());
        hIterator.remove();
        assertEquals(0, hugeSet.size());
        assertFalse(hugeSet.contains(HugeEnum.a));
        hugeSet.add(HugeEnum.a);
        hugeSet.add(HugeEnum.b);
        hIterator = hugeSet.iterator();
        hIterator.next();
        hIterator.remove();
        assertTrue(hIterator.hasNext());
        try {
            hIterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, hugeSet.size());
        assertTrue(hIterator.hasNext());
        assertEquals(HugeEnum.b, hIterator.next());
        hugeSet.remove(HugeEnum.b);
        assertEquals(0, hugeSet.size());
        hIterator.remove();
        assertFalse(hugeSet.contains(HugeEnum.a));
        assertFalse("Should return false", set.contains(EnumFoo.b));
    }

    enum EnumWithInnerClass {
        a, b, c, d, e, f {
        },
    }
    enum EnumFoo {
        a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z,
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll,
    }
    enum EmptyEnum {
        // expected
    }
    enum HugeEnumWithInnerClass {
        a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z,
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll,
        mm {
        },
    }
    enum HugeEnum {
        a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z,
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm,
    }
    
    enum L {
        E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17, E18, E19, E20, E21, E22, E23,
        E24, E25, E26, E27, E28, E29, E30, E31, E32, E33, E34, E35, E36
    }
}
