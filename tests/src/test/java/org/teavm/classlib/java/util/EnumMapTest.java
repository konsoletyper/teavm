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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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

    @Test
    @SuppressWarnings("unchecked")
    public void constructorMap() {
        EnumMap enumMap;
        Map enumColorMap = null;
        try {
            enumMap = new EnumMap(enumColorMap);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
        enumColorMap = new EnumMap<Color, Double>(Color.class);
        enumMap      = new EnumMap(enumColorMap);
        enumColorMap.put(Color.Blue, 3);
        enumMap      = new EnumMap(enumColorMap);

        HashMap hashColorMap = null;
        try {
            enumMap = new EnumMap(hashColorMap);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        hashColorMap = new HashMap();
        try {
            enumMap = new EnumMap(hashColorMap);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        hashColorMap.put(Color.Green, 2);
        enumMap = new EnumMap(hashColorMap);
        assertEquals(2, enumMap.get(Color.Green), "Constructor fails");
        assertNull(enumMap.get(Color.Red), "Constructor fails");
        enumMap.put(Color.Red, 1);
        assertEquals(1, enumMap.get(Color.Red), "Wrong value");
        hashColorMap.put(Size.Big, 3);
        try {
            enumMap = new EnumMap(hashColorMap);
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }

        hashColorMap = new HashMap();
        hashColorMap.put(1, 1);
        try {
            enumMap = new EnumMap(hashColorMap);
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void putAll() {
        EnumMap enumColorMap = new EnumMap(Color.class);
        enumColorMap.put(Color.Green, 2);
        EnumMap enumSizeMap = new EnumMap(Size.class);
        enumColorMap.putAll(enumSizeMap);
        enumSizeMap.put(Size.Big, 1);
        try {
            enumColorMap.putAll(enumSizeMap);
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
        EnumMap enumColorMap1 = new EnumMap<Color, Double>(Color.class);
        enumColorMap1.put(Color.Blue, 3);
        enumColorMap.putAll(enumColorMap1);
        assertEquals(3, enumColorMap.get(Color.Blue), "Get returned incorrect value for given key");
        assertEquals(2, enumColorMap.size(), "Wrong Size");
        enumColorMap = new EnumMap<Color, Double>(Color.class);
        HashMap hashColorMap = null;
        try {
            enumColorMap.putAll(hashColorMap);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
        hashColorMap = new HashMap();
        enumColorMap.putAll(hashColorMap);
        hashColorMap.put(Color.Green, 2);
        enumColorMap.putAll(hashColorMap);
        assertEquals(2, enumColorMap.get(Color.Green), "Get returned incorrect value for given key");
        assertNull(enumColorMap.get(Color.Red), "Get returned non-null for non mapped key");
        hashColorMap.put(Color.Red, 1);
        enumColorMap.putAll(hashColorMap);
        assertEquals(2, enumColorMap.get(Color.Green), "Get returned incorrect value for given key");
        hashColorMap.put(Size.Big, 3);
        try {
            enumColorMap.putAll(hashColorMap);
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
        hashColorMap = new HashMap();
        hashColorMap.put(1, 1);
        try {
            enumColorMap.putAll(hashColorMap);
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Test
    public void cloneWorks() {
        EnumMap<Size, Integer> enumSizeMap = new EnumMap<>(Size.class);
        Integer integer = Integer.valueOf("3");
        enumSizeMap.put(Size.Small, integer);
        EnumMap<Size, Integer> enumSizeMapClone = enumSizeMap.clone();
        assertNotSame(enumSizeMap, enumSizeMapClone, "Should not be same");
        assertEquals(enumSizeMap, enumSizeMapClone, "Clone answered unequal EnumMap");
        assertSame(enumSizeMap.get(Size.Small), enumSizeMapClone.get(Size.Small), "Should be same");
        assertSame(integer, enumSizeMapClone.get(Size.Small), "Clone is not shallow clone");
        enumSizeMap.remove(Size.Small);
        assertSame(integer, enumSizeMapClone.get(Size.Small), "Clone is not shallow clone");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void entrySet() {
        EnumMap<Size, Integer> enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        MockEntry<Size, Integer> mockEntry = new MockEntry<>(Size.Middle, 1);
        Set<Map.Entry<Size, Integer>> set = enumSizeMap.entrySet();
        Set<Map.Entry<Size, Integer>> set1 = enumSizeMap.entrySet();
        assertSame(set1, set, "Should be same");
        try {
            set.add(mockEntry);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertTrue(set.contains(mockEntry), "Returned false for contained object");
        mockEntry = new MockEntry<>(Size.Middle, null);
        assertFalse(set.contains(mockEntry), "Returned true for uncontained object");
        assertFalse(set.contains(Size.Small), "Returned true for uncontained object");
        assertFalse(set.contains(new MockEntry(1, 1)), "Returned true for uncontained object");
        assertFalse(set.contains(1), "Returned true for uncontained object");
        mockEntry = new MockEntry<>(Size.Big, null);
        assertTrue(set.contains(mockEntry), "Returned false for contained object");
        assertTrue(set.remove(mockEntry), "Returned false when the object can be removed");
        assertFalse(set.contains(mockEntry), "Returned true for uncontained object");
        assertFalse(set.remove(mockEntry), "Returned true when the object can not be removed");
        assertFalse(set.remove(new MockEntry(1, 1)), "Returned true when the object can not be removed");
        assertFalse(set.remove(1), "Returned true when the object can not be removed");
        // The set is backed by the map so changes to one are reflected by the
        // other.
        enumSizeMap.put(Size.Big, 3);
        mockEntry = new MockEntry<>(Size.Big, 3);
        assertTrue(set.contains(mockEntry), "Returned false for contained object");
        enumSizeMap.remove(Size.Big);
        assertFalse(set.contains(mockEntry), "Returned true for uncontained object");
        assertEquals(1, set.size(), "Wrong size");
        set.clear();
        assertEquals(0, set.size(), "Wrong size");
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Collection<Map.Entry<Size, Integer>> c = new ArrayList<>();
        c.add(new MockEntry<>(Size.Middle, 1));
        assertTrue(set.containsAll(c), "Return wrong value");
        assertTrue(set.removeAll(c), "Remove does not success");
        enumSizeMap.put(Size.Middle, 1);
        c.add(new MockEntry(Size.Big, 3));
        assertTrue(set.removeAll(c), "Remove does not success");
        assertFalse(set.removeAll(c), "Should return false");
        assertEquals(1, set.size(), "Wrong size");
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        c = new ArrayList<>();
        c.add(new MockEntry(Size.Middle, 1));
        c.add(new MockEntry(Size.Big, 3));
        assertTrue(set.retainAll(c), "Retain does not success");
        assertEquals(1, set.size(), "Wrong size");
        assertFalse(set.retainAll(c), "Should return false");
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Object[] array = set.toArray();
        assertEquals(2, array.length, "Wrong length");
        Map.Entry entry = (Map.Entry) array[0];
        assertEquals(Size.Middle, entry.getKey(), "Wrong key");
        assertEquals(1, entry.getValue(), "Wrong value");
        Object[] array1 = new Object[10];
        array1 = set.toArray();
        assertEquals(2, array1.length, "Wrong length");
        entry = (Map.Entry) array[0];
        assertEquals(Size.Middle, entry.getKey(), "Wrong key");
        assertEquals(1, entry.getValue(), "Wrong value");
        array1 = new Object[10];
        array1 = set.toArray(array1);
        assertEquals(10, array1.length, "Wrong length");
        entry = (Map.Entry) array[1];
        assertEquals(Size.Big, entry.getKey(), "Wrong key");
        assertNull(array1[2], "Should be null");
        set = enumSizeMap.entrySet();
        Integer integer = Integer.valueOf("1");
        assertFalse(set.remove(integer), "Returned true when the object can not be removed");
        assertTrue(set.remove(entry), "Returned false when the object can be removed");
        enumSizeMap = new EnumMap<>(EnumMapTest.Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Iterator<Map.Entry<Size, Integer>> iter = set.iterator();
        entry = iter.next();
        assertTrue(set.contains(entry), "Returned false for contained object");
        mockEntry = new MockEntry<>(Size.Middle, 2);
        assertFalse(set.contains(mockEntry), "Returned true for uncontained object");
        assertFalse(set.contains(new MockEntry(2, 2)), "Returned true for uncontained object");
        entry = iter.next();
        assertTrue(set.contains(entry), "Returned false for contained object");
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.remove(Size.Big);
        mockEntry = new MockEntry<>(Size.Big, null);
        assertEquals(1, set.size(), "Wrong size");
        assertFalse(set.contains(mockEntry), "Returned true for uncontained object");
        enumSizeMap.put(Size.Big, 2);
        mockEntry = new MockEntry<>(Size.Big, 2);
        assertTrue(set.contains(mockEntry), "Returned false for contained object");
        iter.remove();
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            entry.setValue(2);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            set.contains(entry);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        enumSizeMap = new EnumMap(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        iter = set.iterator();
        entry = iter.next();
        assertEquals(Size.Middle, entry.getKey(), "Wrong key");
        assertTrue(set.contains(entry), "Returned false for contained object");
        enumSizeMap.put(Size.Middle, 3);
        assertTrue(set.contains(entry), "Returned false for contained object");
        entry.setValue(2);
        assertTrue(set.contains(entry), "Returned false for contained object");
        assertFalse(set.remove(1), "Returned true for uncontained object");
        iter.next();
        assertEquals(Size.Middle, entry.getKey(), "Wrong key");
        set.clear();
        assertEquals(0, set.size(), "Wrong size");
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        iter = set.iterator();
        mockEntry = new MockEntry<>(Size.Middle, 1);
        assertNotEquals(entry, mockEntry, "Wrong result");
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        entry = iter.next();
        assertEquals(Size.Middle, entry.getKey(), "Wrong key");
        assertEquals(entry, mockEntry, "Should return true");
        assertEquals(mockEntry.hashCode(), entry.hashCode(), "Should be equal");
        mockEntry = new MockEntry<>(Size.Big, 1);
        assertNotEquals(entry, mockEntry, "Wrong result");
        entry = iter.next();
        assertNotEquals(entry, mockEntry, "Wrong result");
        assertEquals(Size.Big, entry.getKey(), "Wrong key");
        iter.remove();
        assertNotEquals(entry, mockEntry, "Wrong result");
        assertEquals(1, set.size(), "Wrong size");
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            iter.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "boxing" })
    public void values() {
        EnumMap<Color, Integer> enumColorMap = new EnumMap<>(Color.class);
        enumColorMap.put(Color.Red, 1);
        enumColorMap.put(Color.Blue, null);
        Collection<Integer> collection = enumColorMap.values();
        Collection<Integer> collection1 = enumColorMap.values();
        assertSame(collection1, collection, "Should be same");
        try {
            collection.add(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertTrue(collection.contains(1), "Returned false for contained object");
        assertTrue(collection.contains(null), "Returned false for contained object");
        assertFalse(collection.contains(2), "Returned true for uncontained object");
        assertTrue(collection.remove(null), "Returned false when the object can be removed");
        assertFalse(collection.contains(null), "Returned true for uncontained object");
        assertFalse(collection.remove(null), "Returned true when the object can not be removed");
        // The set is backed by the map so changes to one are reflected by the other.
        enumColorMap.put(Color.Blue, 3);
        assertTrue(collection.contains(3), "Returned false for contained object");
        enumColorMap.remove(Color.Blue);
        assertFalse(collection.contains(3), "Returned true for uncontained object");
        assertEquals(1, collection.size(), "Wrong size");
        collection.clear();
        assertEquals(0, collection.size(), "Wrong size");
        enumColorMap = new EnumMap<>(Color.class);
        enumColorMap.put(Color.Red, 1);
        enumColorMap.put(Color.Blue, null);
        collection = enumColorMap.values();
        Collection c = new ArrayList<>();
        c.add(1);
        assertTrue(collection.containsAll(c), "Should return true");
        c.add(3.4);
        assertFalse(collection.containsAll(c), "Should return false");
        assertTrue(collection.removeAll(c), "Should return true");
        assertEquals(1, collection.size(), "Wrong size");
        assertFalse(collection.removeAll(c), "Should return false");
        assertEquals(1, collection.size(), "Wrong size");
        try {
            collection.addAll(c);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        enumColorMap.put(Color.Red, 1);
        assertEquals(2, collection.size(), "Wrong size");
        assertTrue(collection.retainAll(c), "Should return true");
        assertEquals(1, collection.size(), "Wrong size");
        assertFalse(collection.retainAll(c), "Should return false");
        assertEquals(1, collection.size());
        Object[] array = collection.toArray();
        assertEquals(1, array.length, "Wrong length");
        assertEquals(1, array[0], "Wrong key");
        enumColorMap = new EnumMap<>(Color.class);
        enumColorMap.put(Color.Red, 1);
        enumColorMap.put(Color.Blue, null);
        collection = enumColorMap.values();
        assertEquals(2, collection.size(), "Wrong size");
        assertFalse(collection.remove(Integer.valueOf("10")), "Returned true when the object can not be removed");
        Iterator<Integer> iter = enumColorMap.values().iterator();
        Object value = iter.next();
        assertTrue(collection.contains(value), "Returned false for contained object");
        value = iter.next();
        assertTrue(collection.contains(value), "Returned false for contained object");
        enumColorMap.put(Color.Green, 1);
        enumColorMap.remove(Color.Blue);
        assertFalse(collection.contains(value), "Returned true for uncontained object");
        iter.remove();
        assertEquals("{Red=1, Green=1}", enumColorMap.toString());
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        assertFalse(collection.contains(value), "Returned true for uncontained object");
        iter = enumColorMap.values().iterator();
        value = iter.next();
        assertTrue(collection.contains(value), "Returned false for contained object");
        enumColorMap.put(Color.Green, 3);
        assertTrue(collection.contains(value), "Returned false for contained object");
        assertTrue(collection.remove(Integer.valueOf("1")), "Returned false for contained object");
        assertEquals(1, collection.size(), "Wrong size");
        collection.clear();
        assertEquals(0, collection.size(), "Wrong size");
        enumColorMap = new EnumMap<>(Color.class);
        Integer integer1 = 1;
        enumColorMap.put(Color.Green, integer1);
        enumColorMap.put(Color.Blue, null);
        collection = enumColorMap.values();
        iter = enumColorMap.values().iterator();
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        value = iter.next();
        assertEquals(integer1, value, "Wrong value");
        assertSame(integer1, value, "Wrong value");
        assertNotEquals(iter, value, "Returned true for unequal object");
        iter.remove();
        assertNotEquals(iter, value, "Returned true for unequal object");
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        assertEquals(1, collection.size(), "Wrong size");
        value = iter.next();
        assertNotEquals(iter, value, "Returned true for unequal object");
        iter.remove();
        try {
            iter.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    enum Size {
        Small, Middle, Big {
        }
    }
    enum Color {
        Red, Green, Blue {
        }
    }
    enum Empty {
        //Empty
    }

    enum L {
        A, B, C
    }

    private static class MockEntry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;
        public MockEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode())
                    ^ (value == null ? 0 : value.hashCode());
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e
                    && Objects.equals(key, e.getKey())
                    && Objects.equals(value, e.getValue());
        }
        @Override
        public K getKey() {
            return key;
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V object) {
            V oldValue = value;
            value = object;
            return oldValue;
        }
    }
}
