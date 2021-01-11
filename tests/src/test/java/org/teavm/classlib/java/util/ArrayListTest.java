/*
 *  Copyright 2014 Alexey Andreev.
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
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ArrayListTest {
    @Test
    public void elementsAdded() {
        List<Integer> list = new ArrayList<>();
        list.add(2);
        list.add(3);
        list.add(4);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(4), list.get(2));
    }

    @Test
    public void capacityIncreased() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 150; ++i) {
            list.add(i);
        }
        assertEquals(150, list.size());
        assertEquals(Integer.valueOf(101), list.get(101));
    }

    @Test
    public void elementsInserted() {
        List<Integer> list = fillFromZeroToNine();
        list.add(5, -1);
        assertEquals(11, list.size());
        assertEquals(Integer.valueOf(-1), list.get(5));
        assertEquals(Integer.valueOf(5), list.get(6));
        assertEquals(Integer.valueOf(9), list.get(10));
    }

    @Test
    public void elementsRemoved() {
        List<Integer> list = fillFromZeroToNine();
        list.remove(5);
        assertEquals(9, list.size());
        assertEquals(Integer.valueOf(6), list.get(5));
        assertEquals(Integer.valueOf(9), list.get(8));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void concurrentModificationsRestricted() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            list.add(i);
        }
        for (Integer item : list) {
            if (item.equals(5)) {
                list.remove(5);
            }
        }
    }

    @Test
    public void manyElementsAdded() {
        List<Integer> list = fillFromZeroToNine();
        list.addAll(3, fillFromZeroToNine());
        assertEquals(20, list.size());
        assertEquals(Integer.valueOf(2), list.get(2));
        assertEquals(Integer.valueOf(0), list.get(3));
        assertEquals(Integer.valueOf(9), list.get(12));
        assertEquals(Integer.valueOf(3), list.get(13));
        assertEquals(Integer.valueOf(9), list.get(19));
    }

    @Test
    public void manyElementsRemoved() {
        List<Integer> list = fillFromZeroToNine();
        list.subList(2, 4).clear();
        assertEquals(8, list.size());
        assertEquals(Integer.valueOf(1), list.get(1));
        assertEquals(Integer.valueOf(4), list.get(2));
        assertEquals(Integer.valueOf(9), list.get(7));
    }

    @Test
    public void elementIndexFound() {
        List<Integer> list = fillFromZeroToNine();
        assertEquals(3, list.indexOf(3));
        assertEquals(-1, list.indexOf(100));
    }

    private List<Integer> fillFromZeroToNine() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            list.add(i);
        }
        return list;
    }

    @Test
    public void subListRange() {
        List<Integer> list = fillFromZeroToNine();

        List<Integer> sublist = list.subList(0, 10);
        assertEquals(10, sublist.size());

        sublist = list.subList(0, 0);
        assertEquals(0, sublist.size());

        sublist = list.subList(10, 10);
        assertEquals(0, sublist.size());

        sublist = list.subList(5, 5);
        assertEquals(0, sublist.size());

        try {
            list.subList(-1, -1);
            fail("Expected IOOBE for negative indexes");
        } catch (IndexOutOfBoundsException e) {
            // OK
        }

        try {
            list.subList(11, 11);
            fail("Expected IOOBE for indexes beyond size");
        } catch (IndexOutOfBoundsException e) {
            // OK
        }

        try {
            list.subList(-1, 11);
            fail("Expected IOOBE for indexes beyond limits");
        } catch (IndexOutOfBoundsException e) {
            // OK
        }

        try {
            list.subList(5, 4);
            fail("Expected IAE for lowerIndex > upperIndex");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void removeIf() {
        List<String> list = new ArrayList<>();
        list.add("A1");
        list.add("A2");
        list.add("B1");
        list.add("B2");

        list.removeIf(e -> e.endsWith("2"));

        assertEquals(2, list.size());
        assertEquals("A1", list.get(0));
        assertEquals("B1", list.get(1));
    }

    @Test
    public void testToString() {
        List list = new ArrayList<>();
        list.add(list);
        list.add("A");

        assertEquals("[(this Collection), A]", list.toString());
        assertEquals("[]", new ArrayList().toString());
    }
}
