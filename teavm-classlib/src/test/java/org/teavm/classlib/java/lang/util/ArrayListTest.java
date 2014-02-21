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
package org.teavm.classlib.java.lang.util;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
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

    private List<Integer> fillFromZeroToNine() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            list.add(i);
        }
        return list;
    }
}
