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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class EnumMapTest {
    @Test
    public void emptyCreated() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        assertEquals(0, map.size());
        assertFalse(map.entrySet().iterator().hasNext());
        assertNull(map.get(L.A));
        assertFalse(map.containsKey(L.A));
    }

    @Test
    public void createdFromOtherEnumMap() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        assertNull(map.put(L.A, "A"));

        EnumMap<L, String> otherMap = new EnumMap<>(map);
        map.clear();
        assertEquals(1, otherMap.size());
        assertEquals("A", otherMap.get(L.A));

        otherMap = new EnumMap<>((Map<L, String>) map);
        assertEquals(0, otherMap.size());
        assertEquals(null, otherMap.get(L.A));
    }

    @Test
    public void createdFromOtherMap() {
        Map<L, String> map = new HashMap<>();
        assertNull(map.put(L.A, "A"));

        EnumMap<L, String> otherMap = new EnumMap<>(map);
        map.clear();
        assertEquals(1, otherMap.size());
        assertEquals("A", otherMap.get(L.A));

        try {
            new EnumMap<>(map);
            fail("Should throw exception when creating from empty map");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void entriesAdded() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        assertNull(map.put(L.A, "A"));
        assertEquals(1, map.size());
        assertEquals("A", map.get(L.A));
        assertTrue(map.containsKey(L.A));

        assertEquals("A", map.put(L.A, "A0"));
        assertEquals(1, map.size());
        assertEquals("A0", map.get(L.A));
        assertTrue(map.containsKey(L.A));

        assertNull(map.put(L.B, "B"));
        assertEquals(2, map.size());
        assertEquals("B", map.get(L.B));
        assertTrue(map.containsKey(L.B));

        List<String> values = new ArrayList<>();
        List<L> keys = new ArrayList<>();
        for (Map.Entry<L, String> entry : map.entrySet()) {
            values.add(entry.getValue());
            keys.add(entry.getKey());
        }
        assertEquals(Arrays.asList("A0", "B"), values);
        assertEquals(Arrays.asList(L.A, L.B), keys);
    }

    @Test
    public void multipleEntriesAdded() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, "A");
        map.put(L.B, "B");

        Map<L, String> otherMap = new HashMap<>();
        otherMap.put(L.B, "B0");
        otherMap.put(L.C, "C0");
        map.putAll(otherMap);

        assertEquals(3, map.size());
        assertEquals("A", map.get(L.A));
        assertEquals("B0", map.get(L.B));
        assertEquals("C0", map.get(L.C));
    }

    @Test
    public void entriesRemoved() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, "A");
        map.put(L.B, "B");
        assertEquals(2, map.size());

        assertEquals("A", map.remove(L.A));
        assertEquals(1, map.size());
        assertNull(map.get(L.A));

        assertNull(map.remove(L.A));
        assertEquals(1, map.size());

        assertNull(map.remove("Dummy"));

        List<String> values = new ArrayList<>();
        List<L> keys = new ArrayList<>();
        for (Map.Entry<L, String> entry : map.entrySet()) {
            values.add(entry.getValue());
            keys.add(entry.getKey());
        }
        assertEquals(Arrays.asList("B"), values);
        assertEquals(Arrays.asList(L.B), keys);
    }

    @Test
    public void containsNullValue() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, null);
        assertEquals(1, map.size());
        assertNull(map.get(L.A));
        assertTrue(map.containsKey(L.A));
        assertNull(map.values().iterator().next());
        assertEquals(L.A, map.keySet().iterator().next());
    }

    @Test
    public void clearWorks() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, "A");
        map.put(L.B, "B");
        assertEquals(2, map.size());

        map.clear();
        assertEquals(0, map.size());
        assertFalse(map.entrySet().iterator().hasNext());
        assertFalse(map.containsKey(L.A));
        assertNull(map.get(L.A));
    }

    @Test
    public void iteratorReplacesValue() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, "A");
        map.put(L.B, "B");

        Iterator<Map.Entry<L, String>> iter = map.entrySet().iterator();
        assertTrue(iter.hasNext());
        Map.Entry<L, String> entry = iter.next();
        assertEquals(L.A, entry.getKey());
        assertEquals("A", entry.setValue("A0"));
        assertTrue(iter.hasNext());
        entry = iter.next();
        assertEquals(L.B, entry.getKey());
        assertEquals("B", entry.setValue("B0"));
        assertFalse(iter.hasNext());

        assertEquals("A0", map.get(L.A));
        assertEquals("B0", map.get(L.B));
    }

    @Test
    public void iteratorRemovesValue() {
        EnumMap<L, String> map = new EnumMap<>(L.class);
        map.put(L.A, "A");
        map.put(L.B, "B");

        Iterator<Map.Entry<L, String>> iter = map.entrySet().iterator();

        try {
            iter.remove();
            fail("Remove without calling next should throw exception");
        } catch (IllegalStateException e) {
            // It's expected
        }

        assertTrue(iter.hasNext());
        Map.Entry<L, String> entry = iter.next();
        assertEquals(L.A, entry.getKey());
        iter.remove();

        try {
            iter.remove();
            fail("Repeated remove should throw exception");
        } catch (IllegalStateException e) {
            // It's expected
        }

        assertTrue(iter.hasNext());
        iter.next();
        assertFalse(iter.hasNext());

        assertEquals(null, map.get(L.A));
        assertEquals("B", map.get(L.B));
        assertEquals(1, map.size());
    }

    enum L {
        A, B, C
    }
}
