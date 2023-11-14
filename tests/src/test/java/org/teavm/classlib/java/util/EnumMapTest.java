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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
        assertEquals("Constructor fails", 2, enumMap.get(Color.Green));
        assertNull("Constructor fails", enumMap.get(Color.Red));
        enumMap.put(Color.Red, 1);
        assertEquals("Wrong value", 1, enumMap.get(Color.Red));
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
        assertEquals("Get returned incorrect value for given key", 3, enumColorMap.get(Color.Blue));
        assertEquals("Wrong Size", 2, enumColorMap.size());
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
        assertEquals("Get returned incorrect value for given key", 2, enumColorMap.get(Color.Green));
        assertNull("Get returned non-null for non mapped key", enumColorMap.get(Color.Red));
        hashColorMap.put(Color.Red, 1);
        enumColorMap.putAll(hashColorMap);
        assertEquals("Get returned incorrect value for given key", 2, enumColorMap.get(Color.Green));
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
        assertNotSame("Should not be same", enumSizeMap, enumSizeMapClone);
        assertEquals("Clone answered unequal EnumMap", enumSizeMap, enumSizeMapClone);
        assertSame("Should be same", enumSizeMap.get(Size.Small), enumSizeMapClone.get(Size.Small));
        assertSame("Clone is not shallow clone", integer, enumSizeMapClone.get(Size.Small));
        enumSizeMap.remove(Size.Small);
        assertSame("Clone is not shallow clone", integer, enumSizeMapClone.get(Size.Small));
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
        assertSame("Should be same", set1, set);
        try {
            set.add(mockEntry);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertTrue("Returned false for contained object", set.contains(mockEntry));
        mockEntry = new MockEntry<>(Size.Middle, null);
        assertFalse("Returned true for uncontained object", set.contains(mockEntry));
        assertFalse("Returned true for uncontained object", set.contains(Size.Small));
        assertFalse("Returned true for uncontained object", set.contains(new MockEntry(1, 1)));
        assertFalse("Returned true for uncontained object", set.contains(1));
        mockEntry = new MockEntry<>(Size.Big, null);
        assertTrue("Returned false for contained object", set.contains(mockEntry));
        assertTrue("Returned false when the object can be removed", set.remove(mockEntry));
        assertFalse("Returned true for uncontained object", set.contains(mockEntry));
        assertFalse("Returned true when the object can not be removed", set.remove(mockEntry));
        assertFalse("Returned true when the object can not be removed", set.remove(new MockEntry(1, 1)));
        assertFalse("Returned true when the object can not be removed", set.remove(1));
        // The set is backed by the map so changes to one are reflected by the
        // other.
        enumSizeMap.put(Size.Big, 3);
        mockEntry = new MockEntry<>(Size.Big, 3);
        assertTrue("Returned false for contained object", set.contains(mockEntry));
        enumSizeMap.remove(Size.Big);
        assertFalse("Returned true for uncontained object", set.contains(mockEntry));
        assertEquals("Wrong size", 1, set.size());
        set.clear();
        assertEquals("Wrong size", 0, set.size());
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Collection<Map.Entry<Size, Integer>> c = new ArrayList<>();
        c.add(new MockEntry<>(Size.Middle, 1));
        assertTrue("Return wrong value", set.containsAll(c));
        assertTrue("Remove does not success", set.removeAll(c));
        enumSizeMap.put(Size.Middle, 1);
        c.add(new MockEntry(Size.Big, 3));
        assertTrue("Remove does not success", set.removeAll(c));
        assertFalse("Should return false", set.removeAll(c));
        assertEquals("Wrong size", 1, set.size());
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        c = new ArrayList<>();
        c.add(new MockEntry(Size.Middle, 1));
        c.add(new MockEntry(Size.Big, 3));
        assertTrue("Retain does not success", set.retainAll(c));
        assertEquals("Wrong size", 1, set.size());
        assertFalse("Should return false", set.retainAll(c));
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Object[] array = set.toArray();
        assertEquals("Wrong length", 2, array.length);
        Map.Entry entry = (Map.Entry) array[0];
        assertEquals("Wrong key", Size.Middle, entry.getKey());
        assertEquals("Wrong value", 1, entry.getValue());
        Object[] array1 = new Object[10];
        array1 = set.toArray();
        assertEquals("Wrong length", 2, array1.length);
        entry = (Map.Entry) array[0];
        assertEquals("Wrong key", Size.Middle, entry.getKey());
        assertEquals("Wrong value", 1, entry.getValue());
        array1 = new Object[10];
        array1 = set.toArray(array1);
        assertEquals("Wrong length", 10, array1.length);
        entry = (Map.Entry) array[1];
        assertEquals("Wrong key", Size.Big, entry.getKey());
        assertNull("Should be null", array1[2]);
        set = enumSizeMap.entrySet();
        Integer integer = Integer.valueOf("1");
        assertFalse("Returned true when the object can not be removed", set.remove(integer));
        assertTrue("Returned false when the object can be removed", set.remove(entry));
        enumSizeMap = new EnumMap<>(EnumMapTest.Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        Iterator<Map.Entry<Size, Integer>> iter = set.iterator();
        entry = iter.next();
        assertTrue("Returned false for contained object", set.contains(entry));
        mockEntry = new MockEntry<>(Size.Middle, 2);
        assertFalse("Returned true for uncontained object", set.contains(mockEntry));
        assertFalse("Returned true for uncontained object", set
                .contains(new MockEntry(2, 2)));
        entry = iter.next();
        assertTrue("Returned false for contained object", set.contains(entry));
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.remove(Size.Big);
        mockEntry = new MockEntry<>(Size.Big, null);
        assertEquals("Wrong size", 1, set.size());
        assertFalse("Returned true for uncontained object", set.contains(mockEntry));
        enumSizeMap.put(Size.Big, 2);
        mockEntry = new MockEntry<>(Size.Big, 2);
        assertTrue("Returned false for contained object", set
                .contains(mockEntry));
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
        assertEquals("Wrong key", Size.Middle, entry.getKey());
        assertTrue("Returned false for contained object", set.contains(entry));
        enumSizeMap.put(Size.Middle, 3);
        assertTrue("Returned false for contained object", set.contains(entry));
        entry.setValue(2);
        assertTrue("Returned false for contained object", set.contains(entry));
        assertFalse("Returned true for uncontained object", set.remove(1));
        iter.next();
        assertEquals("Wrong key", Size.Middle, entry.getKey());
        set.clear();
        assertEquals("Wrong size", 0, set.size());
        enumSizeMap = new EnumMap<>(Size.class);
        enumSizeMap.put(Size.Middle, 1);
        enumSizeMap.put(Size.Big, null);
        set = enumSizeMap.entrySet();
        iter = set.iterator();
        mockEntry = new MockEntry<>(Size.Middle, 1);
        assertNotEquals("Wrong result", entry, mockEntry);
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        entry = iter.next();
        assertEquals("Wrong key", Size.Middle, entry.getKey());
        assertEquals("Should return true", entry, mockEntry);
        assertEquals("Should be equal", mockEntry.hashCode(), entry.hashCode());
        mockEntry = new MockEntry<>(Size.Big, 1);
        assertNotEquals("Wrong result", entry, mockEntry);
        entry = iter.next();
        assertNotEquals("Wrong result", entry, mockEntry);
        assertEquals("Wrong key", Size.Big, entry.getKey());
        iter.remove();
        assertNotEquals("Wrong result", entry, mockEntry);
        assertEquals("Wrong size", 1, set.size());
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
        assertSame("Should be same", collection1, collection);
        try {
            collection.add(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertTrue("Returned false for contained object", collection.contains(1));
        assertTrue("Returned false for contained object", collection.contains(null));
        assertFalse("Returned true for uncontained object", collection.contains(2));
        assertTrue("Returned false when the object can be removed", collection.remove(null));
        assertFalse("Returned true for uncontained object", collection.contains(null));
        assertFalse("Returned true when the object can not be removed", collection.remove(null));
        // The set is backed by the map so changes to one are reflected by the other.
        enumColorMap.put(Color.Blue, 3);
        assertTrue("Returned false for contained object", collection.contains(3));
        enumColorMap.remove(Color.Blue);
        assertFalse("Returned true for uncontained object", collection.contains(3));
        assertEquals("Wrong size", 1, collection.size());
        collection.clear();
        assertEquals("Wrong size", 0, collection.size());
        enumColorMap = new EnumMap<>(Color.class);
        enumColorMap.put(Color.Red, 1);
        enumColorMap.put(Color.Blue, null);
        collection = enumColorMap.values();
        Collection c = new ArrayList<>();
        c.add(1);
        assertTrue("Should return true", collection.containsAll(c));
        c.add(3.4);
        assertFalse("Should return false", collection.containsAll(c));
        assertTrue("Should return true", collection.removeAll(c));
        assertEquals("Wrong size", 1, collection.size());
        assertFalse("Should return false", collection.removeAll(c));
        assertEquals("Wrong size", 1, collection.size());
        try {
            collection.addAll(c);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        enumColorMap.put(Color.Red, 1);
        assertEquals("Wrong size", 2, collection.size());
        assertTrue("Should return true", collection.retainAll(c));
        assertEquals("Wrong size", 1, collection.size());
        assertFalse("Should return false", collection.retainAll(c));
        assertEquals(1, collection.size());
        Object[] array = collection.toArray();
        assertEquals("Wrong length", 1, array.length);
        assertEquals("Wrong key", 1, array[0]);
        enumColorMap = new EnumMap<>(Color.class);
        enumColorMap.put(Color.Red, 1);
        enumColorMap.put(Color.Blue, null);
        collection = enumColorMap.values();
        assertEquals("Wrong size", 2, collection.size());
        assertFalse("Returned true when the object can not be removed",
                collection.remove(Integer.valueOf("10")));
        Iterator<Integer> iter = enumColorMap.values().iterator();
        Object value = iter.next();
        assertTrue("Returned false for contained object", collection.contains(value));
        value = iter.next();
        assertTrue("Returned false for contained object", collection.contains(value));
        enumColorMap.put(Color.Green, 1);
        enumColorMap.remove(Color.Blue);
        assertFalse("Returned true for uncontained object", collection.contains(value));
        iter.remove();
        assertEquals("{Red=1, Green=1}", enumColorMap.toString());
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        assertFalse("Returned true for uncontained object", collection.contains(value));
        iter = enumColorMap.values().iterator();
        value = iter.next();
        assertTrue("Returned false for contained object", collection.contains(value));
        enumColorMap.put(Color.Green, 3);
        assertTrue("Returned false for contained object", collection.contains(value));
        assertTrue("Returned false for contained object", collection.remove(Integer.valueOf("1")));
        assertEquals("Wrong size", 1, collection.size());
        collection.clear();
        assertEquals("Wrong size", 0, collection.size());
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
        assertEquals("Wrong value", integer1, value);
        assertSame("Wrong value", integer1, value);
        assertNotEquals("Returned true for unequal object", iter, value);
        iter.remove();
        assertNotEquals("Returned true for unequal object", iter, value);
        try {
            iter.remove();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        assertEquals("Wrong size", 1, collection.size());
        value = iter.next();
        assertNotEquals("Returned true for unequal object", iter, value);
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
