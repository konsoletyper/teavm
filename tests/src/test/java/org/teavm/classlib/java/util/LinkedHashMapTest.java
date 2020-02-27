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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@SuppressWarnings({ "UnnecessaryUnboxing", "ClassInitializerMayBeStatic", "UnnecessaryTemporaryOnConversionToString",
        "MismatchedQueryAndUpdateOfCollection", "StringEquality" })
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class LinkedHashMapTest {

    private LinkedHashMap<Object, Object> hm;

    private final static int hmSize = 1000;

    private static Object[] objArray;

    private static Object[] objArray2;

    {
        objArray = new Object[hmSize];
        objArray2 = new Object[hmSize];
        for (int i = 0; i < objArray.length; i++) {
            objArray[i] = new Integer(i);
            objArray2[i] = objArray[i].toString();
        }
    }

    public LinkedHashMapTest() {
        hm = new LinkedHashMap<>();
        for (int i = 0; i < objArray.length; i++) {
            hm.put(objArray2[i], objArray[i]);
        }
        hm.put("org/teavm/metaprogramming/test", null);
        hm.put(null, "org/teavm/metaprogramming/test");
    }

    @Test
    public void test_Constructor() {
        // Test for method java.util.LinkedHashMap()
        new MapTest2Support(new LinkedHashMap<>()).runTest();

        LinkedHashMap<Object, Object> hm2 = new LinkedHashMap<>();
        assertEquals("Created incorrect LinkedHashMap", 0, hm2.size());
    }

    @Test
    public void test_ConstructorI() {
        // Test for method java.util.LinkedHashMap(int)
        LinkedHashMap<Object, Object> hm2 = new LinkedHashMap<>(5);
        assertEquals("Created incorrect LinkedHashMap", 0, hm2.size());
        try {
            new LinkedHashMap<>(-1);
            fail("Failed to throw IllegalArgumentException for initial capacity < 0");
        } catch (IllegalArgumentException e) {
            // as expected
        }

        LinkedHashMap<Object, Object> empty = new LinkedHashMap<>(0);
        assertNull("Empty LinkedHashMap access", empty.get("nothing"));
        empty.put("something", "here");

        //CHECKSTYLE:OFF
        assertTrue("cannot get element", empty.get("something") == "here");
        //CHECKSTYLE:ON
    }

    @Test
    public void test_ConstructorIF() {
        // Test for method java.util.LinkedHashMap(int, float)
        LinkedHashMap<Object, Object> hm2 = new LinkedHashMap<>(5, (float) 0.5);
        assertEquals("Created incorrect LinkedHashMap", 0, hm2.size());
        try {
            new LinkedHashMap<>(0, 0);
            fail("Failed to throw IllegalArgumentException for initial load factor <= 0");
        } catch (IllegalArgumentException e) {
            // as expected
        }
        LinkedHashMap<String, String> empty = new LinkedHashMap<>(0, 0.75f);
        assertNull("Empty hashtable access", empty.get("nothing"));
        empty.put("something", "here");

        //CHECKSTYLE:OFF
        assertTrue("cannot get element", empty.get("something") == "here");
        // CHECKSTYLE: ON
    }

    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.LinkedHashMap(java.util.Map)
        Map<Object, Object> myMap = new TreeMap<>();
        for (int counter = 0; counter < hmSize; counter++) {
            myMap.put(objArray2[counter], objArray[counter]);
        }
        LinkedHashMap<Object, Object> hm2 = new LinkedHashMap<>(myMap);
        for (int counter = 0; counter < hmSize; counter++) {
            assertSame("Failed to construct correct LinkedHashMap", hm.get(objArray2[counter]),
                    hm2.get(objArray2[counter]));
        }
    }

    @Test
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.LinkedHashMap.get(java.lang.Object)
        assertNull("Get returned non-null for non existent key", hm.get("T"));
        hm.put("T", "HELLO");
        assertEquals("Get returned incorecct value for existing key", "HELLO", hm.get("T"));

        LinkedHashMap<Object, String> m = new LinkedHashMap<>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertEquals("Failed with null key", "org/teavm/metaprogramming/test", m.get(null));
        assertNull("Failed with missing key matching null hash", m.get(new Integer(0)));
    }

    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.LinkedHashMap.put(java.lang.Object, java.lang.Object)
        hm.put("KEY", "VALUE");
        assertEquals("Failed to install key/value pair", "VALUE", hm.get("KEY"));

        LinkedHashMap<Number, String> m = new LinkedHashMap<>();
        m.put(new Short((short) 0), "short");
        m.put(null, "org/teavm/metaprogramming/test");
        m.put(new Integer(0), "int");
        assertEquals("Failed adding to bucket containing null", "short", m.get(new Short((short) 0)));
        assertEquals("Failed adding to bucket containing null2", "int", m.get(new Integer(0)));
    }

    @Test
    public void test_putAllLjava_util_Map() {
        LinkedHashMap<Object, Object> hm2 = new LinkedHashMap<>();
        hm2.putAll(hm);
        for (int i = 0; i < 1000; i++) {
            assertTrue("Failed to clear all elements", hm2.get(new Integer(i).toString()).equals(new Integer(i)));
        }
    }

    @Test
    public void test_entrySet() {
        assertEquals("Returned set of incorrect size", hm.size(), hm.entrySet().size());
        for (Entry<Object, Object> m : hm.entrySet()) {
            assertTrue("Returned incorrect entry set", hm.containsKey(m.getKey()) && hm.containsValue(m.getValue()));
        }
    }

    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.LinkedHashMap.keySet()
        Set<Object> s = hm.keySet();
        assertEquals("Returned set of incorrect size()", s.size(), hm.size());
        for (int i = 0; i < objArray.length; i++) {
            assertTrue("Returned set does not contain all keys", s.contains(objArray[i].toString()));
        }

        LinkedHashMap<Object, String> m = new LinkedHashMap<>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertTrue("Failed with null key", m.keySet().contains(null));
        assertNull("Failed with null key", m.keySet().iterator().next());

        Map<Integer, String> map = new LinkedHashMap<>(101);
        map.put(new Integer(1), "1");
        map.put(new Integer(102), "102");
        map.put(new Integer(203), "203");
        Iterator<Integer> it = map.keySet().iterator();
        Integer remove1 = it.next();
        it.hasNext();
        it.remove();
        Integer remove2 = it.next();
        it.remove();
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(new Integer[] { 1, 102, 203 }));
        list.remove(remove1);
        list.remove(remove2);
        assertTrue("Wrong result", it.next().equals(list.get(0)));
        assertEquals("Wrong size", 1, map.size());
        assertTrue("Wrong contents", map.keySet().iterator().next().equals(list.get(0)));

        Map<Integer, String> map2 = new LinkedHashMap<>(101);
        map2.put(new Integer(1), "1");
        map2.put(new Integer(4), "4");
        Iterator<Integer> it2 = map2.keySet().iterator();
        Integer remove3 = it2.next();
        Integer next;
        if (remove3.intValue() == 1) {
            next = new Integer(4);
        } else {
            next = new Integer(1);
        }
        it2.hasNext();
        it2.remove();
        assertTrue("Wrong result 2", it2.next().equals(next));
        assertEquals("Wrong size 2", 1, map2.size());
        assertTrue("Wrong contents 2", map2.keySet().iterator().next().equals(next));
    }

    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.LinkedHashMap.values()
        Collection<Object> c = hm.values();
        assertTrue("Returned collection of incorrect size()", c.size() == hm.size());
        for (int i = 0; i < objArray.length; i++) {
            assertTrue("Returned collection does not contain all keys", c.contains(objArray[i]));
        }

        LinkedHashMap<Object, Object> myLinkedHashMap = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            myLinkedHashMap.put(objArray2[i], objArray[i]);
        }
        Collection<Object> values = myLinkedHashMap.values();
        values.remove(0);
        assertTrue("Removing from the values collection should remove from the original map",
                !myLinkedHashMap.containsValue(0));
    }

    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.LinkedHashMap.remove(java.lang.Object)
        int size = hm.size();
        Integer y = new Integer(9);
        Integer x = (Integer) hm.remove(y.toString());
        assertTrue("Remove returned incorrect value", x.equals(new Integer(9)));
        assertNull("Failed to remove given key", hm.get(new Integer(9)));
        assertTrue("Failed to decrement size", hm.size() == (size - 1));
        assertNull("Remove of non-existent key returned non-null", hm.remove("LCLCLC"));

        LinkedHashMap<Object, String> m = new LinkedHashMap<>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertNull("Failed with same hash as null", m.remove(new Integer(0)));
        assertEquals("Failed with null key", "org/teavm/metaprogramming/test", m.remove(null));
    }

    @Test
    public void test_clear() {
        // Test for method void java.util.LinkedHashMap.clear()
        hm.clear();
        assertEquals("Clear failed to reset size", 0, hm.size());
        for (int i = 0; i < hmSize; i++) {
            assertNull("Failed to clear all elements", hm.get(objArray2[i]));
        }
    }

    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.LinkedHashMap.clone()
        @SuppressWarnings("unchecked")
        LinkedHashMap<Object, Object> hm2 = (LinkedHashMap<Object, Object>) hm.clone();
        assertTrue("Clone answered equivalent LinkedHashMap", hm2 != hm);
        for (int counter = 0; counter < hmSize; counter++) {
            assertTrue("Clone answered unequal LinkedHashMap", hm.get(objArray2[counter])
                    == hm2.get(objArray2[counter]));
        }

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("key", "value");
        // get the keySet() and values() on the original Map
        Set<String> keys = map.keySet();
        Collection<String> values = map.values();
        assertEquals("values() does not work", "value", values.iterator().next());
        assertEquals("keySet() does not work", "key", keys.iterator().next());
        @SuppressWarnings("unchecked")
        AbstractMap<String, String> map2 = (AbstractMap<String, String>) map.clone();
        map2.put("key", "value2");
        Collection<String> values2 = map2.values();
        assertTrue("values() is identical", values2 != values);

        // values() and keySet() on the cloned() map should be different
        assertEquals("values() was not cloned", "value2", values2.iterator().next());
        map2.clear();
        map2.put("key2", "value3");
        Set<String> key2 = map2.keySet();
        assertTrue("keySet() is identical", key2 != keys);
        assertEquals("keySet() was not cloned", "key2", key2.iterator().next());
    }

    @Test
    public void test_clone_Mock() {
        LinkedHashMap<String, String> hashMap = new MockMap();
        String value = "value a";
        hashMap.put("key", value);
        MockMap cloneMap = (MockMap) hashMap.clone();
        assertEquals(value, cloneMap.get("key"));
        assertEquals(hashMap, cloneMap);
        assertEquals(1, cloneMap.num);

        hashMap.put("key", "value b");
        assertFalse(hashMap.equals(cloneMap));
    }

    class MockMap extends LinkedHashMap<String, String> {
        private static final long serialVersionUID = 1L;
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
        // Test for method boolean
        // java.util.LinkedHashMap.containsKey(java.lang.Object)
        assertTrue("Returned false for valid key", hm.containsKey(String.valueOf(876)));
        assertTrue("Returned true for invalid key", !hm.containsKey("KKDKDKD"));

        LinkedHashMap<Object, String> m = new LinkedHashMap<>();
        m.put(null, "org/teavm/metaprogramming/test");
        assertTrue("Failed with null key", m.containsKey(null));
        assertTrue("Failed with missing key matching null hash", !m.containsKey(new Integer(0)));
    }

    @Test
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.LinkedHashMap.containsValue(java.lang.Object)
        assertTrue("Returned false for valid value", hm.containsValue(new Integer(875)));
        assertTrue("Returned true for invalid valie", !hm.containsValue(new Integer(-9)));
    }

    @Test
    public void test_isEmpty() {
        // Test for method boolean java.util.LinkedHashMap.isEmpty()
        assertTrue("Returned false for new map", new LinkedHashMap<>().isEmpty());
        assertTrue("Returned true for non-empty", !hm.isEmpty());
    }

    @Test
    public void test_size() {
        // Test for method int java.util.LinkedHashMap.size()
        assertTrue("Returned incorrect size",
                hm.size() == (objArray.length + 2));
    }

    @Test
    public void test_ordered_entrySet() {
        int i;
        int sz = 100;
        LinkedHashMap<Integer, String> lhm = new LinkedHashMap<>();
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lhm.put(ii, ii.toString());
        }

        assertTrue("Returned set of incorrect size 1", lhm.size() == lhm.entrySet().size());
        i = 0;
        for (Map.Entry<Integer, String> m : lhm.entrySet()) {
            Integer jj = m.getKey();
            assertTrue("Returned incorrect entry set 1", jj.intValue() == i++);
        }

        LinkedHashMap<Integer, String> lruhm = new LinkedHashMap<>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lruhm.put(ii, ii.toString());
        }

        Set<Entry<Integer, String>> s3 = lruhm.entrySet();
        Iterator<Entry<Integer, String>> it3 = s3.iterator();
        assertTrue("Returned set of incorrect size 2", lruhm.size() == s3.size());
        for (i = 0; i < sz && it3.hasNext(); i++) {
            Map.Entry<Integer, String> m = it3.next();
            assertTrue("Returned incorrect entry set 2", m.getKey().intValue() == i);
        }

        /* fetch the even numbered entries to affect traversal order */
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            String ii = lruhm.get(new Integer(i));
            p = p + Integer.parseInt(ii);
        }
        assertEquals("invalid sum of even numbers", 2450, p);

        Set<Entry<Integer, String>> s2 = lruhm.entrySet();
        Iterator<Entry<Integer, String>> it2 = s2.iterator();
        assertTrue("Returned set of incorrect size 3", lruhm.size() == s2.size());
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            Entry<Integer, String> m = it2.next();
            assertTrue("Returned incorrect entry set 3", m.getKey().intValue() == i);
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            Entry<Integer, String> m = it2.next();
            assertTrue("Returned incorrect entry set 4", m.getKey().intValue() == i);
        }
        assertTrue("Entries left to iterate on", !it2.hasNext());
    }

    @Test
    public void test_ordered_keySet() {
        int i;
        int sz = 100;
        LinkedHashMap<Integer, String> lhm = new LinkedHashMap<>();
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lhm.put(ii, ii.toString());
        }

        Set<Integer> s1 = lhm.keySet();
        Iterator<Integer> it1 = s1.iterator();
        assertTrue("Returned set of incorrect size", lhm.size() == s1.size());
        for (i = 0; it1.hasNext(); i++) {
            Integer jj = it1.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i);
        }

        LinkedHashMap<Integer, String> lruhm = new LinkedHashMap<>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lruhm.put(ii, ii.toString());
        }

        Set<Integer> s3 = lruhm.keySet();
        Iterator<Integer> it3 = s3.iterator();
        assertTrue("Returned set of incorrect size", lruhm.size() == s3.size());
        for (i = 0; i < sz && it3.hasNext(); i++) {
            Integer jj = it3.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i);
        }

        /* fetch the even numbered entries to affect traversal order */
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            String ii = lruhm.get(new Integer(i));
            p = p + Integer.parseInt(ii);
        }
        assertEquals("invalid sum of even numbers", 2450, p);

        Set<Integer> s2 = lruhm.keySet();
        Iterator<Integer> it2 = s2.iterator();
        assertTrue("Returned set of incorrect size", lruhm.size() == s2.size());
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            Integer jj = it2.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i);
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            Integer jj = it2.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i);
        }
        assertTrue("Entries left to iterate on", !it2.hasNext());
    }

    @Test
    public void test_ordered_values() {
        int i;
        int sz = 100;
        LinkedHashMap<Integer, Integer> lhm = new LinkedHashMap<>();
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lhm.put(ii, new Integer(i * 2));
        }

        Collection<Integer> s1 = lhm.values();
        Iterator<Integer> it1 = s1.iterator();
        assertTrue("Returned set of incorrect size 1", lhm.size() == s1.size());
        for (i = 0; it1.hasNext(); i++) {
            Integer jj = it1.next();
            assertTrue("Returned incorrect entry set 1", jj.intValue() == i * 2);
        }

        LinkedHashMap<Integer, Integer> lruhm = new LinkedHashMap<>(200, .75f, true);
        for (i = 0; i < sz; i++) {
            Integer ii = new Integer(i);
            lruhm.put(ii, new Integer(i * 2));
        }

        Collection<Integer> s3 = lruhm.values();
        Iterator<Integer> it3 = s3.iterator();
        assertTrue("Returned set of incorrect size", lruhm.size() == s3.size());
        for (i = 0; i < sz && it3.hasNext(); i++) {
            Integer jj = it3.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i * 2);
        }

        // fetch the even numbered entries to affect traversal order
        int p = 0;
        for (i = 0; i < sz; i += 2) {
            Integer ii = lruhm.get(new Integer(i));
            p = p + ii.intValue();
        }
        assertTrue("invalid sum of even numbers", p == 2450 * 2);

        Collection<Integer> s2 = lruhm.values();
        Iterator<Integer> it2 = s2.iterator();
        assertTrue("Returned set of incorrect size", lruhm.size() == s2.size());
        for (i = 1; i < sz && it2.hasNext(); i += 2) {
            Integer jj = it2.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i * 2);
        }
        for (i = 0; i < sz && it2.hasNext(); i += 2) {
            Integer jj = it2.next();
            assertTrue("Returned incorrect entry set", jj.intValue() == i * 2);
        }
        assertTrue("Entries left to iterate on", !it2.hasNext());
    }
}
