/*
 *  Copyright 2023 ihromant.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@SuppressWarnings("SuspiciousMethodCalls")
@RunWith(TeaVMTestRunner.class)
public class TreeMapTest1 {
    private TreeMap tm() {
        TreeMap tm = new TreeMap();
        for (int i = 0; i < objArray.length; i++) {
            objArray[i] = Integer.valueOf(i);
            tm.put(objArray[i].toString(), objArray[i]);
        }
        return tm;
    }

    public static class ReversedComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            return -(((Comparable) o1).compareTo(o2));
        }

        public boolean equals(Object o1, Object o2) {
            return (((Comparable) o1).compareTo(o2)) == 0;
        }
    }

    // Regression for Harmony-1161
    public static class MockComparatorNullTolerable<T extends Comparable<T>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
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

    Object[] objArray = new Object[1000];

    /**
     * @tests java.util.TreeMap#TreeMap(java.util.Comparator)
     */
    @Test
    public void test_ConstructorLjava_util_Comparator() {
        // Test for method java.util.TreeMap(java.util.Comparator)
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        assertTrue("TreeMap answered incorrect comparator", reversedTreeMap
                .comparator() == comp);
        reversedTreeMap.put(Integer.valueOf(1).toString(), Integer.valueOf(1));
        reversedTreeMap.put(Integer.valueOf(2).toString(), Integer.valueOf(2));
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                reversedTreeMap.firstKey().equals(Integer.valueOf(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                reversedTreeMap.lastKey().equals(Integer.valueOf(1).toString()));
    }

    /**
     * @tests java.util.TreeMap#TreeMap(java.util.Map)
     */
    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.TreeMap(java.util.Map)
        TreeMap myTreeMap = new TreeMap(new HashMap(tm()));
        assertTrue("Map is incorrect size", myTreeMap.size() == objArray.length);
        for (Object element : objArray) {
            assertTrue("Map has incorrect mappings", myTreeMap.get(
                    element.toString()).equals(element));
        }
    }

    /**
     * @tests java.util.TreeMap#TreeMap(java.util.SortedMap)
     */
    @Test
    public void test_ConstructorLjava_util_SortedMap() {
        // Test for method java.util.TreeMap(java.util.SortedMap)
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        reversedTreeMap.put(Integer.valueOf(1).toString(), Integer.valueOf(1));
        reversedTreeMap.put(Integer.valueOf(2).toString(), Integer.valueOf(2));
        TreeMap anotherTreeMap = new TreeMap(reversedTreeMap);
        assertTrue("New tree map does not answer correct comparator",
                anotherTreeMap.comparator() == comp);
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                anotherTreeMap.firstKey().equals(Integer.valueOf(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                anotherTreeMap.lastKey().equals(Integer.valueOf(1).toString()));
    }

    /**
     * @tests java.util.TreeMap#clear()
     */
    @Test
    public void test_clear() {
        // Test for method void java.util.TreeMap.clear()
        TreeMap tm = tm();
        tm.clear();
        assertEquals("Cleared map returned non-zero size", 0, tm.size());
    }

    /**
     * @tests java.util.TreeMap#clone()
     */
    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.TreeMap.clone()
        TreeMap tm = tm();
        TreeMap clonedMap = (TreeMap) tm.clone();
        assertTrue("Cloned map does not equal the original map", clonedMap
                .equals(tm));
        assertTrue("Cloned map is the same reference as the original map",
                clonedMap != tm);
        for (Object element : objArray) {
            assertTrue("Cloned map contains incorrect elements", clonedMap
                    .get(element.toString()) == tm.get(element.toString()));
        }

        TreeMap map = new TreeMap();
        map.put("key", "value");
        // get the keySet() and values() on the original Map
        Set keys = map.keySet();
        Collection values = map.values();
        assertEquals("values() does not work", "value", values.iterator()
                .next());
        assertEquals("keySet() does not work", "key", keys.iterator().next());
        AbstractMap map2 = (AbstractMap) map.clone();
        map2.put("key", "value2");
        Collection values2 = map2.values();
        assertTrue("values() is identical", values2 != values);
        // values() and keySet() on the cloned() map should be different
        assertEquals("values() was not cloned", "value2", values2.iterator()
                .next());
        map2.clear();
        map2.put("key2", "value3");
        Set key2 = map2.keySet();
        assertTrue("keySet() is identical", key2 != keys);
        assertEquals("keySet() was not cloned", "key2", key2.iterator().next());
    }

    /**
     * @tests java.util.TreeMap#comparator()
     */
    @Test
    public void test_comparator() {
        // Test for method java.util.Comparator java.util.TreeMap.comparator()\
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        assertTrue("TreeMap answered incorrect comparator", reversedTreeMap
                .comparator() == comp);
        reversedTreeMap.put(Integer.valueOf(1).toString(), Integer.valueOf(1));
        reversedTreeMap.put(Integer.valueOf(2).toString(), Integer.valueOf(2));
        assertTrue("TreeMap does not use comparator (firstKey was incorrect)",
                reversedTreeMap.firstKey().equals(Integer.valueOf(2).toString()));
        assertTrue("TreeMap does not use comparator (lastKey was incorrect)",
                reversedTreeMap.lastKey().equals(Integer.valueOf(1).toString()));
    }

    /**
     * @tests java.util.TreeMap#containsKey(java.lang.Object)
     */
    @Test
    public void test_containsKeyLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsKey(java.lang.Object)
        TreeMap tm = tm();
        assertTrue("Returned false for valid key", tm.containsKey("95"));
        assertTrue("Returned true for invalid key", !tm.containsKey("XXXXX"));
    }

    /**
     * @tests java.util.TreeMap#containsValue(java.lang.Object)
     */
    @Test
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsValue(java.lang.Object)
        TreeMap tm = tm();
        assertTrue("Returned false for valid value", tm
                .containsValue(objArray[986]));
        assertTrue("Returned true for invalid value", !tm
                .containsValue(new Object()));
    }

    /**
     * @tests java.util.TreeMap#entrySet()
     */
    @Test
    public void test_entrySet() {
        // Test for method java.util.Set java.util.TreeMap.entrySet()
        TreeMap tm = tm();
        Set anEntrySet = tm.entrySet();
        Iterator entrySetIterator = anEntrySet.iterator();
        assertTrue("EntrySet is incorrect size",
                anEntrySet.size() == objArray.length);
        Map.Entry entry;
        while (entrySetIterator.hasNext()) {
            entry = (Map.Entry) entrySetIterator.next();
            assertTrue("EntrySet does not contain correct mappings", tm
                    .get(entry.getKey()) == entry.getValue());
        }
    }

    /**
     * @tests java.util.TreeMap#firstKey()
     */
    @Test
    public void test_firstKey() {
        // Test for method java.lang.Object java.util.TreeMap.firstKey()
        TreeMap tm = tm();
        assertEquals("Returned incorrect first key", "0", tm.firstKey());
    }

    /**
     * @tests java.util.TreeMap#get(java.lang.Object)
     */
    @Test
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.get(java.lang.Object)
        TreeMap tm = tm();
        Object o = new Object();
        tm.put("Hello", o);
        assertTrue("Failed to get mapping", tm.get("Hello") == o);

        // Test for the same key & same value
        tm = new TreeMap();
        Object o2 = new Object();
        Integer key1 = 1;
        Integer key2 = 2;
        assertNull(tm.put(key1, o));
        assertNull(tm.put(key2, o));
        assertEquals(2, tm.values().size());
        assertEquals(2, tm.keySet().size());
        assertSame(tm.get(key1), tm.get(key2));
        assertSame(o, tm.put(key1, o2));
        assertSame(o2, tm.get(key1));
    }

    /**
     * @tests java.util.TreeMap#headMap(java.lang.Object)
     */
    @Test
    public void test_headMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.headMap(java.lang.Object)
        TreeMap tm = tm();
        Map head = tm.headMap("100");
        assertEquals("Returned map of incorrect size", 3, head.size());
        assertTrue("Returned incorrect elements", head.containsKey("0")
                && head.containsValue(Integer.valueOf("1"))
                && head.containsKey("10"));

        // Regression for Harmony-1026
        TreeMap<Integer, Double> map = new TreeMap<Integer, Double>(
                new MockComparatorNullTolerable<Integer>());
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

        TreeMap<String, String> treemap = new TreeMap();
        SortedMap<String, String> headMap = treemap.headMap("100");
        headMap.headMap("100");

        SortedMap<Integer, Integer> intMap;
        SortedMap<Integer, Integer> sub;
        int size = 16;
        intMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        TreeMap t = new TreeMap();
        try {
            SortedMap th = t.headMap(null);
            fail("Should throw a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        size = 256;
        intMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    /**
     * @tests java.util.TreeMap#keySet()
     */
    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.TreeMap.keySet()
        TreeMap tm = tm();
        Set ks = tm.keySet();
        assertTrue("Returned set of incorrect size",
                ks.size() == objArray.length);
        for (int i = 0; i < tm.size(); i++) {
            assertTrue("Returned set is missing keys", ks.contains(Integer.valueOf(
                    i).toString()));
        }
    }

    /**
     * @tests java.util.TreeMap#lastKey()
     */
    @Test
    public void test_lastKey() {
        // Test for method java.lang.Object java.util.TreeMap.lastKey()
        TreeMap tm = tm();
        assertTrue("Returned incorrect last key", tm.lastKey().equals(
                objArray[objArray.length - 1].toString()));
        assertNotSame(objArray[objArray.length - 1].toString(), tm.lastKey());
        assertEquals(objArray[objArray.length - 2].toString(), tm
                .headMap("999").lastKey());
        assertEquals(objArray[objArray.length - 1].toString(), tm
                .tailMap("123").lastKey());
        assertEquals(objArray[objArray.length - 2].toString(), tm.subMap("99",
                "999").lastKey());
    }

    @Test
    public void test_lastKey_after_subMap() {
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put("001", "VAL001");
        tm.put("003", "VAL003");
        tm.put("002", "VAL002");
        SortedMap<String, String> sm = tm;
        String firstKey = sm.firstKey();
        String lastKey = "";
        for (int i = 1; i <= tm.size(); i++) {
            try {
                lastKey = sm.lastKey();
            } catch (NoSuchElementException excep) {
                fail("NoSuchElementException thrown when there are elements in the map");
            }
            sm = sm.subMap(firstKey, lastKey);
        }
    }

    /**
     * @tests java.util.TreeMap#put(java.lang.Object, java.lang.Object)
     */
    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.put(java.lang.Object, java.lang.Object)
        TreeMap tm = tm();
        Object o = new Object();
        tm.put("Hello", o);
        assertTrue("Failed to put mapping", tm.get("Hello") == o);

        // regression for Harmony-780
//        tm = new TreeMap();
//        assertNull(tm.put(new Object(), new Object()));
//        try {
//            tm.put(Integer.valueOf(1), new Object());
//            fail("should throw ClassCastException");
//        } catch (ClassCastException e) {
//            // expected
//        }

        tm = new TreeMap();
        assertNull(tm.put(Integer.valueOf(1), new Object()));

        try {
            tm.put(new Object(), new Object());
            fail("Should throw a ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }

        // regression for Harmony-2474
        // but RI6 changes its behavior
        // so the test changes too
        tm = new TreeMap();
        try {
            tm.remove(o);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            //expected
        }
    }

    /**
     * @tests java.util.TreeMap#putAll(java.util.Map)
     */
    @Test
    public void test_putAllLjava_util_Map() {
        // Test for method void java.util.TreeMap.putAll(java.util.Map)
        TreeMap x = new TreeMap();
        TreeMap tm = tm();
        x.putAll(tm);
        assertTrue("Map incorrect size after put", x.size() == tm.size());
        for (Object element : objArray) {
            assertTrue("Failed to put all elements", x.get(element.toString())
                    .equals(element));
        }
    }

    /**
     * @tests java.util.TreeMap#remove(java.lang.Object)
     */
    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.remove(java.lang.Object)
        TreeMap tm = tm();
        tm.remove("990");
        assertTrue("Failed to remove mapping", !tm.containsKey("990"));
    }

    /**
     * @tests java.util.TreeMap#size()
     */
    @Test
    public void test_size() {
        // Test for method int java.util.TreeMap.size()
        TreeMap tm = tm();
        assertEquals("Returned incorrect size", 1000, tm.size());
        assertEquals("Returned incorrect size", 447, tm.headMap("500").size());
        assertEquals("Returned incorrect size", 1000, tm.headMap("null").size());
        assertEquals("Returned incorrect size", 0, tm.headMap("").size());
        assertEquals("Returned incorrect size", 448, tm.headMap("500a").size());
        assertEquals("Returned incorrect size", 553, tm.tailMap("500").size());
        assertEquals("Returned incorrect size", 0, tm.tailMap("null").size());
        assertEquals("Returned incorrect size", 1000, tm.tailMap("").size());
        assertEquals("Returned incorrect size", 552, tm.tailMap("500a").size());
        assertEquals("Returned incorrect size", 111, tm.subMap("500", "600")
                .size());
        try {
            tm.subMap("null", "600");
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals("Returned incorrect size", 1000, tm.subMap("", "null")
                .size());
    }

    /**
     * @tests java.util.TreeMap#subMap(java.lang.Object, java.lang.Object)
     */
    @Test
    public void test_subMapLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.subMap(java.lang.Object, java.lang.Object)
        TreeMap tm = tm();
        SortedMap subMap = tm.subMap(objArray[100].toString(), objArray[109]
                .toString());
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
        TreeMap<String, String> treeMapWithNull = new TreeMap<String, String>(
                new MockComparatorNullTolerable<String>());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.subMap(null,
                "key1"); //$NON-NLS-1$
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$

        // Regression test for typo in lastKey method
        SortedMap<String, String> map = new TreeMap<String, String>();
        map.put("1", "one"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("2", "two"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("3", "three"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("3", map.lastKey());
        SortedMap<String, String> sub = map.subMap("1", "3"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("2", sub.lastKey()); //$NON-NLS-1$

        TreeMap t = new TreeMap();
        try {
            SortedMap th = t.subMap(null, new Object());
            fail("Should throw a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * @tests java.util.TreeMap#subMap(java.lang.Object, java.lang.Object)
     */
    @Test
    public void test_subMap_Iterator() {
        TreeMap<String, String> map = new TreeMap<String, String>();

        String[] keys = { "1", "2", "3" };
        String[] values = { "one", "two", "three" };
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        assertEquals(3, map.size());

        Map subMap = map.subMap("", "test");
        assertEquals(3, subMap.size());

        Set entrySet = subMap.entrySet();
        Iterator iter = entrySet.iterator();
        int size = 0;
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter
                    .next();
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            size++;
        }
        assertEquals(map.size(), size);

        Set<String> keySet = subMap.keySet();
        iter = keySet.iterator();
        size = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            assertTrue(map.containsKey(key));
            size++;
        }
        assertEquals(map.size(), size);
    }

    /**
     * @tests java.util.TreeMap#tailMap(java.lang.Object)
     */
    @Test
    public void test_tailMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.tailMap(java.lang.Object)
        TreeMap tm = tm();
        Map tail = tm.tailMap(objArray[900].toString());
        assertTrue("Returned map of incorrect size : " + tail.size(), tail
                .size() == (objArray.length - 900) + 9);
        for (int i = 900; i < objArray.length; i++) {
            assertTrue("Map contains incorrect entries", tail
                    .containsValue(objArray[i]));
        }

        // Regression for Harmony-1066
        assertTrue(tail instanceof Serializable);

        SortedMap<Integer, Integer> intMap;
        SortedMap<Integer, Integer> sub;
        int size = 16;
        intMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        TreeMap t = new TreeMap();
        try {
            SortedMap th = t.tailMap(null);
            fail("Should throw a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        size = 256;
        intMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < size; i++) {
            intMap.put(i, i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero", sub.size(), 0);
        assertTrue("submap should be empty", sub.isEmpty());
        try {
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }

        try {
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    /**
     * @tests java.util.TreeMap#values()
     */
    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.TreeMap.values()
        TreeMap tm = tm();
        Collection vals = tm.values();
        vals.iterator();
        assertTrue("Returned collection of incorrect size",
                vals.size() == objArray.length);
        for (Object element : objArray) {
            assertTrue("Collection contains incorrect elements", vals
                    .contains(element));
        }
        assertEquals(1000, vals.size());
        int j = 0;
        for (Iterator iter = vals.iterator(); iter.hasNext();) {
            Object element = iter.next();
            j++;
        }
        assertEquals(1000, j);

        vals = tm.descendingMap().values();
        vals.iterator();
        assertTrue("Returned collection of incorrect size",
                vals.size() == objArray.length);
        for (Object element : objArray) {
            assertTrue("Collection contains incorrect elements", vals
                    .contains(element));
        }
        assertEquals(1000, vals.size());
        j = 0;
        for (Iterator iter = vals.iterator(); iter.hasNext();) {
            Object element = iter.next();
            j++;
        }
        assertEquals(1000, j);

        TreeMap myTreeMap = new TreeMap();
        for (int i = 0; i < 100; i++) {
            myTreeMap.put(objArray[i], objArray[i]);
        }
        Collection values = myTreeMap.values();
//        new Support_UnmodifiableCollectionTest(
//                "Test Returned Collection From TreeMap.values()", values)
//                .runTest();
        values.remove(Integer.valueOf(0));
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(Integer.valueOf(0)));
        assertEquals(99, values.size());
        j = 0;
        for (Iterator iter = values.iterator(); iter.hasNext();) {
            Object element = iter.next();
            j++;
        }
        assertEquals(99, j);
    }

    /**
     * @tests java.util.TreeMap the values() method in sub maps
     */
    @Test
    public void test_subMap_values_size() {
        TreeMap myTreeMap = new TreeMap();
        Object[] objArray = IntStream.range(0, 1000).boxed().toArray();
        for (int i = 0; i < 1000; i++) {
            myTreeMap.put(i, objArray[i]);
        }
        // Test for method values() in subMaps
        Collection vals = myTreeMap.subMap(200, 400).values();
        assertTrue("Returned collection of incorrect size", vals.size() == 200);
        for (int i = 200; i < 400; i++) {
            assertTrue("Collection contains incorrect elements" + i, vals
                    .contains(objArray[i]));
        }
        assertEquals(200, vals.toArray().length);
        assertTrue(vals.remove(objArray[300]));
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(objArray[300]));
        assertTrue("Returned collection of incorrect size", vals.size() == 199);
        assertEquals(199, vals.toArray().length);

        myTreeMap.put(300, objArray[300]);
        // Test for method values() in subMaps
        vals = myTreeMap.headMap(400).values();
        assertEquals("Returned collection of incorrect size", vals.size(), 400);
        for (int i = 0; i < 400; i++) {
            assertTrue("Collection contains incorrect elements " + i, vals
                    .contains(objArray[i]));
        }
        assertEquals(400, vals.toArray().length);
        vals.remove(objArray[300]);
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(objArray[300]));
        assertTrue("Returned collection of incorrect size", vals.size() == 399);
        assertEquals(399, vals.toArray().length);

        myTreeMap.put(300, objArray[300]);
        // Test for method values() in subMaps
        vals = myTreeMap.tailMap(400).values();
        assertEquals("Returned collection of incorrect size", vals.size(), 600);
        for (int i = 400; i < 1000; i++) {
            assertTrue("Collection contains incorrect elements " + i, vals
                    .contains(objArray[i]));
        }
        assertEquals(600, vals.toArray().length);
        vals.remove(objArray[600]);
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(objArray[600]));
        assertTrue("Returned collection of incorrect size", vals.size() == 599);
        assertEquals(599, vals.toArray().length);

        myTreeMap.put(600, objArray[600]);
        // Test for method values() in subMaps
        vals = myTreeMap.descendingMap().headMap(400).values();
        assertEquals("Returned collection of incorrect size", vals.size(), 599);
        for (int i = 401; i < 1000; i++) {
            assertTrue("Collection contains incorrect elements " + i, vals
                    .contains(objArray[i]));
        }
        assertEquals(599, vals.toArray().length);
        vals.remove(objArray[600]);
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(objArray[600]));
        assertTrue("Returned collection of incorrect size", vals.size() == 598);
        assertEquals(598, vals.toArray().length);

        myTreeMap.put(600, objArray[600]);
        // Test for method values() in subMaps
        vals = myTreeMap.descendingMap().tailMap(400).values();
        assertEquals("Returned collection of incorrect size", vals.size(), 401);
        for (int i = 0; i <= 400; i++) {
            assertTrue("Collection contains incorrect elements " + i, vals
                    .contains(objArray[i]));
        }
        assertEquals(401, vals.toArray().length);
        vals.remove(objArray[300]);
        assertTrue(
                "Removing from the values collection should remove from the original map",
                !myTreeMap.containsValue(objArray[300]));
        assertTrue("Returned collection of incorrect size", vals.size() == 400);
        assertEquals(400, vals.toArray().length);
    }

    /**
     * @tests java.util.TreeMap#subMap()
     */
    @Test
    public void test_subMap_Iterator2() {
        TreeMap<String, String> map = new TreeMap<String, String>();

        String[] keys = { "1", "2", "3" };
        String[] values = { "one", "two", "three" };
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        assertEquals(3, map.size());

        Map subMap = map.subMap("", "test");
        assertEquals(3, subMap.size());

        Set entrySet = subMap.entrySet();
        Iterator iter = entrySet.iterator();
        int size = 0;
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter
                    .next();
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            size++;
        }
        assertEquals(map.size(), size);

        Set<String> keySet = subMap.keySet();
        iter = keySet.iterator();
        size = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            assertTrue(map.containsKey(key));
            size++;
        }
        assertEquals(map.size(), size);
    }

    /**
     * @tests java.util.TreeMap#SerializationTest()
     */
    // Regression for Harmony-1066
    @Test
    public void test_SubMap_Serializable() throws Exception {
        TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
        map.put(1, 2.1);
        map.put(2, 3.1);
        map.put(3, 4.5);
        map.put(7, 21.3);
        SortedMap<Integer, Double> headMap = map.headMap(3);
        assertTrue(headMap instanceof Serializable);
        assertFalse(headMap instanceof TreeMap);
        assertTrue(headMap instanceof SortedMap);

        assertFalse(headMap.entrySet() instanceof Serializable);
        assertFalse(headMap.keySet() instanceof Serializable);
        assertFalse(headMap.values() instanceof Serializable);

        // This assertion will fail on RI. This is a bug of RI.
        //SerializationTest.verifySelf(headMap);
    }

    /**
     * @tests {@link TreeMap#firstEntry()}
     */
    @Test
    public void test_firstEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint = Integer.valueOf(-1);
        Integer testint10000 = Integer.valueOf(-10000);
        Integer testint9999 = Integer.valueOf(-9999);
        assertEquals(objArray[0].toString(), tm.firstEntry().getKey());
        assertEquals(objArray[0], tm.firstEntry().getValue());
        tm.put(testint.toString(), testint);
        assertEquals(testint.toString(), tm.firstEntry().getKey());
        assertEquals(testint, tm.firstEntry().getValue());
        tm.put(testint10000.toString(), testint10000);
        assertEquals(testint.toString(), tm.firstEntry().getKey());
        assertEquals(testint, tm.firstEntry().getValue());
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint.toString(), tm.firstEntry().getKey());
        Map.Entry entry = tm.firstEntry();
        assertEquals(testint, entry.getValue());
        assertEntry(entry);
        tm.clear();
        assertNull(tm.firstEntry());
    }

    /**
     * @tests {@link TreeMap#lastEntry()
     */
    @Test
    public void test_lastEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999].toString(), tm.lastEntry().getKey());
        assertEquals(objArray[999], tm.lastEntry().getValue());
        tm.put(testint10000.toString(), testint10000);
        assertEquals(objArray[999].toString(), tm.lastEntry().getKey());
        assertEquals(objArray[999], tm.lastEntry().getValue());
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint9999.toString(), tm.lastEntry().getKey());
        Map.Entry entry = tm.lastEntry();
        assertEquals(testint9999, entry.getValue());
        assertEntry(entry);
        tm.clear();
        assertNull(tm.lastEntry());
    }

    /**
     * @tests {@link TreeMap#pollFirstEntry()
     */
    @Test
    public void test_pollFirstEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint = Integer.valueOf(-1);
        Integer testint10000 = Integer.valueOf(-10000);
        Integer testint9999 = Integer.valueOf(-9999);
        assertEquals(objArray[0].toString(), tm.pollFirstEntry().getKey());
        assertEquals(objArray[1], tm.pollFirstEntry().getValue());
        assertEquals(objArray[10], tm.pollFirstEntry().getValue());
        tm.put(testint.toString(), testint);
        tm.put(testint10000.toString(), testint10000);
        assertEquals(testint.toString(), tm.pollFirstEntry().getKey());
        assertEquals(testint10000, tm.pollFirstEntry().getValue());
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint9999.toString(), tm.pollFirstEntry().getKey());
        Map.Entry entry = tm.pollFirstEntry();
        assertEntry(entry);
        assertEquals(objArray[100], entry.getValue());
        tm.clear();
        assertNull(tm.pollFirstEntry());
    }

    /**
     * @tests {@link TreeMap#pollLastEntry()
     */
    @Test
    public void test_pollLastEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999].toString(), tm.pollLastEntry().getKey());
        assertEquals(objArray[998], tm.pollLastEntry().getValue());
        assertEquals(objArray[997], tm.pollLastEntry().getValue());
        tm.put(testint10000.toString(), testint10000);
        assertEquals(objArray[996], tm.pollLastEntry().getValue());
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint9999.toString(), tm.pollLastEntry().getKey());
        Map.Entry entry = tm.pollLastEntry();
        assertEquals(objArray[995], entry.getValue());
        assertEntry(entry);
        tm.clear();
        assertNull(tm.pollLastEntry());
    }

    /**
     * @tests {@link TreeMap#lowerEntry(Object)
     */
    @Test
    public void test_lowerEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999], tm.lowerEntry(testint9999.toString())
                .getValue());
        assertEquals(objArray[100], tm.lowerEntry(testint10000.toString())
                .getValue());
        tm.put(testint10000.toString(), testint10000);
        tm.put(testint9999.toString(), testint9999);
        assertEquals(objArray[999], tm.lowerEntry(testint9999.toString())
                .getValue());
        Map.Entry entry = tm.lowerEntry(testint10000.toString());
        assertEquals(objArray[100], entry.getValue());
        assertEntry(entry);
        try {
            tm.lowerEntry(testint10000);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.lowerEntry(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.lowerEntry(testint9999.toString()));
        assertNull(tm.lowerEntry(null));
    }

    /**
     * @tests {@link TreeMap#lowerKey(Object)
     */
    @Test
    public void test_lowerKey() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999].toString(), tm.lowerKey(testint9999
                .toString()));
        assertEquals(objArray[100].toString(), tm.lowerKey(testint10000
                .toString()));
        tm.put(testint10000.toString(), testint10000);
        tm.put(testint9999.toString(), testint9999);
        assertEquals(objArray[999].toString(), tm.lowerKey(testint9999
                .toString()));
        assertEquals(objArray[100].toString(), tm.lowerKey(testint10000
                .toString()));
        try {
            tm.lowerKey(testint10000);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.lowerKey(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.lowerKey(testint9999.toString()));
        assertNull(tm.lowerKey(null));
    }

    /**
     * @tests {@link TreeMap#floorEntry(Object)
     */
    @Test
    public void test_floorEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999], tm.floorEntry(testint9999.toString())
                .getValue());
        assertEquals(objArray[100], tm.floorEntry(testint10000.toString())
                .getValue());
        tm.put(testint10000.toString(), testint10000);
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint9999, tm.floorEntry(testint9999.toString())
                .getValue());
        Map.Entry entry = tm.floorEntry(testint10000.toString());
        assertEquals(testint10000, entry.getValue());
        assertEntry(entry);
        try {
            tm.floorEntry(testint10000);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.floorEntry(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.floorEntry(testint9999.toString()));
        assertNull(tm.floorEntry(null));
    }

    /**
     * @tests {@link TreeMap#floorKey(Object)
     */
    @Test
    public void test_floorKey() throws Exception {
        TreeMap tm = tm();
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint9999 = Integer.valueOf(9999);
        assertEquals(objArray[999].toString(), tm.floorKey(testint9999
                .toString()));
        assertEquals(objArray[100].toString(), tm.floorKey(testint10000
                .toString()));
        tm.put(testint10000.toString(), testint10000);
        tm.put(testint9999.toString(), testint9999);
        assertEquals(testint9999.toString(), tm
                .floorKey(testint9999.toString()));
        assertEquals(testint10000.toString(), tm.floorKey(testint10000
                .toString()));
        try {
            tm.floorKey(testint10000);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.floorKey(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.floorKey(testint9999.toString()));
        assertNull(tm.floorKey(null));
    }

    /**
     * @tests {@link TreeMap#ceilingEntry(Object)
     */
    @Test
    public void test_ceilingEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint100 = Integer.valueOf(100);
        Integer testint = Integer.valueOf(-1);
        assertEquals(objArray[0], tm.ceilingEntry(testint.toString())
                .getValue());
        assertEquals(objArray[100], tm.ceilingEntry(testint100.toString())
                .getValue());
        tm.put(testint.toString(), testint);
        tm.put(testint100.toString(), testint);
        assertEquals(testint, tm.ceilingEntry(testint.toString()).getValue());
        Map.Entry entry = tm.ceilingEntry(testint100.toString());
        assertEquals(testint, entry.getValue());
        assertEntry(entry);
        try {
            tm.ceilingEntry(testint100);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.ceilingEntry(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.ceilingEntry(testint.toString()));
        assertNull(tm.ceilingEntry(null));
    }

    /**
     * @tests {@link TreeMap#ceilingKey(Object)
     */
    @Test
    public void test_ceilingKey() throws Exception {
        TreeMap tm = tm();
        Integer testint100 = Integer.valueOf(100);
        Integer testint = Integer.valueOf(-1);
        assertEquals(objArray[0].toString(), tm.ceilingKey(testint.toString()));
        assertEquals(objArray[100].toString(), tm.ceilingKey(testint100
                .toString()));
        tm.put(testint.toString(), testint);
        tm.put(testint100.toString(), testint);
        assertEquals(testint.toString(), tm.ceilingKey(testint.toString()));
        assertEquals(testint100.toString(), tm
                .ceilingKey(testint100.toString()));
        try {
            tm.ceilingKey(testint100);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.ceilingKey(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.ceilingKey(testint.toString()));
        assertNull(tm.ceilingKey(null));
    }

    /**
     * @tests {@link TreeMap#higherEntry(Object)
     */
    @Test
    public void test_higherEntry() throws Exception {
        TreeMap tm = tm();
        Integer testint9999 = Integer.valueOf(9999);
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint100 = Integer.valueOf(100);
        Integer testint = Integer.valueOf(-1);
        assertEquals(objArray[0], tm.higherEntry(testint.toString()).getValue());
        assertEquals(objArray[101], tm.higherEntry(testint100.toString())
                .getValue());
        assertEquals(objArray[101], tm.higherEntry(testint10000.toString())
                .getValue());
        tm.put(testint9999.toString(), testint);
        tm.put(testint100.toString(), testint);
        tm.put(testint10000.toString(), testint);
        assertEquals(objArray[0], tm.higherEntry(testint.toString()).getValue());
        assertEquals(testint, tm.higherEntry(testint100.toString()).getValue());
        Map.Entry entry = tm.higherEntry(testint10000.toString());
        assertEquals(objArray[101], entry.getValue());
        assertEntry(entry);
        assertNull(tm.higherEntry(testint9999.toString()));
        try {
            tm.higherEntry(testint100);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.higherEntry(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.higherEntry(testint.toString()));
        assertNull(tm.higherEntry(null));
    }

    /**
     * @tests {@link TreeMap#higherKey(Object)
     */
    @Test
    public void test_higherKey() throws Exception {
        TreeMap tm = tm();
        Integer testint9999 = Integer.valueOf(9999);
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint100 = Integer.valueOf(100);
        Integer testint = Integer.valueOf(-1);
        assertEquals(objArray[0].toString(), tm.higherKey(testint.toString()));
        assertEquals(objArray[101].toString(), tm.higherKey(testint100
                .toString()));
        assertEquals(objArray[101].toString(), tm.higherKey(testint10000
                .toString()));
        tm.put(testint9999.toString(), testint);
        tm.put(testint100.toString(), testint);
        tm.put(testint10000.toString(), testint);
        assertEquals(objArray[0].toString(), tm.higherKey(testint.toString()));
        assertEquals(testint10000.toString(), tm.higherKey(testint100
                .toString()));
        assertEquals(objArray[101].toString(), tm.higherKey(testint10000
                .toString()));
        assertNull(tm.higherKey(testint9999.toString()));
        try {
            tm.higherKey(testint100);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.higherKey(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        tm.clear();
        assertNull(tm.higherKey(testint.toString()));
        assertNull(tm.higherKey(null));
    }

    @Test
    public void test_navigableKeySet() throws Exception {
        TreeMap tm = tm();
        Integer testint9999 = Integer.valueOf(9999);
        Integer testint10000 = Integer.valueOf(10000);
        Integer testint100 = Integer.valueOf(100);
        Integer testint0 = Integer.valueOf(0);
        NavigableSet set = tm.navigableKeySet();
        assertFalse(set.contains(testint9999.toString()));
        tm.put(testint9999.toString(), testint9999);
        assertTrue(set.contains(testint9999.toString()));
        tm.remove(testint9999.toString());
        assertFalse(set.contains(testint9999.toString()));
        try {
            set.add(new Object());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            set.add(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            set.addAll(null);
            fail("should throw UnsupportedOperationException");
        } catch (NullPointerException e) {
            // expected
        }
        Collection collection = new LinkedList();
        set.addAll(collection);
        try {
            collection.add(new Object());
            set.addAll(collection);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        set.remove(testint100.toString());
        assertFalse(tm.containsKey(testint100.toString()));
        assertTrue(tm.containsKey(testint0.toString()));
        Iterator iter = set.iterator();
        iter.next();
        iter.remove();
        assertFalse(tm.containsKey(testint0.toString()));
        collection.add(Integer.valueOf(200).toString());
        set.retainAll(collection);
        assertEquals(1, tm.size());
        set.removeAll(collection);
        assertEquals(0, tm.size());
        tm.put(testint10000.toString(), testint10000);
        assertEquals(1, tm.size());
        set.clear();
        assertEquals(0, tm.size());
    }

    private void assertEntry(Map.Entry entry) {
        try {
            entry.setValue(new Object());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        assertEquals((entry.getKey() == null ? 0 : entry.getKey().hashCode())
                        ^ (entry.getValue() == null ? 0 : entry.getValue().hashCode()),
                entry.hashCode());
        assertEquals(entry.toString(), entry.getKey() + "=" + entry.getValue());
    }

    /**
     * @tests java.util.TreeMap#subMap(java.lang.Object, boolean,
     *java.lang.Object, boolean)
     */
    @Test
    public void test_subMapLjava_lang_ObjectZLjava_lang_ObjectZ() {
        TreeMap tm = tm();
        // normal case
        SortedMap subMap = tm.subMap(objArray[100].toString(), true,
                objArray[109].toString(), true);
        assertEquals("subMap is of incorrect size", 10, subMap.size());
        subMap = tm.subMap(objArray[100].toString(), true, objArray[109]
                .toString(), false);
        assertEquals("subMap is of incorrect size", 9, subMap.size());
        for (int counter = 100; counter < 109; counter++) {
            assertTrue("SubMap contains incorrect elements", subMap.get(
                    objArray[counter].toString()).equals(objArray[counter]));
        }
        subMap = tm.subMap(objArray[100].toString(), false, objArray[109]
                .toString(), true);
        assertEquals("subMap is of incorrect size", 9, subMap.size());
        assertNull(subMap.get(objArray[100].toString()));

        // Exceptions
        try {
            tm.subMap(objArray[9].toString(), true, objArray[1].toString(),
                    true);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            tm.subMap(objArray[9].toString(), false, objArray[1].toString(),
                    false);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            tm.subMap(null, true, null, true);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.subMap(null, false, objArray[100], true);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.subMap(new LinkedList(), false, objArray[100], true);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }

        // use integer elements to test
        TreeMap<Integer, String> treeMapInt = new TreeMap<Integer, String>();
        assertEquals(0, treeMapInt.subMap(Integer.valueOf(-1), true,
                Integer.valueOf(100), true).size());
        for (int i = 0; i < 100; i++) {
            treeMapInt.put(Integer.valueOf(i), Integer.valueOf(i).toString());
        }
        SortedMap<Integer, String> result = treeMapInt.subMap(Integer.valueOf(-1),
                true, Integer.valueOf(100), true);
        assertEquals(100, result.size());
        result.put(Integer.valueOf(-1), Integer.valueOf(-1).toString());
        assertEquals(101, result.size());
        assertEquals(101, treeMapInt.size());
        result = treeMapInt
                .subMap(Integer.valueOf(50), true, Integer.valueOf(60), true);
        assertEquals(11, result.size());
        try {
            result.put(Integer.valueOf(-2), Integer.valueOf(-2).toString());
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals(11, result.size());
        treeMapInt.remove(Integer.valueOf(50));
        assertEquals(100, treeMapInt.size());
        assertEquals(10, result.size());
        result.remove(Integer.valueOf(60));
        assertEquals(99, treeMapInt.size());
        assertEquals(9, result.size());
        SortedMap<Integer, String> result2 = null;
        try {
            result2 = result.subMap(Integer.valueOf(-2), Integer.valueOf(100));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        result2 = result.subMap(Integer.valueOf(50), Integer.valueOf(60));
        assertEquals(9, result2.size());

        // sub map of sub map
        NavigableMap<Integer, Object> mapIntObj = new TreeMap<Integer, Object>();
        for (int i = 0; i < 10; ++i) {
            mapIntObj.put(i, new Object());
        }
        mapIntObj = mapIntObj.subMap(5, false, 9, true);
        assertEquals(4, mapIntObj.size());
        mapIntObj = mapIntObj.subMap(5, false, 9, true);
        assertEquals(4, mapIntObj.size());
        mapIntObj = mapIntObj.subMap(5, false, 6, false);
        assertEquals(0, mapIntObj.size());

        // a special comparator dealing with null key
        tm = new TreeMap<String, Integer>(new MockComparatorNullTolerable<String>());
        tm.put(new String("1st"), 1);
        tm.put(new String("2nd"), 2);
        tm.put(new String("3rd"), 3);
        String nullKey = null;
        tm.put(nullKey, -1);
        SortedMap s = tm.subMap(null, "3rd");
        assertEquals(3, s.size());
        assertTrue(s.containsValue(-1));
        assertTrue(s.containsValue(1));
        assertTrue(s.containsValue(2));
        assertTrue(s.containsKey(null));
        assertTrue(s.containsKey("1st"));
        assertTrue(s.containsKey("2nd"));
        s = tm.descendingMap();
        s = s.subMap("3rd", null);
        assertEquals(3, s.size());
        assertFalse(s.containsValue(-1));
        assertTrue(s.containsValue(1));
        assertTrue(s.containsValue(2));
        assertTrue(s.containsValue(3));
        assertFalse(s.containsKey(null));
        assertTrue(s.containsKey("1st"));
        assertTrue(s.containsKey("2nd"));
        assertTrue(s.containsKey("3rd"));
    }

    @Test
    public void test_subMap_NullTolerableComparator() {
        // Null Tolerable Comparator
        TreeMap<String, String> treeMapWithNull = new TreeMap<String, String>(
                new MockComparatorNullTolerable<String>());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.subMap(null,
                true, "key1", true); //$NON-NLS-1$

        // RI fails here
        assertEquals("Size of subMap should be 2:", 2, subMapWithNull.size()); //$NON-NLS-1$
        assertEquals("value1", subMapWithNull.get("key1"));
        assertEquals("value2", subMapWithNull.get(null));
        treeMapWithNull.put("key0", "value2");
        treeMapWithNull.put("key3", "value3");
        treeMapWithNull.put("key4", "value4");
        treeMapWithNull.put("key5", "value5");
        treeMapWithNull.put("key6", "value6");
        assertEquals("Size of subMap should be 3:", 3, subMapWithNull.size()); //$NON-NLS-1$
        subMapWithNull = treeMapWithNull.subMap(null, false, "key1", true); //$NON-NLS-1$
        assertEquals("Size of subMap should be 2:", 2, subMapWithNull.size()); //$NON-NLS-1$
    }

    /**
     * @tests java.util.TreeMap#headMap(java.lang.Object, boolea)
     */
    @Test
    public void test_headMapLjava_lang_ObjectZL() {
        TreeMap tm = tm();
        // normal case
        SortedMap subMap = tm.headMap(objArray[100].toString(), true);
        assertEquals("subMap is of incorrect size", 4, subMap.size());
        subMap = tm.headMap(objArray[109].toString(), true);
        assertEquals("subMap is of incorrect size", 13, subMap.size());
        for (int counter = 100; counter < 109; counter++) {
            assertTrue("SubMap contains incorrect elements", subMap.get(
                    objArray[counter].toString()).equals(objArray[counter]));
        }
        subMap = tm.headMap(objArray[100].toString(), false);
        assertEquals("subMap is of incorrect size", 3, subMap.size());
        assertNull(subMap.get(objArray[100].toString()));

        // Exceptions
        assertEquals(0, tm.headMap("", true).size());
        assertEquals(0, tm.headMap("", false).size());

        try {
            tm.headMap(null, true);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.headMap(null, false);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.headMap(new Object(), true);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.headMap(new Object(), false);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }

        // use integer elements to test
        TreeMap<Integer, String> treeMapInt = new TreeMap<Integer, String>();
        assertEquals(0, treeMapInt.headMap(Integer.valueOf(-1), true).size());
        for (int i = 0; i < 100; i++) {
            treeMapInt.put(Integer.valueOf(i), Integer.valueOf(i).toString());
        }
        SortedMap<Integer, String> result = treeMapInt
                .headMap(Integer.valueOf(101));
        assertEquals(100, result.size());
        try {
            result.put(Integer.valueOf(101), Integer.valueOf(101).toString());
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals(100, result.size());
        assertEquals(100, treeMapInt.size());
        result = treeMapInt.headMap(Integer.valueOf(50), true);
        assertEquals(51, result.size());
        result.put(Integer.valueOf(-1), Integer.valueOf(-1).toString());
        assertEquals(52, result.size());

        treeMapInt.remove(Integer.valueOf(40));
        assertEquals(100, treeMapInt.size());
        assertEquals(51, result.size());
        result.remove(Integer.valueOf(30));
        assertEquals(99, treeMapInt.size());
        assertEquals(50, result.size());
        SortedMap<Integer, String> result2 = null;
        try {
            result.subMap(Integer.valueOf(-2), Integer.valueOf(100));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            result.subMap(Integer.valueOf(1), Integer.valueOf(100));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        result2 = result.subMap(Integer.valueOf(-2), Integer.valueOf(48));
        assertEquals(47, result2.size());

        result2 = result.subMap(Integer.valueOf(40), Integer.valueOf(50));
        assertEquals(9, result2.size());

        // Null Tolerable Comparator
        TreeMap<String, String> treeMapWithNull = new TreeMap<String, String>(
                new MockComparatorNullTolerable<String>());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.headMap(
                null, true); //$NON-NLS-1$
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$
        assertEquals(null, subMapWithNull.get("key1"));
        assertEquals("value2", subMapWithNull.get(null));
        treeMapWithNull.put("key0", "value2");
        treeMapWithNull.put("key3", "value3");
        treeMapWithNull.put("key4", "value4");
        treeMapWithNull.put("key5", "value5");
        treeMapWithNull.put("key6", "value6");
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$
        subMapWithNull = treeMapWithNull.subMap(null, false, "key1", true); //$NON-NLS-1$
        assertEquals("Size of subMap should be 2:", 2, subMapWithNull.size()); //$NON-NLS-1$

        // head map of head map
        NavigableMap<Integer, Object> mapIntObj = new TreeMap<Integer, Object>();
        for (int i = 0; i < 10; ++i) {
            mapIntObj.put(i, new Object());
        }
        mapIntObj = mapIntObj.headMap(5, false);
        assertEquals(5, mapIntObj.size());
        mapIntObj = mapIntObj.headMap(5, false);
        assertEquals(5, mapIntObj.size());
        mapIntObj = mapIntObj.tailMap(5, false);
        assertEquals(0, mapIntObj.size());
    }

    /**
     * @tests java.util.TreeMap#tailMap(java.lang.Object, boolea)
     */
    @Test
    public void test_tailMapLjava_lang_ObjectZL() {
        TreeMap tm = tm();
        // normal case
        SortedMap subMap = tm.tailMap(objArray[100].toString(), true);
        assertEquals("subMap is of incorrect size", 997, subMap.size());
        subMap = tm.tailMap(objArray[109].toString(), true);
        assertEquals("subMap is of incorrect size", 988, subMap.size());
        for (int counter = 119; counter > 110; counter--) {
            assertTrue("SubMap contains incorrect elements", subMap.get(
                    objArray[counter].toString()).equals(objArray[counter]));
        }
        subMap = tm.tailMap(objArray[100].toString(), false);
        assertEquals("subMap is of incorrect size", 996, subMap.size());
        assertNull(subMap.get(objArray[100].toString()));

        // Exceptions
        assertEquals(1000, tm.tailMap("", true).size());
        assertEquals(1000, tm.tailMap("", false).size());

        try {
            tm.tailMap(null, true);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.tailMap(null, false);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            tm.tailMap(new Object(), true);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
        try {
            tm.tailMap(new Object(), false);
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }

        // use integer elements to test
        TreeMap<Integer, String> treeMapInt = new TreeMap<Integer, String>();
        assertEquals(0, treeMapInt.tailMap(Integer.valueOf(-1), true).size());
        for (int i = 0; i < 100; i++) {
            treeMapInt.put(Integer.valueOf(i), Integer.valueOf(i).toString());
        }
        SortedMap<Integer, String> result = treeMapInt.tailMap(Integer.valueOf(1));
        assertEquals(99, result.size());
        try {
            result.put(Integer.valueOf(-1), Integer.valueOf(-1).toString());
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals(99, result.size());
        assertEquals(100, treeMapInt.size());
        result = treeMapInt.tailMap(Integer.valueOf(50), true);
        assertEquals(50, result.size());
        result.put(Integer.valueOf(101), Integer.valueOf(101).toString());
        assertEquals(51, result.size());

        treeMapInt.remove(Integer.valueOf(60));
        assertEquals(100, treeMapInt.size());
        assertEquals(50, result.size());
        result.remove(Integer.valueOf(70));
        assertEquals(99, treeMapInt.size());
        assertEquals(49, result.size());
        SortedMap<Integer, String> result2 = null;
        try {
            result2 = result.subMap(Integer.valueOf(-2), Integer.valueOf(100));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        result2 = result.subMap(Integer.valueOf(60), Integer.valueOf(70));
        assertEquals(9, result2.size());

        // Null Tolerable Comparator
        TreeMap<String, String> treeMapWithNull = new TreeMap<String, String>(
                new MockComparatorNullTolerable<String>());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.tailMap(
                "key1", true); //$NON-NLS-1$
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$
        assertEquals("value1", subMapWithNull.get("key1"));
        assertEquals(null, subMapWithNull.get(null));
        treeMapWithNull.put("key0", "value2");
        treeMapWithNull.put("key3", "value3");
        treeMapWithNull.put("key4", "value4");
        treeMapWithNull.put("key5", "value5");
        treeMapWithNull.put("key6", "value6");
        assertEquals("Size of subMap should be 5:", 5, subMapWithNull.size()); //$NON-NLS-1$
        subMapWithNull = treeMapWithNull.subMap(null, false, "key1", true); //$NON-NLS-1$
        assertEquals("Size of subMap should be 2:", 2, subMapWithNull.size()); //$NON-NLS-1$

        // tail map of tail map
        NavigableMap<Integer, Object> mapIntObj = new TreeMap<Integer, Object>();
        for (int i = 0; i < 10; ++i) {
            mapIntObj.put(i, new Object());
        }
        mapIntObj = mapIntObj.tailMap(5, false);
        assertEquals(4, mapIntObj.size());
        mapIntObj = mapIntObj.tailMap(5, false);
        assertEquals(4, mapIntObj.size());
        mapIntObj = mapIntObj.headMap(5, false);
        assertEquals(0, mapIntObj.size());
    }

    @Test
    public void test_descendingMap_subMap() throws Exception {
        TreeMap<Integer, Object> tm = new TreeMap<Integer, Object>();
        for (int i = 0; i < 10; ++i) {
            tm.put(i, new Object());
        }
        NavigableMap<Integer, Object> descMap = tm.descendingMap();
        assertEquals(7, descMap.subMap(8, true, 1, false).size());
        assertEquals(4, descMap.headMap(6, true).size());
        assertEquals(2, descMap.tailMap(2, false).size());

        // sub map of sub map of descendingMap
        NavigableMap<Integer, Object> mapIntObj = new TreeMap<Integer, Object>();
        for (int i = 0; i < 10; ++i) {
            mapIntObj.put(i, new Object());
        }
        mapIntObj = mapIntObj.descendingMap();
        NavigableMap<Integer, Object> subMapIntObj = mapIntObj.subMap(9, true,
                5, false);
        assertEquals(4, subMapIntObj.size());
        subMapIntObj = subMapIntObj.subMap(9, true, 5, false);
        assertEquals(4, subMapIntObj.size());
        subMapIntObj = subMapIntObj.subMap(6, false, 5, false);
        assertEquals(0, subMapIntObj.size());

        subMapIntObj = mapIntObj.headMap(5, false);
        assertEquals(4, subMapIntObj.size());
        subMapIntObj = subMapIntObj.headMap(5, false);
        assertEquals(4, subMapIntObj.size());
        subMapIntObj = subMapIntObj.tailMap(5, false);
        assertEquals(0, subMapIntObj.size());

        subMapIntObj = mapIntObj.tailMap(5, false);
        assertEquals(5, subMapIntObj.size());
        subMapIntObj = subMapIntObj.tailMap(5, false);
        assertEquals(5, subMapIntObj.size());
        subMapIntObj = subMapIntObj.headMap(5, false);
        assertEquals(0, subMapIntObj.size());
    }

    /**
     * Tests equals() method.
     * Tests that no ClassCastException will be thrown in all cases.
     * Regression test for HARMONY-1639.
     */
    @Test
    public void test_equals() throws Exception {
        // comparing TreeMaps with different object types
        Map m1 = new TreeMap();
        Map m2 = new TreeMap();
        m1.put("key1", "val1");
        m1.put("key2", "val2");
        m2.put(Integer.valueOf(1), "val1");
        m2.put(Integer.valueOf(2), "val2");
        assertFalse("Maps should not be equal 1", m1.equals(m2));
        assertFalse("Maps should not be equal 2", m2.equals(m1));

        // comparing TreeMap with HashMap
        m1 = new TreeMap();
        m2 = new HashMap();
        m1.put("key", "val");
        m2.put(new Object(), "val");
        assertFalse("Maps should not be equal 3", m1.equals(m2));
        assertFalse("Maps should not be equal 4", m2.equals(m1));

        // comparing TreeMaps with not-comparable objects inside
        m1 = new TreeMap();
        try {
            m1.put(new Object(), "val1");
            fail("abc");
        } catch (ClassCastException e) {
            // ok
        } catch (Exception e) {
            //fail(e.getMessage() + " " + e.getClass());
        }
    }

    @Test
    public void test_remove_from_iterator() throws Exception {
        TreeMap tm = tm();
        Set set = tm.keySet();
        Iterator iter = set.iterator();
        iter.next();
        iter.remove();
        try {
            iter.remove();
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Tests entrySet().contains() method behaviour with respect to entries
     * with null values.
     * Regression test for HARMONY-5788.
     */
    @Test
    public void test_entrySet_contains() throws Exception {
        TreeMap master = new TreeMap<String, String>();
        TreeMap testMap = new TreeMap<String, String>();

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
        master.put("null", '0');
        entry = master.entrySet().toArray();
        assertFalse("Null-valued entry should not equal non-null-valued entry",
                testMap.entrySet().contains(entry[0]));
    }

    @Test
    public void test_iterator_next_() {
        TreeMap tm = tm();
        Map m = tm.subMap("0", "1");
        Iterator it = m.entrySet().iterator();
        assertEquals("0=0", it.next().toString());
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("should throw java.util.NoSuchElementException");
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
        }
    }

    @Test
    public void test_empty_subMap() throws Exception {
        TreeMap<Float, List<Integer>> tm = new TreeMap<Float, List<Integer>>();
        SortedMap<Float, List<Integer>> sm = tm.tailMap(1.1f);
        assertTrue(sm.values().size() == 0);
    }

    @Test
    public void test_subMap_values_1() {
        TreeMap tm = tm();
        tm = new TreeMap();
        tm.put("firstKey", "firstValue");
        tm.put("secondKey", "secondValue");
        tm.put("thirdKey", "thirdValue");
        Object firstKey = tm.firstKey();
        SortedMap subMap = ((SortedMap) tm).subMap(firstKey, firstKey);
        Iterator iter = subMap.values().iterator();
    }

    @Test
    public void test_descending_subMap_values_1() {
        TreeMap tm = tm();
        tm = new TreeMap();
        tm.put("firstKey", "firstValue");
        tm.put("secondKey", "secondValue");
        tm.put("thirdKey", "thirdValue");
        Object firstKey = tm.firstKey();
        SortedMap subMap = tm.descendingMap().subMap(firstKey, firstKey);
        Iterator iter = subMap.values().iterator();
    }
}
