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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class EnumSetTest {
    @Test
    public void emptyCreated() {
        EnumSet<L> set = EnumSet.noneOf(L.class);
        assertEquals(0, set.size(), "Size");
        assertFalse(set.iterator().hasNext(), "Iterator.hasNext must return false");
        assertFalse(set.contains(L.E1), "Does not contain E1");
        assertFalse(set.contains(L.E36), "Does not contain E36");

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
        assertEquals(36, set.size(), "Size");
        assertTrue(set.iterator().hasNext(), "Iterator.hasNext must return true");
        assertEquals(L.E1, set.iterator().next(), "Iterator.next must return E1");
        assertTrue(set.contains(L.E1), "Contains E1");
        assertTrue(set.contains(L.E36), "Contains E36");
    }

    @Test
    public void itemAdded() {
        EnumSet<L> set = EnumSet.noneOf(L.class);
        assertTrue(set.add(L.E2), "Adding absent E2 must return true");
        assertEquals(L.E2, set.iterator().next(), "Iterator must return E2");
        assertTrue(set.contains(L.E2), "Set must contain E2");
        assertEquals(1, set.size(), "Size must be 1 after first addition");

        assertFalse(set.add(L.E2), "Adding existing E2 must return false");
        assertEquals(L.E2, set.iterator().next(), "Iterator must return E2 after repeated addition");
        assertTrue(set.contains(L.E2), "Set must contain E2 after repeated addition");
        assertEquals(1, set.size(), "Size must be 1 after repeated addition");

        assertTrue(set.add(L.E4), "Adding absent E4 must return true");
        assertTrue(set.contains(L.E4), "Set must contain E4");
        assertEquals(2, set.size(), "Size must be 2");

        assertTrue(set.add(L.E33), "Adding absent E33 must return true");
        assertTrue(set.contains(L.E33), "Set must contain E4");
        assertEquals(3, set.size(), "Size must be 3");
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
        assertNotSame(iterator, anotherIterator, "Should not be same");
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expectedd
        }
        assertTrue(iterator.hasNext(), "Should has next element:");
        assertSame(EnumFoo.a, iterator.next(), "Should be identical");
        iterator.remove();
        assertTrue(iterator.hasNext(), "Should has next element:");
        assertSame(EnumFoo.b, iterator.next(), "Should be identical");
        assertFalse(iterator.hasNext(), "Should not has next element:");
        assertFalse(iterator.hasNext(), "Should not has next element:");
        assertEquals(1, set.size(), "Size should be 1:");
        try {
            iterator.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        set = EnumSet.noneOf(EnumFoo.class);
        set.add(EnumFoo.a);
        iterator = set.iterator();
        assertEquals(EnumFoo.a, iterator.next(), "Should be equal");
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
        assertSame(EnumWithInnerClass.a, iteratorWithSubclass.next(), "Should be same");
        assertTrue(iteratorWithSubclass.hasNext(), "Should return true");
        assertSame(EnumWithInnerClass.b, iteratorWithSubclass.next(), "Should be same");
        setWithSubclass.remove(EnumWithInnerClass.c);
        assertTrue(iteratorWithSubclass.hasNext(), "Should return true");
        assertSame(EnumWithInnerClass.c, iteratorWithSubclass.next(), "Should be same");
        assertTrue(iteratorWithSubclass.hasNext(), "Should return true");
        assertSame(EnumWithInnerClass.d, iteratorWithSubclass.next(), "Should be same");
        setWithSubclass.add(EnumWithInnerClass.e);
        assertTrue(iteratorWithSubclass.hasNext(), "Should return true");
        assertSame(EnumWithInnerClass.f, iteratorWithSubclass.next(), "Should be same");
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
        assertEquals(EnumFoo.a, iterator.next(), "Should return EnumFoo.a");
        assertEquals(1, set.size(), "Size of set should be 1");
        iterator.remove();
        assertEquals(0, set.size(), "Size of set should be 0");
        assertFalse(set.contains(EnumFoo.a), "Should return false");
        set.add(EnumFoo.a);
        set.add(EnumFoo.b);
        iterator = set.iterator();
        assertEquals(EnumFoo.a, iterator.next(), "Should be equals");
        iterator.remove();
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertTrue(iterator.hasNext(), "Should have next element");
        try {
            iterator.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, set.size(), "Size of set should be 1");
        assertTrue(iterator.hasNext(), "Should have next element");
        assertEquals(EnumFoo.b, iterator.next(), "Should return EnumFoo.b");
        set.remove(EnumFoo.b);
        assertEquals(0, set.size(), "Size of set should be 0");
        iterator.remove();
        assertFalse(set.contains(EnumFoo.a), "Should return false");
        assertFalse(set.contains(EnumFoo.b), "Should return false");
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
        assertFalse(set.contains(EnumFoo.b), "Should return false");
    }

    @Test
    public void innerClassWithMethod() {
        var set = EnumSet.noneOf(EnumWithInnerClassAndMethod.class);
        set.add(EnumWithInnerClassAndMethod.A);
        assertEquals("A.foo", set.iterator().next().foo());
        set.clear();
        set.add(EnumWithInnerClassAndMethod.B);
        assertEquals("B.foo", set.iterator().next().foo());
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

    enum EnumWithInnerClassAndMethod {
        A {
            @Override
            String foo() {
                return "A.foo";
            }
        },
        B {
            @Override
            String foo() {
                return "B.foo";
            }
        };

        abstract String foo();
    }
}
