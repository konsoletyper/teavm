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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
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
    public void nullKeysAndValuesThrow() {
        var map = new ConcurrentHashMap<String, Integer>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
        assertThrows(NullPointerException.class, () -> map.put("a", null));
        assertThrows(NullPointerException.class, () -> map.get(null));
        assertThrows(NullPointerException.class, () -> map.containsKey(null));
        assertThrows(NullPointerException.class, () -> map.remove(null));
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("a", null));
        assertFalse(map.remove("a", null));
        assertTrue(map.isEmpty());
    }

    @Test
    public void computeWithNullResultStoresNothing() {
        var map = new ConcurrentHashMap<String, Integer>();
        assertNull(map.computeIfAbsent("x", k -> null));
        assertFalse(map.containsKey("x"));
        map.put("y", 1);
        assertNull(map.computeIfPresent("y", (k, v) -> null));
        assertFalse(map.containsKey("y"));
    }

    @Test
    public void iteratorIsWeaklyConsistent() {
        var map = new ConcurrentHashMap<String, Integer>();
        for (var i = 0; i < 100; i++) {
            map.put("k" + i, i);
        }
        var seen = new HashSet<String>();
        var originals = 0;
        for (var key : map.keySet()) {
            assertTrue(seen.add(key));
            if (key.startsWith("k")) {
                originals++;
            }
            map.remove(key);
            map.put("new" + key, 0);
        }
        assertEquals(100, originals);

        var iterator = map.keySet().iterator();
        var first = iterator.next();
        iterator.remove();
        assertFalse(map.containsKey(first));
    }

    @Test
    public void keySetViews() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        ConcurrentHashMap.KeySetView<String, Integer> view = map.keySet(9);
        assertEquals((Object) 9, view.getMappedValue());
        assertTrue(view.add("b"));
        assertFalse(view.add("a"));
        assertEquals((Object) 9, map.get("b"));
        assertEquals((Object) 1, map.get("a"));

        var set = ConcurrentHashMap.<String>newKeySet();
        assertTrue(set.add("x"));
        assertTrue(set.contains("x"));
        assertFalse(set.add("x"));
        assertTrue(set.remove("x"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void keysDifferingInHighBitsIterateInJdkOrder() {
        var map = new ConcurrentHashMap<FixedHashKey, Integer>();
        // The hash codes of two keys that a masked raw hash puts in one bucket.
        map.put(new FixedHashKey("node0", -1775181064), 0);
        map.put(new FixedHashKey("node1", -1775150312), 1);
        // The low 16 bits are 0, so a masked raw hash puts these keys in bucket 0.
        for (var i = 1; i < 8; i++) {
            map.put(new FixedHashKey("high" + i, i << 16), i);
        }
        var order = new ArrayList<String>();
        for (var key : map.keySet()) {
            order.add(key.name);
        }
        assertEquals(List.of("high1", "high2", "high3", "high4", "high5", "high6", "high7", "node0", "node1"), order);

        // After resizes (to 64 buckets), and after removals.
        for (var i = 8; i < 40; i++) {
            map.put(new FixedHashKey("high" + i, i << 16), i);
        }
        for (var i = 8; i < 40; i += 2) {
            map.remove(new FixedHashKey("high" + i, i << 16));
        }
        order.clear();
        for (var key : map.keySet()) {
            order.add(key.name);
        }
        assertEquals(List.of("high1", "high2", "high3", "high4", "high5", "high6", "high7", "node0", "high9", "high11",
                "high13", "high15", "high17", "high19", "high21", "high23", "high25", "high27", "high29", "high31",
                "high33", "high35", "high37", "high39", "node1"), order);
    }

    static final class FixedHashKey {
        final String name;
        final int hash;

        FixedHashKey(String name, int hash) {
            this.name = name;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FixedHashKey && ((FixedHashKey) obj).name.equals(name);
        }
    }

    // ConcurrentHashMap: "Like Hashtable but unlike HashMap, this class does not allow null to be used as a key
    // or value."
    @Test
    public void nullKeysAndValuesThrowInEveryOperation() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        assertThrows(NullPointerException.class, () -> map.getOrDefault(null, 0));
        assertThrows(NullPointerException.class, () -> map.containsValue(null));
        assertThrows(NullPointerException.class, () -> map.replace(null, 1));
        assertThrows(NullPointerException.class, () -> map.replace("a", null));
        assertThrows(NullPointerException.class, () -> map.replace("a", null, 2));
        assertThrows(NullPointerException.class, () -> map.replace("a", 1, null));
        assertThrows(NullPointerException.class, () -> map.remove(null, 1));
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, k -> 1));
        assertThrows(NullPointerException.class, () -> map.computeIfPresent(null, (k, v) -> 1));
        assertThrows(NullPointerException.class, () -> map.compute(null, (k, v) -> 1));
        assertThrows(NullPointerException.class, () -> map.replaceAll((k, v) -> null));
        assertThrows(NullPointerException.class, () -> map.keySet(null));
        assertThrows(NullPointerException.class, () -> map.keySet().contains(null));
        var withNullValue = new HashMap<String, Integer>();
        withNullValue.put("b", null);
        assertThrows(NullPointerException.class, () -> map.putAll(withNullValue));
        var entry = map.entrySet().iterator().next();
        assertThrows(NullPointerException.class, () -> entry.setValue(null));
        assertFalse(map.entrySet().contains(new AbstractMap.SimpleEntry<>("a", null)));
        assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("a", null)));
        assertEquals(Map.of("a", 1), new HashMap<>(map));
    }

    // Map.compute: "If the remapping function returns null, the mapping is removed (or remains absent if
    // initially absent)."
    @Test
    public void computeWithNullResultRemovesOrStoresNothing() {
        var map = new ConcurrentHashMap<String, Integer>();
        assertNull(map.compute("x", (k, v) -> null));
        assertFalse(map.containsKey("x"));
        map.put("y", 1);
        assertNull(map.compute("y", (k, v) -> null));
        assertFalse(map.containsKey("y"));
        assertNull(map.computeIfPresent("absent", (k, v) -> 5));
        assertFalse(map.containsKey("absent"));
        map.put("z", 1);
        assertEquals((Object) 1, map.computeIfAbsent("z", k -> 7));
        assertEquals((Object) 3, map.computeIfPresent("z", (k, v) -> v + 2));
        assertEquals((Object) 3, map.get("z"));
    }

    // java.util.concurrent package summary: weakly consistent iterators "are guaranteed to traverse elements as
    // they existed upon construction exactly once, and may (but are not guaranteed to) reflect any modifications
    // subsequent to construction." ConcurrentHashMap: iterators "do not throw ConcurrentModificationException".
    @Test
    public void iteratorTraversesEntriesThatSurviveExactlyOnce() {
        for (var seed = 0; seed < 50; ++seed) {
            var random = new Random(seed);
            var map = new ConcurrentHashMap<Integer, Integer>(random.nextInt(4) * 8);
            var size = 1 + random.nextInt(60);
            for (var i = 0; i < size; ++i) {
                map.put(i, i);
            }
            var removedBeforeReached = new HashSet<Integer>();
            var seen = new HashSet<Integer>();
            var next = size;
            for (var it = map.entrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                var key = entry.getKey();
                assertTrue(seen.add(key), "seed " + seed + ": " + key + " returned twice");
                // An entry removed after the iterator was created may still be returned. One that is still in the
                // map has its current value.
                if (map.containsKey(key)) {
                    assertEquals(map.get(key), entry.getValue());
                }
                switch (random.nextInt(5)) {
                    case 0:
                        it.remove();
                        assertFalse(map.containsKey(key));
                        break;
                    case 1: {
                        // Remove an original key that may not have been reached yet
                        var victim = random.nextInt(size);
                        if (map.remove(victim) != null && !seen.contains(victim)) {
                            removedBeforeReached.add(victim);
                        }
                        break;
                    }
                    case 2:
                        // Enough additions to resize the table while iterating
                        for (var j = 0; j < 20; ++j) {
                            map.put(next++, -1);
                        }
                        break;
                    case 3:
                        if (map.containsKey(key)) {
                            entry.setValue(key + 1000);
                            assertEquals((Object) (key + 1000), map.get(key));
                        }
                        break;
                    default:
                        break;
                }
            }
            for (var i = 0; i < size; ++i) {
                assertTrue(seen.contains(i) || removedBeforeReached.contains(i),
                        "seed " + seed + ": original key " + i + " was not returned");
            }
        }
    }

    @Test
    public void iteratorRemoveRequiresNext() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        var it = map.values().iterator();
        assertThrows(IllegalStateException.class, it::remove);
        assertEquals((Object) 1, it.next());
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertTrue(map.isEmpty());
    }

    @Test
    public void iteratorSurvivesClear() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        var it = map.entrySet().iterator();
        map.clear();
        map.put("a", 3);
        // The iterator may or may not reflect the clear (the JDK returns an entry it had already reached; TeaVM
        // skips the cleared entries), but it does not throw or return a key twice
        var keys = new HashSet<String>();
        while (it.hasNext()) {
            assertTrue(keys.add(it.next().getKey()));
        }
        assertEquals(Map.of("a", 3), new HashMap<>(map));
    }

    // forEach and replaceAll traverse like the weakly consistent iterators, so an action that adds entries (and
    // resizes the table) still visits every original entry exactly once
    @Test
    public void bulkTraversalVisitsOriginalEntriesExactlyOnceWhileTableGrows() {
        var map = new ConcurrentHashMap<Integer, Integer>();
        for (var i = 0; i < 12; ++i) {
            map.put(i, i);
        }
        var visits = new HashMap<Integer, Integer>();
        var next = new int[] { 100 };
        map.forEach((k, v) -> {
            visits.merge(k, 1, Integer::sum);
            for (var j = 0; j < 10; ++j) {
                map.put(next[0]++, -1);
            }
        });
        for (var i = 0; i < 12; ++i) {
            assertEquals((Object) 1, visits.get(i), "forEach visits of " + i);
        }
        for (var count : visits.values()) {
            assertEquals((Object) 1, count);
        }

        var replaced = new HashMap<Integer, Integer>();
        var original = new HashSet<>(map.keySet());
        map.replaceAll((k, v) -> {
            replaced.merge(k, 1, Integer::sum);
            map.put(next[0]++, -1);
            return v;
        });
        for (var k : original) {
            assertEquals((Object) 1, replaced.get(k), "replaceAll visits of " + k);
        }
        for (var count : replaced.values()) {
            assertEquals((Object) 1, count);
        }
    }

    // An entry that an iterator returned stays a view of its mapping after the table grows
    @Test
    public void entryWritesThroughAfterTableGrows() {
        var map = new ConcurrentHashMap<Integer, Integer>();
        map.put(1, 10);
        var it = map.entrySet().iterator();
        var entry = it.next();
        for (var i = 2; i < 200; ++i) {
            map.put(i, i);
        }
        // Map.Entry.setValue: "Replaces the value corresponding to this entry with the specified value (optional
        // operation). (Writes through to the map.)"
        assertEquals((Object) 10, entry.setValue(11));
        assertEquals((Object) 11, map.get(1));
        assertEquals((Object) 11, entry.getValue());
        assertEquals("1=11", entry.toString());
        // The rest of the original entries (there are none) and nothing twice
        var keys = new HashSet<Integer>();
        while (it.hasNext()) {
            assertTrue(keys.add(it.next().getKey()));
        }
        assertFalse(keys.contains(1));

        // Iterating again, then growing again, keeps the entry a view
        var second = map.entrySet().iterator();
        var count = 0;
        while (second.hasNext()) {
            second.next();
            if (++count == 3) {
                for (var i = 200; i < 1000; ++i) {
                    map.put(i, i);
                }
            }
        }
        assertEquals(1000 - 1, map.size());
        entry.setValue(13);
        assertEquals((Object) 13, map.get(1));
    }

    // Map.Entry.equals: two entries are equal if "(e1.getKey()==null ? e2.getKey()==null : e1.getKey().equals(
    // e2.getKey())) && (e1.getValue()==null ? e2.getValue()==null : e1.getValue().equals(e2.getValue()))"
    @Test
    public void entriesAreEqualByKeyAndValue() {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("a", 1);
        var entry = map.entrySet().iterator().next();
        assertTrue(entry.equals(new AbstractMap.SimpleEntry<>("a", 1)));
        assertFalse(entry.equals(new AbstractMap.SimpleEntry<>("a", 2)));
        assertEquals(new AbstractMap.SimpleEntry<>("a", 1).hashCode(), entry.hashCode());
        assertEquals(Map.of("a", 1).entrySet(), map.entrySet());
        assertFalse(Map.of("a", 2).entrySet().equals(map.entrySet()));
    }

    // KeySetView.add: "UnsupportedOperationException - if no default mapped value for additions was provided"
    @Test
    public void keySetViewWithoutMappedValueRejectsAdditions() {
        var map = new ConcurrentHashMap<String, Integer>();
        var view = map.keySet();
        assertNull(view.getMappedValue());
        assertTrue(view.getMap() == map);
        assertThrows(UnsupportedOperationException.class, () -> view.add("a"));
        assertThrows(UnsupportedOperationException.class, () -> view.addAll(List.of("a")));
        var withValue = map.keySet(5);
        assertTrue(withValue.addAll(List.of("a", "b")));
        assertFalse(withValue.addAll(List.of("a", "b")));
        assertThrows(NullPointerException.class, () -> withValue.add(null));
        assertEquals(Map.of("a", 5, "b", 5), new HashMap<>(map));
        assertTrue(view.remove("a"));
        assertFalse(view.remove("a"));
        assertThrows(NullPointerException.class, () -> view.remove(null));
        assertEquals(1, view.size());
        view.clear();
        assertTrue(map.isEmpty());
    }
}
