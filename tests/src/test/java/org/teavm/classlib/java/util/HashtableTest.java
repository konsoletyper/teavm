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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.classlib.support.UnmodifiableCollectionTestSupport;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class HashtableTest {

    private Hashtable<String, String> ht10;

    private Hashtable<String, Integer> ht100;

    private Hashtable<String, String> htfull;

    private Vector<String> keyVector;

    private Vector<String> elmVector;

    public HashtableTest() {
        ht10 = new Hashtable<>(10);
        ht100 = new Hashtable<>(100);
        htfull = new Hashtable<>(10);
        keyVector = new Vector<>(10);
        elmVector = new Vector<>(10);

        for (int i = 0; i < 10; i++) {
            ht10.put("Key " + i, "Val " + i);
            keyVector.addElement("Key " + i);
            elmVector.addElement("Val " + i);
        }

        for (int i = 0; i < 7; i++) {
            htfull.put("FKey " + i, "FVal " + i);
        }
    }

    @Test
    public void test_Constructor() {
        // Test for method java.util.Hashtable()
        new MapTest2Support(new Hashtable<>()).runTest();

        Hashtable<String, String> h = new Hashtable<>();

        assertEquals("Created incorrect hashtable", 0, h.size());
    }

    @Test
    public void test_ConstructorI() {
        // Test for method java.util.Hashtable(int)
        Hashtable<String, String> h = new Hashtable<>(9);

        assertEquals("Created incorrect hashtable", 0, h.size());

        Hashtable<String, String> empty = new Hashtable<>(0);
        assertNull("Empty hashtable access", empty.get("nothing"));
        empty.put("something", "here");
        assertEquals("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void test_ConstructorIF() {
        // Test for method java.util.Hashtable(int, float)
        Hashtable<String, String> h = new Hashtable<>(10, 0.5f);
        assertEquals("Created incorrect hashtable", 0, h.size());

        Hashtable<String, String> empty = new Hashtable<>(0, 0.75f);
        assertNull("Empty hashtable access", empty.get("nothing"));
        empty.put("something", "here");
        assertEquals("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.Hashtable(java.util.Map)
        Map<String, Object> map = new TreeMap<>();
        Object firstVal = "Gabba";
        Object secondVal = new Integer(5);
        map.put("Gah", firstVal);
        map.put("Ooga", secondVal);
        Hashtable<String, Object> ht = new Hashtable<>(map);
        assertSame("a) Incorrect Hashtable constructed", firstVal, ht.get("Gah"));
        assertSame("b) Incorrect Hashtable constructed", secondVal, ht.get("Ooga"));
    }

    public void test_HashTable_Constructor() {
        Hashtable<Hashtable<?, ?>, Set<Hashtable<?, ?>>> hashTable = new Hashtable<>();
        hashTable.put(hashTable, hashTable.keySet());
        new Hashtable<>(hashTable);
    }

    @Test
    public void test_clear() {
        // Test for method void java.util.Hashtable.clear()
        Hashtable<String, String> h = hashtableClone(htfull);
        h.clear();
        assertEquals("Hashtable was not cleared", 0, h.size());
        Enumeration<String> el = h.elements();
        Enumeration<String> keys = h.keys();
        assertTrue("Hashtable improperly cleared", !el.hasMoreElements() && !(keys.hasMoreElements()));
    }

    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.Hashtable.clone()

        @SuppressWarnings("unchecked")
        Hashtable<String, String> h = (Hashtable<String, String>) htfull.clone();
        assertTrue("Clone different size than original", h.size() == htfull.size());

        Enumeration<String> org = htfull.keys();
        Enumeration<String> cpy = h.keys();

        String okey;
        String ckey;
        while (org.hasMoreElements()) {
            okey = org.nextElement();
            ckey = cpy.nextElement();
            assertTrue("Key comparison failed", okey.equals(ckey));
            assertTrue("Value comparison failed", (htfull.get(okey)).equals(h.get(ckey)));
        }
        assertTrue("Copy has more keys than original", !cpy.hasMoreElements());
    }

    @Test
    public void test_containsLjava_lang_Object() {
        // Test for method boolean
        // java.util.Hashtable.contains(java.lang.Object)
        assertTrue("Element not found", ht10.contains("Val 7"));
        assertTrue("Invalid element found", !ht10.contains("ZZZZZZZZZZZZZZZZ"));
    }

    @Test
    public void test_containsKeyLjava_lang_Object() {
        // Test for method boolean
        // java.util.Hashtable.containsKey(java.lang.Object)

        assertTrue("Failed to find key", htfull.containsKey("FKey 4"));
        assertTrue("Failed to find key", !htfull.containsKey("FKey 99"));
    }

    @Test
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.Hashtable.containsValue(java.lang.Object)
        Enumeration<String> e = elmVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("Returned false for valid value", ht10.containsValue(e.nextElement()));
        }
        assertTrue("Returned true for invalid value", !ht10.containsValue(new Object()));
    }

    @Test
    public void test_elements() {
        // Test for method java.util.Enumeration java.util.Hashtable.elements()
        Enumeration<String> elms = ht10.elements();
        while (elms.hasMoreElements()) {
            String s = elms.nextElement();
            assertTrue("Missing key from enumeration", elmVector.contains(s));
        }

        assertEquals("All keys not retrieved", 10, ht10.size());

        // cast Enumeration to Iterator
        @SuppressWarnings("unchecked")
        Iterator<String> iterator = (Iterator<String>) elms;
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void test_elements_subtest0() {
        // this is the reference implementation behavior
        final Hashtable<String, String> ht = new Hashtable<>(7);
        ht.put("1", "a");
        // these three elements hash to the same bucket in a 7 element Hashtable
        ht.put("2", "b");
        ht.put("9", "c");
        ht.put("12", "d");
        // Hashtable looks like:
        // 0: "1"
        // 1: "12" -> "9" -> "2"
        Enumeration<String> en = ht.elements();
        // cache the first entry
        en.hasMoreElements();
        ht.remove("12");
        ht.remove("9");
    }

    @Test
    public void test_entrySet() {
        // Test for method java.util.Set java.util.Hashtable.entrySet()
        Set<Map.Entry<String, String>> s = ht10.entrySet();
        Set<String> s2 = new HashSet<>();
        for (Map.Entry<String, String> entry : s) {
            s2.add(entry.getValue());
        }
        Enumeration<String> e = elmVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("Returned incorrect entry set", s2.contains(e.nextElement()));
        }

        boolean exception = false;
        try {
            (ht10.entrySet().iterator().next()).setValue(null);
        } catch (NullPointerException e1) {
            exception = true;
        }
        assertTrue("Should not be able to assign null to a Hashtable entrySet() Map.Entry", exception);
    }

    @Test
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean java.util.Hashtable.equals(java.lang.Object)
        Hashtable<String, String> h = hashtableClone(ht10);
        assertTrue("Returned false for equal tables", ht10.equals(h));
        assertTrue("Returned true for unequal tables", !ht10.equals(htfull));
    }

    @Test
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.Hashtable.get(java.lang.Object)
        Hashtable<String, String> h = hashtableClone(htfull);
        assertEquals("Could not retrieve element", "FVal 2", h.get("FKey 2"));

        // Regression for HARMONY-262
        ReusableKey k = new ReusableKey();
        Hashtable<ReusableKey, String> h2 = new Hashtable<>();
        k.setKey(1);
        h2.put(k, "value1");

        k.setKey(13);
        assertNull(h2.get(k));

        k.setKey(12);
        assertNull(h2.get(k));
    }

    @Test
    public void test_hashCode() {
        // Test for method int java.util.Hashtable.hashCode()
        Set<Map.Entry<String, String>> entrySet = ht10.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
        int expectedHash;
        for (expectedHash = 0; iterator.hasNext(); expectedHash += iterator.next().hashCode()) {
            // do nothing
        }
        assertTrue("Incorrect hashCode returned.  Wanted: " + expectedHash + " got: " + ht10.hashCode(),
                expectedHash == ht10.hashCode());
    }

    @Test
    public void test_isEmpty() {
        // Test for method boolean java.util.Hashtable.isEmpty()

        assertTrue("isEmpty returned incorrect value", !ht10.isEmpty());
        assertTrue("isEmpty returned incorrect value", new Hashtable<String, String>().isEmpty());
    }

    @Test
    public void test_keys() {
        // Test for method java.util.Enumeration java.util.Hashtable.keys()

        Enumeration<String> keys = ht10.keys();
        while (keys.hasMoreElements()) {
            String s = keys.nextElement();
            assertTrue("Missing key from enumeration", keyVector.contains(s));
        }

        assertEquals("All keys not retrieved", 10, ht10.size());

        // cast Enumeration to Iterator
        @SuppressWarnings("unchecked")
        Iterator<String> iterator = (Iterator<String>) keys;
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void test_keys_subtest0() {
        // this is the reference implementation behavior
        final Hashtable<String, String> ht = new Hashtable<>(3);
        ht.put("initial", "");
        Enumeration<String> en = ht.keys();
        en.hasMoreElements();
        ht.remove("initial");
        boolean exception = false;
        try {
            Object result = en.nextElement();
            assertTrue("unexpected: " + result, "initial".equals(result));
        } catch (NoSuchElementException e) {
            exception = true;
        }
        assertTrue("unexpected NoSuchElementException", !exception);
    }

    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.Hashtable.keySet()
        Set<String> s = ht10.keySet();
        Enumeration<String> e = keyVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("Returned incorrect key set", s.contains(e.nextElement()));
        }

        Map<Integer, String> map = new Hashtable<>(101);
        map.put(new Integer(1), "1");
        map.put(new Integer(102), "102");
        map.put(new Integer(203), "203");
        Iterator<Integer> it = map.keySet().iterator();
        Integer remove1 = it.next();
        it.remove();
        Integer remove2 = it.next();
        it.remove();
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(new Integer[] { 1, 102, 203 }));
        list.remove(remove1);
        list.remove(remove2);
        assertTrue("Wrong result", it.next().equals(list.get(0)));
        assertEquals("Wrong size", 1, map.size());
        assertTrue("Wrong contents", map.keySet().iterator().next().equals(list.get(0)));

        Map<Integer, String> map2 = new Hashtable<>(101);
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

        // cast Iterator to Enumeration
        @SuppressWarnings("unchecked")
        Enumeration<String> enumeration = (Enumeration<String>) s.iterator();
        assertTrue(enumeration.hasMoreElements());
    }

    @Test
    public void test_keySet_subtest0() {
        Set<String> s1 = ht10.keySet();
        assertTrue("should contain key", s1.remove("Key 0"));
        assertTrue("should not contain key", !s1.remove("Key 0"));
    }

    @Test
    public void test_keySet_subtest1() {
        // this is the reference implementation behavior
        final Hashtable<String, String> ht = new Hashtable<>(7);
        ht.put("1", "a");
        // these three elements hash to the same bucket in a 7 element Hashtable
        ht.put("2", "b");
        ht.put("9", "c");
        ht.put("12", "d");
        // Hashtable looks like:
        // 0: "1"
        // 1: "12" -> "9" -> "2"
        Enumeration<String> en = ht.elements();
        // cache the first entry
        en.hasMoreElements();
        Iterator<String> it = ht.keySet().iterator();
        // this is mostly a copy of the test in test_elements_subtest0()
        // test removing with the iterator does not null the values
        while (it.hasNext()) {
            String key = it.next();
            if ("1".equals(key)) {
                it.remove();
            }
        }
        boolean exception = false;
        try {
            // cached "12"
            Set<String> iteratorElements = new HashSet<>();
            iteratorElements.add(en.nextElement());
            iteratorElements.add(en.nextElement());
            iteratorElements.add(en.nextElement());
            assertTrue(iteratorElements.contains("b"));
            assertTrue(iteratorElements.contains("c"));
            assertTrue(iteratorElements.contains("d"));
        } catch (NoSuchElementException e) {
            exception = true;
        }
        assertTrue("unexpected NoSuchElementException", !exception);
    }

    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.Hashtable.put(java.lang.Object, java.lang.Object)
        Hashtable<String, Integer> h = hashtableClone(ht100);
        Integer key = new Integer(100);
        h.put("Value 100", key);
        assertTrue("Key/Value not inserted", h.size() == 1 && (h.contains(key)));
    }

    @Test
    public void test_putAllLjava_util_Map() {
        // Test for method void java.util.Hashtable.putAll(java.util.Map)
        Hashtable<String, String> h = new Hashtable<>();
        h.putAll(ht10);
        Enumeration<String> e = keyVector.elements();
        while (e.hasMoreElements()) {
            Object x = e.nextElement();
            assertTrue("Failed to put all elements", h.get(x).equals(ht10.get(x)));
        }
    }

    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.Hashtable.remove(java.lang.Object)
        Hashtable<String, String> h = hashtableClone(htfull);
        Object k = h.remove("FKey 0");
        assertTrue("Remove failed", !h.containsKey("FKey 0") || k == null);
    }

    @Test
    public void test_HashTable_remove_scenario1() {
        Hashtable<Hashtable<?, ?>, Set<Hashtable<?, ?>>> hashTable = new Hashtable<>();
        Set<Hashtable<?, ?>> keySet = hashTable.keySet();
        hashTable.put(hashTable, keySet);
        hashTable.remove(hashTable);
    }

    @Test
    public void test_HashTable_remove_scenario2() {
        Hashtable<Hashtable<?, ?>, Hashtable<?, ?>> hashTable = new Hashtable<>();
        hashTable.keySet();
        hashTable.put(hashTable, hashTable);
        hashTable.remove(hashTable);
    }

    @Test
    public void test_HashTable_remove_scenario3() {
        Hashtable<Hashtable<Hashtable<?, ?>, Hashtable<?, ?>>, Hashtable<?, ?>> hashTable = new Hashtable<>();
        Hashtable<Hashtable<?, ?>, Hashtable<?, ?>> keyHashTable = new Hashtable<>();
        keyHashTable.put(hashTable, keyHashTable);
        hashTable.put(keyHashTable, hashTable);
        hashTable.remove(keyHashTable);
    }

    @Test
    public void test_size() {
        // Test for method int java.util.Hashtable.size()
        assertTrue("Returned invalid size", ht10.size() == 10 && (ht100.size() == 0));
    }

    @Test
    public void test_toString() {
        // Test for method java.lang.String java.util.Hashtable.toString()
        Hashtable<Serializable, Serializable> h = new Hashtable<>();
        assertEquals("Incorrect toString for Empty table", "{}", h.toString());

        h.put("one", "1");
        h.put("two", h);
        h.put(h, "3");
        h.put(h, h);
        String result = h.toString();
        assertTrue("should contain self ref", result.indexOf("(this") > -1);
    }

    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.Hashtable.values()
        Collection<String> c = ht10.values();
        Enumeration<String> e = elmVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("Returned incorrect values", c.contains(e.nextElement()));
        }

        Hashtable<Integer, Integer> myHashtable = new Hashtable<>();
        for (int i = 0; i < 100; i++) {
            myHashtable.put(new Integer(i), new Integer(i));
        }
        Collection<Integer> values = myHashtable.values();
        new UnmodifiableCollectionTestSupport(values).runTest();
        values.remove(new Integer(0));
        assertTrue("Removing from the values collection should remove from the original map",
                !myHashtable.containsValue(new Integer(0)));
    }

    @Test
    public void test_entrySet_remove() {
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("my.nonexistent.prop", "AAA");
        hashtable.put("parse.error", "BBB");
        Iterator<Map.Entry<String, String>> iterator = hashtable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            final Object value = entry.getValue();
            if (value.equals("AAA")) {
                iterator.remove();
            }
        }
        assertFalse(hashtable.containsKey("my.nonexistent.prop"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_keys_elements_keySet_Exceptions() {
        Hashtable<String, String> hashTable = new Hashtable<>();
        String key = "key";
        String value = "value";
        hashTable.put(key, value);

        Iterator<String> iterator = (Iterator<String>) hashTable.keys();
        assertTrue(iterator.hasNext());
        try {
            iterator.remove();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        iterator.next();
        try {
            iterator.remove();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertFalse(iterator.hasNext());

        iterator = (Iterator<String>) hashTable.elements();
        assertTrue(iterator.hasNext());
        try {
            iterator.remove();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        iterator.next();
        try {
            iterator.remove();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertFalse(iterator.hasNext());

        iterator = hashTable.keySet().iterator();
        assertTrue(iterator.hasNext());
        try {
            iterator.remove();
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        iterator.next();
        iterator.remove();
        assertFalse(iterator.hasNext());

        hashTable.clear();
        for (int i = 0; i < 10; i++) {
            hashTable.put(key + i, value + i);
        }

        // cast Enumeration to Iterator
        Enumeration<String> enumeration = hashTable.keys();
        iterator = (Iterator<String>) enumeration;
        assertTrue(enumeration.hasMoreElements());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.nextElement();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasMoreElements());
        assertFalse(iterator.hasNext());
        try {
            enumeration.nextElement();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
        try {
            iterator.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }

        // cast Enumeration to Iterator
        enumeration = hashTable.elements();
        iterator = (Iterator<String>) enumeration;
        assertTrue(enumeration.hasMoreElements());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.nextElement();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasMoreElements());
        assertFalse(iterator.hasNext());
        try {
            enumeration.nextElement();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
        try {
            iterator.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }

        // cast Iterator to Enumeration
        enumeration = (Enumeration<String>) hashTable.keySet().iterator();
        iterator = (Iterator<String>) enumeration;
        assertTrue(enumeration.hasMoreElements());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.nextElement();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasMoreElements());
        assertFalse(iterator.hasNext());
        try {
            enumeration.nextElement();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
        try {
            iterator.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void test_computeUpdatesValueIfPresent() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.compute("Key5", (k, v) -> "changed");
        assertEquals("changed", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was incorrectly changed", "changed", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_computePutsNewEntryIfKeyIsAbsent() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.compute("absent key", (k, v) -> "added");
        assertEquals("added", newVal);
        assertEquals(11, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
        assertEquals("New value expected", "added", ht10.get("absent key"));
    }

    @Test
    public void test_computeRemovesEntryWhenNullProduced() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.compute("Key5", (k, v) -> null);
        assertEquals(null, newVal);
        assertEquals(9, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was unexpectedly present in map", null, ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_computeIfAbsentNominal() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("absent key", (k) -> "added");
        assertEquals("added", newVal);
        assertEquals(11, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
        assertEquals("New value expected", "added", ht10.get("absent key"));
    }

    @Test
    public void test_computeIfAbsentIgnoresExistingEntry() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("Key5", (v) -> "changed");
        assertEquals("Val5", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfAbsentDoesNothingIfNullProduced() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("absent key", (v) -> null);
        assertEquals(null, newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfPresentNominal() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfPresent("Key5", (k, v) -> "changed");
        assertEquals("changed", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was incorrectly updated", "changed", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_computeIfPresentIgnoresAbsentKeys() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfPresent("absent key", (k, v) -> "added");
        assertEquals(null, newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfPresentRemovesEntryWhenNullProduced() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfPresent("Key5", (k, v) -> null);
        assertEquals(null, newVal);
        assertEquals(9, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertNull("Value unexpectedly present", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_mergeKeyAbsentCase() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.merge("absent key", "changed", (k, v) -> "remapped");
        assertEquals("changed", newVal);
        assertEquals(11, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
        assertEquals("New value expected", "changed", ht10.get("absent key"));
    }

    @Test
    public void test_mergeKeyPresentCase() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.merge("Key5", "changed", (k, v) -> "remapped");
        assertEquals("remapped", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was incorrectly updated", "remapped", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_mergeKeyAbsentCase_remapToNull() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.merge("absent key", "changed", (k, v) -> null);
        assertEquals("changed", newVal);
        assertEquals(11, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
        assertEquals("New value expected", "changed", ht10.get("absent key"));
    }

    @Test
    public void test_mergeKeyPresentCase_remapToNull() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.merge("Key5", "changed", (k, v) -> null);
        assertEquals(null, newVal);
        assertEquals(9, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertNull("Null value expected", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_replaceNominal() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.replace("Key5", "changed");
        assertEquals("Val5", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was incorrectly updated", "changed", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_replaceAbsentKey() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.replace("absent key", "changed");
        assertEquals(null, newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_replace2Nominal() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        boolean replaced = ht10.replace("Key5", "Val5", "changed");
        assertEquals(true, replaced);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertEquals("Value was incorrectly updated", "changed", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_replace2WithIncorrectExpectation() {
        Hashtable<String, String> ht10 = new Hashtable<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        boolean replaced = ht10.replace("Key5", "incorrect value", "changed");
        assertEquals(false, replaced);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @SuppressWarnings("unchecked")
    protected <K, V> Hashtable<K, V> hashtableClone(Hashtable<K, V> s) {
        return (Hashtable<K, V>) s.clone();
    }

    static class ReusableKey {
        private int key;

        public void setKey(int key) {
            this.key = key;
        }

        @Override
        public int hashCode() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ReusableKey)) {
                return false;
            }
            return key == ((ReusableKey) o).key;
        }
    }
}
