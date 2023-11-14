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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.classlib.support.UnmodifiableCollectionTestSupport;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class HashMapTest {
    private HashMap<String, String> ht10;

    private HashMap<String, Integer> ht100;

    private HashMap<String, String> htfull;

    private List<String> keyList;

    private List<String> elmList;

    public HashMapTest() {
        ht10 = new HashMap<>(10);
        ht100 = new HashMap<>(100);
        htfull = new HashMap<>(10);
        keyList = new ArrayList<>(10);
        elmList = new ArrayList<>(10);

        for (int i = 0; i < 10; i++) {
            ht10.put("Key " + i, "Val " + i);
            keyList.add("Key " + i);
            elmList.add("Val " + i);
        }

        for (int i = 0; i < 7; i++) {
            htfull.put("FKey " + i, "FVal " + i);
        }
    }

    @Test
    public void test_Constructor() {
        // Test for method java.util.HashMap()
        new MapTest2Support(new HashMap<>()).runTest();

        HashMap<String, String> h = new HashMap<>();

        assertEquals("Created incorrect HashMap", 0, h.size());
    }

    @Test
    public void test_ConstructorI() {
        // Test for method java.util.HashMap(int)
        HashMap<String, String> h = new HashMap<>(9);

        assertEquals("Created incorrect HashMap", 0, h.size());

        HashMap<String, String> empty = new HashMap<>(0);
        assertNull("Empty HashMap access", empty.get("nothing"));
        empty.put("something", "here");
        assertEquals("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void test_ConstructorIF() {
        // Test for method java.util.HashMap(int, float)
        HashMap<String, String> h = new HashMap<>(10, 0.5f);
        assertEquals("Created incorrect HashMap", 0, h.size());

        HashMap<String, String> empty = new HashMap<>(0, 0.75f);
        assertNull("Empty HashMap access", empty.get("nothing"));
        empty.put("something", "here");
        assertEquals("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.HashMap(java.util.Map)
        Map<String, Object> map = new TreeMap<>();
        Object firstVal = "Gabba";
        Object secondVal = 5;
        map.put("Gah", firstVal);
        map.put("Ooga", secondVal);
        HashMap<String, Object> ht = new HashMap<>(map);
        assertSame("a) Incorrect HashMap constructed", firstVal, ht.get("Gah"));
        assertSame("b) Incorrect HashMap constructed", secondVal, ht.get("Ooga"));
    }

    public void test_HashMap_Constructor() {
        HashMap<HashMap<?, ?>, Set<HashMap<?, ?>>> hashMap = new HashMap<>();
        hashMap.put(hashMap, hashMap.keySet());
        new HashMap<>(hashMap);
    }

    @Test
    public void test_clear() {
        // Test for method void java.util.HashMap.clear()
        HashMap<String, String> h = hashMapClone(htfull);
        h.clear();
        assertEquals("HashMap was not cleared", 0, h.size());
        Iterator<String> el = h.values().iterator();
        Iterator<String> keys = h.keySet().iterator();
        assertTrue("HashMap improperly cleared", !el.hasNext() && !keys.hasNext());
    }

    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.HashMap.clone()

        @SuppressWarnings("unchecked")
        HashMap<String, String> h = (HashMap<String, String>) htfull.clone();
        assertEquals("Clone different size than original", h.size(), htfull.size());

        Iterator<String> org = htfull.keySet().iterator();
        Iterator<String> cpy = h.keySet().iterator();

        String okey;
        String ckey;
        while (org.hasNext()) {
            okey = org.next();
            ckey = cpy.next();
            assertEquals("Key comparison failed", okey, ckey);
            assertEquals("Value comparison failed", htfull.get(okey), h.get(ckey));
        }
        assertFalse("Copy has more keys than original", cpy.hasNext());
    }

    @Test
    public void test_containsLjava_lang_Object() {
        // Test for method boolean
        // java.util.HashMap.contains(java.lang.Object)
        assertTrue("Element not found", ht10.containsValue("Val 7"));
        assertFalse("Invalid element found", ht10.containsValue("ZZZZZZZZZZZZZZZZ"));
    }

    @Test
    public void test_containsKeyLjava_lang_Object() {
        // Test for method boolean
        // java.util.HashMap.containsKey(java.lang.Object)

        assertTrue("Failed to find key", htfull.containsKey("FKey 4"));
        assertFalse("Failed to find key", htfull.containsKey("FKey 99"));
    }

    @Test
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.HashMap.containsValue(java.lang.Object)
        for (String s : elmList) {
            assertTrue("Returned false for valid value", ht10.containsValue(s));
        }
        assertFalse("Returned true for invalid value", ht10.containsValue(new Object()));
    }

    @Test
    public void test_elements() {
        // Test for method java.util.Enumeration java.util.HashMap.elements()
        Iterator<String> elms = ht10.values().iterator();
        while (elms.hasNext()) {
            String s = elms.next();
            assertTrue("Missing key from enumeration", elmList.contains(s));
        }

        assertEquals("All keys not retrieved", 10, ht10.size());

        assertFalse(elms.hasNext());
        try {
            elms.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void test_elements_subtest0() {
        // this is the reference implementation behavior
        final HashMap<String, String> ht = new HashMap<>(7);
        ht.put("1", "a");
        // these three elements hash to the same bucket in a 7 element HashMap
        ht.put("2", "b");
        ht.put("9", "c");
        ht.put("12", "d");
        // HashMap looks like:
        // 0: "1"
        // 1: "12" -> "9" -> "2"
        Iterator<String> en = ht.values().iterator();
        // cache the first entry
        en.hasNext();
        ht.remove("12");
        ht.remove("9");
    }

    @Test
    public void test_entrySet() {
        // Test for method java.util.Set java.util.HashMap.entrySet()
        Set<Map.Entry<String, String>> s = ht10.entrySet();
        Set<String> s2 = new HashSet<>();
        for (Map.Entry<String, String> entry : s) {
            s2.add(entry.getValue());
        }
        for (String string : elmList) {
            assertTrue("Returned incorrect entry set", s2.contains(string));
        }

        ht10.entrySet().iterator().next().setValue(null);
    }

    @Test
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean java.util.HashMap.equals(java.lang.Object)
        HashMap<String, String> h = hashMapClone(ht10);
        assertEquals("Returned false for equal tables", ht10, h);
        assertFalse("Returned true for unequal tables", ht10.equals(htfull));
    }

    @Test
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.HashMap.get(java.lang.Object)
        HashMap<String, String> h = hashMapClone(htfull);
        assertEquals("Could not retrieve element", "FVal 2", h.get("FKey 2"));

        // Regression for HARMONY-262
        org.teavm.classlib.java.util.HashMapTest.ReusableKey
                k = new org.teavm.classlib.java.util.HashMapTest.ReusableKey();
        HashMap<org.teavm.classlib.java.util.HashMapTest.ReusableKey, String> h2 = new HashMap<>();
        k.setKey(1);
        h2.put(k, "value1");

        k.setKey(13);
        assertNull(h2.get(k));

        k.setKey(12);
        assertNull(h2.get(k));
    }

    @Test
    public void test_hashCode() {
        // Test for method int java.util.HashMap.hashCode()
        int expectedHash = 0;
        for (Map.Entry<String, String> e : ht10.entrySet()) {
            expectedHash += e.hashCode();
        }
        assertEquals("Incorrect hashCode returned.  Wanted: " + expectedHash + " got: " + ht10.hashCode(),
                expectedHash, ht10.hashCode());
        assertEquals(ht10.hashCode(), ht10.entrySet().hashCode());
    }

    @Test
    public void test_isEmpty() {
        // Test for method boolean java.util.HashMap.isEmpty()

        assertFalse("isEmpty returned incorrect value", ht10.isEmpty());
        assertTrue("isEmpty returned incorrect value", new HashMap<String, String>().isEmpty());
    }

    @Test
    public void test_keys() {
        // Test for method java.util.Enumeration java.util.HashMap.keys()

        Iterator<String> keys = ht10.keySet().iterator();
        while (keys.hasNext()) {
            String s = keys.next();
            assertTrue("Missing key from enumeration", keyList.contains(s));
        }

        assertEquals("All keys not retrieved", 10, ht10.size());

        assertFalse(keys.hasNext());
        try {
            keys.next();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void test_keys_subtest0() {
        // this is the reference implementation behavior
        final HashMap<String, String> ht = new HashMap<>(3);
        ht.put("initial", "");
        Iterator<String> en = ht.keySet().iterator();
        en.hasNext();
        ht.remove("initial");
        try {
            en.next();
            fail();
        } catch (ConcurrentModificationException e) {
            // ok
        }
    }

    @Test
    public void test_keySet() {
        // Test for method java.util.Set java.util.HashMap.keySet()
        Set<String> s = ht10.keySet();
        for (String string : keyList) {
            assertTrue("Returned incorrect key set", s.contains(string));
        }

        Map<Integer, String> map = new HashMap<>(101);
        map.put(1, "1");
        map.put(102, "102");
        map.put(203, "203");
        Iterator<Integer> it = map.keySet().iterator();
        Integer remove1 = it.next();
        it.remove();
        Integer remove2 = it.next();
        it.remove();
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 102, 203));
        list.remove(remove1);
        list.remove(remove2);
        assertEquals("Wrong result", it.next(), list.get(0));
        assertEquals("Wrong size", 1, map.size());
        assertEquals("Wrong contents", map.keySet().iterator().next(), list.get(0));

        Map<Integer, String> map2 = new HashMap<>(101);
        map2.put(1, "1");
        map2.put(4, "4");
        Iterator<Integer> it2 = map2.keySet().iterator();
        Integer remove3 = it2.next();
        Integer next;
        if (remove3 == 1) {
            next = 4;
        } else {
            next = 1;
        }
        it2.hasNext();
        it2.remove();
        assertEquals("Wrong result 2", it2.next(), next);
        assertEquals("Wrong size 2", 1, map2.size());
        assertEquals("Wrong contents 2", map2.keySet().iterator().next(), next);

        Iterator<String> enumeration = s.iterator();
        assertTrue(enumeration.hasNext());
    }

    @Test
    public void test_keySet_subtest0() {
        Set<String> s1 = ht10.keySet();
        assertTrue("should contain key", s1.remove("Key 0"));
        assertFalse("should not contain key", s1.remove("Key 0"));
    }

    @Test
    public void test_keySet_subtest1() {
        // this is the reference implementation behavior
        final HashMap<String, String> ht = new HashMap<>(7);
        ht.put("1", "a");
        // these three elements hash to the same bucket in a 7 element HashMap
        ht.put("2", "b");
        ht.put("9", "c");
        ht.put("12", "d");
        // HashMap looks like:
        // 0: "1"
        // 1: "12" -> "9" -> "2"
        Iterator<String> it = ht.keySet().iterator();
        // this is mostly a copy of the test in test_elements_subtest0()
        // test removing with the iterator does not null the values
        while (it.hasNext()) {
            String key = it.next();
            if ("1".equals(key)) {
                it.remove();
            }
        }
        it = ht.values().iterator();
        boolean exception = false;
        try {
            // cached "12"
            Set<String> iteratorElements = new HashSet<>();
            iteratorElements.add(it.next());
            iteratorElements.add(it.next());
            iteratorElements.add(it.next());
            assertTrue(iteratorElements.contains("b"));
            assertTrue(iteratorElements.contains("c"));
            assertTrue(iteratorElements.contains("d"));
        } catch (NoSuchElementException e) {
            exception = true;
        }
        assertFalse("unexpected NoSuchElementException", exception);
    }

    @Test
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.HashMap.put(java.lang.Object, java.lang.Object)
        HashMap<String, Integer> h = hashMapClone(ht100);
        Integer key = 100;
        h.put("Value 100", key);
        assertTrue("Key/Value not inserted", h.size() == 1 && h.containsValue(key));
    }

    @Test
    public void test_putAllLjava_util_Map() {
        // Test for method void java.util.HashMap.putAll(java.util.Map)
        HashMap<String, String> h = new HashMap<>();
        h.putAll(ht10);
        for (String x : keyList) {
            assertEquals("Failed to put all elements", h.get(x), ht10.get(x));
        }
    }

    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.HashMap.remove(java.lang.Object)
        HashMap<String, String> h = hashMapClone(htfull);
        Object k = h.remove("FKey 0");
        assertTrue("Remove failed", !h.containsKey("FKey 0") || k == null);
    }

    @Test
    public void test_HashMap_selfReference_scenario1() {
        HashMap<HashMap<?, ?>, Set<HashMap<?, ?>>> hashMap = new HashMap<>();
        Set<HashMap<?, ?>> keySet = hashMap.keySet();
        hashMap.put(hashMap, keySet);
    }

    @Test
    public void test_HashMap_selfReference_scenario2() {
        HashMap<HashMap<?, ?>, HashMap<?, ?>> hashMap = new HashMap<>();
        hashMap.keySet();
        hashMap.put(hashMap, hashMap);
    }

    @Test
    public void test_size() {
        // Test for method int java.util.HashMap.size()
        assertTrue("Returned invalid size", ht10.size() == 10 && ht100.isEmpty());
    }

    @Test
    public void test_toString() {
        // Test for method java.lang.String java.util.HashMap.toString()
        HashMap<Serializable, Serializable> h = new HashMap<>();
        assertEquals("Incorrect toString for Empty table", "{}", h.toString());

        h.put("one", "1");
        h.put("two", h);
        String result = h.toString();
        assertTrue("should contain self ref", result.contains("(this"));
    }

    @Test
    public void test_values() {
        // Test for method java.util.Collection java.util.HashMap.values()
        Collection<String> c = ht10.values();
        for (String s : elmList) {
            assertTrue("Returned incorrect values", c.contains(s));
        }

        HashMap<Integer, Integer> myHashMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            myHashMap.put(i, i);
        }
        Collection<Integer> values = myHashMap.values();
        new UnmodifiableCollectionTestSupport(values).runTest();
        values.remove(0);
        assertFalse("Removing from the values collection should remove from the original map",
                myHashMap.containsValue(0));
    }

    @Test
    public void test_entrySet_remove() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("my.nonexistent.prop", "AAA");
        hashMap.put("parse.error", "BBB");
        Iterator<Map.Entry<String, String>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            final Object value = entry.getValue();
            if (value.equals("AAA")) {
                iterator.remove();
            }
        }
        assertFalse(hashMap.containsKey("my.nonexistent.prop"));
    }

    @Test
    public void test_keys_elements_keySet_Exceptions() {
        HashMap<String, String> hashMap = new HashMap<>();
        String key = "key";
        String value = "value";
        hashMap.put(key, value);

        Iterator<String> iterator = hashMap.keySet().iterator();
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
        assertTrue(hashMap.isEmpty());

        hashMap.put(key, value);
        iterator = hashMap.values().iterator();
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
        assertTrue(hashMap.isEmpty());

        hashMap.put(key, value);
        iterator = hashMap.keySet().iterator();
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

        hashMap.clear();
        for (int i = 0; i < 10; i++) {
            hashMap.put(key + i, value + i);
        }

        Iterator<String> enumeration = hashMap.keySet().iterator();
        iterator = enumeration;
        assertTrue(enumeration.hasNext());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.next();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasNext());
        assertFalse(iterator.hasNext());
        try {
            enumeration.next();
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
        enumeration = hashMap.values().iterator();
        iterator = enumeration;
        assertTrue(enumeration.hasNext());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.next();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasNext());
        assertFalse(iterator.hasNext());
        try {
            enumeration.next();
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
        enumeration = hashMap.keySet().iterator();
        iterator = (Iterator<String>) enumeration;
        assertTrue(enumeration.hasNext());
        assertTrue(iterator.hasNext());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                enumeration.next();
            } else {
                iterator.next();
            }
        }
        assertFalse(enumeration.hasNext());
        assertFalse(iterator.hasNext());
        try {
            enumeration.next();
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.compute("Key5", (k, v) -> null);
        assertNull(newVal);
        assertEquals(9, ht10.size());

        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertNull("Value was unexpectedly present in map", ht10.get("Key" + i));
            } else {
                assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
            }
        }
    }

    @Test
    public void test_computeIfAbsentNominal() {
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("absent key", k -> "added");
        assertEquals("added", newVal);
        assertEquals(11, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
        assertEquals("New value expected", "added", ht10.get("absent key"));
    }

    @Test
    public void test_computeIfAbsentIgnoresExistingEntry() {
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("Key5", v -> "changed");
        assertEquals("Val5", newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfAbsentDoesNothingIfNullProduced() {
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfAbsent("absent key", v -> null);
        assertNull(newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfPresentNominal() {
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfPresent("absent key", (k, v) -> "added");
        assertNull(newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_computeIfPresentRemovesEntryWhenNullProduced() {
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.computeIfPresent("Key5", (k, v) -> null);
        assertNull(newVal);
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.merge("Key5", "changed", (k, v) -> null);
        assertNull(newVal);
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
        HashMap<String, String> ht10 = new HashMap<>();
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
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        String newVal = ht10.replace("absent key", "changed");
        assertNull(newVal);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @Test
    public void test_replace2Nominal() {
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        boolean replaced = ht10.replace("Key5", "Val5", "changed");
        assertTrue(replaced);
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
        HashMap<String, String> ht10 = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            ht10.put("Key" + i, "Val" + i);
        }

        boolean replaced = ht10.replace("Key5", "incorrect value", "changed");
        assertFalse(replaced);
        assertEquals(10, ht10.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Value was unexpectedly changed", "Val" + i, ht10.get("Key" + i));
        }
    }

    @SuppressWarnings("unchecked")
    protected <K, V> HashMap<K, V> hashMapClone(HashMap<K, V> s) {
        return (HashMap<K, V>) s.clone();
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
