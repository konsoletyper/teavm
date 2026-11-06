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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SequencedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.junit.TeaVMTest;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@TeaVMTest
public class LinkedHashMapTest {

    private LinkedHashMap<Object, Object> hm;

    private final static int hmSize = 1000;

    private static Object[] objArray;

    private static Object[] objArray2;

    {
        objArray = new Object[hmSize];
        objArray2 = new Object[hmSize];
        for (int i = 0; i < objArray.length; i++) {
            objArray[i] = i;
            objArray2[i] = objArray[i].toString();
        }
    }

    public LinkedHashMapTest() {
        hm = new LinkedHashMap<>();
        for (int i = 0; i < objArray.length; i++) {
            hm.put(objArray2[i], objArray[i]);
        }
        hm.put("test", null);
        hm.put(null, "test");
    }

    @Test
    public void test_Constructor() {
        new MapTest2Support(new LinkedHashMap<>()).runTest();

        var hm2 = new LinkedHashMap<>();
        assertEquals(0, hm2.size(), "Created incorrect LinkedHashMap");
    }

    @Test
    public void test_ConstructorI() {
        var hm2 = new LinkedHashMap<>(5);
        assertEquals(0, hm2.size(), "Created incorrect LinkedHashMap");
        try {
            new LinkedHashMap<>(-1);
            fail("Failed to throw IllegalArgumentException for initial capacity < 0");
        } catch (IllegalArgumentException e) {
            // as expected
        }

        var empty = new LinkedHashMap<>(0);
        assertNull(empty.get("nothing"), "Empty LinkedHashMap access");
        empty.put("something", "here");

        assertSame("here", empty.get("something"), "cannot get element");
    }

