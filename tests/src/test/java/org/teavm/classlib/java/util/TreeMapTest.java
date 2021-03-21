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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@SuppressWarnings({ "UnnecessaryTemporaryOnConversionToString", "SuspiciousMethodCalls" })
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TreeMapTest {

    public static class ReversedComparator implements Comparator<Object> {
        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object o1, Object o2) {
            return -(((Comparable<Object>) o1).compareTo(o2));
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object o1, Object o2) {
            return (((Comparable<Object>) o1).compareTo(o2)) == 0;
        }
    }

    // Regression for Harmony-1026
    public static class MockComparator<T extends Comparable<T>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            if (o1 == o2) {
                return 0;
            }
            if (null == o1 || null == o2) {
                return -1;
            }
            return o1.compareTo(o2);
        }
    }

    // Regression for Harmony-1161
    class MockComparatorNullTolerable implements Comparator<String> {
        @SuppressWarnings("StringEquality")
        @Override
        public int compare(String o1, String o2) {
            if (o1 == o2) {
                return 0;
            }
            if (null == o1) {
                return -1;
            }
            if (null == o2) { // comparator should be symmetric
                return 1;
            }
            return o1.compareTo(o2);
        }
    }

    private TreeMap<Object, Object> tm;

    private Object[] objArray = new Object[1000];

    public TreeMapTest() {
        tm = new TreeMap<>();
        for (int i = 0; i < objArray.length; i++) {
            Object x = new Integer(i);
            objArray[i] = x;
            tm.put(x.toString(), x);
        }
    }

    @Test
    public void test_ConstructorLjava_util_Comparator() {
        // Test for method java.util.TreeMap(java.util.Comparator)
        Comparator<Object> comp = new ReversedComparator();
        TreeMap<Object, Object> reversedTreeMap = new TreeMap<>(comp);
        assertTrue("TreeMap answered incorrect comparator", reversedTreeMap
                .comparator() == comp);
        reversedTreeMap.put(new Integer(1).toString(), new Integer(1));
        reversedTreeMap.put(new Integer(2).toString(), new Integer(2));
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                reversedTreeMap.firstKey().equals(new Integer(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                reversedTreeMap.lastKey().equals(new Integer(1).toString()));
    }

    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.TreeMap(java.util.Map)
        TreeMap<Object, Object> myTreeMap = new TreeMap<>(new HashMap<>(tm));
        assertTrue("Map is incorrect size", myTreeMap.size() == objArray.length);
        for (Object element : objArray) {
            assertTrue("Map has incorrect mappings", myTreeMap.get(element.toString()).equals(element));
        }
    }

    @Test
    public void test_ConstructorLjava_util_SortedMap() {
        // Test for method java.util.TreeMap(java.util.SortedMap)
        Comparator<Object> comp = new ReversedComparator();
        TreeMap<Object, Object> reversedTreeMap = new TreeMap<>(comp);
        reversedTreeMap.put(new Integer(1).toString(), new Integer(1));
        reversedTreeMap.put(new Integer(2).toString(), new Integer(2));
        TreeMap<Object, Object> anotherTreeMap = new TreeMap<>(reversedTreeMap);
        assertTrue("New tree map does not answer correct comparator",
                anotherTreeMap.comparator() == comp);
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                anotherTreeMap.firstKey().equals(new Integer(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                anotherTreeMap.lastKey().equals(new Integer(1).toString()));
    }

    @Test
    public void test_clear() {
        // Test for method void java.util.TreeMap.clear()
        tm.clear();
        assertEquals("Cleared map returned non-zero size", 0, tm.size());
    }

    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.TreeMap.clone()
        @SuppressWarnings("unchecked")
        TreeMap<Object, Object> clonedMap = (TreeMap<Object, Object>) tm.clone();
        assertEquals("Cloned map does not equal the original map", clonedMap, tm);
        assertNotSame("Cloned map is the same reference as the original map", clonedMap, tm);
        for (Object element : objArray) {
            assertSame("Cloned map contains incorrect elements", clonedMap.get(element.toString()),
                    tm.get(element.toString()));
        }

        TreeMap<Object, Object> map = new TreeMap<>();
        map.put("key", "value");
        // get the keySet() and values() on the original Map
        Set<Object> keys = map.keySet();
        Collection<Object> values = map.values();
        assertEquals("values() does not work", "value", values.iterator().next());
        assertEquals("keySet() does not work", "key", keys.iterator().next());
        @SuppressWarnings("unchecked")
        Map<Object, Object> map2 = (Map<Object, Object>) map.clone();
        map2.put("key", "value2");
        Collection<Object> values2 = map2.values();
        assertNotSame("values() is identical", values, values2);
        // values() and keySet() on the cloned() map should be different
        assertEquals("values() was not cloned", "value2", values2.iterator().next());
        map2.clear();
        map2.put("key2", "value3");
        Set<Object> key2 = map2.keySet();
        assertTrue("keySet() is identical", key2 != keys);
        assertEquals("keySet() was not cloned", "key2", key2.iterator().next());
    }

    @Test
    public void test_comparator() {
        // Test for method java.util.Comparator java.util.TreeMap.comparator()\
        Comparator<Object> comp = new ReversedComparator();
        TreeMap<Object, Object> reversedTreeMap = new TreeMap<>(comp);
        assertTrue("TreeMap answered incorrect comparator", reversedTreeMap.comparator() == comp);
        reversedTreeMap.put(new Integer(1).toString(), new Integer(1));
        reversedTreeMap.put(new Integer(2).toString(), new Integer(2));
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                reversedTreeMap.firstKey().equals(new Integer(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                reversedTreeMap.lastKey().equals(new Integer(1).toString()));
    }

    @Test
    public void test_containsKeyLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsKey(java.lang.Object)
        assertTrue("Returned false for valid key", tm.containsKey("95"));
        assertTrue("Returned true for invalid key", !tm.containsKey("XXXXX"));
    }

    @Test
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsValue(java.lang.Object)
        assertTrue("Returned false for valid value", tm
                .containsValue(objArray[986]));
        assertTrue("Returned true for invalid value", !tm
                .containsValue(new Object()));
    }

    @Test
    public void test_entrySet() {
        // Test for method java.util.Set java.util.TreeMap.entrySet()
        Set<Map.Entry<Object, Object>> anEntrySet = tm.entrySet();
        Iterator<Map.Entry<Object, Object>> entrySetIterator = anEntrySet.iterator();
        assertTrue("EntrySet is incorrect size", anEntrySet.size() == objArray.length);
        Map.Entry<Object, Object> entry;
        while (entrySetIterator.hasNext()) {
            entry = entrySetIterator.next();
            assertTrue("EntrySet does not contain correct mappings", tm.get(entry.getKey()) == entry.getValue());
        }
    }

    @Test
    public void test_firstKey() {
        // Test for method java.lang.Object java.util.TreeMap.firstKey()
        assertEquals("Returned incorrect first key", "0", tm.firstKey());
    }

    @Test
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.get(java.lang.Object)
        Object o = new Object();
        tm.put("Hello", o);
        assertTrue("Failed to get mapping", tm.get("Hello") == o);
    }

    @Test
    public void test_headMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.headMap(java.lang.Object)
        Map<Object, Object> head = tm.headMap("100");
        assertEquals("Returned map of incorrect size", 3, head.size());
        assertTrue("Returned incorrect elements", head.containsKey("0")
                && head.containsValue(new Integer("1"))
                && head.containsKey("10"));

        // Regression for Harmony-1026
        TreeMap<Integer, Double> map = new TreeMap<>(new MockComparator<>());
        map.put(1, 2.1);
        map.put(2, 3.1);
        map.put(3, 4.5);
        map.put(7, 21.3);
        map.put(null, null);

        SortedMap<Integer, Double> smap = map.headMap(null);
        assertEquals(0, smap.size());

        Set<Integer> keySet = smap.keySet();
        assertEquals(0, keySet.size());

        Set<Map.Entry<Integer, Double>> entrySet = smap.entrySet();
        assertEquals(0, entrySet.size());

        Collection<Double> valueCollection = smap.values();
        assertEquals(0, valueCollection.size());

        // Regression for Harmony-1066
        assertTrue(head instanceof Serializable);

        // Regression for ill-behaved collator
        Comparator<String> c = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 == null) {
                    return 0;
                }
                return o1.compareTo(o2);
            }
        };

        TreeMap<String, String> treemap = new TreeMap<>(c);
        assertEquals(0, treemap.headMap(null).size());

        treemap = new TreeMap<>();
        SortedMap<String, String> headMap = treemap.headMap("100");
        headMap.headMap("100");

        SortedMap<Integer, Integer> intMap;
        SortedMap<Integer, Integer> sub;
        int size = 16;
        intMap = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        size = 256;
        intMap = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }
    }

    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.TreeMap.keySet()
        Set<Object> ks = tm.keySet();
        assertTrue("Returned set of incorrect size", ks.size() == objArray.length);
        for (int i = 0; i < tm.size(); i++) {
            assertTrue("Returned set is missing keys", ks.contains(new Integer(i).toString()));
        }
    }

    @Test
    public void test_lastKey() {
        // Test for method java.lang.Object java.util.TreeMap.lastKey()
        assertTrue("Returned incorrect last key", tm.lastKey().equals(objArray[objArray.length - 1].toString()));
    }

    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.put(java.lang.Object, java.lang.Object)
        Object o = new Object();
        tm.put("Hello", o);
        assertTrue("Failed to put mapping", tm.get("Hello") == o);

        tm = new TreeMap<>();
        assertNull(tm.put(new Integer(1), new Object()));
    }

    @Test
    public void test_putAllLjava_util_Map() {
        // Test for method void java.util.TreeMap.putAll(java.util.Map)
        TreeMap<Object, Object> x = new TreeMap<>();
        x.putAll(tm);
        assertTrue("Map incorrect size after put", x.size() == tm.size());
        for (Object element : objArray) {
            assertTrue("Failed to put all elements", x.get(element.toString()).equals(element));
        }
    }

    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.remove(java.lang.Object)
        tm.remove("990");
        assertTrue("Failed to remove mapping", !tm.containsKey("990"));
    }

    @Test
    public void test_size() {
        // Test for method int java.util.TreeMap.size()
        assertEquals("Returned incorrect size", 1000, tm.size());
    }

    @Test
    public void test_subMapLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.subMap(java.lang.Object, java.lang.Object)
        SortedMap<Object, Object> subMap = tm.subMap(objArray[100].toString(), objArray[109].toString());
        assertEquals("subMap is of incorrect size", 9, subMap.size());
        for (int counter = 100; counter < 109; counter++) {
            assertTrue("SubMap contains incorrect elements", subMap.get(
                    objArray[counter].toString()).equals(objArray[counter]));
        }

        try {
            tm.subMap(objArray[9].toString(), objArray[1].toString());
            fail("end key less than start key should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Regression for Harmony-1161
        TreeMap<String, String> treeMapWithNull = new TreeMap<>(new MockComparatorNullTolerable());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.subMap(null,
                "key1"); //$NON-NLS-1$
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$

        // Regression test for typo in lastKey method
        SortedMap<String, String> map = new TreeMap<>();
        map.put("1", "one"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("2", "two"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("3", "three"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("3", map.lastKey());
        SortedMap<String, String> sub = map.subMap("1", "3"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("2", sub.lastKey()); //$NON-NLS-1$
    }

    @Test
    public void test_subMap_Iterator() {
        TreeMap<String, String> map = new TreeMap<>();

        String[] keys = { "1", "2", "3" };
        String[] values = { "one", "two", "three" };
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        assertEquals(3, map.size());

        Map<String, String> subMap = map.subMap("", "org/teavm/metaprogramming/test");
        assertEquals(3, subMap.size());

        int size = 0;
        for (Map.Entry<String, String> entry : subMap.entrySet()) {
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            size++;
        }
        assertEquals(map.size(), size);

        size = 0;
        for (String key : subMap.keySet()) {
            assertTrue(map.containsKey(key));
            size++;
        }
        assertEquals(map.size(), size);
    }

    @Test
    public void test_tailMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.tailMap(java.lang.Object)
        Map<Object, Object> tail = tm.tailMap(objArray[900].toString());
        assertTrue("Returned map of incorrect size : " + tail.size(), tail.size() == (objArray.length - 900) + 9);
        for (int i = 900; i < objArray.length; i++) {
            assertTrue("Map contains incorrect entries", tail.containsValue(objArray[i]));
        }

        // Regression for Harmony-1066
        assertTrue(tail instanceof Serializable);

        SortedMap<Integer, Integer> intMap;
        SortedMap<Integer, Integer> sub;
        int size = 16;
        intMap = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        size = 256;
        intMap = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (java.util.NoSuchElementException e) {
            // as expected
        }
    }

    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.TreeMap.values()
        Collection<Object> vals = tm.values();
        vals.iterator();
        assertTrue("Returned collection of incorrect size",
                vals.size() == objArray.length);
        for (Object element : objArray) {
            assertTrue("Collection contains incorrect elements", vals.contains(element));
        }

        TreeMap<Object, Object> myTreeMap = new TreeMap<>();
        for (int i = 0; i < 100; i++) {
            myTreeMap.put(objArray[i], objArray[i]);
        }
        Collection<Object> values = myTreeMap.values();
        values.remove(new Integer(0));
        assertTrue("Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(new Integer(0)));
    }

    /*
     * Tests entrySet().contains() method behaviour with respect to entries
     * with null values.
     * Regression test for HARMONY-5788.
     */
    @Test
    public void test_entrySet_contains() throws Exception {
        TreeMap<String, String> master = new TreeMap<>();
        TreeMap<String, String> testMap = new TreeMap<>();

        master.put("null", null);
        Object[] entry = master.entrySet().toArray();
        assertFalse("Empty map should not contain the null-valued entry",
                testMap.entrySet().contains(entry[0]));

        Map<String, String> submap = testMap.subMap("a", "z");
        entry = master.entrySet().toArray();
        assertFalse("Empty submap should not contain the null-valued entry",
                submap.entrySet().contains(entry[0]));

        testMap.put("null", null);
        assertTrue("entrySet().containsAll(...) should work with null values",
                testMap.entrySet().containsAll(master.entrySet()));

        master.clear();
        master.put("null", "0");
        entry = master.entrySet().toArray();
        assertFalse("Null-valued entry should not equal non-null-valued entry",
                testMap.entrySet().contains(entry[0]));
    }

    @Test
    public void mapReversed() {
        NavigableMap<Integer, String> map = createMapOfEvenNumbers();
        NavigableMap<Integer, String> reversedMap = map.descendingMap();
        assertEquals("The first key of reverse map is wrong", Integer.valueOf(998), reversedMap.firstKey());
        assertEquals("The last key of reverse map is wrong", Integer.valueOf(0), reversedMap.lastKey());
        assertTrue("Reversed map does not contain element from original map", reversedMap.containsKey(256));
        assertEquals("Reversed map is of a wrong size", 500, reversedMap.size());
        assertNull(reversedMap.get(1000));
        Iterator<Integer> keys = reversedMap.keySet().iterator();
        assertEquals("Wrong first element got from iterator", Integer.valueOf(998), keys.next());
        assertEquals("Wrong second element got from iterator", Integer.valueOf(996), keys.next());
        assertEquals("Wrong third element got from iterator", Integer.valueOf(994), keys.next());
    }

    @Test
    public void submapReversed() {
        NavigableMap<Integer, String> map = createMapOfEvenNumbers();
        NavigableMap<Integer, String> reversedMap = map.subMap(100, true, 201, true).descendingMap();
        assertEquals("The first key of  map is wrong", Integer.valueOf(200), reversedMap.firstKey());
        assertEquals("The last key of map is wrong", Integer.valueOf(100), reversedMap.lastKey());
        assertTrue("Reversed map does not contain element from original map", reversedMap.containsKey(104));
        assertEquals("Reversed map is of a wrong size", 51, reversedMap.size());
        assertNull(reversedMap.get(103));
        assertNull(reversedMap.get(256));
        Iterator<Integer> keys = reversedMap.keySet().iterator();
        assertEquals("Wrong first element got from iterator", Integer.valueOf(200), keys.next());
        assertEquals("Wrong second element got from iterator", Integer.valueOf(198), keys.next());
        assertEquals("Wrong third element got from iterator", Integer.valueOf(196), keys.next());
    }

    @Test
    public void submapOfReverseSubmapObtained() {
        NavigableMap<Integer, String> map = createMapOfEvenNumbers();
        NavigableMap<Integer, String> reversedMap = map.subMap(100, true, 901, true).descendingMap()
                .subMap(800, false, 201, false);
        assertEquals("The first key of map is wrong", Integer.valueOf(798), reversedMap.firstKey());
        assertEquals("The last key of map is wrong", Integer.valueOf(202), reversedMap.lastKey());
        assertTrue("Reversed map does not contain element from original map", reversedMap.containsKey(244));
        assertEquals("Reversed map is of a wrong size", 299, reversedMap.size());
        assertNull(reversedMap.get(225));
        assertNull(reversedMap.get(100));
        Iterator<Integer> keys = reversedMap.keySet().iterator();
        assertEquals("Wrong first element got from iterator", Integer.valueOf(798), keys.next());
        assertEquals("Wrong second element got from iterator", Integer.valueOf(796), keys.next());
        assertEquals("Wrong third element got from iterator", Integer.valueOf(794), keys.next());
    }

    @Test
    public void tailOfReverseSubmapObtained() {
        NavigableMap<Integer, String> map = createMapOfEvenNumbers();
        NavigableMap<Integer, String> reversedMap = map.subMap(100, true, 901, true).descendingMap()
                .tailMap(800, false);
        assertEquals("The first key of map is wrong", Integer.valueOf(798), reversedMap.firstKey());
        assertEquals("The last key of map is wrong", Integer.valueOf(100), reversedMap.lastKey());
        assertTrue("Reversed map does not contain element from original map", reversedMap.containsKey(144));
        assertEquals("Reversed map is of a wrong size", 350, reversedMap.size());
        assertNull(reversedMap.get(225));
        assertNull(reversedMap.get(94));
        assertNull(reversedMap.get(908));
        Iterator<Integer> keys = reversedMap.keySet().iterator();
        assertEquals("Wrong first element got from iterator", Integer.valueOf(798), keys.next());
        assertEquals("Wrong second element got from iterator", Integer.valueOf(796), keys.next());
        assertEquals("Wrong third element got from iterator", Integer.valueOf(794), keys.next());
    }

    private TreeMap<Integer, String> createMapOfEvenNumbers() {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        for (int i = 0; i < 1000; i += 2) {
            treeMap.put(i, String.valueOf(i));
        }
        return treeMap;
    }

    @Test
    public void deletesProperly() {
        TreeMap<Integer, Integer> tm = new TreeMap<>();
        for (int i = 0; i <= 100; ++i) {
            tm.put(i, i);
            assertEquals(i + 1, tm.size());
        }
        for (int i = 0; i <= 100; ++i) {
            Integer removed = tm.remove(i);
            assertEquals(Integer.valueOf(i), removed);
            assertEquals(100, tm.size());
            tm.put(i, i + 1);
            assertTrue("13 is expected to be in the map: " + i, tm.containsKey(13));
            assertTrue("99 is expected to be in the map: " + i, tm.containsKey(99));
            assertEquals(101, tm.size());
        }
    }

    @Test
    public void submap() {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        map.put(1, 1);
        map.put(3, 15);
        map.put(4, 20);
        map.put(6, 13);
        map.put(10, 119);

        assertEquals("{}", map.subMap(0, 0).toString());
        assertEquals("{}", map.subMap(7, 9).toString());
        assertEquals("{3=15, 4=20, 6=13}", map.subMap(3, 9).toString());
        assertEquals("{10=119}", map.subMap(10, 29).toString());
        assertEquals("{}", map.subMap(29, 100).toString());
    }
}
