/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class ConcurrentHashMapTest {
    @Test
    public void constructor() {
        try {
            new ConcurrentHashMap<>(-1);
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            new ConcurrentHashMap<>(1, -1f);
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }

        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(23), map.get("q"));
        assertNull(map.get("e"));
    }

    @Test
    public void getPut() {
        var map = new ConcurrentHashMap<>();

        assertEquals(0, map.size());
        assertNull(map.get("q"));
        assertFalse(map.containsKey("q"));

        assertNull(map.put("q", 23));
        assertEquals(1, map.size());
        assertEquals(23, map.get("q"));
        assertTrue(map.containsKey("q"));

        assertEquals(23, map.put("q", 24));
        assertEquals(1, map.size());
        assertEquals(24, map.get("q"));

        assertNull(map.put("w", 42));
        assertEquals(2, map.size());
        assertEquals(24, map.get("q"));
    }

    @Test
    public void remove() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertEquals(Integer.valueOf(23), map.remove("q"));
        assertEquals(1, map.size());
        assertNull(map.get("q"));

        assertNull(map.remove("q"));
        assertEquals(1, map.size());
    }

    @Test
    public void removeKeyValue() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertFalse(map.remove("q", 42));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(23), map.get("q"));

        assertTrue(map.remove("q", 23));
        assertEquals(1, map.size());
        assertNull(map.get("q"));
    }

    @Test
    public void clear() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        map.clear();
        assertEquals(0, map.size());
        assertNull(map.get("q"));
        assertNull(map.get("w"));
        assertFalse(map.entrySet().iterator().hasNext());
    }

    @Test
    public void containsValue() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertTrue(map.containsValue(23));
        assertTrue(map.containsValue(42));
        assertFalse(map.containsValue(99));
        assertFalse(map.containsValue("q"));
    }

    @Test
    public void entrySet() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        var entries = map.entrySet();
        assertEquals(2, entries.size());

        var entryList = new ArrayList<>(entries);
        entryList.sort(Map.Entry.comparingByKey());
        assertEquals(2, entryList.size());
        assertEquals("q", entryList.get(0).getKey());
        assertEquals(Integer.valueOf(23), entryList.get(0).getValue());
        assertEquals("w", entryList.get(1).getKey());
        assertEquals(Integer.valueOf(42), entryList.get(1).getValue());

        for (var entry : entries) {
            if (entry.getKey().equals("w")) {
                entry.setValue(43);
            }
        }
        assertEquals(Integer.valueOf(43), map.get("w"));

        entries.removeIf(entry -> entry.getKey().equals("q"));
        assertEquals(1, entries.size());
        assertEquals(1, map.size());
        assertNull(map.get("q"));
        assertEquals(Integer.valueOf(43), map.get("w"));
    }

    @Test
    public void replace() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertEquals(Integer.valueOf(23), map.replace("q", 24));
        assertEquals(Integer.valueOf(24), map.get("q"));
        assertEquals(2, map.size());

        assertNull(map.replace("e", 55));
        assertEquals(2, map.size());
        assertNull(map.get("e"));

        assertFalse(map.replace("w", 123, 43));
        assertEquals(Integer.valueOf(42), map.get("w"));

        assertTrue(map.replace("w", 42, 43));
        assertEquals(Integer.valueOf(43), map.get("w"));
    }

    @Test
    public void getOrDefault() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertEquals(Integer.valueOf(23), map.getOrDefault("q", 24));
        assertEquals(Integer.valueOf(55), map.getOrDefault("e", 55));
    }

    @Test
    public void forEach() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        var values = new Object[2];
        var size = new int[1];
        map.forEach((k, v) -> {
            size[0]++;
            if (k.equals("q")) {
                values[0] = v;
            } else if (k.equals("w")) {
                values[1] = v;
            }
        });

        assertEquals(2, size[0]);
        assertArrayEquals(new Object[] { 23, 42 }, values);
    }

    @Test
    public void putIfAbsent() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        assertEquals((Object) 23, map.putIfAbsent("q", 24));
        assertEquals(2, map.size());
        assertEquals((Object) 23, map.get("q"));

        assertNull(map.putIfAbsent("e", 55));
        assertEquals(3, map.size());
        assertEquals((Object) 55, map.get("e"));
    }

    @Test
    public void replaceAll() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        map.replaceAll((k, v) -> v + k.charAt(0) * 100);
        assertEquals(2, map.size());
        assertEquals((Object) 11323, map.get("q"));
        assertEquals((Object) 11942, map.get("w"));
    }

    @Test
    public void computeIfAbsent() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        var count = new int[1];
        Function<String, Integer> f = k -> {
            count[0]++;
            return (int) k.charAt(0);
        };
        assertEquals((Object) 23, map.computeIfAbsent("q", f));
        assertEquals(0, count[0]);
        assertEquals(2, map.size());
        assertEquals((Object) 23, map.get("q"));

        assertEquals((Object) 101, map.computeIfAbsent("e", f));
        assertEquals(1, count[0]);
        assertEquals(3, map.size());
        assertEquals((Object) 101, map.get("e"));
    }

    @Test
    public void computeIfPresent() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        var count = new int[1];
        BiFunction<String, Integer, Integer> f = (k, v) -> {
            count[0]++;
            return (int) k.charAt(0) * 100 + v;
        };
        assertEquals((Object) 11323, map.computeIfPresent("q", f));
        assertEquals(1, count[0]);
        assertEquals(2, map.size());
        assertEquals((Object) 11323, map.get("q"));

        assertNull(map.computeIfPresent("e", f));
        assertEquals(1, count[0]);
        assertEquals(2, map.size());
        assertNull(map.get("e"));
    }

    @Test
    public void compute() {
        var map = new ConcurrentHashMap<>(Map.of("q", 23, "w", 42));

        var count = new int[1];
        BiFunction<String, Integer, Integer> f = (k, v) -> {
            count[0]++;
            return (int) k.charAt(0) * 100 + (v != null ? v : 0);
        };

        assertEquals((Object) 11323, map.compute("q", f));
        assertEquals(1, count[0]);
        assertEquals(2, map.size());
        assertEquals((Object) 11323, map.get("q"));

        assertEquals((Object) 10100, map.compute("e", f));
        assertEquals(2, count[0]);
        assertEquals(3, map.size());
        assertEquals((Object) 10100, map.get("e"));
    }

    @Test
    public void largeMap() {
        var map = new ConcurrentHashMap<Integer, Integer>();

        for (var i = 0; i < 10000; ++i) {
            map.put(i, i + 1);
        }

        assertEquals(10000, map.size());
        for (var i = 0; i < 10000; ++i) {
            assertEquals((Object) (i + 1), map.get(i));
        }

        map = new ConcurrentHashMap<>();
        for (var i = 9999; i >= 0; --i) {
            map.put(i, i + 1);
        }

        assertEquals(10000, map.size());
        for (var i = 0; i < 10000; ++i) {
            assertEquals((Object) (i + 1), map.get(i));
        }
    }

    @Test
    public void mergeRemovesEntryWhenFunctionReturnsNull() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        assertNull(map.merge("a", 2, (oldValue, value) -> null));
        assertFalse(map.containsKey("a"));
        assertEquals(0, map.size());
    }

    @Test
    public void defaultComputeIfAbsentReturnsNewValue() {
        ConcurrentMap<String, Integer> map = new SimpleConcurrentMap<>();
        assertEquals(Integer.valueOf(23), map.computeIfAbsent("a", key -> 23));
        assertEquals(Integer.valueOf(23), map.get("a"));
        assertEquals(Integer.valueOf(23), map.computeIfAbsent("a", key -> 42));
        assertNull(map.computeIfAbsent("b", key -> null));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void defaultMergeRemovesEntryWhenFunctionReturnsNull() {
        ConcurrentMap<String, Integer> map = new SimpleConcurrentMap<>();
        map.put("a", 1);
        assertNull(map.merge("a", 2, (oldValue, value) -> null));
        assertFalse(map.containsKey("a"));
    }

    // Uses the ConcurrentMap default methods, which ConcurrentHashMap partly overrides.
    static class SimpleConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
        private final Map<K, V> map = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public V put(K key, V value) {
            return map.put(key, value);
        }

        @Override
        public V putIfAbsent(K key, V value) {
            V existing = map.get(key);
            if (existing == null) {
                map.put(key, value);
            }
            return existing;
        }

        @Override
        public boolean remove(Object key, Object value) {
            if (map.containsKey(key) && map.get(key).equals(value)) {
                map.remove(key);
                return true;
            }
            return false;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            if (map.containsKey(key) && map.get(key).equals(oldValue)) {
                map.put(key, newValue);
                return true;
            }
            return false;
        }

        @Override
        public V replace(K key, V value) {
            return map.containsKey(key) ? map.put(key, value) : null;
        }
    }
}