    @Test
    public void test_ConstructorIF() {
        // Test for method java.util.LinkedHashMap(int, float)
        var hm2 = new LinkedHashMap<>(5, (float) 0.5);
        assertEquals(0, hm2.size(), "Created incorrect LinkedHashMap");
        try {
            new LinkedHashMap<>(0, 0);
            fail("Failed to throw IllegalArgumentException for initial load factor <= 0");
        } catch (IllegalArgumentException e) {
            // as expected
        }
        var empty = new LinkedHashMap<String, String>(0, 0.75f);
        assertNull(empty.get("nothing"), "Empty hashtable access");
        empty.put("something", "here");

        assertSame("here", empty.get("something"), "cannot get element");
    }

    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.LinkedHashMap(java.util.Map)
        var myMap = new TreeMap<>();
        for (int counter = 0; counter < hmSize; counter++) {
            myMap.put(objArray2[counter], objArray[counter]);
        }
        var hm2 = new LinkedHashMap<>(myMap);
        for (int counter = 0; counter < hmSize; counter++) {
            assertSame(hm.get(objArray2[counter]), hm2.get(objArray2[counter]),
                    "Failed to construct correct LinkedHashMap");
        }
    }

    @Test
    public void test_getLjava_lang_Object() {
        assertNull(hm.get("T"), "Get returned non-null for non existent key");
        hm.put("T", "HELLO");
        assertEquals("HELLO", hm.get("T"), "Get returned incorrect value for existing key");

        var m = new LinkedHashMap<Object, String>();
        m.put(null, "test");
        assertEquals("test", m.get(null), "Failed with null key");
        assertNull(m.get(0), "Failed with missing key matching null hash");
    }

    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        hm.put("KEY", "VALUE");
        assertEquals("VALUE", hm.get("KEY"), "Failed to install key/value pair");

        var m = new LinkedHashMap<Number, String>();
        m.put((short) 0, "short");
        m.put(null, "test");
        m.put(0, "int");
        assertEquals("short", m.get((short) 0), "Failed adding to bucket containing null");
        assertEquals("int", m.get(0), "Failed adding to bucket containing null2");
    }

    @Test
    public void test_putAllLjava_util_Map() {
        var hm2 = new LinkedHashMap<>();
        hm2.putAll(hm);
        for (int i = 0; i < 1000; i++) {
            assertEquals(hm2.get(String.valueOf(i)), i, "Failed to clear all elements");
        }
    }

    @Test
    public void test_entrySet() {
        assertEquals(hm.size(), hm.entrySet().size(), "Returned set of incorrect size");
        for (var m : hm.entrySet()) {
            assertTrue(hm.containsKey(m.getKey()) && hm.containsValue(m.getValue()), "Returned incorrect entry set");
        }
    }

    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.LinkedHashMap.keySet()
        var s = hm.keySet();
        assertEquals(s.size(), hm.size(), "Returned set of incorrect size()");
        for (int i = 0; i < objArray.length; i++) {
            assertTrue(s.contains(objArray[i].toString()), "Returned set does not contain all keys");
        }

        var m = new LinkedHashMap<Object, String>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertTrue(m.keySet().contains(null), "Failed with null key");
        assertNull(m.keySet().iterator().next(), "Failed with null key");

        var map = new LinkedHashMap<Integer, String>(101);
        map.put(1, "1");
        map.put(102, "102");
        map.put(203, "203");
        var it = map.keySet().iterator();
        var remove1 = it.next();
        it.hasNext();
        it.remove();
        var remove2 = it.next();
        it.remove();
        var list = new ArrayList<>(Arrays.asList(1, 102, 203));
        list.remove(remove1);
        list.remove(remove2);
        assertEquals(it.next(), list.get(0), "Wrong result");
        assertEquals(1, map.size(), "Wrong size");
        assertEquals(map.keySet().iterator().next(), list.get(0), "Wrong contents");

        var map2 = new LinkedHashMap<Integer, String>(101);
        map2.put(1, "1");
        map2.put(4, "4");
        var it2 = map2.keySet().iterator();
        var remove3 = it2.next();
        Integer next;
        if (remove3.intValue() == 1) {
            next = 4;
        } else {
            next = 1;
        }
        it2.hasNext();
        it2.remove();
        assertEquals(it2.next(), next, "Wrong result 2");
        assertEquals(1, map2.size(), "Wrong size 2");
        assertEquals(map2.keySet().iterator().next(), next, "Wrong contents 2");
    }

    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.LinkedHashMap.values()
        var c = hm.values();
        assertEquals(c.size(), hm.size(), "Returned collection of incorrect size()");
        for (int i = 0; i < objArray.length; i++) {
            assertTrue(c.contains(objArray[i]), "Returned collection does not contain all keys");
        }

        var myLinkedHashMap = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            myLinkedHashMap.put(objArray2[i], objArray[i]);
        }
        var values = myLinkedHashMap.values();
        values.remove(0);
        assertFalse(myLinkedHashMap.containsValue(0),
                "Removing from the values collection should remove from the original map");
    }

    @Test
    public void test_removeLjava_lang_Object() {
        int size = hm.size();
        var y = Integer.valueOf(9);
        var x = (Integer) hm.remove(y.toString());
        assertEquals(x, Integer.valueOf(9), "Remove returned incorrect value");
        assertNull(hm.get(9), "Failed to remove given key");
        assertEquals(hm.size(), size - 1, "Failed to decrement size");
        assertNull(hm.remove("LCLCLC"), "Remove of non-existent key returned non-null");

        var m = new LinkedHashMap<Object, String>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertNull(m.remove(0), "Failed with same hash as null");
        assertEquals("org/teavm/metaprogramming/test", m.remove(null), "Failed with null key");
    }

    @Test
    public void test_clear() {
        hm.clear();
        assertEquals(0, hm.size(), "Clear failed to reset size");
        for (int i = 0; i < hmSize; i++) {
            assertNull(hm.get(objArray2[i]), "Failed to clear all elements");
        }
    }

    @Test
    public void test_clone() {
        @SuppressWarnings("unchecked")
        var hm2 = (LinkedHashMap<Object, Object>) hm.clone();
        assertNotSame(hm2, hm, "Clone answered equivalent LinkedHashMap");
        for (int counter = 0; counter < hmSize; counter++) {
            assertSame(hm.get(objArray2[counter]), hm2.get(objArray2[counter]), "Clone answered unequal LinkedHashMap");
        }

        var map = new LinkedHashMap<String, String>();
        map.put("key", "value");
        // get the keySet() and values() on the original Map
        var keys = map.keySet();
        var values = map.values();
        assertEquals("value", values.iterator().next(), "values() does not work");
        assertEquals("key", keys.iterator().next(), "keySet() does not work");
        @SuppressWarnings("unchecked")
        var map2 = (AbstractMap<String, String>) map.clone();
        map2.put("key", "value2");
        var values2 = map2.values();
        assertNotSame(values2, values, "values() is identical");

        // values() and keySet() on the cloned() map should be different
        assertEquals("value2", values2.iterator().next(), "values() was not cloned");
        map2.clear();
        map2.put("key2", "value3");
        var key2 = map2.keySet();
        assertNotSame(key2, keys, "keySet() is identical");
        assertEquals("key2", key2.iterator().next(), "keySet() was not cloned");
    }

    @Test
    public void test_clone_Mock() {
        var hashMap = new MockMap();
        String value = "value a";
        hashMap.put("key", value);
        var cloneMap = (MockMap) hashMap.clone();
        assertEquals(value, cloneMap.get("key"));
        assertEquals(hashMap, cloneMap);
        assertEquals(1, cloneMap.num);

        hashMap.put("key", "value b");
        assertNotEquals(hashMap, cloneMap);
    }

    static class MockMap extends LinkedHashMap<String, String> {
        int num;

        @Override
        public String put(String k, String v) {
            num++;
            return super.put(k, v);
        }

        @Override
        protected boolean removeEldestEntry(Entry<String, String> e) {
            return num > 1;
        }
    }

    @Test
    public void test_containsKeyLjava_lang_Object() {
        assertTrue(hm.containsKey(String.valueOf(876)), "Returned false for valid key");
        assertFalse(hm.containsKey("KKDKDKD"), "Returned true for invalid key");

        var m = new LinkedHashMap<Object, String>();
        m.put(null, "test");
        assertTrue(m.containsKey(null), "Failed with null key");
        assertFalse(m.containsKey(0), "Failed with missing key matching null hash");
    }

    @Test
    public void test_containsValueLjava_lang_Object() {
        assertTrue(hm.containsValue(875), "Returned false for valid value");
        assertFalse(hm.containsValue(-9), "Returned true for invalid valie");
    }

    @Test
    public void test_isEmpty() {
        assertTrue(new LinkedHashMap<>().isEmpty(), "Returned false for new map");
        assertFalse(hm.isEmpty(), "Returned true for non-empty");
    }

    @Test
    public void test_size() {
        assertEquals(hm.size(), objArray.length + 2, "Returned incorrect size");
    }

    @Test
    public void test_ordered_entrySet() {
        int i;
        int sz = 100;
        var lhm = new LinkedHashMap<Integer, String>();
        for (i = 0; i < sz; i++) {
            var ii = Integer.valueOf(i);
            lhm.put(ii, ii.toString());
        }

        assertEquals(lhm.size(), lhm.entrySet().size(), "Returned set of incorrect size 1");
        i = 0;
        for (var m : lhm.entrySet()) {
            var jj = m.getKey();
            assertEquals(jj.intValue(), i++, "Returned incorrect entry set 1");
        }

        var lruhm = new LinkedHashMap<Integer, String>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            var ii = Integer.valueOf(i);
            lruhm.put(ii, ii.toString());
        }

        var s3 = lruhm.entrySet();
        var it3 = s3.iterator();
        assertEquals(lruhm.size(), s3.size(), "Returned set of incorrect size 2");
        for (i = 0; i < sz && it3.hasNext(); i++) {
            var m = it3.next();
            assertEquals(m.getKey().intValue(), i, "Returned incorrect entry set 2");
        }

        /* fetch the even numbered entries to affect traversal order */
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            var ii = lruhm.get(i);
            p = p + Integer.parseInt(ii);
        }
        assertEquals(2450, p, "invalid sum of even numbers");

        var s2 = lruhm.entrySet();
        var it2 = s2.iterator();
        assertEquals(lruhm.size(), s2.size(), "Returned set of incorrect size 3");
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            var m = it2.next();
            assertEquals(m.getKey().intValue(), i, "Returned incorrect entry set 3");
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            var m = it2.next();
            assertEquals(m.getKey().intValue(), i, "Returned incorrect entry set 4");
        }
        assertFalse(it2.hasNext(), "Entries left to iterate on");
    }

    @Test
    public void test_ordered_keySet() {
        int i;
        int sz = 100;
        var lhm = new LinkedHashMap<Integer, String>();
        for (i = 0; i < sz; i++) {
            var ii = Integer.valueOf(i);
            lhm.put(ii, ii.toString());
        }

        var s1 = lhm.keySet();
        var it1 = s1.iterator();
        assertEquals(lhm.size(), s1.size(), "Returned set of incorrect size");
        for (i = 0; it1.hasNext(); i++) {
            var jj = it1.next();
            assertEquals(jj.intValue(), i, "Returned incorrect entry set");
        }

        var lruhm = new LinkedHashMap<Integer, String>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            var ii = Integer.valueOf(i);
            lruhm.put(ii, ii.toString());
        }

        var s3 = lruhm.keySet();
        var it3 = s3.iterator();
        assertEquals(lruhm.size(), s3.size(), "Returned set of incorrect size");
        for (i = 0; i < sz && it3.hasNext(); i++) {
            Integer jj = it3.next();
            assertEquals(jj.intValue(), i, "Returned incorrect entry set");
        }

        /* fetch the even numbered entries to affect traversal order */
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            var ii = lruhm.get(i);
            p = p + Integer.parseInt(ii);
        }
        assertEquals(2450, p, "invalid sum of even numbers");

        var s2 = lruhm.keySet();
        var it2 = s2.iterator();
        assertEquals(lruhm.size(), s2.size(), "Returned set of incorrect size");
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            var jj = it2.next();
            assertEquals(jj.intValue(), i, "Returned incorrect entry set");
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            var jj = it2.next();
            assertEquals(jj.intValue(), i, "Returned incorrect entry set");
        }
        assertFalse(it2.hasNext(), "Entries left to iterate on");
    }

    @Test
    public void test_ordered_values() {
        int i;
        int sz = 100;
        var lhm = new LinkedHashMap<Integer, Integer>();
        for (i = 0; i < sz; i++) {
            lhm.put(i, i * 2);
        }

        var s1 = lhm.values();
        var it1 = s1.iterator();
        assertEquals(lhm.size(), s1.size(), "Returned set of incorrect size 1");
        for (i = 0; it1.hasNext(); i++) {
            var jj = it1.next();
            assertEquals(jj.intValue(), i * 2, "Returned incorrect entry set 1");
        }

        var lruhm = new LinkedHashMap<Integer, Integer>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            lruhm.put(i, i * 2);
        }

        var s3 = lruhm.values();
        var it3 = s3.iterator();
        assertEquals(lruhm.size(), s3.size(), "Returned set of incorrect size");
        for (i = 0; i < sz && it3.hasNext(); i++) {
            var jj = it3.next();
            assertEquals(jj.intValue(), i * 2, "Returned incorrect entry set");
        }

        // fetch the even numbered entries to affect traversal order
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            var ii = lruhm.get(i);
            p = p + ii.intValue();
        }
        assertEquals(2450 * 2, p, "invalid sum of even numbers");

        var s2 = lruhm.values();
        var it2 = s2.iterator();
        assertEquals(lruhm.size(), s2.size(), "Returned set of incorrect size");
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            var jj = it2.next();
            assertEquals(jj.intValue(), i * 2, "Returned incorrect entry set");
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            var jj = it2.next();
            assertEquals(jj.intValue(), i * 2, "Returned incorrect entry set");
        }
        assertFalse(it2.hasNext(), "Entries left to iterate on");
    }

    @Test
    public void test_to_String() {
        var lhm = new LinkedHashMap<>();
        lhm.put("A", lhm);
        lhm.put("B", "C");

        assertEquals("{A=(this Map), B=C}", lhm.toString());
        assertEquals("{B=C, A={A=(this Map), B=C}}", lhm.reversed().toString());
        assertEquals("{}", new LinkedHashMap<>().toString());
    }

    private static final List<Integer> BASE_LIST = Arrays.asList(1, 6, 2, 5, 3, 4);

    private SequencedMap<Integer, String> generateMap() {
        return BASE_LIST.stream().collect(Collectors.toMap(Function.identity(), i -> Integer.toString(i),
                (a, b) -> a, LinkedHashMap::new));
    }

    private SequencedMap<Integer, String> generateAccessOrderMap() {
        return BASE_LIST.stream().collect(Collectors.toMap(Function.identity(), i -> Integer.toString(i),
                (a, b) -> a, () -> new LinkedHashMap<>(16, 0.75f, true)));
    }

    @Test
    public void testSequencedMap() {
        var map = generateMap();
        assertEquals(Map.entry(1, "1"), map.pollFirstEntry());
        assertArrayEquals(new Integer[] { 6, 2, 5, 3, 4 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(4, "4"), map.pollLastEntry());
        assertArrayEquals(new Integer[] { 6, 2, 5, 3 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(3, "3"), map.pollLastEntry());
        assertArrayEquals(new Integer[] { 6, 2, 5 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(6, "6"), map.firstEntry());
        assertEquals(Map.entry(5, "5"), map.lastEntry());
        map.putFirst(1, "1");
        map.put(7, "7");
        map.putLast(3, "3");
        assertArrayEquals(new Integer[] { 1, 6, 2, 5, 7, 3 }, map.keySet().toArray(new Integer[0]));

        map = generateMap().reversed();
        assertEquals(Map.entry(4, "4"), map.pollFirstEntry());
        assertArrayEquals(new Integer[] { 3, 5, 2, 6, 1 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(1, "1"), map.pollLastEntry());
        assertArrayEquals(new Integer[] { 3, 5, 2, 6 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(6, "6"), map.pollLastEntry());
        assertArrayEquals(new Integer[] { 3, 5, 2 }, map.keySet().toArray(new Integer[0]));
        assertEquals(Map.entry(3, "3"), map.firstEntry());
        assertEquals(Map.entry(2, "2"), map.lastEntry());
        map.putFirst(1, "1");
        map.put(7, "7");
        map.putLast(6, "6");
        assertArrayEquals(new Integer[] { 7, 1, 3, 5, 2, 6 }, map.keySet().toArray(new Integer[0]));

        map = generateAccessOrderMap();
        map.putFirst(3, "3");
        map.put(5, "5");
        map.putLast(2, "2");
        assertArrayEquals(new Integer[] { 3, 1, 6, 4, 5, 2 }, map.keySet().toArray(new Integer[0]));
        assertArrayEquals(new Integer[] { 2, 5, 4, 6, 1, 3 }, map.reversed().keySet().toArray(new Integer[0]));

        map = generateAccessOrderMap();
        map.putFirst(1, "1");
        assertArrayEquals(new Integer[] { 1, 6, 2, 5, 3, 4 }, map.keySet().toArray(new Integer[0]));
        map.put(1, "1");
        assertArrayEquals(new Integer[] { 6, 2, 5, 3, 4, 1 }, map.keySet().toArray(new Integer[0]));
        map.putLast(6, "6");
        assertArrayEquals(new Integer[] { 2, 5, 3, 4, 1, 6 }, map.keySet().toArray(new Integer[0]));
        map.putFirst(6, "6");
        assertArrayEquals(new Integer[] { 6, 2, 5, 3, 4, 1 }, map.keySet().toArray(new Integer[0]));
        assertArrayEquals(new Integer[] { 1, 4, 3, 5, 2, 6 }, map.reversed().keySet().toArray(new Integer[0]));

        map = generateAccessOrderMap().reversed();
        assertArrayEquals(new Integer[] { 4, 3, 5, 2, 6, 1 }, map.keySet().toArray(new Integer[0]));
        map.putFirst(1, "1");
        assertArrayEquals(new Integer[] { 1, 4, 3, 5, 2, 6 }, map.keySet().toArray(new Integer[0]));
        map.put(1, "1");
        assertArrayEquals(new Integer[] { 1, 4, 3, 5, 2, 6 }, map.keySet().toArray(new Integer[0]));
        map.putLast(1, "1");
        assertArrayEquals(new Integer[] { 4, 3, 5, 2, 6, 1 }, map.keySet().toArray(new Integer[0]));
        assertArrayEquals(new Integer[] { 1, 6, 2, 5, 3, 4 }, map.reversed().keySet().toArray(new Integer[0]));
    }

    @Test
    public void testSequencedIterators() {
        SequencedMap<Integer, String> map = generateMap();
        Iterator<Integer> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(1, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(6, it.next().intValue());
        it.remove();
        assertArrayEquals(new Integer[] { 1, 2, 5, 3, 4 }, map.keySet().toArray(new Integer[0]));
        map = map.reversed();
        it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(4, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(3, it.next().intValue());
        it.remove();
        assertArrayEquals(new Integer[] { 4, 5, 2, 1 }, map.keySet().toArray(new Integer[0]));
        map = generateMap();
        Iterator<String> sit = map.values().iterator();
        assertTrue(sit.hasNext());
        assertEquals("1", sit.next());
        assertTrue(sit.hasNext());
        assertEquals("6", sit.next());
        sit.remove();
        assertArrayEquals(new String[] { "1", "2", "5", "3", "4" }, map.values().toArray(new String[0]));
        map = map.reversed();
        sit = map.values().iterator();
        assertTrue(sit.hasNext());
        assertEquals("4", sit.next());
        assertTrue(sit.hasNext());
        assertEquals("3", sit.next());
        sit.remove();
        assertArrayEquals(new String[] { "4", "5", "2", "1" }, map.values().toArray(new String[0]));
    }

    @Test
    public void testEmpty() {
        var empty = new LinkedHashMap<>();
        assertNull(empty.pollFirstEntry());
        assertNull(empty.pollLastEntry());
        assertNull(empty.firstEntry());
        assertNull(empty.lastEntry());
        try {
            empty.entrySet().iterator().next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            empty.keySet().iterator().next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            empty.values().iterator().next();
            fail();
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    @Test
    public void reinsertPutDoesNotChangeOrder() {
        var map = new LinkedHashMap<String, String>();
        map.put("a", "1");
        map.put("b", "2");
        assertArrayEquals(new String[] { "1", "2" }, map.values().toArray(new String[0]));

        map.put("a", "3");
        assertArrayEquals(new String[] { "3", "2" }, map.values().toArray(new String[0]));
    }

    @Test
    public void removesEldestEntry() {
        var map = new LinkedHashMap<String, String>() {
            @Override
            protected boolean removeEldestEntry(Entry<String, String> eldest) {
                return size() > 3;
            }
        };
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        assertArrayEquals(new String[] { "1", "2", "3" }, map.values().toArray(new String[0]));

        map.put("c", "4");
        assertArrayEquals(new String[] { "1", "2", "4" }, map.values().toArray(new String[0]));

        map.put("d", "5");
        assertArrayEquals(new String[] { "2", "4", "5" }, map.values().toArray(new String[0]));

        map.put("a", "6");
        assertArrayEquals(new String[] { "4", "5", "6" }, map.values().toArray(new String[0]));
    }
}
